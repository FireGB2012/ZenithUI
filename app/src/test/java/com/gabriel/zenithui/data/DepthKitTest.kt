package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/**
 * The colour sums the Depth wallpapers are painted with. The pictures themselves are checked on a
 * device (`WallpaperArtTest`); these are the rules under them, which need no phone.
 */
class DepthKitTest {

    private fun degrees(d: Double) = (d * PI / 180.0).toFloat()

    @Test
    fun `screen values and light turn into each other and back`() {
        assertEquals(0f, Oklab.toLinear(0), 0f)
        assertEquals(1f, Oklab.toLinear(255), 1e-6f)
        for (c in 0..255) assertEquals(c, Oklab.toChannel(Oklab.toLinear(c)))
        // Half the light is not half the value: on the screen's curve it is 188 of 255.
        assertEquals(188, Oklab.toChannel(0.5f))
        // Light past either end is held there.
        assertEquals(0, Oklab.toChannel(-0.3f))
        assertEquals(255, Oklab.toChannel(1.7f))
    }

    @Test
    fun `a colour comes back out of OKLab as it went in`() {
        val out = FloatArray(3)
        for (argb in listOf(0xFF7C6CFF, 0xFF3ED2FF, 0xFFFF2E4D, 0xFF808080, 0xFFFFD43B, 0xFF0A0B18)) {
            val light = Oklab.linearOf(argb.toInt())
            val lab = Oklab.lab(light[0], light[1], light[2])
            Oklab.linear(lab[0], lab[1], lab[2], out)
            for (i in 0..2) assertEquals("%08X".format(argb), light[i], out[i], 1e-4f)
        }
    }

    @Test
    fun `white is all lightness, and a grey has no colour to it`() {
        val white = Spectrum.lch(0xFFFFFFFF.toInt())
        assertEquals(1f, white[0], 1e-3f)
        assertEquals(0f, white[1], 1e-3f)
        assertTrue(Spectrum.lch(0xFF808080.toInt())[1] < 1e-3f)
    }

    @Test
    fun `a colour more vivid than the screen can show gives up chroma, never its hue or lightness`() {
        val out = FloatArray(3)
        Oklab.fromLch(0.7f, 0.5f, 0.5f, out)
        out.forEach { assertTrue("$it", it in 0f..1f) }
        val lab = Oklab.lab(out[0], out[1], out[2])
        assertEquals(0.7f, lab[0], 0.01f)
        assertEquals(0.5f, atan2(lab[2], lab[1]), 0.02f)
        assertTrue(hypot(lab[1], lab[2]) < 0.5f)
    }

    @Test
    fun `the spectrum runs from one accent to the other, carried a quarter past each end`() {
        val accent = 0xFF7C6CFF.toInt()
        val alt = 0xFF3ED2FF.toInt()
        val spectrum = Spectrum(accent, alt)
        val lch = FloatArray(3)
        // A sixth of the way along is the first accent, five sixths the second.
        for ((t, colour) in listOf(1f / 6f to accent, 5f / 6f to alt)) {
            spectrum.at(t, lch)
            val expected = Spectrum.lch(colour)
            assertEquals(expected[0], lch[0], 1e-4f)
            assertEquals(expected[1], lch[1], 1e-4f)
            assertEquals(0f, Spectrum.wrap(lch[2] - expected[2]), 1e-4f)
        }
        // Past the ends the hue keeps turning, so two accents close together still give a range.
        val start = FloatArray(3).also { spectrum.at(0f, it) }
        val end = FloatArray(3).also { spectrum.at(1f, it) }
        val between = abs(Spectrum.wrap(Spectrum.lch(alt)[2] - Spectrum.lch(accent)[2]))
        assertEquals(between * 1.5f, abs(Spectrum.wrap(end[2] - start[2])), 1e-3f)
    }

    @Test
    fun `the spectrum goes round the colour wheel the short way`() {
        // Cherry red to violet: a short step through pink one way, most of the wheel through green the other.
        val spectrum = Spectrum(0xFFFF2E4D.toInt(), 0xFF7B4DFF.toInt())
        val middle = FloatArray(3).also { spectrum.at(0.5f, it) }
        assertTrue(abs(Spectrum.wrap(middle[2] - degrees(334.0))) < degrees(10.0))
    }

    @Test
    fun `its middle stays vivid where the same two colours mixed as paint go grey`() {
        val blue = 0xFF0A6CFF.toInt()
        val yellow = 0xFFFFD43B.toInt()
        val middle = FloatArray(3)
        Spectrum(blue, yellow).ramp().get(0.5f, middle)
        val round = Oklab.lab(middle[0], middle[1], middle[2])
        assertTrue(hypot(round[1], round[2]) > 0.1f)
        val b = Oklab.linearOf(blue)
        val y = Oklab.linearOf(yellow)
        val mixed = Oklab.lab((b[0] + y[0]) / 2f, (b[1] + y[1]) / 2f, (b[2] + y[2]) / 2f)
        assertTrue(hypot(mixed[1], mixed[2]) < 0.05f)
    }

