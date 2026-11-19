package com.gabriel.zenithui.data.connect

import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.Look
import com.gabriel.zenithui.data.Looks
import com.gabriel.zenithui.data.homeItemFromJson
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.temporal.IsoFields
import java.util.Locale

// Zenith Connect's rules. Nothing here draws; the pages in ui/connect read these.
//
// Everything Connect shows is real. A setup is a Look, as Settings → Looks keeps them: the settings
// it puts on and, for one somebody saved, their home screen. Zenith's own picks (ConnectPicks) are
// styles, worn over your own home screen. The rest come from Zenith's server (the Looks its keeper
// featured, and any Look by its code) or are your own saved Looks. Applying one puts it on Zenith.

/** Where a setup in Connect came from. */
enum class SetupSource {
    /** One of Zenith's own, in ConnectPicks: a style worn over your own home screen. */
    PICK,

    /** Featured on Zenith's server by the person who runs it. */
    FEATURED,

    /** Somebody's shared Look, opened by its code. */
    CODE,

    /** A Look saved on this phone, as Settings → Looks has them. */
    MINE,
}

/**
 * What a setup can put on, each a part of Zenith's settings (see [ConnectLayers.keys]). One that is
 * left off stays as you have it. [LAYOUT] is also the pages themselves: where the apps and widgets
 * are, and the dock.
 */
enum class Layer(val label: String) {
    WALLPAPER("Wallpaper"),
    THEME("Theme & glass"),
    ICONS("Icons"),
    WIDGETS("Widgets"),
    LAYOUT("Home screen"),
    MOTION("Motion & feel"),
}

/** A setup, as Discover, the Share tab and the Vault show it. */
data class ConnectSetup(
    val id: String,
    val name: String,
    /** Who made it, as the card says it: "Zenith", "Featured", "You", "Shared". */
    val by: String,
    /** One line about it. */
    val about: String,
    val source: SetupSource,
    /** The settings it puts on, as a Look keeps them ([Looks.KEYS]), as JSON text. */
    val settings: String,
    /** Its home screen, as a Look keeps it (a JSON array), or empty for a style worn over yours. */
    val layout: String = "",
    /** Words it is found by. */
    val tags: List<String> = emptyList(),
    /** The code it is shared under on Zenith's server, without LOOK-; empty when it is not. */
    val code: String = "",
    /** The saved Look it is, for one of yours. */
    val lookId: String = "",
) {
    /** The code as people write it, LOOK- and the six; empty when there is none. */
    val shownCode: String get() = if (code.isEmpty()) "" else "LOOK-$code"

    companion object {
        /** A Look as a setup: one of yours ([SetupSource.MINE]), or one from the server. */
        fun of(look: Look, source: SetupSource, code: String = look.shared): ConnectSetup = ConnectSetup(
            id = when (source) {
                SetupSource.MINE -> "mine-${look.id}"
                else -> "code-${ConnectCodes.clean(code) ?: look.id}"
            },
            name = look.name,
            by = when (source) {
                SetupSource.MINE -> "You"
                SetupSource.FEATURED -> "Featured"
                SetupSource.PICK -> "Zenith"
                SetupSource.CODE -> "Shared"
            },
            about = ConnectApply.describe(look.settings, look.layout),
            source = source,
            settings = look.settings,
            layout = look.layout,
            tags = ConnectApply.tagsOf(look.settings),
            code = ConnectCodes.clean(code).orEmpty(),
            lookId = if (source == SetupSource.MINE) look.id else "",
        )
    }
}

object ConnectLayers {

