package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class LiveWallTest {

    private val openMs = 1500L

    // ---- An unlock design's two poses ----

    @Test
    fun `a design that has just been set shows its home pose and holds still`() {
        val unlock = LiveUnlock(openMs)
        assertEquals(1f, unlock.pose(0L), 0f)
        assertFalse(unlock.closed)
        assertFalse(unlock.opening(0L))
    }

    @Test
    fun `the screen going off takes it back to the lock pose, and it waits there however long`() {
        val unlock = LiveUnlock(openMs)
        unlock.screenOff()
        assertTrue(unlock.closed)
        assertEquals(0f, unlock.pose(0L), 0f)
        assertEquals(0f, unlock.pose(3_600_000L), 0f)
        assertFalse(unlock.opening(3_600_000L))
    }

    @Test
    fun `unlocked while on show, it opens in a straight line of time and then holds its home pose`() {
        val unlock = LiveUnlock(openMs)
        unlock.screenOff()
        unlock.unlocked(now = 10_000L, visible = true)
        assertTrue(unlock.opening(10_000L))
        assertEquals(0f, unlock.pose(10_000L), 1e-6f)
        assertEquals(0.5f, unlock.pose(10_750L), 1e-6f)
        assertTrue(unlock.opening(11_499L))
        assertEquals(1f, unlock.pose(11_500L), 0f)
        assertFalse(unlock.opening(11_500L))
        assertFalse(unlock.closed)
        assertEquals(1f, unlock.pose(20_000L), 0f)
    }

    @Test
    fun `unlocked while out of sight, it waits at the lock pose and opens when it is first seen`() {
        val unlock = LiveUnlock(openMs)
        unlock.screenOff()
        unlock.unlocked(now = 5_000L, visible = false)
        assertEquals(0f, unlock.pose(8_000L), 0f)
        assertFalse(unlock.opening(8_000L))
        unlock.shown(now = 9_000L, locked = false)
        assertTrue(unlock.opening(9_000L))
        assertEquals(0.2f, unlock.pose(9_300L), 1e-6f)
    }

    @Test
    fun `on the lock screen it holds the lock pose, even coming into view`() {
        val unlock = LiveUnlock(openMs)
        unlock.screenOff()
        unlock.shown(now = 1_000L, locked = true)
        assertFalse(unlock.opening(1_000L))
        assertEquals(0f, unlock.pose(2_000L), 0f)
        assertTrue(unlock.closed)
    }

    @Test
    fun `coming into view at the lock pose without the lock screen means the phone was unlocked`() {
        // Android's word that the phone was unlocked can come after the home screen is up.
        val unlock = LiveUnlock(openMs)
        unlock.screenOff()
        unlock.shown(now = 1_000L, locked = false)
        assertTrue(unlock.opening(1_000L))
        // And the late word does not start it again.
        unlock.unlocked(now = 1_400L, visible = true)
        assertEquals(0.4f, unlock.pose(1_600L), 1e-6f)
    }

    @Test
    fun `it opens once for each time the screen went off`() {
        val unlock = LiveUnlock(openMs)
        unlock.screenOff()
        unlock.unlocked(now = 0L, visible = true)
        unlock.unlocked(now = 700L, visible = true)
        assertEquals(0.5f, unlock.pose(750L), 1e-6f)
        assertEquals(1f, unlock.pose(2_000L), 0f)
        // Open: coming and going from view, or being told again, changes nothing.
        unlock.unlocked(now = 3_000L, visible = true)
        unlock.shown(now = 3_000L, locked = false)
        assertFalse(unlock.opening(3_000L))
        assertEquals(1f, unlock.pose(3_100L), 0f)
    }

    @Test
    fun `the screen going off half way through the opening takes it straight back to the lock pose`() {
        val unlock = LiveUnlock(openMs)
        unlock.screenOff()
        unlock.unlocked(now = 0L, visible = true)
        assertEquals(0.5f, unlock.pose(750L), 1e-6f)
        unlock.screenOff()
        assertFalse(unlock.opening(800L))
        assertEquals(0f, unlock.pose(800L), 0f)
    }

    @Test
    fun `the preview opens it again from the lock pose whenever it likes`() {
        val unlock = LiveUnlock(openMs)
        unlock.replay(now = 100L)
        assertEquals(0f, unlock.pose(100L), 1e-6f)
        assertTrue(unlock.opening(100L))
        assertEquals(1f, unlock.pose(1_600L), 0f)
        unlock.replay(now = 5_000L)
        assertEquals(0.5f, unlock.pose(5_750L), 1e-6f)
    }

    @Test
    fun `with movement reduced it settles straight into the home pose without opening`() {
        val unlock = LiveUnlock(openMs)
        unlock.screenOff()
        unlock.settle()
        assertFalse(unlock.closed)
        assertFalse(unlock.opening(0L))
        assertEquals(1f, unlock.pose(0L), 0f)
        // Settled, the word that the phone was unlocked has nothing left to open.
        unlock.unlocked(now = 10L, visible = true)
        assertFalse(unlock.opening(10L))
        assertEquals(1f, unlock.pose(20L), 0f)
        // Settling part of the way through an opening ends it at the home pose too.
        unlock.screenOff()
        unlock.unlocked(now = 100L, visible = true)
        unlock.settle()
        assertFalse(unlock.opening(200L))
        assertEquals(1f, unlock.pose(200L), 0f)
    }

    // ---- Hearing that the phone was unlocked ----

    @Test
    fun `at the lock pose, with nothing yet saying the phone was unlocked, it is waiting on the lock screen`() {
        val unlock = LiveUnlock(openMs)
        // Just set, it is open, and there is nothing to wait for.
        assertFalse(unlock.waiting())
        unlock.screenOff()
        assertTrue(unlock.waiting())
        // Coming into view on the lock screen changes nothing: it still waits for the unlock.
        unlock.shown(now = 1_000L, locked = true)
        assertTrue(unlock.waiting())
    }

    @Test
    fun `opening, open, settled, or told while out of sight, it is not waiting any more`() {
        val opening = LiveUnlock(openMs).apply { screenOff(); unlocked(now = 0L, visible = true) }
        assertFalse(opening.waiting())
        // Once open it stays not waiting, however long after.
        assertEquals(1f, opening.pose(5_000L), 0f)
        assertFalse(opening.waiting())
        val told = LiveUnlock(openMs).apply { screenOff(); unlocked(now = 0L, visible = false) }
        // Told while out of sight, it opens when it is first seen: nothing to look for.
        assertFalse(told.waiting())
        val settled = LiveUnlock(openMs).apply { screenOff(); settle() }
        assertFalse(settled.waiting())
    }

    @Test
    fun `the lock screen starting to go is the word to open, and the screen starting to sleep the word to close`() {
        assertEquals(LockWord.Unlocked, LiveLock.ofCommand("android.wallpaper.keyguardgoingaway"))
        assertEquals(LockWord.ScreenOff, LiveLock.ofCommand("android.wallpaper.goingtosleep"))
        // Taps, drops and Android's other words to a wallpaper mean nothing here.
        assertEquals(LockWord.Nothing, LiveLock.ofCommand("android.wallpaper.tap"))
        assertEquals(LockWord.Nothing, LiveLock.ofCommand("android.wallpaper.wakingup"))
        assertEquals(LockWord.Nothing, LiveLock.ofCommand(null))
    }

    @Test
    fun `the user being present means unlocked, the screen coming on only when there is no lock screen up`() {
        assertEquals(LockWord.Unlocked, LiveLock.ofBroadcast("android.intent.action.USER_PRESENT", keyguardLocked = true))
        assertEquals(LockWord.Unlocked, LiveLock.ofBroadcast("android.intent.action.USER_PRESENT", keyguardLocked = false))
        assertEquals(LockWord.Unlocked, LiveLock.ofBroadcast("android.intent.action.SCREEN_ON", keyguardLocked = false))
        assertEquals(LockWord.Nothing, LiveLock.ofBroadcast("android.intent.action.SCREEN_ON", keyguardLocked = true))
        assertEquals(LockWord.ScreenOff, LiveLock.ofBroadcast("android.intent.action.SCREEN_OFF", keyguardLocked = true))
        assertEquals(LockWord.Nothing, LiveLock.ofBroadcast("android.os.action.POWER_SAVE_MODE_CHANGED", keyguardLocked = false))
    }

    @Test
    fun `waiting on the lock screen, it looks for the lock having gone ten times a second`() {
        assertEquals(100L, LiveLock.WATCH_MS)
    }

    // ---- How often a frame is drawn ----

    @Test
    fun `an idle design moves at 60 frames a second in Full and 30 in Lite`() {
        assertEquals(16, LivePace.frameMs(LiveMotion.Idle, lite = false, reduced = false, powerSave = false, opening = false))
        assertEquals(33, LivePace.frameMs(LiveMotion.Idle, lite = true, reduced = false, powerSave = false, opening = false))
    }

    @Test
    fun `an idle design holds still with movement reduced or Battery saver on`() {
        for (lite in listOf(false, true)) {
            assertEquals(0, LivePace.frameMs(LiveMotion.Idle, lite, reduced = true, powerSave = false, opening = false))
            assertEquals(0, LivePace.frameMs(LiveMotion.Idle, lite, reduced = false, powerSave = true, opening = false))
        }
    }

    @Test
    fun `an unlock design draws nothing except while it opens, and then at 60`() {
        for (lite in listOf(false, true)) for (reduced in listOf(false, true)) for (powerSave in listOf(false, true)) {
            assertEquals(0, LivePace.frameMs(LiveMotion.Unlock, lite, reduced, powerSave, opening = false))
            assertEquals(16, LivePace.frameMs(LiveMotion.Unlock, lite, reduced, powerSave, opening = true))
        }
    }

    /** Which of [vsyncs] vsyncs, [vsyncNanos] apart, a design paced to [frameMs] draws on. */
    private fun drawn(frameMs: Int, vsyncNanos: Long, vsyncs: Int = 24): List<Int> {
        var last = 0L
        val out = mutableListOf<Int>()
        for (n in 1..vsyncs) {
            val now = n * vsyncNanos
            if (LivePace.due(now, last, frameMs, vsyncNanos)) {
                out += n
                last = now
            }
        }
        return out
    }

    @Test
    fun `paced to 30, a 120 Hz screen draws every fourth vsync, a 90 Hz one every third, a 60 Hz one every other`() {
        assertEquals((1..24).filter { (it - 1) % 4 == 0 }, drawn(33, 8_333_333L))
        assertEquals((1..24).filter { (it - 1) % 3 == 0 }, drawn(33, 11_111_111L))
        assertEquals((1..24).filter { (it - 1) % 2 == 0 }, drawn(33, 16_666_667L))
    }

    @Test
    fun `paced to 60, a 120 Hz screen draws every other vsync and a 60 Hz one every vsync`() {
        assertEquals((1..24).filter { (it - 1) % 2 == 0 }, drawn(16, 8_333_333L))
        assertEquals((1..24).toList(), drawn(16, 16_666_667L))
    }

    @Test
    fun `a vsync that comes a little early still draws its frame`() {
        // 120 Hz paced to 30: the fourth vsync, a millisecond early.
        assertTrue(LivePace.due(now = 32_333_333L, last = 0L + 1L, frameMs = 33, vsyncNanos = 8_333_333L))
        assertFalse(LivePace.due(now = 25_000_000L, last = 1L, frameMs = 33, vsyncNanos = 8_333_333L))
    }

    // ---- The sums they move by ----

    @Test
    fun `a swing goes once round in its period, starting where its phase says`() {
        assertEquals(0f, LiveCurves.swing(0.0, 20.0), 1e-6f)
        assertEquals(1f, LiveCurves.swing(5.0, 20.0), 1e-6f)
        assertEquals(-1f, LiveCurves.swing(15.0, 20.0), 1e-6f)
        assertEquals(1f, LiveCurves.swing(0.0, 20.0, phase = 0.25), 1e-6f)
        // Long after it started, it is exactly where it was a whole number of periods before.
        assertEquals(LiveCurves.swing(7.3, 20.0), LiveCurves.swing(7.3 + 20.0 * 5_000, 20.0), 1e-4f)
    }

    @Test
    fun `a turn runs from 0 to 1 and starts again`() {
        assertEquals(0f, LiveCurves.turn(0.0, 360.0), 0f)
        assertEquals(0.5f, LiveCurves.turn(180.0, 360.0), 1e-6f)
        assertEquals(0f, LiveCurves.turn(360.0, 360.0), 1e-6f)
        assertEquals(0.25f, LiveCurves.turn(90.0 + 360.0 * 10_000, 360.0), 1e-4f)
    }

    @Test
    fun `the eases run from 0 to 1 and never step back`() {
        for (ease in listOf(LiveCurves::easeOut, LiveCurves::easeInOut)) {
            assertEquals(0f, ease(0f), 1e-6f)
            assertEquals(1f, ease(1f), 1e-6f)
            assertEquals(0f, ease(-1f), 1e-6f)
            assertEquals(1f, ease(2f), 1e-6f)
            var before = 0f
            for (i in 1..100) {
                val now = ease(i / 100f)
                assertTrue(now >= before)
                before = now
            }
        }
        assertEquals(0.5f, LiveCurves.easeInOut(0.5f), 1e-6f)
        assertTrue(LiveCurves.easeOut(0.2f) > 0.2f)
    }

    @Test
    fun `a spring overshoots once and has settled by the end, so nothing jumps when it stops`() {
        assertEquals(0f, LiveCurves.spring(0f), 0f)
        assertEquals(1f, LiveCurves.spring(1f), 0f)
        val samples = (0..1000).map { LiveCurves.spring(it / 1000f) }
        assertTrue(samples.max() > 1.01f)
        assertTrue(samples.max() < 1.25f)
        assertTrue(abs(LiveCurves.spring(0.999f) - 1f) < 0.01f)
        // A stiffer one overshoots less.
        assertTrue((0..1000).maxOf { LiveCurves.spring(it / 1000f, damping = 0.9f) } < samples.max())
    }

    @Test
    fun `a burst flashes up near a third of the way and is back to 1 at the end`() {
        assertEquals(0f, LiveCurves.burst(0f, 1.6f), 1e-6f)
        assertEquals(1f, LiveCurves.burst(1f, 1.6f), 0f)
        assertTrue(LiveCurves.burst(0.35f, 1.6f) in 1.45f..1.6f)
        assertTrue(abs(LiveCurves.burst(0.99f, 1.6f) - 1f) < 0.01f)
    }
}
