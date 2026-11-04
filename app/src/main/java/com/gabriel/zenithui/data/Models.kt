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
    /** In the work profile: kept apart from the rest, and opened there. See [WorkKeys]. */
    val work: Boolean = false,
) {
    val key: String get() = if (work) WorkKeys.of("$packageName/$className") else "$packageName/$className"

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
    /** Its own colour, as ARGB; 0 for the glass as it is. */
    val colour: Long = 0L,
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
        if (colour != 0L) put("col", colour)
    }
}

/**
 * One of an app's shortcuts put on the page as an icon of its own: a route home, one chat. Its
 * picture is the shortcut's own icon with the app's in the corner, and Android keeps the shortcut
 * pinned for as long as it is on a page.
 */
@Immutable
data class ShortcutCell(
    override val id: String,
    override val page: Int,
    override val col: Int,
    override val row: Int,
    val packageName: String,
    val shortcutId: String,
    /** The shortcut's name when it was put here, for when the app cannot be asked. */
    val label: String,
    override val dx: Float = 0f,
    override val dy: Float = 0f,
) : HomeItem {
    override val spanX: Int get() = 1
    override val spanY: Int get() = ROW_UNITS

    /** What it is known by when it is drawn, opened and landed back into: see [ShortcutKeys]. */
    val key: String get() = ShortcutKeys.of(packageName, shortcutId)

    override fun moved(page: Int, col: Int, row: Int) = copy(page = page, col = col, row = row, dx = 0f, dy = 0f)

    override fun nudged(dx: Float, dy: Float) = copy(dx = dx, dy = dy)

    override fun toJson(): JSONObject = JSONObject().apply {
        put("t", "shortcut")
        put("id", id); put("p", page); put("c", col); put("r", row)
        if (dx != 0f || dy != 0f) { put("dx", dx.toDouble()); put("dy", dy.toDouble()) }
        put("pkg", packageName); put("sid", shortcutId); put("label", label)
    }
}

/**
 * A shortcut is known by a key of its own, `shortcut:<package>/<id>`, which is never mistaken for
 * an app's: icons, the card an app opens through and the landing all go by it.
 */
object ShortcutKeys {
    const val PREFIX = "shortcut:"

    fun of(packageName: String, shortcutId: String): String = "$PREFIX$packageName/$shortcutId"

    fun isShortcut(key: String): Boolean = key.startsWith(PREFIX)

    /** The package and the shortcut's id. An id can hold a slash; a package name never does. */
    fun parse(key: String): Pair<String, String>? {
        if (!isShortcut(key)) return null
        val parts = key.removePrefix(PREFIX).split('/', limit = 2)
        return if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) parts[0] to parts[1] else null
    }
}

/** One of an app's shortcuts, as its long-press menu lists it. */
@Immutable
data class AppShortcut(val packageName: String, val id: String, val label: String) {
    val key: String get() = ShortcutKeys.of(packageName, id)
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
    /** Its own accent, as ARGB; 0 for the theme's. */
    val accent: Long = 0L,
) : HomeItem {
    override fun moved(page: Int, col: Int, row: Int) = copy(page = page, col = col, row = row, dx = 0f, dy = 0f)

    override fun nudged(dx: Float, dy: Float) = copy(dx = dx, dy = dy)

    override fun toJson(): JSONObject = JSONObject().apply {
        put("t", "widget")
        put("id", id); put("p", page); put("c", col); put("r", row)
        if (dx != 0f || dy != 0f) { put("dx", dx.toDouble()); put("dy", dy.toDouble()) }
        put("type", type); put("sx", spanX); put("sy", spanY)
        if (accent != 0L) put("acc", accent)
    }
}

/**
 * Another app's widget on the page — Spotify's, Gmail's, Google's — drawn by that app through
 * Android's widget host, in a frame on Zenith's grid like any widget of its own.
 */
