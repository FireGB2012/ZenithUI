package com.gabriel.zenithui.ui.theme

import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.data.GlassBody
import com.gabriel.zenithui.data.GlassLevels
import com.gabriel.zenithui.data.GlassTuning
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Clear floats over the wallpaper with the strongest bend and only a mist over what is behind it:
 * the dock, folders, small controls. Regular frosts what is behind it so text on top stays
 * readable: widgets, search, sheets.
 */
enum class GlassMaterial { Regular, Clear }

/**
 * Liquid Glass, after Apple's. What is behind a pane shows through it, softened as glass softens it
 * and a little richer in colour; the middle is flat, and only in a narrow band round the rim does the
 * glass curve away and bend what is behind it, magnifying it towards the edge and turning it over
 * right at the edge, like the rounded edge of a thick lens. A hairline of light hugs the outline,
 * brightest on the corner facing the light and on the corner opposite, faint between, in a little of
 * the colour round the pane, and it moves as the phone tilts. Under it is a soft shadow, more of one
 * for a bigger pane. There is no shading of a slab, no glow round the edge and no wobble: the glass
 * is clear and hard, and its edge is what says it is there. An [interactive] pane lights up from
 * within under your finger, and on the home screen every pane lights up, rim and face, as a ring of
 * light from a tap on the wallpaper passes ([LocalGlassRing]). Labels and icons drawn on top stay sharp.
 *
 * Glass standing on the wallpaper ([LocalOnWallpaper]: everything on a home page, the dock, the
 * Search pill) is the same in Light and in Dark, since the picture it stands on does not change with
 * the mode: a neutral grey veil, and the shadow, rim and body Dark gives it, in both. Glass on a page
 * or a sheet keeps a body that follows the mode, as iOS's sheets do: that body is the page, which
 * the glass cannot show under it.
 *
 * On Android 13+ with a readable wallpaper this is a GPU shader over copies of the wallpaper
 * softened ahead of time (no screen capture, no per-frame blur). Android 12 gets the frosted
 * wallpaper with a rim of the same kind; a wallpaper Zenith cannot read gets a frosted body of its
 * own rather than a stale picture.
 */
@Composable
fun Modifier.glass(
    shape: Shape = ZenithShapes.panel,
    tint: Color = Color.Unspecified,
    strength: Float = 1f,
    opacity: Float = 1f,
    border: Boolean = true,
    /** Extra body for surfaces that must stay readable over busy content, such as sheets. */
    solid: Float = 0f,
    material: GlassMaterial = GlassMaterial.Regular,
    /** Lights up from within under a finger: for panes you press, like the dock or a widget. */
    interactive: Boolean = false,
    /** The soft shadow the pane floats on. Off for glass lying flat inside other glass. */
    shadow: Boolean = true,
    /**
     * Coloured glass in [tint] rather than a neutral veil: the accent's glass, for the one button
     * that moves you on. It bends the wallpaper like clear glass and carries its own readability.
     */
    tinted: Boolean = false,
    /** How the pane moves under a finger, if it does: see [GlassMotion]. */
    motion: GlassMotion? = null,
    /** Whether [motion] answers a finger now: off for a button that cannot be pressed. */
    pressEnabled: Boolean = true,
    /** Drives the finger's light from outside, for a pane its own finger cannot reach. */
    light: PressLight? = null,
    /**
     * For a pane that something outside it moves or scales with a graphicsLayer, as a dragged lens
     * is: reads what moves it, while drawing, so the wallpaper behind is lined up again as it goes
     * rather than travelling with the pane. Inside [GlassMovedBy] it follows that as well.
     */
    tracking: (() -> Unit)? = null,
    /**
     * Glass made from the wallpaper, for the home screen's panes that stand on it (the dock, Search,
     * the glass widgets): its veil takes a little of the wallpaper's own colour where it sits
     * ([WALL_TINT]) and it glows with that colour underneath, following the wallpaper as it changes.
     * A pane given a colour in [tint] wears that instead, with no glow; glass that is not standing
     * on the wallpaper ([LocalOnWallpaper]) takes none.
     */
    wallpaperTint: Boolean = false,
): Modifier {
    // A theme that is not Liquid Glass draws every pane in its own material.
    val theme = LocalTheme.current
    if (!theme.isGlass) {
        return themedPanel(theme, shape, tint, tinted, opacity, border, solid, shadow, interactive, motion, pressEnabled, light)
    }
    val palette = LocalPalette.current
    val backdrop = LocalBackdrop.current
    val levels = LocalGlassLevels.current
    val screen = LocalScreenSize.current
    val wallpaperOffset = LocalWallpaperOffset.current
    val wallpaperDim = LocalWallpaperDim.current
    val tilt = LocalTilt.current
    val tuning = LocalGlassTuning.current
    val onWallpaper = LocalOnWallpaper.current
    // On something that moves its panes with a layer, as the tilt floats a home page's icons and
    // widgets and the dock ([GlassMovedBy]), the pane follows that as well as what it was given.
    val carried = LocalGlassTracking.current
    val follows = when {
        carried == null -> tracking
        tracking == null -> carried
        else -> ({ tracking(); carried() })
    }
    // On the wallpaper the glass is made as Dark makes it, in Light too: the shader, the shadow and
    // the rim are told it is dark, so nothing about it follows the mode.
    val dark = onWallpaper || palette.dark
    // A colour somebody chose (a folder's, the blue of a button) still tints the pane; with none,
    // glass on the wallpaper has its neutral grey veil.
    val paneTint = when {
        tint.isSpecified -> tint
        onWallpaper -> WALLPAPER_VEIL
        else -> palette.glassTint
    }
    val amount = (LocalGlassStrength.current * strength).coerceIn(0f, 1.4f)
    // Only glass on the wallpaper is made from it, and a colour somebody chose takes its place.
    val wallTinted = wallpaperTint && onWallpaper && !tinted && !tint.isSpecified
    // The wallpaper's colour for when there is no copy of it to read from: none, if never read.
    val savedWall = LocalWallColour.current
    // The ring of light a tap on the wallpaper sends out, on the home screen: read while drawing.
    val glassRing = LocalGlassRing.current
    val look = glassLook(
        material = material,
        strength = amount,
        solid = solid,
        opacity = opacity,
        dark = dark,
        tint = paneTint,
        border = border,
        depth = LocalGlassBody.current,
        tinted = tinted,
        onWallpaper = onWallpaper,
    ).tuned(tuning).let { if (wallTinted) it.copy(wallTint = WALL_TINT) else it }
    val lens = sharedLens(levels)
    var origin by remember { mutableStateOf(Offset.Zero) }
    // Where the pane was laid out, kept to work out where it is drawn while something moves it.
    val placed = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val scope = rememberCoroutineScope()
    val ownLight = if (interactive && light == null) remember { PressLight(scope) } else null
    val press = light ?: ownLight
    val moved = if (motion != null) remember { PressMotion(scope) } else null

    return this
        .then(if (moved != null && motion != null) Modifier.pressMotion(moved, motion, pressEnabled) else Modifier)
        .onGloballyPositioned {
            origin = it.positionInWindow()
            placed[0] = it
        }
        .then(
            if (ownLight != null) {
                Modifier.pointerInput(Unit) {
                    // Watches the finger without taking it: taps, drags and long presses still
                    // belong to whatever the pane is part of.
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        ownLight.press(down.position)
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            ownLight.at = change.position
                        }
                        ownLight.release()
                    }
                }
            } else {
                Modifier
            }
        )
        .then(
            if (shadow && look.opacity > 0f) {
                Modifier.drawWithCache {
                    val outline = shape.createOutline(size, layoutDirection, this)
                    val path = android.graphics.Path().apply { addOutline(outline) }
                    val shadows = shadowPaints(look, material, dark, density, size)
                    val glow = if (wallTinted) android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG) else null
                    onDrawBehind {
                        // The glow is the wallpaper's colour under the pane, so it is found as the pane
                        // is drawn: where the pane is and how the wallpaper is scrolled are read here.
                        val wall = if (glow != null) wallUnder(levels, origin, size, screen, wallpaperOffset.value, savedWall) else Color.Unspecified
                        drawIntoCanvas {
                            val canvas = it.nativeCanvas
                            canvas.save()
                            // Only the shadow is wanted: the pane itself is drawn by the glass.
                            canvas.clipOutPath(path)
                            // Beneath the shadows, so the close one still shows under the pane's edge.
                            if (glow != null && wall.isSpecified) {
                                glow.glowWith(glowColour(wall, dark), look.opacity, density)
                                canvas.drawPath(path, glow)
                            }
                            for (paint in shadows) canvas.drawPath(path, paint)
                            canvas.restore()
                        }
                    }
                }
            } else {
                Modifier
            }
        )
        .clip(shape)
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            // A glass tile under the iPhone's shape gets the iPhone's rim, not the squircle's.
            val squircleN = squircleExponent(shape)
            onDrawBehind {
                // Position, wallpaper scroll, tilt and the finger's light are all read here, while
                // drawing, so any of them changing repaints the glass without recomposing anything.
                // Where the pane was laid out is read every time, even by a pane that works out where
                // it is drawn: a layer further out, such as the home screen's pull or its zoom, moves
                // it without its motion or tracking knowing, and the new position is what redraws it.
                // So too a pane being laid out somewhere new, as an icon gliding to another cell is:
                // it is drawn again there.
                val laidOut = origin
                val touch = press?.touch(size) ?: GlassTouch.None
                // A pressed pane bends the light a little more, as the finger's light comes up.
                val lit = if (touch.strength > 0f) look.copy(refraction = look.refraction * (1f + 0.22f * touch.strength)) else look
                // Glass made from the wallpaper: the lens reads the colour under each of its points
                // for itself, and without the lens the pane takes the colour under its middle.
                val wall = if (wallTinted) wallUnder(levels, laidOut, size, screen, wallpaperOffset.value, savedWall) else Color.Unspecified
                drawLiquidGlass(
                    paneSize = size,
                    outline = outline,
                    lens = lens,
                    softened = backdrop,
                    origin = if (moved != null || follows != null) drawnOrigin(placed[0], moved, follows) ?: laidOut else laidOut,
                    screen = screen,
                    wallpaperOffset = wallpaperOffset.value,
                    look = lit,
                    lean = tilt.value,
                    dark = dark,
                    touch = touch,
                    tuning = tuning,
                    squircleN = squircleN,
                    wall = wall,
                    ring = glassRing?.drawn(),
                    dim = wallpaperDim.value,
                )
            }
        }
}

