package io.github.aidarnig.wgs.adb;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/** Resolves ADB without requiring Android Studio or manual PATH edits. */
public final class AdbExecutableLocator {
    private AdbExecutableLocator() {}

    public static String detect() {
        String executable = System.getProperty("os.name", "")
                .toLowerCase(Locale.ROOT).contains("win") ? "adb.exe" : "adb";
        return resolve(
                System.getProperty("java.home"),
                System.getProperty("user.home"),
                System.getenv("WRISTPORTER_ADB"),
                System.getenv("WGS_ADB"),
                executable);
    }

    /** Pure environment inputs enable platform-neutral tests of a packaged Windows layout. */
    public static String resolve(String javaHome, String userHome, String override,
                                 String legacyOverride, String executable) {
        if (override != null && !override.isBlank()) return override.strip();
        if (legacyOverride != null && !legacyOverride.isBlank()) return legacyOverride.strip();

        Path home = Path.of(javaHome).toAbsolutePath().normalize();
        Path appRoot = home.getParent();
        if (appRoot != null) {
            Path localAdb = appRoot.resolve("platform-tools").resolve(executable);
            boolean ready = Files.isRegularFile(localAdb);
            if (ready && executable.equalsIgnoreCase("adb.exe")) {
                Path folder = localAdb.getParent();
                ready = Files.isRegularFile(folder.resolve("AdbWinApi.dll"))
                        && Files.isRegularFile(folder.resolve("AdbWinUsbApi.dll"));
            }
            if (ready) return localAdb.toString();
        }

        Path documentsAdb = Path.of(userHome, "Documents", "platform-tools", executable);
        if (Files.isRegularFile(documentsAdb)) return documentsAdb.toString();
        return executable;
    }
}
