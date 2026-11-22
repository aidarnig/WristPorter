package io.github.aidarnig.wristporter.w0

import org.junit.Assert.*
import org.junit.Test

class GattDiagnosticsTest {
    @Test fun diagnosticsExposeOnlyPublicCapabilities() {
        val info = GattDiagnostics.properties(0x18, cccdPresent = true)
        assertTrue(info.contains("N=true"))
        assertTrue(info.contains("W=true"))
        assertTrue(info.contains("WNR=false"))
        assertTrue(info.contains("CCCD=true"))
        assertFalse(info.contains("address"))
    }

    @Test fun writeTypeCanBeAssessedWithoutSendingAProbeWrite() {
        assertEquals("with-response advertised", GattDiagnostics.entityWriteExpectation(0x18))
        assertEquals("NO_RESPONSE only: conflicts with AMS expectation", GattDiagnostics.entityWriteExpectation(0x14))
        assertEquals("no write property advertised", GattDiagnostics.entityWriteExpectation(0x10))
    }

    @Test fun failedRemoteWriteIsDifferentFromLocalStartFailure() {
        assertEquals("WRITE_NOT_PERMITTED", GattDiagnostics.status(3))
        assertEquals("AMS_INVALID_STATE", GattDiagnostics.status(0xA0))
        assertEquals("SUCCESS", GattDiagnostics.status(0))
        assertEquals("status=133", GattDiagnostics.status(133))
    }
}
