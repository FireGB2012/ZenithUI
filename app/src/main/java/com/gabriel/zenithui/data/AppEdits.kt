package com.gabriel.zenithui.data

import androidx.compose.runtime.Immutable
import org.json.JSONObject

/**
 * What you have changed about one app: the name under its icon, where its icon comes from, and
 * whether this one shows a name at all. Kept per app rather than per icon, so the drawer, the
 * dock, folders and search all show the same thing.
 */
@Immutable
data class AppEdit(
    /** Empty: the app's own name. */
    val label: String = "",
    /** Where the icon comes from, written as [IconSource] writes it; empty for the app's own. */
    val icon: String = "",
    /** No name under this one icon, whatever Labels is set to. */
    val hideLabel: Boolean = false,
) {
    /** Changes nothing at all, and so is not worth keeping. */
    val isEmpty: Boolean get() = label.isBlank() && icon.isEmpty() && !hideLabel

    fun toJson(): JSONObject = JSONObject().apply {
        if (label.isNotBlank()) put("label", label)
        if (icon.isNotEmpty()) put("icon", icon)
        if (hideLabel) put("hideLabel", true)
    }

    companion object {
        fun fromJson(o: JSONObject?): AppEdit = if (o == null) AppEdit() else AppEdit(
            label = o.optString("label", "").trim(),
            icon = o.optString("icon", ""),
            hideLabel = o.optBoolean("hideLabel", false),
        )
    }
}

/**
 * Where an icon you chose comes from, as the short line kept in [AppEdit.icon] — so a backup stays
 * plain JSON. `pack:<icon pack>:<drawable>` is one picture out of an icon pack; `photo:<file>` is a
 * picture of your own, kept in Zenith's files. The same line is what the icon loader knows the
 * picture by, so a pack's icons can be shown in a grid before one is chosen.
 */
sealed interface IconSource {
    data class Pack(val packageName: String, val drawable: String) : IconSource
    data class Photo(val file: String) : IconSource

    companion object {
        private const val PACK = "pack:"
        private const val PHOTO = "photo:"

        fun pack(packageName: String, drawable: String): String = "$PACK$packageName:$drawable"
        fun photo(file: String): String = "$PHOTO$file"

        /** Whether [key] names a chosen picture rather than an app. */
        fun isSource(key: String): Boolean = key.startsWith(PACK) || key.startsWith(PHOTO)

        fun parse(raw: String): IconSource? = when {
            raw.startsWith(PACK) -> {
                // Neither a package name nor a drawable's name can hold a colon.
                val parts = raw.removePrefix(PACK).split(':', limit = 2)
                if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) Pack(parts[0], parts[1]) else null
            }
            raw.startsWith(PHOTO) -> raw.removePrefix(PHOTO).takeIf { it.isNotEmpty() && '/' !in it && '\\' !in it }?.let(::Photo)
            else -> null
        }
    }
}

object AppEdits {

    fun toJson(edits: Map<String, AppEdit>): JSONObject = JSONObject().apply {
        edits.forEach { (key, edit) -> if (!edit.isEmpty) put(key, edit.toJson()) }
    }

    fun fromJson(o: JSONObject?): Map<String, AppEdit> {
        if (o == null) return emptyMap()
        return o.keys().asSequence()
            .mapNotNull { key -> AppEdit.fromJson(o.optJSONObject(key)).takeIf { !it.isEmpty }?.let { key to it } }
            .toMap()
    }

    /**
     * [edits] with [edit] for [key]; an edit that changes nothing is taken out instead of kept. A
     * name is kept as it is being typed, spaces and all — it is only trimmed where it is shown.
     */
    fun with(edits: Map<String, AppEdit>, key: String, edit: AppEdit): Map<String, AppEdit> =
        if (edit.isEmpty) edits - key else edits + (key to edit)

    /**
     * The apps with the names you gave them, in order by those names: a renamed app is found
     * where its new name puts it, in the drawer and in search.
     */
    fun relabel(apps: List<AppInfo>, edits: Map<String, AppEdit>): List<AppInfo> {
        if (edits.values.none { it.label.isNotBlank() }) return apps
        return apps
            .map { app -> edits[app.key]?.label?.trim()?.takeIf { it.isNotEmpty() }?.let { app.copy(label = it) } ?: app }
            .sortedBy { it.label.lowercase() }
    }

    /** The photos are only on this phone: [edits] as another phone can use them. */
    fun portable(edits: Map<String, AppEdit>): Map<String, AppEdit> =
        edits.mapValues { (_, edit) -> if (IconSource.parse(edit.icon) is IconSource.Photo) edit.copy(icon = "") else edit }
            .filterValues { !it.isEmpty }

    /** The picture each edited app has instead of its own, by app key. */
    fun icons(edits: Map<String, AppEdit>): Map<String, String> =
        edits.mapNotNull { (key, edit) -> edit.icon.takeIf { it.isNotEmpty() }?.let { key to it } }.toMap()
}
