package com.gabriel.zenithui.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemesTest {

    @Test
    fun `there are four themes, and Liquid Glass is the one Zenith starts in`() {
        assertEquals(listOf("glass", "clean", "neon", "retro"), Themes.all.map { it.id })
        assertEquals(Themes.LiquidGlass.id, Themes.DEFAULT)
        assertEquals(Themes.DEFAULT, ZenithSettings().theme)
        // Settings saved before there were themes are in Liquid Glass, as they always were.
        assertEquals(Themes.DEFAULT, ZenithSettings.fromJson(JSONObject().put("paletteId", "sunset")).theme)
    }

    @Test
    fun `a theme is found by its id, and one Zenith doesn't know is Liquid Glass`() {
        Themes.all.forEach { assertEquals(it, Themes.byId(it.id)) }
        assertEquals(Themes.LiquidGlass, Themes.byId("disco"))
        assertEquals(Themes.LiquidGlass, Themes.byId(""))
    }

    @Test
    fun `your theme is saved with the rest of your settings`() {
        val saved = ZenithSettings(theme = "retro").toJson()
        assertEquals("retro", ZenithSettings.fromJson(saved).theme)
    }

    @Test
    fun `a theme you are trying is shown until Zenith starts again, then your favourite is`() {
        assertEquals(Themes.Neon, Themes.active(favourite = "neon", trying = null))
        assertEquals(Themes.Retro, Themes.active(favourite = "neon", trying = "retro"))
        // Trying your favourite is just your favourite.
        assertEquals(Themes.Neon, Themes.active(favourite = "neon", trying = "neon"))
    }

    @Test
    fun `Neon is always dark, and the others follow light and dark`() {
        assertTrue(Themes.isDark(Themes.Neon, appearanceDark = false))
        listOf(Themes.LiquidGlass, Themes.Clean, Themes.Retro).forEach { theme ->
            assertTrue(theme.id, Themes.isDark(theme, appearanceDark = true))
            assertFalse(theme.id, Themes.isDark(theme, appearanceDark = false))
        }
    }

    @Test
    fun `Liquid Glass is glass, and the others are drawn in their own material`() {
        assertEquals(ThemeMaterial.GLASS, Themes.LiquidGlass.material)
        assertEquals(ThemeMaterial.FLAT, Themes.Clean.material)
        assertEquals(ThemeMaterial.NEON, Themes.Neon.material)
        assertEquals(ThemeMaterial.RETRO, Themes.Retro.material)
        // Liquid Glass keeps every corner as it is drawn; Retro's are cut, and never a pill.
        assertEquals(1f, Themes.LiquidGlass.corners, 0f)
        assertTrue(Themes.Retro.cut && !Themes.Retro.keepPills)
    }

    @Test
    fun `words are easy to read on every theme, light and dark`() {
        Themes.all.forEach { theme ->
            listOf(false, true).forEach { dark ->
                val c = theme.colours(dark)
                val where = "${theme.id} ${if (dark) "dark" else "light"}"
                // On a panel on the page, and on the page itself.
                val page = c.backdrop.first()
                val panel = ThemeContrast.over(c.surface, page)
                assertTrue("$where words on a panel", ThemeContrast.ratio(c.ink, panel) >= 7.0)
                assertTrue("$where second lines on a panel", ThemeContrast.ratio(ThemeContrast.withAlpha(c.ink, ThemeColours.MUTED), panel) >= 4.5)
                assertTrue("$where words on the page", ThemeContrast.ratio(c.ink, page) >= 7.0)
                // The button that does the thing.
                assertTrue("$where words on the accent", ThemeContrast.ratio(c.onAccent, c.accent) >= 3.0)
            }
        }
    }

    @Test
    fun `a theme is shared whole, and what is written is what is read back`() {
        Themes.all.forEach { theme ->
            assertEquals(theme, Theme.fromJson(JSONObject(theme.toJson().toString())))
        }
    }

    @Test
    fun `a theme from somewhere else never breaks Zenith`() {
        // Nothing at all: Liquid Glass under the name it came with, or none.
        val empty = Theme.fromJson(JSONObject())
        assertEquals(Themes.LiquidGlass.material, empty.material)
        assertEquals(Themes.LiquidGlass.dark, empty.dark)
        // Nonsense in every field is kept within what Zenith can draw.
        val odd = Theme.fromJson(
            JSONObject()
                .put("id", "odd").put("name", "x".repeat(200)).put("material", "plasma").put("tone", "sideways")
                .put("corners", 99).put("cornerCap", -4).put("edgeWidth", 50).put("glow", 1e9).put("blurBehind", -1)
                .put("dark", JSONObject().put("ink", "not a colour").put("accent", "#FF00FF").put("backdrop", "nope")),
        )
        assertEquals(ThemeMaterial.GLASS, odd.material)
        assertEquals(ThemeTone.FOLLOWS, odd.tone)
        assertTrue(odd.name.length <= Theme.NAME_MOST)
        assertTrue(odd.corners in Theme.CORNERS)
        assertEquals(0f, odd.cornerCap, 0f)
        assertTrue(odd.edgeWidth in Theme.EDGE)
        assertTrue(odd.glow in Theme.GLOW)
        assertEquals(0f, odd.blurBehind, 0f)
        // What could be read was; the rest is Liquid Glass's.
        assertEquals(0xFFFF00FF, odd.dark.accent)
        assertEquals(Themes.LiquidGlass.dark.ink, odd.dark.ink)
        assertEquals(Themes.LiquidGlass.dark.backdrop, odd.dark.backdrop)
    }

    @Test
    fun `colours are written as hex, and read with or without their alpha`() {
        assertEquals("#CC0D0A1C", ThemeColours.hex(0xCC0D0A1C))
        assertEquals(0xFF336699, ThemeColours.parse("#336699"))
        assertEquals(0x80336699, ThemeColours.parse("#80336699"))
        assertEquals(null, ThemeColours.parse("336699"))
        assertEquals(null, ThemeColours.parse("#12345"))
        assertNotNull(ThemeColours.parse("#ffffff"))
    }

    @Test
    fun `contrast is measured as the web measures it`() {
        assertEquals(21.0, ThemeContrast.ratio(0xFF000000, 0xFFFFFFFF), 0.01)
        assertEquals(1.0, ThemeContrast.ratio(0xFF777777, 0xFF777777), 0.001)
        // Half-white over black is a mid grey.
        assertEquals(0xFF808080, ThemeContrast.over(0x80FFFFFF, 0xFF000000))
    }
}
