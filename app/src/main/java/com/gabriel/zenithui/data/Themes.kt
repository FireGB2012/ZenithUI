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
    FLAT("flat"),

    /** Smoked glass in the dark, with an edge that glows in the accent. */
    NEON("neon"),

    /** Chunky blocks: a solid face, a thick outline and a hard shadow with no blur at all. */
    RETRO("retro");

    companion object {
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

/** How much of what is behind a panel shows through it, and how softened: none is a solid face. */
enum class ThemeBlur(val id: String) {
    NONE("none"),
    MIST("mist"),
    FROST("frost"),
    HAZE("haze");

    companion object {
        fun byId(id: String?): ThemeBlur = entries.firstOrNull { it.id == id } ?: FROST
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
    /** Words, and the icons beside them. Second lines are the same at [MUTED]. */
    val ink: Long,
    /** The colour of actions: buttons, what is chosen, sliders, links. */
    val accent: Long,
    /** A second accent, for the far end of anything drawn in two. */
    val accentAlt: Long,
    /** Words on the accent. */
    val onAccent: Long,
    /** A panel's outline: a hairline, a glowing edge, a thick line. */
    val edge: Long,
    /** What a panel's shadow (or, in Neon, its glow) is drawn in. */
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
        /** Second lines and small print: the ink at this much, which every theme is tested to hold. */
        const val MUTED = 0.74f

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
 * A theme: everything Zenith is drawn with, in one place. The colours for light and dark, what its
 * panels are made of, how much is blurred behind a sheet, how its corners are cut, its outlines,
 * shadows and glow, and its font. Plain data, so a theme can be written down as JSON and read back
 * exactly ([toJson], [fromJson]): what one day lets a theme be shared through Zenith Connect. Adding
 * a theme is adding one of these to [Themes].
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
    /** What shows through a panel that is not Liquid Glass: in Neon, the wallpaper frosted. */
    val panelBlur: ThemeBlur,
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
    /** How far a panel's edge glows in the accent, in dp; 0 for none. */
    val glow: Float,
    /** The font it is set in, one of `ZenithFonts`, unless you have chosen one of your own. */
    val font: String,
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
        put("panelBlur", panelBlur.id)
        put("corners", corners.toDouble())
        put("cornerCap", cornerCap.toDouble())
        put("cut", cut)
        put("keepPills", keepPills)
        put("edgeWidth", edgeWidth.toDouble())
        put("shadowBlur", shadowBlur.toDouble())
        put("shadowX", shadowX.toDouble())
        put("shadowY", shadowY.toDouble())
        put("glow", glow.toDouble())
        put("font", font)
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
        val GLOW = 0f..24f

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
                panelBlur = ThemeBlur.byId(o.optString("panelBlur")),
                corners = number("corners", d.corners, CORNERS),
                cornerCap = number("cornerCap", d.cornerCap, CORNER_CAP),
                cut = o.optBoolean("cut", d.cut),
                keepPills = o.optBoolean("keepPills", d.keepPills),
                edgeWidth = number("edgeWidth", d.edgeWidth, EDGE),
                shadowBlur = number("shadowBlur", d.shadowBlur, SHADOW),
                shadowX = number("shadowX", d.shadowX, SHADOW_FALL),
                shadowY = number("shadowY", d.shadowY, SHADOW_FALL),
                glow = number("glow", d.glow, GLOW),
                font = o.optString("font", d.font).trim().take(NAME_MOST).ifEmpty { d.font },
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
        panelBlur = ThemeBlur.FROST,
        corners = 1f,
        cornerCap = 0f,
        cut = false,
        keepPills = true,
        edgeWidth = 1f,
        shadowBlur = 16f,
        shadowX = 0f,
        shadowY = 6f,
        glow = 0f,
        font = "system",
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
        panelBlur = ThemeBlur.NONE,
        corners = 0.72f,
        cornerCap = 0f,
        cut = false,
        keepPills = true,
        edgeWidth = 1f,
        shadowBlur = 10f,
        shadowX = 0f,
        shadowY = 2f,
        glow = 0f,
        font = "inter",
    )

    private val neonNight = ThemeColours(
        backdrop = listOf(0xFF05030B, 0xFF0B0720, 0xFF04020A),
        surface = 0xCC0D0A1C,
        ink = 0xFFF5F3FF,
        accent = 0xFF00E5FF,
        accentAlt = 0xFFFF2BD6,
        onAccent = 0xFF04020A,
        edge = 0xD900E5FF,
        shadow = 0x9900E5FF,
        switchOn = 0xFF3DFF8F,
        destructive = 0xFFFF3D71,
        glassTint = 0xFF6C3BFF,
        wallInk = 0xFFFFFFFF,
        scrim = 0x8C04020A,
    )

    val Neon = Theme(
        id = "neon",
        name = "Neon",
        tagline = "Dark, with edges that glow",
        material = ThemeMaterial.NEON,
        tone = ThemeTone.DARK,
        light = neonNight,
        dark = neonNight,
        blurBehind = 14f,
        panelBlur = ThemeBlur.FROST,
        corners = 0.85f,
        cornerCap = 0f,
        cut = false,
        keepPills = true,
        edgeWidth = 1.2f,
        shadowBlur = 0f,
        shadowX = 0f,
        shadowY = 0f,
        glow = 9f,
        font = "condensed",
    )

    val Retro = Theme(
        id = "retro",
        name = "Retro",
        tagline = "Warm colours, chunky blocks, a pixel's edge",
        material = ThemeMaterial.RETRO,
        tone = ThemeTone.FOLLOWS,
        light = ThemeColours(
            backdrop = listOf(0xFFF3E3C3, 0xFFF7EBD3, 0xFFFBF3E2),
            surface = 0xFFFFF8EA,
            ink = 0xFF3A2215,
            accent = 0xFFC2410C,
            accentAlt = 0xFFE8A317,
            onAccent = 0xFFFFF8EA,
            edge = 0xFF3A2215,
            shadow = 0xFF3A2215,
            switchOn = 0xFF4D7C2F,
            destructive = 0xFFB3261E,
            glassTint = 0xFFC9A46A,
            wallInk = 0xFFFFF6E0,
            scrim = 0x663A2215,
        ),
        dark = ThemeColours(
            backdrop = listOf(0xFF1F140C, 0xFF2A1B10, 0xFF1A100A),
            surface = 0xFF3A2719,
            ink = 0xFFFBE8C8,
            accent = 0xFFFF7A2F,
            accentAlt = 0xFFFFC23D,
            onAccent = 0xFF2A160A,
            edge = 0xD9FBE8C8,
            shadow = 0xCC000000,
            switchOn = 0xFF8CC152,
            destructive = 0xFFFF5A4A,
            glassTint = 0xFF8A6440,
            wallInk = 0xFFFFF3DD,
            scrim = 0x8C120A05,
        ),
        blurBehind = 0f,
        panelBlur = ThemeBlur.NONE,
        corners = 0.5f,
        cornerCap = 10f,
        cut = true,
        keepPills = false,
        edgeWidth = 2f,
        shadowBlur = 0f,
        shadowX = 3f,
        shadowY = 3f,
        glow = 0f,
        font = "mono",
    )

    val all = listOf(LiquidGlass, Clean, Neon, Retro)

    val DEFAULT: String = LiquidGlass.id

    /** A theme by its id; Liquid Glass for one Zenith does not know. */
    fun byId(id: String?): Theme = all.firstOrNull { it.id == id } ?: LiquidGlass

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

    private fun luminance(argb: Long): Double {
        fun linear(shift: Int): Double {
            val c = ((argb ushr shift) and 0xFF) / 255.0
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear(16) + 0.7152 * linear(8) + 0.0722 * linear(0)
    }
}
