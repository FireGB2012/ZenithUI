package com.gabriel.zenithui.ui.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SheetPullTest {

    private val give = 80f

    @Test
    fun `pulled down it goes exactly as far as the finger`() {
        for (pull in listOf(0f, 1f, 40f, 300f, 2000f)) assertEquals(pull, SheetPull.shown(pull, give), 0f)
    }

    @Test
    fun `pulled up it comes less far than the finger, and less the further`() {
        var last = 0f
        var lastStep = Float.MAX_VALUE
        for (pull in listOf(-20f, -40f, -60f, -80f, -100f, -120f)) {
            val shown = SheetPull.shown(pull, give)
            assertTrue("it does come up: $shown for $pull", shown < last)
            assertTrue("but not as far as the finger: $shown for $pull", shown > pull)
            val step = last - shown
            assertTrue("each 20 px buys less than the 20 before: $step after $lastStep", step < lastStep)
            last = shown
            lastStep = step
        }
    }

    @Test
    fun `however hard it is pulled up it never comes as far as it has to give`() {
        for (pull in listOf(-100f, -1_000f, -100_000f)) {
            assertTrue(SheetPull.shown(pull, give) > -give)
        }
    }

    @Test
    fun `there is no jump where down turns into up`() {
        assertEquals(0f, SheetPull.shown(0f, give), 0f)
        assertEquals(0f, SheetPull.shown(-0.001f, give), 0.001f)
    }

    @Test
    fun `a sheet with nothing to give does not come up at all`() {
        assertEquals(0f, SheetPull.shown(-200f, 0f), 0f)
    }

    @Test
    fun `the pull can be found again from where the sheet is drawn`() {
        for (pull in listOf(-300f, -90f, -12f, 0f, 55f, 700f)) {
            assertEquals(pull, SheetPull.raw(SheetPull.shown(pull, give), give), 0.01f * (1f + kotlin.math.abs(pull)))
        }
    }

    @Test
    fun `let go most of the way up it stays, and most of the way down it goes`() {
        val height = 1000f
        assertFalse(SheetPull.dismisses(0f, height, 0f))
        assertFalse(SheetPull.dismisses(height * SheetPull.AWAY - 1f, height, 0f))
        assertTrue(SheetPull.dismisses(height * SheetPull.AWAY + 1f, height, 0f))
        assertTrue(SheetPull.dismisses(height, height, 0f))
    }

    @Test
    fun `a flick decides whatever the distance`() {
        val height = 1000f
        // Barely moved, thrown down: gone.
        assertTrue(SheetPull.dismisses(10f, height, SheetPull.FLING + 1f))
        // Nearly off the screen, thrown back up: stays.
        assertFalse(SheetPull.dismisses(900f, height, -SheetPull.FLING - 1f))
        // A slow drift either way is not a flick, and the distance decides.
        assertFalse(SheetPull.dismisses(10f, height, SheetPull.FLING - 1f))
        assertTrue(SheetPull.dismisses(900f, height, -SheetPull.FLING + 1f))
    }
}
