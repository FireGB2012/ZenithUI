package com.gabriel.zenithui.data

/** Deciding when one of Zenith's own wallpapers has to be painted again. */
object WallpaperFollow {

    /** Bumped when the designs themselves change, so wallpapers already up are painted again. */
    const val ART_VERSION = 1

    /** The picture that should be on the wall, as a key that changes exactly when the picture would. */
    fun key(style: String, colors: WallColors, width: Int, height: Int): String = listOf(
        style, colors.accent, colors.accentAlt, colors.deep, colors.mid, colors.base,
        if (colors.dark) "dark" else "light", width, height, ART_VERSION,
    ).joinToString("|")

    /**
     * Whether to repaint without being asked: only while following is on, one of Zenith's designs
     * is the wallpaper, Zenith's kept copy is still what is on screen (so a wallpaper chosen
     * anywhere else is never replaced), and the picture wanted is not the one already up.
     */
    fun shouldRepaint(settings: ZenithSettings, desired: String, applied: String?, ownWallpaperUp: Boolean): Boolean =
        settings.wallpaperFollow && settings.wallpaperStyle.isNotEmpty() && ownWallpaperUp && desired != applied

    /**
     * Whether Android's word that the wallpaper changed means [style], the design the settings
     * name, is no longer in use: something else has become the wallpaper. Never while Zenith's
     * live wallpaper is up ([liveUp]): that is Zenith's wallpaper whatever it shows, and it
     * announces new colours of its own as a still design goes up over it. A still design is in use
     * while its picture is up ([copyUp]); one that moves only while the live wallpaper is.
     */
    fun forgets(style: String, liveUp: Boolean, copyUp: Boolean): Boolean {
        val design = WallStyle.byName(style) ?: return false
        if (liveUp) return false
        return design.live != null || !copyUp
    }

    /**
     * The design to name again when the settings name none ([style] empty) while one of Zenith's
     * is the wallpaper all the same: the one its live wallpaper shows ([liveShows]), or the one
     * whose picture is up ([appliedKey], see [key]). A photo has no key, and a wallpaper from
     * anywhere else is neither, so neither names anything.
     */
    fun adopt(style: String, liveUp: Boolean, liveShows: String?, copyUp: Boolean, appliedKey: String?): String? {
        if (style.isNotEmpty()) return null
        val named = when {
            liveUp -> liveShows
            copyUp -> appliedKey?.substringBefore('|')
            else -> null
        }
        return named?.takeIf { WallStyle.byName(it) != null }
    }
}
