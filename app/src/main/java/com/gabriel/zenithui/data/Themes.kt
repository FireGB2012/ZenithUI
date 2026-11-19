package com.gabriel.zenithui.data

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * How a theme's panels are made. Every panel in Zenith is asked for as glass (`Modifier.glass`);
 * the theme says what that glass is.
 */
enum class ThemeMaterial(val id: String) {
    /** Liquid glass: the wallpaper through the pane, bent at its rim, a hairline of light round it. */
    GLASS("glass"),

    /** Flat cards: a solid face, a hairline edge and a soft, short shadow. */
    FLAT("flat");

    companion object {
        /**
         * The material by its id, and glass for any other: one Zenith never had, or Neon's smoke or
         * Retro's blocks, which went with those themes (see [Themes.successor]).
         */
        fun byId(id: String?): ThemeMaterial = entries.firstOrNull { it.id == id } ?: GLASS
    }
}

/** Whether a theme follows Light and Dark, or is always one of them. */
enum class ThemeTone(val id: String) {
    FOLLOWS("follows"),
    DARK("dark"),
    LIGHT("light");

    companion object {
        fun byId(id: String?): ThemeTone = entries.firstOrNull { it.id == id } ?: FOLLOWS
    }
}

/**
 * A theme's colours in one tone, light or dark, as ARGB. Kept as plain numbers, not Compose's
 * colours, so a theme can be written down, tested and handed to somebody else whole.
 */
@Immutable
data class ThemeColours(
    /** The page, from top to bottom: Settings, sheets, setup. */
    val backdrop: List<Long>,
    /** The face of a panel that is not glass: a group of rows, a card. Its alpha is its own. */
    val surface: Long,
    /** Words, and the icons beside them. Second lines are the same at [muted]. */
    val ink: Long,
    /** The colour of actions: buttons, what is chosen, sliders, links. */
    val accent: Long,
    /** A second accent, for the far end of anything drawn in two. */
    val accentAlt: Long,
    /** Words on the accent. */
    val onAccent: Long,
    /** A panel's outline, in a theme that is not glass: a hairline. */
    val edge: Long,
    /** What a panel's shadow is drawn in. */
    val shadow: Long,
    /** A switch that is on. */
    val switchOn: Long,
    /** What cannot be undone, and notification dots. */
    val destructive: Long,
    /** The veil in glass where nothing else colours it. */
    val glassTint: Long,
    /** Names on the wallpaper, under the icons. */
    val wallInk: Long,
    /** What dims the screen behind a sheet. */
    val scrim: Long,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("backdrop", JSONArray(backdrop.map(::hex)))
        put("surface", hex(surface))
        put("ink", hex(ink))
        put("accent", hex(accent))
        put("accentAlt", hex(accentAlt))
        put("onAccent", hex(onAccent))
        put("edge", hex(edge))
        put("shadow", hex(shadow))
        put("switchOn", hex(switchOn))
        put("destructive", hex(destructive))
        put("glassTint", hex(glassTint))
        put("wallInk", hex(wallInk))
        put("scrim", hex(scrim))
    }

    companion object {
        /** Second lines and small print in Light: the ink at this much, which every theme is tested to hold. */
        const val MUTED = 0.74f

        /**
         * The same in Dark. Testers asked twice for the subtitles in Settings to stand out more
         * against the dark, so there they are 84% of the ink: seven to one or better on every dark
         * page and panel, and still a step behind the words above them.
         */
        const val MUTED_DARK = 0.84f

        /** How much of the ink second lines and small print are, in Dark or in Light. */
        fun muted(dark: Boolean): Float = if (dark) MUTED_DARK else MUTED

        /** "#AARRGGBB", as a theme is written down. */
        fun hex(argb: Long): String = "#%08X".format(argb and 0xFFFFFFFFL)

        /** "#RRGGBB" or "#AARRGGBB"; null for anything else. */
        fun parse(text: String?): Long? {
            if (text == null || !text.startsWith("#")) return null
            val digits = text.substring(1)
            if (digits.length != 6 && digits.length != 8) return null
            val value = digits.toLongOrNull(16) ?: return null
            return if (digits.length == 6) 0xFF000000L or value else value
        }

        /** What can be read from [o], and [fallback]'s colours for the rest. Never throws. */
        fun fromJson(o: JSONObject?, fallback: ThemeColours): ThemeColours {
            if (o == null) return fallback
            fun colour(key: String, otherwise: Long): Long = parse(o.optString(key, "")) ?: otherwise
            val backdrop = o.optJSONArray("backdrop")
                ?.let { list -> (0 until list.length()).mapNotNull { parse(list.optString(it, "")) } }
                ?.takeIf { it.isNotEmpty() }
                ?.take(4)
                ?: fallback.backdrop
            return ThemeColours(
                backdrop = backdrop,
                surface = colour("surface", fallback.surface),
                ink = colour("ink", fallback.ink),
                accent = colour("accent", fallback.accent),
                accentAlt = colour("accentAlt", fallback.accentAlt),
                onAccent = colour("onAccent", fallback.onAccent),
                edge = colour("edge", fallback.edge),
                shadow = colour("shadow", fallback.shadow),
                switchOn = colour("switchOn", fallback.switchOn),
                destructive = colour("destructive", fallback.destructive),
                glassTint = colour("glassTint", fallback.glassTint),
                wallInk = colour("wallInk", fallback.wallInk),
                scrim = colour("scrim", fallback.scrim),
            )
        }
    }
}