    /**
     * Which part of a setup each setting a Look keeps belongs to. Every one of [Looks.KEYS] is in
     * exactly one (ConnectRulesTest says so), so a setup put on with every layer on is the Look.
     */
    val keys: Map<Layer, Set<String>> = mapOf(
        Layer.WALLPAPER to setOf(
            "wallpaperStyle", "wallpaperPalette", "wallpaperLock", "wallpaperFollow", "paletteId", "dayPalettes",
            "customAccent", "customAccentAlt", "customBackdrop", "customDark",
            "wallAccent", "wallAccentAlt", "wallBackdrop", "wallDark",
        ),
        Layer.THEME to setOf("appearance", "theme", "glass", "glassPanels", "glassShine", "glassTuning", "font", "numberFont"),
        Layer.ICONS to setOf("iconShape", "iconPack", "iconStyle", "iconSize", "appEdits", "badges", "labelSize", "labelShadow", "showLabels"),
        Layer.WIDGETS to setOf("widgetLook"),
        Layer.LAYOUT to setOf(
            "gridDensity", "gridColumns", "gridRows", "columns", "rows", "freeform", "pages", "layoutVersion",
            "dock", "showDock", "searchBar", "header", "dockStyle", "indicator", "folderStyle", "hideStatusBar", "island", "toggles",
        ),
        Layer.MOTION to setOf(
            "openAnimation", "closeAnimation", "motion", "animation", "pageEffect", "arrival", "drawerArrival", "press",
            "haptics", "gestures", "appsView", "drawerSort", "drawerColumns",
        ),
    )

    /** The layer a setting is part of, or null for one a Look does not keep. */
    fun of(key: String): Layer? = keys.entries.firstOrNull { key in it.value }?.key

    /**
     * What [setup] has to put on: each layer it holds any setting of, and the home screen if it has
     * pages of its own. Its wallpaper only when that is one of Zenith's designs: a photo is the
     * phone's own and does not travel, so a Look saved over one has no wallpaper to give.
     */
    fun available(setup: ConnectSetup): Set<Layer> {
        val settings = runCatching { JSONObject(setup.settings) }.getOrNull() ?: JSONObject()
        val held = buildSet {
            settings.keys().forEach { key -> of(key)?.let(::add) }
            if (hasLayout(setup)) add(Layer.LAYOUT)
        }
        // A style worn over your home screen has no pages to give, so its grid and dock are not its to give either.
        val paged = if (hasLayout(setup)) held else held - Layer.LAYOUT
        return if (settings.optString("wallpaperStyle").isEmpty()) paged - Layer.WALLPAPER else paged
    }

    /** Whether [setup] carries a home screen of its own, rather than going over yours. */
    fun hasLayout(setup: ConnectSetup): Boolean =
        setup.layout.isNotBlank() && runCatching { JSONArray(setup.layout).length() > 0 }.getOrDefault(false)

    /**
     * What is on to begin with: everything it has, except its pages and its feel when it is somebody
     * else's (a Look from another phone has their apps in their places, and their gestures; you
     * choose to take those), and except its wallpaper while yours is a photo of your own
     * ([ownPhoto]): one of Zenith's designs goes in place of the photo, and Undo cannot bring a
     * photo back, so that is for you to turn on.
     */
    fun startingOn(setup: ConnectSetup, ownPhoto: Boolean = false): Set<Layer> {
        val all = available(setup)
        val theirs = when (setup.source) {
            SetupSource.FEATURED, SetupSource.CODE -> all - Layer.LAYOUT - Layer.MOTION
            else -> all
        }
        return if (ownPhoto) theirs - Layer.WALLPAPER else theirs
    }
}

/**
 * A setup as it will be put on: the settings and the home screen, as a Look holds them. [keptWallpaper]
 * says its wallpaper was one of the designs that need Plus, which this phone does not have, so the
 * wallpaper there is stays (as Zenith keeps it when a Look like that is put on).
 */
data class Wearing(val settings: String, val layout: String, val fromElsewhere: Boolean, val keptWallpaper: Boolean = false)

object ConnectApply {

