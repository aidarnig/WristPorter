package io.github.aidarnig.wgs.adb;

public record WatchDevice(String serial, String state) {
    public boolean ready() { return "device".equals(state); }
}
