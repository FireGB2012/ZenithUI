package com.gabriel.zenithui.ui.home

import androidx.compose.animation.core.FloatSpringSpec
import androidx.compose.ui.geometry.Offset
import com.gabriel.zenithui.data.AnimationTuning
import com.gabriel.zenithui.data.Tune
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LiquidArrivalTest {

    private val tuning = AnimationTuning()

    /** A wave from the middle of a 1080 by 2340 screen, at 2.625 pixels to the dp, as on a Galaxy A55. */
    private fun wave(t: AnimationTuning = tuning, centre: Offset = Offset(540f, 1170f)) =
        LiquidArrival.wave(centre, t, 2.625f) { 1400f }

    @Test
    fun `each thing rises on the same spring as Compose's own`() {
        for ((stiffness, damping) in listOf(220f to 0.8f, 380f to 0.5f, 180f to 1f, 120f to 1.2f, 1200f to 0.3f)) {
            val compose = FloatSpringSpec(dampingRatio = damping, stiffness = stiffness)
            var ms = 0L
            while (ms <= 1500L) {
                val theirs = compose.getValueFromNanos(ms * 1_000_000L, 0f, 1f, 0f)
                val ours = LiquidArrival.rise(ms / 1000f, stiffness, damping)
                assertEquals("$stiffness/$damping at $ms ms", theirs, ours, 0.002f)
                ms += 8L
            }
        }
    }

    @Test
    fun `a spring rises from its start to its place, past it only when it is under-damped`() {
        assertEquals(0f, LiquidArrival.rise(0f, 220f, 0.8f), 0f)
        assertEquals(0f, LiquidArrival.rise(-1f, 220f, 0.8f), 0f)
        var highest = 0f
        var last = 0f
        var t = 0f
        while (t < 2f) {
            val swung = LiquidArrival.rise(t, 220f, 0.8f)
            highest = maxOf(highest, swung)
            val damped = LiquidArrival.rise(t, 220f, 1f)
            assertTrue("never past its place: $damped at $t", damped <= 1f)
            assertTrue("and never back: $damped at $t", damped >= last - 1e-6f)
            last = damped
            t += 0.004f
        }
        assertTrue("a hair past: $highest", highest > 1f && highest < 1.03f)
    }

    @Test
    fun `once settled a spring stays within a hair of its place`() {
        for ((stiffness, damping) in listOf(220f to 0.8f, 40f to 0.3f, 180f to 1f, 120f to 1.2f, 600f to 0.999f)) {
            val settle = LiquidArrival.settleMs(stiffness, damping)
            assertTrue("$stiffness/$damping: $settle", settle in 1f..LiquidArrival.LONGEST_SETTLE_MS)
            var ms = settle
            while (ms < settle + 2000f) {
                val away = abs(1f - LiquidArrival.rise(ms / 1000f, stiffness, damping))
                assertTrue("$stiffness/$damping at $ms ms: $away", away <= LiquidArrival.STILL * 1.01f)
                ms += 5f
            }
        }
    }

    @Test
    fun `the wave reaches what is near before what is far, and its light keeps step`() {
        val wave = wave()
        assertEquals(0f, wave.delayMs(0f), 0f)
        val distances = listOf(0f, 100f, 300f, 600f, 900f, 1200f, 1400f)
        distances.zipWithNext { near, far -> assertTrue("$near before $far", wave.delayMs(near) <= wave.delayMs(far)) }
        assertTrue(wave.delayMs(300f) < wave.delayMs(1200f))
        // Something sets off a moment before the light is over it, and the light is over it then;
        // what the light is over sooner than that sets off at once.
        for (distance in listOf(600f, 900f, 1200f)) {
            val delay = wave.delayMs(distance)
            assertTrue("$distance: $delay", delay > 0f)
            assertEquals("light at $distance", distance, wave.front(delay + wave.leadMs), 1f)
        }
        assertEquals(0f, wave.delayMs(wave.front(wave.leadMs) - 1f), 0f)
        // Quick off the start, slowing as it spreads.
        assertTrue(wave.front(wave.runMs / 4f) > wave.reach / 4f)
        assertEquals(wave.reach, wave.front(wave.runMs), 0.5f)
    }

    @Test
    fun `before the wave reaches it, something is small, faint and drawn in towards where it started`() {
        val wave = wave()
        val spot = LiquidSpot()
        // Far down and to the right of the middle.
        spot.meet(wave) { Offset(540f + 600f, 1170f + 800f) }
        assertTrue(spot.delayMs > 0f)
        val out = spot.out(spot.delayMs - 1f)
        assertEquals(1f, out, 0f)
        assertEquals(tuning[Tune.ArriveFrom], wave.scaleAt(out), 0.0001f)
        assertEquals(0f, LiquidArrival.alphaAt(out), 0f)
        // Up and to the left, the whole of the pull, which is in dp.
        assertTrue(spot.startX < 0f && spot.startY < 0f)
        assertEquals(tuning[Tune.ArrivePull] * 2.625f, kotlin.math.hypot(spot.startX, spot.startY), 0.01f)
        assertEquals(0.75f, spot.startX / spot.startY, 0.001f)
        // Right by where it started it is not drawn anywhere.
        val near = LiquidSpot()
        near.meet(wave) { Offset(540f, 1170f) }
        assertEquals(0f, near.startX, 0f)
        assertEquals(0f, near.startY, 0f)
        assertEquals(0f, near.delayMs, 0f)
    }

    @Test
    fun `everything is in its place by the end, and stays there`() {
        for (t in listOf(tuning, AnimationTuning.of(com.gabriel.zenithui.data.AnimationPreset.Bouncy), tuning.paced(0.4f), tuning.paced(2.5f))) {
            val wave = wave(t)
            for (distance in listOf(0f, 200f, 700f, wave.reach, wave.reach * 2f)) {
                val delay = wave.delayMs(distance)
                for (clock in listOf(wave.totalMs, wave.totalMs + 50f, wave.totalMs + 4000f)) {
                    val out = wave.out(clock, delay)
                    assertTrue("$distance at $clock: $out", abs(out) <= LiquidArrival.STILL * 1.01f)
                }
            }
        }
    }

    @Test
    fun `it rises a little past its place and back, never far, and is solid once it has`() {
        val wave = wave()
        var lowest = 1f
        var clock = 0f
        while (clock <= wave.totalMs) {
            val out = wave.out(clock, 0f)
            lowest = minOf(lowest, out)
            assertTrue("never further than it started: $out at $clock", out <= 1f)
            val alpha = LiquidArrival.alphaAt(out)
            assertTrue(alpha in 0f..1f)
            if (out <= 0f) assertEquals(1f, alpha, 0f)
            clock += 4f
        }
        assertTrue("past its place: $lowest", lowest < 0f)
        assertTrue("by a hair: $lowest", lowest > -0.03f)
        assertEquals(1.0f, wave.scaleAt(0f), 0f)
    }

    @Test
    fun `the pace set runs it all quicker or slower, light included`() {
        val own = wave()
        val twice = wave(tuning.paced(2f))
        assertEquals(own.runMs / 2f, twice.runMs, 0.01f)
        assertEquals(own.leadMs / 2f, twice.leadMs, 0.01f)
        assertEquals(own.stiffness * 4f, twice.stiffness, 0.5f)
        assertTrue(twice.totalMs < own.totalMs)
        assertEquals(tuning[Tune.ArriveLight], own.light.peak, 0f)
    }

    @Test
    fun `a spot meets each wave once, where it was then`() {
        val first = wave()
        val spot = LiquidSpot()
        var at = Offset(540f, 300f)
        spot.meet(first) { at }
        val delay = spot.delayMs
        // Moved on the way, by the home screen coming forward say: it still rises when it was due to.
        at = Offset(540f, 1100f)
        spot.meet(first) { at }
        assertEquals(delay, spot.delayMs, 0f)
        // A new wave finds it where it is now.
        spot.meet(wave()) { at }
        assertTrue(spot.delayMs < delay)
        // Nowhere known yet: it rises with where the wave starts.
        val lost = LiquidSpot()
        lost.meet(first) { Offset.Unspecified }
        assertEquals(0f, lost.delayMs, 0f)
        assertEquals(0f, LiquidSpot().out(100f), 0f)
    }

    @Test
    fun `at its own numbers the whole screen has come home inside a second and a quarter`() {
        val wave = wave()
        assertTrue("${wave.totalMs}", wave.totalMs <= 1250f)
        // And what is near where it started is most of the way up within a fifth of a second.
        assertTrue(wave.out(200f, wave.delayMs(150f)) < 0.2f)
    }
}
