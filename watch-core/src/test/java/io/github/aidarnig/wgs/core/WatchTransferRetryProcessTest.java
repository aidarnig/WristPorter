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

/** Scanner-only retries are verified at the ADB subprocess boundary. */
@EnabledOnOs(OS.LINUX)
class WatchTransferRetryProcessTest {
    @TempDir Path temp;

    @Test void retriesFailedScanWithoutPushingTheImageAgain() throws Exception {
        Path calls = temp.resolve("adb-calls.txt");
        Path firstScanMarker = temp.resolve("scan-already-attempted");
        String fake = """
                printf '%%s\\n' "$3-$4" >> '%s'
                if [ "$1" = "devices" ]; then
                  printf 'List of devices attached\\n192.168.1.101:38267\\tdevice\\n'
                fi
                if [ "$3" = "shell" ] && [ "$4" = "am" ] && [ ! -f '%s' ]; then
                  touch '%s'
                  echo 'simulated scanner failure'
                  exit 14
                fi
                """.formatted(calls, firstScanMarker, firstScanMarker);
        Path fakeAdb = Files.writeString(temp.resolve("adb-fake"), "#!/bin/sh\n" + fake);
        assertTrue(fakeAdb.toFile().setExecutable(true));
        byte[] png = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};
        Path photo = Files.write(temp.resolve("picture.png"), png);
        String device = "192.168.1.101:38267";
        WatchTransferService service = new WatchTransferService();
        List<TransferUpdate> first = new ArrayList<>();

        service.send(fakeAdb.toString(), device, List.of(photo), first::add);
        assertEquals(1, first.size());
        assertEquals(TransferUpdate.Status.COPIED_SCAN_FAILED, first.getFirst().status());
        assertNotNull(first.getFirst().remoteName());
        assertTrue(first.getFirst().retryable());

        // Rescan does not require the original photo to exist locally.
        Files.delete(photo);
        List<TransferUpdate> retry = new ArrayList<>();
        service.retry(fakeAdb.toString(), device, first, retry::add);
        assertEquals(1, retry.size());
        assertEquals(TransferUpdate.Status.COPIED_SCAN_REQUESTED, retry.getFirst().status());
        assertEquals(first.getFirst().remoteName(), retry.getFirst().remoteName());

        List<String> invocations = Files.readAllLines(calls);
        assertEquals(1, invocations.stream().filter(line -> line.startsWith("push-")).count());
        assertEquals(2, invocations.stream().filter("shell-am"::equals).count());
    }

    @Test void ignoresSuccessfulItemsDuringRetry() throws Exception {
        Path fakeAdb = Files.writeString(temp.resolve("adb-unused"), "#!/bin/sh\nexit 99\n");
        List<TransferUpdate> messages = new ArrayList<>();
        new WatchTransferService().retry(fakeAdb.toString(), "watch",
                List.of(new TransferUpdate(temp.resolve("photo.png"), 1, 1,
                        TransferUpdate.Status.COPIED_SCAN_REQUESTED, "done", "wgs-123.jpg")),
                messages::add);
        assertTrue(messages.isEmpty());
    }
}
