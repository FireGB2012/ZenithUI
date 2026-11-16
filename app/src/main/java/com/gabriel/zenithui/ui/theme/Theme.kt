package com.gabriel.zenithui.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.gabriel.zenithui.data.Theme
import com.gabriel.zenithui.data.Themes
import com.gabriel.zenithui.data.WidgetLooks
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.LabelStyle
import com.gabriel.zenithui.ui.common.LocalIconStyle
import com.gabriel.zenithui.ui.common.LocalLabelStyle
import com.gabriel.zenithui.ui.common.LocalPressStyle
import com.gabriel.zenithui.ui.common.MotionLevel
import com.gabriel.zenithui.ui.common.rememberAnimationsEnabled

/** The colours everything is drawn in: the theme's in Light or Dark (see [LocalTheme]), or a widget's own colour inside it. */
val LocalPalette = staticCompositionLocalOf { NeutralLook.Dark }

/**
 * The wallpaper frosted: what glass shows where there is no shader to draw it, and how anything
 * else asks whether there is a wallpaper to show at all.
 */
val LocalBackdrop = staticCompositionLocalOf<ImageBitmap?> { null }

/** The wallpaper at every softness the lens shows it: bent at a pane's rim, softened in its face, thrown back by its edge. */
val LocalGlassLevels = staticCompositionLocalOf<GlassLevels?> { null }

/**
 * The lens made for [LocalGlassLevels], once for each wallpaper, which every pane shares (see
 * `sharedLens`). Null where none was made: before Android 13, without a copy of the wallpaper, and
 * outside [ZenithTheme].
 */
internal val LocalLens = staticCompositionLocalOf<LiquidGlassShader?> { null }

/** Android's current wallpaper scroll, read during drawing so paging only invalidates the material. */
val LocalWallpaperOffset = staticCompositionLocalOf<State<Offset>> { mutableStateOf(Offset(0.5f, 0.5f)) }

/**
 * How much darker the home screen draws the wallpaper (Customise's "Dim the wallpaper"): 0 for not
 * at all. Glass shows its own copy of the wallpaper, not the veil laid over it, so it darkens that
 * copy by as much. Read while drawing, so the veil fading in redraws only the glass.
 */
val LocalWallpaperDim = staticCompositionLocalOf<State<Float>> { mutableStateOf(0f) }

/** Size of the window in pixels, so glass panels can align the backdrop to the screen. */
val LocalScreenSize = staticCompositionLocalOf { IntSize.Zero }

/** 0f = flat translucency, 1f = full liquid glass. */
val LocalGlassStrength = staticCompositionLocalOf { 0.8f }

/** How much body every pane has: whether it is lifted off a wallpaper of its own tone. */
val LocalGlassBody = staticCompositionLocalOf { GlassBody.Clear }

/**
 * How Zenith's own widgets are drawn, colour or glass ([WidgetLooks]): one switch for every one of
 * them. Glass when nothing says otherwise, as the widgets were before there was a choice.
 */
val LocalWidgetLook = staticCompositionLocalOf { WidgetLooks.GLASS }

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
    /**
     * Whether a change of theme cross-fades what is on screen: yes for a screen of Zenith's, no
     * for the Notch's little window over other apps, which changes at once.
     */
    crossfade: Boolean = true,
    content: @Composable () -> Unit,
) {
    // The theme being tried, if any, else yours: see Themes.trying.
    val trying by Themes.trying.collectAsState()
    val requested = Themes.active(settings.theme, trying)
    val reduced = MotionLevel.of(settings.motion, rememberAnimationsEnabled()).reduced
    if (crossfade) {
        ThemeCrossfade(requested, reduced) { shown -> Themed(settings, shown, glass, screenSize, glassTuning, content) }
    } else {
        Themed(settings, requested, glass, screenSize, glassTuning, content)
    }
}

/** Everything [content] is drawn with, in [theme]. */
@Composable
private fun Themed(
    settings: ZenithSettings,
    theme: Theme,
    glass: GlassLevels?,
    screenSize: IntSize,
    glassTuning: GlassTuning,
    content: @Composable () -> Unit,
) {
    // The theme's colours in Light or in Dark: Neon is always dark, the others follow Appearance.
    // Light and Dark change at once, as an iPhone's do; a change of theme cross-fades.
    val dark = Themes.isDark(theme, settings.isDark(isSystemInDarkTheme()))
    val palette = remember(theme, dark) { theme.palette(dark) }

    // The accent for Material's own controls too (the date picker, the handles that select text),
    // with the theme's words on it.
    val scheme = if (palette.dark) {
        darkColorScheme(
            primary = palette.accent,
            secondary = palette.accent,
            background = palette.backdrop.first(),
            surface = palette.surface,
            onPrimary = palette.onAccent,
            onBackground = palette.ink,
            onSurface = palette.ink,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            secondary = palette.accent,
            background = palette.backdrop.first(),
            surface = palette.surface,
            onPrimary = palette.onAccent,
            onBackground = palette.ink,
            onSurface = palette.ink,
        )
    }

    // Your own font if you chose one; with the default, the theme's.
    val font = ZenithFonts.forTheme(settings.font, theme.font)
    val typography = remember(font) { typographyIn(font.family) }
    val numbers = remember(font, settings.numberFont) { ZenithFonts.numbers(settings.numberFont, font) }

    // One sensor for the whole launcher: the light every glass surface reads, and how far the tilt
    // floats the home screen, which it does not with movement reduced. Only glass catches the light.
    val tilt = rememberTilt(settings.glassShine && theme.isGlass, floats = !MotionLevel.of(settings.motion, rememberAnimationsEnabled()).reduced)

    // One lens for the wallpaper, shared by every pane of glass, made again only when the wallpaper
    // changes: a page of panes coming into view no longer compiles the shader once for each.
    val lens = rememberLens(if (theme.isGlass) glass else null)

    CompositionLocalProvider(
        LocalTheme provides theme,
        LocalTilt provides tilt.light,
        LocalParallax provides tilt.parallax,
        LocalPalette provides palette,
        LocalBackdrop provides glass?.frost,
        LocalGlassLevels provides glass,
        LocalLens provides lens,
        // For glass made from the wallpaper where there is no copy of the wallpaper to read.
        LocalWallColour provides savedWallColour(settings),
        LocalScreenSize provides screenSize,
        LocalGlassStrength provides settings.glass,
        LocalGlassBody provides GlassBody.byId(settings.glassBody),
        LocalWidgetLook provides WidgetLooks.of(settings.widgetLook),
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
                extraLarge = theme.shape(androidx.compose.foundation.shape.RoundedCornerShape(34.dp)) as androidx.compose.foundation.shape.CornerBasedShape,
            ),
            content = content,
        )
    }
}
