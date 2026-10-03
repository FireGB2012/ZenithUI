package com.gabriel.zenithui.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlyInTest {

    private val distances = listOf(0f, 0.1f, 0.25f, 0.5f, 0.75f, 1f)

    @Test
    fun `nothing has moved before its turn`() {
        for (distance in distances) {
            val delay = FlyIn.delayMs(distance)
            assertEquals(0f, FlyIn.progress(0f, delay), 0f)
            assertEquals(0f, FlyIn.progress(delay.toFloat(), delay), 0f)
            assertEquals(0f, FlyIn.progress(delay - 20f, delay), 0f)
        }
    }

    @Test
    fun `everything is down by the end, and stays down`() {
        for (distance in distances) {
            val delay = FlyIn.delayMs(distance)
            assertEquals(1f, FlyIn.progress(FlyIn.TOTAL_MS.toFloat(), delay), 0f)
            assertEquals(1f, FlyIn.progress(FlyIn.TOTAL_MS + 500f, delay), 0f)
        }
        assertTrue("the whole arrival fits inside 600 ms", FlyIn.TOTAL_MS <= 600)
    }

    @Test
    fun `what is nearest the middle lands first`() {
        assertEquals(0, FlyIn.delayMs(0f))
        distances.zipWithNext { nearer, farther ->
            assertTrue(FlyIn.delayMs(nearer) < FlyIn.delayMs(farther))
        }
        // Part way through, the nearer of two is always the further along.
        for (clock in listOf(100f, 200f, 300f, 400f)) {
            distances.zipWithNext { nearer, farther ->
                assertTrue(
                    "at $clock ms",
                    FlyIn.progress(clock, FlyIn.delayMs(nearer)) >= FlyIn.progress(clock, FlyIn.delayMs(farther)) ||
                        // Both past the top of their overshoot and settling: the order no longer shows.
                        FlyIn.progress(clock, FlyIn.delayMs(farther)) > 1f,
                )
            }
        }
    }

    @Test
    fun `an item goes a little past its place and comes back, never far`() {
        var highest = 0f
        var clock = 0f
        while (clock <= FlyIn.TOTAL_MS) {
            val p = FlyIn.progress(clock, 0)
            assertTrue("never backwards past the start: $p at $clock", p >= 0f)
            highest = maxOf(highest, p)
            clock += 4f
        }
        assertTrue("it overshoots: $highest", highest > 1f)
        assertTrue("but by less than a tenth: $highest", highest < 1.1f)
    }

    @Test
    fun `a distance outside the page counts as the edge of it`() {
        assertEquals(FlyIn.delayMs(1f), FlyIn.delayMs(4f))
        assertEquals(FlyIn.delayMs(0f), FlyIn.delayMs(-1f))
    }
}
