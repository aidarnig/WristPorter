package io.github.aidarnig.wgs.adb;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BooleanSupplier;

/**
 * Local-only APK installation backend. NO UI, HTTP endpoint, or photo-transfer coordinator hook.
 * Caller MUST first own an exclusive selected-watch operation lease from W11-B; rechecks do not
 * replace atomic coordinator admission. Never log a CommandResult, serial, or APK path.
 */
public final class ApkInstallService {
    private static final Duration DISCOVER_TIMEOUT = Duration.ofSeconds(15);
    private static final Duration QUERY_TIMEOUT = Duration.ofSeconds(20);
    private static final Duration INSTALL_TIMEOUT = Duration.ofMinutes(4);
    private static final int CAPTURE_LIMIT = 16 * 1024;

    public enum Code { INSTALLED, UPDATED, LEASE_REVOKED, INVALID_REQUEST, APK_CHANGED, UNTRUSTED_APK,
        WATCH_MISSING, WATCH_OFFLINE, WATCH_UNAUTHORIZED, DEVICE_QUERY_FAILED,
        SIGNATURE_MISMATCH, VERSION_DOWNGRADE, LOW_STORAGE, INSTALL_REJECTED,
        TIMED_OUT, INTERRUPTED, ADB_FAILED }
    public record Result(Code code, String message) {
        public boolean succeeded() { return code == Code.INSTALLED || code == Code.UPDATED; }
        static Result of(Code code) { return new Result(code, messageFor(code)); }
    }
    /** The lease is checked at every boundary; only W11-B provides atomic reservation. */
    public record Request(Path adbExecutable, String deviceSerial, ApkPreflight.Inspected apk,
                          ApkPreflight.Policy policy, BooleanSupplier exclusiveLeaseIsActive) {}
    public record Command(int exitCode, String output) {
        public Command { output = Objects.requireNonNullElse(output, ""); }
    }
    @FunctionalInterface public interface Runner {
        Command run(List<String> argv, Duration timeout) throws IOException, InterruptedException;
    }
    public static final class TimedOut extends IOException {
        public TimedOut() { super("ADB command timed out"); }
    }

    @FunctionalInterface interface Inspector {
        ApkPreflight.Inspected inspect(Path path, ApkPreflight.Policy policy) throws ApkPreflight.Rejected;
    }
    private final Runner runner;
    private final Inspector inspector;
    public ApkInstallService() { this(new ProcessRunner(), new ApkPreflight()::inspect); }
    // Only accessible to same-package deterministic tests; production cannot override verification.
    ApkInstallService(Runner runner, Inspector inspector) {
        this.runner = Objects.requireNonNull(runner);
        this.inspector = Objects.requireNonNull(inspector);
    }

