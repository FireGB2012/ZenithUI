package com.gabriel.zenithui.ui.theme

import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.gabriel.zenithui.data.ZenithSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PaletteToneTest {

    private val every = Palettes.all + Palettes.aura

    @Test
    fun `toning a palette to its own appearance changes nothing`() {
        every.forEach { assertSame(it, it.toned(it.dark)) }
    }

    @Test
    fun `every palette has a light and a dark backdrop and keeps its accents in both`() {
        for (palette in every) {
            val light = palette.toned(dark = false)
            val dark = palette.toned(dark = true)
            assertFalse(palette.name, light.dark)
            assertTrue(palette.name, dark.dark)
            assertTrue("${palette.name} in light", light.backdrop.all { it.luminance() > 0.7f })
            assertTrue("${palette.name} in dark", dark.backdrop.all { it.luminance() < 0.12f })
            assertEquals(palette.accent, light.accent)
            assertEquals(palette.accentAlt, dark.accentAlt)
        }
    }

    @Test
    fun `a backdrop made for the other tone is tinted with the palette's own accents`() {
        val sunset = Palettes.Sunset.toned(dark = false).backdrop
        val aurora = Palettes.Aurora.toned(dark = false).backdrop
        assertNotEquals(sunset, aurora)
        assertNotEquals(Palettes.Frost.backdrop, sunset)
        assertNotEquals(Palettes.Midnight.backdrop, Palettes.Frost.toned(dark = true).backdrop)
    }

    @Test
    fun `a wallpaper is painted in the theme's palette unless it was pinned to another`() {
        val sunset = ZenithSettings(paletteId = "sunset")
        assertEquals(Palettes.Sunset.toned(dark = false), Palettes.forWallpaper(sunset, dark = false))
        assertEquals(Palettes.Sunset, Palettes.forWallpaper(sunset, dark = true))
        val pinned = sunset.copy(wallpaperPalette = "aurora")
        assertEquals(Palettes.Aurora, Palettes.forWallpaper(pinned, dark = true))
        assertEquals(Palettes.Aurora.toned(dark = false), Palettes.forWallpaper(pinned, dark = false))
    }

    @Test
    fun `a wallpaper pinned to the palette you made is painted in it`() {
        val made = ZenithSettings(paletteId = "midnight", wallpaperPalette = Palettes.CUSTOM, customAccent = 0xFF112233)
        assertEquals(Palettes.custom(made).accent, Palettes.forWallpaper(made, dark = true).accent)
    }

    @Test
    fun `wall colours carry the palette's five colours and its tone`() {
        val palette = Palettes.Sakura.toned(dark = false)
        val colors = palette.wallColors()
        assertEquals(palette.accent.toArgb(), colors.accent)
        assertEquals(palette.accentAlt.toArgb(), colors.accentAlt)
        assertEquals(palette.backdrop[0].toArgb(), colors.deep)
        assertEquals(palette.backdrop[1].toArgb(), colors.mid)
        assertEquals(palette.backdrop[2].toArgb(), colors.base)
        assertFalse(colors.dark)
        assertTrue(Palettes.Sakura.wallColors().dark)
    }
}
