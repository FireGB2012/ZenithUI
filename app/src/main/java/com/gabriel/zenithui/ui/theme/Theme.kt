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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.GlassBody
import com.gabriel.zenithui.data.GlassLevels
import com.gabriel.zenithui.data.ZenithSettings

val LocalPalette = staticCompositionLocalOf { Palettes.Midnight }

/**
 * The wallpaper frosted: what glass shows where there is no shader to draw it, and how anything
 * else asks whether there is a wallpaper to show at all.
 */
val LocalBackdrop = staticCompositionLocalOf<ImageBitmap?> { null }

/** The wallpaper at every softness the lens shows it: bent at a pane's rim, softened in its face, thrown back by its edge. */
val LocalGlassLevels = staticCompositionLocalOf<GlassLevels?> { null }

/** Android's current wallpaper scroll, read during drawing so paging only invalidates the material. */
val LocalWallpaperOffset = staticCompositionLocalOf<State<Offset>> { mutableStateOf(Offset(0.5f, 0.5f)) }

/** Size of the window in pixels, so glass panels can align the backdrop to the screen. */
val LocalScreenSize = staticCompositionLocalOf { IntSize.Zero }

/** 0f = flat translucency, 1f = full liquid glass. */
val LocalGlassStrength = staticCompositionLocalOf { 0.8f }

/** How much body every pane has: whether it is lifted off a wallpaper of its own tone. */
val LocalGlassBody = staticCompositionLocalOf { GlassBody.Clear }

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
    /** The wallpaper for the glass to show, if Zenith has a copy of it. */
    glass: GlassLevels? = null,
    screenSize: IntSize = IntSize.Zero,
    content: @Composable () -> Unit,
) {
    val base = Palettes.resolve(settings)
    val dark = settings.isDark(isSystemInDarkTheme())
    // In the other tone a palette keeps its accents and gets a backdrop of its own (see toned).
    val target = base.toned(dark)

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

    // One sensor for the whole launcher, read by every glass surface.
    val tilt = rememberTilt(settings.glassShine)

    CompositionLocalProvider(
        LocalTilt provides tilt,
        LocalPalette provides palette,
        LocalBackdrop provides glass?.frost,
        LocalGlassLevels provides glass,
        LocalScreenSize provides screenSize,
        LocalGlassStrength provides settings.glass,
        LocalGlassBody provides GlassBody.byId(settings.glassBody),
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