    /** Never retries: an interrupted install may already have succeeded on the watch. */
    public Result install(Request request) {
        if (!validRequest(request)) return Result.of(Code.INVALID_REQUEST);
        if (!leaseValid(request)) return Result.of(Code.LEASE_REVOKED);
        Path staged = null;
        try {
            // The caller-inspected file may have changed. Install a private byte-for-byte snapshot.
            staged = Files.createTempFile("wristporter-install-", ".apk");
            if (!copyInspectedBytes(request.apk(), staged))
                return Result.of(Code.APK_CHANGED);
            // Signature and package MUST be checked again on the exact bytes passed to ADB.
            ApkPreflight.Inspected snapshot;
            try {
                snapshot = inspector.inspect(staged, request.policy());
            } catch (ApkPreflight.Rejected ex) {
                return Result.of(Code.UNTRUSTED_APK);
            }
            if (!snapshot.fileSha256().equalsIgnoreCase(request.apk().fileSha256())
                    || !snapshot.packageName().equals(request.apk().packageName())
                    || snapshot.versionCode() != request.apk().versionCode()
                    || !snapshot.signerSha256().equalsIgnoreCase(request.apk().signerSha256())
                    || snapshot.testOnly() != request.apk().testOnly()
                    || snapshot.mode() != request.apk().mode()) return Result.of(Code.APK_CHANGED);
            if (!leaseValid(request)) return Result.of(Code.LEASE_REVOKED);

            Command devices = call(request.adbExecutable(), DISCOVER_TIMEOUT, "devices", "-l");
            if (devices.exitCode() != 0) return Result.of(Code.DEVICE_QUERY_FAILED);
            List<WatchDevice> watches = AdbDeviceParser.parse(devices.output());
            List<WatchDevice> selected = watches.stream().filter(d -> request.deviceSerial().equals(d.serial())).toList();
            if (selected.size() != 1) return Result.of(Code.WATCH_MISSING);
            if ("unauthorized".equals(selected.getFirst().state())) return Result.of(Code.WATCH_UNAUTHORIZED);
            if (!selected.getFirst().ready()) return Result.of(Code.WATCH_OFFLINE);
            if (!leaseValid(request)) return Result.of(Code.LEASE_REVOKED);

            Command existing = call(request.adbExecutable(), QUERY_TIMEOUT, "-s", request.deviceSerial(),
                    "shell", "pm", "path", request.apk().packageName());
            if (existing.exitCode() != 0) return Result.of(Code.DEVICE_QUERY_FAILED);
            String state = existing.output().strip();
            // Strictly accept the one read-only package query response. No guessed installation state.
            if (!state.isEmpty() && !state.lines().allMatch(s -> s.startsWith("package:/")))
                return Result.of(Code.DEVICE_QUERY_FAILED);
            boolean isUpdate = !state.isEmpty();
            if (!leaseValid(request)) return Result.of(Code.LEASE_REVOKED);

            List<String> args = new ArrayList<>(List.of("-s", request.deviceSerial(), "install", "-r"));
            if (request.apk().testOnly()) args.add("-t");
            args.add(staged.toAbsolutePath().toString());
            Command response = call(request.adbExecutable(), INSTALL_TIMEOUT, args.toArray(String[]::new));
            if (response.exitCode() == 0 && response.output().lines().anyMatch(s -> "Success".equals(s.strip())))
                return Result.of(isUpdate ? Code.UPDATED : Code.INSTALLED);
            return Result.of(interpretFailure(response.output()));
        } catch (TimedOut ex) {
            return Result.of(Code.TIMED_OUT);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return Result.of(Code.INTERRUPTED);
        } catch (IOException ex) {
            return Result.of(Code.ADB_FAILED);
        } finally {
            if (staged != null) {
                try { Files.deleteIfExists(staged); } catch (IOException ignored) {
                    // Never leak the private staging path in logs; OS temp cleanup is a fallback.
                }
            }
        }
    }


    /**
     * The owner-selected APK may grow or be replaced after preflight. Refuse to follow
     * newly planted source links and stop at the *inspected* byte limit, not only after
     * a potentially unbounded copy has exhausted the Windows temp volume.
     */
    private static boolean copyInspectedBytes(ApkPreflight.Inspected original, Path staged)
            throws IOException {
        long total = 0;
        try (InputStream in = Files.newInputStream(original.path(), LinkOption.NOFOLLOW_LINKS);
             OutputStream out = Files.newOutputStream(staged, StandardOpenOption.WRITE,
                     StandardOpenOption.TRUNCATE_EXISTING, LinkOption.NOFOLLOW_LINKS)) {
            byte[] buffer = new byte[32 * 1024];
            for (int read; (read = in.read(buffer)) != -1;) {
                if (read == 0) continue;
                total += read;
                if (total > original.size()) return false;
                out.write(buffer, 0, read);
            }
        }
        return total == original.size()
                && ApkPreflight.sha256File(staged).equalsIgnoreCase(original.fileSha256());
    }

