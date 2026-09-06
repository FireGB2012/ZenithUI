package com.gabriel.zenithui.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.ZenithSettings

val LocalPalette = staticCompositionLocalOf { Palettes.Midnight }

/** Pre-blurred wallpaper used as the backdrop of every glass surface. */
val LocalBackdrop = staticCompositionLocalOf<ImageBitmap?> { null }

/** Size of the window in pixels, so glass panels can align the backdrop to the screen. */
val LocalScreenSize = staticCompositionLocalOf { IntSize.Zero }

/** 0f = flat translucency, 1f = full liquid glass. */
val LocalGlassStrength = staticCompositionLocalOf { 0.8f }

private val ZenithTypography = Typography(
    displayLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Light, fontSize = 64.sp, letterSpacing = (-2).sp),
    headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 32.sp, letterSpacing = (-0.8).sp),
    headlineSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = (-0.4).sp),
    titleMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Normal, fontSize = 14.sp, letterSpacing = 0.1.sp),
    labelSmall = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Medium, fontSize = 11.sp, letterSpacing = 0.2.sp),
)

@Composable
fun ZenithTheme(
    settings: ZenithSettings,
    backdrop: ImageBitmap? = null,
    screenSize: IntSize = IntSize.Zero,
    content: @Composable () -> Unit,
) {
    val base = Palettes.byId(settings.paletteId)
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.appearance) {
        "light" -> false
        "dark" -> true
        else -> systemDark
    }
    val target = if (dark == base.dark) base else base.copy(
        dark = dark,
        backdrop = if (dark) Palettes.Midnight.backdrop else Palettes.Frost.backdrop,
    )

    // Switching palette should look like the light changing, not like a page reload.
    val spec = tween<Color>(durationMillis = 750)
    val accent by animateColorAsState(target.accent, spec, label = "accent")
    val accentAlt by animateColorAsState(target.accentAlt, spec, label = "accentAlt")
    val glassTint by animateColorAsState(target.glassTint, spec, label = "glassTint")
    val backdropTop by animateColorAsState(target.backdrop[0], spec, label = "backdropTop")
    val backdropMid by animateColorAsState(target.backdrop.getOrElse(1) { target.backdrop[0] }, spec, label = "backdropMid")
    val backdropEnd by animateColorAsState(target.backdrop.last(), spec, label = "backdropEnd")

    val palette = target.copy(
        accent = accent,
        accentAlt = accentAlt,
        glassTint = glassTint,
        backdrop = listOf(backdropTop, backdropMid, backdropEnd),
    )

    val scheme = if (palette.dark) {
        darkColorScheme(
            primary = palette.accent,
            secondary = palette.accentAlt,
            background = palette.backdrop.first(),
            surface = Color(0x1FFFFFFF),
            onPrimary = Color(0xFF0B0B12),
            onBackground = Color.White,
            onSurface = Color.White,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            secondary = palette.accentAlt,
            background = palette.backdrop.first(),
            surface = Color(0x33FFFFFF),
            onPrimary = Color.White,
            onBackground = Color(0xFF10131A),
            onSurface = Color(0xFF10131A),
        )
    }

    CompositionLocalProvider(
        LocalPalette provides palette,
        LocalBackdrop provides backdrop,
        LocalScreenSize provides screenSize,
        LocalGlassStrength provides settings.glass,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = ZenithTypography,
            shapes = MaterialTheme.shapes.copy(
                extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(34.dp),
            ),
            content = content,
        )
    }
}