@Immutable
data class HostedWidgetCell(
    override val id: String,
    override val page: Int,
    override val col: Int,
    override val row: Int,
    override val spanX: Int,
    override val spanY: Int,
    /** Android's number for this one widget: what the host draws it by. Only good on this phone. */
    val appWidgetId: Int,
    /** Which widget it is, as a flattened component name: how it is found again after a restore. */
    val provider: String,
    /**
     * A solid tile behind it (saved as "glass", from when it was a pane of glass). Off by default:
     * most widgets bring a background of their own.
     */
    val glass: Boolean = false,
    override val dx: Float = 0f,
    override val dy: Float = 0f,
) : HomeItem {
    override fun moved(page: Int, col: Int, row: Int) = copy(page = page, col = col, row = row, dx = 0f, dy = 0f)

    override fun nudged(dx: Float, dy: Float) = copy(dx = dx, dy = dy)

    override fun toJson(): JSONObject = JSONObject().apply {
        put("t", "hosted")
        put("id", id); put("p", page); put("c", col); put("r", row)
        if (dx != 0f || dy != 0f) { put("dx", dx.toDouble()); put("dy", dy.toDouble()) }
        put("sx", spanX); put("sy", spanY)
        put("wid", appWidgetId); put("prov", provider)
        if (glass) put("glass", true)
    }
}

/**
 * One widget of a stack: one of Zenith's by its type, or another app's by its host number. It
 * keeps the id it had as a widget of its own, so whatever was set for it stays with it.
 */
@Immutable
data class StackMember(
    val id: String,
    val type: String = "",
    val appWidgetId: Int = 0,
    val provider: String = "",
) {
    val hosted: Boolean get() = appWidgetId != 0

    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        if (hosted) { put("wid", appWidgetId); put("prov", provider) } else put("type", type)
    }

    companion object {
        fun fromJson(o: JSONObject): StackMember? {
            val id = o.optString("id")
            val wid = o.optInt("wid", 0)
            val type = o.optString("type")
            return when {
                id.isEmpty() -> null
                wid != 0 -> StackMember(id, appWidgetId = wid, provider = o.optString("prov"))
                type.isNotEmpty() -> StackMember(id, type = Widgets.solid(type))
                else -> null
            }
        }
    }
}

/**
 * Several widgets in one spot, one over another: a swipe up or down brings the next, as with the
 * Smart Stacks on an iPhone. Made from a widget's menu, and taken apart from it too.
 */
