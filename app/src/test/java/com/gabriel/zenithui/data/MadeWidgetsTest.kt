package com.gabriel.zenithui.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MadeWidgetsTest {

    private val time = Piece(id = "p1", kind = PieceKinds.TIME, x = 0.1f, y = 0.3f, size = 0.32f, align = PieceAlign.START, weight = 300)
    private val words = Piece(id = "p2", kind = PieceKinds.TEXT, text = "Stay hungry", colour = 0xFFFF9F0A, glass = true)
    private val photo = Piece(id = "p3", kind = PieceKinds.PHOTO, text = "made-1.jpg", w = 0.5f, h = 0.6f, corner = 0.4f, alpha = 0.8f)
    private val design = MadeWidget(
        id = "abc",
        name = "Morning",
        spanX = 4,
        rows = 2,
        back = MadeBack.COLOUR,
        colour = 0xFF0A84FF,
        pieces = listOf(time, words, photo),
    )

    // ------------------------------------------------------------------ written and read back

    @Test
    fun `a made widget reads back exactly as it was written`() {
        val back = MadeWidget.fromJson(JSONObject(design.toJson().toString()))
        assertEquals(design, back)
    }

    @Test
    fun `the list of them travels in the settings, and so in a backup`() {
        val settings = ZenithSettings(madeWidgets = listOf(design, design.copy(id = "def", name = "Night")))
        val back = ZenithSettings.fromJson(JSONObject(settings.toJson().toString()))
        assertEquals(settings.madeWidgets, back.madeWidgets)
        val config = ZenithConfig(settings = settings)
        assertEquals(settings.madeWidgets, ZenithConfig.parse(config.toJson(0L).toString())?.settings?.madeWidgets)
    }

    @Test
    fun `settings from before there were any read as none`() {
        val old = ZenithSettings().toJson().apply { remove("madeWidgets") }
        assertEquals(emptyList<MadeWidget>(), ZenithSettings.fromJson(old).madeWidgets)
    }

    @Test
    fun `a damaged one is put right rather than lost`() {
        val o = JSONObject()
            .put("id", "x")
            .put("name", "A name far, far longer than anybody would ever want to read")
            .put("sx", 0)
            .put("rows", 99)
            .put("back", "velvet")
            .put(
                "pieces",
                JSONArray()
                    .put(JSONObject().put("id", "a").put("k", "time").put("x", 3.0).put("y", -1.0).put("s", 40.0))
                    .put(JSONObject().put("id", "b").put("k", "teleporter"))
                    .put(JSONObject().put("k", "date"))
                    .put(JSONObject().put("id", "c").put("k", "text").put("t", "x".repeat(200)).put("o", 0.0)),
            )
        val read = MadeWidget.fromJson(o)!!
        assertEquals(MadeWidgets.MOST_NAME, read.name.length)
        assertEquals(1, read.spanX)
        assertEquals(MadeWidgets.MOST_ROWS, read.rows)
        assertEquals(MadeBack.GLASS, read.back)
        // A kind this Zenith does not know, and a piece with no id, are passed over.
        assertEquals(listOf("a", "c"), read.pieces.map { it.id })
        val placed = read.pieces[0]
        assertEquals(1f, placed.x)
        assertEquals(0f, placed.y)
        assertEquals(Pieces.LARGEST, placed.size)
        assertEquals(MadeWidgets.MOST_LETTERS, read.pieces[1].text.length)
        assertEquals(Pieces.FAINTEST, read.pieces[1].alpha)
    }

    @Test
    fun `one with no id is not one`() {
        assertNull(MadeWidget.fromJson(JSONObject().put("name", "Lost")))
    }

    @Test
    fun `no more pieces are read than a widget holds`() {
        val many = design.copy(pieces = (1..40).map { Piece(id = "p$it", kind = PieceKinds.TEXT, text = "$it") })
        assertEquals(MadeWidgets.MOST_PIECES, MadeWidget.fromJson(many.toJson())!!.pieces.size)
    }

    // ------------------------------------------------------------------ on the page

    @Test
    fun `its type on the page says which one it is`() {
        val type = MadeWidgets.typeOf("abc")
        assertTrue(MadeWidgets.isMade(type))
        assertEquals("abc", MadeWidgets.idOf(type))
        assertFalse(MadeWidgets.isMade("clock"))
        assertFalse(MadeWidgets.isMade("bare_clock"))
        assertNull(MadeWidgets.idOf("clock"))
        assertNull(MadeWidgets.idOf(MadeWidgets.PREFIX))
        assertEquals(design, MadeWidgets.find(listOf(design), type))
        assertNull(MadeWidgets.find(listOf(design), MadeWidgets.typeOf("gone")))
    }

    @Test
    fun `it is a widget like any other to the layout`() {
        val type = MadeWidgets.typeOf("abc")
        val spec = Widgets.byType(type)
        assertNotNull(spec)
        assertEquals(type, spec!!.type)
        // As small as one icon, and pulled out to any size from there.
        assertEquals(1, spec.minSpanX)
        assertEquals(ROW_UNITS, spec.minSpanY)
        assertFalse(spec.isBare)
        // Its sizes are the gallery's.
        assertEquals(
            listOf(WidgetSizes.SMALL, WidgetSizes.MEDIUM, WidgetSizes.WIDE, WidgetSizes.TALL, WidgetSizes.LARGE),
            WidgetSizes.options(spec, 4, 6).map { it.name },
        )
    }

    @Test
    fun `it goes by the name it was given`() {
        val type = MadeWidgets.typeOf("abc")
        assertEquals("Morning", Widgets.shortName(type, listOf(design)))
        assertEquals(Widgets.UNNAMED, Widgets.shortName(type, emptyList()))
        assertEquals(Widgets.UNNAMED, Widgets.shortName(type, listOf(design.copy(name = "  "))))
        // Zenith's own keep theirs.
        assertEquals("Clock", Widgets.shortName("bare_clock", listOf(design)))
    }

    @Test
    fun `each new one gets a name of its own`() {
        assertEquals("My widget", MadeWidgets.nextName(emptyList()))
        val one = design.copy(id = "1", name = "My widget")
        assertEquals("My widget 2", MadeWidgets.nextName(listOf(one)))
        val two = design.copy(id = "2", name = "My widget 2")
        assertEquals("My widget 3", MadeWidgets.nextName(listOf(two, one)))
        // A name changed by hand frees its number.
        assertEquals("My widget", MadeWidgets.nextName(listOf(two)))
    }

    @Test
    fun `saving puts a new one first and an old one back where it was`() {
        val a = design.copy(id = "a")
        val b = design.copy(id = "b")
        val c = design.copy(id = "c")
        assertEquals(listOf(c, a, b), MadeWidgets.saved(listOf(a, b), c))
        val changed = b.copy(name = "Changed")
        assertEquals(listOf(a, changed), MadeWidgets.saved(listOf(a, b), changed))
    }

    @Test
    fun `no more are kept than the gallery holds`() {
        val full = (1..MadeWidgets.MOST).map { design.copy(id = "$it") }
        assertFalse(MadeWidgets.canMakeMore(full))
        assertTrue(MadeWidgets.canMakeMore(full.drop(1)))
        // A new one is refused; one already there is still saved.
        assertEquals(full, MadeWidgets.saved(full, design.copy(id = "new")))
        val changed = full[3].copy(name = "Changed")
        assertEquals(changed, MadeWidgets.saved(full, changed)[3])
    }

    @Test
    fun `one with nothing on it is not worth keeping`() {
        assertFalse(MadeWidgets.worthKeeping(design.copy(pieces = emptyList())))
        assertTrue(MadeWidgets.worthKeeping(design))
    }

    @Test
    fun `deleting one takes it off the page and out of stacks`() {
        val type = MadeWidgets.typeOf("abc")
        val other = MadeWidgets.typeOf("def")
        val layout = listOf(
            WidgetCell("w1", 0, 0, 0, type, 2, 2 * ROW_UNITS),
            WidgetCell("w2", 0, 2, 0, "clock", 2, 2 * ROW_UNITS),
            StackCell(
                "s1", 1, 0, 0, 2, 2 * ROW_UNITS,
                listOf(StackMember("m1", type = type), StackMember("m2", type = other)),
            ),
            StackCell(
                "s2", 1, 2, 0, 2, 2 * ROW_UNITS,
                listOf(StackMember("m3", type = type), StackMember("m4", type = "weather"), StackMember("m5", type = "clock")),
            ),
            AppCell("a", 2, 0, 0, "com.mail", ".Main"),
        )
        val left = MadeWidgets.withoutDesign(layout, "abc")
        assertEquals(listOf("w2", "m2", "s2", "a"), left.map { it.id })
        // A stack left with one widget is that widget, where the stack was.
        val alone = left[1] as WidgetCell
        assertEquals(other, alone.type)
        assertEquals(1, alone.page)
        assertEquals(listOf("m4", "m5"), (left[2] as StackCell).members.map { it.id })
        // Nothing else is touched.
        assertEquals(layout, MadeWidgets.withoutDesign(layout, "nothing"))
    }

    @Test
    fun `a widget whose design is gone is left out, as when a Look kept from before is worn`() {
        val type = MadeWidgets.typeOf("abc")
        val gone = MadeWidgets.typeOf("gone")
        val layout = listOf(
            WidgetCell("w1", 0, 0, 0, type, 2, 2 * ROW_UNITS),
            WidgetCell("w2", 0, 2, 0, gone, 2, 2 * ROW_UNITS),
            StackCell("s1", 1, 0, 0, 2, 2 * ROW_UNITS, listOf(StackMember("m1", type = gone), StackMember("m2", type = "clock"))),
            WidgetCell("w3", 1, 2, 0, "clock", 2, 2 * ROW_UNITS),
        )
        assertEquals(listOf("w1", "m2", "w3"), MadeWidgets.onlyKnown(layout, listOf(design)).map { it.id })
        // With every design there, nothing changes.
        assertEquals(layout.take(1), MadeWidgets.onlyKnown(layout.take(1), listOf(design)))
    }

    @Test
    fun `the photos in use are the ones on any widget`() {
        val second = design.copy(id = "z", pieces = listOf(Piece(id = "q", kind = PieceKinds.PHOTO, text = "made-2.jpg"), words))
        val empty = design.copy(id = "e", pieces = listOf(Piece(id = "r", kind = PieceKinds.PHOTO)))
        assertEquals(setOf("made-1.jpg", "made-2.jpg"), MadeWidgets.photos(listOf(design, second, empty)))
    }

    // ------------------------------------------------------------------ the pieces

    @Test
    fun `a new piece goes in the middle, over the others`() {
        val added = Pieces.added(design, Pieces.new(PieceKinds.DATE, "n"))
        assertEquals(listOf("p1", "p2", "p3", "n"), added.pieces.map { it.id })
        assertEquals(0.5f, added.pieces.last().x)
        assertEquals(0.5f, added.pieces.last().y)
        // Each kind comes as it is most often wanted.
        assertEquals(PieceStyles.DATE_SHORT, added.pieces.last().style)
        assertEquals(PieceStyles.BATTERY_RING, Pieces.new(PieceKinds.BATTERY, "b").style)
        assertTrue(Pieces.new(PieceKinds.TEXT, "t").text.isNotBlank())
    }

    @Test
    fun `a full widget takes no more pieces`() {
        val full = design.copy(pieces = (1..MadeWidgets.MOST_PIECES).map { Piece(id = "p$it", kind = PieceKinds.TEXT) })
        assertFalse(Pieces.canAdd(full))
        assertEquals(full, Pieces.added(full, Pieces.new(PieceKinds.TIME, "more")))
        assertEquals(full, Pieces.duplicated(full, "p1", "copy"))
    }

    @Test
    fun `a copy goes just off the piece it was copied from, over it`() {
        val copied = Pieces.duplicated(design, "p1", "p1b")
        assertEquals(listOf("p1", "p1b", "p2", "p3"), copied.pieces.map { it.id })
        val copy = copied.pieces[1]
        assertEquals(time.x + Pieces.NUDGE, copy.x, 0.0001f)
        assertEquals(time.y + Pieces.NUDGE, copy.y, 0.0001f)
        assertEquals(time.copy(id = "p1b", x = copy.x, y = copy.y), copy)
        // Near the far edge it goes the other way, to stay on the widget.
        val edge = design.copy(pieces = listOf(time.copy(x = 0.98f, y = 0.99f)))
        val back = Pieces.duplicated(edge, "p1", "c").pieces[1]
        assertEquals(0.98f - Pieces.NUDGE, back.x, 0.0001f)
        assertEquals(0.99f - Pieces.NUDGE, back.y, 0.0001f)
    }

    @Test
    fun `forward and back change what is drawn over what`() {
        assertEquals(listOf("p2", "p1", "p3"), Pieces.forward(design, "p1").pieces.map { it.id })
        assertEquals(listOf("p1", "p3", "p2"), Pieces.backward(design, "p3").pieces.map { it.id })
        // The top one goes no higher, and the bottom one no lower.
        assertEquals(design, Pieces.forward(design, "p3"))
        assertEquals(design, Pieces.backward(design, "p1"))
    }

    @Test
    fun `a piece is changed and deleted on its own`() {
        val bigger = time.copy(size = 0.5f)
        assertEquals(listOf(bigger, words, photo), Pieces.changed(design, bigger).pieces)
        assertEquals(listOf(time, photo), Pieces.removed(design, "p2").pieces)
        assertEquals(design, Pieces.removed(design, "nothing"))
    }

    @Test
    fun `made bigger or smaller, a piece keeps to its limits`() {
        assertEquals(0.64f, Pieces.scaled(time, 2f).size, 0.0001f)
        assertEquals(Pieces.LARGEST, Pieces.scaled(time, 100f).size)
        assertEquals(Pieces.SMALLEST, Pieces.scaled(time, 0.001f).size)
        // A shape or a photo grows both ways.
        val grown = Pieces.scaled(photo, 1.5f)
        assertEquals(0.75f, grown.w, 0.0001f)
        assertEquals(0.9f, grown.h, 0.0001f)
        assertEquals(1f, Pieces.scaled(photo, 3f).h)
    }

    // ------------------------------------------------------------------ starting points

    @Test
    fun `every starting point is a widget worth keeping, and reads back as it was written`() {
        assertEquals(4, MadeStarters.all.size)
        for (starter in MadeStarters.all) {
            val made = starter.madeAs("new", "My widget")
            assertTrue(starter.name, MadeWidgets.worthKeeping(made))
            assertEquals(starter.name, made, MadeWidget.fromJson(JSONObject(made.toJson().toString())))
            assertEquals(starter.name, made.pieces.size, made.pieces.map { it.id }.toSet().size)
        }
    }
}