/**
 * The colours a theme's glass is made in, where its panels are Liquid Glass: the veil it lays thin
 * over the wallpaper, and the light its edge catches. The same in Light and in Dark, as glass on the
 * wallpaper is. Liquid Glass's are Apple's: a grey with no colour in it, and white light. A theme of
 * its own colours its glass with these and with nothing else: the face stays flat and clear, and the
 * light stays on the edge, a hairline, never a line drawn round it. Their alpha is not used: how
 * much of each there is, the glass decides.
 */
@Immutable
data class ThemeGlass(
    /** The veil of glass standing on the wallpaper (the dock, widgets, folders, Search): thin, and as deep as a grey. */
    val veil: Long,
    /** The light on the edge where it faces the lamp, up and to the left. */
    val rim: Long,
    /** The light on the far corner, where light that crossed the glass leaves it. Round the sides, a mix of the two. */
    val rimFar: Long,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("veil", ThemeColours.hex(veil))
        put("rim", ThemeColours.hex(rim))
        put("rimFar", ThemeColours.hex(rimFar))
    }

    companion object {
        /** What can be read from [o], and [fallback]'s colours for the rest. Never throws. */
        fun fromJson(o: JSONObject?, fallback: ThemeGlass): ThemeGlass {
            if (o == null) return fallback
            fun colour(key: String, otherwise: Long): Long = ThemeColours.parse(o.optString(key, "")) ?: otherwise
            return ThemeGlass(
                veil = colour("veil", fallback.veil),
                rim = colour("rim", fallback.rim),
                rimFar = colour("rimFar", fallback.rimFar),
            )
        }
    }
}

/**
 * A theme: everything Zenith is drawn with, in one place. The colours for light and dark, what its
 * panels are made of and the colours of its glass, how much is blurred behind a sheet, how its
 * corners are cut, its outlines and shadows, and its font. Plain data, so a theme can be written
 * down as JSON and read back exactly ([toJson], [fromJson]): what one day lets a theme be shared
 * through Zenith Connect. Adding a theme is adding one of these to [Themes].
 */
