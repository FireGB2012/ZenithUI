package com.gabriel.zenithui.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import com.gabriel.zenithui.data.Theme
import com.gabriel.zenithui.data.ThemeMaterial

/**
 * A panel in its theme's own material, where the theme is not glass: what `Modifier.glass` draws in
 * Clean, so every panel on every screen changes with the theme. Clean is a flat card: a solid face,
 * a hairline edge and a short, soft shadow. (Aurora and Frost are glass, and drawn as glass.)
 *
 * Its corners are the theme's ([Theme.shape]), its colours the palette's. A pane in [tint] takes a
 * little of it into its face; [tinted], the one button that moves you on, is the accent outright.
 * It presses as glass does ([motion]), and an [interactive] one lights a little under the finger.
 */
@Composable
internal fun Modifier.themedPanel(
    theme: Theme,
    shape: Shape,
    tint: Color,
    tinted: Boolean,
    opacity: Float,
    border: Boolean,
    solid: Float,
    shadow: Boolean,
    interactive: Boolean,
    motion: GlassMotion?,
    pressEnabled: Boolean,
    light: PressLight?,
): Modifier {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    val ownLight = if (interactive && light == null) remember { PressLight(scope) } else null
    val press = light ?: ownLight
    val moved = if (motion != null) remember { PressMotion(scope) } else null
    val outlined = theme.shape(shape)
    val face = panelFace(theme, palette, tint, tinted, solid)
    // The accent's button is its colour, edge to edge.
    val edge = if (tinted) Color.Transparent else palette.edge
    val alpha = opacity.coerceIn(0f, 1f)

    return this
        .then(if (moved != null && motion != null) Modifier.pressMotion(moved, motion, pressEnabled) else Modifier)
        .then(
            if (ownLight != null) {
                Modifier.pointerInput(Unit) {
                    // Watches the finger without taking it, as glass does.
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
        .then(if (shadow && alpha > 0f) Modifier.panelShadow(theme, palette, outlined, alpha) else Modifier)
        .clip(outlined)
        .drawWithCache {
            val outline = outlined.createOutline(size, layoutDirection, this)
            val edgePx = theme.edgeWidth * density
            onDrawBehind {
                if (alpha <= 0f) return@onDrawBehind
                drawRect(face, alpha = alpha)
                // The finger's light: a little of the ink under it.
                val touch = press?.touch(size)
                if (touch != null && touch.strength > 0f) {
                    val lit = palette.ink
                    drawRect(
                        Brush.radialGradient(
                            0f to lit.copy(alpha = PRESS_LIGHT * touch.strength),
                            1f to lit.copy(alpha = PRESS_LIGHT * 0.4f * touch.strength),
                            center = touch.at,
                            radius = touch.reach.coerceAtLeast(1f) * 2f,
                        ),
                        alpha = alpha,
                    )
                }
                // The stroke straddles the outline, and the clip keeps the inside half of it.
                if (border && edgePx > 0f && edge.alpha > 0f) {
                    drawOutline(outline, edge, alpha = alpha, style = Stroke(width = edgePx * 2f))
                }
            }
        }
}

/**
 * Glass lying in glass, in a theme that is not glass: a tile in a sheet, a toggle in a panel. A
 * faint face of the ink and the theme's edge, thinner than a panel's.
 */
@Composable
internal fun Modifier.themedOnPanel(theme: Theme, shape: Shape, interactive: Boolean): Modifier {
    val palette = LocalPalette.current
    val scope = rememberCoroutineScope()
    val press = if (interactive) remember { PressLight(scope) } else null
    val outlined = theme.shape(shape)
    val face = palette.ink.copy(alpha = 0.06f)
    val edge = palette.edge
    return this
        .then(
            if (press != null) {
                Modifier.pointerInput(Unit) {
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
        .clip(outlined)
        .drawWithCache {
            val outline = outlined.createOutline(size, layoutDirection, this)
            // Lying in a panel, its edge is a little thinner than the panel's own.
            val edgePx = (theme.edgeWidth * 0.75f).coerceAtLeast(1f) * density
            onDrawBehind {
                drawRect(face)
                val touch = press?.touch(size)
                if (touch != null && touch.strength > 0f) drawRect(palette.ink.copy(alpha = PRESS_LIGHT * touch.strength))
                drawOutline(outline, edge, style = Stroke(width = edgePx * 2f))
            }
        }
}

/** The face of a panel: the theme's surface, a little of a pane's own colour in it, or the accent outright. */
@Suppress("UNUSED_PARAMETER") // [theme] and [solid] are kept for the callers: Clean's face is the same for a sheet.
internal fun panelFace(theme: Theme, palette: ZenithPalette, tint: Color, tinted: Boolean, solid: Float): Color {
    if (tinted && tint.isSpecified) return tint
    val surface = palette.surface
    return if (tint.isSpecified) lerp(surface, tint.copy(alpha = surface.alpha), TINT_SHARE) else surface
}

/** Under a panel, outside its outline: Clean's short, soft shadow. */
private fun Modifier.panelShadow(theme: Theme, palette: ZenithPalette, shape: Shape, alpha: Float): Modifier =
    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = android.graphics.Path().apply { addOutline(outline) }
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
        val dx = theme.shadowX * density
        val dy = theme.shadowY * density
        paint.color = android.graphics.Color.BLACK
        paint.setShadowLayer((theme.shadowBlur * density).coerceAtLeast(0.1f), dx, dy, palette.shadow.copy(alpha = palette.shadow.alpha * alpha).toArgb())
        onDrawBehind {
            drawIntoCanvas {
                val canvas = it.nativeCanvas
                canvas.save()
                // Only what falls outside the panel: the panel itself is drawn over it.
                canvas.clipOutPath(path)
                canvas.drawPath(path, paint)
                canvas.restore()
            }
        }
    }

/** How much a pane's own colour colours a theme's face. */
private const val TINT_SHARE = 0.22f

/** How strong the finger's light is on a panel that is not glass. */
private const val PRESS_LIGHT = 0.10f

/**
 * A block on a page: a group of rows in Settings, a card. In the glass themes a faint wash of the
 * ink, as iOS's grouped rows are on their page; in Clean its own panel, with its corners, edge and
 * shadow, so Settings is made of the same stuff as everything else.
 */
@Composable
fun Modifier.themeSurface(shape: Shape): Modifier {
    val theme = LocalTheme.current
    if (theme.isGlass) return clip(shape).background(LocalPalette.current.surface)
    return themedPanel(
        theme = theme,
        shape = shape,
        tint = Color.Unspecified,
        tinted = false,
        opacity = 1f,
        border = true,
        solid = 0f,
        shadow = true,
        interactive = false,
        motion = null,
        pressEnabled = true,
        light = null,
    )
}

/**
 * A full page's face in a theme that is not glass: the theme's page, top to bottom, as Settings and
 * the other full pages stand on it. (The glass themes draw their pages as glass.)
 */
fun Modifier.themePage(palette: ZenithPalette, shape: Shape): Modifier =
    clip(shape).background(Brush.verticalGradient(palette.backdrop.ifEmpty { listOf(Color.Black) }.let { if (it.size == 1) it + it else it }))

/**
 * The colour tile beside a row in Settings, in [colours], as the theme draws one: the glass themes'
 * gradient, Clean's flat colour. [themeTileInk] is the colour of the symbol on it.
 */
@Composable
fun Modifier.themeTile(colours: List<Color>, shape: Shape): Modifier {
    val theme = LocalTheme.current
    val palette = LocalPalette.current
    val first = colours.firstOrNull() ?: palette.accent
    val outlined = theme.shape(shape)
    return when (theme.material) {
        ThemeMaterial.GLASS -> clip(outlined).background(Brush.linearGradient(colours.ifEmpty { listOf(first, first) }.let { if (it.size == 1) it + it else it }))
        ThemeMaterial.FLAT -> clip(outlined).background(first)
    }
}

/** The symbol on a [themeTile]: white on its colour, in every theme. */
@Suppress("UNUSED_PARAMETER") // [colours] is kept for the callers: the symbol is white on any of them.
@Composable
@androidx.compose.runtime.ReadOnlyComposable
fun themeTileInk(colours: List<Color>): Color = Color.White
