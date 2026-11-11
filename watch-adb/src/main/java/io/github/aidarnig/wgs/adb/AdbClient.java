package io.github.aidarnig.wgs.adb;

import io.github.aidarnig.wgs.domain.PhotoFiles;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

public final class AdbClient {
    private static final String DEST = "/sdcard/DCIM/Camera";
    private static final Duration SHORT = Duration.ofSeconds(25);
    private static final Duration PUSH = Duration.ofMinutes(5);
    private final String executable;

    public AdbClient(String executable) {
        if (executable == null || executable.isBlank()) throw new IllegalArgumentException("ADB executable is required");
        this.executable = executable;
    }

    public List<WatchDevice> devices() throws IOException, InterruptedException {
        return AdbDeviceParser.parse(checked(SHORT, "devices", "-l"));
    }

    public void connect(String address) throws IOException, InterruptedException {
        validateEndpoint(address);
        checked(SHORT, "connect", address);
    }

    /** Writes pairing code over stdin, not a process argument or log. */
    public void pair(String address, String code) throws IOException, InterruptedException {
        validateEndpoint(address);
        if (code == null || !code.matches("[0-9]{6}")) throw new IllegalArgumentException("Six-digit pairing code required");
        AdbResponse response = run(SHORT, code + "\n", "pair", address);
        if (!response.succeeded() || !response.output().contains("Successfully paired")) {
            throw new IOException("Pairing failed: " + response.output().replace(code, "[hidden]").strip());
        }
    }

    public void assertConnected(String serial) throws IOException, InterruptedException {
        if (serial == null || serial.isBlank() ||
                devices().stream().noneMatch(device -> device.ready() && device.serial().equals(serial))) {
            throw new IOException("Selected watch is offline, unauthorized, or missing: " + serial);
        }
    }

    public void ensureFolder(String serial) throws IOException, InterruptedException {
        checked(SHORT, "-s", serial, "shell", "mkdir", "-p", DEST);
    }

    public String push(Path file, String serial) throws IOException, InterruptedException {
        PhotoFiles.requireReadable(file);
        String remote = PhotoFiles.remoteName(file);
        checked(PUSH, "-s", serial, "push", file.toAbsolutePath().toString(), DEST + "/" + remote);
        return remote;
    }

    public void scan(String serial, String remoteName) throws IOException, InterruptedException {
        Objects.requireNonNull(remoteName);
        if (!remoteName.matches("wgs-[0-9a-f-]{36}\\.(jpg|jpeg|png)")) {
            throw new IllegalArgumentException("Invalid remote filename");
        }
        checked(SHORT, "-s", serial, "shell", "am", "broadcast", "-a",
                "android.intent.action.MEDIA_SCANNER_SCAN_FILE", "-d", "file://" + DEST + "/" + remoteName);
    }

    private String checked(Duration timeout, String... args) throws IOException, InterruptedException {
        AdbResponse result = run(timeout, null, args);
        if (!result.succeeded()) throw new IOException("ADB returned " + result.exitCode() + ": " + result.output().strip());
        return result.output();
    }

    private AdbResponse run(Duration timeout, String input, String... args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(executable);
        command.addAll(List.of(args));
        Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
        try (ExecutorService reader = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<String> captured = reader.submit(() -> readLimited(process.getInputStream()));
            try {
                if (input != null) {
                    process.getOutputStream().write(input.getBytes(StandardCharsets.UTF_8));
                }
                process.getOutputStream().close();
                if (!process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                    throw new IOException("ADB timed out after " + timeout.toSeconds() + " seconds");
                }
                return new AdbResponse(process.exitValue(), captured.get(3, TimeUnit.SECONDS));
            } catch (InterruptedException ex) {
                process.destroyForcibly();
                Thread.currentThread().interrupt();
                throw ex;
            } catch (ExecutionException | TimeoutException ex) {
                process.destroyForcibly();
                throw new IOException("Could not read ADB output", ex);
            }
        } finally {
            if (process.isAlive()) process.destroyForcibly();
        }
    }

    private static String readLimited(InputStream input) throws IOException {
        try (input; ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = input.read(buffer)) != -1) {
                out.write(buffer, 0, Math.min(read, Math.max(0, 65536 - out.size())));
            }
            return out.toString(StandardCharsets.UTF_8);
        }
    }

    private static void validateEndpoint(String address) {
        WatchEndpoint.parse(address);
    }
}
