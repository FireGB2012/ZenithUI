package com.gabriel.zenithui.data

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/*
 * Widgets you make yourself, in the widget maker: a size, something to stand on, and pieces put on
 * it with a finger. See specs/2026-11-18-widget-maker-design.md.
 *
 * On the page one is a WidgetCell like any other, of the type "made:<id>", so it moves, resizes,
 * stacks and comes off the same way. What it looks like is kept once, in the settings, so a change
 * shows on every copy at once and a backup carries it.
 */

/** What a made widget stands on. */
enum class MadeBack(val key: String) {
    /** The pane Zenith's widgets have, tinted with the widget's colour when it has one. */
    GLASS("glass"),

    /** A card in the widget's colour, as a widget given its own colour is. */
    COLOUR("colour"),

    /** Nothing but the wallpaper. */
    CLEAR("clear");

    companion object {
        fun of(key: String): MadeBack = entries.firstOrNull { it.key == key } ?: GLASS
    }
}

/** How a piece lines up on its point across: by its start, its middle or its end. Down it is always its middle. */
enum class PieceAlign(val key: String) {
    START("start"),
    CENTER("middle"),
    END("end");

    companion object {
        fun of(key: String): PieceAlign = entries.firstOrNull { it.key == key } ?: CENTER
    }
}

/** The kinds of piece a widget can be made of. */
object PieceKinds {
    const val TIME = "time"
    const val DATE = "date"
    const val TEXT = "text"
    const val BATTERY = "battery"
    const val DIAL = "dial"
    const val WEATHER = "weather"
    const val SYMBOL = "symbol"
    const val SHAPE = "shape"
    const val PHOTO = "photo"
    const val GREETING = "greeting"
    const val ALARM = "alarm"

    /** In the order the maker offers them. */
    val all = listOf(TIME, DATE, TEXT, BATTERY, DIAL, WEATHER, SYMBOL, SHAPE, PHOTO, GREETING, ALARM)

    /** What each is called in the maker. */
    fun name(kind: String): String = when (kind) {
        TIME -> "Time"
        DATE -> "Date"
        TEXT -> "Words"
        BATTERY -> "Battery"
        DIAL -> "Clock face"
        WEATHER -> "Weather"
        SYMBOL -> "Symbol"
        SHAPE -> "Shape"
        PHOTO -> "Photo"
        GREETING -> "Greeting"
        ALARM -> "Next alarm"
        else -> "Piece"
    }

    /** Kinds whose size is a box of their own, a share of the widget's width and height, rather than a size of type. */
    fun isBox(kind: String): Boolean = kind == SHAPE || kind == PHOTO

    /** Kinds that are written: what has a weight, a font and a way of lining up its lines. */
    fun isWords(kind: String): Boolean = kind == TIME || kind == DATE || kind == TEXT || kind == GREETING || kind == ALARM
}

/**
 * The versions of a piece, kept as its [Piece.style]. Each kind has its own, and a version this
 * Zenith does not know is drawn as the kind's first.
 */
object PieceStyles {
    // The time: 12:45; with seconds; the hour alone; the minutes alone; with AM or PM.
    const val TIME = "hm"
    const val TIME_SECONDS = "hms"
    const val HOUR = "h"
    const val MINUTES = "m"
    const val TIME_HALF = "ampm"
    val times = listOf(TIME, TIME_SECONDS, HOUR, MINUTES, TIME_HALF)

    // The date: Wed 18 Nov; Wednesday; 18 November; Wednesday 18 November; 18/11/2026; 18; November.
    const val DATE_SHORT = "short"
    const val WEEKDAY = "weekday"
    const val DAY_MONTH = "daymonth"
    const val DATE_FULL = "full"
    const val DATE_NUMBERS = "numbers"
    const val DAY_NUMBER = "number"
    const val MONTH = "month"
    val dates = listOf(DATE_SHORT, WEEKDAY, DAY_MONTH, DATE_FULL, DATE_NUMBERS, DAY_NUMBER, MONTH)

    // The battery: a ring with the number in it, the number alone, a bar.
    const val BATTERY_RING = "ring"
    const val BATTERY_NUMBER = "number"
    const val BATTERY_BAR = "bar"
    val batteries = listOf(BATTERY_RING, BATTERY_NUMBER, BATTERY_BAR)