    private Command call(Path adb, Duration timeout, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>(args.length + 1);
        command.add(adb.toString());
        command.addAll(List.of(args));
        return runner.run(List.copyOf(command), timeout);
    }
    private static boolean validRequest(Request r) {
        if (r == null || r.adbExecutable() == null || r.deviceSerial() == null
                || r.apk() == null || r.policy() == null || r.exclusiveLeaseIsActive() == null) return false;
        String serial = r.deviceSerial();
        if (!serial.matches("[A-Za-z0-9._:-]{1,160}") || serial.startsWith("-")) return false;
        if (!r.adbExecutable().isAbsolute() || !Files.isRegularFile(r.adbExecutable(), LinkOption.NOFOLLOW_LINKS))
            return false;
        Path apk = r.apk().path();
        return apk != null && apk.isAbsolute() && Files.isRegularFile(apk, LinkOption.NOFOLLOW_LINKS)
                && !Files.isSymbolicLink(apk) && ApkPreflight.BRIDGE_PACKAGE.equals(r.apk().packageName())
                && r.apk().size() > 0 && r.apk().size() <= ApkPreflight.MAX_BYTES
                && r.apk().mode() == r.policy().mode()
                && r.apk().signerSha256() != null && r.apk().signerSha256().matches("[0-9A-Fa-f]{64}")
                && r.apk().fileSha256() != null
                && r.apk().fileSha256().matches("[0-9A-Fa-f]{64}");
    }
    private static boolean leaseValid(Request request) {
        return request.exclusiveLeaseIsActive().getAsBoolean();
    }
    private static Code interpretFailure(String raw) {
        String output = raw == null ? "" : raw;
        if (output.contains("INSTALL_FAILED_UPDATE_INCOMPATIBLE") || output.contains("INSTALL_FAILED_SHARED_USER_INCOMPATIBLE"))
            return Code.SIGNATURE_MISMATCH;
        if (output.contains("INSTALL_FAILED_VERSION_DOWNGRADE")) return Code.VERSION_DOWNGRADE;
        if (output.contains("INSTALL_FAILED_INSUFFICIENT_STORAGE")) return Code.LOW_STORAGE;
        return Code.INSTALL_REJECTED;
    }
    private static String messageFor(Code code) {
        return switch (code) {
            case INSTALLED -> "WristPorter Bridge installed";
            case UPDATED -> "WristPorter Bridge updated";
            case LEASE_REVOKED -> "Exclusive watch operation is no longer authorized";
            case INVALID_REQUEST -> "Select an authorized watch, bundled ADB, and inspected APK";
            case APK_CHANGED -> "APK changed after inspection; inspect again";
            case UNTRUSTED_APK -> "APK is unsigned, malformed, or not trusted for the chosen signing mode";
            case WATCH_MISSING -> "Selected watch is no longer connected";
            case WATCH_OFFLINE -> "Selected watch is offline";
            case WATCH_UNAUTHORIZED -> "Authorize ADB on the selected watch";
            case DEVICE_QUERY_FAILED -> "Cannot verify selected watch or installed package";
            case SIGNATURE_MISMATCH -> "Existing app has a different signing certificate; nothing was uninstalled";
            case VERSION_DOWNGRADE -> "Downgrade refused; no app data was cleared";
            case LOW_STORAGE -> "Watch has insufficient free storage";
            case INSTALL_REJECTED -> "Watch rejected APK installation";
            case TIMED_OUT -> "Install timed out; watch state may have changed. Check before trying again";
            case INTERRUPTED -> "Install interrupted; watch state is unknown. Check before trying again";
            case ADB_FAILED -> "ADB operation failed; verify connection without retrying automatically";
        };
    }

    /** Real runner: no shell, bounded time and capture, destroys children on timeout/interruption. */
    public static final class ProcessRunner implements Runner {
        @Override public Command run(List<String> argv, Duration timeout) throws IOException, InterruptedException {
            if (argv == null || argv.isEmpty() || timeout.isNegative() || timeout.isZero())
                throw new IllegalArgumentException("Invalid command");
            Process process = new ProcessBuilder(argv).redirectErrorStream(true).start();
            try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
                Future<String> reader = executor.submit(() -> readBounded(process.getInputStream()));
                process.getOutputStream().close();
                try {
                    if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                        process.destroyForcibly();
                        throw new TimedOut();
                    }
                    return new Command(process.exitValue(), reader.get(3, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    process.destroyForcibly();
                    throw e;
                } catch (ExecutionException | TimeoutException e) {
                    process.destroyForcibly();
                    throw new IOException("ADB output unavailable");
                }
            } finally {
                if (process.isAlive()) process.destroyForcibly();
            }
        }
        private static String readBounded(InputStream stream) throws IOException {
            try (stream; ByteArrayOutputStream captured = new ByteArrayOutputStream()) {
                byte[] chunk = new byte[4096];
                int n;
                while ((n = stream.read(chunk)) != -1) {
                    if (captured.size() < CAPTURE_LIMIT) captured.write(chunk, 0, Math.min(n, CAPTURE_LIMIT - captured.size()));
                }
                return captured.toString(StandardCharsets.UTF_8);
            }
        }
    }
}