package com.gabriel.zenithui.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min

/**
 * What an iPhone is drawn in, in the light or in the dark: the page behind a screen of its own, the
 * cards on it, the words and the quieter words, the lines between rows, the fill of a second-choice
 * button and the red of a badge. Solid colours, with nothing of the wallpaper or the theme in them:
 * a theme only gives the accent ([AppleColours.accent]).
 */
@Immutable
data class AppleSurfaces(
    val dark: Boolean,
    /** Behind everything on a page of its own: Settings, the gallery, setup. */
    val page: Color,
    /** A card on that page, a widget's tile, a card inside a sheet. */
    val card: Color,
    /** A card on a card, and the track of a control. */
    val raised: Color,
    val words: Color,
    /** Second lines, values, footnotes: the words, quieter. */
    val secondary: Color,
    /** The hairline between the rows of a card. */
    val separator: Color,
    /** The fill of a second-choice button. */
    val grey: Color,
    /** A notification badge. */
    val badge: Color,
)

object AppleColours {

    val Dark = AppleSurfaces(
        dark = true,
        page = Color(0xFF000000),
        card = Color(0xFF1C1C1E),
        raised = Color(0xFF2C2C2E),
        words = Color.White,
        secondary = Color.White.copy(alpha = 0.6f),
        separator = Color(0xFF38383A),
        grey = Color(0xFF787880).copy(alpha = 0.36f),
        badge = Color(0xFFFF453A),
    )

    val Light = AppleSurfaces(
        dark = false,
        page = Color(0xFFF2F2F7),
        card = Color.White,
        raised = Color(0xFFE5E5EA),
        words = Color.Black,
        secondary = Color.Black.copy(alpha = 0.6f),
        separator = Color(0xFFC6C6C8),
        grey = Color(0xFF787880).copy(alpha = 0.20f),
        badge = Color(0xFFFF3B30),
    )

    fun of(dark: Boolean): AppleSurfaces = if (dark) Dark else Light

    /** How far an accent stands from the cards and pages it is drawn on, at the least. */
    const val ACCENT_CONTRAST = 3f

    /**
     * A palette's accent as the interface draws it, in the light or in the dark: its own colour (or
     * the one it gives for the light, as Frost gives Apple's light blue), moved only as far as it
     * must be to stand [ACCENT_CONTRAST] to one on a card and on the page. Apple's own colours come
     * in a light and a dark version for the same reason.
     */
    fun accent(palette: ZenithPalette, dark: Boolean): Color =
        readable(if (dark) palette.accent else palette.accentLight ?: palette.accent, dark)

    /**
     * [colour], taken deeper in the light or lighter in the dark until it stands on Apple's card
     * and page, and no further: a colour that already stands is left as it is.
     */
    fun readable(colour: Color, dark: Boolean): Color {
        val s = of(dark)
        val own = colour.copy(alpha = 1f)
        fun stands(c: Color) = min(contrast(c, s.card), contrast(c, s.page)) >= ACCENT_CONTRAST
        if (stands(own)) return own
        val towards = if (dark) Color.White else Color.Black
        // Darker towards black (or lighter towards white) the whole way, so the least that stands
        // can be found by halving.
        var low = 0f
        var high = 1f
        repeat(24) {
            val mid = (low + high) / 2f
            if (stands(lerp(own, towards, mid))) high = mid else low = mid
        }
        return lerp(own, towards, high)
    }
}

/** The colours of the light or the dark in use, for anything drawn solid. */
val LocalApple = staticCompositionLocalOf { AppleColours.Dark }

/**
 * How far apart two colours stand, as the Web's accessibility guidelines measure it: 1 to 1 for the
 * same colour, 21 to 1 for black on white. A see-through [front] is taken as seen over [back].
 */
fun contrast(front: Color, back: Color): Float {
    val a = front.compositeOver(back).luminance()
    val b = back.luminance()
    return (max(a, b) + 0.05f) / (min(a, b) + 0.05f)
}
