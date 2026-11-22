package io.github.aidarnig.wristporter.bridge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ShellUiStateTest {
    @Test
    fun initialStateIsExplicitlyOffline() {
        val state = ShellUiState()
        assertEquals(ConnectionState.NOT_CONNECTED, state.connection)
        assertFalse(state.bluetoothImplemented)
        assertFalse(state.pairingEnabled)
    }
}
