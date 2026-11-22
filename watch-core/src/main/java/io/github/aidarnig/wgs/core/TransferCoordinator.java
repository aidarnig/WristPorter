package io.github.aidarnig.wgs.core;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * One Windows-process owner for the selected watch and all Desktop/Web photo transfers.
 * Never chooses an ADB device implicitly. No JavaFX or HTTP dependencies.
 *
 * The supplied executor must run submitted work off the UI thread. Observers are called
 * on that worker thread; UI adapters must dispatch to their own UI event loop.
 * Web adapters must map events to coarse states: paths, ADB errors and serials are private.
 */
public final class TransferCoordinator {
    public enum Owner { DESKTOP, WEB }
    public enum Phase { STARTED, PROGRESS, COMPLETED, FAILED, CANCELLED }

    /** An immutable watch+ADB snapshot, invalidated on every selection change. */
    public record WatchLease(long generation, String adb, String serial) {
        public WatchLease {
            if (adb == null || adb.isBlank() || serial == null || serial.isBlank())
                throw new IllegalArgumentException("Explicit watch and ADB executable required");
        }
    }

    /** Immutable progress; the failure cause is INTERNAL and must never be returned to browsers. */
    public record Event(long jobId, Owner owner, Phase phase, int completed, int total,
                        int errors, TransferUpdate update, Throwable failure) { }

    /** Seam for fake-ADB tests. Production delegates without changing scan-only retry semantics. */
    public interface TransferBackend {
        void send(String adb, String serial, List<Path> files, Consumer<TransferUpdate> observer)
                throws Exception;
        void retry(String adb, String serial, List<TransferUpdate> failures,
                   Consumer<TransferUpdate> observer) throws Exception;
    }

    private static final class Session {
        List<Path> queue = List.of();
        final Map<Path, TransferUpdate> results = new LinkedHashMap<>();
        WatchLease previousLease;
        boolean resumable;
    }

    private static final class Job {
        final long id;
        final Owner owner;
        final WatchLease lease;
        final List<Path> files;
        final List<TransferUpdate> retries;
        final int total;
        final Consumer<Event> observer;
        volatile boolean cancelRequested;
        volatile Thread runner;
        int completed;
        int errors;

        Job(long id, Owner owner, WatchLease lease, List<Path> files,
            List<TransferUpdate> retries, Consumer<Event> observer) {
            this.id = id;
            this.owner = owner;
            this.lease = lease;
            this.files = files;
            this.retries = retries;
            this.total = retries == null ? files.size() : retries.size();
            this.observer = observer;
        }
    }

    private final TransferBackend backend;
    private final Executor executor;
    private final Map<Owner, Session> sessions = new EnumMap<>(Owner.class);
    private long nextGeneration;
    private long nextJobId;
    private WatchLease selected;
    private Job active;

    public TransferCoordinator(WatchTransferService service, Executor executor) {
        this(new TransferBackend() {
            @Override public void send(String adb, String serial, List<Path> files,
                                       Consumer<TransferUpdate> observer) throws Exception {
                service.send(adb, serial, files, observer);
            }
            @Override public void retry(String adb, String serial, List<TransferUpdate> failures,
                                        Consumer<TransferUpdate> observer) throws Exception {
                service.retry(adb, serial, failures, observer);
            }
        }, executor);
        Objects.requireNonNull(service, "service");
    }

    public TransferCoordinator(TransferBackend backend, Executor executor) {
        this.backend = Objects.requireNonNull(backend, "backend");
        this.executor = Objects.requireNonNull(executor, "executor");
        for (Owner owner : Owner.values()) sessions.put(owner, new Session());
    }

    /**
     * Only the Windows UI/controller may call this. A change revokes old leases and
     * requests interruption, but does not release the active slot until ADB really exits.
     */
    public synchronized void selectWatch(String adb, String serial) {
        String executable = adb == null ? "" : adb.strip();
        String device = serial == null ? "" : serial.strip();
        if (selected != null && selected.adb().equals(executable)
                && selected.serial().equals(device)) return;
        selected = device.isEmpty() || executable.isEmpty()
                ? null : new WatchLease(++nextGeneration, executable, device);
        if (selected == null) ++nextGeneration;
        if (active != null) interrupt(active);
    }