@Immutable
data class Theme(
    val id: String,
    val name: String,
    val tagline: String,
    val material: ThemeMaterial,
    val tone: ThemeTone,
    val light: ThemeColours,
    val dark: ThemeColours,
    /** How far the home screen behind a sheet, a folder or the drawer is blurred, in dp: 0 only dims it. */
    val blurBehind: Float,
    /** Corners as they are drawn (1), or tighter: a share of the radius each was drawn with. */
    val corners: Float,
    /** The largest a corner is cut or rounded, in dp; 0 for no limit. */
    val cornerCap: Float,
    /** Corners cut off straight, as on a screen of big pixels, rather than rounded. */
    val cut: Boolean,
    /** Whether a pill stays a pill, whatever [corners] does to the rest. */
    val keepPills: Boolean,
    /** How thick a panel's outline is, in dp. */
    val edgeWidth: Float,
    /** How soft a panel's shadow is, in dp; 0 for a hard one, cut like paper. */
    val shadowBlur: Float,
    /** How far a panel's shadow falls, in dp: right, and down. */
    val shadowX: Float,
    val shadowY: Float,
    /** The font it is set in, one of `ZenithFonts`, unless you have chosen one of your own. */
    val font: String,
    /** The colours of its glass, where its panels are glass: Liquid Glass's unless it has its own. */
    val glass: ThemeGlass = Themes.LIQUID_GLASS,
) {
    /** Its colours in the tone in use. */
    fun colours(dark: Boolean): ThemeColours = if (dark) this.dark else light

    fun toJson(): JSONObject = JSONObject().apply {
        put("version", VERSION)
        put("id", id)
        put("name", name)
        put("tagline", tagline)
        put("material", material.id)
        put("tone", tone.id)
        put("light", light.toJson())
        put("dark", dark.toJson())
        put("blurBehind", blurBehind.toDouble())
        put("corners", corners.toDouble())
        put("cornerCap", cornerCap.toDouble())
        put("cut", cut)
        put("keepPills", keepPills)
        put("edgeWidth", edgeWidth.toDouble())
        put("shadowBlur", shadowBlur.toDouble())
        put("shadowX", shadowX.toDouble())
        put("shadowY", shadowY.toDouble())
        put("font", font)
        put("glass", glass.toJson())
    }

    companion object {
        /** What [toJson] writes, so a later Zenith can tell an older theme from its own. */
        const val VERSION = 1

        const val NAME_MOST = 40
        val CORNERS = 0f..1.5f
        val CORNER_CAP = 0f..64f
        val BLUR_BEHIND = 0f..32f
        val EDGE = 0f..4f
        val SHADOW = 0f..24f
        val SHADOW_FALL = -12f..12f

        /**
         * A theme from JSON, whoever wrote it: whatever can be read is, kept within what Zenith can
         * draw, and the rest is Liquid Glass's. Never throws.
         */
        fun fromJson(o: JSONObject): Theme {
            val d = Themes.LiquidGlass
            fun number(key: String, otherwise: Float, range: ClosedFloatingPointRange<Float>): Float {
                val read = o.optDouble(key, otherwise.toDouble()).toFloat()
                return if (read.isNaN()) otherwise else read.coerceIn(range)
            }
            return Theme(
                id = o.optString("id", "").trim().take(NAME_MOST).ifEmpty { "shared" },
                name = o.optString("name", "").trim().take(NAME_MOST).ifEmpty { "Shared theme" },
                tagline = o.optString("tagline", "").trim().take(NAME_MOST * 2),
                material = ThemeMaterial.byId(o.optString("material")),
                tone = ThemeTone.byId(o.optString("tone")),
                light = ThemeColours.fromJson(o.optJSONObject("light"), d.light),
                dark = ThemeColours.fromJson(o.optJSONObject("dark"), d.dark),
                blurBehind = number("blurBehind", d.blurBehind, BLUR_BEHIND),
                corners = number("corners", d.corners, CORNERS),
                cornerCap = number("cornerCap", d.cornerCap, CORNER_CAP),
                cut = o.optBoolean("cut", d.cut),
                keepPills = o.optBoolean("keepPills", d.keepPills),
                edgeWidth = number("edgeWidth", d.edgeWidth, EDGE),
                shadowBlur = number("shadowBlur", d.shadowBlur, SHADOW),
                shadowX = number("shadowX", d.shadowX, SHADOW_FALL),
                shadowY = number("shadowY", d.shadowY, SHADOW_FALL),
                font = o.optString("font", d.font).trim().take(NAME_MOST).ifEmpty { d.font },
                glass = ThemeGlass.fromJson(o.optJSONObject("glass"), d.glass),
            )
        }
    }
}

