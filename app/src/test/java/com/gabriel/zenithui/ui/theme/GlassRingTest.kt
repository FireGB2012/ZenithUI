package com.gabriel.zenithui.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class GlassRingTest {

    private val eps = 0.001f

    @Test
    fun `a tap's ring runs as it always has, quick off the finger and fading as it goes`() {
        val tap = RingStyle.TAP
        assertEquals(RING_MS, tap.durationMs, eps)
        for (p in listOf(0f, 0.2f, 0.5f, 0.9f, 1f)) {
            assertEquals(1f - (1f - p).pow(3), tap.spreadAt(p), eps)
            assertEquals(1f - p, tap.strengthAt(p), eps)
        }
    }

    @Test
    fun `the wave that wakes the home screen is quicker, and slows as it spreads`() {
        val wake = RingStyle.WAKE
        assertEquals(640f, wake.durationMs, eps)
        assertEquals(0f, wake.spreadAt(0f), eps)
        assertEquals(1f, wake.spreadAt(1f), eps)
        // Halfway out long before halfway through its time.
        assertTrue(wake.progressAt(0.5f) < 0.35f)
    }

    @Test
    fun `a wave keeps most of its light past halfway, then fades to nothing`() {
        val wake = RingStyle.WAKE
        assertEquals(1f, wake.strengthAt(0f), eps)
        assertTrue(wake.strengthAt(0.5f) > 0.8f)
        assertEquals(0f, wake.strengthAt(1f), eps)
    }

    @Test
    fun `the wave that settles it is slower and fainter`() {
        val calm = RingStyle.CALM
        assertTrue(calm.durationMs > RingStyle.WAKE.durationMs)
        assertTrue(calm.peak < 1f)
        assertEquals(calm.peak, calm.strengthAt(0f), eps)
    }

    @Test
    fun `when a ring reaches something is when it has spread that far`() {
        for (style in listOf(RingStyle.TAP, RingStyle.WAKE, RingStyle.CALM)) {
            assertEquals(0f, style.progressAt(0f), eps)
            assertEquals(1f, style.progressAt(1f), eps)
            for (part in listOf(0.1f, 0.3f, 0.6f, 0.9f)) {
                assertEquals(part, style.spreadAt(style.progressAt(part)), eps)
            }
        }
    }

    @Test
    fun `a wave reaches what is near before what is far, and what is off the screen at once`() {
        val ring = GlassRing(screen = Size(1000f, 2000f), density = 1f)
        ring.start(Offset(500f, 1500f), nowNanos = 0L, style = RingStyle.WAKE)
        val near = ring.reachesIn(Offset(500f, 1400f), nowNanos = 0L)
        val far = ring.reachesIn(Offset(100f, 100f), nowNanos = 0L)
        assertTrue(near > 0f && near < far)
        assertTrue(far <= RingStyle.WAKE.durationMs)
        assertEquals(0f, ring.reachesIn(Offset(1500f, 1500f), nowNanos = 0L), eps)
        // Asked later, it is that much nearer.
        assertEquals(far - 100f, ring.reachesIn(Offset(100f, 100f), nowNanos = 100_000_000L), 1f)
        // Once it has run out it reaches nothing more.
        assertEquals(0f, ring.reachesIn(Offset(100f, 100f), nowNanos = 700_000_000L), eps)
    }

    @Test
    fun `a ring is drawn until its time is up`() {
        val ring = GlassRing(screen = Size(1000f, 2000f), density = 1f)
        assertNull(ring.now(0L))
        ring.start(Offset(500f, 1000f), nowNanos = 0L, style = RingStyle.CALM)
        val early = ring.now(100_000_000L)
        val late = ring.now(800_000_000L)
        assertNotNull(early)
        assertNotNull(late)
        assertTrue(late!!.radius > early!!.radius)
        assertTrue(early.strength <= RingStyle.CALM.peak)
        assertNull(ring.now(1_000_000_000L))
    }

    @Test
    fun `a ring waiting for its first frame reaches things from the start of its run`() {
        val ring = GlassRing(screen = Size(1000f, 2000f), density = 1f)
        ring.startOnNextFrame(Offset(500f, 1500f), RingStyle.WAKE)
        // Not drawn before the frame it starts on.
        assertNull(ring.now(5_000_000_000L))
        val started = GlassRing(screen = Size(1000f, 2000f), density = 1f)
        started.start(Offset(500f, 1500f), nowNanos = 0L, style = RingStyle.WAKE)
        assertEquals(
            started.reachesIn(Offset(100f, 100f), nowNanos = 0L),
            ring.reachesIn(Offset(100f, 100f), nowNanos = 5_000_000_000L),
            eps,
        )
    }

    @Test
    fun `the light of coming home runs as the wave does, soft on the wallpaper`() {
        val light = RingStyle.arrive(520f, 0.6f)
        assertEquals(520f, light.durationMs, 0f)
        assertEquals(0.6f, light.peak, 0f)
        assertTrue("a softer band than a tap's, and hardly a line", light.band < 1f && light.line < light.band)
        // Slowing as it spreads, but less than edit mode's waves, so it is seen to travel.
        assertTrue(light.spreadAt(0.2f) > 0.2f && light.spreadAt(0.2f) < RingStyle.WAKE.spreadAt(0.2f))
        assertEquals(1f - 0.8f * kotlin.math.sqrt(0.8f), light.spreadAt(0.2f), eps)
        assertEquals(1f, light.spreadAt(1f), 0f)
        // A tap's ring is as it always was on the wallpaper.
        assertEquals(1f, RingStyle.TAP.band, 0f)
        assertEquals(1f, RingStyle.TAP.line, 0f)
        // What keeps step with a ring knows how far it runs, and it runs that far.
        val ring = GlassRing(Size(1080f, 2340f), 2.625f)
        val at = Offset(300f, 1900f)
        ring.start(at, nowNanos = 0L, style = light)
        val reach = ring.reachFrom(at, light)
        assertTrue(reach > ringReach(at, Size(1080f, 2340f)))
        val half = assertNotNullNow(ring.now(260_000_000L))
        assertEquals(reach * light.spreadAt(0.5f), half.radius, 0.5f)
        assertEquals(light.band, half.band, 0f)
        assertEquals(light.line, half.line, 0f)
    }

    private fun assertNotNullNow(now: RingNow?): RingNow {
        assertNotNull(now)
        return now!!
    }
}