/**
 * Where a pane that something is moving is drawn on screen, worked out while drawing. A graphicsLayer
 * moves a pane without laying it out again, so where it was laid out is not where it is; this asks
 * the layout for where its middle is now, so the wallpaper stays put behind a pane that leans or is
 * dragged. A pane that swells is a lens that swells: what is behind it grows about its middle.
 *
 * Reads [moved] and [tracking] so that whatever moves the pane redraws it as it goes.
 */
private fun DrawScope.drawnOrigin(placed: LayoutCoordinates?, moved: PressMotion?, tracking: (() -> Unit)?): Offset? {
    moved?.watch()
    tracking?.invoke()
    if (placed == null || !placed.isAttached) return null
    return placed.localToWindow(center) - center
}

/**
 * The light a finger puts into a pane. It follows the finger, comes up quickly, and spreads out
 * from under the fingertip while held; let go and it fades away slowly.
 *
 * A pane lights itself when it is `interactive`; one its own finger cannot reach, under the words it
 * carries, is given one of these to drive (`glass(light = ...)`).
 */
@Stable
class PressLight internal constructor(private val scope: CoroutineScope) {
    internal var at by mutableStateOf(Offset.Zero)
    private val strength = Animatable(0f)
    private val spread = Animatable(0f)

    internal fun press(position: Offset) {
        at = position
        scope.launch { strength.animateTo(1f, tween(140)) }
        scope.launch {
            spread.snapTo(0f)
            spread.animateTo(1f, tween(700, easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)))
        }
    }

    internal fun release() {
        scope.launch { strength.animateTo(0f, tween(560)) }
    }

    /** Read while drawing, so the glow animates without recomposing. */
    internal fun touch(pane: Size): GlassTouch {
        val level = strength.value
        if (level <= 0.001f) return GlassTouch.None
        val reach = max(pane.width, pane.height) * (0.1f + 0.32f * spread.value)
        return GlassTouch(at, reach, level)
    }
}

/**
 * Apple's glass floats on a soft shadow, and that is all the shadow it has: no dark rim under its
 * edge, nothing that makes it a heavy thing sitting on the picture. See [glassShadows] for what it
 * is; these are the paints that cast it, black, each drawn under the pane's own outline.
 */
