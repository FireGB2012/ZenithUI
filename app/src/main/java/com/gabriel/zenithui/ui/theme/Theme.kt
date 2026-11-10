package com.gabriel.zenithui.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
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
import com.gabriel.zenithui.data.GlassTuning
import com.gabriel.zenithui.data.IconStyles
import com.gabriel.zenithui.data.LabelSizes
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.LabelStyle
import com.gabriel.zenithui.ui.common.LocalIconStyle
import com.gabriel.zenithui.ui.common.LocalLabelStyle
import com.gabriel.zenithui.ui.common.LocalPressStyle
import com.gabriel.zenithui.ui.common.MotionLevel
import com.gabriel.zenithui.ui.common.rememberAnimationsEnabled

/** The colours everything is drawn in: Zenith's one look in Light or Dark, or a widget's own colour inside it. */
val LocalPalette = staticCompositionLocalOf { NeutralLook.Dark }

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

/**
 * Every style the theme has, set in [family]: Zenith's own sizes and weights where it has them,
 * Material's for the rest, so no text anywhere is left in a font that was not chosen.
 */
private fun typographyIn(family: FontFamily): Typography {
    val base = ZenithTypography
    fun TextStyle.inFamily() = copy(fontFamily = family)
    return Typography(
        displayLarge = base.displayLarge.inFamily(),
        displayMedium = base.displayMedium.inFamily(),
        displaySmall = base.displaySmall.inFamily(),
        headlineLarge = base.headlineLarge.inFamily(),
        headlineMedium = base.headlineMedium.inFamily(),
        headlineSmall = base.headlineSmall.inFamily(),
        titleLarge = base.titleLarge.inFamily(),
        titleMedium = base.titleMedium.inFamily(),
        titleSmall = base.titleSmall.inFamily(),
        bodyLarge = base.bodyLarge.inFamily(),
        bodyMedium = base.bodyMedium.inFamily(),
        bodySmall = base.bodySmall.inFamily(),
        labelLarge = base.labelLarge.inFamily(),
        labelMedium = base.labelMedium.inFamily(),
        labelSmall = base.labelSmall.inFamily(),
    )
}

@Composable
fun ZenithTheme(
    settings: ZenithSettings,
    /** The wallpaper for the glass to show, if Zenith has a copy of it. */
    glass: GlassLevels? = null,
    screenSize: IntSize = IntSize.Zero,
    /** The Glass studio's tuning; Zenith's own glass when it is not in effect. */
    glassTuning: GlassTuning = GlassTuning(),
    content: @Composable () -> Unit,
) {
    // One look, iOS's own, in Light or in Dark. The palette once chosen (paletteId) is still kept
    // in the settings, so old settings, Looks and backups read, but nothing here reads it. With no
    // palette to cross-fade, Light and Dark change at once, as an iPhone's do.
    val dark = settings.isDark(isSystemInDarkTheme())
    val palette = NeutralLook.of(dark)

    // The blue for Material's own controls too (the date picker, the handles that select text),
    // with white words on it.
    val scheme = if (palette.dark) {
        darkColorScheme(
            primary = palette.accent,
            secondary = palette.accent,
            background = palette.backdrop.first(),
            surface = Color(0x1FFFFFFF),
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            secondary = palette.accent,
            background = palette.backdrop.first(),
            surface = Color(0x33FFFFFF),
            onPrimary = Color.White,
            onBackground = Color(0xFF10131A),
            onSurface = Color(0xFF10131A),
        )
    }

    val font = ZenithFonts.byId(settings.font)
    val typography = remember(font) { typographyIn(font.family) }
    val numbers = remember(font, settings.numberFont) { ZenithFonts.numbers(settings.numberFont, font) }

    // One sensor for the whole launcher: the light every glass surface reads, and how far the tilt
    // floats the home screen, which it does not with movement reduced.
    val tilt = rememberTilt(settings.glassShine, floats = !MotionLevel.of(settings.motion, rememberAnimationsEnabled()).reduced)

    CompositionLocalProvider(
        LocalTilt provides tilt.light,
        LocalParallax provides tilt.parallax,
        LocalPalette provides palette,
        LocalBackdrop provides glass?.frost,
        LocalGlassLevels provides glass,
        LocalScreenSize provides screenSize,
        LocalGlassStrength provides settings.glass,
        LocalGlassBody provides GlassBody.byId(settings.glassBody),
        LocalIconStyle provides IconStyles.of(settings.iconStyle),
        LocalLabelStyle provides LabelStyle(LabelSizes.sp(settings.labelSize).sp, settings.labelShadow),
        LocalZenithFont provides font.family,
        LocalNumberFont provides numbers,
        LocalGlassTuning provides glassTuning,
        LocalPressStyle provides settings.press,
    ) {
        MaterialTheme(
            colorScheme = scheme,
            typography = typography,
            shapes = MaterialTheme.shapes.copy(
                extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(34.dp),
            ),
            content = content,
        )
    }
}