    /**
     * [setup] over what you have now ([current] settings and [currentLayout], as a Look keeps
     * them): every setting of a layer in [on] is the setup's, every other is yours, and the home
     * screen is the setup's only with [Layer.LAYOUT] on and pages of its own to give. A Look from
     * somebody else's phone, with its pages taken, is fitted to this phone as it goes on. A
     * wallpaper design this phone does not [own] stays as yours, wallpaper and colours both.
     */
    fun wear(
        current: String,
        currentLayout: String,
        setup: ConnectSetup,
        on: Set<Layer>,
        owns: (String) -> Boolean = { true },
    ): Wearing {
        val mine = runCatching { JSONObject(current) }.getOrNull() ?: JSONObject()
        val theirs = runCatching { JSONObject(setup.settings) }.getOrNull() ?: JSONObject()
        val out = JSONObject()
        mine.keys().forEach { key -> if (key in Looks.KEYS) out.put(key, mine.get(key)) }
        val taking = on intersect ConnectLayers.available(setup)
        theirs.keys().forEach { key ->
            val layer = ConnectLayers.of(key) ?: return@forEach
            if (layer in taking && key in Looks.KEYS) out.put(key, theirs.get(key))
        }
        val design = theirs.optString("wallpaperStyle")
        val kept = Layer.WALLPAPER in taking && design.isNotEmpty() && !owns(design)
        if (kept) {
            for (key in listOf("wallpaperStyle", "wallpaperPalette")) {
                if (mine.has(key)) out.put(key, mine.get(key)) else out.remove(key)
            }
        }
        val pages = Layer.LAYOUT in taking && ConnectLayers.hasLayout(setup)
        return Wearing(
            settings = out.toString(),
            layout = if (pages) setup.layout else currentLayout.ifBlank { "[]" },
            fromElsewhere = pages && (setup.source == SetupSource.FEATURED || setup.source == SetupSource.CODE),
            keptWallpaper = kept,
        )
    }

    /**
     * Whether putting [setup] on with [on] swaps a photo of your own ([ownPhoto]) for one of
     * Zenith's designs, which Undo cannot take back: the photo is not kept anywhere.
     */
    fun replacesPhoto(setup: ConnectSetup, on: Set<Layer>, ownPhoto: Boolean): Boolean =
        ownPhoto && Layer.WALLPAPER in on && Layer.WALLPAPER in ConnectLayers.available(setup)

    /** Whether there is anything to put on: some layer that is on and the setup's to give. */
    fun canApply(setup: ConnectSetup, on: Set<Layer>): Boolean = ConnectLayers.available(setup).any { it in on }

    /** What applying will put on, in words, for the apply screen: "Wallpaper · Theme & glass · Icons". */
    fun parts(setup: ConnectSetup, on: Set<Layer>): List<String> =
        Layer.entries.filter { it in on && it in ConnectLayers.available(setup) }.map { it.label }

    /** A line about a Look, from what it holds: "Aurora · Dunes wallpaper · glass icons". */
    fun describe(settings: String, layout: String): String {
        val o = runCatching { JSONObject(settings) }.getOrNull() ?: JSONObject()
        val parts = buildList {
            o.optString("theme").takeIf { it.isNotEmpty() }?.let { add(themeName(it)) }
            o.optString("wallpaperStyle").takeIf { it.isNotEmpty() }?.let { add("$it wallpaper") }
            when (o.optString("iconStyle")) {
                "glass" -> add("glass icons")
                "tinted" -> add("tinted icons")
            }
            if (o.optString("widgetLook") == "colour") add("colour widgets")
            val count = pageCount(layout)
            if (count > 0) add(if (count == 1) "1 page" else "$count pages")
        }
        return parts.joinToString(" · ").ifEmpty { "A Zenith Look" }
    }

    /** The home screen a Look keeps, read the way Zenith reads it back. */
    fun items(layout: String): List<HomeItem> {
        val array = runCatching { JSONArray(layout) }.getOrNull() ?: return emptyList()
        return (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(::homeItemFromJson) }
    }