internal fun shadowPaints(look: GlassLook, material: GlassMaterial, dark: Boolean, density: Float, pane: Size): List<android.graphics.Paint> =
    glassShadows(material, dark, glassThickness(pane, density)).map { it.paint(density, look.opacity) }

/** One shadow under a pane: how soft it is and how far it falls, in dp, and how dark at most. */
internal data class GlassShadow(val blurDp: Float, val dropDp: Float, val alpha: Float)

/** The colour of this shadow at its darkest: black, as dark as the shadow is, times how much of the pane is there. */
internal fun GlassShadow.colour(opacity: Float = 1f): Color = Color.Black.copy(alpha = (alpha * opacity).coerceIn(0f, 1f))

/** The paint that casts this shadow, black, drawn under the pane's outline with the pane clipped out. */
internal fun GlassShadow.paint(density: Float, opacity: Float = 1f): android.graphics.Paint {
    // Worked out here, not inside the paint's apply, where alpha would be the paint's own.
    val shadow = colour(opacity).toArgb()
    val blur = blurDp * density
    val drop = dropDp * density
    return android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        setShadowLayer(blur, 0f, drop, shadow)
    }
}

/**
 * The shadows a pane of glass casts, as iOS's glass does: one wide and faint, falling straight down,
 * and wider, further and a little deeper for a thicker slab ([glassThickness]: a bigger pane floats
 * higher); deeper in the dark, where it has to show against a dark picture, and lighter for Clear,
 * which floats over the picture with the least between them. Under it a whisper of a close one, only
 * enough to draw the outline where the picture is as pale as the glass. Glass on the wallpaper casts
 * Dark's in Light as well: the picture they fall on is the same in both.
 */
internal fun glassShadows(material: GlassMaterial, dark: Boolean, thick: Float): List<GlassShadow> {
    val t = thick.coerceIn(0f, 1f)
    val most = (if (dark) SHADOW_DARK else SHADOW_LIGHT) * (if (material == GlassMaterial.Clear) 0.8f else 1f)
    return listOf(
        GlassShadow(blurDp = 10f + 14f * t, dropDp = 2f + 5f * t, alpha = most * (0.65f + 0.35f * t)),
        GlassShadow(blurDp = 1.5f, dropDp = 0.5f, alpha = if (dark) 0.06f else 0.05f),
    )
}

/** How dark a pane's wide shadow is at its darkest, for the thickest glass, in the dark and the light. */
internal const val SHADOW_DARK = 0.16f
internal const val SHADOW_LIGHT = 0.10f

/**
 * Makes this the paint under glass made from the wallpaper: a soft light of [colour] ([glowColour])
 * spilling out under the pane, close and gentle, as the colour of the picture under glass bleeds
 * into its shadow, fading with the pane as [opacity] says. It is set again each time the pane is
 * drawn, since its colour is the wallpaper's under the pane and changes as the pane moves over it.
 */
internal fun android.graphics.Paint.glowWith(colour: Color, opacity: Float, density: Float) {
    color = android.graphics.Color.BLACK
    setShadowLayer(
        GLOW_BLUR_DP * density,
        0f,
        GLOW_FALL_DP * density,
        colour.copy(alpha = (colour.alpha * opacity * GLOW_SHARE).coerceIn(0f, 1f)).toArgb(),
    )
}

/** How softly the glow under glass made from the wallpaper is spread, how far it falls, in dp, and how much of [glowColour]'s strength it has. */
private const val GLOW_BLUR_DP = 16f
private const val GLOW_FALL_DP = 4f
private const val GLOW_SHARE = 0.55f

/**
 * How thick a slab a pane is, 0 to 1, by its shorter side: a small control, 28 dp or less, is thin
 * glass, and from 120 dp up (a widget, a sheet) it is as thick as glass gets; the dock, 92 dp high,
 * is most of the way. A thicker slab bends harder at its rim and floats higher on its shadow.
 */
internal fun glassThickness(pane: Size, density: Float): Float {
    val shortest = min(pane.width, pane.height) / density.coerceAtLeast(0.01f)
    return ((shortest - THIN_GLASS_DP) / (THICK_GLASS_DP - THIN_GLASS_DP)).coerceIn(0f, 1f)
}

private const val THIN_GLASS_DP = 28f
private const val THICK_GLASS_DP = 120f

/**
 * How wide the band round a pane's rim is where the glass curves away and bends what is behind it,
 * in pixels; inside it the pane is flat. A quarter of the pane's shorter side, from 5 dp, and at most
 * 12 dp on thin glass up to 22 dp on the thickest. Never more than half the pane, and for an outline
 * with round corners ([rounded], a rounded outline's) never more than its tightest round corner, past
 * which the band would crease at the corner.
 */
internal fun lensBand(pane: Size, density: Float, rounded: RoundRect? = null): Float {
    val shortest = min(pane.width, pane.height)
    val most = (LENS_BAND_THIN_DP + (LENS_BAND_THICK_DP - LENS_BAND_THIN_DP) * glassThickness(pane, density)) * density
    var band = (shortest * 0.25f).coerceIn(min(LENS_BAND_LEAST_DP * density, most), most)
    if (rounded != null) {
        val corners = listOf(rounded.topLeftCornerRadius.x, rounded.topRightCornerRadius.x, rounded.bottomRightCornerRadius.x, rounded.bottomLeftCornerRadius.x)
        corners.filter { it > 0f }.minOrNull()?.let { band = min(band, it) }
    }
    return min(band, shortest * 0.5f).coerceAtLeast(1f)
}

private const val LENS_BAND_LEAST_DP = 5f
private const val LENS_BAND_THIN_DP = 12f
private const val LENS_BAND_THICK_DP = 22f

internal fun android.graphics.Path.addOutline(outline: Outline) {
    when (outline) {
        is Outline.Rectangle -> addRect(
            outline.rect.left, outline.rect.top, outline.rect.right, outline.rect.bottom,
            android.graphics.Path.Direction.CW,
        )
        is Outline.Rounded -> {
            val r = outline.roundRect
            addRoundRect(
                r.left, r.top, r.right, r.bottom,
                floatArrayOf(
                    r.topLeftCornerRadius.x, r.topLeftCornerRadius.y,
                    r.topRightCornerRadius.x, r.topRightCornerRadius.y,
                    r.bottomRightCornerRadius.x, r.bottomRightCornerRadius.y,
                    r.bottomLeftCornerRadius.x, r.bottomLeftCornerRadius.y,
                ),
                android.graphics.Path.Direction.CW,
            )
        }
        is Outline.Generic -> addPath(outline.path.asAndroidPath())
    }
}

