package io.github.aidarnig.wgs.adb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import static org.junit.jupiter.api.Assertions.*;

class ApkInstallServiceTest {
    @TempDir Path dir;
    private final String serial = "192.168.0.45:33999";
    private final ApkPreflight.Policy policy = ApkPreflight.Policy.explicitlyAllowDebug();
    private Path adb;
    private Path file;
    private ApkPreflight.Inspected inspected;

    private void ready() throws Exception {
        adb = Files.createFile(dir.resolve("adb executable.exe"));
        file = Files.write(dir.resolve("Überraschung photo; 1.apk"), new byte[]{1, 2, 3, 4});
        inspected = new ApkPreflight.Inspected(file.toAbsolutePath(), 4L, ApkPreflight.sha256File(file),
                ApkPreflight.BRIDGE_PACKAGE, 2L, "2.0", 30, "A".repeat(64), false, ApkPreflight.Mode.DEBUG);
    }
    private ApkInstallService.Request request(BooleanSupplier lease) {
        return new ApkInstallService.Request(adb.toAbsolutePath(), serial, inspected, policy, lease);
    }
    private ApkInstallService service(List<List<String>> calls, String devices, String installed, ApkInstallService.Command install) {
        ApkInstallService.Runner runner = (argv, duration) -> {
            calls.add(argv);
            if (argv.contains("devices")) return new ApkInstallService.Command(0, devices);
            if (argv.contains("pm")) return new ApkInstallService.Command(0, installed);
            return install;
        };
        // Controlled unit double; production constructor always uses cryptographic inspector.
        return new ApkInstallService(runner, (path, p) -> new ApkPreflight.Inspected(path,
                inspected.size(), inspected.fileSha256(), inspected.packageName(), inspected.versionCode(),
                inspected.versionName(), inspected.minSdk(), inspected.signerSha256(), inspected.testOnly(), p.mode()));
    }
    @Test void firstInstallAndUpdateUseExactlySelectedDeviceAndArgumentList() throws Exception {
        ready();
        String devices = "List of devices attached\n" + serial + "\tdevice product:watch\nother-watch\tdevice\n";
        for (String installed : List.of("", "package:/data/app/base.apk")) {
            List<List<String>> calls = new ArrayList<>();
            ApkInstallService svc = service(calls, devices, installed, new ApkInstallService.Command(0, "Success\n"));
            ApkInstallService.Result out = svc.install(request(() -> true));
            assertEquals(installed.isEmpty() ? ApkInstallService.Code.INSTALLED : ApkInstallService.Code.UPDATED, out.code());
            assertEquals(3, calls.size());
            assertEquals(List.of(adb.toAbsolutePath().toString(), "devices", "-l"), calls.get(0));
            assertEquals(List.of(adb.toAbsolutePath().toString(), "-s", serial,
                    "shell", "pm", "path", ApkPreflight.BRIDGE_PACKAGE), calls.get(1));
            List<String> cmd = calls.get(2);
            assertEquals(List.of(adb.toAbsolutePath().toString(), "-s", serial, "install", "-r"), cmd.subList(0, 5));
            assertEquals(6, cmd.size(), "APK must be one argument, even with spaces/Unicode");
            assertTrue(cmd.getLast().endsWith(".apk"));
            assertFalse(cmd.getLast().contains("Überraschung"), "Install private snapshot rather than mutable source file");
            assertFalse(Files.exists(Path.of(cmd.getLast())), "Temporary APK must be deleted");
        }
    }
    @Test void missingOfflineUnauthorizedAndAmbiguousDeviceAreNotInstalled() throws Exception {
        ready();
        for (var scenario : List.of(
                new Object[]{"List of devices attached\nother\tdevice\n", ApkInstallService.Code.WATCH_MISSING},
                new Object[]{serial + "\toffline\n", ApkInstallService.Code.WATCH_OFFLINE},
                new Object[]{serial + "\tunauthorized\n", ApkInstallService.Code.WATCH_UNAUTHORIZED},
                new Object[]{serial + "\tdevice\n" + serial + "\tdevice\n", ApkInstallService.Code.WATCH_MISSING})) {
            List<List<String>> calls = new ArrayList<>();
            var svc = service(calls, (String) scenario[0], "", new ApkInstallService.Command(0, "Success"));
            assertEquals(scenario[1], svc.install(request(() -> true)).code());
            assertEquals(1, calls.size());
        }
    }
    @Test void certificateMismatchDowngradeAndStorageErrorsAreCategorized() throws Exception {
        ready();
        for (var scenario : List.of(
                new Object[]{"INSTALL_FAILED_UPDATE_INCOMPATIBLE", ApkInstallService.Code.SIGNATURE_MISMATCH},
                new Object[]{"INSTALL_FAILED_VERSION_DOWNGRADE", ApkInstallService.Code.VERSION_DOWNGRADE},
                new Object[]{"INSTALL_FAILED_INSUFFICIENT_STORAGE", ApkInstallService.Code.LOW_STORAGE},
                new Object[]{"INSTALL_FAILED_USER_RESTRICTED", ApkInstallService.Code.INSTALL_REJECTED})) {
            List<List<String>> calls = new ArrayList<>();
            var svc = service(calls, serial + "\tdevice\n", "", new ApkInstallService.Command(1, (String) scenario[0]));
            var result = svc.install(request(() -> true));
            assertEquals(scenario[1], result.code());
            assertFalse(result.message().contains(serial));
            assertFalse(result.message().contains(file.toString()));
        }
    }
    @Test void staleFileOrLeasePreventsSideEffects() throws Exception {
        ready();
        List<List<String>> calls = new ArrayList<>();
        var svc = service(calls, serial + "\tdevice\n", "", new ApkInstallService.Command(0, "Success"));
        Files.writeString(file, "changed since inspection");
        assertEquals(ApkInstallService.Code.APK_CHANGED, svc.install(request(() -> true)).code());
        assertTrue(calls.isEmpty());
        assertEquals(ApkInstallService.Code.LEASE_REVOKED, svc.install(request(() -> false)).code());
    }
    @Test void leaseRevocationBetweenDiscoveryAndInstallationIsRejected() throws Exception {
        ready();
        AtomicBoolean lease = new AtomicBoolean(true);
        List<List<String>> calls = new ArrayList<>();
        ApkInstallService.Runner runner = (argv, timeout) -> {
            calls.add(argv);
            lease.set(false);
            return new ApkInstallService.Command(0, serial + "\tdevice\n");
        };
        ApkInstallService svc = new ApkInstallService(runner, (p, policy) -> new ApkPreflight.Inspected(p,
                inspected.size(), inspected.fileSha256(), inspected.packageName(), inspected.versionCode(),
                inspected.versionName(), inspected.minSdk(), inspected.signerSha256(), false, policy.mode()));
        assertEquals(ApkInstallService.Code.LEASE_REVOKED, svc.install(request(lease::get)).code());
        assertEquals(1, calls.size());
    }
    @Test void timeoutsAndInterruptionsAreNeverAutomaticallyRetried() throws Exception {
        ready();
        for (boolean interrupted : new boolean[]{false, true}) {
            List<List<String>> calls = new ArrayList<>();
            ApkInstallService.Runner r = (argv, timeout) -> {
                calls.add(argv);
                if (interrupted) throw new InterruptedException("simulated");
                throw new ApkInstallService.TimedOut();
            };
            ApkInstallService svc = new ApkInstallService(r, (p, policy) -> new ApkPreflight.Inspected(p,
                    inspected.size(), inspected.fileSha256(), inspected.packageName(), inspected.versionCode(),
                    inspected.versionName(), inspected.minSdk(), inspected.signerSha256(), false, policy.mode()));
            assertEquals(interrupted ? ApkInstallService.Code.INTERRUPTED : ApkInstallService.Code.TIMED_OUT,
                    svc.install(request(() -> true)).code());
            assertEquals(1, calls.size());
            if (interrupted) assertTrue(Thread.interrupted(), "Test clears interrupt flag");
        }
    }
    @Test void forgedInspectableMetadataDoesNotBypassSnapshotPreflight() throws Exception {
        ready();
        List<List<String>> calls = new ArrayList<>();
        ApkInstallService svc = new ApkInstallService((args, timeout) -> {
            calls.add(args); return new ApkInstallService.Command(0, "Success");
        }, (p, policy) -> { throw new ApkPreflight.Rejected(ApkPreflight.Error.UNSIGNED_OR_INVALID); });
        assertEquals(ApkInstallService.Code.UNTRUSTED_APK, svc.install(request(() -> true)).code());
        assertTrue(calls.isEmpty());
    }
    @Test void manifestTestOnlyEnablesTWithoutDowngradeOrUninstall() throws Exception {
        ready();
        inspected = new ApkPreflight.Inspected(inspected.path(), inspected.size(),
                inspected.fileSha256(), inspected.packageName(), inspected.versionCode(),
                inspected.versionName(), inspected.minSdk(), inspected.signerSha256(), true, inspected.mode());
        List<List<String>> calls = new ArrayList<>();
        var svc = service(calls, serial + "\tdevice\n", "", new ApkInstallService.Command(0, "Success"));
        assertEquals(ApkInstallService.Code.INSTALLED, svc.install(request(() -> true)).code());
        assertEquals(List.of(adb.toAbsolutePath().toString(), "-s", serial, "install", "-r", "-t"),
                calls.get(2).subList(0, 6));
        assertEquals(7, calls.get(2).size());
        assertFalse(calls.get(2).contains("-d"));
        assertFalse(calls.get(2).contains("uninstall"));
    }

}