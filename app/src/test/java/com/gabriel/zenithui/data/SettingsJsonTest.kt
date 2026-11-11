package com.gabriel.zenithui.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsJsonTest {

    /** Every field set to something other than its default, so a field left out of the JSON shows. */
    private val changed = ZenithSettings(
        onboarded = true,
        userName = "Gabriel",
        paletteId = "sunset",
        appearance = "auto",
        glass = 0.55f,
        glassBody = "solid",
        gridDensity = "roomy",
        gridColumns = 6,
        gridRows = 7,
        columns = 5,
        rows = 6,
        iconSize = 48,
        showLabels = false,
        iconShape = "circle",
        iconPack = "com.example.icons",
        appEdits = mapOf(
            "a/b" to AppEdit(label = "Mail", icon = IconSource.pack("com.example.icons", "mail_blue")),
            "c/d" to AppEdit(hideLabel = true),
        ),
        iconStyle = "glass",
        badges = "count",
        labelSize = "large",
        labelShadow = true,
        glassShine = false,
        openAnimation = "glass",
        appsView = "library",
        recentLaunches = listOf("a/b", "c/d"),
        launchCounts = mapOf("a/b" to 7),
        drawerSort = "used",
        drawerColumns = 5,
        drawerTabs = listOf(DrawerTab("Games", listOf("g/h"))),
        freeform = true,
        widgetTuck = 2,
        dock = listOf("x/y", "z/w"),
        showDock = false,
        searchBar = true,
        header = "greeting",
        wallpaperStyle = "Waves",
        wallpaperPalette = "aurora",
        wallpaperLock = true,
        wallpaperFollow = false,
        motion = "reduced",
        animation = AnimationTuning(preset = "bouncy", speed = 1.25f, changed = mapOf("openArc" to 24f, "homeScale" to 1.05f)),
        pageEffect = "cube",
        haptics = false,
        gestures = mapOf(Gestures.DOUBLE_TAP to GestureActions.LOCK),
        toggles = listOf("wifi", "torch"),
        pages = 3,
        note = "milk",
        worldClocks = listOf("Asia/Kolkata"),
        countdownTitle = "Lisbon",
        countdownDay = 20_500L,
        countdownFrom = 20_400L,
        framePhotos = listOf("frame-1.jpg"),
        todos = listOf(TodoItem("bread"), TodoItem("post", done = true)),
        weatherPlace = "Oslo",
        weatherLat = 59.91,
        weatherLon = 10.75,
        weatherUnits = "f",
        lookRules = listOf(LookRule("night", 1320, 420, setOf(5, 6))),
        lookOccasion = "night@1320@20400",
        activeLook = "night",
        font = "inter",
        numberFont = "thin",
        glassTuning = GlassTuning(bend = 1.4f, frost = -0.5f, rim = 0.6f, rainbow = 2f, tint = 0.2f, lightAngle = 90f, lightTilt = false),
        dayPalettes = mapOf("dawn" to "sunset", "night" to "midnight"),
        paletteOccasion = "night@20400",
        wallAccent = 0xFFFF6B8B,
        wallAccentAlt = 0xFF4FB4FF,
        wallBackdrop = 0xFF101018,
        wallDark = false,
        arrival = "cascade",
        drawerArrival = "grow",
        press = "ripple",
        dockStyle = "floating",
        indicator = "numbers",
        folderStyle = "cover",
        widgetLook = "colour",
        lockLayout = true,
        dimWallpaper = true,
        hideStatusBar = true,
        island = IslandSettings(on = true, live = false, always = true, hideSideways = false),
        homePrompts = 2,
        calendarDeclined = true,
        newLookAsked = true,
        layoutVersion = 2,
        plusTrialStart = 1_700_000_000_000L,
        plusPurchased = true,
        hiddenApps = listOf("h/i"),
        hiddenPin = "1234",
        aura = 70,
        streak = 4,
        lastCheckIn = 20_000L,
        unlockedThemes = listOf("ember"),
        referralCode = "ZEN123",
        referredBy = "ZEN999",
        referralsPaid = mapOf("ZEN123" to 3),
        customAccent = 0xFF112233,
        customAccentAlt = 0xFF445566,
        customBackdrop = 0xFF778899,
        customDark = false,
    )

    private fun throughText(settings: ZenithSettings): ZenithSettings =
        ZenithSettings.fromJson(JSONObject(settings.toJson().toString()))

    @Test
    fun `settings survive being written and read back`() {
        assertEquals(changed, throughText(changed))
        assertEquals(ZenithSettings(), throughText(ZenithSettings()))
    }

    @Test
    fun `the fixture really does change every field`() {
        // Compared as text, field by field: a field added later and forgotten above fails here.
        val defaults = ZenithSettings().toJson()
        val written = changed.toJson()
        for (key in defaults.keys()) {
            assertNotEquals("$key is still at its default in the fixture", defaults.get(key).toString(), written.get(key).toString())
        }
    }

    @Test
    fun `settings saved before this work read with today's defaults`() {
        val old = ZenithSettings.fromJson(JSONObject("""{"onboarded":true,"paletteId":"sunset","columns":4,"rows":5}"""))
        assertTrue(old.onboarded)
        assertEquals("sunset", old.paletteId)
        assertEquals("full", old.motion)
        assertEquals("depth", old.pageEffect)
        assertTrue(old.haptics)
        assertTrue(old.wallpaperFollow)
        assertEquals("", old.wallpaperStyle)
        assertFalse(old.searchBar)
        assertEquals("name", old.header)
        // The glass is the glass it has always been unless more body is asked for.
        assertEquals("clear", old.glassBody)
        assertEquals(GlassBody.Clear, GlassBody.byId(old.glassBody))
        // Frosted was what everybody got for a day, saved under another name: that is not kept.
        assertEquals("clear", ZenithSettings.fromJson(JSONObject("""{"glassBody":"frosted"}""")).glassBody)
        // Asked for by name, it is.
        assertEquals("frosted", ZenithSettings.fromJson(JSONObject("""{"glassPanels":"frosted"}""")).glassBody)
        // Icons as their apps draw them, dots that only say something is there, names as they were.
        assertEquals(IconStyles.ORIGINAL, old.iconStyle)
        assertEquals(Badges.DOT, old.badges)
        assertEquals(LabelSizes.MEDIUM, old.labelSize)
        assertFalse(old.labelShadow)
        assertTrue(old.appEdits.isEmpty())
    }

    @Test
    fun `settings saved before the new look read as glass widgets, not yet asked`() {
        // A phone that already has Zenith keeps the glass widgets it had until it has been asked.
        val old = ZenithSettings.fromJson(JSONObject("{}"))
        assertEquals("glass", old.widgetLook)
        assertFalse(old.newLookAsked)
    }

    @Test
    fun `what each account's code has paid out is remembered code by code`() {
        val paid = ZenithSettings(referralsPaid = mapOf("WTR4RT" to 4, "BCDFGH" to 1))
        assertEquals(paid, throughText(paid))
        // Something that is not a count is not a count.
        val odd = ZenithSettings.fromJson(JSONObject("""{"referralsPaid":{"WTR4RT":2,"BCDFGH":"many","ZZZZZZ":-3}}"""))
        assertEquals(mapOf("WTR4RT" to 2), odd.referralsPaid)
        // And settings from before there were accounts have paid nobody.
        assertEquals(emptyMap<String, Int>(), ZenithSettings.fromJson(JSONObject("""{"referralsPaid":3}""")).referralsPaid)
    }

    @Test
    fun `a value this version does not know is kept as it was written`() {
        // A backup from a newer Zenith: nothing is thrown away on the way through an older one.
        val newer = ZenithSettings.fromJson(JSONObject("""{"pageEffect":"origami","motion":"extra"}"""))
        assertEquals("origami", newer.pageEffect)
        assertEquals("extra", throughText(newer).motion)
    }
}
