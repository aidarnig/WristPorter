package io.github.aidarnig.wristporter.bridge

/**
 * Deliberately static shell state. No transport, pairing or persisted device identity exists.
 * The W1 implementation will introduce a real state reducer separately.
 */
enum class ConnectionState {
    NOT_CONNECTED
}

data class ShellUiState(
    val connection: ConnectionState = ConnectionState.NOT_CONNECTED
) {
    val bluetoothImplemented: Boolean get() = false
    val pairingEnabled: Boolean get() = false
}