    public synchronized WatchLease selection() { return selected; }
    public synchronized boolean isBusy() { return active != null; }

    /** The coordinator owns the logical queue; the Desktop gallery is just its view. */
    public synchronized void replaceQueue(Owner owner, List<Path> items) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(items, "items");
        if (active != null && active.owner == owner)
            throw new IllegalStateException("Cannot modify the active transfer queue");
        Session session = sessions.get(owner);
        List<Path> replacement = List.copyOf(items);
        if (!session.queue.equals(replacement)) session.resumable = false;
        session.queue = replacement;
        session.results.keySet().retainAll(session.queue);
    }

    public synchronized List<Path> queue(Owner owner) {
        return sessions.get(Objects.requireNonNull(owner)).queue;
    }

    public synchronized Map<Path, TransferUpdate> outcomes(Owner owner) {
        return java.util.Collections.unmodifiableMap(
                new LinkedHashMap<>(sessions.get(Objects.requireNonNull(owner)).results));
    }

    public synchronized boolean canRetry(Owner owner) {
        Session session = sessions.get(Objects.requireNonNull(owner));
        return selected != null && selected.equals(session.previousLease)
                && session.queue.stream().anyMatch(path -> {
                    TransferUpdate update = session.results.get(path);
                    return update != null && update.retryable();
                });
    }

    /**
     * A cancelled/failed batch can resume on its ORIGINAL watch lease only.
     * Successfully indexed files are skipped. Scan failures preserve remoteName for
     * the unchanged scan-only retry; unreported/incomplete items retry their copy.
     * The in-flight ADB push interrupted before reporting may be ambiguous.
     */
    public synchronized boolean canResume(Owner owner) {
        Session session = sessions.get(Objects.requireNonNull(owner));
        return active == null && session.resumable && selected != null
                && selected.equals(session.previousLease) && !resumePending(session).isEmpty();
    }

    public synchronized int resumeCount(Owner owner) {
        if (!canResume(owner)) return 0;
        return resumePending(sessions.get(owner)).size();
    }

    public synchronized long resume(Owner owner, WatchLease lease, Consumer<Event> observer) {
        requireAdmission(owner, lease, observer);
        if (!canResume(owner)) throw new IllegalStateException("No interrupted batch for selected watch");
        Session session = sessions.get(owner);
        List<TransferUpdate> pending = resumePending(session);
        session.resumable = false;
        return schedule(new Job(++nextJobId, owner, lease, List.of(), pending, observer));
    }

    private static List<TransferUpdate> resumePending(Session session) {
        List<TransferUpdate> pending = new ArrayList<>();
        for (Path path : session.queue) {
            TransferUpdate previous = session.results.get(path);
            if (previous != null && previous.status() == TransferUpdate.Status.COPIED_SCAN_REQUESTED)
                continue;
            if (previous != null && previous.retryable()) {
                pending.add(previous);
            } else {
                // No completion acknowledgement: push may have been interrupted.
                // Re-send only this unresolved path; never assume success.
                pending.add(new TransferUpdate(path, pending.size() + 1, session.queue.size(),
                        TransferUpdate.Status.FAILED, "No acknowledged result before interruption"));
            }
        }
        return List.copyOf(pending);
    }

    /**
     * Reject a busy slot rather than run Desktop and Web ADB commands concurrently.
     * The caller must hold a current, explicitly issued watch lease.
     */
    public synchronized long send(Owner owner, WatchLease lease, List<Path> files,
                                  Consumer<Event> observer) {
        requireAdmission(owner, lease, observer);
        List<Path> snapshot = List.copyOf(Objects.requireNonNull(files, "files"));
        if (snapshot.isEmpty()) throw new IllegalArgumentException("Empty transfer");
        Session session = sessions.get(owner);
        session.queue = snapshot;
        session.results.clear();
        session.previousLease = lease;
        session.resumable = false;
        return schedule(new Job(++nextJobId, owner, lease, snapshot, null, observer));
    }

    public synchronized long retry(Owner owner, WatchLease lease, Consumer<Event> observer) {
        requireAdmission(owner, lease, observer);
        if (!canRetry(owner)) throw new IllegalStateException("No failures for selected watch");
        Session session = sessions.get(owner);
        List<TransferUpdate> pending = new ArrayList<>();
        for (Path path : session.queue) {
            TransferUpdate result = session.results.get(path);
            if (result != null && result.retryable()) pending.add(result);
        }
        session.resumable = false;
        return schedule(new Job(++nextJobId, owner, lease, List.of(), List.copyOf(pending), observer));
    }

    /** Requests cancellation; busy remains true until the worker has actually exited. */
    public synchronized boolean cancel(Owner owner) {
        if (active == null || active.owner != owner) return false;
        interrupt(active);
        return true;
    }

    private void requireAdmission(Owner owner, WatchLease lease, Consumer<Event> observer) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(observer, "observer");
        if (selected == null || !selected.equals(lease))
            throw new IllegalStateException("Selected watch lease is no longer valid");
        if (active != null) throw new IllegalStateException("Another transfer owns the ADB queue");
    }

    private long schedule(Job job) {
        active = job;
        try {
            executor.execute(() -> run(job));
        } catch (RuntimeException exception) {
            active = null;
            throw exception;
        }
        return job.id;
    }

    private void run(Job job) {
        job.runner = Thread.currentThread();
        Phase terminal = Phase.COMPLETED;
        Throwable failure = null;
        try {
            if (job.cancelRequested || !leaseIsCurrent(job.lease))
                throw new InterruptedException("Watch selection changed before transfer");
            emit(job, Phase.STARTED, null, null);
            Consumer<TransferUpdate> onProgress = update -> {
                Event event;
                synchronized (TransferCoordinator.this) {
                    // Preserve the remote name for a later scan-only retry.
                    sessions.get(job.owner).results.put(update.source(), update);
                    job.completed++;
                    if (update.status() != TransferUpdate.Status.COPIED_SCAN_REQUESTED) job.errors++;
                    event = event(job, Phase.PROGRESS, update, null);
                }
                deliver(job.observer, event);
            };
            if (job.retries != null)
                backend.retry(job.lease.adb(), job.lease.serial(), job.retries, onProgress);
            else backend.send(job.lease.adb(), job.lease.serial(), job.files, onProgress);
            if (job.cancelRequested || !leaseIsCurrent(job.lease))
                terminal = Phase.CANCELLED;
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            terminal = Phase.CANCELLED;
        } catch (Exception exception) {
            terminal = job.cancelRequested ? Phase.CANCELLED : Phase.FAILED;
            if (terminal == Phase.FAILED) failure = exception;
        } finally {
            synchronized (this) {
                if (active == job) {
                    active = null;
                    sessions.get(job.owner).resumable =
                            terminal == Phase.CANCELLED || terminal == Phase.FAILED;
                }
            }
            emit(job, terminal, null, failure);
            job.runner = null;
        }
    }

    private synchronized boolean leaseIsCurrent(WatchLease lease) {
        return lease.equals(selected);
    }

    private static void interrupt(Job job) {
        job.cancelRequested = true;
        Thread runner = job.runner;
        if (runner != null) runner.interrupt();
    }

    private synchronized Event event(Job job, Phase phase, TransferUpdate update, Throwable cause) {
        return new Event(job.id, job.owner, phase, job.completed, job.total,
                job.errors, update, cause);
    }

    private void emit(Job job, Phase phase, TransferUpdate update, Throwable cause) {
        deliver(job.observer, event(job, phase, update, cause));
    }

    private static void deliver(Consumer<Event> observer, Event event) {
        try {
            observer.accept(event);
        } catch (RuntimeException ignored) {
            // A broken UI/browser observer must never disrupt the actual ADB result.
        }
    }
}
