package com.gabriel.zenithui.ui.common

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.IconLoader
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import kotlin.math.cos
import kotlin.math.sin

val LocalIconLoader = staticCompositionLocalOf<IconLoader> { error("IconLoader not provided") }

/** Slow drifting light, used behind onboarding and the app drawer. */
@Composable
fun AuroraBackdrop(modifier: Modifier = Modifier, intensity: Float = 1f) {
    val palette = LocalPalette.current
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(26_000, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val accent = palette.accent
    val accentAlt = palette.accentAlt
    val tint = palette.glassTint

    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(palette.backdrop))
            .drawBehind {
                val orbs = listOf(
                    Triple(accent, Offset(0.22f + 0.10f * cos(phase), 0.16f + 0.06f * sin(phase * 1.3f)), 0.80f),
                    Triple(accentAlt, Offset(0.86f + 0.08f * sin(phase * 0.8f), 0.36f + 0.09f * cos(phase)), 0.66f),
                    Triple(tint, Offset(0.44f + 0.12f * cos(phase * 0.6f), 0.88f + 0.05f * sin(phase)), 0.95f),
                )
                orbs.forEach { (color, unit, radiusFraction) ->
                    val center = Offset(unit.x * size.width, unit.y * size.height)
                    val radius = radiusFraction * size.width
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(color.copy(alpha = 0.32f * intensity), Color.Transparent),
                            center = center,
                            radius = radius,
                        ),
                        radius = radius,
                        center = center,
                    )
                }
            }
    )
}

@Composable
fun AppIcon(
    key: String,
    label: String,
    size: Dp,
    modifier: Modifier = Modifier,
    shape: Shape = ZenithShapes.icon,
) {
    val loader = LocalIconLoader.current
    val version by loader.version.collectAsState()
    val px = with(LocalDensity.current) { size.roundToPx() }.coerceIn(48, 288)
    val bitmap by produceState<ImageBitmap?>(initialValue = null, key, px, version) {
        value = loader.load(key, px)
    }
    val palette = LocalPalette.current

    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        val bmp = bitmap
        val appear by animateFloatAsState(
            targetValue = if (bmp != null) 1f else 0f,
            animationSpec = tween(340),
            label = "iconFade",
        )
        if (bmp != null) {
            Image(
                bitmap = bmp,
                contentDescription = label,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = appear
                        val s = 0.92f + 0.08f * appear
                        scaleX = s
                        scaleY = s
                    },
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(shape)
                    .background(
                        Brush.linearGradient(
                            listOf(palette.accent.copy(alpha = 0.55f), palette.accentAlt.copy(alpha = 0.35f))
                        )
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = (size.value * 0.36f).sp,
                )
            }
        }
    }
}

@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = false,
    leading: (@Composable () -> Unit)? = null,
) {
    val palette = LocalPalette.current
    val contentAlpha = if (enabled) 1f else 0.4f
    Box(
        modifier = modifier
            .height(56.dp)
            .pressBounce(0.96f, enabled = enabled)
            .graphicsLayer { alpha = contentAlpha }
            .then(
                if (filled) Modifier
                    .clip(ZenithShapes.pill)
                    .background(Brush.horizontalGradient(listOf(palette.accent, palette.accentAlt)))
                else Modifier.glass(ZenithShapes.pill, strength = 0.9f)
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        val contentColor = if (filled) Color(0xFF0A0B12) else palette.onGlass
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.padding(horizontal = 28.dp),
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                leading?.invoke()
                Text(
                    text = text,
                    color = contentColor,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Text(
        text = text.uppercase(),
        color = palette.onGlassMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.4.sp,
        modifier = modifier,
    )
}

val PillShape: Shape = RoundedCornerShape(percent = 50)
