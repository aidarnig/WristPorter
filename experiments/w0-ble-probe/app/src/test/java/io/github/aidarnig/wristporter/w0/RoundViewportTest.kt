package io.github.aidarnig.wristporter.w0

import org.junit.Assert.*
import org.junit.Test

class RoundViewportTest {
    @Test fun roundDisplayMaintainsCentralSafeAreaAndScrollSlack() {
        val result = RoundViewport.insets(widthPx = 450, density = 2f, isRound = true)
        assertEquals(72, result.side)
        assertEquals(67, result.top)
        assertEquals(216, result.bottomScrollSlack)
        assertTrue(result.side * 2 < 450)
    }

    @Test fun smallRoundDisplayScalesMarginsWithDiameter() {
        val large = RoundViewport.insets(widthPx = 450, density = 2f, isRound = true)
        val small = RoundViewport.insets(widthPx = 360, density = 2f, isRound = true)
        assertTrue(small.side < large.side)
        assertTrue(small.bottomScrollSlack < large.bottomScrollSlack)
    }

    @Test fun rectangularDisplayKeepsModestDpInsets() {
        val result = RoundViewport.insets(widthPx = 450, density = 2f, isRound = false)
        assertEquals(24, result.side)
        assertEquals(24, result.top)
        assertEquals(120, result.bottomScrollSlack)
    }
}
