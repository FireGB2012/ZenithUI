package com.gabriel.zenithui.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.data.Theme
import com.gabriel.zenithui.data.ThemeMaterial
import com.gabriel.zenithui.data.Themes
import kotlin.math.min

/**
 * The theme everything is drawn in (see [Themes]): what its panels are made of, its corners, its
 * outlines, shadows and glow. Its colours come through [LocalPalette], made from it for Light or
 * Dark, and its font through the typography. Liquid Glass outside ZenithTheme.
 */
val LocalTheme = staticCompositionLocalOf { Themes.LiquidGlass }

/** Whether this theme's panels are Liquid Glass, which the glass engine draws. */
val Theme.isGlass: Boolean get() = material == ThemeMaterial.GLASS

/**
 * [shape] as this theme draws it. Liquid Glass keeps every shape exactly as it is drawn; the others
 * take a share of each rounded corner ([Theme.corners]), no more than [Theme.cornerCap], and Retro
 * cuts them off straight. A pill stays a pill where the theme keeps pills. Anything not made of
 * corners (an icon's squircle, a free-form outline) is left as it is.
 */
fun Theme.shape(shape: Shape): Shape {
    if (corners == 1f && cornerCap == 0f && !cut) return shape
    val based = shape as? CornerBasedShape ?: return shape
    val cap = cornerCap.dp
    fun themed(corner: CornerSize): CornerSize = ThemedCorner(corner, corners, cap, keepPills)
    return if (cut) {
        CutCornerShape(themed(based.topStart), themed(based.topEnd), themed(based.bottomEnd), themed(based.bottomStart))
    } else {
        RoundedCornerShape(themed(based.topStart), themed(based.topEnd), themed(based.bottomEnd), themed(based.bottomStart))
    }
}

/**
 * A rounded corner of [radius], as the theme in use draws corners: for anything Zenith rounds, a
 * card, a tile, a field, so that every corner on every screen follows the theme.
 */
@Composable
@ReadOnlyComposable
fun corners(radius: Dp): Shape = LocalTheme.current.shape(RoundedCornerShape(radius))

/** [shape] in the theme in use's corners: see [Theme.shape]. */
@Composable
@ReadOnlyComposable
fun themed(shape: Shape): Shape = LocalTheme.current.shape(shape)

/**
 * One corner as a theme draws it: the corner it was drawn with ([base]), times [scale], no more than
 * [cap] (none at 0), and a pill's left a pill if [keepPills]. A data class, so the same corner is the
 * same shape and never counts as a change.
 */
private data class ThemedCorner(val base: CornerSize, val scale: Float, val cap: Dp, val keepPills: Boolean) : CornerSize {
    override fun toPx(shapeSize: Size, density: Density): Float {
        val drawn = base.toPx(shapeSize, density)
        if (keepPills && drawn >= shapeSize.minDimension / 2f - 0.5f) return drawn
        val scaled = drawn * scale
        return if (cap > 0.dp) min(scaled, cap.value * density.density) else scaled
    }
}

/**
 * Everything in [content] changes theme with a cross-fade: what was on screen in the old theme is
 * kept as a picture, the new theme is drawn under it, and the picture fades away. One picture, faded,
 * whatever is on screen: nothing is drawn twice and nothing is composed twice. [theme] is the key
 * that changes; [reduced] changes at once, as movement reduced asks.
 */
@Composable
fun <T> ThemeCrossfade(theme: T, reduced: Boolean, content: @Composable (shown: T) -> Unit) {
    val layer = rememberGraphicsLayer()
    var shown by remember { mutableStateOf(theme) }
    var leaving by remember { mutableStateOf<ImageBitmap?>(null) }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(theme) {
        if (theme == shown) return@LaunchedEffect
        // The screen as it is, in the theme it was in: the layer still holds the last frame drawn.
        leaving = if (reduced) null else runCatching { layer.toImageBitmap() }.getOrNull()
        shown = theme
        if (leaving != null) {
            fade.snapTo(1f)
            fade.animateTo(0f, tween(THEME_FADE_MS, easing = FastOutSlowInEasing))
        }
        leaving = null
    }
    Box(
        Modifier.drawWithContent {
            layer.record { this@drawWithContent.drawContent() }
            drawLayer(layer)
            leaving?.let { drawImage(it, alpha = fade.value.coerceIn(0f, 1f)) }
        },
    ) {
        content(shown)
    }
}

/** How long a change of theme takes to cross-fade. */
const val THEME_FADE_MS = 380
