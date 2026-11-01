package com.gabriel.zenithui.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.gabriel.zenithui.R

/** A typeface Zenith can be set in: what it is saved as, what it is called, and the font itself. */
@Immutable
data class ZenithFont(val id: String, val label: String, val family: FontFamily)

/**
 * How big numbers are set — the clocks, the battery, the countdown: in a font of their own, and
 * with [weight] in place of whatever weight each number asks for. Thin is a weight, not a font:
 * the phone's own sans at its lightest.
 */
@Immutable
data class NumberFont(val family: FontFamily, val weight: FontWeight? = null)

/**
 * The typefaces Zenith can be set in. Two are carried in the app, unchanged: Inter and JetBrains
 * Mono, under the SIL Open Font Licence (About & help → Fonts shows it). The rest are the phone's
 * own, asked for by the names Android gives them; a phone that lacks one shows its usual font.
 */
object ZenithFonts {
    const val SYSTEM = "system"

    /** For numbers: the same font as everything else. */
    const val SAME = "same"

    /** For numbers: the phone's own sans at its thinnest, as on the bedside clock. */
    const val THIN = "thin"

    private val weights = listOf(FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

    private fun device(name: String) = FontFamily(weights.map { Font(DeviceFontFamilyName(name), it) })

    val all: List<ZenithFont> = listOf(
        ZenithFont(SYSTEM, "System", FontFamily.SansSerif),
        ZenithFont(
            "inter", "Inter",
            FontFamily(Font(R.font.inter_regular, FontWeight.Normal), Font(R.font.inter_semibold, FontWeight.SemiBold)),
        ),
        ZenithFont("condensed", "Condensed", device("sans-serif-condensed")),
        ZenithFont("serif", "Serif", FontFamily.Serif),
        ZenithFont(
            "mono", "Mono",
            FontFamily(
                Font(R.font.jetbrains_mono_light, FontWeight.Light),
                Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
                Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
                Font(R.font.jetbrains_mono_semibold, FontWeight.SemiBold),
            ),
        ),
        ZenithFont("typewriter", "Typewriter", device("serif-monospace")),
        ZenithFont("casual", "Casual", device("casual")),
        ZenithFont("hand", "Hand", FontFamily.Cursive),
        ZenithFont("smallcaps", "Small caps", device("sans-serif-smallcaps")),
    )

    /** What the numbers can be set in: the same as the rest, thin, or any of the fonts. */
    val forNumbers: List<Pair<String, String>> = listOf(SAME to "Same", THIN to "Thin") + all.map { it.id to it.label }

    fun byId(id: String): ZenithFont = all.firstOrNull { it.id == id } ?: all.first()

    fun numbers(id: String, text: ZenithFont): NumberFont = when (id) {
        SAME, "" -> NumberFont(text.family)
        THIN -> NumberFont(FontFamily.SansSerif, FontWeight.Thin)
        else -> NumberFont(byId(id).family)
    }
}

/** The font everything is set in, for text given a style of its own rather than the theme's. */
val LocalZenithFont = staticCompositionLocalOf<FontFamily> { FontFamily.SansSerif }

/** How big numbers are set. */
val LocalNumberFont = staticCompositionLocalOf { NumberFont(FontFamily.SansSerif) }
