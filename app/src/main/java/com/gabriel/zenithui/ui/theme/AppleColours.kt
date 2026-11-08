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
    /** A row under a finger, for as long as it is held: Apple's shade, in place of a bounce. */
    val pressed: Color,
    /** The raised pill of a segmented control, standing on its grey track. */
    val pill: Color,
    /**
     * The fill of the search field and the track of a segmented control. It is see-through, so it
     * is a shade deeper than whatever it lies on, a card or the page.
     */
    val field: Color,
    /** Words for what cannot be undone: start again, take it away. */
    val danger: Color,
    /** A row's chevron, and marks quieter still than a second line. */
    val faint: Color,
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
        pressed = Color(0xFF3A3A3C),
        pill = Color(0xFF636366),
        field = Color(0xFF767680).copy(alpha = 0.24f),
        danger = Color(0xFFFF453A),
        faint = Color(0xFFEBEBF5).copy(alpha = 0.3f),
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
        pressed = Color(0xFFD1D1D6),
        pill = Color.White,
        field = Color(0xFF767680).copy(alpha = 0.12f),
        danger = Color(0xFFFF3B30),
        faint = Color(0xFF3C3C43).copy(alpha = 0.3f),
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

/**
 * [colour] as a tile for white words: left as it is when it is deep enough already, and otherwise
 * taken towards black only as far as white words need to stand four and a half to one on it. For a
 * colour Zenith did not choose, the main colour of an album's picture.
 */
fun AppleColours.underWhite(colour: Color): Color {
    val own = colour.copy(alpha = 1f)
    fun stands(c: Color) = contrast(Color.White, c) >= WORDS_CONTRAST
    if (stands(own)) return own
    var low = 0f
    var high = 1f
    repeat(24) {
        val mid = (low + high) / 2f
        if (stands(lerp(own, Color.Black, mid))) high = mid else low = mid
    }
    return lerp(own, Color.Black, high)
}

/** How far words stand from what they are written on, at the least. */
const val WORDS_CONTRAST = 4.5f

/** The three kinds of button an iPhone has. */
enum class ButtonKind {
    /** The one that does the thing (Continue, Set wallpaper, Done): a solid pill of the accent. */
    Main,

    /** A second choice (Not now, Keep it, Clear): a grey pill, its words in the words' colour. */
    Grey,

    /** A small action: words in the accent, with nothing behind them. */
    Plain,
}

/** What a button is filled with, and the colour of its words. */
@Immutable
data class ButtonColours(val fill: Color, val words: Color)

/**
 * A button of [kind] in the light or dark of [surfaces], for a palette whose accent, as the
 * interface draws it, is [accent]. A main button's words take the ink the accent needs
 * ([inkOnTint]): white on blue, near-black on yellow.
 */
fun AppleColours.button(kind: ButtonKind, accent: Color, surfaces: AppleSurfaces): ButtonColours = when (kind) {
    ButtonKind.Main -> ButtonColours(fill = accent, words = inkOnTint(accent))
    ButtonKind.Grey -> ButtonColours(fill = surfaces.grey, words = surfaces.words)
    ButtonKind.Plain -> ButtonColours(fill = Color.Transparent, words = accent)
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
