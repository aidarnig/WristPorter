package io.github.aidarnig.wgs.core;

import java.nio.file.Path;

/**
 * Outcome of a single file. A non-null remoteName means the push has completed.
 * For COPIED_SCAN_FAILED it permits retrying indexing without copying again.
 */
public record TransferUpdate(
        Path source, int position, int total, Status status, String message, String remoteName
) {
    public enum Status { COPIED_SCAN_REQUESTED, COPIED_SCAN_FAILED, FAILED }

    public TransferUpdate(Path source, int position, int total, Status status, String message) {
        this(source, position, total, status, message, null);
    }

    public boolean retryable() {
        return status == Status.FAILED
                || (status == Status.COPIED_SCAN_FAILED && remoteName != null);
    }
}
