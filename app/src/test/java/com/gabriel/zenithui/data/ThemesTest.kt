package com.gabriel.zenithui.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemesTest {

    @Test
    fun `there are four themes, and Liquid Glass is the one Zenith starts in`() {
        assertEquals(listOf("glass", "aurora", "frost", "clean"), Themes.all.map { it.id })
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
        val saved = ZenithSettings(theme = "frost").toJson()
        assertEquals("frost", ZenithSettings.fromJson(saved).theme)
    }

    @Test
    fun `Neon and Retro are gone, and a phone that was in Neon is in Aurora, and one in Retro in Liquid Glass`() {
        assertEquals(Themes.Aurora.id, Themes.successor("neon"))
        assertEquals(Themes.LiquidGlass.id, Themes.successor("retro"))
        // Asked for by its old name, a theme that went is the one after it.
        assertEquals(Themes.Aurora, Themes.byId("neon"))
        assertEquals(Themes.LiquidGlass, Themes.byId("retro"))
        // Settings saved while they were here, a backup or a Look among them, read as the theme after.
        assertEquals("aurora", ZenithSettings.fromJson(JSONObject().put("theme", "neon")).theme)
        assertEquals("glass", ZenithSettings.fromJson(JSONObject().put("theme", "retro")).theme)
        // Every theme there is keeps its id, and so does one of your own.
        Themes.all.forEach { assertEquals(it.id, Themes.successor(it.id)) }
        assertEquals("mine", Themes.successor("mine"))
        assertEquals("mine", ZenithSettings.fromJson(JSONObject().put("theme", "mine")).theme)
    }

    @Test
    fun `a theme you are trying is shown until Zenith starts again, then your favourite is`() {
        assertEquals(Themes.Aurora, Themes.active(favourite = "aurora", trying = null))
        assertEquals(Themes.Frost, Themes.active(favourite = "aurora", trying = "frost"))
        // Trying your favourite is just your favourite.
        assertEquals(Themes.Aurora, Themes.active(favourite = "aurora", trying = "aurora"))
    }

    @Test
    fun `Aurora is always dark, and the others follow light and dark`() {
        assertTrue(Themes.isDark(Themes.Aurora, appearanceDark = false))
        listOf(Themes.LiquidGlass, Themes.Frost, Themes.Clean).forEach { theme ->
            assertTrue(theme.id, Themes.isDark(theme, appearanceDark = true))
            assertFalse(theme.id, Themes.isDark(theme, appearanceDark = false))
        }
    }

    @Test
    fun `every theme but Clean is Liquid Glass, and a material Zenith no longer has is glass too`() {
        listOf(Themes.LiquidGlass, Themes.Aurora, Themes.Frost).forEach { assertEquals(it.id, ThemeMaterial.GLASS, it.material) }
        assertEquals(ThemeMaterial.FLAT, Themes.Clean.material)
        // A theme written while Neon and Retro were here, shared or kept, is glass.
        assertEquals(ThemeMaterial.GLASS, ThemeMaterial.byId("neon"))
        assertEquals(ThemeMaterial.GLASS, ThemeMaterial.byId("retro"))
        // Glass keeps every corner as it is drawn, and every pill a pill.
        listOf(Themes.LiquidGlass, Themes.Aurora, Themes.Frost).forEach { theme ->
            assertEquals(theme.id, 1f, theme.corners, 0f)
            assertEquals(theme.id, 0f, theme.cornerCap, 0f)
            assertTrue(theme.id, !theme.cut && theme.keepPills)
        }
    }

    @Test
    fun `Liquid Glass's glass is Apple's, a grey veil with no colour in it and white light on its edge`() {
        assertEquals(ThemeGlass(veil = 0xFF3A3A3C, rim = 0xFFFFFFFF, rimFar = 0xFFFFFFFF), Themes.LiquidGlass.glass)
    }

    @Test
    fun `Aurora and Frost are glass of their own, a veil with a little colour and two colours of light round the edge`() {
        listOf(Themes.Aurora, Themes.Frost).forEach { theme ->
            assertNotEquals(theme.id, Themes.LiquidGlass.glass.veil, theme.glass.veil)
            assertNotEquals(theme.id, theme.glass.rim, theme.glass.rimFar)
        }
        assertNotEquals(Themes.Aurora.glass, Themes.Frost.glass)
    }

    @Test
    fun `a glass theme's edge catches light, not paint, and its veil keeps the glass clear`() {
        val grey = ThemeContrast.luminance(Themes.LiquidGlass.glass.veil)
        Themes.all.filter { it.material == ThemeMaterial.GLASS }.forEach { theme ->
            // Light, as bright as a colour can be and still have colour in it where the edge faces
            // the lamp, and at least a third of white's on the far side: added to the glass where
            // its edge catches it, a hairline of light, never a coloured line drawn round it, which
            // is what Neon's edge was.
            assertTrue("${theme.id} rim", ThemeContrast.luminance(theme.glass.rim) >= 0.6)
            assertTrue("${theme.id} far rim", ThemeContrast.luminance(theme.glass.rimFar) >= 0.33)
            // As deep as Liquid Glass's grey, within a little: a thin veil over the wallpaper,
            // never a coloured card laid on it nor a smoke that darkens it.
            assertEquals("${theme.id} veil", grey, ThemeContrast.luminance(theme.glass.veil), 0.015)
        }
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
                assertTrue("$where second lines on a panel", ThemeContrast.ratio(ThemeContrast.withAlpha(c.ink, ThemeColours.muted(dark)), panel) >= 4.5)
                // Testers found the subtitles hard to pick out against the dark: in Dark they are
                // held to seven to one, what the web asks of small print at its strictest.
                if (dark) {
                    assertTrue("$where second lines on a panel in the dark", ThemeContrast.ratio(ThemeContrast.withAlpha(c.ink, ThemeColours.muted(dark)), panel) >= 7.0)
                    assertTrue("$where second lines on the page in the dark", ThemeContrast.ratio(ThemeContrast.withAlpha(c.ink, ThemeColours.muted(dark)), page) >= 7.0)
                }
                assertTrue("$where words on the page", ThemeContrast.ratio(c.ink, page) >= 7.0)
                // The button that does the thing.
                assertTrue("$where words on the accent", ThemeContrast.ratio(c.onAccent, c.accent) >= 3.0)
            }
        }
    }

    @Test
    fun `second lines are stronger in the dark than in the light, and still a step behind the words`() {
        assertEquals(0.74f, ThemeColours.muted(dark = false), 0f)
        assertTrue(ThemeColours.muted(dark = true) > ThemeColours.muted(dark = false))
        assertTrue(ThemeColours.muted(dark = true) <= 0.86f)
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
                .put("corners", 99).put("cornerCap", -4).put("edgeWidth", 50).put("blurBehind", -1)
                .put("dark", JSONObject().put("ink", "not a colour").put("accent", "#FF00FF").put("backdrop", "nope"))
                .put("glass", JSONObject().put("veil", "#1F3A40").put("rim", "bright")),
        )
        assertEquals(ThemeMaterial.GLASS, odd.material)
        assertEquals(ThemeTone.FOLLOWS, odd.tone)
        assertTrue(odd.name.length <= Theme.NAME_MOST)
        assertTrue(odd.corners in Theme.CORNERS)
        assertEquals(0f, odd.cornerCap, 0f)
        assertTrue(odd.edgeWidth in Theme.EDGE)
        assertEquals(0f, odd.blurBehind, 0f)
        // What could be read was; the rest is Liquid Glass's.
        assertEquals(0xFFFF00FF, odd.dark.accent)
        assertEquals(Themes.LiquidGlass.dark.ink, odd.dark.ink)
        assertEquals(Themes.LiquidGlass.dark.backdrop, odd.dark.backdrop)
        assertEquals(0xFF1F3A40, odd.glass.veil)
        assertEquals(Themes.LiquidGlass.glass.rim, odd.glass.rim)
        assertEquals(Themes.LiquidGlass.glass.rimFar, odd.glass.rimFar)
        // No glass at all is Liquid Glass's.
        assertEquals(Themes.LiquidGlass.glass, empty.glass)
    }

    @Test
    fun `colours are written as hex, and read with or without their alpha`() {
        assertEquals("#CC0D0A1C", ThemeColours.hex(0xCC0D0A1C))
        assertEquals("#FF3A3A3C", ThemeColours.hex(Themes.LiquidGlass.glass.veil))
        assertEquals(0xFF336699, ThemeColours.parse("#336699"))
        assertEquals(0x80336699, ThemeColours.parse("#80336699"))
        assertEquals(null, ThemeColours.parse("336699"))
        assertEquals(null, ThemeColours.parse("#12345"))
        assertNotNull(ThemeColours.parse("#ffffff"))
    }

    @Test
    fun `contrast is measured as the web measures it`() {
        assertEquals(21.0, ThemeContrast.ratio(0xFF000000, 0xFFFFFFFF), 0.01)
        assertEquals(1.0, ThemeContrast.luminance(0xFFFFFFFF), 1e-9)
        assertEquals(0.0, ThemeContrast.luminance(0xFF000000), 1e-9)
        assertEquals(1.0, ThemeContrast.ratio(0xFF777777, 0xFF777777), 0.001)
        // Half-white over black is a mid grey.
        assertEquals(0xFF808080, ThemeContrast.over(0x80FFFFFF, 0xFF000000))
    }
}
