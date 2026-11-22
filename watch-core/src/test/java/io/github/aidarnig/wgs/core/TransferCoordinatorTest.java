package io.github.aidarnig.wgs.core;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class TransferCoordinatorTest {
    private final ExecutorService worker = Executors.newSingleThreadExecutor(
            Thread.ofVirtual().factory());

    @AfterEach void cleanup() throws Exception {
        worker.shutdownNow();
        assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
    }

    private static final String ADB = "fake-adb";
    private static final Path FIRST = Path.of("one.png");
    private static final Path SECOND = Path.of("two.jpg");

    @Test void transfersOnlyToExplicitlySelectedWatchAndPublishesImmutableWorkerEvents()
            throws Exception {
        AtomicReference<String> observedTarget = new AtomicReference<>();
        AtomicReference<Thread> callbackThread = new AtomicReference<>();
        TransferCoordinator coordinator = new TransferCoordinator(new FakeBackend() {
            @Override public void send(String adb, String serial, List<Path> files,
                                       Consumer<TransferUpdate> observer) {
                observedTarget.set(adb + "|" + serial);
                for (int i = 0; i < files.size(); i++)
                    observer.accept(new TransferUpdate(files.get(i), i + 1, files.size(),
                            TransferUpdate.Status.COPIED_SCAN_REQUESTED, "scan requested", "remote"));
            }
        }, worker);
        coordinator.selectWatch(ADB, "watch-A");
        coordinator.replaceQueue(TransferCoordinator.Owner.DESKTOP, List.of(FIRST, SECOND));
        TransferCoordinator.WatchLease lease = coordinator.selection();
        BlockingQueue<TransferCoordinator.Event> events = new LinkedBlockingQueue<>();
        coordinator.send(TransferCoordinator.Owner.DESKTOP, lease, coordinator.queue(
                TransferCoordinator.Owner.DESKTOP), event -> {
            callbackThread.set(Thread.currentThread());
            events.add(event);
        });
        assertEquals(TransferCoordinator.Phase.STARTED, take(events).phase());
        TransferCoordinator.Event first = take(events);
        assertEquals(TransferCoordinator.Phase.PROGRESS, first.phase());
        assertEquals(1, first.completed());
        assertEquals(2, first.total());
        assertEquals(0, first.errors());
        assertEquals(TransferCoordinator.Phase.PROGRESS, take(events).phase());
        assertEquals(TransferCoordinator.Phase.COMPLETED, take(events).phase());
        assertEquals("fake-adb|watch-A", observedTarget.get());
        assertNotEquals(Thread.currentThread(), callbackThread.get());
        Map<Path, TransferUpdate> outcomes = coordinator.outcomes(TransferCoordinator.Owner.DESKTOP);
        assertEquals(2, outcomes.size());
        assertThrows(UnsupportedOperationException.class,
                () -> outcomes.clear());
        assertFalse(coordinator.canRetry(TransferCoordinator.Owner.DESKTOP));
    }

    @Test void preservesScanOnlyRetryPayloadAndDoesNotPushAgain() throws Exception {
        String remote = "wgs-123e4567-e89b-12d3-a456-426614174000.png";
        AtomicInteger pushes = new AtomicInteger();
        AtomicInteger scans = new AtomicInteger();
        TransferCoordinator coordinator = new TransferCoordinator(new FakeBackend() {
            @Override public void send(String adb, String serial, List<Path> photos,
                                       Consumer<TransferUpdate> observer) {
                pushes.incrementAndGet();
                observer.accept(new TransferUpdate(FIRST, 1, 1,
                        TransferUpdate.Status.COPIED_SCAN_FAILED, "scan failed", remote));
            }
            @Override public void retry(String adb, String serial, List<TransferUpdate> previous,
                                        Consumer<TransferUpdate> observer) {
                assertEquals(1, previous.size());
                assertEquals(TransferUpdate.Status.COPIED_SCAN_FAILED, previous.getFirst().status());
                assertEquals(remote, previous.getFirst().remoteName());
                assertEquals("watch-A", serial);
                scans.incrementAndGet();
                observer.accept(new TransferUpdate(FIRST, 1, 1,
                        TransferUpdate.Status.COPIED_SCAN_REQUESTED, "scan requested", remote));
            }
        }, worker);
        coordinator.selectWatch(ADB, "watch-A");
        var lease = coordinator.selection();
        BlockingQueue<TransferCoordinator.Event> events = new LinkedBlockingQueue<>();
        coordinator.send(TransferCoordinator.Owner.DESKTOP, lease, List.of(FIRST), events::add);
        assertEquals(TransferCoordinator.Phase.COMPLETED, terminal(events).phase());
        assertTrue(coordinator.canRetry(TransferCoordinator.Owner.DESKTOP));
        assertEquals(remote, coordinator.outcomes(TransferCoordinator.Owner.DESKTOP)
                .get(FIRST).remoteName());
        coordinator.retry(TransferCoordinator.Owner.DESKTOP, lease, events::add);
        assertEquals(TransferCoordinator.Phase.COMPLETED, terminal(events).phase());
        assertEquals(1, pushes.get());
        assertEquals(1, scans.get());
        assertFalse(coordinator.canRetry(TransferCoordinator.Owner.DESKTOP));
    }

    @Test void cancellationKeepsQueueExclusiveUntilBackendExits() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch permitExit = new CountDownLatch(1);
        TransferCoordinator coordinator = new TransferCoordinator(new FakeBackend() {
            @Override public void send(String adb, String serial, List<Path> paths,
                                       Consumer<TransferUpdate> observer) {
                entered.countDown();
                while (permitExit.getCount() > 0) {
                    try {
                        permitExit.await(50, TimeUnit.MILLISECONDS);
                    } catch (InterruptedException ignored) {
                        // Emulate a slow ADB termination after cancellation.
                    }
                }
            }
        }, worker);
        coordinator.selectWatch(ADB, "watch-A");
        var lease = coordinator.selection();
        BlockingQueue<TransferCoordinator.Event> events = new LinkedBlockingQueue<>();
        coordinator.send(TransferCoordinator.Owner.DESKTOP, lease, List.of(FIRST), events::add);
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        assertFalse(coordinator.cancel(TransferCoordinator.Owner.WEB));
        assertTrue(coordinator.cancel(TransferCoordinator.Owner.DESKTOP));
        assertTrue(coordinator.isBusy(), "Cancelling is not the same as worker termination");
        assertThrows(IllegalStateException.class, () -> coordinator.send(
                TransferCoordinator.Owner.WEB, lease, List.of(SECOND), events::add));
        assertThrows(IllegalStateException.class, () -> coordinator.replaceQueue(
                TransferCoordinator.Owner.DESKTOP, List.of(SECOND)));
        permitExit.countDown();
        assertEquals(TransferCoordinator.Phase.CANCELLED, terminal(events).phase());
        assertFalse(coordinator.isBusy());
    }

    @Test void watchSwitchRevokesQueuedJobBeforeAnyAdbCommand() throws Exception {
        CountDownLatch beforeJob = new CountDownLatch(1);
        CountDownLatch unblock = new CountDownLatch(1);
        worker.execute(() -> {
            beforeJob.countDown();
            try { unblock.await(); } catch (InterruptedException ignored) { }
        });
        assertTrue(beforeJob.await(5, TimeUnit.SECONDS));
        AtomicInteger calls = new AtomicInteger();
        TransferCoordinator coordinator = new TransferCoordinator(new FakeBackend() {
            @Override public void send(String adb, String serial, List<Path> photos,
                                       Consumer<TransferUpdate> observer) {
                calls.incrementAndGet();
            }
        }, worker);
        coordinator.selectWatch(ADB, "watch-A");
        var oldLease = coordinator.selection();
        BlockingQueue<TransferCoordinator.Event> events = new LinkedBlockingQueue<>();
        coordinator.send(TransferCoordinator.Owner.DESKTOP, oldLease, List.of(FIRST), events::add);
        coordinator.selectWatch(ADB, "watch-B");
        assertTrue(coordinator.isBusy());
        assertThrows(IllegalStateException.class, () -> coordinator.send(
                TransferCoordinator.Owner.WEB, oldLease, List.of(FIRST), events::add));
        unblock.countDown();
        assertEquals(TransferCoordinator.Phase.CANCELLED, terminal(events).phase());
        assertEquals(0, calls.get());
        assertEquals("watch-B", coordinator.selection().serial());
    }

    @Test void connectionFailureReleasesSlotWithoutImplicitFallback() throws Exception {
        TransferCoordinator coordinator = new TransferCoordinator(new FakeBackend() {
            @Override public void send(String adb, String serial, List<Path> photos,
                                       Consumer<TransferUpdate> observer) throws Exception {
                assertEquals("watch-offline", serial);
                throw new IOException("Selected watch offline");
            }
        }, worker);
        coordinator.selectWatch(ADB, "watch-offline");
        BlockingQueue<TransferCoordinator.Event> events = new LinkedBlockingQueue<>();
        coordinator.send(TransferCoordinator.Owner.WEB, coordinator.selection(),
                List.of(FIRST), events::add);
        TransferCoordinator.Event finished = terminal(events);
        assertEquals(TransferCoordinator.Phase.FAILED, finished.phase());
        assertInstanceOf(IOException.class, finished.failure());
        assertFalse(coordinator.isBusy());
        assertTrue(coordinator.outcomes(TransferCoordinator.Owner.WEB).isEmpty());
    }

    @Test void retryCannotTargetADifferentWatchOrAdbExecutable() throws Exception {
        TransferCoordinator coordinator = new TransferCoordinator(new FakeBackend() {
            @Override public void send(String adb, String serial, List<Path> paths,
                                       Consumer<TransferUpdate> observer) {
                observer.accept(new TransferUpdate(FIRST, 1, 1,
                        TransferUpdate.Status.FAILED, "copy failed"));
            }
        }, worker);
        coordinator.selectWatch(ADB, "watch-A");
        var oldLease = coordinator.selection();
        BlockingQueue<TransferCoordinator.Event> events = new LinkedBlockingQueue<>();
        coordinator.send(TransferCoordinator.Owner.DESKTOP, oldLease, List.of(FIRST), events::add);
        assertEquals(TransferCoordinator.Phase.COMPLETED, terminal(events).phase());
        assertTrue(coordinator.canRetry(TransferCoordinator.Owner.DESKTOP));
        coordinator.selectWatch("other-adb", "watch-A");
        assertFalse(coordinator.canRetry(TransferCoordinator.Owner.DESKTOP));
        assertThrows(IllegalStateException.class, () -> coordinator.retry(
                TransferCoordinator.Owner.DESKTOP, oldLease, events::add));
        coordinator.selectWatch(ADB, "watch-A");
        assertFalse(coordinator.canRetry(TransferCoordinator.Owner.DESKTOP),
                "A revoked lease must not revive when returning to an old device");
    }

    private static TransferCoordinator.Event take(BlockingQueue<TransferCoordinator.Event> queue)
            throws Exception {
        var event = queue.poll(5, TimeUnit.SECONDS);
        assertNotNull(event, "Expected coordinator event");
        return event;
    }

    private static TransferCoordinator.Event terminal(BlockingQueue<TransferCoordinator.Event> events)
            throws Exception {
        for (int i = 0; i < 10; i++) {
            var event = take(events);
            if (event.phase() == TransferCoordinator.Phase.COMPLETED
                    || event.phase() == TransferCoordinator.Phase.FAILED
                    || event.phase() == TransferCoordinator.Phase.CANCELLED) return event;
        }
        fail("No terminal event");
        return null;
    }

    private static class FakeBackend implements TransferCoordinator.TransferBackend {
        @Override public void send(String adb, String serial, List<Path> paths,
                                   Consumer<TransferUpdate> observer) throws Exception { }
        @Override public void retry(String adb, String serial, List<TransferUpdate> failures,
                                    Consumer<TransferUpdate> observer) throws Exception { }
    }
}
