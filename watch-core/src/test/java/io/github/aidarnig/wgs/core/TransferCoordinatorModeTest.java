package io.github.aidarnig.wgs.core;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class TransferCoordinatorModeTest {
    private static final String ADB = "fake-adb";
    private static final Path FIRST = Path.of("one.png");
    private static final Path SECOND = Path.of("two.png");

    @Test void modeIsPerOwnerSnapshottedPerJobAndCannotChangeWhileRunning() {
        List<Runnable> scheduled = new ArrayList<>();
        List<WatchTransferService.BatchMode> executedModes = new ArrayList<>();
        TransferCoordinator.TransferBackend backend = new TransferCoordinator.TransferBackend() {
            @Override public void send(String adb, String serial, List<Path> paths,
                                       Consumer<TransferUpdate> observer) {
                fail("Production must dispatch the mode-aware overload");
            }

            @Override public void send(String adb, String serial, List<Path> paths,
                                       WatchTransferService.BatchMode mode,
                                       Consumer<TransferUpdate> observer) {
                executedModes.add(mode);
                for (int i = 0; i < paths.size(); i++) {
                    observer.accept(new TransferUpdate(paths.get(i), i + 1, paths.size(),
                            TransferUpdate.Status.COPIED_SCAN_REQUESTED, "scan requested", "remote"));
                }
            }

            @Override public void retry(String adb, String serial, List<TransferUpdate> previous,
                                        Consumer<TransferUpdate> observer) {
                fail("No retries expected");
            }
        };
        TransferCoordinator coordinator = new TransferCoordinator(backend, scheduled::add);
        assertEquals(WatchTransferService.BatchMode.ORDERED,
                coordinator.batchMode(TransferCoordinator.Owner.DESKTOP));
        assertEquals(WatchTransferService.BatchMode.ORDERED,
                coordinator.batchMode(TransferCoordinator.Owner.WEB));

        coordinator.selectWatch(ADB, "watch-A");
        coordinator.setBatchMode(TransferCoordinator.Owner.DESKTOP, WatchTransferService.BatchMode.FAST);
        var lease = coordinator.selection();
        coordinator.send(TransferCoordinator.Owner.DESKTOP, lease, List.of(FIRST), ignored -> {});
        assertThrows(IllegalStateException.class, () -> coordinator.setBatchMode(
                TransferCoordinator.Owner.DESKTOP, WatchTransferService.BatchMode.ORDERED));
        assertEquals(WatchTransferService.BatchMode.ORDERED,
                coordinator.batchMode(TransferCoordinator.Owner.WEB));
        scheduled.removeFirst().run();
        assertEquals(List.of(WatchTransferService.BatchMode.FAST), executedModes);

        coordinator.setBatchMode(TransferCoordinator.Owner.DESKTOP, WatchTransferService.BatchMode.ORDERED);
        coordinator.send(TransferCoordinator.Owner.DESKTOP, lease, List.of(SECOND), ignored -> {});
        scheduled.removeFirst().run();
        assertEquals(List.of(WatchTransferService.BatchMode.FAST,
                WatchTransferService.BatchMode.ORDERED), executedModes);
        assertEquals(WatchTransferService.BatchMode.ORDERED,
                coordinator.batchMode(TransferCoordinator.Owner.WEB));
    }
}
