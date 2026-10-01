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
    /** compact | balanced | roomy — the cell size you asked for. */
    val gridDensity: String = "balanced",
    /** Derived from [gridDensity] and the screen; never edited directly. */
    val columns: Int = 4,
    val rows: Int = 5,
    val iconSize: Int = 56,
    val showLabels: Boolean = true,
    /** squircle | circle | rounded | square */
    val iconShape: String = "squircle",
    val iconPack: String = "",
    /** The specular highlight that follows how you are holding the phone. */
    val glassShine: Boolean = true,
    /** How an app grows out of its icon: colour (a card in the icon's colour) | glass (grey liquid glass). */
    val openAnimation: String = "colour",
    /** What swiping up shows: drawer (every app, A to Z) | library (apps grouped by kind). */
    val appsView: String = "drawer",
    /** Apps you opened from Zenith, most recent first. Feeds Suggestions; never leaves the phone. */
    val recentLaunches: List<String> = emptyList(),
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
    /** The Zenith design in use as the wallpaper, by its WallStyle name; empty for a photo or one set elsewhere. */
    val wallpaperStyle: String = "",
    /** The palette that wallpaper is pinned to, or empty to paint it in the theme's own. */
    val wallpaperPalette: String = "",
    /** Whether it is on the lock screen too. */
    val wallpaperLock: Boolean = false,
    /** Paint it again when the palette, or light and dark, changes. */
    val wallpaperFollow: Boolean = true,
    /** The controls the Quick Toggles widget shows, in the order they appear. */
    val toggles: List<String> = listOf("torch", "wifi", "bluetooth", "focus"),
    /** How many home pages you have asked for. Empty trailing ones are forgotten again. */
    val pages: Int = 1,
    val note: String = "",
    /** How many times we have shown the system "default home app" prompt. */
    val homePrompts: Int = 0,
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

    /** The palette you built yourself, used when [paletteId] is "custom". ARGB. */
    val customAccent: Long = 0xFF7C6CFF,
    val customAccentAlt: Long = 0xFF3ED2FF,
    val customBackdrop: Long = 0xFF0A0B18,
    val customDark: Boolean = true,
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
        put("gridDensity", gridDensity)
        put("columns", columns)
        put("rows", rows)
        put("iconSize", iconSize)
        put("showLabels", showLabels)
        put("iconShape", iconShape)
        put("iconPack", iconPack)
        put("glassShine", glassShine)
        put("openAnimation", openAnimation)
        put("appsView", appsView)
        put("recentLaunches", JSONArray(recentLaunches))
        put("freeform", freeform)
        put("widgetTuck", widgetTuck)
        put("dock", JSONArray(dock))
        put("showDock", showDock)
        put("searchBar", searchBar)
        put("header", header)
        put("wallpaperStyle", wallpaperStyle)
        put("wallpaperPalette", wallpaperPalette)
        put("wallpaperLock", wallpaperLock)
        put("wallpaperFollow", wallpaperFollow)
        put("toggles", JSONArray(toggles))
        put("plusTrialStart", plusTrialStart)
        put("plusPurchased", plusPurchased)
        put("hiddenApps", JSONArray(hiddenApps))
        put("hiddenPin", hiddenPin)
        put("customAccent", customAccent)
        put("customAccentAlt", customAccentAlt)
        put("customBackdrop", customBackdrop)
        put("customDark", customDark)
        put("aura", aura)
        put("streak", streak)
        put("lastCheckIn", lastCheckIn)
        put("unlockedThemes", JSONArray(unlockedThemes))
        put("referralCode", referralCode)
        put("referredBy", referredBy)
        put("pages", pages)
        put("note", note)
        put("homePrompts", homePrompts)
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
                // Layouts saved before densities existed are matched to the nearest preset.
                gridDensity = o.optString("gridDensity", GridDensity.fromGrid(columns, rows).id),
                columns = columns,
                rows = rows,
                iconSize = o.optInt("iconSize", d.iconSize),
                showLabels = o.optBoolean("showLabels", d.showLabels),
                iconShape = o.optString("iconShape", d.iconShape),
                iconPack = o.optString("iconPack", d.iconPack),
                glassShine = o.optBoolean("glassShine", d.glassShine),
                openAnimation = o.optString("openAnimation", d.openAnimation),
                appsView = o.optString("appsView", d.appsView),
                recentLaunches = o.optJSONArray("recentLaunches").toStringList(),
                freeform = o.optBoolean("freeform", d.freeform),
                widgetTuck = o.optInt("widgetTuck", d.widgetTuck),
                dock = o.optJSONArray("dock").toStringList(),
                showDock = o.optBoolean("showDock", d.showDock),
                searchBar = o.optBoolean("searchBar", d.searchBar),
                header = o.optString("header", d.header),
                wallpaperStyle = o.optString("wallpaperStyle", d.wallpaperStyle),
                wallpaperPalette = o.optString("wallpaperPalette", d.wallpaperPalette),
                wallpaperLock = o.optBoolean("wallpaperLock", d.wallpaperLock),
                wallpaperFollow = o.optBoolean("wallpaperFollow", d.wallpaperFollow),
                toggles = o.optJSONArray("toggles").toStringList().ifEmpty { d.toggles },
                plusTrialStart = o.optLong("plusTrialStart", d.plusTrialStart),
                plusPurchased = o.optBoolean("plusPurchased", d.plusPurchased),
                hiddenApps = o.optJSONArray("hiddenApps").toStringList(),
                hiddenPin = o.optString("hiddenPin", d.hiddenPin),
                customAccent = o.optLong("customAccent", d.customAccent),
                customAccentAlt = o.optLong("customAccentAlt", d.customAccentAlt),
                customBackdrop = o.optLong("customBackdrop", d.customBackdrop),
                customDark = o.optBoolean("customDark", d.customDark),
                aura = o.optInt("aura", d.aura),
                streak = o.optInt("streak", d.streak),
                lastCheckIn = o.optLong("lastCheckIn", d.lastCheckIn),
                unlockedThemes = o.optJSONArray("unlockedThemes").toStringList(),
                referralCode = o.optString("referralCode", d.referralCode),
                referredBy = o.optString("referredBy", d.referredBy),
                pages = o.optInt("pages", d.pages),
                note = o.optString("note", d.note),
                homePrompts = o.optInt("homePrompts", d.homePrompts),
                layoutVersion = o.optInt("layoutVersion", d.layoutVersion),
            )
        }
    }
}

/** Tiny SharedPreferences-backed store. Everything the launcher remembers lives here. */
class SettingsStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences("zenith", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(readSettings())
    val settings: StateFlow<ZenithSettings> = _settings.asStateFlow()

    private val _layout = MutableStateFlow(readLayout())
    val layout: StateFlow<List<HomeItem>> = _layout.asStateFlow()

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

    private companion object {
        const val KEY_SETTINGS = "settings"
        const val KEY_LAYOUT = "layout"
    }
}
