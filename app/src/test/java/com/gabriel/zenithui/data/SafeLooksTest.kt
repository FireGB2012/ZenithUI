package com.gabriel.zenithui.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A Look opened by a code was made by somebody else, on a phone that may not run Zenith at all:
 * what it says is read as a stranger's word. Nothing in it may crash the home screen, put words
 * on it, or reach past what a Look is for. The same bounds keep a damaged save or backup from
 * taking the home screen down.
 */
class SafeLooksTest {

    private fun settings(vararg pairs: Pair<String, Any>) = ZenithSettings.fromJson(JSONObject().apply { pairs.forEach { (k, v) -> put(k, v) } })

    private fun item(vararg pairs: Pair<String, Any>) = homeItemFromJson(JSONObject().apply { pairs.forEach { (k, v) -> put(k, v) } })!!

    @Test
    fun `numbers out of all reason are brought back inside what Zenith can draw`() {
        val wild = settings(
            "glass" to 50.0, "columns" to 100_000, "rows" to -5, "gridColumns" to 99, "gridRows" to 1_000,
            "drawerColumns" to 1_000_000, "pages" to 1_000_000_000,
        )
        assertEquals(1.2f, wild.glass)
        assertEquals(GridDensity.MAX_COLUMNS, wild.columns)
        assertEquals(GridDensity.MIN_ROWS, wild.rows)
        assertEquals(GridDensity.MAX_COLUMNS, wild.gridColumns)
        assertEquals(GridDensity.MAX_ROWS, wild.gridRows)
        assertEquals(GridDensity.MAX_COLUMNS, wild.drawerColumns)
        assertEquals(LayoutBuilder.MAX_PAGES, wild.pages)
        // Not a number at all (org.json reads the word "NaN" as one): the default.
        assertEquals(ZenithSettings().glass, settings("glass" to "NaN").glass)
        assertEquals(ZenithSettings().glass, settings("glass" to "Infinity").glass)
        // No grid of your own, and the drawer following the home screen, stay just that.
        assertEquals(0, settings("gridColumns" to 0, "gridRows" to -2).gridColumns)
        assertEquals(0, settings("gridColumns" to 0, "gridRows" to -2).gridRows)
        assertEquals(0, settings("drawerColumns" to -3).drawerColumns)
    }

    @Test
    fun `every number Zenith itself writes reads back as it was`() {
        val own = ZenithSettings(glass = 0.2f, columns = 3, rows = 10, gridColumns = 16, gridRows = 3, drawerColumns = 6, pages = LayoutBuilder.MAX_PAGES)
        assertEquals(own, ZenithSettings.fromJson(own.toJson()))
        val other = ZenithSettings(glass = 1.2f, columns = 16, rows = 3, drawerColumns = 0, pages = 1)
        assertEquals(other, ZenithSettings.fromJson(other.toJson()))
    }

    @Test
    fun `a photo for the frame is a name in the frame's folder, never a path out of it`() {
        val read = settings("framePhotos" to JSONArray(listOf("frame-1-0.jpg", "../shared_prefs/zenith.xml", "a/b.jpg", "a\\b.jpg", "..", ".", "", "frame-2-1.jpg")))
        assertEquals(listOf("frame-1-0.jpg", "frame-2-1.jpg"), read.framePhotos)
        assertTrue(FramePhotos.isPlainName("frame-1700000000000-3.jpg"))
        assertFalse(FramePhotos.isPlainName("../x.jpg"))
        assertFalse(FramePhotos.isPlainName("x".repeat(300)))
    }