/**
 * How much Android enlarges a still wallpaper beyond the screen. From Android 14 it zooms a
 * screen-sized wallpaper in by a tenth, so there is width to spare for scrolling with the pages;
 * measured on a Pixel image: grid lines 72px apart in the picture sit 79.2px apart on screen.
 */
internal val SYSTEM_WALLPAPER_ZOOM: Float = if (Build.VERSION.SDK_INT >= 34) 1.1f else 1f

/**
 * Where across the wallpaper Android is showing. From Android 14 a still wallpaper stays centred
 * whatever page you are on (the launcher's offsets are received but not used for it: measured,
 * mWallpaperX 1.0 on the last page and the picture did not move), so the glass stays centred too.
 * Before that, the launcher's offsets slide it, as they always did.
 */
internal fun systemWallpaperOffset(launcher: Offset): Offset =
    if (Build.VERSION.SDK_INT >= 34) Offset(0.5f, 0.5f) else launcher

/**
 * A lens of its own for [levels], rebuilt only when the wallpaper itself changes: what [ZenithTheme]
 * makes the shared one with, and what a pane falls back to where none was made (see [sharedLens]).
 */
@Composable
internal fun rememberLens(levels: GlassLevels?): LiquidGlassShader? =
    remember(levels) {
        if (Build.VERSION.SDK_INT >= 33 && levels != null) {
            // A GPU that cannot compile it still gets the Android 12 glass below.
            runCatching { LiquidGlassShader(levels) }.getOrNull()
        } else {
            null
        }
    }

/**
 * The lens for [levels]: the one [ZenithTheme] made for them ([LocalLens]), which every pane shares,
 * so a page of panes coming into view, or a new wallpaper, compiles the shader once and not once a
 * pane. Where none was made for these levels (a preview, PinItemActivity, the island), the pane
 * makes its own, as every pane once did.
 */
@Composable
internal fun sharedLens(levels: GlassLevels?): LiquidGlassShader? {
    val shared = LocalLens.current
    // Before Android 13 there is no lens, shared or not, and nothing to ask what one was made for.
    val made = Build.VERSION.SDK_INT >= 33 && levels != null && shared != null && shared.levels === levels
    return if (made) shared else rememberLens(levels)
}

/** The recipe for one pane. Kept apart from drawing so the launch card can use the same glass. */
internal fun glassLook(
    material: GlassMaterial,
    strength: Float,
    solid: Float,
    opacity: Float,
    dark: Boolean,
    tint: Color,
    border: Boolean = true,
    /** How much body the person asked the glass to have. Clear is the glass with nothing added. */
    depth: GlassBody = GlassBody.Clear,
    /** Coloured glass in [tint]: see `glass(tinted = ...)`. */
    tinted: Boolean = false,
    /**
     * Glass standing on the wallpaper ([LocalOnWallpaper]): made with Dark's numbers in Light as
     * well, under a neutral grey veil, so it is the same clear glass in both.
     */
    onWallpaper: Boolean = false,
): GlassLook {
    val clear = material == GlassMaterial.Clear
    val amount = strength.coerceIn(0f, 1.4f)
    // The wallpaper does not change with the mode, so neither does the glass standing on it.
    val toneDark = dark || onWallpaper
    if (tinted) {
        // Clear glass's bend under a coat of the accent, about three fifths of it, so the wallpaper
        // still shows through, its tone following how bright the picture is behind, and bends at
        // the rim, as iOS's tinted glass does. The colour already carries the words on it, so the
        // veil hardly needs to thicken.
        return GlassLook(
            refraction = 0.35f + 0.65f * min(amount, 1f),
            frost = FROST_TINTED,
            dispersion = 0.04f * min(amount, 1f),
            shine = if (border) shineFor(amount) else 0f,
            vibrance = VIBRANCE_CLEAR,
            wash = tint.copy(alpha = 0.62f),
            adapt = 0.10f,
            opacity = opacity.coerceIn(0f, 1f),
            glow = if (border) GLOW else 0f,
            rimmed = border,
            tinted = true,
            onWallpaper = onWallpaper,
        )
    }
    val body = (solid + depth.solid).coerceIn(0f, 1.5f)
    // A whisper of the tint in the veil, never enough to turn the glass into a coloured card. On the
    // wallpaper the veil is a neutral grey, neither the white that made Light's glass milky nor the
    // near-black that made Dark's smoky.
    val veil = lerp(if (onWallpaper) WALLPAPER_VEIL else if (toneDark) Color(0xFF0A0C12) else Color.White, tint, 0.10f)
    val veilAlpha = if (toneDark) {
        (if (clear) 0.08f else 0.20f) + 0.42f * body
    } else {
        (if (clear) 0.12f else 0.30f) + 0.40f * body
    }
    return GlassLook(
        // Clear glass bends hardest; Regular a little less, its frost already softening the edge.
        refraction = (if (clear) 1f else 0.8f) * (0.35f + 0.65f * min(amount, 1f)),
        // Clear glass barely mists what is behind it; Regular frosts it, as iOS's glass under words
        // does, and a pane with more body, like a sheet, more still.
        frost = ((if (clear) FROST_CLEAR else FROST_REGULAR) + 0.8f * body).coerceIn(0f, 3f),
        // Only clear glass splits light into colour at its rim: through frost the split is lost,
        // and it costs the shader three looks at the wallpaper where one will do.
        dispersion = if (clear) 0.06f * min(amount, 1f) else 0f,
        // A lifted pane gets a slightly surer rim too: the edge is what is read first.
        shine = if (border) shineFor(amount) + 0.06f * depth.lift else 0f,
        vibrance = if (clear) VIBRANCE_CLEAR else VIBRANCE_REGULAR,
        wash = veil.copy(alpha = veilAlpha.coerceIn(0f, 0.92f)),
        adapt = (if (clear) 0.30f else 0.34f) + 0.2f * min(body, 1f),
        opacity = opacity.coerceIn(0f, 1f),
        lift = glassLift(clear, toneDark, tint, body, depth.lift),
        glow = if (border) GLOW else 0f,
        rimmed = border,
        onWallpaper = onWallpaper,
    )
}

/** How brightly a pane's edge catches the light, for glass turned up to [amount]: never none, so the edge always reads. */
internal fun shineFor(amount: Float): Float = SHINE_LEAST + SHINE_RANGE * min(amount.coerceAtLeast(0f), 1.2f)

