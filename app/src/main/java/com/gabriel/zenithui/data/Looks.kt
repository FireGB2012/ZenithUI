package com.gabriel.zenithui.data

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.LocalDateTime

/**
 * A Look: everything you see on the home screen, saved under a name — the palette, light or dark,
 * the glass, the wallpaper design, the icons, the grid, the dock, the layout and the motion — and
 * nothing that is yours alone: not your name, your note or lists, your hidden apps, Aura or Plus.
 * Kept as the very JSON the settings and layout are saved as, so a Look made today still reads
 * when settings have grown by tomorrow.
 */
@Immutable
data class Look(
    val id: String,
    val name: String,
    /** The settings a Look keeps ([Looks.KEYS]), as JSON text. */
    val settings: String,
    /** The layout, as the JSON array a backup holds. */
    val layout: String,
    val savedAt: Long,
    /** The code it was shared under, while it is; empty when it is not shared. */
    val shared: String = "",
    /** When it was shared: a Look saved over since is not what its code gives. */
    val sharedAt: Long = 0L,
) {
    /** Shared, and saved over since: the code gives it as it was. */
    val sharedIsOld: Boolean get() = shared.isNotEmpty() && savedAt > sharedAt

    fun toJson(): JSONObject = JSONObject().apply {
        put("format", Looks.FORMAT)
        put("version", 1)
        put("id", id)
        put("name", name)
        put("savedAt", savedAt)
        put("settings", JSONObject(settings))
        put("layout", JSONArray(layout))
        if (shared.isNotEmpty()) {
            put("shared", shared)
            put("sharedAt", sharedAt)
        }
    }

    companion object {
        fun fromJson(o: JSONObject): Look? = runCatching {
            if (o.optString("format") != Looks.FORMAT) return null
            Look(
                id = o.optString("id").ifEmpty { LayoutBuilder.newId() },
                name = o.optString("name").trim().ifEmpty { "A Look" },
                settings = (o.optJSONObject("settings") ?: JSONObject()).toString(),
                layout = (o.optJSONArray("layout") ?: JSONArray()).toString(),
                savedAt = o.optLong("savedAt"),
                shared = o.optString("shared"),
                sharedAt = o.optLong("sharedAt"),
            )
        }.getOrNull()
    }
}

object Looks {
    const val FORMAT = "zenith-look"

    /** As many as are kept on the phone. */
    const val MOST = 12

    /** What a Look keeps, by the names the settings are written under. */
    val KEYS = listOf(
        "paletteId", "appearance", "glass", "glassPanels", "glassShine", "font", "numberFont",
        "customAccent", "customAccentAlt", "customBackdrop", "customDark",
        "wallAccent", "wallAccentAlt", "wallBackdrop", "wallDark", "glassTuning", "dayPalettes",
        "wallpaperStyle", "wallpaperPalette", "wallpaperLock", "wallpaperFollow",
        "gridDensity", "gridColumns", "gridRows", "columns", "rows", "freeform", "pages",
        // The layout is kept as it was written: the version it was written in goes with it, so a
        // Look saved by an older Zenith is brought up to date the way an old backup is.
        "layoutVersion",
        "iconShape", "iconPack", "iconStyle", "iconSize", "appEdits", "badges", "labelSize", "labelShadow", "showLabels",
        "dock", "showDock", "searchBar", "header", "dockStyle", "indicator", "folderStyle", "hideStatusBar", "island",
        "openAnimation", "motion", "animation", "pageEffect", "arrival", "drawerArrival", "press", "haptics", "gestures",
        "appsView", "drawerSort", "drawerColumns", "toggles",
    )

    /** The home screen as it is now, as a Look called [name]. */
    fun capture(settings: ZenithSettings, layout: List<HomeItem>, name: String, id: String, now: Long): Look {
        val all = settings.toJson()
        val kept = JSONObject()
        KEYS.forEach { key -> if (all.has(key)) kept.put(key, all.get(key)) }
        return Look(id, name.trim().ifEmpty { "My look" }, kept.toString(), JSONArray().apply { layout.forEach { put(it.toJson()) } }.toString(), now)
    }

    /**
     * [current] wearing [look]: what the Look keeps put in, everything else — your name, your
     * lists, Aura, Plus, the schedule — left as it is. A Look from a newer Zenith, holding settings
     * this one does not know, still puts in all that this one does.
     */
    fun apply(current: ZenithSettings, look: Look): Pair<ZenithSettings, List<HomeItem>> {
        val merged = current.toJson()
        val kept = runCatching { JSONObject(look.settings) }.getOrDefault(JSONObject())
        kept.keys().forEach { key -> if (key in KEYS) merged.put(key, kept.get(key)) }
        val settings = ZenithSettings.fromJson(JSONObject(merged.toString()))
        val array = runCatching { JSONArray(look.layout) }.getOrDefault(JSONArray())
        val items = (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(::homeItemFromJson) }
        return settings to items
    }

