package com.gabriel.zenithui.ui.home

import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.VectorConverter
import com.gabriel.zenithui.ui.common.Motion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DrawerMotionTest {

    /** Where Motion.rise itself has an app [ms] into its rise, as Compose animates it. */
    private fun rise(ms: Int): Float {
        val spring = Motion.rise<Float>().vectorize(Float.VectorConverter)
        return spring.getValueFromNanos(
            playTimeNanos = ms * 1_000_000L,
            initialValue = AnimationVector1D(0f),
            targetValue = AnimationVector1D(1f),
            initialVelocity = AnimationVector1D(0f),
        ).value
    }

    @Test
    fun `an app waits for its turn and then rises on the drawer's own spring`() {
        val turn = DrawerRise.turn(7, columns = 4)
        assertEquals(0f, DrawerRise.progress(turn - 1f, turn), 0f)
        assertEquals(0f, DrawerRise.progress(turn.toFloat(), turn), 0f)
        for (ms in 0 until DrawerRise.SETTLE_MS step 10) {
            assertEquals("$ms ms in", rise(ms), DrawerRise.progress(turn + ms.toFloat(), turn), 0.002f)
        }
    }

    @Test
    fun `it goes a touch past its place, and is put exactly there once it has settled`() {
        val most = (0 until DrawerRise.SETTLE_MS).maxOf { DrawerRise.progress(it.toFloat(), 0) }
        assertTrue("goes past by ${most - 1f}", most > 1f && most < 1.02f)
        // Settled means a step to its place too small to see.
        assertEquals(1f, rise(DrawerRise.SETTLE_MS), 0.001f)
        assertEquals(1f, DrawerRise.progress(DrawerRise.SETTLE_MS.toFloat(), 0), 0f)
        assertEquals(1f, DrawerRise.progress(60_000f, 0), 0f)
    }

    @Test
    fun `a row sets off a turn after the row above, and along it from the left`() {
        assertEquals(0, DrawerRise.turn(0, columns = 5))
        assertEquals(DrawerRise.ICON_TURN_MS, DrawerRise.turn(1, columns = 5))
        assertEquals(DrawerRise.ROW_TURN_MS, DrawerRise.turn(5, columns = 5))
        assertEquals(2 * DrawerRise.ROW_TURN_MS + 3 * DrawerRise.ICON_TURN_MS, DrawerRise.turn(13, columns = 5))
        // A drawer with no columns yet is one app to a row, not a crash.
        assertEquals(2 * DrawerRise.ROW_TURN_MS, DrawerRise.turn(2, columns = 0))
    }

    @Test
    fun `the clock runs until the last app has settled, and not at all for none`() {
        assertEquals(0, DrawerRise.totalMs(0, columns = 4))
        assertEquals(DrawerRise.SETTLE_MS, DrawerRise.totalMs(1, columns = 4))
        val last = DrawerRise.turn(39, columns = 4)
        assertEquals(last + DrawerRise.SETTLE_MS, DrawerRise.totalMs(40, columns = 4))
        assertEquals(1f, DrawerRise.progress(DrawerRise.totalMs(40, 4).toFloat(), last), 0f)
    }

    @Test
    fun `the home screen frosts over while the drawer opens, not before it has started or after it is up`() {
        assertEquals(0f, DrawerFrost.amount(0f), 0f)
        assertEquals(0f, DrawerFrost.amount(DrawerFrost.FROM), 0f)
        assertEquals(1f, DrawerFrost.amount(DrawerFrost.TO), 0f)
        assertEquals(1f, DrawerFrost.amount(1f), 0f)
        var before = 0f
        for (step in 0..100) {
            val now = DrawerFrost.amount(step / 100f)
            assertTrue(now >= before)
            before = now
        }
    }

    @Test
    fun `with nothing blurred, the page's veil comes in with the frost and is as dark as it was`() {
        assertEquals(0f, DrawerFrost.veil(DrawerFrost.amount(0f)), 0f)
        assertEquals(0f, DrawerFrost.veil(DrawerFrost.amount(DrawerFrost.FROM)), 0f)
        // As much of the page as was seen when the veil sank back inside the home screen: 45% of it
        // there, with the home screen at 45% behind the open drawer. Now it covers the whole screen.
        assertEquals(0.45f * 0.45f, DrawerFrost.veil(DrawerFrost.amount(1f)), 0.005f)
        var before = 0f
        for (step in 0..100) {
            val now = DrawerFrost.veil(DrawerFrost.amount(step / 100f))
            assertTrue("veil goes back to $now at $step%", now >= before)
            before = now
        }
        // Never more than all of it in, whatever it is handed.
        assertEquals(DrawerFrost.VEIL, DrawerFrost.veil(3f), 0f)
        assertEquals(0f, DrawerFrost.veil(-1f), 0f)
    }

    @Test
    fun `the sharp and the blurred home screen together never let much of the wallpaper through`() {
        for (step in 0..100) {
            val blurred = step / 100f
            val sharp = DrawerFrost.sharpAlpha(blurred)
            // The blurred copy drawn over the sharp one: how much of something solid still covers.
            val covers = 1f - (1f - sharp) * (1f - blurred)
            assertTrue("at $blurred it covers $covers", covers >= 0.89f)
        }
        assertEquals(1f, DrawerFrost.sharpAlpha(0f), 0f)
        // Once the blurred copy is all in, nothing sharp shows through where the blur is thin.
        assertEquals(0f, DrawerFrost.sharpAlpha(1f), 0f)
    }
}