    @Test
    fun `an item is never off every page, out of the grid, or nudged by what is not a number`() {
        val far = item("t" to "app", "id" to "a", "p" to 1_000_000_000, "c" to -4, "r" to 9_999_999, "pkg" to "com.x", "cls" to ".Main", "sx" to 1_000_000, "sy" to -1, "dx" to "NaN", "dy" to 40.0)
        assertEquals(LayoutBuilder.MAX_PAGES - 1, far.page)
        assertEquals(0, far.col)
        assertTrue(far.row < GridDensity.MAX_ROWS * ROW_UNITS)
        assertEquals(GridDensity.MAX_COLUMNS, far.spanX)
        assertTrue(far.spanY >= 1)
        assertEquals(0f, far.dx)
        assertEquals(0.5f, far.dy)
        val widget = item("t" to "widget", "id" to "w", "type" to "clock", "sx" to 99_999, "sy" to 99_999)
        assertEquals(GridDensity.MAX_COLUMNS, widget.spanX)
        assertEquals(GridDensity.MAX_ROWS * ROW_UNITS, widget.spanY)
        // A folder named at great length keeps a name, not a page of text.
        val folder = item("t" to "folder", "id" to "f", "title" to "x".repeat(10_000), "keys" to JSONArray(listOf("a/b")))
        assertEquals(FolderCell.TITLE_MOST, (folder as FolderCell).title.length)
    }

    @Test
    fun `what Zenith writes for an item reads back as it was`() {
        val own = listOf(
            AppCell("a", 7, 15, 39, "com.x", ".Main", spanX = 2, spanY = 2 * ROW_UNITS, dx = -0.5f, dy = 0.25f),
            FolderCell("f", 3, 2, 8, "Games", listOf("a/b", "c/d")),
            WidgetCell("w", 0, 0, 4, "clock", 4, 2 * ROW_UNITS),
        )
        assertEquals(own, own.map { homeItemFromJson(it.toJson()) })
    }

    @Test
    fun `two things on the home screen never answer to the same name`() {
        val layout = JSONArray(listOf(
            AppCell("same", 0, 0, 0, "com.a", ".A").toJson(),
            AppCell("same", 0, 1, 0, "com.b", ".B").toJson(),
            AppCell("", 0, 2, 0, "com.c", ".C").toJson(),
        ))
        val read = ZenithConfig.layoutFrom(layout)
        assertEquals(3, read.size)
        assertEquals(3, read.map { it.id }.toSet().size)
        assertTrue(read.none { it.id.isEmpty() })
        // The first keeps its name; only the ones after it are given new ones.
        assertEquals("same", read.first().id)
        val (_, worn) = Looks.apply(ZenithSettings(), Look("l", "L", "{}", layout.toString(), 0L))
        assertEquals(3, worn.map { it.id }.toSet().size)
    }

    private fun stranger(): Look {
        val settings = JSONObject()
            .put("theme", "aurora")
            .put("columns", 5)
            // What a Look never carries, put in by hand.
            .put("hiddenPin", "1234").put("plusPurchased", true).put("aura", 99_999).put("userName", "Mallory")
            .put("appEdits", JSONObject().put("com.bank/.Main", JSONObject().put("label", "Type your password here").put("icon", IconSource.pack("com.pack", "bank"))))
        val layout = JSONArray(listOf(
            AppCell("a", 0, 0, 0, "com.mail", ".Main").toJson(),
            HostedWidgetCell("h", 0, 0, 1, 4, 2 * ROW_UNITS, appWidgetId = 3, provider = "com.x/.W").toJson(),
            ShortcutCell("s", 0, 1, 0, "com.bank", "pay", "Free money").toJson(),
            FolderCell("f", 0, 2, 0, "y".repeat(500), listOf("com.mail/.Main", "com.chat/.Main")).toJson(),
            StackCell("st", 1, 0, 0, 4, 2 * ROW_UNITS, listOf(StackMember("m1", type = "clock"), StackMember("m2", appWidgetId = 9, provider = "com.x/.W"))).toJson(),
        ))
        return Look("theirs", "  Free\nPlus  " + "z".repeat(200), settings.toString(), layout.toString(), 5L, shared = "BCDFGH", sharedAt = 4L)
    }