    // The clock face: with marks, with the second hand too, with the hours written round it.
    const val DIAL_MARKS = "marks"
    const val DIAL_SECONDS = "seconds"
    const val DIAL_NUMERALS = "numerals"
    val dials = listOf(DIAL_MARKS, DIAL_SECONDS, DIAL_NUMERALS)

    // The weather: the sky and the temperature; the temperature; the sky; the sky in words; the day's high and low.
    const val WEATHER_BOTH = "both"
    const val WEATHER_TEMPERATURE = "temp"
    const val WEATHER_SKY = "sky"
    const val WEATHER_WORDS = "words"
    const val WEATHER_HIGH_LOW = "highlow"
    val weathers = listOf(WEATHER_BOTH, WEATHER_TEMPERATURE, WEATHER_SKY, WEATHER_WORDS, WEATHER_HIGH_LOW)

    // A shape: a rectangle with round corners, a circle, a ring.
    const val SHAPE_RECT = "rect"
    const val SHAPE_CIRCLE = "circle"
    const val SHAPE_RING = "ring"
    val shapes = listOf(SHAPE_RECT, SHAPE_CIRCLE, SHAPE_RING)

    /** The symbols there are, by name; the maker draws each with an icon of its own. */
    val symbols = listOf(
        "heart", "star", "bolt", "sun", "moon", "cloud", "flame", "leaf", "drop", "music",
        "coffee", "home", "bell", "camera", "game", "rocket", "sparkle", "pin", "smile", "check",
    )

    /** What each version is called in the maker. */
    fun name(style: String, kind: String): String = when (kind) {
        PieceKinds.TIME -> when (style) {
            TIME_SECONDS -> "Seconds"
            HOUR -> "Hour"
            MINUTES -> "Minutes"
            TIME_HALF -> "AM/PM"
            else -> "12:45"
        }
        PieceKinds.DATE -> when (style) {
            WEEKDAY -> "Weekday"
            DAY_MONTH -> "Day and month"
            DATE_FULL -> "In full"
            DATE_NUMBERS -> "In numbers"
            DAY_NUMBER -> "Day"
            MONTH -> "Month"
            else -> "Short"
        }
        PieceKinds.BATTERY -> when (style) {
            BATTERY_NUMBER -> "Number"
            BATTERY_BAR -> "Bar"
            else -> "Ring"
        }
        PieceKinds.DIAL -> when (style) {
            DIAL_SECONDS -> "Second hand"
            DIAL_NUMERALS -> "Numbers"
            else -> "Marks"
        }
        PieceKinds.WEATHER -> when (style) {
            WEATHER_TEMPERATURE -> "Temperature"
            WEATHER_SKY -> "Sky"
            WEATHER_WORDS -> "In words"
            WEATHER_HIGH_LOW -> "High and low"
            else -> "Sky and temperature"
        }
        PieceKinds.SHAPE -> when (style) {
            SHAPE_CIRCLE -> "Circle"
            SHAPE_RING -> "Ring"
            else -> "Rectangle"
        }
        else -> style
    }

    /** The versions [kind] has, the one a new piece starts with first; none for a kind with only one. */
    fun of(kind: String): List<String> = when (kind) {
        PieceKinds.TIME -> times
        PieceKinds.DATE -> dates
        PieceKinds.BATTERY -> batteries
        PieceKinds.DIAL -> dials
        PieceKinds.WEATHER -> weathers
        PieceKinds.SHAPE -> shapes
        PieceKinds.SYMBOL -> symbols
        else -> emptyList()
    }
}

/**
 * One piece of a made widget. Its point ([x], [y]) is a share of the widget's width and height, and
 * it lines up on it by [align] across and by its middle down. [size] is the height of its type, or
 * of a ring, a face or a symbol, as a share of the widget's shorter side; a shape or a photo has a
 * box of its own instead, [w] and [h] of the widget's width and height. So a made widget reads the
 * same on any phone, and pulled to another size its pieces keep their places in proportion.
 */
