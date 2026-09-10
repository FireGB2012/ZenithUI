package com.gabriel.zenithui.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The Liquid Glass surface: the wallpaper, blurred once and painted behind the panel at exactly
 * the offset it sits on screen, with a translucent gradient, a colour tint and a specular rim.
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
): Modifier {
    val palette = LocalPalette.current
    val backdrop = LocalBackdrop.current
    val screen = LocalScreenSize.current
    val glass = (LocalGlassStrength.current * strength).coerceIn(0f, 1.4f)
    val tilt = LocalTilt.current
    val dark = palette.dark
    val tintColor = if (tint.isSpecified) tint else palette.glassTint
    var origin by remember { mutableStateOf(Offset.Zero) }

    val base = if (dark) palette.backdrop.first() else Color.White
    val scrimAlpha = ((if (dark) 0.26f else 0.34f) + 0.22f * glass + 0.42f * solid).coerceAtMost(0.94f) * opacity
    val sheen = Color.White
    val topAlpha = (if (dark) 0.17f else 0.62f) * (0.55f + 0.45f * glass) * opacity
    val bottomAlpha = (if (dark) 0.06f else 0.34f) * (0.55f + 0.45f * glass) * opacity

    return this
        .onGloballyPositioned { origin = it.positionInWindow() }
        .clip(shape)
        .drawBehind {
            if (backdrop != null && screen.width > 0 && screen.height > 0) {
                val scale = max(
                    screen.width / backdrop.width.toFloat(),
                    screen.height / backdrop.height.toFloat(),
                )
                val dstW = (backdrop.width * scale).roundToInt()
                val dstH = (backdrop.height * scale).roundToInt()
                drawImage(
                    image = backdrop,
                    dstOffset = IntOffset(
                        -origin.x.roundToInt() + (screen.width - dstW) / 2,
                        -origin.y.roundToInt(),
                    ),
                    dstSize = IntSize(dstW, dstH),
                    alpha = opacity,
                )
                drawRect(
                    color = (if (dark) Color(0xFF05060B) else Color.White)
                        .copy(alpha = (0.24f + 0.20f * glass + 0.30f * solid) * opacity)
                )
            } else {
                // No readable wallpaper copy: give the panel its own frosted body instead.
                drawRect(color = base.copy(alpha = scrimAlpha))
            }
            if (solid > 0f) {
                drawRect(color = base.copy(alpha = 0.55f * solid * opacity))
            }
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(sheen.copy(alpha = topAlpha), sheen.copy(alpha = bottomAlpha))
                )
            )
            drawRect(color = tintColor.copy(alpha = 0.07f * glass * opacity))

            // The moving part: the rim, not the body. Glass gives itself away at its edge —
            // the bevel picks up the light and carries a bright arc round with it as the panel
            // turns, while the face itself stays the same frosted grey. Read inside the draw
            // lambda, so a new sensor reading repaints without recomposing anything.
            val lean = tilt.value
            val leanLength = kotlin.math.hypot(lean.x, lean.y)
            if (leanLength > 0.015f && glass > 0.05f) {
                // The light is fixed in the room, so it sits on the side opposite the lean.
                val unit = Offset(-lean.x / leanLength, -lean.y / leanLength)
                val centre = Offset(size.width / 2f, size.height / 2f)
                val reach = size.maxDimension * 0.55f
                val lit = centre - unit * reach
                val away = centre + unit * reach

                // How lit the rim gets, tapering off as the panel comes back to flat so the
                // highlight arrives and leaves rather than snapping on.
                val heat = (leanLength * 2.4f).coerceAtMost(1f)
                val hot = (if (dark) 0.50f else 0.42f) * heat * opacity
                val outline = shape.createOutline(size, layoutDirection, this)

                // A wide, soft pass first: the glow the edge throws onto its surroundings.
                drawOutline(
                    outline = outline,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            sheen.copy(alpha = hot * 0.22f),
                            sheen.copy(alpha = hot * 0.06f),
                            Color.Transparent,
                        ),
                        start = lit,
                        end = away,
                    ),
                    style = Stroke(width = 3.dp.toPx()),
                )

                // Then the bright line itself, tight against the edge.
                drawOutline(
                    outline = outline,
                    brush = Brush.linearGradient(
                        colors = listOf(
                            sheen.copy(alpha = hot),
                            sheen.copy(alpha = hot * 0.18f),
                            Color.Transparent,
                        ),
                        start = lit,
                        end = away,
                    ),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }
        .then(
            if (!border) Modifier else Modifier.border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = (if (dark) 0.34f else 0.78f) * opacity),
                        Color.White.copy(alpha = 0.05f * opacity),
                        Color.White.copy(alpha = (if (dark) 0.12f else 0.32f) * opacity),
                    )
                ),
                shape = shape,
            )
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
    content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier = modifier.glass(shape, tint, strength, opacity, border, solid)) {
        Box(modifier = Modifier.padding(contentPadding), content = content)
    }
}