    /** How many pages a Look's home screen has: none for a style, which has no home screen. */
    fun pageCount(layout: String): Int = items(layout).maxOfOrNull { it.page }?.plus(1) ?: 0

    /** The words a Look is found by: its theme, light or dark, its wallpaper, and its icons and widgets. */
    fun tagsOf(settings: String): List<String> {
        val o = runCatching { JSONObject(settings) }.getOrNull() ?: JSONObject()
        return buildList {
            o.optString("theme").takeIf { it.isNotEmpty() }?.let { add(themeName(it)) }
            when (o.optString("appearance")) {
                "light" -> add("Light")
                "dark" -> add("Dark")
            }
            if (o.optString("theme") in setOf("glass", "aurora", "frost")) add("Glass")
            o.optString("wallpaperStyle").takeIf { it.isNotEmpty() }?.let(::add)
            if (o.optString("iconStyle") == "glass") add("Glass icons")
            if (o.optString("widgetLook") == "colour") add("Colour")
        }.distinct()
    }

    /** A theme's name as Zenith says it. */
    fun themeName(id: String): String = when (id) {
        "glass" -> "Liquid Glass"
        "aurora" -> "Aurora"
        "frost" -> "Frost"
        "clean" -> "Clean"
        // Neon and Retro became Aurora and Liquid Glass (Themes.successor).
        "neon" -> "Aurora"
        "retro" -> "Liquid Glass"
        else -> id.replaceFirstChar { it.titlecase(Locale.US) }
    }

    /** The three lines the apply screen ticks off, in order, for what is being put on. */
    fun steps(setup: ConnectSetup, on: Set<Layer>): List<String> {
        val taking = on intersect ConnectLayers.available(setup)
        return buildList {
            if (Layer.THEME in taking || Layer.WIDGETS in taking || Layer.ICONS in taking) add("Theme, icons and widgets")
            if (Layer.LAYOUT in taking) add("Home screen")
            if (Layer.MOTION in taking) add("Motion & feel")
            if (Layer.WALLPAPER in taking) add("Wallpaper")
        }.ifEmpty { listOf("Your setup") }
    }

    /** How long each line of the apply screen takes to tick, and the pause before Done. */
    const val STEP_MS = 340L
    const val DONE_PAUSE_MS = 260L
}

/** Codes for shared Looks, as Zenith's server makes them: six of these, after LOOK-. */
object ConnectCodes {
    /** The server's alphabet (`CODE_ALPHABET` in server/zenith_server.py): no vowels, no 0, 1. */
    const val ALPHABET = "BCDFGHJKLMNPQRSTVWXZ23456789"

    /**
     * The six characters of a code however it was typed or pasted: "LOOK-ABC234", "abc 234",
     * "zenith://look/ABC234", a whole message with the code in it. Null when there is no code there.
     */
    fun clean(text: String?): String? {
        if (text.isNullOrBlank()) return null
        val upper = text.uppercase(Locale.US)
        // A message with LOOK- in it: what follows.
        val marked = Regex("LOOK[\\s/:#-]*([A-Z0-9]{6})(?![A-Z0-9])").find(upper)?.groupValues?.get(1)
        val bare = upper.filter { it.isLetterOrDigit() }.removePrefix("LOOK")
        val candidate = marked ?: bare.takeIf { it.length == 6 }
        return candidate?.takeIf { code -> code.all { it in ALPHABET } }
    }

    /** What sharing a code says, for a message app. */
    fun message(name: String, code: String): String =
        "Try my Zenith setup \"${name.trim().ifEmpty { "My look" }}\": open Zenith Connect, tap Have a code, and type LOOK-$code"
}

/** Discover's filters. */
enum class Filter(val label: String) {
    ALL("All"),
    GLASS("Glass"),
    DARK("Dark"),
    LIGHT("Light"),
    COLOUR("Colour"),
}

object ConnectFeed {

