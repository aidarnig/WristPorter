package io.github.aidarnig.wgs.adb;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/** Process-level tests using an executable ADB double; no physical watch needed. */
@EnabledOnOs(OS.LINUX)
class AdbClientProcessTest {
    @TempDir Path temporary;

    private Path executable(String body) throws Exception {
        Path script = temporary.resolve("adb-fake");
        Files.writeString(script, "#!/bin/sh\n" + body);
        assertTrue(script.toFile().setExecutable(true));
        return script;
    }

    @Test void sendsPairingCodeOnlyThroughStdin() throws Exception {
        Path args = temporary.resolve("arguments.txt");
        String fake = """
                printf 'CALL\\n' >> '%s'
                for arg in "$@"; do printf 'ARG:%%s\\n' "$arg" >> '%s'; done
                if [ "$1" = "pair" ]; then
                  read pairing_code
                  if [ "$pairing_code" = "990860" ]; then
                    printf 'Successfully paired to 192.168.1.101:39337\\n'
                    exit 0
                  fi
                  exit 1
                fi
                """.formatted(args, args);
        AdbClient adb = new AdbClient(executable(fake).toString());
        adb.pair("192.168.1.101:39337", "990860");

        String loggedArgs = Files.readString(args);
        assertTrue(loggedArgs.contains("ARG:pair"));
        assertTrue(loggedArgs.contains("ARG:192.168.1.101:39337"));
        assertFalse(loggedArgs.contains("990860"), "Pairing code must not be a process argument");
        assertThrows(IllegalArgumentException.class, () -> adb.pair("192.168.1.101:39337", "not-a-code"));
    }

    @Test void pushesToSelectedWatchAndRequestsScanAfterCopy() throws Exception {
        Path log = temporary.resolve("commands.txt");
        String fake = """
                printf 'CALL\\n' >> '%s'
                for arg in "$@"; do printf 'ARG:%%s\\n' "$arg" >> '%s'; done
                if [ "$1" = "devices" ]; then
                  printf 'List of devices attached\\n192.168.1.101:38267\\tdevice\\n'
                fi
                """.formatted(log, log);
        AdbClient adb = new AdbClient(executable(fake).toString());
        byte[] imageBytes = {(byte)137, 80, 78, 71, 13, 10, 26, 10};
        Path image = Files.write(temporary.resolve("name with spaces;é.png"), imageBytes);
        String serial = "192.168.1.101:38267";

        adb.assertConnected(serial);
        adb.ensureFolder(serial);
        String remote = adb.push(image, serial);
        adb.scan(serial, remote);

        String calls = Files.readString(log);
        assertTrue(calls.contains("ARG:" + serial));
        assertTrue(calls.contains("ARG:" + image.toAbsolutePath()));
        assertTrue(calls.contains("ARG:/sdcard/DCIM/Camera/" + remote));
        assertTrue(calls.contains("ARG:file:///sdcard/DCIM/Camera/" + remote));
        assertTrue(calls.indexOf("ARG:push") < calls.indexOf("ARG:android.intent.action.MEDIA_SCANNER_SCAN_FILE"));
        assertArrayEquals(imageBytes, Files.readAllBytes(image), "Local image must not change");
    }
}
