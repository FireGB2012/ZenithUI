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
        }
        .then(
            if (!border) Modifier else Modifier.border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    listOf(
                        Color.White.copy(alpha = (if (dark) 0.42f else 0.85f) * opacity),
                        Color.White.copy(alpha = 0.05f * opacity),
                        Color.White.copy(alpha = (if (dark) 0.14f else 0.35f) * opacity),
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
