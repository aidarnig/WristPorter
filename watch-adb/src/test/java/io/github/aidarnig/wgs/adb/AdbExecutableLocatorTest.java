package io.github.aidarnig.wgs.adb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class AdbExecutableLocatorTest {
    @TempDir Path temporary;

    @Test void bundledAdbIsTheDefaultWithoutAnInstaller() throws Exception {
        Path runtime = Files.createDirectories(temporary.resolve("WristPorter/runtime"));
        Path tools = Files.createDirectories(temporary.resolve("WristPorter/platform-tools"));
        Files.createFile(tools.resolve("adb.exe"));
        Files.createFile(tools.resolve("AdbWinApi.dll"));
        Files.createFile(tools.resolve("AdbWinUsbApi.dll"));

        assertEquals(tools.resolve("adb.exe").toString(),
                AdbExecutableLocator.resolve(runtime.toString(), temporary.toString(), null, null, "adb.exe"));
    }

    @Test void incompleteBundledAdbFallsBackToDocuments() throws Exception {
        Path runtime = Files.createDirectories(temporary.resolve("WristPorter/runtime"));
        Path tools = Files.createDirectories(temporary.resolve("WristPorter/platform-tools"));
        Files.createFile(tools.resolve("adb.exe"));
        Path documents = Files.createDirectories(temporary.resolve("Documents/platform-tools"));
        Files.createFile(documents.resolve("adb.exe"));

        assertEquals(documents.resolve("adb.exe").toString(),
                AdbExecutableLocator.resolve(runtime.toString(), temporary.toString(), null, null, "adb.exe"));
    }

    @Test void explicitOverrideTakesPrecedenceAndLegacyOverrideRemainsSupported() throws Exception {
        Path runtime = Files.createDirectories(temporary.resolve("runtime"));
        assertEquals("D:\\tools\\adb.exe",
                AdbExecutableLocator.resolve(runtime.toString(), temporary.toString(),
                        " D:\\tools\\adb.exe ", "legacy", "adb.exe"));
        assertEquals("legacy",
                AdbExecutableLocator.resolve(runtime.toString(), temporary.toString(),
                        "", "legacy", "adb.exe"));
    }

    @Test void defaultsToPathCommandWhenNothingIsFound() throws Exception {
        Path runtime = Files.createDirectories(temporary.resolve("runtime"));
        assertEquals("adb.exe", AdbExecutableLocator.resolve(
                runtime.toString(), temporary.toString(), null, null, "adb.exe"));
    }
}
