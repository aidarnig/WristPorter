package io.github.aidarnig.wristporter.w0

/** Sanitized protocol indicators only. Never return UID, message text, artist or track name. */
internal object ProbeSummaries {
    fun ancs(data: ByteArray): String = if (data.size == 8) {
        val event = when (data[0].toInt() and 0xff) {
            0 -> "added"
            1 -> "modified"
            2 -> "removed"
            else -> "unknown"
        }
        "ANCS event=$event category=${data[2].toInt() and 0xff}"
    } else "ANCS malformed event (bytes=${data.size})"

    fun amsEntity(data: ByteArray): String = if (data.size >= 3) {
        "AMS metadata event entity=${data[0].toInt() and 0xff} attribute=${data[1].toInt() and 0xff} truncated=${(data[2].toInt() and 1) != 0}"
    } else "AMS malformed entity update (bytes=${data.size})"

    fun amsCommands(data: ByteArray): String =
        "AMS advertised command count=${data.size} (contents omitted)"
}
