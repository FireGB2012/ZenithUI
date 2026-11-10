package com.gabriel.zenithui.ui.home

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import com.gabriel.zenithui.data.WeatherCodes
import com.gabriel.zenithui.data.Widgets
import com.gabriel.zenithui.ui.theme.NeutralLook
import kotlin.math.max
import kotlin.math.min

/**
 * How a widget in the colour look is painted: a card running from [top] down to [bottom], its words
 * in [ink], its second lines in [quiet], and what can be pressed in [accent]. On every card Zenith
 * paints itself the words and the second lines both stand at least [WidgetColours.READABLE] to 1 on
 * either end, which is what WCAG asks of small words. A colour somebody chose gets whichever ink
 * reads better on it, which on a middling colour can fall a little short of that.
 */
@Immutable
data class WidgetFill(val top: Color, val bottom: Color, val ink: Color, val quiet: Color, val accent: Color)

/** The sky a weather widget is painted as in the colour look: see [WidgetColours.skyFor]. */
enum class Sky { Day, Night, Cloud, Rain, Snow, Storm }

/**
 * The colour look's colours, in the spirit of iOS's own widgets: the weather is its sky, the calendar
 * a white page with iOS's red, the note pale yellow paper, and every other widget a white card, or
 * iOS's dark grey in Dark. Each has a dark version for Dark. A widget given a colour of its own is a
 * card in that colour instead, whatever its kind, so nobody's chosen colour stops showing.
 *
 * The words on a card are whichever of Zenith's two inks, white or its near-black, stands out more
 * on it; its second lines are those words made as quiet as they can be while they still read.
 */
object WidgetColours {

    /** The contrast, on both ends of its card, that a widget's words and second lines never go below. */
    const val READABLE = 4.5f

    /**
     * The fill for a widget of [kind] ("weather", "calendar" and so on; a "No glass" type is read as
     * the widget it is a version of), in Dark or in Light. [sky] is the weather's, and only the
     * weather reads it. [own], a colour somebody chose for this widget, is the card whatever the kind.
     */
    fun of(kind: String, dark: Boolean, sky: Sky = Sky.Day, own: Color? = null): WidgetFill {
        if (own != null) return ownCard(own)
        val plain = if (dark) DARK_CARD else Color.White
        return when (Widgets.baseType(kind)) {
            Widgets.Weather.type -> skyCard(sky, dark)
            // iOS's red, as Zenith's badges have it, on a page that is white in Light.
            Widgets.Calendar.type -> card(plain, plain, accent = NeutralLook.of(dark).destructive)
            Widgets.Notes.type -> blueWhereItStands(if (dark) NOTE_DARK else NOTE_LIGHT, dark)
            else -> blueWhereItStands(plain, dark)
        }
    }

    /**
     * The sky for weather [code] (Open-Meteo's, as [WeatherCodes] reads it), [day] or night: clear
     * and partly cloudy skies are the day's blue or the night's, cloud and fog are grey, drizzle is
     * rain, and snow and thunder have skies of their own whatever the hour.
     */
    fun skyFor(code: Int, day: Boolean): Sky = when (WeatherCodes.kind(code)) {
        WeatherCodes.Kind.Clear, WeatherCodes.Kind.PartlyCloudy -> if (day) Sky.Day else Sky.Night
        WeatherCodes.Kind.Cloudy, WeatherCodes.Kind.Fog -> Sky.Cloud
        WeatherCodes.Kind.Drizzle, WeatherCodes.Kind.Rain -> Sky.Rain
        WeatherCodes.Kind.Snow -> Sky.Snow
        WeatherCodes.Kind.Storm -> Sky.Storm
    }

    /** A card from [top] to [bottom], in the ink that stands out more on it, with [accent] for actions, or that ink. */
    private fun card(top: Color, bottom: Color, accent: Color? = null): WidgetFill {
        val ink = inkOn(top, bottom)
        return WidgetFill(top, bottom, ink, quietOn(ink, top, bottom), accent ?: ink)
    }

    /**
     * A plain card in [colour]: iOS's blue for what can be pressed, as everywhere in Zenith, where
     * the blue stands out from the card, and the words' ink where it would not.
     */
    private fun blueWhereItStands(colour: Color, dark: Boolean): WidgetFill {
        val blue = NeutralLook.of(dark).accent
        return card(colour, colour, accent = if (contrast(blue, colour) >= STANDS_OUT) blue else null)
    }

