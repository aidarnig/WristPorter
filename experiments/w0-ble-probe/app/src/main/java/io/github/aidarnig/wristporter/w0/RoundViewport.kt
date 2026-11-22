package io.github.aidarnig.wristporter.w0

/**
 * Pixel insets for the temporary classic-View W0 diagnostics.
 * The safe-width margin is proportional to a round watch's diameter.
 * A bottom spacer keeps the final line scrollable into the center of the dial.
 */
internal object RoundViewport {
    data class Insets(val side: Int, val top: Int, val bottomScrollSlack: Int)

    fun insets(widthPx: Int, density: Float, isRound: Boolean): Insets {
        require(widthPx > 0) { "widthPx must be positive" }
        require(density > 0f) { "density must be positive" }
        fun dp(n: Int) = (n * density + 0.5f).toInt()

        return if (isRound) Insets(
            side = (widthPx * 0.16f).toInt(),
            top = (widthPx * 0.15f).toInt(),
            bottomScrollSlack = (widthPx * 0.48f).toInt()
        ) else Insets(side = dp(12), top = dp(12), bottomScrollSlack = dp(60))
    }
}