    @Test
    fun `a grey accent takes the other's hue, and two greys stay grey`() {
        val red = 0xFFFF2E4D.toInt()
        val half = Spectrum(0xFF808080.toInt(), red)
        val start = FloatArray(3).also { half.at(0f, it) }
        assertEquals(0f, Spectrum.wrap(start[2] - Spectrum.lch(red)[2]), 1e-4f)
        // Graphite's two greys, both a little blue: every colour along it is no more colourful
        // than the bluer of the two, and the hue does not turn.
        val pale = 0xFFE6E8EF.toInt()
        val steel = 0xFF9AA0AE.toInt()
        val most = maxOf(Spectrum.lch(pale)[1], Spectrum.lch(steel)[1])
        val graphite = Spectrum(pale, steel).ramp()
        val c = FloatArray(3)
        for (i in 0..20) {
            graphite.get(i / 20f, c)
            val lab = Oklab.lab(c[0], c[1], c[2])
            assertTrue("$i", hypot(lab[1], lab[2]) <= most + 1e-3f)
            assertEquals("$i", 0f, Spectrum.wrap(atan2(lab[2], lab[1]) - Spectrum.lch(pale)[2]), 0.02f)
        }
    }

    @Test
    fun `a ramp gives its ends exactly and blends between them`() {
        val ramp = Ramp(floatArrayOf(0f, 0f, 0f, 1f, 0.5f, 0.25f))
        val c = FloatArray(3)
        ramp.get(0f, c)
        assertEquals(listOf(0f, 0f, 0f), c.toList())
        ramp.get(1f, c)
        assertEquals(listOf(1f, 0.5f, 0.25f), c.toList())
        ramp.get(0.5f, c)
        assertEquals(listOf(0.5f, 0.25f, 0.125f), c.toList())
        // Asked past an end, it gives the end.
        ramp.get(-1f, c)
        assertEquals(0f, c[0])
        assertEquals(1f, ramp.red(3f))
        assertEquals(0.25f, ramp.green(0.5f))
        assertEquals(0.125f, ramp.blue(0.5f))
    }

    @Test
    fun `the sky runs from the backdrop's top to its foot as light, and dusk to its middle`() {
        val kit = DepthKit(WallColors(0xFF7C6CFF.toInt(), 0xFF3ED2FF.toInt(), 0xFF000000.toInt(), 0xFF808080.toInt(), 0xFFFFFFFF.toInt()))
        val c = FloatArray(4)
        kit.sky(0f, c)
        assertEquals(0f, c[0], 1e-6f)
        kit.sky(1f, c, 1)
        assertEquals(1f, c[1], 1e-6f)
        assertEquals(1f, c[3], 1e-6f)
        // Halfway is half the light, which the screen shows lighter than its middle value.
        kit.sky(0.5f, c)
        assertEquals(0.5f, c[0], 1e-6f)
        kit.sky(2f, c)
        assertEquals(1f, c[0], 1e-6f)
        kit.dusk(1f, c)
        assertEquals(Oklab.toLinear(0x80), c[0], 1e-6f)
        // And the quiet under the icons settles towards the foot.
        val rgb = floatArrayOf(0f, 0f, 0f)
        kit.towardsBase(rgb, 0.25f)
        assertEquals(0.25f, rgb[0], 1e-6f)
    }

    @Test
    fun `smoothstep holds still past its edges, and the dither averages out`() {
        assertEquals(0f, Shade.smoothstep(0.2f, 0.8f, 0.1f))
        assertEquals(1f, Shade.smoothstep(0.2f, 0.8f, 0.9f))
        assertEquals(0.5f, Shade.smoothstep(0.2f, 0.8f, 0.5f), 1e-6f)
        val dither = (0 until 16).map { Shade.dither(it % 4, it / 4) }
        assertEquals(16, dither.toSet().size)
        dither.forEach { assertTrue(it > -0.5f && it < 0.5f) }
        assertEquals(0f, dither.sum(), 1e-5f)
        // It repeats every four pixels each way.
        assertEquals(Shade.dither(1, 2), Shade.dither(5, 6))
    }

    @Test
    fun `highlights are the powers they say`() {
        assertEquals(0.5.pow(64).toFloat(), Shade.pow64(0.5f), 1e-12f)
        assertEquals(0.9.pow(16).toFloat(), Shade.pow16(0.9f), 1e-6f)
        assertEquals(0.9.pow(8).toFloat(), Shade.pow8(0.9f), 1e-6f)
        assertEquals(0f, Shade.pow8(-1f))
        val h = Shade.half(Shade.unit(0f, 0f, 1f))
        assertEquals(listOf(0f, 0f, 1f), h.toList())
    }

    private fun Double.pow(n: Int): Double = Math.pow(this, n.toDouble())
}
