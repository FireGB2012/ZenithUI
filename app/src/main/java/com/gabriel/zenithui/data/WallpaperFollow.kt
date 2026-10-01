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
}
