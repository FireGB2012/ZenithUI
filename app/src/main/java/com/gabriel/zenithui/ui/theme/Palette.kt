package com.gabriel.zenithui.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import com.gabriel.zenithui.data.WallColors
import com.gabriel.zenithui.data.ZenithSettings

/**
 * The colours something is drawn in. Zenith itself has one look, [NeutralLook], in Light or in
 * Dark, and a widget or folder may wear a colour of its own inside it. The other palettes, in
 * [Palettes], are only the colour sets Zenith's painted wallpapers are made from.
 */
@Immutable
data class ZenithPalette(
    val id: String,
    val name: String,
    val tagline: String,
    val accent: Color,
    val accentAlt: Color,
    val backdrop: List<Color>,
    val glassTint: Color,
    val dark: Boolean = true,
    /** How much of a widget's own colour this carries, 0 to 1; only InOwnColour sets it. */
    val ownColour: Float = 0f,
) {
    val onGlass: Color get() = if (dark) Color.White else Color(0xFF10131A)

    /**
     * Second lines, small print, the names of options not chosen. It was 62% of the ink, which on
     * a pale backdrop came out barely above what small print needs; testers found the subtitles
     * in Settings hard to pick out from the dark. At 74% it holds six to one on every backdrop.
     */
    val onGlassMuted: Color get() = onGlass.copy(alpha = 0.74f)

    /** A switch that is on: iOS's green, as an iPhone's switches are, rather than the blue of actions. */
    val switchOn: Color get() = if (dark) Color(0xFF30D158) else Color(0xFF34C759)

    /** What cannot be undone, and notification dots: iOS's red, deeper in Light so it holds on white. */
    val destructive: Color get() = if (dark) Color(0xFFFF453A) else Color(0xFFFF3B30)

    /**
     * Decoration: a widget's own colour where it has one, else the words' ink. A heading or a gauge
     * in the plain look is in the ink, as on an iPhone, and only a colour somebody chose for a widget
     * shows in it, eased in and out as [ownColour] is.
     */
    fun decor(colour: Color): Color = if (ownColour <= 0f) onGlass else lerp(onGlass, colour, ownColour.coerceAtMost(1f))
}

/**
 * Zenith's one look: iOS 26's own colours, in Light or in Dark, made by ZenithTheme from
 * Appearance. Words in the ink; actions in iOS blue, with white words on it, the same blue at both
 * ends so nothing is a two-colour gradient; pages from black to iOS's dark grey, or from its pale
 * grey to white; and a grey that is no colour at all for the glass's veil.
 */
object NeutralLook {

    val Dark = ZenithPalette(
        id = "neutral",
        name = "Dark",
        tagline = "",
        accent = Color(0xFF0A84FF),
        accentAlt = Color(0xFF0A84FF),
        backdrop = listOf(Color(0xFF000000), Color(0xFF0E0E10), Color(0xFF1C1C1E)),
        glassTint = Color(0xFF8E8E93),
        dark = true,
    )

    val Light = ZenithPalette(
        id = "neutral",
        name = "Light",
        tagline = "",
        accent = Color(0xFF007AFF),
        accentAlt = Color(0xFF007AFF),
        backdrop = listOf(Color(0xFFF2F2F7), Color(0xFFF8F8FB), Color.White),
        glassTint = Color(0xFF8E8E93),
        dark = false,
    )

    fun of(dark: Boolean): ZenithPalette = if (dark) Dark else Light
}

/**
 * A wallpaper's colour set in light or in dark. Its accents stay. When the tone asked for is not
 * the set's own, the backdrop is made from those accents — a near-white or near-black base with a
 * little of each mixed in — rather than borrowed from another set, so a wallpaper painted from it
 * still belongs to this one. Only Zenith's painted wallpapers use it now, and its sums are kept as
 * they were, to the bit: a wallpaper is known by the colours it was painted in, so any change here
 * would paint every one again.
 */
fun ZenithPalette.toned(dark: Boolean): ZenithPalette {
    if (dark == this.dark) return this
    val backdrop = if (dark) {
        listOf(
            lerp(Color(0xFF07080D), accent, 0.07f),
            lerp(Color(0xFF0C0F1A), accentAlt, 0.16f),
            lerp(Color(0xFF06070B), accent, 0.05f),
        )
    } else {
        listOf(
            lerp(Color.White, accent, 0.10f),
            lerp(Color.White, accentAlt, 0.18f),
            lerp(Color.White, accent, 0.05f),
        )
    }
    return copy(dark = dark, backdrop = backdrop)
}

