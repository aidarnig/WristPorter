package io.github.aidarnig.wgs.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** End-to-end service orchestration using a fake adb process. */
@EnabledOnOs(OS.LINUX)
class WatchTransferServiceProcessTest {
    @TempDir Path temp;

    @Test void continuesAfterFailedFileAndScansOnlySuccessfulCopies() throws Exception {
        Path log = temp.resolve("calls.txt");
        String fake = """
                printf 'COMMAND:%%s-%%s\\n' "$3" "$4" >> '%s'
                if [ "$1" = "devices" ]; then
                  printf 'List of devices attached\\n192.168.1.101:38267\\tdevice\\n'
                fi
                if [ "$3" = "push" ]; then
                  case "$4" in *fail.png) echo 'simulated copy failure'; exit 2;; esac
                fi
                """.formatted(log);
        Path adb = temp.resolve("adb-fake");
        Files.writeString(adb, "#!/bin/sh\n" + fake);
        assertTrue(adb.toFile().setExecutable(true));

        Path good1 = Files.writeString(temp.resolve("first.png"), "placeholder");
        Path bad = Files.writeString(temp.resolve("fail.png"), "placeholder");
        Path good2 = Files.writeString(temp.resolve("second.jpg"), "placeholder");
        List<TransferUpdate> updates = new ArrayList<>();

        new WatchTransferService().send(adb.toString(), "192.168.1.101:38267",
                List.of(good1, bad, good2), updates::add);

        assertEquals(3, updates.size());
        assertEquals(TransferUpdate.Status.COPIED_SCAN_REQUESTED, updates.get(0).status());
        assertEquals(TransferUpdate.Status.FAILED, updates.get(1).status());
        assertEquals(TransferUpdate.Status.COPIED_SCAN_REQUESTED, updates.get(2).status());
        assertEquals(3, updates.get(2).position());
        long scans = Files.readString(log).lines().filter("COMMAND:shell-am"::equals).count();
        assertEquals(2, scans, "A failed push must not trigger a scan");
    }
}
