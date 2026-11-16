package com.gabriel.zenithui.data

import com.gabriel.zenithui.data.Performance.Verdict
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformanceTest {

    @Test
    fun `there are three choices, and auto is the one you start with`() {
        assertEquals(listOf("auto", "full", "lite"), Performance.CHOICES)
        assertEquals(Performance.AUTO, ZenithSettings().performance)
        assertEquals(Performance.AUTO, Performance.of("something else"))
        assertEquals(Performance.LITE, Performance.of("lite"))
    }

    @Test
    fun `full and lite are what they say, whatever the phone`() {
        for (verdict in Verdict.entries) {
            assertFalse(Performance.isLite(Performance.FULL, verdict))
            assertTrue(Performance.isLite(Performance.LITE, verdict))
        }
    }

    @Test
    fun `auto is lite only on a phone found to need it, and full until that is known`() {
        assertTrue(Performance.isLite(Performance.AUTO, Verdict.LITE))
        assertFalse(Performance.isLite(Performance.AUTO, Verdict.FULL))
        assertFalse(Performance.isLite(Performance.AUTO, Verdict.UNKNOWN))
        assertFalse(Performance.isLite("nonsense", Verdict.UNKNOWN))
    }

    @Test
    fun `the galaxy a55's exynos 1480 and the other mid-range chips get lite`() {
        // Galaxy A55, A54, A35, A25: Exynos 1480, 1380, 1380, 1280.
        for (chip in listOf("s5e8845", "s5e8835", "S5E8835", "s5e8825", "s5e8535", "exynos850", "exynos9611")) {
            assertEquals(chip, Verdict.LITE, Performance.byChip(chip))
        }
        // Snapdragon 4, 6 and the older 7 series; Dimensity 6000 and 7000; Helio.
        for (chip in listOf("SM4350", "SM6375", "SM7325", "SM7450", "MT6833", "MT6877V/ZA", "MT6789", "MT6768")) {
            assertEquals(chip, Verdict.LITE, Performance.byChip(chip))
        }
        // Unisoc's, by its maker.
        assertEquals(Verdict.LITE, Performance.byChip("T606", manufacturer = "Spreadtrum"))
        assertEquals(Verdict.LITE, Performance.byChip("ums9230", manufacturer = "Unisoc"))
    }

    @Test
    fun `flagship graphics get full`() {
        // Exynos 2200, 2400, 2500; Snapdragon 8 series and the newest 7s; Dimensity 8000 and 9000; Tensor.
        for (chip in listOf("s5e9925", "s5e9945", "s5e9955", "exynos2100", "SM8550", "SM8650", "SM8750", "SM7675", "SM7550",
            "MT6983", "MT6989", "MT6991", "MT6897", "GS201", "Tensor G3", "zuma")) {
            assertEquals(chip, Verdict.FULL, Performance.byChip(chip))
        }
    }

    @Test
    fun `a chip it does not know is left to the frames`() {
        assertEquals(Verdict.UNKNOWN, Performance.byChip(""))
        assertEquals(Verdict.UNKNOWN, Performance.byChip("unknown"))
        assertEquals(Verdict.UNKNOWN, Performance.byChip("Kirin9000"))
    }

    @Test
    fun `a phone short of memory gets lite, whatever its chip`() {
        assertEquals(Verdict.LITE, Performance.byChip("SM8650", lowRam = true))
        assertEquals(Verdict.LITE, Performance.byChip("", totalRamMb = 3_000))
        assertEquals(Verdict.FULL, Performance.byChip("SM8650", totalRamMb = 12_000))
        // Not known is not short.
        assertEquals(Verdict.UNKNOWN, Performance.byChip("", totalRamMb = 0))
    }

    @Test
    fun `the frames say nothing until there are enough of them`() {
        assertEquals(Verdict.UNKNOWN, Performance.byFrames(late = 200, drawn = Performance.FRAMES_TO_JUDGE - 1))
        assertEquals(Verdict.LITE, Performance.byFrames(late = 200, drawn = Performance.FRAMES_TO_JUDGE))
    }

    @Test
    fun `a phone that is late with more than a quarter of its frames gets lite`() {
        val drawn = 400
        assertEquals(Verdict.FULL, Performance.byFrames(late = 0, drawn = drawn))
        assertEquals(Verdict.FULL, Performance.byFrames(late = 100, drawn = drawn))
        assertEquals(Verdict.LITE, Performance.byFrames(late = 101, drawn = drawn))
    }

    @Test
    fun `a frame is late when it takes half as long again as the screen gives it`() {
        // 120 Hz gives a frame 8.3 ms; 60 Hz, 16.7.
        val at120 = 1_000_000_000L / 120
        assertFalse(Performance.isLate(durationNanos = 12_000_000L, intervalNanos = at120))
        assertTrue(Performance.isLate(durationNanos = 13_000_000L, intervalNanos = at120))
        assertFalse(Performance.isLate(durationNanos = 24_000_000L, intervalNanos = 1_000_000_000L / 60))
        assertTrue(Performance.isLate(durationNanos = 26_000_000L, intervalNanos = 1_000_000_000L / 60))
    }

    @Test
    fun `lite keeps the light where it sits and floats nothing`() {
        assertTrue(Performance.lightTilts(tiltShine = true, glass = true, lite = false))
        assertFalse(Performance.lightTilts(tiltShine = true, glass = true, lite = true))
        // As before Lite: only glass catches the light, and only with Tilt shine on.
        assertFalse(Performance.lightTilts(tiltShine = false, glass = true, lite = false))
        assertFalse(Performance.lightTilts(tiltShine = true, glass = false, lite = false))
        assertTrue(Performance.floats(reduced = false, lite = false))
        assertFalse(Performance.floats(reduced = false, lite = true))
        assertFalse(Performance.floats(reduced = true, lite = false))
    }

    @Test
    fun `lite dims what full frosts`() {
        assertEquals(20f, Performance.blurBehind(theme = 20f, lite = false))
        assertEquals(0f, Performance.blurBehind(theme = 20f, lite = true))
        assertEquals(0f, Performance.blurBehind(theme = 0f, lite = false))
    }

    @Test
    fun `lite glass is the glass you tuned, without the colour at its rim`() {
        val yours = GlassTuning(bend = 1.4f, frost = 0.3f, rim = 0.8f, rainbow = 2f, lightAngle = 200f, lightTilt = false)
        assertEquals(yours, Performance.tuning(yours, lite = false))
        assertEquals(yours.copy(rainbow = 0f), Performance.tuning(yours, lite = true))
        assertEquals(0f, Performance.tuning(GlassTuning(), lite = true).rainbow)
    }

    @Test
    fun `the choice is saved, and a settings file from before it reads as auto`() {
        val lite = ZenithSettings(performance = Performance.LITE)
        assertEquals(Performance.LITE, ZenithSettings.fromJson(lite.toJson()).performance)
        val old = ZenithSettings().toJson().apply { remove("performance") }
        assertEquals(Performance.AUTO, ZenithSettings.fromJson(old).performance)
    }

    @Test
    fun `it is about this phone, so a look does not carry it`() {
        assertFalse("performance" in Looks.KEYS)
    }
}