/**
 * This palette with [accent] in place of its own, for a widget given a colour of its own: the
 * second accent a little round the colour wheel from it, so gradients still have two ends.
 */
fun ZenithPalette.withAccent(accent: Color): ZenithPalette {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(accent.toArgb(), hsv)
    hsv[0] = (hsv[0] + 40f) % 360f
    return copy(accent = accent, accentAlt = Color(android.graphics.Color.HSVToColor(hsv)), glassTint = accent)
}

/**
 * The five colours a wallpaper is painted from, and whether it is the light or the dark picture.
 * Made from a wallpaper's colour set, never from the look Zenith itself is in.
 */
fun ZenithPalette.wallColors(): WallColors = WallColors(
    accent = accent.toArgb(),
    accentAlt = accentAlt.toArgb(),
    deep = backdrop.first().toArgb(),
    mid = backdrop.getOrElse(1) { backdrop.first() }.toArgb(),
    base = backdrop.last().toArgb(),
    dark = dark,
)

/**
 * The colour sets Zenith's painted wallpapers are made from, and nothing else now: the six free
 * ones, the ones bought with Aura points, your own, made in the theme maker while there was one,
 * and the one taken from the wallpaper. They were the app's themes once; Zenith itself is drawn in
 * [NeutralLook] whatever is stored. Their values are kept exactly, since a wallpaper painted from
 * one is known by its colours.
 */
object Palettes {

    val Midnight = ZenithPalette(
        id = "midnight",
        name = "Midnight",
        tagline = "Deep indigo, cool violet light",
        accent = Color(0xFF7C6CFF),
        accentAlt = Color(0xFF3ED2FF),
        backdrop = listOf(Color(0xFF0A0B18), Color(0xFF161033), Color(0xFF08111F)),
        glassTint = Color(0xFF6E63FF),
    )

    val Aurora = ZenithPalette(
        id = "aurora",
        name = "Aurora",
        tagline = "Northern greens over deep teal",
        accent = Color(0xFF3BE8B0),
        accentAlt = Color(0xFF48A3FF),
        backdrop = listOf(Color(0xFF04140F), Color(0xFF0A2A2B), Color(0xFF061726)),
        glassTint = Color(0xFF2FD3A5),
    )

    val Sunset = ZenithPalette(
        id = "sunset",
        name = "Sunset",
        tagline = "Warm amber melting into rose",
        accent = Color(0xFFFF8A5B),
        accentAlt = Color(0xFFFF5C8A),
        backdrop = listOf(Color(0xFF1B0A12), Color(0xFF3A1220), Color(0xFF190A17)),
        glassTint = Color(0xFFFF7A5C),
    )

    val Frost = ZenithPalette(
        id = "frost",
        name = "Frost",
        tagline = "Clear ice, bright daylight glass",
        accent = Color(0xFF2E7BFF),
        accentAlt = Color(0xFF00C2D1),
        backdrop = listOf(Color(0xFFEFF4FF), Color(0xFFDCE7F7), Color(0xFFF6F9FF)),
        glassTint = Color(0xFF9FC2FF),
        dark = false,
    )

    val Sakura = ZenithPalette(
        id = "sakura",
        name = "Sakura",
        tagline = "Soft petals with a plum shadow",
        accent = Color(0xFFFF8FC0),
        accentAlt = Color(0xFFB98CFF),
        backdrop = listOf(Color(0xFF190B18), Color(0xFF33143A), Color(0xFF1A0B1E)),
        glassTint = Color(0xFFFF9AC8),
    )

    val Graphite = ZenithPalette(
        id = "graphite",
        name = "Graphite",
        tagline = "Pure monochrome, zero distraction",
        accent = Color(0xFFE6E8EF),
        accentAlt = Color(0xFF9AA0AE),
        backdrop = listOf(Color(0xFF07080A), Color(0xFF15171C), Color(0xFF0B0C10)),
        glassTint = Color(0xFFAEB4C2),
    )

    val all = listOf(Midnight, Aurora, Sunset, Frost, Sakura, Graphite)

    // ---------------------------------------------------------------- aura sets
    // Bought with points rather than money, while Aura had a shop. Kept apart from [all], so only
    // those who bought one are offered it, and looked up by the id it was sold under.

    val Ember = ZenithPalette(
        id = "ember",
        name = "Ember",
        tagline = "Banked coals under dark iron",
        accent = Color(0xFFFF7A3D),
        accentAlt = Color(0xFFFFC46B),
        backdrop = listOf(Color(0xFF120A07), Color(0xFF2C1109), Color(0xFF160B08)),
        glassTint = Color(0xFFFF8A4C),
    )

