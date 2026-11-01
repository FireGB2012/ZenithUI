package com.gabriel.zenithui.data

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

@Immutable
data class ZenithSettings(
    val onboarded: Boolean = false,
    val userName: String = "",
    val paletteId: String = "midnight",
    /** auto | light | dark */
    val appearance: String = "dark",
    val glass: Float = 0.8f,
    /** How much body the glass panels have: clear | frosted | solid. See [GlassBody]. */
    val glassBody: String = "clear",
    /** compact | balanced | roomy — the cell size you asked for — or custom, for a grid of your own. */
    val gridDensity: String = "balanced",
    /** The grid picked by hand, used when [gridDensity] is custom. 0 means none has been picked. */
    val gridColumns: Int = 0,
    val gridRows: Int = 0,
    /** Derived from [gridDensity] and the screen; never edited directly. */
    val columns: Int = 4,
    val rows: Int = 5,
    val iconSize: Int = 56,
    val showLabels: Boolean = true,
    /** squircle | circle | rounded | square */
    val iconShape: String = "squircle",
    val iconPack: String = "",
    /** Names, icons and hidden names you have set for single apps, by app key. See [AppEdit]. */
    val appEdits: Map<String, AppEdit> = emptyMap(),
    /** How every app icon is drawn: original | tinted | glass. See [IconStyles]. */
    val iconStyle: String = IconStyles.ORIGINAL,
    /** Notification dots: off | dot | count. Seen only once Android lets Zenith see notifications. */
    val badges: String = Badges.DOT,
    /** small | medium | large: the names under the icons. See [LabelSizes]. */
    val labelSize: String = LabelSizes.MEDIUM,
    /** A soft shadow under those names, for a wallpaper they get lost on. */
    val labelShadow: Boolean = false,
    /** The specular highlight that follows how you are holding the phone. */
    val glassShine: Boolean = true,
    /** The typeface everything is set in: see `ZenithFonts`. */
    val font: String = "system",
    /** The typeface big numbers are set in: "same" as the rest, "thin", or one of the fonts. */
    val numberFont: String = "same",
    /** The Glass studio's changes to the glass (Plus): see [GlassTuning]. */
    val glassTuning: GlassTuning = GlassTuning(),
    /** A palette for each part of the day, by part (Plus); empty when the palette stays put. */
    val dayPalettes: Map<String, String> = emptyMap(),
    /** Which part of the day last changed the palette, so each changes it once. */
    val paletteOccasion: String = "",
    /** How an app grows out of its icon: colour (a card in the icon's colour) | glass (grey liquid glass). */
    val openAnimation: String = "colour",
    /** What swiping up shows: drawer (every app, A to Z) | library (apps grouped by kind). */
    val appsView: String = "drawer",
    /** Apps you opened from Zenith, most recent first. Feeds Suggestions; never leaves the phone. */
    val recentLaunches: List<String> = emptyList(),
    /** How many times each app has been opened from Zenith, for the drawer's Most used. Never leaves the phone. */
    val launchCounts: Map<String, Int> = emptyMap(),
    /** The order the drawer lists apps in: see [DrawerSort]. */
    val drawerSort: String = DrawerSort.AZ,
    /** How many apps across the drawer has; 0 is as many as the home screen. */
    val drawerColumns: Int = 0,
    /** Tabs of the drawer you made yourself. */
    val drawerTabs: List<DrawerTab> = emptyList(),
    /**
     * Plus: put things anywhere. Icons on any quarter row rather than every half, and widgets at
     * any size the grid can hold instead of their designed range. Nothing may overlap either way.
     */
    val freeform: Boolean = false,
    /** No longer used: nothing tucks under a widget. Kept so that older backups still read. */
    val widgetTuck: Int = 0,
    val dock: List<String> = emptyList(),
    /** Whether the dock is shown. Its apps are kept either way, so turning it back on restores them. */
    val showDock: Boolean = true,
    /** A search bar above the dock, between the page dots and the dock. */
    val searchBar: Boolean = false,
    /** What is written at the top of the home screen: name | greeting | off. */
    val header: String = "name",
    /** The dock: "glass" (a bar), "floating" (a pill round its icons) or "none" (icons only). */
    val dockStyle: String = "glass",
    /** Which page is in view: "dots", "line", "numbers" or "none". */
    val indicator: String = "dots",
    /** How a folder shows what is in it: "grid", "stack" or "cover". */
    val folderStyle: String = "grid",
    /** Nothing on the home screen can be moved, removed or resized while this is on. */
    val lockLayout: Boolean = false,
    /** The status bar stays away on the home screen until swiped in from the top. */
    val hideStatusBar: Boolean = false,
    /** The island round the camera, over every app (Plus): see [IslandSettings]. */
    val island: IslandSettings = IslandSettings(),
    /** The Zenith design in use as the wallpaper, by its WallStyle name; empty for a photo or one set elsewhere. */
    val wallpaperStyle: String = "",
    /** The palette that wallpaper is pinned to, or empty to paint it in the theme's own. */
    val wallpaperPalette: String = "",
    /** Whether it is on the lock screen too. */
    val wallpaperLock: Boolean = false,
    /** Paint it again when the palette, or light and dark, changes. */
    val wallpaperFollow: Boolean = true,
    /** How much moves: full | reduced. The phone's own "Remove animations" reduces it too. */
    val motion: String = "full",
    /** The numbers apps open and close on: a preset, and whatever has been tuned by hand. */
    val animation: AnimationTuning = AnimationTuning(),
    /** How one page gives way to the next: depth | slide | cube | stack. */
    val pageEffect: String = "depth",
    /** How the home screen arrives: "middle", "below" or "cascade" (see `FlyIn`). */
    val arrival: String = "middle",
    /** How the drawer comes up: "rise", "grow" or "glass". */
    val drawerArrival: String = "rise",
    /** What a press does: "bounce", "squish" or "ripple". */
    val press: String = "bounce",
    /** The small ticks under your finger when you pick something up, switch something or drop it. */
    val haptics: Boolean = true,
    /** What you chose for each gesture on the home screen; one not here does its default. See [Gestures]. */
    val gestures: Map<String, String> = emptyMap(),
    /** The controls the Quick Toggles widget shows, in the order they appear. */
    val toggles: List<String> = listOf("torch", "wifi", "bluetooth", "focus"),
    /** How many home pages you have asked for. Empty trailing ones are forgotten again. */
    val pages: Int = 1,
    val note: String = "",
    /** The cities on the World clock, as time zones, in the order shown. */
    val worldClocks: List<String> = WorldClocks.DEFAULT,
    /** What the Countdown widget counts down to, and the day it is; empty and 0 until it is set. */
    val countdownTitle: String = "",
    val countdownDay: Long = 0L,
    /** The day the countdown was set, so its ring can show how much of the wait is behind you. */
    val countdownFrom: Long = 0L,
    /** The pictures in the Photo frame, kept in Zenith's files, by name. */
    val framePhotos: List<String> = emptyList(),
    /** The To-do widget's list. */
    val todos: List<TodoItem> = emptyList(),
    /** Where the Weather widget's forecast is for; empty until a place is chosen, and nothing is sent until then. */
    val weatherPlace: String = "",
    val weatherLat: Double = 0.0,
    val weatherLon: Double = 0.0,
    /** c | f */
    val weatherUnits: String = "c",
    /** When each saved Look comes on by itself. See [LookSchedule]. */
    val lookRules: List<LookRule> = emptyList(),
    /** Which slot of the schedule last switched the Look, so each switches once. */
    val lookOccasion: String = "",
    /** The Look last put on, by id; empty when none, or when the home screen has been changed since. */
    val activeLook: String = "",
    /** How many times we have shown the system "default home app" prompt. */
    val homePrompts: Int = 0,
    /** "Not now" to today's events on the bedside clock: it stops offering them until asked. */
    val calendarDeclined: Boolean = false,
    /** 0 = rows only, 1 = rows divided into units, 2 = everything on whole rows. */
    val layoutVersion: Int = 0,

    // ------------------------------------------------------------------ Zenith Plus
    /** When the free trial began, in epoch millis. 0 means it has not started yet. */
    val plusTrialStart: Long = 0L,
    /** Play is the authority; this is the copy that keeps Plus working offline. */
    val plusPurchased: Boolean = false,
    /** Apps kept out of the drawer, as package/class keys. */
    val hiddenApps: List<String> = emptyList(),
    /** Digits asked for before the hidden list can be seen. Empty means no PIN. */
    val hiddenPin: String = "",
    // ------------------------------------------------------------------ Aura
    /** Points in hand. */
    val aura: Int = 0,
    /** Days in a row, counting today once it has been checked in. */
    val streak: Int = 0,
    /** The day the streak was last counted, as an epoch day. 0 means never. */
    val lastCheckIn: Long = 0L,
    /** Themes bought with aura. */
    val unlockedThemes: List<String> = emptyList(),
    /** This install's own code, made once and kept. */
    val referralCode: String = "",
    /** Somebody else's code, once. */
    val referredBy: String = "",
    /**
     * For each Zenith account signed in to on this phone, by its code: how many of the friends
     * who used that code have been paid for here. It travels with [aura] in a backup, so that
     * restoring one neither pays a friend twice nor forgets one; and it is kept code by code, so
     * that signing in to one account and then another never pays either of them again.
     */
    val referralsPaid: Map<String, Int> = emptyMap(),

    /** The palette you built yourself, used when [paletteId] is "custom". ARGB. */
    val customAccent: Long = 0xFF7C6CFF,
    val customAccentAlt: Long = 0xFF3ED2FF,
    val customBackdrop: Long = 0xFF0A0B18,
    val customDark: Boolean = true,
    /** The palette taken from the wallpaper, as it was last worked out: see [WallpaperPalette]. */
    val wallAccent: Long = 0xFF7C6CFF,
    val wallAccentAlt: Long = 0xFF3ED2FF,
    val wallBackdrop: Long = 0xFF0A0B18,
    val wallDark: Boolean = true,
) {
    /** Whether the launcher is dark right now: what was asked for, or with "auto" what the phone is. */
    fun isDark(systemDark: Boolean): Boolean = when (appearance) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("onboarded", onboarded)
        put("userName", userName)
        put("paletteId", paletteId)
        put("appearance", appearance)
        put("glass", glass.toDouble())
        // Saved as "glassPanels". For a day it was saved as "glassBody" with Frosted as what
        // everybody got; nobody chose that, so those copies are not read and the glass goes back
        // to Clear.
        put("glassPanels", glassBody)
        put("gridDensity", gridDensity)
        put("gridColumns", gridColumns)
        put("gridRows", gridRows)
        put("columns", columns)
        put("rows", rows)
        put("iconSize", iconSize)
        put("showLabels", showLabels)
        put("iconShape", iconShape)
        put("iconPack", iconPack)
        put("appEdits", AppEdits.toJson(appEdits))
        put("iconStyle", iconStyle)
        put("badges", badges)
        put("labelSize", labelSize)
        put("labelShadow", labelShadow)
        put("glassShine", glassShine)
        put("font", font)
        put("numberFont", numberFont)
        put("glassTuning", glassTuning.toJson())
        put("dayPalettes", DayParts.toJson(dayPalettes))
        put("paletteOccasion", paletteOccasion)
        put("openAnimation", openAnimation)
        put("appsView", appsView)
        put("recentLaunches", JSONArray(recentLaunches))
        put("launchCounts", JSONObject().apply { launchCounts.forEach { (key, count) -> put(key, count) } })
        put("drawerSort", drawerSort)
        put("drawerColumns", drawerColumns)
        put("drawerTabs", JSONArray().apply { drawerTabs.forEach { put(it.toJson()) } })
        put("freeform", freeform)
        put("widgetTuck", widgetTuck)
        put("dock", JSONArray(dock))
        put("showDock", showDock)
        put("searchBar", searchBar)
        put("header", header)
        put("dockStyle", dockStyle)
        put("indicator", indicator)
        put("folderStyle", folderStyle)
        put("lockLayout", lockLayout)
        put("hideStatusBar", hideStatusBar)
        put("island", island.toJson())
        put("wallpaperStyle", wallpaperStyle)
        put("wallpaperPalette", wallpaperPalette)
        put("wallpaperLock", wallpaperLock)
        put("wallpaperFollow", wallpaperFollow)
        put("motion", motion)
        put("animation", animation.toJson())
        put("pageEffect", pageEffect)
        put("arrival", arrival)
        put("drawerArrival", drawerArrival)
        put("press", press)
        put("haptics", haptics)
        put("gestures", JSONObject().apply { gestures.forEach { (gesture, action) -> put(gesture, action) } })
        put("toggles", JSONArray(toggles))
        put("plusTrialStart", plusTrialStart)
        put("plusPurchased", plusPurchased)
        put("hiddenApps", JSONArray(hiddenApps))
        put("hiddenPin", hiddenPin)
        put("customAccent", customAccent)
        put("customAccentAlt", customAccentAlt)
        put("customBackdrop", customBackdrop)
        put("customDark", customDark)
        put("wallAccent", wallAccent)
        put("wallAccentAlt", wallAccentAlt)
        put("wallBackdrop", wallBackdrop)
        put("wallDark", wallDark)
        put("aura", aura)
        put("streak", streak)
        put("lastCheckIn", lastCheckIn)
        put("unlockedThemes", JSONArray(unlockedThemes))
        put("referralCode", referralCode)
        put("referredBy", referredBy)
        put("referralsPaid", JSONObject().apply { referralsPaid.forEach { (code, paid) -> put(code, paid) } })
        put("pages", pages)
        put("note", note)
        put("worldClocks", JSONArray(worldClocks))
        put("countdownTitle", countdownTitle)
        put("countdownDay", countdownDay)
        put("countdownFrom", countdownFrom)
        put("framePhotos", JSONArray(framePhotos))
        put("todos", JSONArray().apply { todos.forEach { put(it.toJson()) } })
        put("weatherPlace", weatherPlace)
        put("weatherLat", weatherLat)
        put("weatherLon", weatherLon)
        put("weatherUnits", weatherUnits)
        put("lookRules", JSONArray().apply { lookRules.forEach { put(it.toJson()) } })
        put("lookOccasion", lookOccasion)
        put("activeLook", activeLook)
        put("homePrompts", homePrompts)
        put("calendarDeclined", calendarDeclined)
        put("layoutVersion", layoutVersion)
    }

    companion object {
        fun fromJson(o: JSONObject): ZenithSettings {
            val d = ZenithSettings()
            val columns = o.optInt("columns", d.columns)
            val rows = o.optInt("rows", d.rows)
            return ZenithSettings(
                onboarded = o.optBoolean("onboarded", d.onboarded),
                userName = o.optString("userName", d.userName),
                paletteId = o.optString("paletteId", d.paletteId),
                appearance = o.optString("appearance", d.appearance),
                glass = o.optDouble("glass", d.glass.toDouble()).toFloat(),
                glassBody = o.optString("glassPanels", d.glassBody),
                // Layouts saved before densities existed are matched to the nearest preset.
                gridDensity = o.optString("gridDensity", GridDensity.fromGrid(columns, rows).id),
                gridColumns = o.optInt("gridColumns", d.gridColumns),
                gridRows = o.optInt("gridRows", d.gridRows),
                columns = columns,
                rows = rows,
                iconSize = o.optInt("iconSize", d.iconSize),
                showLabels = o.optBoolean("showLabels", d.showLabels),
                iconShape = o.optString("iconShape", d.iconShape),
                iconPack = o.optString("iconPack", d.iconPack),
                appEdits = AppEdits.fromJson(o.optJSONObject("appEdits")),
                iconStyle = o.optString("iconStyle", d.iconStyle),
                badges = o.optString("badges", d.badges),
                labelSize = o.optString("labelSize", d.labelSize),
                labelShadow = o.optBoolean("labelShadow", d.labelShadow),
                glassShine = o.optBoolean("glassShine", d.glassShine),
                font = o.optString("font", d.font),
                numberFont = o.optString("numberFont", d.numberFont),
                glassTuning = GlassTuning.fromJson(o.optJSONObject("glassTuning")),
                dayPalettes = DayParts.fromJson(o.optJSONObject("dayPalettes")),
                paletteOccasion = o.optString("paletteOccasion", d.paletteOccasion),
                openAnimation = o.optString("openAnimation", d.openAnimation),
                appsView = o.optString("appsView", d.appsView),
                recentLaunches = o.optJSONArray("recentLaunches").toStringList(),
                launchCounts = o.optJSONObject("launchCounts")?.let { counts ->
                    counts.keys().asSequence().mapNotNull { key -> (counts.opt(key) as? Int)?.takeIf { it > 0 }?.let { key to it } }.toMap()
                } ?: d.launchCounts,
                drawerSort = o.optString("drawerSort", d.drawerSort),
                drawerColumns = o.optInt("drawerColumns", d.drawerColumns),
                drawerTabs = o.optJSONArray("drawerTabs")?.let { tabs ->
                    (0 until tabs.length()).mapNotNull { tabs.optJSONObject(it)?.let(DrawerTab::fromJson) }
                } ?: d.drawerTabs,
                freeform = o.optBoolean("freeform", d.freeform),
                widgetTuck = o.optInt("widgetTuck", d.widgetTuck),
                dock = o.optJSONArray("dock").toStringList(),
                showDock = o.optBoolean("showDock", d.showDock),
                searchBar = o.optBoolean("searchBar", d.searchBar),
                header = o.optString("header", d.header),
                dockStyle = o.optString("dockStyle", d.dockStyle),
                indicator = o.optString("indicator", d.indicator),
                folderStyle = o.optString("folderStyle", d.folderStyle),
                lockLayout = o.optBoolean("lockLayout", d.lockLayout),
                hideStatusBar = o.optBoolean("hideStatusBar", d.hideStatusBar),
                island = IslandSettings.fromJson(o.optJSONObject("island")),
                wallpaperStyle = o.optString("wallpaperStyle", d.wallpaperStyle),
                wallpaperPalette = o.optString("wallpaperPalette", d.wallpaperPalette),
                wallpaperLock = o.optBoolean("wallpaperLock", d.wallpaperLock),
                wallpaperFollow = o.optBoolean("wallpaperFollow", d.wallpaperFollow),
                motion = o.optString("motion", d.motion),
                animation = AnimationTuning.fromJson(o.optJSONObject("animation")),
                pageEffect = o.optString("pageEffect", d.pageEffect),
                arrival = o.optString("arrival", d.arrival),
                drawerArrival = o.optString("drawerArrival", d.drawerArrival),
                press = o.optString("press", d.press),
                haptics = o.optBoolean("haptics", d.haptics),
                gestures = o.optJSONObject("gestures")?.let { chosen ->
                    chosen.keys().asSequence().mapNotNull { gesture ->
                        chosen.optString(gesture).takeIf { it.isNotEmpty() }?.let { gesture to it }
                    }.toMap()
                } ?: d.gestures,
                toggles = o.optJSONArray("toggles").toStringList().ifEmpty { d.toggles },
                plusTrialStart = o.optLong("plusTrialStart", d.plusTrialStart),
                plusPurchased = o.optBoolean("plusPurchased", d.plusPurchased),
                hiddenApps = o.optJSONArray("hiddenApps").toStringList(),
                hiddenPin = o.optString("hiddenPin", d.hiddenPin),
                customAccent = o.optLong("customAccent", d.customAccent),
                customAccentAlt = o.optLong("customAccentAlt", d.customAccentAlt),
                customBackdrop = o.optLong("customBackdrop", d.customBackdrop),
                customDark = o.optBoolean("customDark", d.customDark),
                wallAccent = o.optLong("wallAccent", d.wallAccent),
                wallAccentAlt = o.optLong("wallAccentAlt", d.wallAccentAlt),
                wallBackdrop = o.optLong("wallBackdrop", d.wallBackdrop),
                wallDark = o.optBoolean("wallDark", d.wallDark),
                aura = o.optInt("aura", d.aura),
                streak = o.optInt("streak", d.streak),
                lastCheckIn = o.optLong("lastCheckIn", d.lastCheckIn),
                unlockedThemes = o.optJSONArray("unlockedThemes").toStringList(),
                referralCode = o.optString("referralCode", d.referralCode),
                referredBy = o.optString("referredBy", d.referredBy),
                referralsPaid = o.optJSONObject("referralsPaid")?.let { paid ->
                    paid.keys().asSequence()
                        .mapNotNull { code -> (paid.opt(code) as? Int)?.takeIf { it >= 0 }?.let { code to it } }
                        .toMap()
                } ?: d.referralsPaid,
                pages = o.optInt("pages", d.pages),
                note = o.optString("note", d.note),
                // A list that was there but emptied stays empty; one never written gets the three cities.
                worldClocks = if (o.has("worldClocks")) o.optJSONArray("worldClocks").toStringList() else d.worldClocks,
                countdownTitle = o.optString("countdownTitle", d.countdownTitle),
                countdownDay = o.optLong("countdownDay", d.countdownDay),
                countdownFrom = o.optLong("countdownFrom", d.countdownFrom),
                framePhotos = o.optJSONArray("framePhotos").toStringList(),
                todos = o.optJSONArray("todos")?.let { list ->
                    (0 until list.length()).mapNotNull { list.optJSONObject(it)?.let(TodoItem::fromJson) }
                } ?: d.todos,
                weatherPlace = o.optString("weatherPlace", d.weatherPlace),
                weatherLat = o.optDouble("weatherLat", d.weatherLat),
                weatherLon = o.optDouble("weatherLon", d.weatherLon),
                weatherUnits = o.optString("weatherUnits", d.weatherUnits),
                lookRules = o.optJSONArray("lookRules")?.let { rules ->
                    (0 until rules.length()).mapNotNull { rules.optJSONObject(it)?.let(LookRule::fromJson) }
                } ?: d.lookRules,
                lookOccasion = o.optString("lookOccasion", d.lookOccasion),
                activeLook = o.optString("activeLook", d.activeLook),
                homePrompts = o.optInt("homePrompts", d.homePrompts),
                calendarDeclined = o.optBoolean("calendarDeclined", d.calendarDeclined),
                layoutVersion = o.optInt("layoutVersion", d.layoutVersion),
            )
        }
    }
}

