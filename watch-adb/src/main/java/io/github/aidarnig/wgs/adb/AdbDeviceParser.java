package io.github.aidarnig.wgs.adb;

import java.util.ArrayList;
import java.util.List;

public final class AdbDeviceParser {
    private AdbDeviceParser() {}

    public static List<WatchDevice> parse(String output) {
        if (output == null) return List.of();
        List<WatchDevice> devices = new ArrayList<>();
        output.lines().map(String::strip).filter(line -> !line.isEmpty())
                .filter(line -> !line.startsWith("*") && !line.startsWith("List of devices attached"))
                .forEach(line -> {
                    String[] parts = line.split("\\s+", 3);
                    if (parts.length >= 2) devices.add(new WatchDevice(parts[0], parts[1]));
                });
        return List.copyOf(devices);
    }
}
