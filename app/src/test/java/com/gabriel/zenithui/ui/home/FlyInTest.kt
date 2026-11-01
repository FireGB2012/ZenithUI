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

    @Test
    fun `from the middle is the arrival as it always was`() {
        val start = FlyIn.start("middle", 300f, -400f, 1000f, 2000f)
        assertEquals(FlyIn.delayMs(kotlin.math.hypot(300f, -400f) / kotlin.math.hypot(500f, 1000f)), start.delayMs)
        assertEquals(300f * FlyIn.SPREAD, start.x, 0f)
        assertEquals(-400f * FlyIn.SPREAD, start.y, 0f)
        assertEquals(start, FlyIn.start("anything else", 300f, -400f, 1000f, 2000f))
        assertEquals("middle", FlyIn.styles.first())
    }

    @Test
    fun `from below everything rises, the foot of the page first`() {
        val foot = FlyIn.start("below", 0f, 1000f, 1000f, 2000f)
        val top = FlyIn.start("below", 0f, -1000f, 1000f, 2000f)
        assertEquals(0, foot.delayMs)
        assertTrue(top.delayMs > foot.delayMs)
        assertTrue("under its place", foot.y > 0f && top.y > 0f)
        assertEquals(0f, foot.x, 0f)
    }

    @Test
    fun `a cascade drops everything in, a column at a time from the left`() {
        val left = FlyIn.start("cascade", -500f, 0f, 1000f, 2000f)
        val right = FlyIn.start("cascade", 500f, 0f, 1000f, 2000f)
        assertEquals(0, left.delayMs)
        assertTrue(right.delayMs > left.delayMs)
        assertTrue("over its place", left.y < 0f)
        // Every style is over inside the arrival's own time.
        for (style in FlyIn.styles) {
            val last = FlyIn.start(style, 500f, -1000f, 1000f, 2000f)
            assertEquals(1f, FlyIn.progress(FlyIn.TOTAL_MS.toFloat(), last.delayMs), 0f)
        }
    }
}