/**
 * Zenith's themes. Liquid Glass is the one it starts in, and the one any setting that does not name
 * one is in. Your theme ([ZenithSettings.theme]) is your favourite: what Zenith starts in. Any other
 * is yours to try ([trying]) until Zenith next starts.
 */
object Themes {

    /** Liquid Glass's glass, and any theme's that has none of its own: Apple's grey veil and white light. */
    val LIQUID_GLASS = ThemeGlass(veil = 0xFF3A3A3C, rim = 0xFFFFFFFF, rimFar = 0xFFFFFFFF)

    val LiquidGlass = Theme(
        id = "glass",
        name = "Liquid Glass",
        tagline = "Clear glass over your wallpaper, lit at the rim",
        material = ThemeMaterial.GLASS,
        tone = ThemeTone.FOLLOWS,
        light = ThemeColours(
            backdrop = listOf(0xFFF2F2F7, 0xFFF8F8FB, 0xFFFFFFFF),
            surface = 0x1210131A,
            ink = 0xFF10131A,
            accent = 0xFF007AFF,
            accentAlt = 0xFF007AFF,
            onAccent = 0xFFFFFFFF,
            edge = 0x2410131A,
            shadow = 0x33000000,
            switchOn = 0xFF34C759,
            destructive = 0xFFFF3B30,
            glassTint = 0xFF8E8E93,
            wallInk = 0xFFFFFFFF,
            scrim = 0x59000000,
        ),
        dark = ThemeColours(
            backdrop = listOf(0xFF000000, 0xFF0E0E10, 0xFF1C1C1E),
            surface = 0x12FFFFFF,
            ink = 0xFFFFFFFF,
            accent = 0xFF0A84FF,
            accentAlt = 0xFF0A84FF,
            onAccent = 0xFFFFFFFF,
            edge = 0x26FFFFFF,
            shadow = 0x66000000,
            switchOn = 0xFF30D158,
            destructive = 0xFFFF453A,
            glassTint = 0xFF8E8E93,
            wallInk = 0xFFFFFFFF,
            scrim = 0x59000000,
        ),
        blurBehind = 20f,
        corners = 1f,
        cornerCap = 0f,
        cut = false,
        keepPills = true,
        edgeWidth = 1f,
        shadowBlur = 16f,
        shadowX = 0f,
        shadowY = 6f,
        font = "system",
        glass = LIQUID_GLASS,
    )

    val Clean = Theme(
        id = "clean",
        name = "Clean",
        tagline = "Flat and light, nothing in the way",
        material = ThemeMaterial.FLAT,
        tone = ThemeTone.FOLLOWS,
        light = ThemeColours(
            backdrop = listOf(0xFFF4F4F5, 0xFFF7F7F8, 0xFFFAFAFA),
            surface = 0xFFFFFFFF,
            ink = 0xFF18181B,
            accent = 0xFF2563EB,
            accentAlt = 0xFF2563EB,
            onAccent = 0xFFFFFFFF,
            edge = 0x1A18181B,
            shadow = 0x1A000000,
            switchOn = 0xFF16A34A,
            destructive = 0xFFDC2626,
            glassTint = 0xFFA1A1AA,
            wallInk = 0xFFFFFFFF,
            scrim = 0x4D18181B,
        ),
        dark = ThemeColours(
            backdrop = listOf(0xFF09090B, 0xFF0F0F12, 0xFF141417),
            surface = 0xFF1C1C20,
            ink = 0xFFFAFAFA,
            accent = 0xFF3B82F6,
            accentAlt = 0xFF3B82F6,
            onAccent = 0xFFFFFFFF,
            edge = 0x1FFFFFFF,
            shadow = 0x66000000,
            switchOn = 0xFF22C55E,
            destructive = 0xFFEF4444,
            glassTint = 0xFF71717A,
            wallInk = 0xFFFFFFFF,
            scrim = 0x66000000,
        ),
        blurBehind = 0f,
        corners = 0.72f,
        cornerCap = 0f,
        cut = false,
        keepPills = true,
        edgeWidth = 1f,
        shadowBlur = 10f,
        shadowX = 0f,
        shadowY = 2f,
        font = "inter",
    )

