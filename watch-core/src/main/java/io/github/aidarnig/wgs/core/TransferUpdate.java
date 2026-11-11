package io.github.aidarnig.wgs.core;

import java.nio.file.Path;

public record TransferUpdate(Path source, int position, int total, Status status, String message) {
    public enum Status { COPIED_SCAN_REQUESTED, COPIED_SCAN_FAILED, FAILED }
}
