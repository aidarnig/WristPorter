package io.github.aidarnig.wgs.adb;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WatchEndpointTest {
    @Test void acceptsWatchAddresses() {
        var parsed = WatchEndpoint.parse("192.168.1.101:38267");
        assertEquals("192.168.1.101", parsed.host());
        assertEquals(38267, parsed.port());
        assertEquals("192.168.1.101:38267", parsed.toString());
    }

    @Test void rejectsMalformedOrDangerousEndpoints() {
        assertThrows(IllegalArgumentException.class, () -> WatchEndpoint.parse(""));
        assertThrows(IllegalArgumentException.class, () -> WatchEndpoint.parse("192.168.1.101"));
        assertThrows(IllegalArgumentException.class, () -> WatchEndpoint.parse("192.168.1.101:0"));
        assertThrows(IllegalArgumentException.class, () -> WatchEndpoint.parse("192.168.1.101:70000"));
        assertThrows(IllegalArgumentException.class, () -> WatchEndpoint.parse("192.168.1.101:8000; rm -rf /"));
        assertThrows(IllegalArgumentException.class, () -> WatchEndpoint.parse("bad..host:3333"));
        assertThrows(IllegalArgumentException.class, () -> WatchEndpoint.parse(null));
    }
}
