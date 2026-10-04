package com.gabriel.zenithui.ui.common

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
    val cell = spring<IntOffset>(dampingRatio = 0.66f, stiffness = 420f)

    // By what is happening, so two things that are the same kind of event move the same way.

    /** Something squeezed under a finger and let go. */
    fun <T> press() = spring<T>(dampingRatio = 0.36f, stiffness = 420f)

    /** Something picked up: it comes off the page quickly and rocks once. */
    fun <T> lift() = spring<T>(dampingRatio = 0.38f, stiffness = 260f)

    /** Something put down: firm, with one small bounce. */
    fun <T> drop() = spring<T>(dampingRatio = 0.62f, stiffness = 380f)

    /** Something growing out of what was tapped: a folder, a preview. */
    fun <T> open() = spring<T>(dampingRatio = 0.78f, stiffness = 300f)

    /** The same thing going back: quicker than it came, and it does not overshoot. */
    fun <T> close() = spring<T>(dampingRatio = 1f, stiffness = 420f)

    /** Sheets and pages coming up from an edge. */
    fun <T> sheet() = smooth<T>()

    /** One page of something giving way to the next. */
    fun <T> page() = spring<T>(dampingRatio = 1f, stiffness = 420f)

    /** Colours changing with the theme, in milliseconds: long enough to watch, short enough not to wait for. */
    const val THEME_MS = 420

    /** What stands in for a spring when movement is reduced: a fade this long, at most. */
    const val REDUCED_MS = 150

    const val ENTER_STAGGER_MS = 34
    const val MAX_STAGGER_STEPS = 16

    /** Fast out of the gate, then a long, soft landing. For things arriving that should feel expensive. */
    val emphasized = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
}

/**
 * False when the phone's "Remove animations" is on. Springs and tweens already honour that on
 * their own; this is for the things that never end by themselves, like drifting light, which
 * would otherwise keep a frame loop running for nothing.
 */
@Composable
fun rememberAnimationsEnabled(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
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

/**
 * Fades, lifts and scales content in, optionally staggered behind its neighbours. Once, unless
 * [replay] changes, when it plays again: the app drawer's rows rise in every time it opens, not
 * only the first. With movement reduced it is a quick fade where the content stands, all at once.
 */
@Composable
fun Modifier.appearIn(
    delayMillis: Int = 0,
    fromScale: Float = 0.84f,
    riseFrom: Float = 28f,
    replay: Any? = Unit,
    /** Settles without the bounce: for whole blocks of a page, where a wobble would be too much. */
    gentle: Boolean = false,
): Modifier {
    val reduced = LocalMotionLevel.current.reduced
    val progress = remember(replay) { Animatable(0f) }
    LaunchedEffect(replay) {
        if (reduced) {
            progress.animateTo(1f, tween(Motion.REDUCED_MS))
        } else {
            if (delayMillis > 0) delay(delayMillis.toLong())
            progress.animateTo(
                1f,
                if (gentle) spring(dampingRatio = 0.86f, stiffness = 240f) else spring(dampingRatio = 0.55f, stiffness = 170f),
            )
        }
    }
    return this.graphicsLayer {
        val value = progress.value
        alpha = value.coerceIn(0f, 1f)
        if (!reduced) {
            val scale = fromScale + (1f - fromScale) * value
            scaleX = scale
            scaleY = scale
            translationY = (1f - value) * riseFrom
        }
    }
}

/**
 * A short shake of the head, played each time [trigger] changes: "no", without a word. Nothing
 * happens when it first appears, and nothing at all with movement reduced.
 */
@Composable
fun Modifier.shake(trigger: Int): Modifier {
    val reduced = LocalMotionLevel.current.reduced
    val offset = remember { Animatable(0f) }
    val seen = remember { intArrayOf(trigger) }
    LaunchedEffect(trigger) {
        if (trigger == seen[0]) return@LaunchedEffect
        seen[0] = trigger
        if (reduced) return@LaunchedEffect
        offset.snapTo(0f)
        offset.animateTo(
            0f,
            keyframes {
                durationMillis = 420
                -1f at 50
                0.85f at 130
                -0.55f at 210
                0.3f at 290
                0f at 420
            },
        )
    }
    return this.graphicsLayer { translationX = offset.value * 10.dp.toPx() }
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
