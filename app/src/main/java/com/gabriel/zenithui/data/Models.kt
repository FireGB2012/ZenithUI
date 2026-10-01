package com.gabriel.zenithui.data

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * A home row is divided into this many vertical units. Icons and widgets keep to whole rows, so
 * everything on a page lines up; the quarter steps are what Plus free placement puts things on.
 */
const val ROW_UNITS = 4

/** A launchable activity discovered on the device. */
@Immutable
data class AppInfo(
    val packageName: String,
    val className: String,
    val label: String,
    val system: Boolean = false,
) {
    val key: String get() = "$packageName/$className"

    companion object {
        fun keyOf(packageName: String, className: String) = "$packageName/$className"
    }
}

/** Anything that can occupy cells on a home page. */
sealed interface HomeItem {
    val id: String
    val page: Int
    val col: Int
    val row: Int
    val spanX: Int
    val spanY: Int

    /**
     * Free placement (Plus): how far off its cell the item was let go, as a fraction of a cell
     * across and a row unit down, within half a cell either way. Zero for anything placed normally.
     */
    val dx: Float
    val dy: Float

    /** Moves to a cell, dropping any free-placement offset. */
    fun moved(page: Int, col: Int, row: Int): HomeItem

    /** Keeps the cell, sets the free-placement offset. */
    fun nudged(dx: Float, dy: Float): HomeItem
    fun toJson(): JSONObject
}

@Immutable
data class AppCell(
    override val id: String,
    override val page: Int,
    override val col: Int,
    override val row: Int,
    val packageName: String,
    val className: String,
    override val dx: Float = 0f,
    override val dy: Float = 0f,
    /**
     * One column by one row normally. A super icon is any number of columns wide and whole rows
     * tall, up to the whole page: the icon stretched to fill it.
     */
    override val spanX: Int = 1,
    override val spanY: Int = ROW_UNITS,
) : HomeItem {
    val key: String get() = "$packageName/$className"

    /** An ordinary one-cell icon: the only kind that folders are made from. */
    val isPlainIcon: Boolean get() = spanX == 1 && spanY == ROW_UNITS

    override fun moved(page: Int, col: Int, row: Int) = copy(page = page, col = col, row = row, dx = 0f, dy = 0f)

    override fun nudged(dx: Float, dy: Float) = copy(dx = dx, dy = dy)

    override fun toJson(): JSONObject = JSONObject().apply {
        put("t", "app")
        put("id", id); put("p", page); put("c", col); put("r", row)
        if (dx != 0f || dy != 0f) { put("dx", dx.toDouble()); put("dy", dy.toDouble()) }
        put("pkg", packageName); put("cls", className)
        if (spanX != 1 || spanY != ROW_UNITS) { put("sx", spanX); put("sy", spanY) }
    }
}

@Immutable
data class FolderCell(
    override val id: String,
    override val page: Int,
    override val col: Int,
    override val row: Int,
    val title: String,
    val keys: List<String>,
    override val dx: Float = 0f,
    override val dy: Float = 0f,
) : HomeItem {
    override val spanX: Int get() = 1
    override val spanY: Int get() = ROW_UNITS

    override fun moved(page: Int, col: Int, row: Int) = copy(page = page, col = col, row = row, dx = 0f, dy = 0f)

    override fun nudged(dx: Float, dy: Float) = copy(dx = dx, dy = dy)

    override fun toJson(): JSONObject = JSONObject().apply {
        put("t", "folder")
        put("id", id); put("p", page); put("c", col); put("r", row)
        if (dx != 0f || dy != 0f) { put("dx", dx.toDouble()); put("dy", dy.toDouble()) }
        put("title", title)
        put("keys", JSONArray(keys))
    }
}

@Immutable
data class WidgetCell(
    override val id: String,
    override val page: Int,
    override val col: Int,
    override val row: Int,
    val type: String,
    override val spanX: Int,
    override val spanY: Int,
    override val dx: Float = 0f,
    override val dy: Float = 0f,
) : HomeItem {
    override fun moved(page: Int, col: Int, row: Int) = copy(page = page, col = col, row = row, dx = 0f, dy = 0f)

    override fun nudged(dx: Float, dy: Float) = copy(dx = dx, dy = dy)

    override fun toJson(): JSONObject = JSONObject().apply {
        put("t", "widget")
        put("id", id); put("p", page); put("c", col); put("r", row)
        if (dx != 0f || dy != 0f) { put("dx", dx.toDouble()); put("dy", dy.toDouble()) }
        put("type", type); put("sx", spanX); put("sy", spanY)
    }
}

/**
 * Catalogue of the widgets Zenith ships with: the size each starts at and the smallest it can be
 * made. There is no largest; any widget can be pulled out to fill the whole page.
 */
@Immutable
data class WidgetSpec(
    val type: String,
    val title: String,
    val description: String,
    val spanX: Int,
    val spanY: Int,
    val minSpanX: Int = 1,
    val minSpanY: Int = 1,
    /** Search stretches across whatever grid you chose. */
    val fullWidth: Boolean = false,
) {
    /** A "No glass" widget: no pane behind it; its numbers and icons are the glass. */
    val isBare: Boolean get() = type.startsWith(Widgets.BARE_PREFIX)
}

object Widgets {
    /** Marks a "No glass" version of a widget; what follows is the widget it is a version of. */
    const val BARE_PREFIX = "bare_"

    /** The widget a type draws, whichever section it came from: "bare_clock" is a clock. */
    fun baseType(type: String): String = type.removePrefix(BARE_PREFIX)

