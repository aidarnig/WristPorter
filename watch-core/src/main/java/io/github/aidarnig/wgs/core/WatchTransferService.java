package io.github.aidarnig.wgs.core;

import io.github.aidarnig.wgs.adb.AdbClient;
import io.github.aidarnig.wgs.adb.WatchDevice;
import io.github.aidarnig.wgs.domain.PhotoFiles;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

@Service
public class WatchTransferService {
    public List<WatchDevice> devices(String adb) throws IOException, InterruptedException {
        return new AdbClient(adb).devices();
    }

    public void connect(String adb, String endpoint) throws IOException, InterruptedException {
        var client = new AdbClient(adb);
        client.connect(endpoint);
        client.assertConnected(endpoint);
    }

    public void pair(String adb, String endpoint, String pairingCode) throws IOException, InterruptedException {
        new AdbClient(adb).pair(endpoint, pairingCode);
    }

    /** Copies each file once, and requests a separate media scan after every successful copy. */
    public void send(String adb, String serial, List<Path> files, Consumer<TransferUpdate> callback)
            throws IOException, InterruptedException {
        Objects.requireNonNull(files, "files");
        Objects.requireNonNull(callback, "callback");
        if (files.isEmpty()) return;

        AdbClient client = connectedClient(adb, serial);
        client.ensureFolder(serial);
        for (int i = 0; i < files.size(); i++) {
            checkInterrupted();
            callback.accept(copyAndScan(client, serial, files.get(i), i + 1, files.size()));
        }
    }

    /**
     * Retry only prior failures. A file that copied but could not be indexed receives
     * a scan-only retry using the original remoteName; it is never pushed twice.
     * The caller must ensure that serial matches the watch used for the original attempt.
     */
    public void retry(String adb, String serial, List<TransferUpdate> previous,
                      Consumer<TransferUpdate> callback) throws IOException, InterruptedException {
        Objects.requireNonNull(previous, "previous");
        Objects.requireNonNull(callback, "callback");
        List<TransferUpdate> pending = previous.stream().filter(TransferUpdate::retryable).toList();
        if (pending.isEmpty()) return;

        AdbClient client = connectedClient(adb, serial);
        if (pending.stream().anyMatch(item -> item.status() == TransferUpdate.Status.FAILED)) {
            client.ensureFolder(serial);
        }

        for (int i = 0; i < pending.size(); i++) {
            checkInterrupted();
            TransferUpdate old = pending.get(i);
            if (old.status() == TransferUpdate.Status.FAILED) {
                callback.accept(copyAndScan(client, serial, old.source(), i + 1, pending.size()));
            } else {
                callback.accept(scan(client, serial, old.source(), old.remoteName(), i + 1, pending.size()));
            }
        }
    }

    private static AdbClient connectedClient(String adb, String serial) throws IOException, InterruptedException {
        AdbClient client = new AdbClient(adb);
        client.assertConnected(serial);
        return client;
    }

    private static TransferUpdate copyAndScan(AdbClient client, String serial, Path source,
                                               int position, int total) throws InterruptedException {
        String remote;
        try {
            PhotoFiles.requireReadable(source);
            remote = client.push(source, serial);
        } catch (IllegalArgumentException | IOException exception) {
            return new TransferUpdate(source, position, total, TransferUpdate.Status.FAILED,
                    exception.getMessage());
        }
        return scan(client, serial, source, remote, position, total);
    }

    private static TransferUpdate scan(AdbClient client, String serial, Path source, String remoteName,
                                        int position, int total) throws InterruptedException {
        try {
            client.scan(serial, remoteName);
            return new TransferUpdate(source, position, total,
                    TransferUpdate.Status.COPIED_SCAN_REQUESTED, "Media scan requested", remoteName);
        } catch (IOException exception) {
            return new TransferUpdate(source, position, total,
                    TransferUpdate.Status.COPIED_SCAN_FAILED, exception.getMessage(), remoteName);
        }
    }

    private static void checkInterrupted() throws InterruptedException {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Transfer interrupted");
    }
}