@Immutable
data class Piece(
    val id: String,
    val kind: String,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val size: Float = Pieces.STANDARD,
    val w: Float = 0.5f,
    val h: Float = 0.5f,
    val align: PieceAlign = PieceAlign.CENTER,
    /** As ARGB; 0 for the ink that reads on what it stands on. */
    val colour: Long = 0L,
    val alpha: Float = 1f,
    /** Of the type, from 100 to 900. */
    val weight: Int = 500,
    /** One of [com.gabriel.zenithui.ui.theme.ZenithFonts]; empty for the one the rest of Zenith is set in. */
    val font: String = "",
    /** Which version of its kind it is: see [PieceStyles]. A symbol's name. */
    val style: String = "",
    /** Words of your own; a photo's file, in Zenith's files. */
    val text: String = "",
    /** How round a shape's or a photo's corners are, from square (0) to as round as they go (1). */
    val corner: Float = 0.3f,
    /** Made of glass: words, numbers, symbols and rings as the No glass widgets' are; a shape as a pane. */
    val glass: Boolean = false,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("k", kind)
        put("x", x.toDouble())
        put("y", y.toDouble())
        put("s", size.toDouble())
        put("w", w.toDouble())
        put("h", h.toDouble())
        put("a", align.key)
        if (colour != 0L) put("c", colour)
        put("o", alpha.toDouble())
        put("wt", weight)
        if (font.isNotEmpty()) put("f", font)
        if (style.isNotEmpty()) put("v", style)
        if (text.isNotEmpty()) put("t", text)
        put("r", corner.toDouble())
        if (glass) put("g", true)
    }

    companion object {
        /** A piece as it was written, put right where it is damaged; null for one with no id, or of a kind this Zenith does not know. */
        fun fromJson(o: JSONObject): Piece? {
            val id = o.optString("id")
            val kind = o.optString("k")
            if (id.isEmpty() || kind !in PieceKinds.all) return null
            return Piece(
                id = id,
                kind = kind,
                x = o.share("x", 0.5f, 0f, 1f),
                y = o.share("y", 0.5f, 0f, 1f),
                size = o.share("s", Pieces.STANDARD, Pieces.SMALLEST, Pieces.LARGEST),
                w = o.share("w", 0.5f, Pieces.THINNEST, 1f),
                h = o.share("h", 0.5f, Pieces.THINNEST, 1f),
                align = PieceAlign.of(o.optString("a")),
                colour = o.optLong("c", 0L),
                alpha = o.share("o", 1f, Pieces.FAINTEST, 1f),
                weight = o.optInt("wt", 500).coerceIn(100, 900),
                font = o.optString("f"),
                style = o.optString("v"),
                text = Pieces.words(o.optString("t")),
                corner = o.share("r", 0.3f, 0f, 1f),
                glass = o.optBoolean("g", false),
            )
        }

        private fun JSONObject.share(key: String, default: Float, low: Float, high: Float): Float {
            val value = optDouble(key, default.toDouble())
            return if (value.isNaN()) default else value.toFloat().coerceIn(low, high)
        }
    }
}

/** A widget you made: what it is called, the size it is put on the page at, what it stands on, and its pieces, the lowest first. */
@Immutable
data class MadeWidget(
    val id: String,
    /** Written under it on the home screen, as an icon's name is. */
    val name: String = "",
    /** The columns it takes when put on the page. */
    val spanX: Int = 2,
    /** The rows it takes when put on the page. */
    val rows: Int = 2,
    val back: MadeBack = MadeBack.GLASS,
    /** As ARGB; 0 for none: the glass as it is, or the accent's card. */
    val colour: Long = 0L,
    val pieces: List<Piece> = emptyList(),
) {
    /** Its type on the page. */
    val type: String get() = MadeWidgets.typeOf(id)

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("sx", spanX)
        put("rows", rows)
        put("back", back.key)
        if (colour != 0L) put("colour", colour)
        put("pieces", JSONArray().apply { pieces.forEach { put(it.toJson()) } })
    }

    companion object {
        /** One as it was written, put right where it is damaged rather than lost; null for one with no id. */
        fun fromJson(o: JSONObject): MadeWidget? {
            val id = o.optString("id")
            if (id.isEmpty()) return null
            val array = o.optJSONArray("pieces") ?: JSONArray()
            val pieces = (0 until array.length())
                .mapNotNull { array.optJSONObject(it)?.let(Piece::fromJson) }
                .distinctBy { it.id }
                .take(MadeWidgets.MOST_PIECES)
            return MadeWidget(
                id = id,
                name = o.optString("name").trim().take(MadeWidgets.MOST_NAME),
                spanX = o.optInt("sx", 2).coerceIn(1, MadeWidgets.MOST_COLUMNS),
                rows = o.optInt("rows", 2).coerceIn(1, MadeWidgets.MOST_ROWS),
                back = MadeBack.of(o.optString("back")),
                colour = o.optLong("colour", 0L),
                pieces = pieces,
            )
        }
    }
}

