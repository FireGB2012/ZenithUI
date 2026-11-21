package com.gabriel.zenithui.data

import java.io.File

/**
 * The Photo frame's pictures are kept by name in a folder of Zenith's own (ZenithViewModel's
 * frameDir), and the settings list those names. A name is only ever a plain file name: one read
 * back from a backup that holds a path ("../shared_prefs/zenith.xml") would otherwise have Zenith
 * open, or delete, a file that is not a photo at all.
 */
object FramePhotos {
    private const val NAME_MOST = 120

    fun isPlainName(name: String): Boolean =
        name.isNotEmpty() && name.length <= NAME_MOST && name != "." && name != ".." &&
            name.none { it == '/' || it == '\\' || it == '\u0000' || it.isISOControl() }

    /** The file [name] is in [folder], or null when the name is not a plain one in it. */
    fun fileIn(folder: File, name: String): File? {
        if (!isPlainName(name)) return null
        val file = File(folder, name)
        return file.takeIf { runCatching { it.canonicalFile.parentFile == folder.canonicalFile }.getOrDefault(false) }
    }
}
