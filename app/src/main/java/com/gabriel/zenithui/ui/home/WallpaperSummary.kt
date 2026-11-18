package com.gabriel.zenithui.ui.home

import com.gabriel.zenithui.data.LiveMotion
import com.gabriel.zenithui.data.WallStyle
import com.gabriel.zenithui.ui.theme.Palettes
import com.gabriel.zenithui.ui.theme.ZenithPalette

/**
 * What the Customise card says of the wallpaper in use, beside a picture of it. Testers wanted the
 * way to the wallpapers to show more than the word: so it shows the wallpaper, what it is called
 * and in which colours, and what it does.
 */
internal object WallpaperSummary {

    /**
     * "Aurora · Midnight colours" for one of Zenith's designs, painted in [colours]; otherwise
     * whether it is a picture of your own ([ownPicture]: Zenith keeps a copy of it for the glass)
     * or one set in another app, which Android keeps from Zenith.
     */
    fun name(style: WallStyle?, colours: ZenithPalette?, ownPicture: Boolean): String = when {
        style != null -> style.label + (colours?.let { " · " + coloursOf(it) } ?: "")
        ownPicture -> "Your own picture"
        else -> "From another app"
    }

    /** What it does: moves, changes with light and dark ([follows]), or stays as it is. */
    fun how(style: WallStyle?, follows: Boolean, ownPicture: Boolean): String = when {
        style?.live == LiveMotion.Idle -> "Live: moves gently on its own"
        style?.live == LiveMotion.Unlock -> "Live: opens as you unlock"
        style != null && follows -> "Changes with light and dark"
        style != null -> "Stays as it is in light and dark"
        ownPicture -> "The glass bends it too"
        else -> "The glass stays frosted"
    }

    private fun coloursOf(set: ZenithPalette): String = when (set.id) {
        Palettes.CUSTOM -> "Your colours"
        Palettes.WALLPAPER -> "Colours from a picture"
        else -> "${set.name} colours"
    }
}
