package io.github.aidarnig.wristporter.w0

/**
 * Safe GATT capability/status descriptions for the disposable W0 probe.
 * No characteristic payload, MAC, UID, title, artist or device name is retained.
 * Property flags describe the service discovered by Android; they do not
 * themselves prove the iPhone will authorize a write.
 */
internal object GattDiagnostics {
    private const val READ = 0x02
    private const val WRITE_NO_RESPONSE = 0x04
    private const val WRITE = 0x08
    private const val NOTIFY = 0x10
    private const val INDICATE = 0x20

    fun properties(bits: Int, cccdPresent: Boolean): String =
        "N=" + enabled(bits, NOTIFY) +
        " I=" + enabled(bits, INDICATE) +
        " W=" + enabled(bits, WRITE) +
        " WNR=" + enabled(bits, WRITE_NO_RESPONSE) +
        " R=" + enabled(bits, READ) +
        " CCCD=" + cccdPresent +
        " mask=0x" + bits.toString(16)

    fun entityWriteExpectation(bits: Int): String = when {
        bits and WRITE != 0 -> "with-response advertised"
        bits and WRITE_NO_RESPONSE != 0 -> "NO_RESPONSE only: conflicts with AMS expectation"
        else -> "no write property advertised"
    }

    fun status(code: Int): String = when (code) {
        0 -> "SUCCESS"
        1 -> "INVALID_HANDLE"
        2 -> "READ_NOT_PERMITTED"
        3 -> "WRITE_NOT_PERMITTED"
        5 -> "INSUFFICIENT_AUTHENTICATION"
        6 -> "REQUEST_NOT_SUPPORTED"
        8 -> "INSUFFICIENT_AUTHORIZATION"
        13 -> "INVALID_ATTRIBUTE_LENGTH"
        15 -> "INSUFFICIENT_ENCRYPTION"
        0xA0 -> "AMS_INVALID_STATE"
        0xA1 -> "AMS_INVALID_COMMAND"
        0xA2 -> "AMS_ABSENT_ATTRIBUTE"
        else -> "status=$code"
    }

    private fun enabled(bits: Int, flag: Int) = bits and flag != 0
}