/** The rules for the widgets you make, as a whole: their types on the page, names, and keeping them. */
object MadeWidgets {
    /** What a made widget's type on the page starts with; its id follows. */
    const val PREFIX = "made:"

    /** As many as the gallery keeps. */
    const val MOST = 24

    /** As many pieces as one holds: past this a widget is a page. */
    const val MOST_PIECES = 20

    /** As many letters as a piece of words holds, and as many as a name. */
    const val MOST_LETTERS = 60
    const val MOST_NAME = 24

    /** As big as one can be made: the largest grid there is. */
    const val MOST_COLUMNS = GridDensity.MAX_COLUMNS
    const val MOST_ROWS = GridDensity.MAX_ROWS

    /** What a new one is called, before you call it anything: "My widget", then "My widget 2". */
    const val FIRST_NAME = "My widget"

    fun typeOf(id: String): String = PREFIX + id

    fun isMade(type: String): Boolean = type.length > PREFIX.length && type.startsWith(PREFIX)

    fun idOf(type: String): String? = if (isMade(type)) type.substring(PREFIX.length) else null

    /** The design a widget of [type] on the page is drawn from, if it is still kept. */
    fun find(designs: List<MadeWidget>, type: String): MadeWidget? = idOf(type)?.let { id -> designs.firstOrNull { it.id == id } }

    /**
     * What a made widget is to the layout. As small as one icon, and pulled out to any size from
     * there, as the others are; the size it is put on the page at is its own (see [MadeWidget.spanX]).
     */
    fun spec(type: String): WidgetSpec = WidgetSpec(
        type, "Your widget", "A widget you made.",
        spanX = 2, spanY = 2 * ROW_UNITS, minSpanX = 1, minSpanY = ROW_UNITS,
    )

    fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(12)

    /** A name for a new one that none of [designs] has: the first free of "My widget", "My widget 2", and so on. */
    fun nextName(designs: List<MadeWidget>): String {
        val taken = designs.mapTo(HashSet()) { it.name.trim() }
        if (FIRST_NAME !in taken) return FIRST_NAME
        var n = 2
        while ("$FIRST_NAME $n" in taken) n++
        return "$FIRST_NAME $n"
    }

    fun canMakeMore(designs: List<MadeWidget>): Boolean = designs.size < MOST

    /** [designs] with [design] saved: a new one first, an old one back where it was. A new one past [MOST] is not kept. */
    fun saved(designs: List<MadeWidget>, design: MadeWidget): List<MadeWidget> {
        val at = designs.indexOfFirst { it.id == design.id }
        return when {
            at >= 0 -> designs.toMutableList().also { it[at] = design }
            !canMakeMore(designs) -> designs
            else -> listOf(design) + designs
        }
    }

    /** A widget with nothing on it is not kept when the maker is closed. */
    fun worthKeeping(design: MadeWidget): Boolean = design.pieces.isNotEmpty()

    /** [layout] with every copy of the design [id] taken off the page and out of stacks. */
    fun withoutDesign(layout: List<HomeItem>, id: String): List<HomeItem> = keeping(layout) { idOf(it) != id }

    /** [layout] with every made widget whose design is not among [designs] left out. */
    fun onlyKnown(layout: List<HomeItem>, designs: List<MadeWidget>): List<HomeItem> {
        val ids = designs.mapTo(HashSet()) { it.id }
        return keeping(layout) { type -> idOf(type)?.let { it in ids } ?: true }
    }

    /** [layout] keeping only Zenith's widgets whose type passes [keep]: a stack left with one is that one, in its place. */
    private fun keeping(layout: List<HomeItem>, keep: (String) -> Boolean): List<HomeItem> = layout.mapNotNull { item ->
        when (item) {
            is WidgetCell -> item.takeIf { keep(it.type) }
            is StackCell -> {
                val left = item.members.filter { it.hosted || keep(it.type) }
                when {
                    left.size == item.members.size -> item
                    left.size >= 2 -> item.copy(members = left)
                    left.size == 1 -> StackRules.alone(item, left.first())
                    else -> null
                }
            }
            else -> item
        }
    }