    @Test
    fun `a Look from a code keeps only what a Look is, and none of what only their phone has`() {
        val safe = Looks.fromElsewhere(stranger())
        val kept = JSONObject(safe.settings)
        assertTrue(kept.keys().asSequence().all { it in Looks.KEYS })
        assertEquals("aurora", kept.getString("theme"))
        // The name they gave an app is theirs to see, not yours; an icon from a pack is the Look's.
        val edits = AppEdits.fromJson(kept.optJSONObject("appEdits"))
        assertEquals(AppEdit(icon = IconSource.pack("com.pack", "bank")), edits["com.bank/.Main"])
        val items = Looks.apply(ZenithSettings(), safe).second
        assertTrue(items.none { it is HostedWidgetCell || it is ShortcutCell })
        assertEquals(WidgetCell("m1", 1, 0, 0, "clock", 4, 2 * ROW_UNITS), items.single { it.id == "m1" })
        assertEquals(FolderCell.TITLE_MOST, (items.single { it.id == "f" } as FolderCell).title.length)
        // A name on one line, no longer than one you could give it here, and never a code of yours.
        assertEquals("Free Plus " + "z".repeat(Looks.NAME_MOST - "Free Plus ".length), safe.name)
        assertEquals("", safe.shared)
        // Putting it on gives nobody Plus, Aura, a PIN or a new name.
        val worn = Looks.apply(ZenithSettings(userName = "Gabriel"), safe).first
        assertEquals("Gabriel", worn.userName)
        assertEquals("", worn.hiddenPin)
        assertFalse(worn.plusPurchased)
        assertEquals(0, worn.aura)
        // And going through it twice changes nothing more.
        assertEquals(safe, Looks.fromElsewhere(safe))
    }

    @Test
    fun `a Look from a code holds no more than a home screen can`() {
        val many = JSONArray((0 until 50_000).map { AppCell("a$it", 0, 0, 0, "com.x", ".M$it").toJson() })
        val safe = Looks.fromElsewhere(Look("x", "Many", "{}", many.toString(), 0L))
        assertEquals(Looks.ITEMS_MOST, JSONArray(safe.layout).length())
        // Settings or a layout that are not JSON at all are an empty Look, not a crash.
        val broken = Looks.fromElsewhere(Look("x", "", "not json", "[{", 0L))
        assertEquals("{}", broken.settings)
        assertEquals("[]", broken.layout)
    }

    @Test
    fun `their gestures open only apps this phone has, and never one of their shortcuts`() {
        val theirs = ZenithSettings(
            gestures = mapOf(
                Gestures.SWIPE_DOWN to GestureActions.NOTIFICATIONS,
                Gestures.DOUBLE_TAP to GestureActions.app("com.mail/.Main"),
                Gestures.TWO_UP to GestureActions.app("com.missing/.Main"),
                Gestures.TWO_DOWN to ShortcutKeys.of("com.bank", "pay"),
                Gestures.PINCH_IN to "something-else",
                "aGestureZenithHasNot" to GestureActions.DRAWER,
            ),
        )
        val look = Looks.capture(theirs, emptyList(), "Theirs", "t", 0L)
        val worn = Looks.apply(ZenithSettings(), Looks.fromElsewhere(look, installed = setOf("com.mail/.Main"))).first
        assertEquals(
            mapOf(Gestures.SWIPE_DOWN to GestureActions.NOTIFICATIONS, Gestures.DOUBLE_TAP to GestureActions.app("com.mail/.Main")),
            worn.gestures,
        )
        // Not knowing what this phone has, every app may stay; a shortcut never does.
        val blind = Looks.apply(ZenithSettings(), Looks.fromElsewhere(look)).first
        assertEquals(setOf(Gestures.SWIPE_DOWN, Gestures.DOUBLE_TAP, Gestures.TWO_UP), blind.gestures.keys)
    }

    @Test
    fun `fitting their Look to this phone leaves your own gestures alone`() {
        val mine = ZenithSettings(gestures = mapOf(Gestures.DOUBLE_TAP to ShortcutKeys.of("com.maps", "home")))
        val (fitted, _) = Looks.keepInstalled(mine, emptyList(), emptySet())
        assertEquals(mine.gestures, fitted.gestures)
    }
}
