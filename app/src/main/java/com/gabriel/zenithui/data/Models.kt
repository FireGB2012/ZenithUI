package com.gabriel.zenithui.data

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * A home row is divided into this many vertical units. Icons take a whole row; a slim widget such
 * as the search bar can take a quarter of one, so it never blocks a row you wanted apps in.
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

    fun moved(page: Int, col: Int, row: Int): HomeItem
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
) : HomeItem {
    override val spanX: Int get() = 1
    override val spanY: Int get() = ROW_UNITS
    val key: String get() = "$packageName/$className"

    override fun moved(page: Int, col: Int, row: Int) = copy(page = page, col = col, row = row)

    override fun toJson(): JSONObject = JSONObject().apply {
        put("t", "app")
        put("id", id); put("p", page); put("c", col); put("r", row)
        put("pkg", packageName); put("cls", className)
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
) : HomeItem {
    override val spanX: Int get() = 1
    override val spanY: Int get() = ROW_UNITS

    override fun moved(page: Int, col: Int, row: Int) = copy(page = page, col = col, row = row)

    override fun toJson(): JSONObject = JSONObject().apply {
        put("t", "folder")
        put("id", id); put("p", page); put("c", col); put("r", row)
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
) : HomeItem {
    override fun moved(page: Int, col: Int, row: Int) = copy(page = page, col = col, row = row)

    override fun toJson(): JSONObject = JSONObject().apply {
        put("t", "widget")
        put("id", id); put("p", page); put("c", col); put("r", row)
        put("type", type); put("sx", spanX); put("sy", spanY)
    }
}

/** Catalogue of the widgets Zenith ships with, and the sizes each one may take. */
@Immutable
data class WidgetSpec(
    val type: String,
    val title: String,
    val description: String,
    val spanX: Int,
    val spanY: Int,
    val minSpanX: Int = 1,
    val minSpanY: Int = 1,
    val maxSpanX: Int = 4,
    val maxSpanY: Int = 3,
    /** Search stretches across whatever grid you chose. */
    val fullWidth: Boolean = false,
)

object Widgets {
    val Clock = WidgetSpec(
        "clock", "Zenith Clock",
        "Time, date and a greeting that knows your name.",
        spanX = 2, spanY = 2 * ROW_UNITS, minSpanX = 2, minSpanY = 2, maxSpanX = 6, maxSpanY = 3 * ROW_UNITS,
    )
    val Battery = WidgetSpec(
        "battery", "Power",
        "Live battery ring that tracks charge and charging state.",
        spanX = 2, spanY = 2 * ROW_UNITS, minSpanX = 1, minSpanY = 3, maxSpanX = 4, maxSpanY = 3 * ROW_UNITS,
    )
    val Calendar = WidgetSpec(
        "calendar", "Calendar",
        "The month at a glance with today highlighted.",
        spanX = 2, spanY = 2 * ROW_UNITS, minSpanX = 2, minSpanY = 6, maxSpanX = 6, maxSpanY = 4 * ROW_UNITS,
    )
    val Notes = WidgetSpec(
        "notes", "Quick Note",
        "A scratchpad that stays on your home screen.",
        spanX = 2, spanY = 2 * ROW_UNITS, minSpanX = 2, minSpanY = 3, maxSpanX = 6, maxSpanY = 4 * ROW_UNITS,
    )
    val Toggles = WidgetSpec(
        "toggles", "Quick Toggles",
        "Torch, Wi-Fi, Bluetooth and Do Not Disturb.",
        spanX = 2, spanY = 2 * ROW_UNITS, minSpanX = 2, minSpanY = 3, maxSpanX = 6, maxSpanY = 2 * ROW_UNITS,
    )
    val Search = WidgetSpec(
        "search", "Search Bar",
        "One line: search your apps, or jump straight to Google.",
        // Two units tall: the bar's real height, not a whole row.
        spanX = 4, spanY = 2, minSpanX = 2, minSpanY = 2, maxSpanX = 6, maxSpanY = ROW_UNITS,
        fullWidth = true,
    )

    val all = listOf(Clock, Battery, Calendar, Notes, Toggles, Search)

    fun byType(type: String): WidgetSpec? = all.firstOrNull { it.type == type }

    /** The size a widget takes when first placed. [rows] is in rows; spans are in row units. */
    fun defaultSpan(spec: WidgetSpec, columns: Int, rows: Int): Pair<Int, Int> =
        clampSpan(spec, if (spec.fullWidth) columns else scaleX(spec.spanX, columns), spec.spanY, columns, rows)

    /**
     * Spans are declared against a four column grid, so on a wider one they are scaled to keep
     * their share of it: half the width on a phone stays half the width on a tablet.
     */
    private fun scaleX(spanX: Int, columns: Int): Int =
        (spanX * columns / GridDensity.BASE_COLUMNS.toFloat()).roundToInt().coerceAtLeast(1)

    fun clampSpan(spec: WidgetSpec, spanX: Int, spanY: Int, columns: Int, rows: Int): Pair<Int, Int> {
        val widest = minOf(scaleX(spec.maxSpanX, columns), columns).coerceAtLeast(spec.minSpanX)
        return spanX.coerceIn(spec.minSpanX, widest) to
            spanY.coerceIn(spec.minSpanY, minOf(spec.maxSpanY, rows * ROW_UNITS))
    }
}

fun homeItemFromJson(o: JSONObject): HomeItem? = when (o.optString("t")) {
    "app" -> AppCell(
        id = o.optString("id"), page = o.optInt("p"), col = o.optInt("c"), row = o.optInt("r"),
        packageName = o.optString("pkg"), className = o.optString("cls"),
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
