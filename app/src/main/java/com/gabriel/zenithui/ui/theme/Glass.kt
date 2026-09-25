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
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Clear floats over the wallpaper with the strongest bend: the dock, folders, small controls.
 * Regular softens what is behind it so text on top stays readable: widgets, search, sheets.
 */
enum class GlassMaterial { Regular, Clear }

/**
 * Liquid Glass. What is behind a pane shows through it; towards the rounded rim it bends and
 * magnifies like the edge of a thick lens, and a thin highlight runs round the rim on the side
 * facing the light, which moves as the phone tilts. Labels and icons drawn on top stay sharp.
 *
 * On Android 13+ with a readable wallpaper this is a GPU shader over a cached copy of the
 * wallpaper (no screen capture, no per-frame blur). Android 12 gets the softened wallpaper with the
 * same rim; a wallpaper Zenith cannot read gets a frosted body of its own rather than a stale picture.
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
): Modifier {
    val palette = LocalPalette.current
    val backdrop = LocalBackdrop.current
    val sharp = LocalGlassBackdrop.current ?: backdrop
    val screen = LocalScreenSize.current
    val wallpaperOffset = LocalWallpaperOffset.current
    val tilt = LocalTilt.current
    val amount = (LocalGlassStrength.current * strength).coerceIn(0f, 1.4f)
    val look = glassLook(
        material = material,
        strength = amount,
        solid = solid,
        opacity = opacity,
        dark = palette.dark,
        tint = if (tint.isSpecified) tint else palette.glassTint,
        border = border,
    )
    val lens = rememberLens(sharp, backdrop)
    var origin by remember { mutableStateOf(Offset.Zero) }

    return this
        .onGloballyPositioned { origin = it.positionInWindow() }
        .clip(shape)
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            onDrawBehind {
                // Position, wallpaper scroll and tilt are all read here, while drawing, so moving
                // any of them repaints the glass without recomposing anything.
                drawLiquidGlass(
                    paneSize = size,
                    outline = outline,
                    lens = lens,
                    softened = backdrop,
                    origin = origin,
                    screen = screen,
                    wallpaperOffset = wallpaperOffset.value,
                    look = look,
                    lean = tilt.value,
                    dark = palette.dark,
                )
            }
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
internal fun rememberLens(sharp: ImageBitmap?, softened: ImageBitmap?): LiquidGlassShader? =
    remember(sharp, softened) {
        if (Build.VERSION.SDK_INT >= 33 && sharp != null && softened != null) {
            // A GPU that cannot compile it still gets the Android 12 glass below.
            runCatching { LiquidGlassShader(sharp, softened) }.getOrNull()
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
): GlassLook {
    val clear = material == GlassMaterial.Clear
    val amount = strength.coerceIn(0f, 1.4f)
    val body = solid.coerceIn(0f, 1.5f)
    // A whisper of the theme in the veil, never enough to turn the glass into a coloured card.
    val veil = lerp(if (dark) Color(0xFF0A0C12) else Color.White, tint, 0.10f)
    val veilAlpha = if (dark) {
        (if (clear) 0.08f else 0.20f) + 0.42f * body
    } else {
        (if (clear) 0.12f else 0.30f) + 0.40f * body
    }
    return GlassLook(
        refraction = (if (clear) 1f else 0.72f) * (0.35f + 0.65f * min(amount, 1f)),
        frost = ((if (clear) 0.08f else 0.80f) + 0.20f * body).coerceIn(0f, 1f),
        dispersion = (if (clear) 0.08f else 0.05f) * min(amount, 1f),
        shine = if (border) 0.55f + 0.45f * min(amount, 1.2f) else 0f,
        vibrance = if (clear) 1.14f else 1.22f,
        wash = veil.copy(alpha = veilAlpha.coerceIn(0f, 0.92f)),
        adapt = (if (clear) 0.30f else 0.34f) + 0.2f * min(body, 1f),
        opacity = opacity.coerceIn(0f, 1f),
    )
}

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
) {
    if (paneSize.width <= 0f || paneSize.height <= 0f || look.opacity <= 0f) return
    // The light sits up and to the left, and swings the other way as the phone tips.
    val light = Offset(-0.45f - 1.6f * lean.x, -1f - 1.2f * lean.y)
    val canSee = softened != null && screen.width > 0 && screen.height > 0
    val shown = systemWallpaperOffset(wallpaperOffset)

    if (canSee && lens != null && Build.VERSION.SDK_INT >= 33) {
        drawIntoCanvas {
            lens.draw(it.nativeCanvas, paneSize, outline, origin, screen, shown, density, look, light, dark)
        }
        return
    }

    if (canSee && softened != null) {
        // Android 12: the softened wallpaper, lined up with the real one, under the same veil.
        val scale = max(screen.width / softened.width.toFloat(), screen.height / softened.height.toFloat()) *
            SYSTEM_WALLPAPER_ZOOM
        val width = (softened.width * scale).roundToInt()
        val height = (softened.height * scale).roundToInt()
        drawImage(
            image = softened,
            dstOffset = IntOffset(
                (-origin.x - (width - screen.width) * shown.x).roundToInt(),
                (-origin.y - (height - screen.height) * shown.y).roundToInt(),
            ),
            dstSize = IntSize(width, height),
            alpha = look.opacity,
            filterQuality = FilterQuality.Low,
        )
        drawRect(look.wash, size = paneSize, alpha = look.opacity)
    } else {
        // Nothing readable behind (a live wallpaper, or setup): a frosted body of its own, lit
        // from the top, so the pane still reads as glass rather than a flat card.
        val base = if (dark) Color(0xFF181B24) else Color.White
        // Light for floating controls, much fuller for sheets, whose text sits over the page.
        val bodyAlpha = (if (dark) 0.22f else 0.28f) + 1.3f * look.wash.alpha
        drawRect(base.copy(alpha = bodyAlpha.coerceAtMost(0.94f)), size = paneSize, alpha = look.opacity)
        drawRect(
            Brush.verticalGradient(
                0f to Color.White.copy(alpha = if (dark) 0.10f else 0.30f),
                0.5f to Color.Transparent,
                endY = paneSize.height,
            ),
            size = paneSize,
            alpha = look.opacity,
        )
    }
    drawRim(paneSize, outline, look, light, dark)
}

/** The rim for the non-shader paths: a hairline glint facing the light and a fainter one opposite. */
private fun DrawScope.drawRim(paneSize: Size, outline: Outline, look: GlassLook, light: Offset, dark: Boolean) {
    if (look.shine <= 0f) return
    val strength = look.shine * look.opacity * (if (dark) 0.85f else 1f)
    val centre = Offset(paneSize.width / 2f, paneSize.height / 2f)
    val reach = max(paneSize.width, paneSize.height) * 0.6f
    val length = kotlin.math.hypot(light.x, light.y).coerceAtLeast(0.001f)
    val towards = Offset(light.x / length, light.y / length)
    drawOutline(
        outline = outline,
        brush = Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.75f * strength),
            0.35f to Color.White.copy(alpha = 0.14f * strength),
            0.65f to Color.White.copy(alpha = 0.10f * strength),
            1f to Color.White.copy(alpha = 0.42f * strength),
            start = centre + towards * reach,
            end = centre - towards * reach,
        ),
        style = Stroke(1.dp.toPx()),
    )
}

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