    /**
     * [look] as it may go to somebody else. Left out: what only works on this phone (other apps'
     * widgets, shortcuts, photos chosen as icons) and the names you gave apps, which can be personal.
     * Icons chosen from an icon pack stay: they mean the same on any phone with the pack.
     */
    fun forSharing(look: Look): Look {
        val settings = runCatching { JSONObject(look.settings) }.getOrDefault(JSONObject())
        settings.optJSONObject("appEdits")?.let { edits ->
            val portable = AppEdits.portable(AppEdits.fromJson(edits)).mapValues { (_, edit) -> edit.copy(label = "") }
            settings.put("appEdits", AppEdits.toJson(portable.filterValues { !it.isEmpty }))
        }
        val array = runCatching { JSONArray(look.layout) }.getOrDefault(JSONArray())
        val items = (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(::homeItemFromJson) }
            .mapNotNull { item ->
                when (item) {
                    is HostedWidgetCell, is ShortcutCell -> null
                    is StackCell -> {
                        val ours = item.members.filter { !it.hosted }
                        when (ours.size) {
                            0 -> null
                            1 -> StackRules.alone(item, ours.first())
                            else -> item.copy(members = ours)
                        }
                    }
                    else -> item
                }
            }
        return look.copy(
            settings = settings.toString(),
            layout = JSONArray().apply { items.forEach { put(it.toJson()) } }.toString(),
            shared = "",
            sharedAt = 0L,
        )
    }

    /**
     * A Look somebody else made, fitted to this phone: apps that are not installed here are left
     * out of the page, the dock and folders, and a folder left with one app is that app.
     */
    fun keepInstalled(settings: ZenithSettings, items: List<HomeItem>, installed: Set<String>): Pair<ZenithSettings, List<HomeItem>> {
        val kept = items.mapNotNull { item ->
            when (item) {
                is AppCell -> item.takeIf { it.key in installed }
                is FolderCell -> {
                    val keys = item.keys.filter { it in installed }
                    when (keys.size) {
                        0 -> null
                        1 -> keys.first().split('/', limit = 2).let { (pkg, cls) -> AppCell(item.id, item.page, item.col, item.row, pkg, cls) }
                        else -> item.copy(keys = keys)
                    }
                }
                else -> item
            }
        }
        return settings.copy(dock = settings.dock.filter { it in installed }) to kept
    }

    /** The palette a Look is in, for drawing its colours beside its name. */
    fun paletteOf(look: Look): String = runCatching { JSONObject(look.settings).optString("paletteId") }.getOrDefault("")

    fun toJsonList(looks: List<Look>): String = JSONArray().apply { looks.forEach { put(it.toJson()) } }.toString()

    fun fromJsonList(text: String?): List<Look> = runCatching {
        val array = JSONArray(text ?: return emptyList())
        (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(Look::fromJson) }
    }.getOrDefault(emptyList())
}

/**
 * A time for a Look: from [start] to [end], in minutes of the day, on [days] (Monday is 1).
 * A slot that ends before it starts runs past midnight into the next day.
 */
@Immutable
data class LookRule(val lookId: String, val start: Int, val end: Int, val days: Set<Int> = (1..7).toSet()) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("look", lookId)
        put("start", start)
        put("end", end)
        put("days", JSONArray(days.sorted()))
    }

    companion object {
        fun fromJson(o: JSONObject): LookRule? {
            val look = o.optString("look").ifEmpty { return null }
            val days = o.optJSONArray("days")?.let { a -> (0 until a.length()).map { a.optInt(it) }.filter { it in 1..7 }.toSet() } ?: (1..7).toSet()
            return LookRule(look, o.optInt("start").coerceIn(0, 1439), o.optInt("end").coerceIn(0, 1439), days.ifEmpty { (1..7).toSet() })
        }
    }
}

/**
 * When a scheduled Look comes on. Only as a slot begins, or on the way home in a slot that began
 * while away — once for each slot, so a Look chosen by hand in the middle of one is left alone
 * until the next.
 */
object LookSchedule {

    /** The rule whose slot [now] is in, and the day that slot began. */
    fun inForce(rules: List<LookRule>, now: LocalDateTime): Pair<Int, Long>? {
        val minute = now.hour * 60 + now.minute
        val today = now.toLocalDate()
        for ((index, rule) in rules.withIndex()) {
            if (rule.start == rule.end) continue
            if (rule.start < rule.end) {
                if (minute in rule.start until rule.end && today.dayOfWeek.value in rule.days) return index to today.toEpochDay()
            } else {
                // Past midnight: tonight's part, or the morning's tail of yesterday's slot.
                if (minute >= rule.start && today.dayOfWeek.value in rule.days) return index to today.toEpochDay()
                val yesterday = today.minusDays(1)
                if (minute < rule.end && yesterday.dayOfWeek.value in rule.days) return index to yesterday.toEpochDay()
            }
        }
        return null
    }

    /** What to switch to at [now]: the rule, and what to remember the switch by. Null when nothing is due. */
    fun due(rules: List<LookRule>, now: LocalDateTime, last: String): Pair<LookRule, String>? {
        val (index, began) = inForce(rules, now) ?: return null
        val rule = rules[index]
        val occasion = "${rule.lookId}@${rule.start}@$began"
        return if (occasion == last) null else rule to occasion
    }

    /** The days of a rule, as people say them: "Every day", "Weekdays", "Mon, Wed". */
    fun days(days: Set<Int>): String = when (days) {
        (1..7).toSet() -> "Every day"
        (1..5).toSet() -> "Weekdays"
        setOf(6, 7) -> "Weekends"
        else -> days.sorted().joinToString(", ") { DayOfWeek.of(it).name.take(3).lowercase().replaceFirstChar(Char::uppercase) }
    }

    /** A minute of the day as a clock says it: 9:05 is "09:05". */
    fun clock(minute: Int): String = "%02d:%02d".format(minute / 60, minute % 60)
}