    val Clock = WidgetSpec(
        "clock", "Zenith Clock",
        "Time, date and a greeting that knows your name.",
        spanX = 2, spanY = 2 * ROW_UNITS, minSpanX = 2, minSpanY = ROW_UNITS,
    )
    val Battery = WidgetSpec(
        "battery", "Power",
        "Live battery ring that tracks charge and charging state.",
        spanX = 2, spanY = 2 * ROW_UNITS, minSpanX = 1, minSpanY = ROW_UNITS,
    )
    val Calendar = WidgetSpec(
        "calendar", "Calendar",
        "The month at a glance with today highlighted.",
        spanX = 2, spanY = 2 * ROW_UNITS, minSpanX = 2, minSpanY = 2 * ROW_UNITS,
    )
    val Notes = WidgetSpec(
        "notes", "Quick Note",
        "A scratchpad that stays on your home screen.",
        spanX = 2, spanY = 2 * ROW_UNITS, minSpanX = 2, minSpanY = ROW_UNITS,
    )
    val Toggles = WidgetSpec(
        "toggles", "Quick Toggles",
        "Torch, Wi-Fi, Bluetooth and Do Not Disturb.",
        spanX = 2, spanY = 2 * ROW_UNITS, minSpanX = 2, minSpanY = ROW_UNITS,
    )

    // No longer placed on a page: the search bar is the bar above the dock, switched on in
    // settings. The type stays so that layouts saved with one can be read and converted.
    val Search = WidgetSpec(
        "search", "Search Bar",
        "One line: search your apps, or jump straight to Google.",
        // Two units tall: the bar's real height, not a whole row.
        spanX = 4, spanY = 2, minSpanX = 2, minSpanY = 2,
        fullWidth = true,
    )

    val all = listOf(Clock, Battery, Calendar, Notes, Toggles, Search)

    /** The widgets that can be put on a page. */
    val placeable = listOf(Clock, Battery, Calendar, Notes, Toggles)

    /**
     * The "No glass" section: the same widgets with no pane round them. Their numbers, rings and
     * icons are made of glass instead, like the clock on an iPhone's Lock Screen.
     */
    val noGlass = listOf(
        Clock.copy(type = BARE_PREFIX + Clock.type, title = "Glass Clock", description = "Big glass numbers straight on the wallpaper, with a sun or moon for the time of day."),
        Battery.copy(type = BARE_PREFIX + Battery.type, title = "Glass Power", description = "A glass ring and battery icon floating on the wallpaper."),
        Calendar.copy(type = BARE_PREFIX + Calendar.type, title = "Glass Calendar", description = "Today's date in glass, with the month around it."),
        Notes.copy(type = BARE_PREFIX + Notes.type, title = "Glass Note", description = "Your note written on the wallpaper, under a glass pencil."),
        Toggles.copy(type = BARE_PREFIX + Toggles.type, title = "Glass Toggles", description = "Your quick controls as glass icons, lit in colour when on."),
    )

    fun byType(type: String): WidgetSpec? = all.firstOrNull { it.type == type } ?: noGlass.firstOrNull { it.type == type }

    /** The size a widget takes when first placed. [rows] is in rows; spans are in row units. */
    fun defaultSpan(spec: WidgetSpec, columns: Int, rows: Int): Pair<Int, Int> =
        clampSpan(spec, if (spec.fullWidth) columns else scaleX(spec.spanX, columns), spec.spanY, columns, rows)

    /**
     * Spans are declared against a four column grid, so on a wider one they are scaled to keep
     * their share of it: half the width on a phone stays half the width on a tablet.
     */
    private fun scaleX(spanX: Int, columns: Int): Int =
        (spanX * columns / GridDensity.BASE_COLUMNS.toFloat()).roundToInt().coerceAtLeast(1)

    /** Any size from the widget's smallest up to the whole page. */
    fun clampSpan(spec: WidgetSpec, spanX: Int, spanY: Int, columns: Int, rows: Int): Pair<Int, Int> =
        spanX.coerceIn(spec.minSpanX, columns.coerceAtLeast(spec.minSpanX)) to
            spanY.coerceIn(spec.minSpanY, (rows * ROW_UNITS).coerceAtLeast(spec.minSpanY))
}

fun homeItemFromJson(o: JSONObject): HomeItem? =
    baseItemFromJson(o)?.let { item ->
        // Only items placed freely carry an offset; everything else reads back exactly as before.
        val dx = o.optDouble("dx", 0.0).toFloat()
        val dy = o.optDouble("dy", 0.0).toFloat()
        if (dx == 0f && dy == 0f) item else item.nudged(dx, dy)
    }

private fun baseItemFromJson(o: JSONObject): HomeItem? = when (o.optString("t")) {
    "app" -> AppCell(
        id = o.optString("id"), page = o.optInt("p"), col = o.optInt("c"), row = o.optInt("r"),
        packageName = o.optString("pkg"), className = o.optString("cls"),
        spanX = o.optInt("sx", 1), spanY = o.optInt("sy", ROW_UNITS),
    )

    "folder" -> FolderCell(
        id = o.optString("id"), page = o.optInt("p"), col = o.optInt("c"), row = o.optInt("r"),
        title = o.optString("title", "Folder"),
        keys = o.optJSONArray("keys").toStringList(),
    )

    "widget" -> WidgetCell(
        id = o.optString("id"), page = o.optInt("p"), col = o.optInt("c"), row = o.optInt("r"),
        type = o.optString("type"), spanX = o.optInt("sx", 2).coerceAtLeast(1), spanY = o.optInt("sy", 2).coerceAtLeast(1),
    )

    else -> null
}

fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotEmpty() } }
}
