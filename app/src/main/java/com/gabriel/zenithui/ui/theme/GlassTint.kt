package com.gabriel.zenithui.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntSize
import com.gabriel.zenithui.data.GlassLevels
import com.gabriel.zenithui.data.WallpaperPalette
import com.gabriel.zenithui.data.ZenithSettings
import kotlin.math.max

// Glass made from your wallpaper, the home screen's second twist: the dock, Search and the glass
// widgets take a little of the wallpaper's own colour where each one sits, and glow with it
// underneath. The lens reads that colour under every point of a pane for itself; these are the
// rules for everything else, kept apart from drawing so they can be tested without a phone.

/**
 * How far the veil of glass made from the wallpaper is taken towards the wallpaper's colour: a tint,
 * never a coloured card, since the veil is also what keeps the words on the glass readable.
 */
const val WALL_TINT = 0.18f

/**
 * [veil] taken [amount] of the way, 0 to 1, towards [wall]'s colour. It keeps its own alpha: how
 * thick the veil is still says how readable the words on it are, whatever colour it takes. With no
 * [wall], or none of it, the veil is as it was.
 */
internal fun tintMix(veil: Color, wall: Color, amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    if (t == 0f || !wall.isSpecified) return veil
    val space = veil.colorSpace
    val towards = wall.convert(space)
    // Each end exactly: nothing of the wall at 0, nothing of the veil's own colour at 1. Kept in
    // range, since a colour a rounding step past white throws, and this is worked out while drawing.
    fun mixed(channel: Int, from: Float, to: Float) =
        (from * (1f - t) + to * t).coerceIn(space.getMinValue(channel), space.getMaxValue(channel))
    return Color(
        red = mixed(0, veil.red, towards.red),
        green = mixed(1, veil.green, towards.green),
        blue = mixed(2, veil.blue, towards.blue),
        alpha = veil.alpha,
        colorSpace = space,
    )
}

/**
 * The glow under glass made from the wallpaper: the wallpaper's own hue there, a little more vivid
 * than the wallpaper is (a grey stays grey rather than taking a hue it does not have), and light
 * enough to read as light rather than as more shadow. Stronger on glass made in the dark, [dark],
 * where a faint glow would be lost.
 */
internal fun glowColour(wall: Color, dark: Boolean): Color {
    if (!wall.isSpecified) return Color.Unspecified
    val hsl = WallpaperPalette.Hsl.of(wall.toArgb())
    val saturation = (hsl.s + (1f - hsl.s) * hsl.s).coerceIn(0f, 1f)
    val lightness = hsl.l.coerceIn(GLOW_DARKEST, GLOW_PALEST)
    return Color(WallpaperPalette.Hsl(hsl.h, saturation, lightness).argb()).copy(alpha = if (dark) 0.36f else 0.28f)
}

/** The glow is never darker or paler than this, in lightness: a dark wallpaper still glows, a pale one still shows it. */
private const val GLOW_DARKEST = 0.5f
private const val GLOW_PALEST = 0.78f

/**
 * The wallpaper's colour under the middle of a pane, [centreInScreen] on a screen [screen] pixels in
 * size, read from [haze], Zenith's softest copy of the wallpaper, which is little but its colours.
 * It is lined up as the panes line the wallpaper up: cropped to fill the screen, zoomed as Android
 * zooms it, and slid as far across as [shown], Android's scroll, says. Unspecified where there is
 * nothing to read, or nothing there but transparency.
 */
internal fun paneColour(haze: ImageBitmap, centreInScreen: Offset, screen: IntSize, shown: Offset = Offset(0.5f, 0.5f)): Color {
    if (haze.width <= 0 || haze.height <= 0 || screen.width <= 0 || screen.height <= 0) return Color.Unspecified
    val scale = max(screen.width / haze.width.toFloat(), screen.height / haze.height.toFloat()) * SYSTEM_WALLPAPER_ZOOM
    val cropX = (haze.width * scale - screen.width) * shown.x.coerceIn(0f, 1f)
    val cropY = (haze.height * scale - screen.height) * shown.y.coerceIn(0f, 1f)
    val x = ((centreInScreen.x + cropX) / scale).toInt().coerceIn(0, haze.width - 1)
    val y = ((centreInScreen.y + cropY) / scale).toInt().coerceIn(0, haze.height - 1)
    val pixel = IntArray(1)
    // Read while a pane draws, where anything thrown would take the launcher down with it.
    val read = runCatching { haze.readPixels(pixel, startX = x, startY = y, width = 1, height = 1) }.isSuccess
    if (!read || (pixel[0] ushr 24) < 128) return Color.Unspecified
    return Color(pixel[0]).copy(alpha = 1f)
}

/**
 * The wallpaper's colour under a pane laid out at [origin], its top-left on screen, [size] big: read
 * from Zenith's copy of the wallpaper when there is one ([paneColour]), with Android's scroll
 * [wallpaperOffset] as the launcher hears it, and otherwise [saved], the colour last worked out from
 * the wallpaper ([LocalWallColour]), which is unspecified when there is none.
 */
internal fun wallUnder(levels: GlassLevels?, origin: Offset, size: Size, screen: IntSize, wallpaperOffset: Offset, saved: Color): Color {
    val haze = levels?.haze ?: return saved
    val centre = Offset(origin.x + size.width / 2f, origin.y + size.height / 2f)
    val read = paneColour(haze, centre, screen, systemWallpaperOffset(wallpaperOffset))
    return if (read.isSpecified) read else saved
}

/**
 * The wallpaper's colour as Zenith last worked it out, for glass made from the wallpaper that has no
 * copy of it to read the colour from: the colours one of Zenith's own designs is painted in, or the
 * liveliest colour read from any other wallpaper ([ZenithSettings.wallAccent] and the rest). While
 * those have never been read they are still the defaults, which are nobody's wallpaper, and there is
 * none: the glass is not tinted at all.
 */
internal fun savedWallColour(settings: ZenithSettings): Color = when {
    // A design's accent is the same whichever tone it is painted in.
    settings.wallpaperStyle.isNotEmpty() -> Palettes.forWallpaper(settings, dark = true).accent
    settings.wallAccent == UNREAD.wallAccent && settings.wallAccentAlt == UNREAD.wallAccentAlt &&
        settings.wallBackdrop == UNREAD.wallBackdrop -> Color.Unspecified
    else -> Color(settings.wallAccent.toInt())
}

/** The wallpaper colours a phone starts with, before any wallpaper has been read. */
private val UNREAD by lazy { ZenithSettings() }

/**
 * The wallpaper's colour as Zenith last worked it out ([savedWallColour]), for glass made from the
 * wallpaper with no copy of the wallpaper to read its colour from. Unspecified when there is none.
 * Given by ZenithTheme; it changes only when the wallpaper does.
 */
internal val LocalWallColour = staticCompositionLocalOf { Color.Unspecified }
