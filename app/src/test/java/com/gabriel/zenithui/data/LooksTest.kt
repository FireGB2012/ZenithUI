package com.gabriel.zenithui.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class LooksTest {

    private val mine = ZenithSettings(
        userName = "Gabriel",
        note = "buy milk",
        todos = listOf(TodoItem("call home")),
        hiddenApps = listOf("com.secret/.Main"),
        hiddenPin = "1234",
        aura = 420,
        plusPurchased = true,
        referralCode = "WTR4RT",
        paletteId = "aurora",
        appearance = "dark",
        glass = 0.8f,
        iconShape = "clover",
        columns = 5,
        rows = 6,
        dock = listOf("com.phone/.Main", "com.camera/.Main"),
        appEdits = mapOf(
            "com.mail/.Main" to AppEdit(label = "Work mail", icon = IconSource.pack("com.pack", "mail")),
            "com.photos/.Main" to AppEdit(icon = IconSource.photo("photo-1.png"), hideLabel = true),
        ),
    )

    private val layout = listOf(
        AppCell("a", 0, 0, 0, "com.mail", ".Main"),
        WidgetCell("w", 0, 0, 1, "clock", 4, 2 * ROW_UNITS),
        FolderCell("f", 1, 0, 0, "Fun", listOf("com.game/.Main", "com.music/.Main")),
    )

    private fun look(settings: ZenithSettings = mine, items: List<HomeItem> = layout) =
        Looks.capture(settings, items, "Night", "look-1", 1_000L)

    @Test
    fun `a Look keeps what you see and nothing of yours`() {
        val kept = JSONObject(look().settings)
        assertEquals("aurora", kept.getString("paletteId"))
        assertEquals("clover", kept.getString("iconShape"))
        assertEquals(5, kept.getInt("columns"))
        for (personal in listOf("userName", "note", "todos", "hiddenApps", "hiddenPin", "aura", "plusPurchased", "referralCode", "lookRules", "activeLook")) {
            assertFalse("$personal is not in a Look", kept.has(personal))
        }
        assertEquals(layout.size, JSONArray(look().layout).length())
    }

    @Test
    fun `a Look reads back as it was written`() {
        val written = look()
        assertEquals(written, Look.fromJson(written.toJson()))
        assertEquals(listOf(written), Looks.fromJsonList(Looks.toJsonList(listOf(written))))
        assertNull(Look.fromJson(JSONObject("""{"format":"zenith-ui-backup"}""")))
        assertEquals(emptyList<Look>(), Looks.fromJsonList("not json"))
        assertEquals(emptyList<Look>(), Looks.fromJsonList(null))
    }

    @Test
    fun `a nameless Look is still a Look`() {
        assertEquals("My look", Looks.capture(mine, layout, "   ", "x", 0L).name)
        assertEquals("A Look", Look.fromJson(JSONObject("""{"format":"zenith-look"}"""))?.name)
    }

    @Test
    fun `putting a Look on changes how it looks and leaves what is yours`() {
        val now = ZenithSettings(userName = "Sam", aura = 7, plusTrialStart = 5L, paletteId = "sunset", iconShape = "circle", columns = 4)
        val (worn, items) = Looks.apply(now, look())
        assertEquals("aurora", worn.paletteId)
        assertEquals("clover", worn.iconShape)
        assertEquals(5, worn.columns)
        assertEquals(mine.dock, worn.dock)
        assertEquals("Sam", worn.userName)
        assertEquals(7, worn.aura)
        assertEquals(5L, worn.plusTrialStart)
        assertEquals(layout, items)
    }

    @Test
    fun `a Look cannot give anybody Plus, Aura or somebody else's name`() {
        val tampered = look().let { it.copy(settings = JSONObject(it.settings).put("plusPurchased", true).put("aura", 99999).put("userName", "Mallory").toString()) }
        val (worn, _) = Looks.apply(ZenithSettings(), tampered)
        assertFalse(worn.plusPurchased)
        assertEquals(0, worn.aura)
        assertEquals("", worn.userName)
    }

    @Test
    fun `a Look from a newer Zenith still puts on all this one knows`() {
        val newer = look().let { it.copy(settings = JSONObject(it.settings).put("somethingNew", 3).toString()) }
        val (worn, _) = Looks.apply(ZenithSettings(), newer)
        assertEquals("aurora", worn.paletteId)
    }

    @Test
    fun `a Look keeps the version its layout was written in`() {
        val old = look(mine.copy(layoutVersion = 1))
        assertEquals(1, Looks.apply(ZenithSettings(layoutVersion = LayoutTidy.VERSION), old).first.layoutVersion)
    }

    @Test
    fun `a shared Look leaves out what only works on this phone, and the names you gave apps`() {
        val items = layout + listOf(
            HostedWidgetCell("h", 2, 0, 0, 4, 2 * ROW_UNITS, appWidgetId = 17, provider = "com.weather/.Widget"),
            ShortcutCell("s", 2, 0, 3, "com.maps", "home", "Home"),
            StackCell("st", 3, 0, 0, 4, 2 * ROW_UNITS, listOf(StackMember("m1", type = "clock"), StackMember("m2", appWidgetId = 9, provider = "com.x/.W"))),
            StackCell("gone", 3, 0, 4, 4, 2 * ROW_UNITS, listOf(StackMember("m3", appWidgetId = 10, provider = "com.x/.W"), StackMember("m4", appWidgetId = 11, provider = "com.y/.W"))),
        )
        val shared = Looks.forSharing(look(items = items))
        val (worn, kept) = Looks.apply(ZenithSettings(), shared)
        assertTrue(kept.none { it is HostedWidgetCell || it is ShortcutCell })
        // A stack keeps Zenith's own widgets: here one, so it is that widget on its own.
        assertEquals(WidgetCell("m1", 3, 0, 0, "clock", 4, 2 * ROW_UNITS), kept.single { it.id == "m1" })
        assertTrue(kept.none { it.id == "gone" })
        assertEquals(layout, kept.filter { it.id in setOf("a", "w", "f") })
        // An icon from a pack stays, a photo goes, and so does the name; a hidden name stays hidden.
        assertEquals(AppEdit(icon = IconSource.pack("com.pack", "mail")), worn.appEdits["com.mail/.Main"])
        assertEquals(AppEdit(hideLabel = true), worn.appEdits["com.photos/.Main"])
    }

    @Test
    fun `a shared Look remembers its code, and what goes to somebody else does not`() {
        val shared = look().copy(shared = "BCDFGH", sharedAt = 2_000L)
        assertEquals(shared, Look.fromJson(shared.toJson()))
        assertFalse(shared.sharedIsOld)
        // Saved over after it was shared: the code gives it as it was.
        assertTrue(shared.copy(savedAt = 3_000L).sharedIsOld)
        assertFalse(look().copy(savedAt = 3_000L).sharedIsOld)
        val sent = Looks.forSharing(shared)
        assertEquals("", sent.shared)
        assertFalse(sent.toJson().has("shared"))
        assertFalse(sent.toJson().has("sharedAt"))
    }

    @Test
    fun `a Look from another phone keeps only the apps this one has`() {
        val installed = setOf("com.mail/.Main", "com.music/.Main", "com.phone/.Main")
        val (worn, items) = Looks.apply(ZenithSettings(), look())
        val (fitted, kept) = Looks.keepInstalled(worn, items, installed)
        assertEquals(listOf("com.phone/.Main"), fitted.dock)
        assertTrue(kept.any { it is AppCell && it.key == "com.mail/.Main" })
        // The folder had one app this phone has: it is that app, where the folder was.
        assertEquals(AppCell("f", 1, 0, 0, "com.music", ".Main"), kept.single { it.id == "f" })
        assertTrue(kept.any { it is WidgetCell })
        val (_, none) = Looks.keepInstalled(worn, items, emptySet())
        assertEquals(listOf("w"), none.map { it.id })
    }

    @Test
    fun `a Look's palette can be read without putting it on`() {
        assertEquals("aurora", Looks.paletteOf(look()))
        assertEquals("", Looks.paletteOf(look().copy(settings = "broken")))
    }

    // ---------------------------------------------------------------- the schedule

    private fun at(day: Int, hour: Int, minute: Int = 0): LocalDateTime =
        // 2 November 2026 is a Monday: day 1.
        LocalDateTime.of(2026, 11, 1 + day, hour, minute)

    @Test
    fun `a slot in the day is in force from its start until its end`() {
        val rules = listOf(LookRule("work", 9 * 60, 17 * 60, (1..5).toSet()))
        assertNull(LookSchedule.inForce(rules, at(1, 8, 59)))
        assertEquals(0, LookSchedule.inForce(rules, at(1, 9))?.first)
        assertEquals(0, LookSchedule.inForce(rules, at(1, 16, 59))?.first)
        assertNull(LookSchedule.inForce(rules, at(1, 17)))
        // Saturday is not one of its days.
        assertNull(LookSchedule.inForce(rules, at(6, 10)))
    }

    @Test
    fun `a slot past midnight belongs to the day it began`() {
        // Friday nights only: 22:00 until 07:00.
        val rules = listOf(LookRule("night", 22 * 60, 7 * 60, setOf(5)))
        assertNull(LookSchedule.inForce(rules, at(5, 21, 59)))
        val friday = at(5, 23)
        assertEquals(friday.toLocalDate().toEpochDay(), LookSchedule.inForce(rules, friday)?.second)
        // Saturday morning is still Friday's night.
        val saturday = at(6, 6, 59)
        assertEquals(friday.toLocalDate().toEpochDay(), LookSchedule.inForce(rules, saturday)?.second)
        assertNull(LookSchedule.inForce(rules, at(6, 7)))
        // Friday morning is Thursday's night, which is not one of its nights.
        assertNull(LookSchedule.inForce(rules, at(5, 3)))
    }

    @Test
    fun `each slot switches the Look once`() {
        val rules = listOf(LookRule("work", 9 * 60, 17 * 60))
        val (rule, occasion) = LookSchedule.due(rules, at(1, 9, 30), last = "")!!
        assertEquals("work", rule.lookId)
        // Later in the same slot, with a Look chosen by hand meanwhile: left alone.
        assertNull(LookSchedule.due(rules, at(1, 15), last = occasion))
        // The next day's slot switches again.
        assertTrue(LookSchedule.due(rules, at(2, 9, 30), last = occasion) != null)
        // Out of every slot nothing is due.
        assertNull(LookSchedule.due(rules, at(1, 20), last = ""))
    }

    @Test
    fun `the first rule whose slot it is wins`() {
        val rules = listOf(LookRule("first", 8 * 60, 12 * 60), LookRule("second", 10 * 60, 14 * 60))
        assertEquals("first", LookSchedule.due(rules, at(3, 11), "")?.first?.lookId)
        assertEquals("second", LookSchedule.due(rules, at(3, 13), "")?.first?.lookId)
    }

    @Test
    fun `a slot that starts where it ends is never in force`() {
        assertNull(LookSchedule.inForce(listOf(LookRule("x", 600, 600)), at(1, 10)))
    }

    @Test
    fun `a rule reads back as it was written, and nonsense is made safe`() {
        val rule = LookRule("night", 22 * 60, 7 * 60, setOf(5, 6))
        assertEquals(rule, LookRule.fromJson(rule.toJson()))
        assertNull(LookRule.fromJson(JSONObject("""{"start":0}""")))
        val wild = LookRule.fromJson(JSONObject("""{"look":"x","start":-5,"end":99999,"days":[0,8]}"""))!!
        assertEquals(0, wild.start)
        assertEquals(1439, wild.end)
        assertEquals((1..7).toSet(), wild.days)
    }

    @Test
    fun `days and times are said the way people say them`() {
        assertEquals("Every day", LookSchedule.days((1..7).toSet()))
        assertEquals("Weekdays", LookSchedule.days((1..5).toSet()))
        assertEquals("Weekends", LookSchedule.days(setOf(6, 7)))
        assertEquals("Mon, Wed", LookSchedule.days(setOf(3, 1)))
        assertEquals("09:05", LookSchedule.clock(9 * 60 + 5))
        assertEquals("00:00", LookSchedule.clock(0))
    }

    @Test
    fun `the schedule and the Look on now are kept with the settings`() {
        val settings = ZenithSettings(lookRules = listOf(LookRule("night", 1320, 420, setOf(5))), lookOccasion = "night@1320@20000", activeLook = "night")
        assertEquals(settings, ZenithSettings.fromJson(JSONObject(settings.toJson().toString())))
    }

    // ---------------------------------------------------------------- backups

    @Test
    fun `a backup carries the Looks, and one from before Looks carries none`() {
        val text = Backup.encode(mine, layout, listOf(look()))
        assertEquals(listOf(look()), Backup.looks(text))
        assertEquals(mine to layout, Backup.decode(text))
        assertEquals(emptyList<Look>(), Backup.looks(Backup.encode(mine, layout)))
        assertFalse(JSONObject(Backup.encode(mine, layout)).has("looks"))
        assertEquals(emptyList<Look>(), Backup.looks("""{"format":"something else","looks":[]}"""))
    }
}