private const val SHINE_LEAST = 0.55f
private const val SHINE_RANGE = 0.3f

/**
 * The ink for words on tinted glass or a pill of the accent: dark on anything lighter than a mid
 * tone, white on a deep colour. White on Sunset's orange, or dark on violet, was hard to read.
 */
fun inkOnTint(fill: Color): Color = if (fill.luminance() > 0.3f) Color(0xFF0B0C10) else Color.White

/** The Glass studio's tuning, for every pane: Zenith's own glass with the defaults, and without Plus. */
val LocalGlassTuning = staticCompositionLocalOf { GlassTuning() }

/**
 * True for glass standing on the wallpaper: everything on a home page and on the small home screens
 * that show one, the dock's slab, the Search pill. That glass is one recipe in Light and in Dark,
 * since the picture under it does not change with the mode; glass on a page or a sheet keeps a body
 * that follows the mode.
 */
val LocalOnWallpaper = staticCompositionLocalOf { false }

/**
 * What moves the panes here with a layer, read while drawing, as a pane's own `tracking` is, so the
 * wallpaper behind each stays where it is on screen while the pane moves. Given by [GlassMovedBy];
 * null where nothing moves them.
 */
internal val LocalGlassTracking = staticCompositionLocalOf<(() -> Unit)?> { null }

/**
 * [content] standing on something a layer moves, as the tilt floats the icons and widgets on a home
 * page and the dock, [moves] reading whatever moves it: every pane of glass in it reads [moves] while
 * drawing and lines its wallpaper up again as it goes, as a pane given `glass(tracking = ...)` does.
 * Inside another it follows both. [moves] is best remembered, since a new one composes all of
 * [content] again.
 */
@Composable
fun GlassMovedBy(moves: () -> Unit, content: @Composable () -> Unit) {
    val outer = LocalGlassTracking.current
    val both = remember(outer, moves) { if (outer == null) moves else ({ outer(); moves() }) }
    CompositionLocalProvider(LocalGlassTracking provides both, content = content)
}

/**
 * The veil of glass on the wallpaper, in Light and in Dark alike: a grey with no colour in it, and
 * its body where there is no wallpaper to show.
 */
internal val WALLPAPER_VEIL = Color(0xFF3A3A3C)

/**
 * This pane as the Glass studio has it; with the defaults, exactly as it was. The studio's old
 * "Theme in the glass" ([GlassTuning.tint]) is still read and saved, but no longer reaches the
 * veil: there is no theme left to put in it.
 */
internal fun GlassLook.tuned(tuning: GlassTuning): GlassLook {
    if (tuning.isDefault) return this
    return copy(
        refraction = refraction * tuning.bend,
        frost = (frost + tuning.frost).coerceIn(0f, 3f),
        dispersion = dispersion * tuning.rainbow,
        shine = shine * tuning.rim,
    )
}

/** How far Zenith's own light is set from a pane: the length it has always had, so tipping the phone swings it as far. */
private val ZENITH_LIGHT_REACH = hypot(0.45f, 1f)

/**
 * Where the light falls on a pane from, the phone leaning by [lean]: up and to the left, on the
 * diagonal, as iOS's light is, so the corner nearest it and the one opposite catch it and the long
 * sides between hardly do; or from where the Glass studio put it. It swings the other way as the
 * phone tips, unless it is stilled.
 */
internal fun lightFrom(lean: Offset, tuning: GlassTuning): Offset {
    val swing = if (tuning.lightTilt) lean else Offset.Zero
    val radians = Math.toRadians((tuning.lightAngle ?: GlassTuning.ZENITH_LIGHT).toDouble())
    val base = Offset((cos(radians) * ZENITH_LIGHT_REACH).toFloat(), (sin(radians) * ZENITH_LIGHT_REACH).toFloat())
    return Offset(base.x - 1.6f * swing.x, base.y - 1.2f * swing.y)
}

/** How soft the wallpaper is through the face of a pane with no extra body: see [GlassLook.frost]. */
internal const val FROST_CLEAR = 0.7f
internal const val FROST_REGULAR = 2.0f
internal const val FROST_TINTED = 0.6f

/** How much richer the colour behind is through the glass: see [GlassLook.vibrance]. */
internal const val VIBRANCE_CLEAR = 1.3f
internal const val VIBRANCE_REGULAR = 1.45f

/** How far the light on the glass takes the colour of the wallpaper round it: see [GlassLook.glow]. */
internal const val GLOW = 0.25f

/**
 * The frost that lifts a pane off a picture of its own tone. In the dark a pane over a dark
 * wallpaper is a dark veil on a dark picture, and only its rim says it is there; a little pale
 * frost, in the pane's tint, makes it a shade lighter than what is round it. In the light it is
 * the other way about: over a near-white picture the pane takes a faint cool shade. Glass on the
 * wallpaper takes the dark one in Light as well, and over a pale picture its veil thickens instead.
 * The colour is the frost; its alpha is how much of it, and the shader lays it on only where the
 * picture behind really is that dark or that pale, so glass over anything else stays as clear as it was.
 *
 * A pane that already has a full veil, like a sheet, needs less of it.
 */
internal fun glassLift(clear: Boolean, dark: Boolean, tint: Color, body: Float, lift: Float): Color {
    val tone = if (dark) lerp(Color.White, tint, 0.22f) else lerp(Color(0xFF5E6878), tint, 0.18f)
    val most = if (dark) (if (clear) LIFT_DARK_CLEAR else LIFT_DARK) else (if (clear) LIFT_LIGHT_CLEAR else LIFT_LIGHT)
    val amount = most * lift.coerceIn(0f, 1f) * (1f - 0.5f * body.coerceIn(0f, 1f))
    return tone.copy(alpha = amount)
}

internal const val LIFT_DARK = 0.115f
internal const val LIFT_DARK_CLEAR = 0.085f
internal const val LIFT_LIGHT = 0.14f
internal const val LIFT_LIGHT_CLEAR = 0.10f

/**
 * Paints one pane of glass into [paneSize] at the top-left of the current draw scope. [origin] is
 * where that top-left sits on screen, which is what lines the wallpaper up behind it.
 */