/**
 * Tiny SharedPreferences-backed store. Everything the launcher remembers lives here. There is one
 * for the whole app ([get]), so a screen of its own — the one that takes a shortcut another app
 * asks to put on the home screen — changes the same layout the home screen is showing.
 */
class SettingsStore private constructor(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("zenith", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<ZenithSettings> = _settings.asStateFlow()

    private val _layout = MutableStateFlow(readLayout())
    val layout: StateFlow<List<HomeItem>> = _layout.asStateFlow()

    private val _looks = MutableStateFlow(Looks.fromJsonList(prefs.getString(KEY_LOOKS, null)))

    /** The Looks saved on this phone, newest first. Kept apart from the settings, so a Look never holds Looks. */
    val looks: StateFlow<List<Look>> = _looks.asStateFlow()

    fun setLooks(looks: List<Look>) {
        _looks.value = looks
        prefs.edit().putString(KEY_LOOKS, Looks.toJsonList(looks)).apply()
    }

    fun update(transform: (ZenithSettings) -> ZenithSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        prefs.edit().putString(KEY_SETTINGS, next.toJson().toString()).apply()
    }

    fun setLayout(items: List<HomeItem>) {
        _layout.value = items
        val arr = JSONArray()
        items.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(KEY_LAYOUT, arr.toString()).apply()
    }

    fun mutateLayout(transform: (List<HomeItem>) -> List<HomeItem>) = setLayout(transform(_layout.value))

    /** Puts a whole backup back in one step, so nothing is half restored. */
    fun restore(settings: ZenithSettings, items: List<HomeItem>) {
        _settings.value = settings
        _layout.value = items
        val arr = JSONArray()
        items.forEach { arr.put(it.toJson()) }
        prefs.edit()
            .putString(KEY_SETTINGS, settings.toJson().toString())
            .putString(KEY_LAYOUT, arr.toString())
            .apply()
    }

    fun reset() {
        prefs.edit().clear().apply()
        _settings.value = ZenithSettings()
        _layout.value = emptyList()
        _looks.value = emptyList()
    }

    private fun readSettings(): ZenithSettings = runCatching {
        prefs.getString(KEY_SETTINGS, null)?.let { ZenithSettings.fromJson(JSONObject(it)) }
    }.getOrNull() ?: ZenithSettings()

    private fun readLayout(): List<HomeItem> = runCatching {
        val raw = prefs.getString(KEY_LAYOUT, null) ?: return@runCatching emptyList()
        val arr = JSONArray(raw)
        // Read back exactly as saved. Sizes and positions are put right by the view model, which
        // can see the grid and everything else on the page; enlarging one widget here, blind to its
        // neighbours, is what let widgets grow over the icons around them.
        (0 until arr.length()).mapNotNull { homeItemFromJson(arr.getJSONObject(it)) }
    }.getOrNull() ?: emptyList()

    companion object {
        private const val KEY_SETTINGS = "settings"
        private const val KEY_LAYOUT = "layout"
        private const val KEY_LOOKS = "looks"

        @Volatile private var instance: SettingsStore? = null

        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) { instance ?: SettingsStore(context).also { instance = it } }
    }
}
