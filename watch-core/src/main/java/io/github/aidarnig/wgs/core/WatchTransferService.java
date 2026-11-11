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
    /** Each file receives a separate result; one failed file does not abort the batch. */
    public void send(String adb, String serial, List<Path> files, Consumer<TransferUpdate> callback)
            throws IOException, InterruptedException {
        Objects.requireNonNull(files, "files");
        Objects.requireNonNull(callback, "callback");
        if (files.isEmpty()) return;
        AdbClient client = new AdbClient(adb);
        client.assertConnected(serial);
        client.ensureFolder(serial);
        for (int i = 0; i < files.size(); i++) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedException("Transfer interrupted");
            Path file = files.get(i);
            String name;
            try {
                PhotoFiles.requireReadable(file);
                name = client.push(file, serial);
            } catch (IllegalArgumentException | IOException ex) {
                callback.accept(new TransferUpdate(file, i + 1, files.size(), TransferUpdate.Status.FAILED, ex.getMessage()));
                continue;
            }
            try {
                client.scan(serial, name);
                callback.accept(new TransferUpdate(file, i + 1, files.size(), TransferUpdate.Status.COPIED_SCAN_REQUESTED, "Scan requested"));
            } catch (IOException ex) {
                callback.accept(new TransferUpdate(file, i + 1, files.size(), TransferUpdate.Status.COPIED_SCAN_FAILED, ex.getMessage()));
            }
        }
    }
}
