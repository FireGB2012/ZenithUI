package com.gabriel.zenithui.data

/**
 * How every app icon is drawn. Original is as the app draws it. Tinted and Glass are made from the
 * app's one-colour symbol — Android 13's themed icon — or, for an app that has none, from what
 * stands out of its icon: Tinted puts it in the accent on a tile of the theme's tint, as Android
 * does; Glass puts it in white on a tile of clear glass that bends the wallpaper, the way iOS draws
 * clear icons. An icon you chose for an app yourself is always shown as you chose it.
 */
object IconStyles {
    const val ORIGINAL = "original"
    const val TINTED = "tinted"
    const val GLASS = "glass"

    val ids = listOf(ORIGINAL, TINTED, GLASS)

    fun of(id: String): String = if (id in ids) id else ORIGINAL

    fun label(id: String): String = when (of(id)) {
        TINTED -> "Tinted"
        GLASS -> "Glass"
        else -> "Original"
    }

    fun detail(id: String): String = when (of(id)) {
        TINTED -> "Every app's symbol in your colour, on a tile of the theme's tint."
        GLASS -> "Every app's symbol in white, on a tile of clear glass over your wallpaper."
        else -> "Icons as their apps draw them."
    }
}

/**
 * How Zenith's own widgets are drawn. Colour is solid, each kind in colours of its own, as iOS
 * draws its widgets: a sky for the weather, a white page for the calendar. Glass is Zenith's
 * Liquid Glass over the wallpaper, with white words. Anything else reads as Glass, which is what
 * the widgets were before there was a choice. Other apps' widgets draw themselves either way.
 */
object WidgetLooks {
    const val COLOUR = "colour"
    const val GLASS = "glass"

    val ids = listOf(COLOUR, GLASS)

    fun of(id: String): String = if (id in ids) id else GLASS

    fun label(id: String): String = when (of(id)) {
        COLOUR -> "Colour"
        else -> "Glass"
    }

    fun detail(id: String): String = when (of(id)) {
        COLOUR -> "Solid widgets, each in colours of its own: a sky for the weather, a white page for the calendar."
        else -> "Widgets of clear glass over your wallpaper, with white words."
    }
}

/**
 * Notification dots: none, a dot, or a dot with the count in it. A dot only ever says what Android
 * would badge itself — nothing ongoing, like music or a download, and nothing an app has asked
 * not to badge.
 */
object Badges {
    const val OFF = "off"
    const val DOT = "dot"
    const val COUNT = "count"

    val ids = listOf(OFF, DOT, COUNT)

    fun of(id: String): String = if (id in ids) id else DOT

    fun label(id: String): String = when (of(id)) {
        OFF -> "Off"
        COUNT -> "Count"
        else -> "Dot"
    }

    /** One notification, as the dots see it. */
    data class Seen(
        val packageName: String,
        /** Music playing, a call, a download: there for as long as it lasts, not something to read. */
        val ongoing: Boolean = false,
        /** The heading over a bundle of an app's notifications; its members are counted instead. */
        val summary: Boolean = false,
        /** Whether the app lets this kind of notification show as a badge. */
        val canBadge: Boolean = true,
        /** How many it stands for, when the app says (three new messages in one). 0 when it does not. */
        val number: Int = 0,
    )

    /** How many notifications each app has that a dot should count, by package. */
    fun tally(seen: List<Seen>): Map<String, Int> {
        val counts = HashMap<String, Int>()
        for (one in seen) {
            if (one.ongoing || one.summary || !one.canBadge) continue
            counts[one.packageName] = (counts[one.packageName] ?: 0) + one.number.coerceAtLeast(1)
        }
        return counts
    }

    /** An app's count from its key, `package/class`. */
    fun forKey(key: String, counts: Map<String, Int>): Int =
        counts[key.substringBefore('/')] ?: 0

    /**
     * A folder's dot is all of its apps' together. An app is one package however many of its
     * screens are in the folder, so it is counted once.
     */
    fun total(keys: List<String>, counts: Map<String, Int>): Int =
        keys.map { it.substringBefore('/') }.distinct().sumOf { counts[it] ?: 0 }

    /** What a counting dot says. */
    fun text(count: Int): String = if (count > 99) "99+" else count.toString()
}

/** The size of the names under icons, inside the same strip, so nothing on the page moves. */
object LabelSizes {
    const val SMALL = "small"
    const val MEDIUM = "medium"
    const val LARGE = "large"

    val ids = listOf(SMALL, MEDIUM, LARGE)

    fun of(id: String): String = if (id in ids) id else MEDIUM

    fun sp(id: String): Float = when (of(id)) {
        SMALL -> 10f
        LARGE -> 12.5f
        else -> 11f
    }

    fun label(id: String): String = when (of(id)) {
        SMALL -> "Small"
        LARGE -> "Large"
        else -> "Medium"
    }
}
