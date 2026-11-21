package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameReportTest {

    /** A 120 Hz screen's frame. */
    private val hz120 = 8_333_333L

    private fun frames(
        count: Int,
        every: Long,
        from: Long = 1_000_000_000L,
        app: Long = 3_000_000L,
        drawing: Long = 2_000_000L,
        gpu: Long = 2_000_000L,
        interval: Long = hz120,
    ): List<FrameSample> = (0 until count).map {
        FrameSample(start = from + it * every, app = app, drawing = drawing, gpu = gpu, interval = interval)
    }

    // ---- How it ran while things moved ----

    @Test
    fun `there is nothing to say until the home screen has moved for a while`() {
        assertNull(FrameReport.of(emptyList()))
        assertNull(FrameReport.of(frames(FrameReport.MIN_FRAMES - 1, hz120)))
    }

    @Test
    fun `a phone keeping up shows the screen's own rate, every frame on time`() {
        val report = FrameReport.of(frames(120, hz120))!!
        assertEquals(120, report.frames)
        assertEquals(120, report.fps)
        assertEquals(1f, report.onTime, 0.001f)
    }

    @Test
    fun `a new frame every fifty milliseconds is twenty frames a second, and none of them on time`() {
        val report = FrameReport.of(frames(60, 50_000_000L, app = 40_000_000L))!!
        assertEquals(20, report.fps)
        assertEquals(0f, report.onTime, 0.001f)
    }

    @Test
    fun `a clock ticking over once a second is not movement`() {
        val ticks = frames(100, 1_000_000_000L)
        assertNull(FrameReport.of(ticks))
    }

    @Test
    fun `the wait between two swipes is not counted as a slow frame`() {
        val first = frames(60, hz120, from = 0L)
        val second = frames(60, hz120, from = 5_000_000_000L)
        val report = FrameReport.of(second + first)!!
        assertEquals(120, report.frames)
        assertEquals(120, report.fps)
        assertEquals(1f, report.onTime, 0.001f)
    }

    @Test
    fun `a frame that missed its turn shows in the share on time`() {
        // Forty frames, every one on its vsync but one that took three.
        val starts = (0 until 40).map { it * hz120 } .map { if (it >= 20 * hz120) it + 2 * hz120 else it }
        val samples = starts.map { FrameSample(start = it, app = 3_000_000L, drawing = 2_000_000L, gpu = 2_000_000L, interval = hz120) }
        val report = FrameReport.of(samples)!!
        // 39 steps from one frame to the next, one of them late.
        assertEquals(38f / 39f, report.onTime, 0.001f)
        assertTrue(report.fps < 120)
    }

    @Test
    fun `a sixty hertz screen is on time at sixty`() {
        val report = FrameReport.of(frames(60, 16_666_667L, interval = 16_666_667L))!!
        assertEquals(60, report.fps)
        assertEquals(1f, report.onTime, 0.001f)
    }

    // ---- Which part is slow ----

    @Test
    fun `zenith's own thread is the slow part when it takes longest`() {
        val report = FrameReport.of(frames(60, 25_000_000L, app = 22_000_000L, drawing = 3_000_000L, gpu = 5_000_000L))!!
        assertEquals(FrameReport.Part.APP, report.slowest)
        assertEquals(22f, report.appMs, 0.01f)
    }

    @Test
    fun `the graphics chip is the slow part when it takes longest`() {
        val report = FrameReport.of(frames(60, 25_000_000L, app = 3_000_000L, drawing = 4_000_000L, gpu = 21_000_000L))!!
        assertEquals(FrameReport.Part.GPU, report.slowest)
        assertEquals(21f, report.gpuMs!!, 0.01f)
    }

    @Test
    fun `the drawing thread is the slow part when it takes longest`() {
        val report = FrameReport.of(frames(60, 25_000_000L, app = 3_000_000L, drawing = 18_000_000L, gpu = 6_000_000L))!!
        assertEquals(FrameReport.Part.DRAWING, report.slowest)
    }

    @Test
    fun `where android does not time the graphics chip it is left out`() {
        val report = FrameReport.of(frames(60, 25_000_000L, app = 3_000_000L, drawing = 9_000_000L, gpu = -1L))!!
        assertNull(report.gpuMs)
        assertEquals(FrameReport.Part.DRAWING, report.slowest)
    }

    @Test
    fun `the time shown for each part is what nine frames in ten take at most, so a few spikes show`() {
        val calm = frames(90, hz120, app = 2_000_000L)
        val spiky = frames(10, hz120, from = 1_000_000_000L + 90 * hz120, app = 30_000_000L)
        val report = FrameReport.of(calm + spiky)!!
        assertTrue("p90 ${report.appMs}", report.appMs in 2f..30f)
    }

    // ---- Whether Zenith is moving, for the live wallpaper to make way ----

    @Test
    fun `zenith is moving while it draws frame after frame`() {
        val times = (0 until 10).map { it * hz120 }
        assertTrue(LauncherMotion.moving(times, now = 9 * hz120))
    }

    @Test
    fun `a clock ticking over once a second does not make the wallpaper wait`() {
        val times = listOf(0L, 1_000_000_000L, 2_000_000_000L)
        assertFalse(LauncherMotion.moving(times, now = 2_010_000_000L))
    }

    @Test
    fun `a moment after the last frame zenith is still again`() {
        val times = (0 until 30).map { it * hz120 }
        val last = times.last()
        assertTrue(LauncherMotion.moving(times, now = last + 100_000_000L))
        assertFalse(LauncherMotion.moving(times, now = last + 400_000_000L))
    }

    @Test
    fun `the frames it keeps are its last few, whatever came before`() {
        LauncherMotion.forget()
        for (i in 0 until 200) LauncherMotion.record(i * hz120)
        assertTrue(LauncherMotion.moving(now = 199 * hz120))
        assertFalse(LauncherMotion.moving(now = 199 * hz120 + 1_000_000_000L))
        LauncherMotion.forget()
        assertFalse(LauncherMotion.moving(now = 199 * hz120))
    }

    // ---- What the readout says ----

    @Test
    fun `the readout says how it ran, which part is slow and whether zenith's code is ready`() {
        val report = FrameReport.of(frames(60, 50_000_000L, app = 40_000_000L, drawing = 6_000_000L, gpu = 9_000_000L))!!
        val lines = FrameCheck.lines(report, CompiledCode.WAITING, hz = 120, movingWallpaper = true)
        val text = lines.joinToString("\n")
        assertTrue(text, text.contains("20 frames a second"))
        assertTrue(text, text.contains("0% on time"))
        assertTrue(text, text.contains("Zenith's own work"))
        assertTrue(text, text.contains("40 ms"))
        assertTrue(text, text.contains("120 Hz"))
        assertTrue(text, text.contains("charg"))
        assertTrue(text, text.contains("moving wallpaper"))
    }

    @Test
    fun `with nothing measured yet the readout says how to get it`() {
        val text = FrameCheck.lines(null, CompiledCode.READY, hz = 120, movingWallpaper = false).joinToString("\n")
        assertTrue(text, text.contains("Swipe"))
        assertTrue(text, text.contains("ready"))
        assertFalse(text, text.contains("moving wallpaper"))
    }

    @Test
    fun `compiled code is ready, queued code waits for the charger, and anything else is unknown`() {
        assertEquals(CompiledCode.READY, CompiledCode.of(compiled = true, queued = false, known = true))
        assertEquals(CompiledCode.READY, CompiledCode.of(compiled = true, queued = true, known = true))
        assertEquals(CompiledCode.WAITING, CompiledCode.of(compiled = false, queued = true, known = true))
        assertEquals(CompiledCode.NOT, CompiledCode.of(compiled = false, queued = false, known = true))
        assertEquals(CompiledCode.UNKNOWN, CompiledCode.of(compiled = false, queued = false, known = false))
    }
}
