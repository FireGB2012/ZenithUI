package com.gabriel.zenithui.ui.common

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import kotlinx.coroutines.delay

/**
 * One motion vocabulary for the whole launcher. Springs, not curves — nothing should look like it
 * is following a timeline.
 */
object Motion {
    /** Small state flips: quick to start, with a visible settle. */
    fun <T> snappy() = spring<T>(dampingRatio = 0.48f, stiffness = 260f)

    /** Anything where an overshoot would expose an edge: sheets, the drawer. Slow, no bounce. */
    fun <T> smooth() = spring<T>(dampingRatio = 0.92f, stiffness = 190f)

    /** The luxurious one: slow, and it rocks into place. */
    fun <T> bouncy() = spring<T>(dampingRatio = 0.40f, stiffness = 200f)

    /** Icons gliding to a new cell, with a little overshoot as they land. */
    val cell = spring<IntOffset>(dampingRatio = 0.58f, stiffness = 240f)

    const val ENTER_STAGGER_MS = 34
    const val MAX_STAGGER_STEPS = 16
}

/** Squeezes anything under a finger and lets it spring back — works alongside tap and drag. */
@Composable
fun Modifier.pressBounce(pressedScale: Float = 0.88f, enabled: Boolean = true): Modifier {
    if (!enabled) return this
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.36f, stiffness = 420f),
        label = "press",
    )
    return this
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    pressed = true
                    waitForUpOrCancellation()
                    pressed = false
                }
            }
        }
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
}

/** Fades, lifts and scales content in once, optionally staggered behind its neighbours. */
@Composable
fun Modifier.appearIn(
    delayMillis: Int = 0,
    fromScale: Float = 0.84f,
    riseFrom: Float = 28f,
): Modifier {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        if (delayMillis > 0) delay(delayMillis.toLong())
        progress.animateTo(1f, spring(dampingRatio = 0.55f, stiffness = 170f))
    }
    return this.graphicsLayer {
        val value = progress.value
        alpha = value
        val scale = fromScale + (1f - fromScale) * value
        scaleX = scale
        scaleY = scale
        translationY = (1f - value) * riseFrom
    }
}

/** A light sweep that travels across the content, used on the wordmark. */
@Composable
fun Modifier.shimmer(widthFraction: Float = 0.34f, periodMillis: Int = 2800): Modifier {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val position by transition.animateFloat(
        initialValue = -0.6f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(periodMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "sweep",
    )
    return this
        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
        .drawWithContent {
            drawContent()
            val centre = position * size.width
            val half = size.width * widthFraction / 2f
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.75f), Color.Transparent),
                    start = Offset(centre - half, 0f),
                    end = Offset(centre + half, size.height),
                ),
                blendMode = BlendMode.SrcAtop,
            )
        }
}

/** Stagger delay for the item at [index], capped so long lists never crawl in. */
fun staggerDelay(index: Int): Int =
    minOf(index, Motion.MAX_STAGGER_STEPS) * Motion.ENTER_STAGGER_MS