internal fun DrawScope.drawLiquidGlass(
    paneSize: Size,
    outline: Outline,
    lens: LiquidGlassShader?,
    softened: ImageBitmap?,
    origin: Offset,
    screen: IntSize,
    wallpaperOffset: Offset,
    look: GlassLook,
    lean: Offset,
    dark: Boolean,
    touch: GlassTouch = GlassTouch.None,
    /** Where the light is, if the Glass studio has moved it or stilled it. */
    tuning: GlassTuning = GlassTuning(),
    /**
     * For a free-form [outline], the superellipse the lens draws it as: see [squircleExponent].
     * Without the lens the rim is stroked along the outline itself, so it needs no telling.
     */
    squircleN: Float = SQUIRCLE_EXPONENT,
    /**
     * For glass made from the wallpaper ([GlassLook.wallTint]) drawn without the lens: the colour
     * its veil takes, one for the whole pane ([wallUnder]). Unspecified for none; the lens reads the
     * wallpaper's colour under each point itself.
     */
    wall: Color = Color.Unspecified,
    /**
     * A ring of light from a tap on the wallpaper ([GlassRing]), its centre where the screen has it,
     * or null for none. The lens lights the band where it crosses the pane; without the lens the
     * rim brightens as the ring passes the pane's middle ([paneLight]).
     */
    ring: RingNow? = null,
    /**
     * How much darker the wallpaper is drawn under the pane ([LocalWallpaperDim]), so what the
     * glass shows of it is as dark as what is round it. Glass with no copy of the wallpaper shows
     * the veil itself and needs no telling.
     */
    dim: Float = 0f,
) {
    if (paneSize.width <= 0f || paneSize.height <= 0f || look.opacity <= 0f) return
    // The light sits up and to the left, and swings the other way as the phone tips.
    val light = lightFrom(lean, tuning)
    val canSee = softened != null && screen.width > 0 && screen.height > 0
    val shown = systemWallpaperOffset(wallpaperOffset)
    // The ring in the pane's own pixels, as everything about the pane is drawn.
    val ringHere = ring?.let { it.copy(centre = it.centre - origin) }

    if (canSee && lens != null && Build.VERSION.SDK_INT >= 33) {
        drawIntoCanvas {
            lens.draw(it.nativeCanvas, paneSize, outline, origin, screen, shown, density, look, light, dark, touch, squircleN, ringHere, dim)
        }
        return
    }

    if (canSee && softened != null) {
        // Android 12: the softened wallpaper, lined up with the real one, under the same veil.
        drawSoftenedWallpaper(softened, origin, screen, shown, paneSize, look.opacity)
        // The veil the home screen lays over the wallpaper, when it is dimmed, on the copy too.
        if (dim > 0f) drawRect(Color.Black, size = paneSize, alpha = dim.coerceAtMost(1f) * look.opacity)
        // Glass made from the wallpaper takes a little of the colour under its middle into the veil.
        drawRect(tintMix(look.wash, wall, look.wallTint), size = paneSize, alpha = look.opacity)
        // No shader to see how dark the picture is under each point, so the lift goes on evenly
        // and a little thinner.
        if (look.lift.alpha > 0f) {
            drawRect(look.lift.copy(alpha = look.lift.alpha * 0.7f), size = paneSize, alpha = look.opacity)
        }
    } else {
        // Nothing readable behind (a live wallpaper, or setup): a frosted body of its own, lit
        // from the top, so the pane still reads as glass rather than a flat card.
        val overWallpaper = look.onWallpaper && !look.tinted
        if (look.tinted) {
            // Tinted glass keeps its colour, a little fuller, rather than turning into a white slab.
            drawRect(look.wash.copy(alpha = 0.88f), size = paneSize, alpha = look.opacity)
        } else if (overWallpaper) {
            // On the wallpaper, one thin neutral body in Light and in Dark: glass over a picture,
            // never the white slab Light made of it or the smoke Dark did. Glass made from the
            // wallpaper takes a little of the colour last read from it, if any was.
            val body = WALLPAPER_VEIL.copy(alpha = (0.14f + look.wash.alpha).coerceAtMost(0.6f))
            drawRect(tintMix(body, wall, look.wallTint), size = paneSize, alpha = look.opacity)
        } else {
            val base = if (dark) Color(0xFF181B24) else Color.White
            // Light for floating controls, much fuller for sheets, whose text sits over the page.
            val bodyAlpha = (if (dark) 0.22f else 0.28f) + 1.3f * look.wash.alpha
            drawRect(base.copy(alpha = bodyAlpha.coerceAtMost(0.94f)), size = paneSize, alpha = look.opacity)
        }
        drawRect(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = if (overWallpaper) 0.16f else if (dark) 0.10f else 0.30f),
                0.5f to Color.Transparent,
                endY = paneSize.height,
            ),
            size = paneSize,
            alpha = look.opacity,
        )
    }
    if (touch.strength > 0f) {
        // The finger's light, as the shader draws it: brightest under the fingertip.
        drawRect(
            Brush.radialGradient(
                0f to Color.White.copy(alpha = 0.16f * touch.strength),
                1f to Color.Transparent,
                center = touch.at,
                radius = touch.reach.coerceAtLeast(1f),
            ),
            size = paneSize,
        )
    }
    // Without the lens there is one light for the whole pane: as bright as the ring is where the
    // pane's middle is.
    val ringLight = ringHere?.let { paneLight(hypot(it.centre.x - paneSize.width / 2f, it.centre.y - paneSize.height / 2f), it) } ?: 0f
    drawRim(paneSize, outline, look, light, dark, ringLight)
}

/**
 * The softened wallpaper behind a pane, lined up with the real one: where a pane at [origin] in a
 * window [screen] big sees it, Android having scrolled it to [shown]. What glass without its lens
 * shows, and what Neon's smoke is laid over.
 */
internal fun DrawScope.drawSoftenedWallpaper(softened: ImageBitmap, origin: Offset, screen: IntSize, shown: Offset, paneSize: Size, alpha: Float) {
    val scale = max(screen.width / softened.width.toFloat(), screen.height / softened.height.toFloat()) *
        SYSTEM_WALLPAPER_ZOOM
    val width = (softened.width * scale).roundToInt()
    val height = (softened.height * scale).roundToInt()
    val left = (-origin.x - (width - screen.width) * shown.x).roundToInt()
    val top = (-origin.y - (height - screen.height) * shown.y).roundToInt()
    drawImage(
        image = softened,
        dstOffset = IntOffset(left, top),
        dstSize = IntSize(width, height),
        alpha = alpha,
        filterQuality = FilterQuality.Low,
    )
    // A pane inside something that is still growing is larger than the room it has on screen,
    // and can reach past the right or the foot of the picture as it is lined up. The picture's
    // last column and last row are drawn out over what that would leave bare, which is what
    // the shader does at its edges too.
    val beyondRight = ceil(paneSize.width).toInt() - (left + width)
    if (beyondRight > 0) {
        drawImage(
            image = softened,
            srcOffset = IntOffset(softened.width - 1, 0),
            srcSize = IntSize(1, softened.height),
            dstOffset = IntOffset(left + width, top),
            dstSize = IntSize(beyondRight, height),
            alpha = alpha,
            filterQuality = FilterQuality.Low,
        )
    }
    val beyondFoot = ceil(paneSize.height).toInt() - (top + height)
    if (beyondFoot > 0) {
        drawImage(
            image = softened,
            srcOffset = IntOffset(0, softened.height - 1),
            srcSize = IntSize(softened.width, 1),
            dstOffset = IntOffset(left, top + height),
            dstSize = IntSize(width + beyondRight.coerceAtLeast(0), beyondFoot),
            alpha = alpha,
            filterQuality = FilterQuality.Low,
        )
    }
}

