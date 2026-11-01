package com.gabriel.zenithui.data

import android.content.Context
import android.net.Uri
import org.json.JSONArray
import org.json.JSONObject

/**
 * The whole home screen in one file: settings, layout, dock, the lot.
 *
 * It is plain JSON on purpose. A backup you cannot read, or cannot restore on a phone that no
 * longer has this exact version of Zenith, is not a backup — so the format is obvious, versioned,
 * and every field is optional on the way back in.
 */
object Backup {

    const val VERSION = 1
    const val MIME = "application/json"

    fun fileName(): String {
        val stamp = android.text.format.DateFormat.format("yyyy-MM-dd", System.currentTimeMillis())
        return "zenith-backup-$stamp.json"
    }

    fun encode(settings: ZenithSettings, items: List<HomeItem>, looks: List<Look> = emptyList()): String {
        val layout = JSONArray()
        items.forEach { layout.put(it.toJson()) }
        return JSONObject().apply {
            put("format", "zenith-ui-backup")
            put("version", VERSION)
            put("savedAt", System.currentTimeMillis())
            put("settings", settings.toJson())
            put("layout", layout)
            // The Looks saved on the phone go too: a Zenith from before there were Looks reads past them.
            if (looks.isNotEmpty()) put("looks", JSONArray().apply { looks.forEach { put(it.toJson()) } })
        }.toString(2)
    }

    /** The Looks a backup holds. One made before there were Looks holds none. */
    fun looks(text: String): List<Look> = runCatching {
        val root = JSONObject(text)
        if (root.optString("format") != "zenith-ui-backup") return emptyList()
        val array = root.optJSONArray("looks") ?: return emptyList()
        (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(Look::fromJson) }
    }.getOrDefault(emptyList())

    /** Reads a backup, or returns null if the file is not one. */
    fun decode(text: String): Pair<ZenithSettings, List<HomeItem>>? = runCatching {
        val root = JSONObject(text)
        if (root.optString("format") != "zenith-ui-backup") return null
        val settings = ZenithSettings.fromJson(root.getJSONObject("settings"))
        val array = root.optJSONArray("layout") ?: JSONArray()
        val items = (0 until array.length()).mapNotNull { homeItemFromJson(array.getJSONObject(it)) }
        settings to items
    }.getOrNull()

    fun write(context: Context, uri: Uri, text: String): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
            ?: return false
        true
    }.getOrDefault(false)

    fun read(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
    }.getOrNull()
}