    val Tide = ZenithPalette(
        id = "tide",
        name = "Tide",
        tagline = "Deep water with a green shore",
        accent = Color(0xFF34D6C8),
        accentAlt = Color(0xFF6FE39A),
        backdrop = listOf(Color(0xFF04121A), Color(0xFF072A33), Color(0xFF04161C)),
        glassTint = Color(0xFF2FC9C0),
    )

    val Orchid = ZenithPalette(
        id = "orchid",
        name = "Orchid",
        tagline = "Violet bloom on near-black",
        accent = Color(0xFFB06CFF),
        accentAlt = Color(0xFFFF7ADF),
        backdrop = listOf(Color(0xFF0D0714), Color(0xFF23103A), Color(0xFF120820)),
        glassTint = Color(0xFFA96BFF),
    )

    val Solstice = ZenithPalette(
        id = "solstice",
        name = "Solstice",
        tagline = "Low sun, long gold shadows",
        accent = Color(0xFFFFC24D),
        accentAlt = Color(0xFFFF8E6E),
        backdrop = listOf(Color(0xFF14100A), Color(0xFF2E2312), Color(0xFF19120B)),
        glassTint = Color(0xFFFFB865),
    )

    val Obsidian = ZenithPalette(
        id = "obsidian",
        name = "Obsidian",
        tagline = "Black glass with a gold rim",
        accent = Color(0xFFE8C88A),
        accentAlt = Color(0xFF8A93A8),
        backdrop = listOf(Color(0xFF040406), Color(0xFF101014), Color(0xFF07070A)),
        glassTint = Color(0xFFD8BE86),
    )

    val aura = listOf(Ember, Tide, Orchid, Solstice, Obsidian)

    /** The id of the colour set you made yourself, in the Plus theme maker while there was one. */
    const val CUSTOM = "custom"

    fun byId(id: String): ZenithPalette =
        (all + aura).firstOrNull { it.id == id } ?: Midnight

    /**
     * The colour set made from your own three colours. The backdrop is a gradient from the
     * background you chose towards your accent, as it was when the set was a theme the glass had
     * to catch something of; it stays so, since a wallpaper painted in it is known by its colours.
     */
    fun custom(settings: ZenithSettings): ZenithPalette {
        val accent = Color(settings.customAccent.toInt())
        val accentAlt = Color(settings.customAccentAlt.toInt())
        val base = Color(settings.customBackdrop.toInt())
        return ZenithPalette(
            id = CUSTOM,
            name = "Yours",
            tagline = "Built in the theme maker",
            accent = accent,
            accentAlt = accentAlt,
            backdrop = listOf(base, lerp(base, accent, 0.16f), lerp(base, accentAlt, 0.08f)),
            glassTint = accent,
            dark = settings.customDark,
        )
    }

    /** The id of the colour set taken from your wallpaper. */
    const val WALLPAPER = "wallpaper"

    /**
     * The colour set taken from the wallpaper, as it was last worked out (`WallpaperPalette`): its
     * liveliest colour, a second to go with it, and its main colour, kept deep or pale.
     */
    fun fromWallpaper(settings: ZenithSettings): ZenithPalette {
        val accent = Color(settings.wallAccent.toInt())
        val accentAlt = Color(settings.wallAccentAlt.toInt())
        val base = Color(settings.wallBackdrop.toInt())
        return ZenithPalette(
            id = WALLPAPER,
            name = "Wallpaper",
            tagline = "Taken from your wallpaper",
            accent = accent,
            accentAlt = accentAlt,
            backdrop = listOf(base, lerp(base, accent, 0.16f), lerp(base, accentAlt, 0.08f)),
            glassTint = accent,
            dark = settings.wallDark,
        )
    }

    /**
     * A wallpaper colour set by its id: one of the six, an Aura set, your own ([CUSTOM]) or the one
     * taken from the wallpaper ([WALLPAPER]). An id it does not know is Midnight, as it always was.
     */
    fun setById(id: String, settings: ZenithSettings): ZenithPalette = when (id) {
        CUSTOM -> custom(settings)
        WALLPAPER -> fromWallpaper(settings)
        else -> byId(id)
    }

    /**
     * The colours one of Zenith's designs is painted in, in the tone in use: the set it was given.
     * Settings saved before every design was given one have none, and take the set paletteId
     * names, which is what such a design was painted in, so it comes out exactly as it did.
     */
    fun forWallpaper(settings: ZenithSettings, dark: Boolean): ZenithPalette =
        setById(settings.wallpaperPalette.ifEmpty { settings.paletteId }, settings).toned(dark)
}
