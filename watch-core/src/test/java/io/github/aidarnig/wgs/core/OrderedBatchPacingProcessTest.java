package io.github.aidarnig.wgs.core;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Process-boundary regressions for non-destructive, interruptible batch pacing. */
@EnabledOnOs(OS.LINUX)
class OrderedBatchPacingProcessTest {
    @TempDir Path temp;

    @Test void preservesQueueOrderAndSpacesNextPushAfterMediaScan() throws Exception {
        Path log = temp.resolve("adb-calls.txt");
        Path adb = fakeAdb(log);
        Path first = png("01.png");
        Path second = jpeg("02.jpg");
        List<TransferUpdate> results = new ArrayList<>();
        byte[] firstBefore = Files.readAllBytes(first);
        byte[] secondBefore = Files.readAllBytes(second);

        new WatchTransferService().send(adb.toString(), "watch-1",
                List.of(first, second), results::add);

        assertEquals(List.of(first, second), results.stream().map(TransferUpdate::source).toList());
        assertTrue(results.stream().allMatch(r ->
                r.status() == TransferUpdate.Status.COPIED_SCAN_REQUESTED));
        List<String> calls = Files.readAllLines(log);
        assertEquals(4, calls.size());
        assertTrue(calls.get(0).startsWith("push|01.png|"));
        assertTrue(calls.get(1).startsWith("scan|"));
        assertTrue(calls.get(2).startsWith("push|02.jpg|"));
        assertTrue(calls.get(3).startsWith("scan|"));
        long firstScanMillis = Long.parseLong(calls.get(1).split("\\|")[1]);
        long secondPushMillis = Long.parseLong(calls.get(2).split("\\|")[2]);
        assertTrue(secondPushMillis - firstScanMillis >= 1_100,
                "A second copy must not start immediately after the previous scan request");
        assertArrayEquals(firstBefore, Files.readAllBytes(first));
        assertArrayEquals(secondBefore, Files.readAllBytes(second));
    }

    @Test void cancellingAfterFirstPhotoInterruptsPacingWithoutStartingSecondPush() throws Exception {
        Path log = temp.resolve("cancel-calls.txt");
        Path adb = fakeAdb(log);
        Path first = png("01.png");
        Path second = png("02.png");
        CountDownLatch firstDone = new CountDownLatch(1);
        AtomicBoolean interrupted = new AtomicBoolean(false);
        AtomicReference<Throwable> unexpected = new AtomicReference<>();

        Thread sender = new Thread(() -> {
            try {
                new WatchTransferService().send(adb.toString(), "watch-1",
                        List.of(first, second), ignored -> firstDone.countDown());
            } catch (InterruptedException expected) {
                interrupted.set(true);
            } catch (Throwable failure) {
                unexpected.set(failure);
            }
        });
        sender.start();
        try {
            assertTimeoutPreemptively(Duration.ofSeconds(10),
                    () -> assertTrue(firstDone.await(8, TimeUnit.SECONDS)));
        } finally {
            sender.interrupt();
            sender.join(5_000);
        }

        assertFalse(sender.isAlive(), "Cancellation must not block the worker");
        assertNull(unexpected.get());
        assertTrue(interrupted.get(), "Batch pacing must honor cancellation");
        assertEquals(1, Files.readAllLines(log).stream()
                .filter(line -> line.startsWith("push|")).count());
    }

    private Path png(String name) throws Exception {
        return Files.write(temp.resolve(name),
                new byte[] {(byte) 137, 80, 78, 71, 13, 10, 26, 10});
    }

    private Path jpeg(String name) throws Exception {
        return Files.write(temp.resolve(name),
                new byte[] {(byte) 255, (byte) 216, (byte) 255, 0, 0, 0, 0, 0});
    }

    private Path fakeAdb(Path log) throws Exception {
        String script = """
                #!/bin/sh
                if [ "$1" = "devices" ]; then
                  printf 'List of devices attached\\nwatch-1\\tdevice\\n'
                elif [ "$3" = "push" ]; then
                  printf 'push|%%s|%%s\\n' "$(basename "$4")" "$(date +%%s%%3N)" >> '%s'
                elif [ "$3" = "shell" ] && [ "$4" = "am" ]; then
                  printf 'scan|%%s\\n' "$(date +%%s%%3N)" >> '%s'
                fi
                """.formatted(log, log);
        Path adb = Files.writeString(temp.resolve("adb-fake"), script);
        assertTrue(adb.toFile().setExecutable(true));
        return adb;
    }
}
