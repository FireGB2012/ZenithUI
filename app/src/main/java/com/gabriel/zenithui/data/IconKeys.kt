package com.gabriel.zenithui.data

/**
 * What an icon is saved under, taken apart again. An icon is saved under the key of what it is the
 * icon of (an app's `package/Activity`, a work app's `work:` key, a shortcut's `shortcut:` key, a
 * symbol's `glyph|` in front) and then `|` and how it was drawn. A plain function, so which icons
 * an app's update makes old can be tested without a phone.
 */
object IconKeys {

    /** The package whose icon the saved [cacheKey] is, or "" when it is not an app's: a pack's picture asked for by itself. */
    fun packageOf(cacheKey: String): String {
        val key = cacheKey.removePrefix(GLYPH).substringBefore('|')
        if (IconSource.isSource(key)) return ""
        val bare = key.removePrefix(WorkKeys.PREFIX).removePrefix(ShortcutKeys.PREFIX)
        return bare.substringBefore('/')
    }

    private const val GLYPH = "glyph|"
}
