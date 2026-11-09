package com.gabriel.zenithui.ui.theme

import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
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
 * Liquid Glass, after Apple's. What is behind a pane shows through it, softened as glass softens
 * it and a little brighter where it is dark; towards the rounded rim it bends and magnifies like
 * the edge of a thick lens, and the rim throws back the wallpaper beside the pane. A crisp white
 * highlight runs all the way round, brightest on the side facing the light and bright again
 * opposite, and it moves as the phone tilts. Nothing of the wallpaper's colour or the theme's is
 * added to it: the glass is clear, as Apple's is. The pane is a solid slab and looks it: its
 * shoulders are shaded by that light, its polished edge glints, and it casts a shadow, wide where
 * it floats and close and dark under its edge, deeper in the dark. Labels and icons drawn on top
 * stay sharp.
 *
 * Glass goes only where the wallpaper is right behind it: the dock, search, folders, sheets, the
 * drawer and controls floating over the home screen. It shows the wallpaper, so on a solid page it
 * would be a hole; there things are solid (`solidCard`).
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
    /**
     * Extra body for surfaces that must stay readable over busy content, such as sheets. From
     * [COVERING_BODY] the pane is one that covers the home screen, as a sheet does.
     */
    solid: Float = 0f,
    material: GlassMaterial = GlassMaterial.Regular,
    /** The soft shadow the pane floats on. Off for a pane that fills the screen, as the drawer does. */
    shadow: Boolean = true,
    /** How the pane moves under a finger, if it does: see [GlassMotion]. */
    motion: GlassMotion? = null,
    /** Whether [motion] answers a finger now: off for a button that cannot be pressed. */
    pressEnabled: Boolean = true,
    /**
     * For a pane that something outside it moves or scales with a graphicsLayer, as a dragged lens
     * is: reads what moves it, while drawing, so the wallpaper behind is lined up again as it goes
     * rather than travelling with the pane.
     */
    tracking: (() -> Unit)? = null,
): Modifier {
    val palette = LocalPalette.current
    val backdrop = LocalBackdrop.current
    val levels = LocalGlassLevels.current
    val screen = LocalScreenSize.current
    val wallpaperOffset = LocalWallpaperOffset.current
    val tilt = LocalTilt.current
    val tuning = LocalGlassTuning.current
    val amount = (LocalGlassStrength.current * strength).coerceIn(0f, 1.4f)
    val look = glassLook(
        material = material,
        strength = amount,
        solid = solid,
        opacity = opacity,
        dark = palette.dark,
        tint = if (tint.isSpecified) tint else palette.glassTint,
        border = border,
        depth = LocalGlassBody.current,
    ).tuned(tuning, if (tint.isSpecified) tint else palette.glassTint)
    val lens = rememberLens(levels)
    var origin by remember { mutableStateOf(Offset.Zero) }
    // Where the pane was laid out, kept to work out where it is drawn while something moves it.
    val placed = remember { arrayOfNulls<LayoutCoordinates>(1) }
    val scope = rememberCoroutineScope()
    val moved = if (motion != null) remember { PressMotion(scope) } else null

    return this
        .then(if (moved != null && motion != null) Modifier.pressMotion(moved, motion, pressEnabled) else Modifier)
        .onGloballyPositioned {
            origin = it.positionInWindow()
            placed[0] = it
        }
        .then(
            if (shadow && look.opacity > 0f) {
                Modifier.drawWithCache {
                    val outline = shape.createOutline(size, layoutDirection, this)
                    val path = android.graphics.Path().apply { addOutline(outline) }
                    val shadows = shadowPaints(look, material, palette.dark, density, size)
                    onDrawBehind {
                        drawIntoCanvas {
                            val canvas = it.nativeCanvas
                            canvas.save()
                            // Only the shadow is wanted: the pane itself is drawn by the glass.
                            canvas.clipOutPath(path)
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
            onDrawBehind {
                // Position, wallpaper scroll and tilt are all read here, while drawing, so any of
                // them changing repaints the glass without recomposing anything.
                drawLiquidGlass(
                    paneSize = size,
                    outline = outline,
                    lens = lens,
                    softened = backdrop,
                    origin = if (moved != null || tracking != null) drawnOrigin(placed[0], moved, tracking) ?: origin else origin,
                    screen = screen,
                    wallpaperOffset = wallpaperOffset.value,
                    look = look,
                    // Only a pane whose rim bends and catches the light changes as the phone
                    // tips; one without is not drawn again for it.
                    lean = if (look.followsTilt) tilt.value else Offset.Zero,
                    dark = palette.dark,
                    tuning = tuning,
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
 * Apple's glass casts a soft shadow that separates it from what is behind, and a slab's has two
 * parts: a wide, faint one that says how far off the wallpaper it floats, and a close, darker one
 * under its edge that says it is a solid thing. Both fall away from the light, down and a little
 * to the right, and a bigger pane is a thicker slab and casts more of each. They are a little
 * deeper in the dark, where they have to show against a dark picture, and lighter for Clear.
 */
internal fun shadowPaints(look: GlassLook, material: GlassMaterial, dark: Boolean, density: Float, pane: Size): List<android.graphics.Paint> {
    val clear = material == GlassMaterial.Clear
    val alpha = (if (dark) 0.34f else 0.18f) * (if (clear) 0.75f else 1f) * look.opacity
    val thick = slabThickness(pane, density)
    fun layer(blur: Float, reach: Float, strength: Float) = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        setShadowLayer(
            blur * density,
            SHADOW_FALL.x * reach * density,
            SHADOW_FALL.y * reach * density,
            Color.Black.copy(alpha = strength.coerceIn(0f, 1f)).toArgb(),
        )
    }
    return listOf(
        layer(blur = 12f + 12f * thick, reach = 3f + 6f * thick, strength = alpha),
        // The close one has to show even where the wallpaper is nearly black already.
        layer(blur = 2.5f + 3f * thick, reach = 1f + 2f * thick, strength = alpha * 0.9f * (if (dark) 1.5f else 1f)),
    )
}

/**
 * Which way shadows fall: straight away from the lamp as it hangs when the phone is held still
 * (see the light in [drawLiquidGlass]). They do not swing with the phone as the highlights do.
 */
private val SHADOW_FALL = Offset(0.41f, 0.91f)

/**
 * How thick a slab a pane is, 0 to 1, by the sum the shader does for the width of its shoulder:
 * about a quarter of the pane's shorter side, from 12 dp for a small control to 28 for a widget
 * or a sheet, and never so much that two shoulders meet.
 */
internal fun slabThickness(pane: Size, density: Float): Float {
    val shortest = min(pane.width, pane.height) / density.coerceAtLeast(0.01f)
    return min((shortest * 0.24f).coerceIn(12f, SLAB_SHOULDER), shortest * 0.45f).coerceAtLeast(0f) / SLAB_SHOULDER
}

/** The widest a slab's shoulder gets, in dp. */
private const val SLAB_SHOULDER = 28f

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

/** One shader per pane, rebuilt only when the wallpaper itself changes. */
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
): GlassLook {
    val clear = material == GlassMaterial.Clear
    val amount = strength.coerceIn(0f, 1.4f)
    val body = (solid + depth.solid).coerceIn(0f, 1.5f)
    // A neutral veil, as Apple's glass has: nothing of the theme in it. (The Glass studio can still
    // add some: see tuned.)
    val veil = if (dark) Color(0xFF0B0B0D) else Color.White
    val veilAlpha = if (dark) {
        (if (clear) 0.08f else 0.20f) + 0.42f * body
    } else {
        (if (clear) 0.12f else 0.30f) + 0.40f * body
    }
    return GlassLook(
        refraction = (if (clear) 1f else 0.72f) * (0.35f + 0.65f * min(amount, 1f)),
        // Clear glass mists what is behind it, as any thickness of glass does; Regular frosts it,
        // and a pane with more body, like a sheet, more still.
        frost = ((if (clear) FROST_CLEAR else FROST_REGULAR) + 0.9f * body).coerceIn(0f, 3f),
        // Only clear glass splits light into colour at its rim: through frost the split is lost,
        // and it costs the shader three looks at the wallpaper where one will do.
        dispersion = if (clear) 0.08f * min(amount, 1f) else 0f,
        // A lifted pane gets a slightly surer rim too: the edge is what is read first.
        shine = if (border) 0.32f + 0.26f * min(amount, 1.2f) + 0.06f * depth.lift else 0f,
        vibrance = if (clear) 1.14f else 1.22f,
        wash = veil.copy(alpha = veilAlpha.coerceIn(0f, 0.92f)),
        adapt = (if (clear) 0.30f else 0.34f) + 0.2f * min(body, 1f),
        opacity = opacity.coerceIn(0f, 1f),
        lift = glassLift(clear, dark, body, depth.lift),
        // A pane with no rim to catch the light has none to hold a reflection either.
        mirror = if (border) (if (clear) MIRROR_CLEAR else MIRROR_REGULAR) * min(amount, 1f) else 0f,
        glow = if (border) GLOW else 0f,
        gather = if (clear) GATHER_CLEAR else GATHER_REGULAR,
        rimmed = border,
        // Turning the glass down flattens it: a slab at full strength, a sheet of light at none.
        relief = if (border) min(amount, 1f) else 0f,
        // Asked for by what the pane is, not by how much body the person likes their glass with:
        // Solid glass makes the dock fuller, not a lid over the wallpaper.
        covers = solid >= COVERING_BODY,
    )
}

/**
 * The body from which a pane covers what is behind it rather than floating over the wallpaper:
 * a sheet's, an open folder's, the drawer's. Floating controls ask for less.
 */
internal const val COVERING_BODY = 0.6f

/**
 * The frosted body a pane gets of its own when Zenith cannot read the wallpaper (a live wallpaper,
 * or setup). A floating control keeps a light one, so the live wallpaper still shows through it;
 * one with more veil, and so words on it, gets more. A pane that [GlassLook.covers] what is behind
 * it is not see-through at all, since behind a sheet is the home screen, not the wallpaper, and its
 * words would sit over the icons: it is Apple's page instead, which the cards in it stand off as
 * they do in Settings.
 */
internal fun fallbackFill(look: GlassLook, dark: Boolean): Color {
    if (look.covers) return AppleColours.of(dark).page
    val base = if (dark) Color(0xFF181B24) else Color.White
    return base.copy(alpha = ((if (dark) 0.22f else 0.28f) + 1.3f * look.wash.alpha).coerceAtMost(0.94f))
}

/**
 * How bright the light is across the top of a pane's own frosted body when Zenith cannot read the
 * wallpaper, so a floating control still reads as glass. A covering pane has none: it is Apple's
 * page, and the light would lift it to the colour of the cards on it just where the first ones sit.
 */
internal fun fallbackSheen(look: GlassLook, dark: Boolean): Float =
    if (look.covers) 0f else if (dark) 0.10f else 0.30f

/**
 * The ink for words on a pill of the accent: dark on anything lighter than a mid tone, white on a
 * deep colour. White on Sunset's orange, or dark on violet, was hard to read.
 */
fun inkOnTint(fill: Color): Color = if (fill.luminance() > 0.3f) Color(0xFF0B0C10) else Color.White

/** The Glass studio's tuning, for every pane: Zenith's own glass with the defaults, and without Plus. */
val LocalGlassTuning = staticCompositionLocalOf { GlassTuning() }

/** This pane as the Glass studio has it; with the defaults, exactly as it was. */
internal fun GlassLook.tuned(tuning: GlassTuning, tint: Color): GlassLook {
    if (tuning.isDefault) return this
    return copy(
        refraction = refraction * tuning.bend,
        frost = (frost + tuning.frost).coerceIn(0f, 3f),
        dispersion = dispersion * tuning.rainbow,
        shine = shine * tuning.rim,
        mirror = (mirror * tuning.rim).coerceIn(0f, 1f),
        // More of the theme in the veil, as thick as it was: readable as before, just more coloured.
        wash = if (tuning.tint <= 0f) wash else lerp(wash, tint.copy(alpha = wash.alpha), tuning.tint),
    )
}

/** How far Zenith's own light is set from a pane: up and to the left, the length of (-0.45, -1). */
private val ZENITH_LIGHT_REACH = hypot(0.45f, 1f)

/**
 * Where the light falls on a pane from, the phone leaning by [lean]: up and to the left, or from
 * where the Glass studio put it, and swinging the other way as the phone tips unless it is stilled.
 */
internal fun lightFrom(lean: Offset, tuning: GlassTuning): Offset {
    val swing = if (tuning.lightTilt) lean else Offset.Zero
    val angle = tuning.lightAngle
    val base = if (angle == null) {
        Offset(-0.45f, -1f)
    } else {
        val radians = Math.toRadians(angle.toDouble())
        Offset((cos(radians) * ZENITH_LIGHT_REACH).toFloat(), (sin(radians) * ZENITH_LIGHT_REACH).toFloat())
    }
    return Offset(base.x - 1.6f * swing.x, base.y - 1.2f * swing.y)
}

/** How soft the wallpaper is through the face of a pane with no extra body: see [GlassLook.frost]. */
internal const val FROST_CLEAR = 1.0f
internal const val FROST_REGULAR = 1.55f

/** How much of the picture beside it a pane's rim throws back at full strength: see [GlassLook.mirror]. */
internal const val MIRROR_CLEAR = 0.7f
internal const val MIRROR_REGULAR = 0.6f

/**
 * How far the light on the glass takes the colour of the wallpaper round it: see [GlassLook.glow].
 * None: Apple's rim is white, and a rim in the wallpaper's colour read as a glow round every pane.
 */
internal const val GLOW = 0f

/** How much brighter what is dark comes through a pane: see [GlassLook.gather]. */
internal const val GATHER_CLEAR = 0.35f
internal const val GATHER_REGULAR = 0.28f

/**
 * The frost that lifts a pane off a picture of its own tone. In the dark a pane over a dark
 * wallpaper is a dark veil on a dark picture, and only its rim says it is there; a little white
 * frost makes it a shade lighter than what is round it. In the light it is the other way about:
 * over a near-white picture the pane takes a faint grey shade. Neither has the theme in it. The
 * colour is the frost; its alpha is how much of it, and the shader lays it on only where the
 * picture behind really is that dark or that pale, so glass over anything else stays as clear as it
 * was.
 *
 * A pane that already has a full veil, like a sheet, needs less of it.
 */
internal fun glassLift(clear: Boolean, dark: Boolean, body: Float, lift: Float): Color {
    val tone = if (dark) Color.White else Color(0xFF636366)
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
) {
    if (paneSize.width <= 0f || paneSize.height <= 0f || look.opacity <= 0f) return
    // The light sits up and to the left, and swings the other way as the phone tips.
    val light = lightFrom(lean, tuning)
    val leaning = if (tuning.lightTilt) lean else Offset.Zero
    val canSee = softened != null && screen.width > 0 && screen.height > 0
    val shown = systemWallpaperOffset(wallpaperOffset)

    if (canSee && lens != null && Build.VERSION.SDK_INT >= 33) {
        drawIntoCanvas {
            lens.draw(it.nativeCanvas, paneSize, outline, origin, screen, shown, density, look, light, dark, touch, leaning)
        }
        return
    }

    if (canSee && softened != null) {
        // Android 12: the softened wallpaper, lined up with the real one, under the same veil.
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
            alpha = look.opacity,
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
                alpha = look.opacity,
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
                alpha = look.opacity,
                filterQuality = FilterQuality.Low,
            )
        }
        drawRect(look.wash, size = paneSize, alpha = look.opacity)
        // No shader to see how dark the picture is under each point, so the lift goes on evenly
        // and a little thinner.
        if (look.lift.alpha > 0f) {
            drawRect(look.lift.copy(alpha = look.lift.alpha * 0.7f), size = paneSize, alpha = look.opacity)
        }
    } else {
        // Nothing readable behind (a live wallpaper, or setup): a frosted body of its own, lit
        // from the top, so the pane still reads as glass rather than a flat card.
        drawRect(fallbackFill(look, dark), size = paneSize, alpha = look.opacity)
        val sheen = fallbackSheen(look, dark)
        if (sheen > 0f) {
            drawRect(
                Brush.verticalGradient(
                    0f to Color.White.copy(alpha = sheen),
                    0.5f to Color.Transparent,
                    endY = paneSize.height,
                ),
                size = paneSize,
                alpha = look.opacity,
            )
        }
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
    // A covering pane with nothing to show is Apple's page, flat: its slab is not shaded either.
    drawRim(paneSize, outline, if (!canSee && look.covers) look.copy(relief = 0f) else look, light, dark)
}

/**
 * The rim for the non-shader paths, as near the shader's as strokes can come: a line all the way
 * round, brightest facing the light and bright again opposite, and a soft glow just inside it.
 * Each stroke straddles the outline and the pane's own clip keeps the inner half, so three of
 * different widths laid over each other fade inwards.
 *
 * The slab's form ([GlassLook.relief]) is drawn the same way and under the rim: light falling away
 * evenly across the pane, and the edge turned from the lamp in its own shade.
 */
private fun DrawScope.drawRim(paneSize: Size, outline: Outline, look: GlassLook, light: Offset, dark: Boolean) {
    val centre = Offset(paneSize.width / 2f, paneSize.height / 2f)
    val length = kotlin.math.hypot(light.x, light.y).coerceAtLeast(0.001f)
    val towards = Offset(light.x / length, light.y / length)
    // Where each part of the pane stands to the light, as a line across it from its lit corner
    // to its far one. The line is stretched to the pane's own shape, so the whole top edge of a
    // wide bar is lit, as the shader lights it, and not just the corner nearest the lamp.
    val across = Offset(towards.x / (paneSize.width / 2f), towards.y / (paneSize.height / 2f))
    val span = (kotlin.math.abs(towards.x) + kotlin.math.abs(towards.y)) / (across.x * across.x + across.y * across.y)
    val lamp = centre + across * span
    val far = centre - across * span
    // A small pane gets narrower strokes, or it would be all edge and no face.
    val widest = min(paneSize.width, paneSize.height) * 0.3f

    val relief = look.relief * look.opacity
    if (relief > 0f) {
        drawRect(
            Brush.linearGradient(
                0f to Color.White.copy(alpha = 0.06f * relief),
                0.5f to Color.Transparent,
                1f to Color.Black.copy(alpha = (if (dark) 0.26f else 0.12f) * relief),
                start = lamp,
                end = far,
            ),
            size = paneSize,
        )
        val depth = (if (dark) 0.16f else 0.07f) * relief
        val shade = Brush.linearGradient(
            0f to Color.Transparent,
            0.5f to Color.Black.copy(alpha = 0.4f * depth),
            1f to Color.Black.copy(alpha = depth),
            start = lamp,
            end = far,
        )
        for (width in RIM_GLOW_WIDTHS) drawOutline(outline, shade, style = Stroke(min(width.dp.toPx(), widest)))
    }

    if (look.shine <= 0f) return
    val strength = look.shine * look.opacity * (if (dark) 0.88f else 1f)
    fun lit(amount: Float) = Brush.linearGradient(
        0f to Color.White.copy(alpha = (1.5f * amount).coerceAtMost(1f)),
        0.35f to Color.White.copy(alpha = (0.5f * amount).coerceAtMost(1f)),
        0.65f to Color.White.copy(alpha = (0.42f * amount).coerceAtMost(1f)),
        1f to Color.White.copy(alpha = (0.95f * amount).coerceAtMost(1f)),
        start = lamp,
        end = far,
    )
    val glow = lit(0.085f * strength)
    for (width in RIM_GLOW_WIDTHS) drawOutline(outline, glow, style = Stroke(min(width.dp.toPx(), widest)))
    drawOutline(outline, lit(strength), style = Stroke(2.5.dp.toPx()))
}

/** The strokes that make the glow inside the rim, in dp; the pane's clip shows the inner half of each. */
private val RIM_GLOW_WIDTHS = floatArrayOf(14f, 9f, 5f)

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
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.glass(shape, tint, strength, opacity, border, solid, material)) {
        Box(modifier = Modifier.padding(contentPadding), content = content)
    }
}