    /** [all] found by [query] and kept to [filter], in the order they came. */
    fun list(all: List<ConnectSetup>, query: String, filter: Filter): List<ConnectSetup> {
        val q = query.trim().lowercase(Locale.US)
        return all.filter { setup ->
            val words = "${setup.name} ${setup.by} ${setup.about} ${setup.tags.joinToString(" ")} ${setup.shownCode}".lowercase(Locale.US)
            (q.isEmpty() || q.split(Regex("\\s+")).all { words.contains(it) }) && filter.keeps(setup)
        }
    }

    private fun Filter.keeps(setup: ConnectSetup): Boolean {
        if (this == Filter.ALL) return true
        val o = runCatching { JSONObject(setup.settings) }.getOrNull() ?: JSONObject()
        return when (this) {
            Filter.GLASS -> o.optString("theme", "glass") != "clean"
            Filter.DARK -> o.optString("appearance") == "dark" || o.optString("theme") == "aurora"
            Filter.LIGHT -> o.optString("appearance") == "light" && o.optString("theme") != "aurora"
            Filter.COLOUR -> o.optString("widgetLook") == "colour"
            Filter.ALL -> true
        }
    }

    /** The heading over what was found, and the small note beside it. */
    fun heading(query: String, filter: Filter, found: Int): Pair<String, String> = when {
        query.isNotBlank() -> "Results" to (if (found == 1) "1 found" else "$found found")
        filter != Filter.ALL -> filter.label to (if (found == 1) "1 setup" else "$found setups")
        else -> "Zenith Picks" to "made for this Zenith"
    }

    /** The pick of the week for [today]: one after another, a week each, round the picks. */
    fun ofTheWeek(picks: List<ConnectSetup>, today: LocalDate): ConnectSetup? {
        if (picks.isEmpty()) return null
        val week = today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR) + today.get(IsoFields.WEEK_BASED_YEAR) * 53
        return picks[Math.floorMod(week, picks.size)]
    }
}

/** What the server is to Connect at the moment. */
enum class ServerState {
    /** This build has no server (server.properties is empty): nothing is ever sent. */
    NONE,
    ASKING,
    UP,
    DOWN,
}

object ConnectServer {
    /** The pill at the head of Discover. */
    fun pill(state: ServerState): String = when (state) {
        ServerState.NONE -> "On this phone only"
        ServerState.ASKING -> "Connecting…"
        ServerState.UP -> "Connected to Zenith"
        ServerState.DOWN -> "Can't reach Zenith's server"
    }
}

/** What a problem report carries besides your words, so it can be acted on. */
object ConnectReport {
    fun lines(version: String, code: Int, model: String, maker: String, release: String, lite: Boolean, theme: String): List<String> = listOf(
        "Zenith $version ($code)",
        "${maker.trim().replaceFirstChar { it.titlecase(Locale.US) }} ${model.trim()}".trim().ifEmpty { "Phone" } + " · Android $release",
        "Drawing: " + (if (lite) "Lite" else "Full") + " · Theme: " + ConnectApply.themeName(theme),
    )

    /** The report as it goes to a mail or chat app: what happened, then the phone. */
    fun text(words: String, lines: List<String>): String =
        words.trim() + "\n\n—\n" + lines.joinToString("\n")

    fun problem(words: String): String? = if (words.isBlank()) "Say what happened first." else null
}

object ConnectStudio {
    const val MAX_NAME = 30

    fun nameProblem(name: String): String? = if (name.isBlank()) "Give your setup a name first." else null

    /** Your handle: the name you gave Zenith, made into one word, or "you" with none. */
    fun handle(userName: String): String =
        userName.trim().lowercase(Locale.US).split(Regex("\\s+")).filter { it.isNotEmpty() }.joinToString(".").ifEmpty { "you" }

    /** The letter on your avatar. */
    fun initial(handle: String): String = handle.firstOrNull { it.isLetterOrDigit() }?.uppercaseChar()?.toString() ?: "Y"
}