/**
 * The edge for the non-shader paths, as near the shader's as strokes can come: a hairline of light
 * round the outline, brightest on the corner facing the light and on the corner opposite and faint
 * between, with a soft light just inside it. Each stroke straddles the outline and the pane's own
 * clip keeps the inner half, so a narrow one and two wider, fainter ones laid over each other fade
 * inwards. No shading of a slab: glass is clear, and its edge is what shows it.
 *
 * A ring of light from a tap on the wallpaper passing the pane, [ringLight] of it, brightens the
 * edge all the way round, since it comes from no one side.
 */
private fun DrawScope.drawRim(
    paneSize: Size,
    outline: Outline,
    look: GlassLook,
    light: Offset,
    dark: Boolean,
    ringLight: Float = 0f,
    hairline: Float = 1.6.dp.toPx(),
) {
    val centre = Offset(paneSize.width / 2f, paneSize.height / 2f)
    val length = kotlin.math.hypot(light.x, light.y).coerceAtLeast(0.001f)
    val towards = Offset(light.x / length, light.y / length)
    // Where each part of the pane stands to the light, as a line across it from its lit corner
    // to its far one. The line is stretched to the pane's own shape, so the corner nearest the lamp
    // is lit on a wide bar as on a square, as the shader lights it.
    val across = Offset(towards.x / (paneSize.width / 2f), towards.y / (paneSize.height / 2f))
    val span = (kotlin.math.abs(towards.x) + kotlin.math.abs(towards.y)) / (across.x * across.x + across.y * across.y)
    val lamp = centre + across * span
    val far = centre - across * span
    // A small pane gets narrower strokes, or it would be all edge and no face.
    val widest = min(paneSize.width, paneSize.height) * 0.2f

    if (look.shine > 0f) {
        // In the light the pane is pale already, and the same light would whiten its edge.
        val strength = look.shine * look.opacity * (if (dark) 1f else 0.75f)
        fun lit(amount: Float) = Brush.linearGradient(
            0f to Color.White.copy(alpha = (1.05f * amount).coerceAtMost(1f)),
            0.3f to Color.White.copy(alpha = (0.2f * amount).coerceAtMost(1f)),
            0.7f to Color.White.copy(alpha = (0.2f * amount).coerceAtMost(1f)),
            1f to Color.White.copy(alpha = (0.75f * amount).coerceAtMost(1f)),
            start = lamp,
            end = far,
        )
        val sheen = lit(0.18f * strength)
        for (width in RIM_GLOW_WIDTHS) drawOutline(outline, sheen, style = Stroke(min(width.dp.toPx(), widest)))
        drawOutline(outline, lit(strength), style = Stroke(hairline))
    }

    val ring = ringLight.coerceIn(0f, 1f) * look.opacity
    if (ring > 0.004f) {
        val ringGlow = Color.White.copy(alpha = RING_RIM_GLOW * ring)
        for (width in RIM_GLOW_WIDTHS) drawOutline(outline, ringGlow, style = Stroke(min(width.dp.toPx(), widest)))
        drawOutline(outline, Color.White.copy(alpha = RING_RIM * ring), style = Stroke(hairline))
    }
}

/** How bright a passing ring of light makes the rim, and the light just inside it, at its brightest. */
private const val RING_RIM = 0.85f
private const val RING_RIM_GLOW = 0.10f

/** The strokes that make the soft light inside the rim, in dp; the pane's clip shows the inner half of each. */
private val RIM_GLOW_WIDTHS = floatArrayOf(7f, 4f)

/**
 * Glass lying in glass, or in the drawer: an app's tile in the drawer or in a folder when icons are
 * glass. A pane of its own there would show the wallpaper through a hole in what holds it, so it is
 * drawn from what it can know without seeing behind itself, as Apple's glass on glass looks: a frost
 * that lifts it a step from what it lies in ([onGlassFrost]), the same hairline of light as every
 * pane, with the soft light just inside it, from the same lamp, moving as the phone tilts, a soft
 * shadow straight down, lower than a pane's, as it floats just off what it lies in ([onGlassShadow]),
 * and on a pale page a soft grey band round its rim, where it bends ([onGlassBand]). [dark] is
 * whether what it lies in is dark; an [interactive] tile lights up from within under a finger, as a
 * pane does.
 */
@Composable
fun Modifier.glassOnGlass(shape: Shape, dark: Boolean, interactive: Boolean = false): Modifier {
    val theme = LocalTheme.current
    if (!theme.isGlass) return themedOnPanel(theme, shape, interactive)
    val tilt = LocalTilt.current
    val tuning = LocalGlassTuning.current
    val amount = (LocalGlassStrength.current * ON_GLASS_STRENGTH).coerceIn(0f, 1.4f)
    val look = onGlassLook(dark, amount).tuned(tuning)
    val scope = rememberCoroutineScope()
    val press = if (interactive) remember { PressLight(scope) } else null
    return this
        .then(
            if (press != null) {
                Modifier.pointerInput(Unit) {
                    // Watches the finger without taking it, as a pane does: the tap is the tile's.
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        press.press(down.position)
                        while (true) {
                            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                            if (!change.pressed) break
                            press.at = change.position
                        }
                        press.release()
                    }
                }
            } else {
                Modifier
            }
        )
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val path = android.graphics.Path().apply { addOutline(outline) }
            val shadow = onGlassShadow(dark, min(size.width, size.height) / density).paint(density)
            onDrawBehind {
                drawIntoCanvas {
                    val canvas = it.nativeCanvas
                    canvas.save()
                    // Only the shadow: the tile is see-through, and drawn over it.
                    canvas.clipOutPath(path)
                    canvas.drawPath(path, shadow)
                    canvas.restore()
                }
            }
        }
        .clip(shape)
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val band = onGlassBand(dark, min(size.width, size.height) / density)?.let {
                GlassBand(android.graphics.Path().apply { addOutline(outline) }, it.bandPaint(density))
            }
            onDrawBehind {
                // The tilt and the finger are read here, while drawing, as a pane reads them.
                drawGlassOnGlass(size, outline, look, lightFrom(tilt.value, tuning), dark, press?.touch(size) ?: GlassTouch.None, band)
            }
        }
}

