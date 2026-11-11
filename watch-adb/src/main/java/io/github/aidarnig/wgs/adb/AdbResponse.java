package io.github.aidarnig.wgs.adb;

public record AdbResponse(int exitCode, String output) {
    public boolean succeeded() { return exitCode == 0; }
}
