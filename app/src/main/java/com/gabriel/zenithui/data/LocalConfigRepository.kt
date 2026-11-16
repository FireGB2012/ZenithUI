package com.gabriel.zenithui.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * The config kept on this phone, in Zenith's own preferences. Each part is kept under a name of its
 * own, the names Zenith has always used, so no phone starts again for the change; and each is
 * written only when it changes, so moving an icon writes the layout and not the Looks. A part that
 * cannot be read starts again on its own, and the rest is kept.
 */
class LocalConfigRepository internal constructor(private val storage: ConfigStorage) : ConfigRepository {

    private val _settings = MutableStateFlow(readSettings())
    override val settings: StateFlow<ZenithSettings> = _settings.asStateFlow()

    private val _layout = MutableStateFlow(readList(KEY_LAYOUT) { ZenithConfig.layoutFrom(it) })
    override val layout: StateFlow<List<HomeItem>> = _layout.asStateFlow()

    private val _looks = MutableStateFlow(readList(KEY_LOOKS) { ZenithConfig.looksFrom(it) })
    override val looks: StateFlow<List<Look>> = _looks.asStateFlow()

    private val _themes = MutableStateFlow(readList(KEY_THEMES) { ZenithConfig.themesFrom(it) })
    override val themes: StateFlow<List<Theme>> = _themes.asStateFlow()

    override fun update(transform: (ZenithSettings) -> ZenithSettings) {
        val next = transform(_settings.value)
        _settings.value = next
        storage.write(mapOf(KEY_SETTINGS to next.toJson().toString()))
    }

    override fun setLayout(items: List<HomeItem>) {
        _layout.value = items
        storage.write(mapOf(KEY_LAYOUT to ZenithConfig.layoutJson(items).toString()))
    }

    override fun setLooks(looks: List<Look>) {
        _looks.value = looks
        storage.write(mapOf(KEY_LOOKS to ZenithConfig.looksJson(looks).toString()))
    }

    override fun replace(config: ZenithConfig) {
        val changed = buildMap {
            if (config.settings != _settings.value) put(KEY_SETTINGS, config.settings.toJson().toString())
            if (config.layout != _layout.value) put(KEY_LAYOUT, ZenithConfig.layoutJson(config.layout).toString())
            if (config.looks != _looks.value) put(KEY_LOOKS, ZenithConfig.looksJson(config.looks).toString())
            if (config.themes != _themes.value) put(KEY_THEMES, ZenithConfig.themesJson(config.themes).toString())
        }
        _settings.value = config.settings
        _layout.value = config.layout
        _looks.value = config.looks
        _themes.value = config.themes
        if (changed.isNotEmpty()) storage.write(changed)
    }

    override fun reset() {
        storage.clear()
        _settings.value = ZenithSettings()
        _layout.value = emptyList()
        _looks.value = emptyList()
        _themes.value = emptyList()
    }

    override suspend fun save(): Boolean = withContext(Dispatchers.IO) { storage.commit() }

    private fun readSettings(): ZenithSettings =
        storage.read(KEY_SETTINGS)?.let { runCatching { ZenithSettings.fromJson(JSONObject(it)) }.getOrNull() }
            ?: ZenithSettings()

    private fun <T> readList(key: String, read: (JSONArray) -> List<T>): List<T> =
        storage.read(key)?.let { runCatching { read(JSONArray(it)) }.getOrNull() } ?: emptyList()

    companion object {
        private const val KEY_SETTINGS = "settings"
        private const val KEY_LAYOUT = "layout"
        private const val KEY_LOOKS = "looks"
        private const val KEY_THEMES = "themes"

        @Volatile private var instance: LocalConfigRepository? = null

        fun get(context: Context): LocalConfigRepository = instance ?: synchronized(this) {
            instance ?: LocalConfigRepository(PrefsConfigStorage(context)).also { instance = it }
        }
    }
}

/** Where a [LocalConfigRepository] keeps the text of each part: Zenith's preferences, or memory in a test. */
interface ConfigStorage {
    fun read(key: String): String?

    /** Writes [values] in one step, on their way to disk: this returns before they are there. */
    fun write(values: Map<String, String>)

    fun clear()

    /** Waits until everything written so far is on disk. Whether it is. */
    fun commit(): Boolean
}

/** Zenith's preferences, the file everything it remembers has always been kept in. */
private class PrefsConfigStorage(context: Context) : ConfigStorage {

    private val prefs = context.applicationContext.getSharedPreferences("zenith", Context.MODE_PRIVATE)

    override fun read(key: String): String? = prefs.getString(key, null)

    override fun write(values: Map<String, String>) {
        val edit = prefs.edit()
        values.forEach { (key, text) -> edit.putString(key, text) }
        edit.apply()
    }

    override fun clear() = prefs.edit().clear().apply()

    // A commit with nothing in it returns once every write before it is on disk.
    override fun commit(): Boolean = prefs.edit().commit()
}
