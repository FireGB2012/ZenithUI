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
        gridDensity = "roomy",
        columns = 5,
        rows = 6,
        iconSize = 48,
        showLabels = false,
        iconShape = "circle",
        iconPack = "com.example.icons",
        glassShine = false,
        openAnimation = "glass",
        appsView = "library",
        recentLaunches = listOf("a/b", "c/d"),
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
        pageEffect = "cube",
        haptics = false,
        toggles = listOf("wifi", "torch"),
        pages = 3,
        note = "milk",
        homePrompts = 2,
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
        referralsPaid = 3,
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
    }

    @Test
    fun `a value this version does not know is kept as it was written`() {
        // A backup from a newer Zenith: nothing is thrown away on the way through an older one.
        val newer = ZenithSettings.fromJson(JSONObject("""{"pageEffect":"origami","motion":"extra"}"""))
        assertEquals("origami", newer.pageEffect)
        assertEquals("extra", throughText(newer).motion)
    }
}
