package com.gabriel.zenithui.data

import androidx.compose.runtime.Immutable
import org.json.JSONArray
import org.json.JSONObject

/**
 * Everything that makes Zenith yours, as one thing: your settings (the theme you chose, light or
 * dark and the font among them), the home screen's layout, the Looks you saved and themes of your
 * own. Plain data, written down as JSON and read back exactly ([toJson], [parse]), so that wherever
 * it is kept it is kept the same way: on this phone today ([LocalConfigRepository]), and one day on
 * your own server too, behind the same [ConfigRepository].
 *
 * Its JSON is a backup's ([Backup]): one format, not two. A backup made before there was a config
 * reads as one, and the server, which keeps a backup as it is sent, already keeps a config.
 */
@Immutable
data class ZenithConfig(
    val settings: ZenithSettings = ZenithSettings(),
    val layout: List<HomeItem> = emptyList(),
    /** The Looks saved on the phone, newest first. Kept apart from the settings, so a Look never holds Looks. */
    val looks: List<Look> = emptyList(),
    /**
     * Themes of your own, beside the ones Zenith comes with ([Themes.all]), each by an id of its
     * own. None can be made yet: this is where one made, or shared with you, will be kept, so the
     * format need not change when one can.
     */
    val themes: List<Theme> = emptyList(),
) {
    fun toJson(savedAt: Long): JSONObject = JSONObject().apply {
        put("format", FORMAT)
        put("version", VERSION)
        put("savedAt", savedAt)
        put("settings", settings.toJson())
        put("layout", layoutJson(layout))
        // Left out when empty: a Zenith from before there were any reads past them anyway.
        if (looks.isNotEmpty()) put("looks", looksJson(looks))
        if (themes.isNotEmpty()) put("themes", themesJson(themes))
    }

    /**
     * This config put back over [here], as a backup is: its settings and layout in place of those,
     * and its Looks and themes joining the ones [here] has, the one in both being its own. No more
     * Looks are kept than a phone keeps ([Looks.MOST]), its own first.
     */
    fun over(here: ZenithConfig): ZenithConfig {
        val lookIds = looks.mapTo(HashSet()) { it.id }
        val themeIds = themes.mapTo(HashSet()) { it.id }
        return copy(
            looks = (looks + here.looks.filterNot { it.id in lookIds }).take(Looks.MOST),
            themes = themes + here.themes.filterNot { it.id in themeIds },
        )
    }

    companion object {
        /** What a config's JSON says it is: a backup's name, since the two are one. */
        const val FORMAT = "zenith-ui-backup"

        /** What [toJson] writes. A part added later is optional, so this stays 1 until one cannot be. */
        const val VERSION = 1

        /** A config from its JSON text, or null when the text is not one. Never throws. */
        fun parse(text: String?): ZenithConfig? =
            runCatching { fromJson(JSONObject(text ?: return null)) }.getOrNull()

        /**
         * A config from JSON, or null when it is not one: it says it is, and it has settings. Every
         * other part is optional, and an entry in one that cannot be read is passed over.
         */
        fun fromJson(o: JSONObject): ZenithConfig? {
            if (o.optString("format") != FORMAT) return null
            val settings = o.optJSONObject("settings") ?: return null
            return ZenithConfig(
                settings = ZenithSettings.fromJson(settings),
                layout = layoutFrom(o.optJSONArray("layout")),
                looks = looksFrom(o.optJSONArray("looks")),
                themes = themesFrom(o.optJSONArray("themes")),
            )
        }

        fun layoutJson(items: List<HomeItem>): JSONArray = JSONArray().apply { items.forEach { put(it.toJson()) } }

        fun looksJson(looks: List<Look>): JSONArray = JSONArray().apply { looks.forEach { put(it.toJson()) } }

        fun themesJson(themes: List<Theme>): JSONArray = JSONArray().apply { themes.forEach { put(it.toJson()) } }

        /**
         * The layout in [array], read back exactly as saved. Sizes and positions are put right by the
         * view model, which can see the grid and everything else on the page; enlarging one widget
         * here, blind to its neighbours, is what once let widgets grow over the icons around them.
         */
        fun layoutFrom(array: JSONArray?): List<HomeItem> = entries(array) { homeItemFromJson(it) }

        fun looksFrom(array: JSONArray?): List<Look> = entries(array) { Look.fromJson(it) }

        /**
         * Themes of your own in [array]. One with no id cannot be chosen, and one with the id of a
         * theme Zenith comes with, or of one before it, would never be drawn: those are passed over.
         */
        fun themesFrom(array: JSONArray?): List<Theme> {
            val taken = Themes.all.mapTo(HashSet()) { it.id }
            return entries(array) { if (it.optString("id").isBlank()) null else Theme.fromJson(it) }
                .filter { taken.add(it.id) }
        }

        private inline fun <T> entries(array: JSONArray?, read: (JSONObject) -> T?): List<T> {
            if (array == null) return emptyList()
            return (0 until array.length()).mapNotNull { index ->
                array.optJSONObject(index)?.let { entry -> runCatching { read(entry) }.getOrNull() }
            }
        }
    }
}
