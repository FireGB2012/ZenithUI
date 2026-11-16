package com.gabriel.zenithui.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZenithConfigTest {

    private val settings = ZenithSettings(
        userName = "Gabriel",
        theme = "neon",
        appearance = "light",
        font = "rounded",
        columns = 5,
        rows = 6,
        dock = listOf("com.phone/.Main", "com.camera/.Main"),
    )

    private val layout = listOf(
        AppCell("a", 0, 0, 0, "com.mail", ".Main"),
        WidgetCell("w", 0, 0, 1, "clock", 4, 2 * ROW_UNITS),
        FolderCell("f", 1, 0, 0, "Fun", listOf("com.game/.Main", "com.music/.Main")),
    )

    private val look = Looks.capture(settings, layout, "Night", "look-1", 1_000L)

    private val mine = Themes.Neon.copy(id = "mine", name = "Mine")

    private val config = ZenithConfig(settings, layout, listOf(look), listOf(mine))

    @Test
    fun `a config is read back exactly as it was written`() {
        assertEquals(config, ZenithConfig.parse(config.toJson(savedAt = 5L).toString()))
        assertEquals(ZenithConfig(), ZenithConfig.parse(ZenithConfig().toJson(savedAt = 5L).toString()))
    }

    @Test
    fun `a config is written as a backup, so a backup from before configs reads as one`() {
        val written = config.toJson(savedAt = 5L)
        assertEquals("zenith-ui-backup", written.getString("format"))
        assertEquals(1, written.getInt("version"))
        assertEquals(5L, written.getLong("savedAt"))

        val older = JSONObject()
            .put("format", "zenith-ui-backup")
            .put("version", 1)
            .put("savedAt", 1L)
            .put("settings", settings.toJson())
            .put("layout", JSONArray().apply { layout.forEach { put(it.toJson()) } })
        assertEquals(ZenithConfig(settings, layout), ZenithConfig.parse(older.toString()))
    }

    @Test
    fun `parts with nothing in them are left out, and read back as nothing`() {
        val written = ZenithConfig(settings, layout).toJson(savedAt = 5L)
        assertFalse(written.has("looks"))
        assertFalse(written.has("themes"))
        assertTrue(written.has("layout"))
        assertEquals(ZenithConfig(settings), ZenithConfig.parse("""{"format":"zenith-ui-backup","settings":${settings.toJson()}}"""))
    }

    @Test
    fun `what is not a config reads as none`() {
        listOf(
            null,
            "",
            "not json",
            "[1, 2]",
            """{"format":"something else","settings":{}}""",
            """{"format":"zenith-ui-backup"}""",
            """{"format":"zenith-ui-backup","settings":"not settings"}""",
        ).forEach { assertNull(it, ZenithConfig.parse(it)) }
    }

    @Test
    fun `an entry that cannot be read is passed over, and everything else is kept`() {
        val written = config.toJson(savedAt = 5L)
        written.getJSONArray("layout").put("not an item").put(JSONObject().put("t", "nonsense"))
        written.getJSONArray("looks").put(42).put(JSONObject().put("format", "not a look"))
        written.getJSONArray("themes").put("not a theme").put(JSONObject().put("name", "No id"))
        written.put("somethingNew", true)
        assertEquals(config, ZenithConfig.parse(written.toString()))
    }

    @Test
    fun `a theme of your own cannot take the id of one Zenith comes with, or of another of yours`() {
        val written = config.toJson(savedAt = 5L)
        written.getJSONArray("themes").put(Themes.Retro.toJson()).put(mine.copy(name = "Again").toJson())
        assertEquals(listOf(mine), ZenithConfig.parse(written.toString())?.themes)
    }

    @Test
    fun `a config put back over another brings its own Looks and themes, and keeps the others`() {
        val older = look.copy(name = "Older")
        val other = Looks.capture(settings, layout, "Day", "look-2", 2_000L)
        val theirs = Themes.Retro.copy(id = "theirs", name = "Theirs")
        val here = ZenithConfig(ZenithSettings(userName = "Here"), emptyList(), listOf(older, other), listOf(mine.copy(name = "Old"), theirs))

        val back = config.over(here)
        assertEquals(settings, back.settings)
        assertEquals(layout, back.layout)
        // The one in both is the one put back.
        assertEquals(listOf(look, other), back.looks)
        assertEquals(listOf(mine, theirs), back.themes)
    }

    @Test
    fun `a config put back over another keeps no more Looks than a phone does`() {
        val many = (1..Looks.MOST).map { Looks.capture(settings, layout, "Look $it", "here-$it", it.toLong()) }
        val back = config.over(ZenithConfig(looks = many))
        assertEquals(Looks.MOST, back.looks.size)
        assertEquals(look, back.looks.first())
    }
}