    /** The photos on any of [designs], by file name: the ones that must not be deleted. */
    fun photos(designs: List<MadeWidget>): Set<String> =
        designs.flatMapTo(HashSet()) { design -> design.pieces.filter { it.kind == PieceKinds.PHOTO && it.text.isNotEmpty() }.map { it.text } }
}

/** Changing the pieces of one widget. Each gives the widget changed, or as it was when the change cannot be made. */
object Pieces {
    /** The size a piece is made at when nothing better is known. */
    const val STANDARD = 0.2f

    /** The least and the most a piece's [Piece.size] can be. */
    const val SMALLEST = 0.05f
    const val LARGEST = 1f

    /** The least a shape's or a photo's width or height can be. */
    const val THINNEST = 0.02f

    /** The faintest a piece can be made: past this it is lost. */
    const val FAINTEST = 0.1f

    /** How far a copy is put from the piece it was copied from. */
    const val NUDGE = 0.06f

    /** The most lines a piece of words holds. */
    const val MOST_LINES = 3

    /** A piece of [kind], in the middle, as it is most often wanted. */
    fun new(kind: String, id: String): Piece = when (kind) {
        PieceKinds.TIME -> Piece(id, kind, size = 0.36f, weight = 600, style = PieceStyles.TIME)
        PieceKinds.DATE -> Piece(id, kind, size = 0.12f, weight = 600, style = PieceStyles.DATE_SHORT)
        PieceKinds.TEXT -> Piece(id, kind, size = 0.14f, weight = 600, text = "Your words")
        PieceKinds.BATTERY -> Piece(id, kind, size = 0.5f, weight = 600, style = PieceStyles.BATTERY_RING)
        PieceKinds.DIAL -> Piece(id, kind, size = 0.8f, style = PieceStyles.DIAL_MARKS)
        PieceKinds.WEATHER -> Piece(id, kind, size = 0.2f, weight = 500, style = PieceStyles.WEATHER_BOTH)
        PieceKinds.SYMBOL -> Piece(id, kind, size = 0.24f, style = PieceStyles.symbols.first())
        PieceKinds.SHAPE -> Piece(id, kind, w = 0.6f, h = 0.3f, corner = 0.5f, style = PieceStyles.SHAPE_RECT)
        PieceKinds.PHOTO -> Piece(id, kind, w = 0.8f, h = 0.8f, corner = 0.25f)
        PieceKinds.GREETING -> Piece(id, kind, size = 0.12f, weight = 600)
        PieceKinds.ALARM -> Piece(id, kind, size = 0.1f, weight = 500)
        else -> Piece(id, kind)
    }

    fun canAdd(design: MadeWidget): Boolean = design.pieces.size < MadeWidgets.MOST_PIECES

    /** [design] with [piece] on top of the others. */
    fun added(design: MadeWidget, piece: Piece): MadeWidget =
        if (!canAdd(design)) design else design.copy(pieces = design.pieces + piece)

    /** [design] with a copy of the piece [id], as [newId], just off it and over it. */
    fun duplicated(design: MadeWidget, id: String, newId: String): MadeWidget {
        val at = design.pieces.indexOfFirst { it.id == id }
        if (at < 0 || !canAdd(design)) return design
        val piece = design.pieces[at]
        val copy = piece.copy(id = newId, x = nudged(piece.x), y = nudged(piece.y))
        return design.copy(pieces = design.pieces.toMutableList().also { it.add(at + 1, copy) })
    }

    private fun nudged(share: Float): Float = if (share + NUDGE <= 1f) share + NUDGE else share - NUDGE

    /** [design] with the piece [id] drawn over the one above it. */
    fun forward(design: MadeWidget, id: String): MadeWidget {
        val at = design.pieces.indexOfFirst { it.id == id }
        if (at < 0 || at == design.pieces.lastIndex) return design
        return design.copy(pieces = design.pieces.toMutableList().also { java.util.Collections.swap(it, at, at + 1) })
    }

    /** [design] with the piece [id] drawn under the one below it. */
    fun backward(design: MadeWidget, id: String): MadeWidget {
        val at = design.pieces.indexOfFirst { it.id == id }
        if (at <= 0) return design
        return design.copy(pieces = design.pieces.toMutableList().also { java.util.Collections.swap(it, at, at - 1) })
    }

