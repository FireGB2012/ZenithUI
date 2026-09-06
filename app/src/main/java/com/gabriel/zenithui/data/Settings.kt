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
    val columns: Int = 4,
    val rows: Int = 5,
    val iconSize: Int = 56,
    val showLabels: Boolean = true,
    /** squircle | circle | rounded | square */
    val iconShape: String = "squircle",
    val iconPack: String = "",
    val dock: List<String> = emptyList(),
    val note: String = "",
    /** How many times we have shown the system "default home app" prompt. */
    val homePrompts: Int = 0,
    /** 0 = rows only, 1 = rows divided into units. */
    val layoutVersion: Int = 0,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("onboarded", onboarded)
        put("userName", userName)
        put("paletteId", paletteId)
        put("appearance", appearance)
        put("glass", glass.toDouble())
        put("columns", columns)
        put("rows", rows)
        put("iconSize", iconSize)
        put("showLabels", showLabels)
        put("iconShape", iconShape)
        put("iconPack", iconPack)
        put("dock", JSONArray(dock))
        put("note", note)
        put("homePrompts", homePrompts)
        put("layoutVersion", layoutVersion)
    }

    companion object {
        fun fromJson(o: JSONObject): ZenithSettings {
            val d = ZenithSettings()
            return ZenithSettings(
                onboarded = o.optBoolean("onboarded", d.onboarded),
                userName = o.optString("userName", d.userName),
                paletteId = o.optString("paletteId", d.paletteId),
                appearance = o.optString("appearance", d.appearance),
                glass = o.optDouble("glass", d.glass.toDouble()).toFloat(),
                columns = o.optInt("columns", d.columns),
                rows = o.optInt("rows", d.rows),
                iconSize = o.optInt("iconSize", d.iconSize),
                showLabels = o.optBoolean("showLabels", d.showLabels),
                iconShape = o.optString("iconShape", d.iconShape),
                iconPack = o.optString("iconPack", d.iconPack),
                dock = o.optJSONArray("dock").toStringList(),
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

    fun reset() {
        prefs.edit().clear().apply()
        _settings.value = ZenithSettings()
        _layout.value = emptyList()
    }

    private fun readSettings(): ZenithSettings = runCatching {
        prefs.getString(KEY_SETTINGS, null)?.let { ZenithSettings.fromJson(JSONObject(it)) }
    }.getOrNull() ?: ZenithSettings()

    private fun readLayout(): List<HomeItem> = runCatching {
        val upgraded = _settings.value.layoutVersion >= 1
        val raw = prefs.getString(KEY_LAYOUT, null) ?: return@runCatching emptyList()
        val arr = JSONArray(raw)
        (0 until arr.length())
            .mapNotNull { homeItemFromJson(arr.getJSONObject(it)) }
            .map { item ->
                // Sizes are only checked against the spec once they are in row units; a layout
                // still awaiting that upgrade is left exactly as saved.
                if (!upgraded || item !is WidgetCell) return@map item
                val spec = Widgets.byType(item.type) ?: return@map item
                item.copy(
                    spanX = item.spanX.coerceIn(spec.minSpanX, spec.maxSpanX),
                    spanY = item.spanY.coerceIn(spec.minSpanY, spec.maxSpanY),
                )
            }
    }.getOrNull() ?: emptyList()

    private companion object {
        const val KEY_SETTINGS = "settings"
        const val KEY_LAYOUT = "layout"
    }
}