@Immutable
data class StackCell(
    override val id: String,
    override val page: Int,
    override val col: Int,
    override val row: Int,
    override val spanX: Int,
    override val spanY: Int,
    /** What it holds, the one on top first. Never fewer than two: one alone is a widget again. */
    val members: List<StackMember>,
    override val dx: Float = 0f,
    override val dy: Float = 0f,
) : HomeItem {
    override fun moved(page: Int, col: Int, row: Int) = copy(page = page, col = col, row = row, dx = 0f, dy = 0f)

    override fun nudged(dx: Float, dy: Float) = copy(dx = dx, dy = dy)

    override fun toJson(): JSONObject = JSONObject().apply {
        put("t", "stack")
        put("id", id); put("p", page); put("c", col); put("r", row)
        if (dx != 0f || dy != 0f) { put("dx", dx.toDouble()); put("dy", dy.toDouble()) }
        put("sx", spanX); put("sy", spanY)
        put("members", JSONArray().apply { members.forEach { put(it.toJson()) } })
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
)

object Widgets {
    /**
     * Marked a "No glass" version of a widget, in layouts saved before every widget became a solid
     * tile. Such a widget is read as the widget it was a version of ([solid]).
     */
    private const val BARE_PREFIX = "bare_"

    /** The widget a type draws: "bare_clock", from before the iPhone look, is a clock. */
    fun solid(type: String): String = type.removePrefix(BARE_PREFIX)

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
    val Media = WidgetSpec(
        "media", "Now playing",
        "What is playing, with play, pause and skip.",
        spanX = 4, spanY = 2 * ROW_UNITS, minSpanX = 2, minSpanY = ROW_UNITS,
    )
    val WorldClock = WidgetSpec(
        "world", "World clock",
        "The time in the cities you choose, and whether it is day there.",
        spanX = 2, spanY = 2 * ROW_UNITS, minSpanX = 2, minSpanY = ROW_UNITS,
    )
    val Countdown = WidgetSpec(
        "countdown", "Countdown",
        "The days to a date you name, on a ring that fills as it comes.",
        spanX = 2, spanY = 2 * ROW_UNITS, minSpanX = 1, minSpanY = ROW_UNITS,
    )
    val Photos = WidgetSpec(
        "photo", "Photo frame",
        "Your photos, a different one each time you come home.",
        spanX = 2, spanY = 2 * ROW_UNITS, minSpanX = 1, minSpanY = ROW_UNITS,
    )
    val Todo = WidgetSpec(
        "todo", "To-do",
        "A short list, ticked off as you go.",
        spanX = 2, spanY = 2 * ROW_UNITS, minSpanX = 2, minSpanY = ROW_UNITS,
    )
    val Streak = WidgetSpec(
        "aura", "Aura",
        "Your streak, the week behind it and your aura.",
        spanX = 2, spanY = 2 * ROW_UNITS, minSpanX = 1, minSpanY = ROW_UNITS,
    )
    val Weather = WidgetSpec(
        "weather", "Weather",
        "Now, today's high and low, and the hours ahead.",
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

    val all = listOf(Clock, Battery, Calendar, Notes, Toggles, Media, WorldClock, Countdown, Photos, Todo, Streak, Weather, Search)

    /** The widgets that can be put on a page. */
    val placeable = listOf(Clock, Battery, Calendar, Notes, Toggles, Media, WorldClock, Countdown, Photos, Todo, Streak, Weather)

    fun byType(type: String): WidgetSpec? = solid(type).let { kind -> all.firstOrNull { it.type == kind } }

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
        colour = o.optLong("col", 0L),
    )

    "widget" -> WidgetCell(
        id = o.optString("id"), page = o.optInt("p"), col = o.optInt("c"), row = o.optInt("r"),
        // A "No glass" widget from before the iPhone look is its solid self from now on.
        type = Widgets.solid(o.optString("type")), spanX = o.optInt("sx", 2).coerceAtLeast(1), spanY = o.optInt("sy", 2).coerceAtLeast(1),
        accent = o.optLong("acc", 0L),
    )

    "shortcut" -> {
        val pkg = o.optString("pkg")
        val sid = o.optString("sid")
        if (pkg.isEmpty() || sid.isEmpty()) null else ShortcutCell(
            id = o.optString("id"), page = o.optInt("p"), col = o.optInt("c"), row = o.optInt("r"),
            packageName = pkg, shortcutId = sid, label = o.optString("label"),
        )
    }

    "hosted" -> {
        val provider = o.optString("prov")
        if (provider.isEmpty()) null else HostedWidgetCell(
            id = o.optString("id"), page = o.optInt("p"), col = o.optInt("c"), row = o.optInt("r"),
            spanX = o.optInt("sx", 2).coerceAtLeast(1), spanY = o.optInt("sy", 2 * ROW_UNITS).coerceAtLeast(1),
            appWidgetId = o.optInt("wid", 0), provider = provider, glass = o.optBoolean("glass", false),
        )
    }

    "stack" -> {
        val array = o.optJSONArray("members") ?: JSONArray()
        val members = (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(StackMember::fromJson) }
        val stack = StackCell(
            id = o.optString("id"), page = o.optInt("p"), col = o.optInt("c"), row = o.optInt("r"),
            spanX = o.optInt("sx", 2).coerceAtLeast(1), spanY = o.optInt("sy", 2 * ROW_UNITS).coerceAtLeast(1),
            members = members,
        )
        // A stack written with one widget left in it is that widget; with none, nothing.
        when (members.size) {
            0 -> null
            1 -> StackRules.alone(stack, members.first())
            else -> stack
        }
    }

    else -> null
}

fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { optString(it).takeIf { s -> s.isNotEmpty() } }
}

/** How long something taken off the home screen can be put back. */
const val UNDO_MS = 5000L

/** [this], the same thing in the same place, under the id [id]. */
fun HomeItem.withId(id: String): HomeItem = when (this) {
    is AppCell -> copy(id = id)
    is FolderCell -> copy(id = id)
    is WidgetCell -> copy(id = id)
    is ShortcutCell -> copy(id = id)
    is HostedWidgetCell -> copy(id = id)
    is StackCell -> copy(id = id)
}