    /** [design] with [piece] in place of the one with its id. */
    fun changed(design: MadeWidget, piece: Piece): MadeWidget =
        design.copy(pieces = design.pieces.map { if (it.id == piece.id) piece else it })

    fun removed(design: MadeWidget, id: String): MadeWidget = design.copy(pieces = design.pieces.filter { it.id != id })

    /** [piece] made [by] times bigger, within its limits: a shape or a photo both ways, anything else its size. */
    fun scaled(piece: Piece, by: Float): Piece =
        if (PieceKinds.isBox(piece.kind)) {
            piece.copy(w = (piece.w * by).coerceIn(THINNEST, 1f), h = (piece.h * by).coerceIn(THINNEST, 1f))
        } else {
            piece.copy(size = (piece.size * by).coerceIn(SMALLEST, LARGEST))
        }

    /** Words as a piece keeps them: no more than [MadeWidgets.MOST_LETTERS] letters, on no more than [MOST_LINES] lines. */
    fun words(text: String): String = text.lines().take(MOST_LINES).joinToString("\n").take(MadeWidgets.MOST_LETTERS)
}

/** A starting point for a new widget, offered while it has nothing on it. */
@Immutable
data class Starter(
    val name: String,
    val spanX: Int,
    val rows: Int,
    val back: MadeBack,
    val colour: Long,
    val pieces: List<Piece>,
) {
    /** A new widget made from it, as [id] and called [name]. */
    fun madeAs(id: String, name: String): MadeWidget = MadeWidget(id, name, spanX, rows, back, colour, pieces)
}

object MadeStarters {
    val all = listOf(
        Starter(
            "Big time", 2, 2, MadeBack.GLASS, 0L,
            listOf(
                Piece("time", PieceKinds.TIME, x = 0.5f, y = 0.42f, size = 0.4f, weight = 300, style = PieceStyles.TIME),
                Piece("date", PieceKinds.DATE, x = 0.5f, y = 0.76f, size = 0.11f, weight = 600, alpha = 0.7f, style = PieceStyles.DATE_FULL),
            ),
        ),
        Starter(
            "Good morning", 4, 2, MadeBack.COLOUR, 0xFF0A84FF,
            listOf(
                Piece("greeting", PieceKinds.GREETING, x = 0.06f, y = 0.24f, size = 0.12f, weight = 600, align = PieceAlign.START, alpha = 0.85f),
                Piece("time", PieceKinds.TIME, x = 0.06f, y = 0.56f, size = 0.34f, weight = 600, align = PieceAlign.START, style = PieceStyles.TIME),
                Piece("weather", PieceKinds.WEATHER, x = 0.94f, y = 0.24f, size = 0.16f, weight = 600, align = PieceAlign.END, style = PieceStyles.WEATHER_BOTH),
                Piece("date", PieceKinds.DATE, x = 0.06f, y = 0.84f, size = 0.11f, weight = 600, align = PieceAlign.START, alpha = 0.75f, style = PieceStyles.DATE_FULL),
            ),
        ),
        Starter(
            "Power", 2, 2, MadeBack.CLEAR, 0L,
            listOf(
                Piece("ring", PieceKinds.BATTERY, x = 0.5f, y = 0.45f, size = 0.66f, weight = 600, style = PieceStyles.BATTERY_RING, glass = true),
                Piece("bolt", PieceKinds.SYMBOL, x = 0.5f, y = 0.88f, size = 0.14f, style = "bolt", glass = true),
            ),
        ),
        Starter(
            "Words", 4, 2, MadeBack.COLOUR, 0xFFFF9F0A,
            listOf(
                Piece("spark", PieceKinds.SYMBOL, x = 0.06f, y = 0.24f, size = 0.16f, align = PieceAlign.START, style = "sparkle"),
                Piece("words", PieceKinds.TEXT, x = 0.06f, y = 0.56f, size = 0.2f, weight = 800, align = PieceAlign.START, text = "Make it happen"),
                Piece("date", PieceKinds.DATE, x = 0.06f, y = 0.84f, size = 0.11f, weight = 600, align = PieceAlign.START, alpha = 0.75f, style = PieceStyles.DATE_SHORT),
            ),
        ),
    )
}