/** The band round a tile's rim, ready to draw: its outline and the soft grey stroke laid on it. */
internal class GlassBand(val outline: android.graphics.Path, val paint: android.graphics.Paint)

/**
 * The paint for the band round a rim: a stroke as wide as the band, in its grey, blurred as wide
 * again, laid on the outline; the tile's clip keeps the half inside it, fading inwards from the edge.
 */
internal fun GlassShadow.bandPaint(density: Float): android.graphics.Paint {
    // Worked out here, not inside the paint's apply, where alpha would be the paint's own.
    val ink = colour().toArgb()
    val width = blurDp * density
    return android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = width
        color = ink
        if (width > 0f) maskFilter = android.graphics.BlurMaskFilter(width, android.graphics.BlurMaskFilter.Blur.NORMAL)
    }
}

/** How glass lying in glass catches the light: as Clear glass of the same strength does. */
internal fun onGlassLook(dark: Boolean, strength: Float): GlassLook =
    glassLook(GlassMaterial.Clear, strength, solid = 0f, opacity = 1f, dark = dark, tint = Color.White)

/**
 * How much white frost lifts glass lying in glass from what it lies in: a little over something
 * dark, and more over something pale, where white glass would otherwise not be there at all.
 */
internal fun onGlassFrost(dark: Boolean): Float = if (dark) ON_GLASS_FROST_DARK else ON_GLASS_FROST_LIGHT

/**
 * The shadow glass lying in glass casts, [sideDp] being its shorter side: as a pane's wide one, soft
 * and faint and falling straight down, but lower, as it floats just off what it lies in. Its softness
 * and its fall go by its size, so a folder's tiny tile, a miniature of the drawer's, has a
 * miniature's shadow; deeper in the dark, where it has to show against a dark page. It has no close
 * one to draw its edge, as a pane has: on a pale page that would ink a line round it, and the band
 * round its rim ([onGlassBand]) shows where it is instead.
 */
internal fun onGlassShadow(dark: Boolean, sideDp: Float): GlassShadow {
    val side = sideDp.coerceAtLeast(0f)
    return GlassShadow(
        blurDp = side * ON_GLASS_SHADOW_BLUR,
        dropDp = side * ON_GLASS_SHADOW_DROP,
        alpha = if (dark) ON_GLASS_SHADOW_DARK else ON_GLASS_SHADOW_LIGHT,
    )
}

/**
 * The band round the rim of glass lying in a pale page, [sideDp] being its shorter side: where it
 * curves away it bends in the page's shade and its own shadow, so a soft grey lies just inside the
 * edge, all the way round, and the white hairline of light shows against it, as the lens round a
 * pane's rim shows where there is more behind it to bend. None in the dark, where the frost and the
 * light already show where it is. As wide as a twelfth of its side, so a folder's tiny tile has a
 * miniature's. It is drawn as a [GlassShadow] that falls nowhere.
 */
internal fun onGlassBand(dark: Boolean, sideDp: Float): GlassShadow? =
    if (dark) null else GlassShadow(blurDp = sideDp.coerceAtLeast(0f) * ON_GLASS_BAND, dropDp = 0f, alpha = ON_GLASS_BAND_LIGHT)

/**
 * How thick the hairline of light round a pane is: 1.6 dp, and on a tile too small for that, a
 * thirtieth of its shorter side, so a folder's tiny tiles are glass with a rim, not all rim.
 */
internal fun rimHairline(pane: Size, density: Float): Float = min(1.6f * density, min(pane.width, pane.height) / 30f)

/** Glass lying in glass: see [glassOnGlass]. */
internal fun DrawScope.drawGlassOnGlass(
    paneSize: Size,
    outline: Outline,
    look: GlassLook,
    light: Offset,
    dark: Boolean,
    touch: GlassTouch,
    band: GlassBand? = null,
) {
    if (paneSize.width <= 0f || paneSize.height <= 0f) return
    drawRect(Color.White.copy(alpha = onGlassFrost(dark)), size = paneSize)
    if (band != null) drawIntoCanvas { it.nativeCanvas.drawPath(band.outline, band.paint) }
    if (touch.strength > 0f) {
        drawRect(
            Brush.radialGradient(
                0f to Color.White.copy(alpha = 0.16f * touch.strength),
                1f to Color.Transparent,
                center = touch.at,
                radius = touch.reach.coerceAtLeast(1f),
            ),
            size = paneSize,
        )
    }
    drawRim(paneSize, outline, look, light, dark, hairline = rimHairline(paneSize, density))
}

/** Glass lying in glass is a touch livelier than the setting alone, as an icon's glass tile is. */
private const val ON_GLASS_STRENGTH = 1.2f
internal const val ON_GLASS_FROST_DARK = 0.09f
internal const val ON_GLASS_FROST_LIGHT = 0.26f
/** How soft the wide shadow under glass lying in glass is, and how far it falls, as shares of its side. */
private const val ON_GLASS_SHADOW_BLUR = 0.12f
private const val ON_GLASS_SHADOW_DROP = 0.03f
private const val ON_GLASS_SHADOW_DARK = 0.08f
private const val ON_GLASS_SHADOW_LIGHT = 0.05f
/** How wide the grey band round the rim of glass lying in a pale page is, as a share of its side, and how grey. */
private const val ON_GLASS_BAND = 1f / 12f
private const val ON_GLASS_BAND_LIGHT = 0.05f

@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    shape: Shape = ZenithShapes.panel,
    tint: Color = Color.Unspecified,
    strength: Float = 1f,
    opacity: Float = 1f,
    border: Boolean = true,
    solid: Float = 0f,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    material: GlassMaterial = GlassMaterial.Regular,
    interactive: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.glass(shape, tint, strength, opacity, border, solid, material, interactive)) {
        Box(modifier = Modifier.padding(contentPadding), content = content)
    }
}
