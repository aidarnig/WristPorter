package io.github.aidarnig.wgs.adb;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AdbDeviceParserTest {
    @Test void parsesDeviceStates() {
        String output = """
            List of devices attached
            192.168.1.101:38267 device product:watch
            R123 unauthorized
            192.168.1.99:4444 offline
            """;
        List<WatchDevice> items = AdbDeviceParser.parse(output);
        assertEquals(3, items.size());
        assertTrue(items.get(0).ready());
        assertFalse(items.get(1).ready());
        assertEquals("offline", items.get(2).state());
    }
    @Test void ignoresDaemonNoise() {
        assertTrue(AdbDeviceParser.parse("* daemon started\nList of devices attached\n").isEmpty());
    }
}
