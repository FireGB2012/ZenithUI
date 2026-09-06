package com.gabriel.zenithui.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

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
) {
    val onGlass: Color get() = if (dark) Color.White else Color(0xFF10131A)
    val onGlassMuted: Color get() = onGlass.copy(alpha = 0.62f)
}

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

    fun byId(id: String): ZenithPalette = all.firstOrNull { it.id == id } ?: Midnight
}
