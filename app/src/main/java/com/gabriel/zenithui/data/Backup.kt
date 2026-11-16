package com.gabriel.zenithui.data

import android.content.Context
import android.net.Uri

/**
 * The whole home screen in one file: settings, layout, dock, Looks, the lot. It is the config
 * ([ZenithConfig]) written down, so there is one format for both.
 *
 * It is plain JSON on purpose. A backup you cannot read, or cannot restore on a phone that no
 * longer has this exact version of Zenith, is not a backup — so the format is obvious, versioned,
 * and every field is optional on the way back in.
 */
object Backup {

    const val MIME = "application/json"

    fun fileName(): String {
        val stamp = android.text.format.DateFormat.format("yyyy-MM-dd", System.currentTimeMillis())
        return "zenith-backup-$stamp.json"
    }

    /** The whole config as a backup's text: see [ZenithConfig], whose JSON a backup is. */
    fun encode(config: ZenithConfig): String = config.toJson(savedAt = System.currentTimeMillis()).toString(2)

    /** Reads a backup, or returns null if the file is not one. */
    fun decode(text: String): ZenithConfig? = ZenithConfig.parse(text)

    fun write(context: Context, uri: Uri, text: String): Boolean = runCatching {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray()) }
            ?: return false
        true
    }.getOrDefault(false)

    fun read(context: Context, uri: Uri): String? = runCatching {
        context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
    }.getOrNull()
}