    /**
     * A sky, paler at the top and deeper at the foot, as a sky is. Each is deep enough for white
     * words: iOS's own skies are paler than this, but white words on them fall short of what small
     * words need. In Dark every sky is a little deeper again.
     */
    private fun skyCard(sky: Sky, dark: Boolean): WidgetFill {
        val (top, bottom) = when (sky) {
            Sky.Day -> Color(0xFF2563D3) to Color(0xFF1A4BAE)
            Sky.Night -> Color(0xFF1C2A4A) to Color(0xFF0B1226)
            Sky.Cloud -> Color(0xFF5E6B7B) to Color(0xFF444F5D)
            Sky.Rain -> Color(0xFF52677D) to Color(0xFF384A5C)
            Sky.Snow -> Color(0xFF566D8B) to Color(0xFF3E5372)
            Sky.Storm -> Color(0xFF4A4F5C) to Color(0xFF2B2E36)
        }
        return if (dark) card(deeper(top), deeper(bottom)) else card(top, bottom)
    }

    /** [colour] in Dark: each of its channels at [DEEPER] of itself, the same sky later in the evening. */
    private fun deeper(colour: Color): Color = Color(colour.red * DEEPER, colour.green * DEEPER, colour.blue * DEEPER)

    /**
     * A card in a widget's own colour: the colour itself, a little lighter at the top and a little
     * deeper at the foot, the same in Light and in Dark, since it is the colour somebody chose.
     */
    private fun ownCard(own: Color): WidgetFill {
        val colour = own.copy(alpha = 1f)
        return card(lerp(colour, Color.White, OWN_SPREAD), lerp(colour, Color.Black, OWN_SPREAD))
    }

    /**
     * White or Zenith's near-black, whichever stands out more where it stands out least. They are
     * the two inks a palette carries ([NeutralLook.Dark] and [NeutralLook.Light]), so a card's words
     * can be drawn by every widget as they always are, in the palette's ink.
     */
    private fun inkOn(top: Color, bottom: Color): Color {
        val light = NeutralLook.Dark.onGlass
        val dark = NeutralLook.Light.onGlass
        fun worst(ink: Color) = min(contrast(ink, top), contrast(ink, bottom))
        return if (worst(light) >= worst(dark)) light else dark
    }

    /**
     * [ink] as quiet as a second line can be on this card: Zenith's muted 74%, made stronger a step
     * at a time only where the card needs it to read, and the ink itself where nothing less will.
     */
    private fun quietOn(ink: Color, top: Color, bottom: Color): Color {
        var alpha = QUIET
        while (alpha < 1f) {
            val quiet = ink.copy(alpha = alpha)
            if (contrast(quiet, top) >= READABLE && contrast(quiet, bottom) >= READABLE) return quiet
            alpha += QUIET_STEP
        }
        return ink
    }

    /** iOS's dark grey, its widgets' own in Dark. */
    private val DARK_CARD = Color(0xFF1C1C1E)

    /** A note's paper: pale yellow in Light, and a warm near-black in Dark. */
    private val NOTE_LIGHT = Color(0xFFFFF8D6)
    private val NOTE_DARK = Color(0xFF2C2A20)

    /** How much of each channel a sky keeps in Dark. */
    private const val DEEPER = 0.78f

    /** How far a widget's own colour is lightened at the top of its card and deepened at the foot. */
    private const val OWN_SPREAD = 0.10f

    /** Where a second line starts: the muted words' 74%. */
    private const val QUIET = 0.74f

    /** How much stronger a second line is made at a time until it reads. */
    private const val QUIET_STEP = 0.02f

    /** The contrast at which a colour for pressing stands out from its card: what WCAG asks of a control. */
    private const val STANDS_OUT = 3f
}

/**
 * How far apart two colours are to read, as WCAG has it: from 1 for the same to 21 for black on
 * white. [a] is the one drawn on [b]; a translucent [a] is laid over [b] first, as it would be drawn.
 */
internal fun contrast(a: Color, b: Color): Float {
    val front = if (a.alpha < 1f) a.compositeOver(b) else a
    val one = front.luminance()
    val other = b.luminance()
    return (max(one, other) + 0.05f) / (min(one, other) + 0.05f)
}
