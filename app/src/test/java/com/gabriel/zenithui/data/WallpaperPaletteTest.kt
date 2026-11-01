package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class WallpaperPaletteTest {

    private fun picture(vararg parts: Pair<Int, Int>): IntArray = parts.flatMap { (colour, count) -> List(count) { colour } }.toIntArray()

    private fun hue(argb: Int) = WallpaperPalette.Hsl.of(argb).h

    private fun apart(a: Float, b: Float): Float = abs(a - b).let { if (it > 180f) 360f - it else it }

    @Test
    fun `the liveliest colour there is enough of glows, and the second is another hue`() {
        // Mostly a deep navy sky, a red sun, a little blue sea.
        val picked = WallpaperPalette.fromPixels(picture(0xFF0B1030.toInt() to 700, 0xFFE8402A.toInt() to 200, 0xFF2F7FE0.toInt() to 100))!!
        assertTrue("the accent is the sun's red", apart(hue(picked.accent), hue(0xFFE8402A.toInt())) < 15f)
        assertTrue("the second is the sea's blue", apart(hue(picked.accentAlt), hue(0xFF2F7FE0.toInt())) < 25f)
        assertTrue(picked.dark)
    }

    @Test
    fun `a speck of colour does not make the theme, however bright`() {
        val picked = WallpaperPalette.fromPixels(picture(0xFF3A6B48.toInt() to 9990, 0xFFFF00FF.toInt() to 10))!!
        assertTrue("green, not the magenta speck", apart(hue(picked.accent), hue(0xFF3A6B48.toInt())) < 15f)
    }

    @Test
    fun `an accent is always strong enough to glow and never too dark or pale to read`() {
        for (colour in listOf(0xFF1A0505, 0xFFFFE0E0, 0xFF556B2F, 0xFF101060)) {
            val picked = WallpaperPalette.fromPixels(picture(colour.toInt() to 100))!!
            val hsl = WallpaperPalette.Hsl.of(picked.accent)
            assertTrue("lightness of ${colour.toString(16)}: ${hsl.l}", hsl.l in 0.5f..0.7f)
            assertTrue("saturation of ${colour.toString(16)}: ${hsl.s}", hsl.s >= 0.53f)
        }
    }

    @Test
    fun `a picture with no colour gets a cool violet that goes with any grey`() {
        val picked = WallpaperPalette.fromPixels(picture(0xFF202020.toInt() to 500, 0xFF808080.toInt() to 300, 0xFFE0E0E0.toInt() to 200))!!
        assertEquals(247f, hue(picked.accent), 3f)
        assertTrue(apart(hue(picked.accent), hue(picked.accentAlt)) > 30f)
    }

    @Test
    fun `a dark picture makes a dark palette and a pale one a light one`() {
        val dark = WallpaperPalette.fromPixels(picture(0xFF0A0A14.toInt() to 900, 0xFF3355AA.toInt() to 100))!!
        val pale = WallpaperPalette.fromPixels(picture(0xFFF4EFE6.toInt() to 900, 0xFFE09A3C.toInt() to 100))!!
        assertTrue(dark.dark)
        assertFalse(pale.dark)
        // The backdrop is the picture's main colour, kept deep for a dark one and pale for a light one.
        assertTrue(WallpaperPalette.Hsl.of(dark.backdrop).l <= 0.13f)
        assertTrue(WallpaperPalette.Hsl.of(pale.backdrop).l >= 0.89f)
    }

    @Test
    fun `reds either side of the top of the wheel are one red`() {
        // 350 degrees and 10 degrees average to red, not to the cyan on the far side.
        val picked = WallpaperPalette.fromPixels(picture(0xFFE0284A.toInt() to 500, 0xFFE04A28.toInt() to 500))!!
        assertTrue(apart(hue(picked.accent), 0f) < 15f)
    }

    @Test
    fun `a picture of nothing gives nothing`() {
        assertNull(WallpaperPalette.fromPixels(IntArray(0)))
        assertNull(WallpaperPalette.fromPixels(IntArray(100) { 0x00FF0000 }))
        assertNull(WallpaperPalette.pick(emptyList()))
    }

    @Test
    fun `Android's own three colours make a palette too`() {
        val picked = WallpaperPalette.pick(listOf(0xFF14213D.toInt() to 0.6f, 0xFFFCA311.toInt() to 0.25f, 0xFF3FA7D6.toInt() to 0.15f))
        assertNotNull(picked)
        assertTrue(picked!!.dark)
        assertTrue("amber glows", apart(hue(picked.accent), hue(0xFFFCA311.toInt())) < 15f)
    }

    @Test
    fun `colours go round to HSL and back`() {
        for (colour in listOf(0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFF7C6CFF, 0xFF3ED2FF, 0xFF808080, 0xFFFFFFFF, 0xFF000000)) {
            assertEquals(colour.toInt(), WallpaperPalette.Hsl.of(colour.toInt()).argb())
        }
    }
}
