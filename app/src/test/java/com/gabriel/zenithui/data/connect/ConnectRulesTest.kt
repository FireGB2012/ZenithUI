package com.gabriel.zenithui.data.connect

import com.gabriel.zenithui.BuildConfig
import com.gabriel.zenithui.data.AppCell
import com.gabriel.zenithui.data.IconStyles
import com.gabriel.zenithui.data.Look
import com.gabriel.zenithui.data.Looks
import com.gabriel.zenithui.data.Themes
import com.gabriel.zenithui.data.WallStyle
import com.gabriel.zenithui.data.WidgetLooks
import com.gabriel.zenithui.ui.theme.Palettes
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ConnectRulesTest {

    /** Settings as a Look keeps them, and one that it does not (your name). */
    private val mine = JSONObject()
        .put("theme", "clean")
        .put("appearance", "light")
        .put("wallpaperStyle", "Calm")
        .put("wallpaperPalette", "lavender")
        .put("iconStyle", "original")
        .put("widgetLook", "glass")
        .put("columns", 4)
        .put("rows", 6)
        .put("openAnimation", "colour")
        .put("userName", "Gabriel")
        .toString()

    /** Home screens as a Look keeps them: real items, written the way Zenith writes them. */
    private fun app(id: String, page: Int) = AppCell(id = id, page = page, col = 0, row = 0, packageName = "their.app", className = "their.app.Main")

    private val myLayout = JSONArray().put(app("a1", 0).toJson()).toString()

    private val theirLayout = JSONArray()
        .put(app("b1", 0).toJson())
        .put(app("b2", 1).toJson())
        .toString()

    private val theirSettings = JSONObject()
        .put("theme", "aurora")
        .put("appearance", "dark")
        .put("wallpaperStyle", "Dunes")
        .put("wallpaperPalette", "sunset")
        .put("iconStyle", "glass")
        .put("widgetLook", "colour")
        .put("columns", 5)
        .put("rows", 7)
        .put("openAnimation", "glass")
        .toString()

    private fun shared(code: String = "BCD234", source: SetupSource = SetupSource.CODE) =
        ConnectSetup.of(Look("x1", "Dusk", theirSettings, theirLayout, savedAt = 1L), source, code)

    private val pick = ConnectPicks.all.first { it.id == "pick-golden-hour" }

    private fun JSONObject.str(key: String) = optString(key)

    // ------------------------------------------------------------------ layers

    @Test
    fun `every setting a Look keeps is in exactly one layer, and nothing else is`() {
        for (key in Looks.KEYS) {
            val holders = ConnectLayers.keys.filterValues { key in it }.keys
            assertEquals("$key is in $holders", 1, holders.size)
        }
        val all = ConnectLayers.keys.values.flatten()
        assertEquals(all.size, all.toSet().size)
        assertEquals(Looks.KEYS.toSet(), all.toSet())
    }

    @Test
    fun `a style has its wallpaper, theme, icons and widgets, and no pages to give`() {
        assertEquals(setOf(Layer.WALLPAPER, Layer.THEME, Layer.ICONS, Layer.WIDGETS), ConnectLayers.available(pick))
        assertFalse(ConnectLayers.hasLayout(pick))
    }

    @Test
    fun `a Look with pages of its own has the home screen to give, and its grid with it`() {
        val look = shared()
        assertTrue(ConnectLayers.hasLayout(look))
        assertTrue(Layer.LAYOUT in ConnectLayers.available(look))
        // The same settings with no pages: the grid is not its to give.
        assertFalse(Layer.LAYOUT in ConnectLayers.available(look.copy(layout = "[]")))
    }

    @Test
    fun `a photo wallpaper does not travel, so a Look saved over one has no wallpaper to give`() {
        val photo = shared().copy(settings = JSONObject(theirSettings).put("wallpaperStyle", "").toString())
        assertFalse(Layer.WALLPAPER in ConnectLayers.available(photo))
    }

    @Test
    fun `somebody else's Look starts without their pages and their feel, yours and Zenith's with everything`() {
        assertEquals(setOf(Layer.WALLPAPER, Layer.THEME, Layer.ICONS, Layer.WIDGETS), ConnectLayers.startingOn(shared()))
        assertEquals(setOf(Layer.WALLPAPER, Layer.THEME, Layer.ICONS, Layer.WIDGETS), ConnectLayers.startingOn(shared(source = SetupSource.FEATURED)))
        val own = shared(source = SetupSource.MINE)
        assertEquals(ConnectLayers.available(own), ConnectLayers.startingOn(own))
        assertEquals(ConnectLayers.available(pick), ConnectLayers.startingOn(pick))
    }

    @Test
    fun `with a photo of your own the wallpaper starts off, as Undo cannot bring a photo back`() {
        assertFalse(Layer.WALLPAPER in ConnectLayers.startingOn(pick, ownPhoto = true))
        assertTrue(ConnectApply.replacesPhoto(pick, setOf(Layer.WALLPAPER), ownPhoto = true))
        assertFalse(ConnectApply.replacesPhoto(pick, setOf(Layer.THEME), ownPhoto = true))
        assertFalse(ConnectApply.replacesPhoto(pick, setOf(Layer.WALLPAPER), ownPhoto = false))
    }

    // ------------------------------------------------------------------ wearing

    @Test
    fun `a style worn over your home screen puts its layers on and keeps your pages and grid`() {
        val worn = ConnectApply.wear(mine, myLayout, pick, ConnectLayers.available(pick))
        val o = JSONObject(worn.settings)
        assertEquals("glass", o.str("theme"))
        assertEquals("dark", o.str("appearance"))
        assertEquals("Dunes", o.str("wallpaperStyle"))
        assertEquals("sunset", o.str("wallpaperPalette"))
        assertEquals("colour", o.str("widgetLook"))
        assertEquals(4, o.getInt("columns"))
        assertEquals("colour", o.str("openAnimation"))
        assertEquals(myLayout, worn.layout)
        assertFalse(worn.fromElsewhere)
    }

    @Test
    fun `a layer left off stays as you have it`() {
        val worn = ConnectApply.wear(mine, myLayout, pick, setOf(Layer.THEME, Layer.ICONS))
        val o = JSONObject(worn.settings)
        assertEquals("glass", o.str("theme"))
        assertEquals("Calm", o.str("wallpaperStyle"))
        assertEquals("lavender", o.str("wallpaperPalette"))
        assertEquals("glass", o.str("widgetLook"))
    }

    @Test
    fun `only what a Look keeps goes on, never your name`() {
        val o = JSONObject(ConnectApply.wear(mine, myLayout, pick, ConnectLayers.available(pick)).settings)
        assertFalse(o.has("userName"))
        o.keys().forEach { assertTrue(it, it in Looks.KEYS) }
    }

    @Test
    fun `taking somebody else's pages takes their grid with them and fits them to this phone`() {
        val look = shared()
        val worn = ConnectApply.wear(mine, myLayout, look, ConnectLayers.available(look))
        assertEquals(theirLayout, worn.layout)
        assertEquals(5, JSONObject(worn.settings).getInt("columns"))
        assertTrue(worn.fromElsewhere)
        // Their style without their pages: yours stay, and so does your grid.
        val styled = ConnectApply.wear(mine, myLayout, look, ConnectLayers.startingOn(look))
        assertEquals(myLayout, styled.layout)
        assertEquals(4, JSONObject(styled.settings).getInt("columns"))
        assertEquals("colour", JSONObject(styled.settings).str("openAnimation"))
        assertFalse(styled.fromElsewhere)
    }

    @Test
    fun `your own Look puts its pages back as they were, not as from elsewhere`() {
        val own = shared(source = SetupSource.MINE)
        val worn = ConnectApply.wear(mine, myLayout, own, ConnectLayers.available(own))
        assertEquals(theirLayout, worn.layout)
        assertFalse(worn.fromElsewhere)
    }

    @Test
    fun `a wallpaper that needs Plus stays yours without it, colours and all`() {
        val worn = ConnectApply.wear(mine, myLayout, pick, ConnectLayers.available(pick), owns = { false })
        val o = JSONObject(worn.settings)
        assertEquals("Calm", o.str("wallpaperStyle"))
        assertEquals("lavender", o.str("wallpaperPalette"))
        assertEquals("glass", o.str("theme"))
        assertTrue(worn.keptWallpaper)
        assertFalse(ConnectApply.wear(mine, myLayout, pick, ConnectLayers.available(pick)).keptWallpaper)
    }

    @Test
    fun `nothing on is nothing to apply`() {
        assertFalse(ConnectApply.canApply(pick, emptySet()))
        assertFalse(ConnectApply.canApply(pick, setOf(Layer.LAYOUT)))
        assertTrue(ConnectApply.canApply(pick, setOf(Layer.ICONS)))
    }

    @Test
    fun `what applying puts on is said in order, and the steps follow it`() {
        val look = shared()
        assertEquals(listOf("Wallpaper", "Theme & glass", "Home screen"), ConnectApply.parts(look, setOf(Layer.LAYOUT, Layer.THEME, Layer.WALLPAPER)))
        assertEquals(listOf("Theme, icons and widgets", "Home screen", "Wallpaper"), ConnectApply.steps(look, setOf(Layer.LAYOUT, Layer.THEME, Layer.WALLPAPER)))
        assertEquals(listOf("Motion & feel"), ConnectApply.steps(look, setOf(Layer.MOTION)))
        assertEquals(listOf("Your setup"), ConnectApply.steps(pick, emptySet()))
    }

    @Test
    fun `a Look is described by what it holds`() {
        assertEquals("Aurora · Dunes wallpaper · glass icons · colour widgets · 2 pages", ConnectApply.describe(theirSettings, theirLayout))
        assertEquals("A Zenith Look", ConnectApply.describe("{}", "[]"))
        assertEquals("Liquid Glass", ConnectApply.themeName("glass"))
        assertEquals("Aurora", ConnectApply.themeName("neon"))
    }

    @Test
    fun `a Look from the server becomes a setup known by its code`() {
        val setup = shared(code = "look-bcd234", source = SetupSource.FEATURED)
        assertEquals("code-BCD234", setup.id)
        assertEquals("BCD234", setup.code)
        assertEquals("LOOK-BCD234", setup.shownCode)
        assertEquals("Featured", setup.by)
        assertEquals("", setup.lookId)
        val own = ConnectSetup.of(Look("m1", "Mine", theirSettings, theirLayout, 1L), SetupSource.MINE)
        assertEquals("mine-m1", own.id)
        assertEquals("m1", own.lookId)
        assertEquals("", own.shownCode)
    }

    // ------------------------------------------------------------------ codes

    @Test
    fun `a code is found however it was typed or pasted`() {
        assertEquals("BCD234", ConnectCodes.clean("LOOK-BCD234"))
        assertEquals("BCD234", ConnectCodes.clean("look bcd 234"))
        assertEquals("BCD234", ConnectCodes.clean("bcd234"))
        assertEquals("BCD234", ConnectCodes.clean("  LOOK-bcd234  "))
        assertEquals("BCD234", ConnectCodes.clean(ConnectCodes.message("Dusk", "BCD234")))
        assertEquals("BCD234", ConnectCodes.clean("zenith://look/BCD234"))
    }

    @Test
    fun `what is not a code is no code`() {
        assertNull(ConnectCodes.clean(null))
        assertNull(ConnectCodes.clean(""))
        assertNull(ConnectCodes.clean("hello"))
        // A and 1 are never in a code: the server leaves out vowels, 0 and 1.
        assertNull(ConnectCodes.clean("LOOK-ABC123"))
        assertNull(ConnectCodes.clean("BCD2345"))
        assertNull(ConnectCodes.clean("LOOK-BCD2345"))
    }

    // ------------------------------------------------------------------ Discover

    @Test
    fun `search finds a setup by its name, its words and its code`() {
        val all = ConnectPicks.all + shared()
        assertEquals(listOf("pick-deep-ocean"), ConnectFeed.list(all, "ocean", Filter.ALL).map { it.id })
        assertEquals(listOf("code-BCD234"), ConnectFeed.list(all, "look-bcd", Filter.ALL).map { it.id })
        assertEquals(
            listOf("pick-liquid-night", "pick-deep-ocean", "pick-sakura"),
            ConnectFeed.list(ConnectPicks.all, "glass icons", Filter.ALL).map { it.id },
        )
        assertEquals(all, ConnectFeed.list(all, "  ", Filter.ALL))
    }

    @Test
    fun `the filters keep what they say`() {
        val all = ConnectPicks.all
        assertTrue(ConnectFeed.list(all, "", Filter.GLASS).none { JSONObject(it.settings).str("theme") == "clean" })
        assertTrue(ConnectFeed.list(all, "", Filter.LIGHT).all { JSONObject(it.settings).str("appearance") == "light" })
        assertTrue(ConnectFeed.list(all, "", Filter.COLOUR).all { JSONObject(it.settings).str("widgetLook") == "colour" })
        assertTrue(ConnectFeed.list(all, "", Filter.DARK).isNotEmpty())
        assertTrue(ConnectFeed.list(all, "", Filter.LIGHT).isNotEmpty())
    }

    @Test
    fun `the heading says what is shown`() {
        assertEquals("Zenith Picks" to "made for this Zenith", ConnectFeed.heading("", Filter.ALL, 10))
        assertEquals("Results" to "1 found", ConnectFeed.heading("sea", Filter.ALL, 1))
        assertEquals("Light" to "3 setups", ConnectFeed.heading("", Filter.LIGHT, 3))
    }

    @Test
    fun `the setup of the week stays all week and moves on the next`() {
        val monday = LocalDate.of(2026, 11, 16)
        val week = ConnectFeed.ofTheWeek(ConnectPicks.all, monday)
        assertEquals(week, ConnectFeed.ofTheWeek(ConnectPicks.all, monday.plusDays(6)))
        assertNotEquals(week, ConnectFeed.ofTheWeek(ConnectPicks.all, monday.plusDays(7)))
        assertNull(ConnectFeed.ofTheWeek(emptyList(), monday))
        // Round them all, one a week.
        val seen = (0 until ConnectPicks.all.size).map { ConnectFeed.ofTheWeek(ConnectPicks.all, monday.plusWeeks(it.toLong()))!!.id }.toSet()
        assertEquals(ConnectPicks.all.size, seen.size)
    }

    // ------------------------------------------------------------------ Zenith's picks

    @Test
    fun `every pick is free, still, and only what Zenith has`() {
        val palettes = Palettes.all.map { it.id }.toSet()
        val themes = Themes.all.map { it.id }.toSet()
        assertEquals(ConnectPicks.all.size, ConnectPicks.all.map { it.id }.toSet().size)
        for (pick in ConnectPicks.all) {
            val o = JSONObject(pick.settings)
            val design = WallStyle.byName(o.str("wallpaperStyle"))
            assertTrue(pick.name, design != null)
            assertFalse("${pick.name} needs Plus", design!!.plus)
            assertNull("${pick.name} moves", design.live)
            assertTrue(pick.name, o.str("wallpaperPalette") in palettes)
            assertTrue(pick.name, o.str("theme") in themes)
            assertTrue(pick.name, o.str("iconStyle") in IconStyles.ids)
            assertTrue(pick.name, o.str("widgetLook") in WidgetLooks.ids)
            assertTrue(pick.name, o.str("appearance") in setOf("light", "dark"))
            o.keys().forEach { assertTrue(it, it in Looks.KEYS) }
            assertEquals(SetupSource.PICK, pick.source)
            assertTrue(pick.about.isNotBlank())
        }
    }

    // ------------------------------------------------------------------ the rest

    @Test
    fun `what is new leads with this very build`() {
        assertEquals(BuildConfig.VERSION_NAME.removeSuffix("-debug"), ConnectNews.all.first().version)
        assertTrue(ConnectNews.all.all { it.lines.isNotEmpty() })
    }

    @Test
    fun `a problem report carries the phone and the build under your words`() {
        val lines = ConnectReport.lines("3.9.2", 111, "SM-A556B", "samsung", "15", lite = true, theme = "glass")
        assertEquals(listOf("Zenith 3.9.2 (111)", "Samsung SM-A556B · Android 15", "Drawing: Lite · Theme: Liquid Glass"), lines)
        assertEquals("It froze\n\n—\nZenith 3.9.2 (111)", ConnectReport.text("  It froze ", lines.take(1)))
        assertEquals("Say what happened first.", ConnectReport.problem(" "))
        assertNull(ConnectReport.problem("It froze"))
    }

    @Test
    fun `the server pill says how it stands`() {
        assertEquals("Connected to Zenith", ConnectServer.pill(ServerState.UP))
        assertEquals("Can't reach Zenith's server", ConnectServer.pill(ServerState.DOWN))
        assertEquals("On this phone only", ConnectServer.pill(ServerState.NONE))
    }

    @Test
    fun `your handle is the name you gave Zenith`() {
        assertEquals("gabriel.t", ConnectStudio.handle("  Gabriel  T "))
        assertEquals("you", ConnectStudio.handle(""))
        assertEquals("G", ConnectStudio.initial("gabriel.t"))
        assertEquals("Give your setup a name first.", ConnectStudio.nameProblem(" "))
    }
}
