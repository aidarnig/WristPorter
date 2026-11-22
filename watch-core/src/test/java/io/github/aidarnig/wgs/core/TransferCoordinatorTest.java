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

    @Test void resumeSkipsAcknowledgedCopiesButRetriesScanFailureAndUnreportedItems()
            throws Exception {
        Path third = Path.of("three.png");
        Path fourth = Path.of("four.jpg");
        String remote = "wgs-123e4567-e89b-12d3-a456-426614174000.jpg";
        CountDownLatch firstTwoFinished = new CountDownLatch(1);
        AtomicInteger initialSendCalls = new AtomicInteger();
        AtomicInteger resumedCalls = new AtomicInteger();
        TransferCoordinator coordinator = new TransferCoordinator(new FakeBackend() {
            @Override public void send(String adb, String serial, List<Path> photos,
                                       Consumer<TransferUpdate> observer) throws Exception {
                initialSendCalls.incrementAndGet();
                assertEquals(List.of(FIRST, SECOND, third, fourth), photos);
                observer.accept(new TransferUpdate(FIRST, 1, 4,
                        TransferUpdate.Status.COPIED_SCAN_REQUESTED, "copied", "remote-first.png"));
                observer.accept(new TransferUpdate(SECOND, 2, 4,
                        TransferUpdate.Status.COPIED_SCAN_FAILED, "needs scan", remote));
                firstTwoFinished.countDown();
                new CountDownLatch(1).await(); // Cancellable while a later file is pending.
            }
            @Override public void retry(String adb, String serial, List<TransferUpdate> pending,
                                        Consumer<TransferUpdate> observer) {
                resumedCalls.incrementAndGet();
                assertEquals("watch-A", serial);
                assertEquals(List.of(SECOND, third, fourth),
                        pending.stream().map(TransferUpdate::source).toList());
                assertEquals(TransferUpdate.Status.COPIED_SCAN_FAILED, pending.getFirst().status());
                assertEquals(remote, pending.getFirst().remoteName());
                assertEquals(TransferUpdate.Status.FAILED, pending.get(1).status());
                assertEquals(TransferUpdate.Status.FAILED, pending.get(2).status());
                for (int i = 0; i < pending.size(); i++) {
                    Path path = pending.get(i).source();
                    observer.accept(new TransferUpdate(path, i + 1, pending.size(),
                            TransferUpdate.Status.COPIED_SCAN_REQUESTED, "done",
                            path.equals(SECOND) ? remote : "new-copy"));
                }
            }
        }, worker);
        coordinator.selectWatch(ADB, "watch-A");
        var lease = coordinator.selection();
        BlockingQueue<TransferCoordinator.Event> events = new LinkedBlockingQueue<>();
        coordinator.send(TransferCoordinator.Owner.DESKTOP, lease,
                List.of(FIRST, SECOND, third, fourth), events::add);
        assertTrue(firstTwoFinished.await(5, TimeUnit.SECONDS));
        assertTrue(coordinator.cancel(TransferCoordinator.Owner.DESKTOP));
        assertEquals(TransferCoordinator.Phase.CANCELLED, terminal(events).phase());
        assertTrue(coordinator.canResume(TransferCoordinator.Owner.DESKTOP));
        assertEquals(3, coordinator.resumeCount(TransferCoordinator.Owner.DESKTOP));
        assertEquals(TransferUpdate.Status.COPIED_SCAN_REQUESTED,
                coordinator.outcomes(TransferCoordinator.Owner.DESKTOP).get(FIRST).status());
        coordinator.resume(TransferCoordinator.Owner.DESKTOP, lease, events::add);
        assertEquals(TransferCoordinator.Phase.COMPLETED, terminal(events).phase());
        assertEquals(1, initialSendCalls.get(), "Acknowledged first copy cannot be pushed again");
        assertEquals(1, resumedCalls.get());
        assertEquals(4, coordinator.outcomes(TransferCoordinator.Owner.DESKTOP).size());
        assertFalse(coordinator.canResume(TransferCoordinator.Owner.DESKTOP));
    }

    @Test void changingWatchOrQueueDisablesInterruptedBatchResume() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        TransferCoordinator coordinator = new TransferCoordinator(new FakeBackend() {
            @Override public void send(String adb, String serial, List<Path> paths,
                                       Consumer<TransferUpdate> observer) throws InterruptedException {
                observer.accept(new TransferUpdate(FIRST, 1, 2,
                        TransferUpdate.Status.COPIED_SCAN_REQUESTED, "done", "remote"));
                entered.countDown();
                new CountDownLatch(1).await();
            }
        }, worker);
        coordinator.selectWatch(ADB, "watch-A");
        var lease = coordinator.selection();
        BlockingQueue<TransferCoordinator.Event> events = new LinkedBlockingQueue<>();
        coordinator.send(TransferCoordinator.Owner.DESKTOP, lease, List.of(FIRST, SECOND), events::add);
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        coordinator.cancel(TransferCoordinator.Owner.DESKTOP);
        assertEquals(TransferCoordinator.Phase.CANCELLED, terminal(events).phase());
        assertTrue(coordinator.canResume(TransferCoordinator.Owner.DESKTOP));
        assertEquals(1, coordinator.resumeCount(TransferCoordinator.Owner.DESKTOP));

        coordinator.replaceQueue(TransferCoordinator.Owner.DESKTOP, List.of(SECOND));
        assertFalse(coordinator.canResume(TransferCoordinator.Owner.DESKTOP));
        assertThrows(IllegalStateException.class, () -> coordinator.resume(
                TransferCoordinator.Owner.DESKTOP, lease, events::add));

        coordinator.selectWatch(ADB, "watch-B");
        coordinator.replaceQueue(TransferCoordinator.Owner.DESKTOP, List.of(FIRST, SECOND));
        coordinator.selectWatch(ADB, "watch-A");
        assertFalse(coordinator.canResume(TransferCoordinator.Owner.DESKTOP));
        assertThrows(IllegalStateException.class, () -> coordinator.resume(
                TransferCoordinator.Owner.DESKTOP, lease, events::add));
    }

    @Test void failedPreflightCanResumeUnreportedFilesWithoutChangingWatch() throws Exception {
        AtomicInteger initialCalls = new AtomicInteger();
        AtomicInteger recovery = new AtomicInteger();
        TransferCoordinator coordinator = new TransferCoordinator(new FakeBackend() {
            @Override public void send(String adb, String serial, List<Path> paths,
                                       Consumer<TransferUpdate> observer) throws Exception {
                initialCalls.incrementAndGet();
                throw new IOException("Watch temporarily offline");
            }
            @Override public void retry(String adb, String serial, List<TransferUpdate> previous,
                                        Consumer<TransferUpdate> observer) {
                recovery.incrementAndGet();
                assertEquals(List.of(FIRST, SECOND), previous.stream()
                        .map(TransferUpdate::source).toList());
                assertTrue(previous.stream().allMatch(u -> u.status() == TransferUpdate.Status.FAILED));
                for (int i = 0; i < previous.size(); i++) {
                    observer.accept(new TransferUpdate(previous.get(i).source(), i + 1, previous.size(),
                            TransferUpdate.Status.COPIED_SCAN_REQUESTED, "done", "remote"));
                }
            }
        }, worker);
        coordinator.selectWatch(ADB, "watch-A");
        var lease = coordinator.selection();
        BlockingQueue<TransferCoordinator.Event> events = new LinkedBlockingQueue<>();
        coordinator.send(TransferCoordinator.Owner.DESKTOP, lease, List.of(FIRST, SECOND), events::add);
        assertEquals(TransferCoordinator.Phase.FAILED, terminal(events).phase());
        assertEquals(2, coordinator.resumeCount(TransferCoordinator.Owner.DESKTOP));
        coordinator.resume(TransferCoordinator.Owner.DESKTOP, lease, events::add);
        assertEquals(TransferCoordinator.Phase.COMPLETED, terminal(events).phase());
        assertEquals(1, initialCalls.get());
        assertEquals(1, recovery.get());
    }

    @Test void appendingPhotosSendsOnlyUnsentFilesOnSameWatch() throws Exception {
        List<List<Path>> batches = new java.util.concurrent.CopyOnWriteArrayList<>();
        TransferCoordinator coordinator = new TransferCoordinator(new FakeBackend() {
            @Override public void send(String adb, String serial, List<Path> photos,
                                       Consumer<TransferUpdate> observer) {
                assertEquals("watch-A", serial);
                batches.add(List.copyOf(photos));
                for (int i = 0; i < photos.size(); i++)
                    observer.accept(new TransferUpdate(photos.get(i), i + 1, photos.size(),
                            TransferUpdate.Status.COPIED_SCAN_REQUESTED, "copied", "remote"));
            }
        }, worker);
        coordinator.selectWatch(ADB, "watch-A");
        var lease = coordinator.selection();
        BlockingQueue<TransferCoordinator.Event> events = new LinkedBlockingQueue<>();

        coordinator.replaceQueue(TransferCoordinator.Owner.DESKTOP, List.of(FIRST));
        assertEquals(1, coordinator.pendingCount(TransferCoordinator.Owner.DESKTOP));
        coordinator.sendPending(TransferCoordinator.Owner.DESKTOP, lease, events::add);
        assertEquals(TransferCoordinator.Phase.COMPLETED, terminal(events).phase());
        var firstResult = coordinator.outcomes(TransferCoordinator.Owner.DESKTOP).get(FIRST);
        assertNotNull(firstResult);
        assertEquals(0, coordinator.pendingCount(TransferCoordinator.Owner.DESKTOP));

        // Original item remains selected in the Desktop queue; add 10 new images.
        List<Path> more = java.util.stream.IntStream.range(0, 10)
                .mapToObj(i -> Path.of("new-" + i + ".png")).toList();
        List<Path> combined = new java.util.ArrayList<>();
        combined.add(FIRST);
        combined.addAll(more);
        coordinator.replaceQueue(TransferCoordinator.Owner.DESKTOP, combined);
        assertEquals(10, coordinator.pendingCount(TransferCoordinator.Owner.DESKTOP));
        assertEquals(firstResult, coordinator.outcomes(TransferCoordinator.Owner.DESKTOP).get(FIRST));

        coordinator.sendPending(TransferCoordinator.Owner.DESKTOP, lease, events::add);
        TransferCoordinator.Event finished = terminal(events);
        assertEquals(TransferCoordinator.Phase.COMPLETED, finished.phase());
        assertEquals(10, finished.total());
        assertEquals(List.of(List.of(FIRST), more), batches,
                "First photo must never be re-copied as part of the second batch");
        assertEquals(11, coordinator.outcomes(TransferCoordinator.Owner.DESKTOP).size());
        assertEquals(firstResult, coordinator.outcomes(TransferCoordinator.Owner.DESKTOP).get(FIRST));
        assertEquals(0, coordinator.pendingCount(TransferCoordinator.Owner.DESKTOP));
        assertThrows(IllegalStateException.class, () -> coordinator.sendPending(
                TransferCoordinator.Owner.DESKTOP, lease, events::add));
    }

    @Test void changingWatchInvalidatesPreviousSuccessForNewDevice() throws Exception {
        List<String> calls = new java.util.concurrent.CopyOnWriteArrayList<>();
        TransferCoordinator coordinator = new TransferCoordinator(new FakeBackend() {
            @Override public void send(String adb, String serial, List<Path> photos,
                                       Consumer<TransferUpdate> observer) {
                calls.add(serial + "|" + photos);
                observer.accept(new TransferUpdate(FIRST, 1, 1,
                        TransferUpdate.Status.COPIED_SCAN_REQUESTED, "done", "remote"));
            }
        }, worker);
        coordinator.selectWatch(ADB, "watch-A");
        var original = coordinator.selection();
        BlockingQueue<TransferCoordinator.Event> events = new LinkedBlockingQueue<>();
        coordinator.replaceQueue(TransferCoordinator.Owner.DESKTOP, List.of(FIRST));
        coordinator.sendPending(TransferCoordinator.Owner.DESKTOP, original, events::add);
        assertEquals(TransferCoordinator.Phase.COMPLETED, terminal(events).phase());
        assertEquals(0, coordinator.pendingCount(TransferCoordinator.Owner.DESKTOP));

        coordinator.selectWatch(ADB, "watch-B");
        assertEquals(1, coordinator.pendingCount(TransferCoordinator.Owner.DESKTOP));
        assertThrows(IllegalStateException.class, () -> coordinator.sendPending(
                TransferCoordinator.Owner.DESKTOP, original, events::add));
        var other = coordinator.selection();
        coordinator.sendPending(TransferCoordinator.Owner.DESKTOP, other, events::add);
        assertEquals(TransferCoordinator.Phase.COMPLETED, terminal(events).phase());
        assertEquals(List.of("watch-A|[one.png]", "watch-B|[one.png]"), calls);
        assertEquals(0, coordinator.pendingCount(TransferCoordinator.Owner.DESKTOP));

        coordinator.selectWatch(ADB, "watch-A");
        assertEquals(1, coordinator.pendingCount(TransferCoordinator.Owner.DESKTOP),
                "Returning to an old device cannot revive its previous lease");
    }

    @Test void newPhotosDoNotRepushEarlierScanFailuresAndRetryRemainsScanOnly() throws Exception {
        String remote = "wgs-123e4567-e89b-12d3-a456-426614174000.png";
        AtomicInteger sends = new AtomicInteger();
        TransferCoordinator coordinator = new TransferCoordinator(new FakeBackend() {
            @Override public void send(String adb, String serial, List<Path> photos,
                                       Consumer<TransferUpdate> observer) {
                int n = sends.incrementAndGet();
                assertEquals(n == 1 ? List.of(FIRST) : List.of(SECOND), photos);
                observer.accept(new TransferUpdate(photos.getFirst(), 1, 1,
                        n == 1 ? TransferUpdate.Status.COPIED_SCAN_FAILED
                               : TransferUpdate.Status.COPIED_SCAN_REQUESTED,
                        "copied", n == 1 ? remote : "remote-second"));
            }
            @Override public void retry(String adb, String serial, List<TransferUpdate> prior,
                                        Consumer<TransferUpdate> observer) {
                assertEquals(1, prior.size());
                assertEquals(FIRST, prior.getFirst().source());
                assertEquals(TransferUpdate.Status.COPIED_SCAN_FAILED, prior.getFirst().status());
                assertEquals(remote, prior.getFirst().remoteName());
                observer.accept(new TransferUpdate(FIRST, 1, 1,
                        TransferUpdate.Status.COPIED_SCAN_REQUESTED, "rescanned", remote));
            }
        }, worker);
        coordinator.selectWatch(ADB, "watch-A");
        var lease = coordinator.selection();
        BlockingQueue<TransferCoordinator.Event> events = new LinkedBlockingQueue<>();
        coordinator.replaceQueue(TransferCoordinator.Owner.DESKTOP, List.of(FIRST));
        coordinator.sendPending(TransferCoordinator.Owner.DESKTOP, lease, events::add);
        assertEquals(TransferCoordinator.Phase.COMPLETED, terminal(events).phase());
        assertTrue(coordinator.canRetry(TransferCoordinator.Owner.DESKTOP));

        coordinator.replaceQueue(TransferCoordinator.Owner.DESKTOP, List.of(FIRST, SECOND));
        assertEquals(1, coordinator.pendingCount(TransferCoordinator.Owner.DESKTOP));
        coordinator.sendPending(TransferCoordinator.Owner.DESKTOP, lease, events::add);
        assertEquals(TransferCoordinator.Phase.COMPLETED, terminal(events).phase());
        assertEquals(2, sends.get());
        assertTrue(coordinator.canRetry(TransferCoordinator.Owner.DESKTOP));
        assertEquals(TransferUpdate.Status.COPIED_SCAN_FAILED,
                coordinator.outcomes(TransferCoordinator.Owner.DESKTOP).get(FIRST).status());
        coordinator.retry(TransferCoordinator.Owner.DESKTOP, lease, events::add);
        assertEquals(TransferCoordinator.Phase.COMPLETED, terminal(events).phase());
        assertEquals(2, sends.get(), "Scan-only retry must never invoke a second photo push");
        assertFalse(coordinator.canRetry(TransferCoordinator.Owner.DESKTOP));
    }

    @Test void removingAndReaddingPreviouslySentPhotoMakesItExplicitlyNew() throws Exception {
        AtomicInteger pushes = new AtomicInteger();
        TransferCoordinator coordinator = new TransferCoordinator(new FakeBackend() {
            @Override public void send(String adb, String serial, List<Path> photos,
                                       Consumer<TransferUpdate> observer) {
                pushes.incrementAndGet();
                observer.accept(new TransferUpdate(FIRST, 1, 1,
                        TransferUpdate.Status.COPIED_SCAN_REQUESTED, "done", "remote"));
            }
        }, worker);
        coordinator.selectWatch(ADB, "watch-A");
        var lease = coordinator.selection();
        BlockingQueue<TransferCoordinator.Event> events = new LinkedBlockingQueue<>();
        coordinator.replaceQueue(TransferCoordinator.Owner.DESKTOP, List.of(FIRST));
        coordinator.sendPending(TransferCoordinator.Owner.DESKTOP, lease, events::add);
        assertEquals(TransferCoordinator.Phase.COMPLETED, terminal(events).phase());
        coordinator.replaceQueue(TransferCoordinator.Owner.DESKTOP, List.of());
        assertTrue(coordinator.outcomes(TransferCoordinator.Owner.DESKTOP).isEmpty());
        coordinator.replaceQueue(TransferCoordinator.Owner.DESKTOP, List.of(FIRST));
        assertEquals(1, coordinator.pendingCount(TransferCoordinator.Owner.DESKTOP));
        coordinator.sendPending(TransferCoordinator.Owner.DESKTOP, lease, events::add);
        assertEquals(TransferCoordinator.Phase.COMPLETED, terminal(events).phase());
        assertEquals(2, pushes.get());
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