    /**
     * Aurora's colours, always dark: a night of deep teal into indigo, an aurora's green for actions
     * with violet beside it, and words of a white with a breath of green in it.
     */
    private val auroraNight = ThemeColours(
        backdrop = listOf(0xFF031A1D, 0xFF061529, 0xFF110B2B),
        surface = 0x14D4FFF2,
        ink = 0xFFF2FFFB,
        accent = 0xFF4BE3B4,
        accentAlt = 0xFFA996FF,
        onAccent = 0xFF04241B,
        edge = 0x2EB8FFE6,
        shadow = 0x66000000,
        switchOn = 0xFF4BE3B4,
        destructive = 0xFFFF5C70,
        glassTint = 0xFF00E0C0,
        wallInk = 0xFFFFFFFF,
        scrim = 0x6602080C,
    )

    /**
     * Liquid Glass under the northern lights: the same clear glass, bent at the rim, with the light
     * its edge catches in an aurora's colours, green on the corner facing the light and violet on the
     * far one, over a veil with a little of the night's teal in it. Always dark, as the lights are.
     */
    val Aurora = Theme(
        id = "aurora",
        name = "Aurora",
        tagline = "Glass that catches the northern lights",
        material = ThemeMaterial.GLASS,
        tone = ThemeTone.DARK,
        light = auroraNight,
        dark = auroraNight,
        blurBehind = 20f,
        corners = 1f,
        cornerCap = 0f,
        cut = false,
        keepPills = true,
        edgeWidth = 1f,
        shadowBlur = 16f,
        shadowX = 0f,
        shadowY = 6f,
        font = "system",
        glass = ThemeGlass(veil = 0xFF1C3A40, rim = 0xFF4DFFB0, rimFar = 0xFFAE8CFF),
    )

    /**
     * Liquid Glass gone cold: the same clear glass, with the light on its edge the white of frost on
     * the corner facing the light and an ice blue on the far one, over a veil of cold slate. Pale ice
     * by day and a glacier's deep blue by night, with an ice blue for actions in both.
     */
    val Frost = Theme(
        id = "frost",
        name = "Frost",
        tagline = "Ice-cold glass, crisp and clear",
        material = ThemeMaterial.GLASS,
        tone = ThemeTone.FOLLOWS,
        light = ThemeColours(
            backdrop = listOf(0xFFE6F1FA, 0xFFEFF6FC, 0xFFF9FCFF),
            surface = 0x140C2A45,
            ink = 0xFF0A1A2B,
            accent = 0xFF0279BE,
            accentAlt = 0xFF0AA5D6,
            onAccent = 0xFFFFFFFF,
            edge = 0x240C2A45,
            shadow = 0x2E0A2540,
            switchOn = 0xFF0A9EE0,
            destructive = 0xFFE0244E,
            glassTint = 0xFF5CC8FF,
            wallInk = 0xFFFFFFFF,
            scrim = 0x4D0A1B2C,
        ),
        dark = ThemeColours(
            backdrop = listOf(0xFF030B16, 0xFF07182B, 0xFF0B243C),
            surface = 0x14D8EEFF,
            ink = 0xFFF0F8FF,
            accent = 0xFF4DC3FA,
            accentAlt = 0xFF8BE3FF,
            onAccent = 0xFF03182A,
            edge = 0x29D8EEFF,
            shadow = 0x66000000,
            switchOn = 0xFF4DC3FA,
            destructive = 0xFFFF6B86,
            glassTint = 0xFF3FB8FF,
            wallInk = 0xFFFFFFFF,
            scrim = 0x66020814,
        ),
        blurBehind = 20f,
        corners = 1f,
        cornerCap = 0f,
        cut = false,
        keepPills = true,
        edgeWidth = 1f,
        shadowBlur = 16f,
        shadowX = 0f,
        shadowY = 6f,
        font = "system",
        glass = ThemeGlass(veil = 0xFF2A3A4A, rim = 0xFFD2F3FF, rimFar = 0xFF52B9FF),
    )

    /** The three of glass first, Liquid Glass leading them, and then Clean. */
    val all = listOf(LiquidGlass, Aurora, Frost, Clean)

    val DEFAULT: String = LiquidGlass.id

    /** A theme by its id, or for one that went, the one after it ([successor]); Liquid Glass for one Zenith does not know. */
    fun byId(id: String?): Theme {
        val now = id?.let(::successor)
        return all.firstOrNull { it.id == now } ?: LiquidGlass
    }

    /**
     * The themes that went, and what a phone that was in one is in now: Neon, dark with a light of
     * its own on every edge, in Aurora, which is that in glass; Retro, which nothing is like any
     * more, in Liquid Glass, where everybody starts.
     */
    private val retired = mapOf("neon" to "aurora", "retro" to DEFAULT)

    /** The theme [id] is now: itself, or for one that went, the one after it. A theme of your own keeps its id. */
    fun successor(id: String): String = retired[id] ?: id

    /** What is shown: the one being tried, if any, else your favourite. */
    fun active(favourite: String, trying: String?): Theme = byId(trying ?: favourite)

    /** Whether [theme] is drawn dark, with Appearance asking for dark or not. */
    fun isDark(theme: Theme, appearanceDark: Boolean): Boolean = when (theme.tone) {
        ThemeTone.DARK -> true
        ThemeTone.LIGHT -> false
        ThemeTone.FOLLOWS -> appearanceDark
    }

    /**
     * A theme being tried, shown in place of your favourite until Zenith next starts. Not saved: a
     * Zenith that starts again starts in your theme. Null when nothing is being tried.
     */
    val trying = MutableStateFlow<String?>(null)
}

/** How readable one colour is on another, as the web's guidelines measure it. */
object ThemeContrast {
    /** From 1 (the same) to 21 (black on white). [fore] is composited onto [back] if it is see-through. */
    fun ratio(fore: Long, back: Long): Double {
        val behind = opaque(back)
        val a = luminance(over(fore, behind))
        val b = luminance(behind)
        return (maxOf(a, b) + 0.05) / (minOf(a, b) + 0.05)
    }

    /** [fore] laid over [back], as one opaque colour. */
    fun over(fore: Long, back: Long): Long {
        val alpha = ((fore ushr 24) and 0xFF) / 255.0
        val under = opaque(back)
        fun channel(shift: Int): Long {
            val f = (fore ushr shift) and 0xFF
            val b = (under ushr shift) and 0xFF
            return (f * alpha + b * (1 - alpha)).roundToLong().coerceIn(0, 255)
        }
        return 0xFF000000L or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    /** [argb] at [alpha] of its own alpha. */
    fun withAlpha(argb: Long, alpha: Float): Long {
        val a = (((argb ushr 24) and 0xFF) * alpha).roundToLong().coerceIn(0, 255)
        return (a shl 24) or (argb and 0xFFFFFF)
    }

    /** What is behind is taken as solid: a page is, and a panel is measured laid over one. */
    private fun opaque(argb: Long): Long = 0xFF000000L or (argb and 0xFFFFFFL)

    /** How bright [argb] is to the eye, from 0 (black) to 1 (white), its alpha left out. */
    fun luminance(argb: Long): Double {
        fun linear(shift: Int): Double {
            val c = ((argb ushr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear(16) + 0.7152 * linear(8) + 0.0722 * linear(0)
    }
}
