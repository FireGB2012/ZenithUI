package com.gabriel.zenithui.ui.theme

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.rememberHaptics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

/**
 * How a pane of glass answers a finger, after the four buttons of the Liquid Glass design.
 *
 * - **Flex** swells and lights up from the fingertip: plain glass buttons, in sheets.
 * - **Jelly** squishes and wobbles back: tinted glass, the one button that moves you on.
 * - **Lean** swells and leans towards the finger: clear glass over the wallpaper or the dark.
 * - **Drag** swells most, for a lens that is picked up and moved: the thumb of a row of options.
 */
enum class GlassMotion { Flex, Jelly, Lean, Drag }

/** Where a pane is going under a finger: how big, and how far squashed (wider by it, shorter by it). */
@Immutable
data class PressTarget(val scale: Float, val squash: Float)

/** A spring as Compose takes it: damping ratio, and stiffness for a unit mass. */
@Immutable
data class SpringParams(val dampingRatio: Float, val stiffness: Float)

internal fun <T> SpringParams.spec(visibilityThreshold: T? = null): SpringSpec<T> =
    spring(dampingRatio, stiffness, visibilityThreshold)

/**
 * Where a pane goes when pressed, and back to when let go. With movement reduced the change in size
 * is two fifths of what it was and nothing squashes: a small, plain answer that a press was felt.
 */
fun pressTarget(motion: GlassMotion, pressed: Boolean, reduced: Boolean): PressTarget {
    if (!pressed) return PressTarget(1f, 0f)
    val scale = when (motion) {
        GlassMotion.Flex -> 1.075f
        GlassMotion.Jelly -> 0.95f
        GlassMotion.Lean -> 1.05f
        GlassMotion.Drag -> 1.09f
    }
    return PressTarget(
        scale = if (reduced) 1f + (scale - 1f) * REDUCED_SHARE else scale,
        squash = if (motion == GlassMotion.Jelly && !reduced) JELLY_SQUASH else 0f,
    )
}

/** The spring a pane's size moves on, pressed and let go alike. */
fun pressSpring(motion: GlassMotion, reduced: Boolean): SpringParams = when {
    reduced -> CRITICAL
    motion == GlassMotion.Jelly -> SpringParams(0.24f, 520f)
    motion == GlassMotion.Lean -> SpringParams(0.56f, 380f)
    else -> SpringParams(0.41f, 420f)
}

/** Jelly's squash: quick and loose, so it wobbles back. */
fun squashSpring(reduced: Boolean): SpringParams = if (reduced) CRITICAL else SpringParams(0.18f, 640f)

/** How a leaning pane follows the finger, and comes back upright. */
fun leanSpring(reduced: Boolean): SpringParams = if (reduced) CRITICAL else SpringParams(0.74f, 260f)

/** How a lens that was let go settles on the option nearest it. */
fun snapSpring(reduced: Boolean): SpringParams = if (reduced) SpringParams(1f, 700f) else SpringParams(0.57f, 700f)

/**
 * How far a Lean pane leans towards a [finger] on it, from its [centre]: a tenth of the way across
 * and a little more of the way down, never more than 12 dp across or 9 dp down. [density] is pixels
 * per dp; positions are in pixels.
 */
fun leanOffset(finger: Offset, centre: Offset, density: Float): Offset {
    val across = LEAN_MAX_X * density
    val down = LEAN_MAX_Y * density
    return Offset(
        ((finger.x - centre.x) * 0.10f).coerceIn(-across, across),
        ((finger.y - centre.y) * 0.14f).coerceIn(-down, down),
    )
}

/**
 * How far a lens being dragged at [velocityDp] dp a second stretches along its track: more the faster
 * it goes, up to a fifth. It is that much longer and about half that much shorter. None with movement
 * reduced.
 */
fun dragStretch(velocityDp: Float, reduced: Boolean): Float =
    if (reduced) 0f else min(abs(velocityDp) / STRETCH_SPEED, MAX_STRETCH)

/** How much shorter a stretched lens gets, for each part it gets longer. */
const val STRETCH_THIN = 0.55f

private const val REDUCED_SHARE = 0.4f
private const val JELLY_SQUASH = 0.05f
private const val LEAN_MAX_X = 12f
private const val LEAN_MAX_Y = 9f
private const val STRETCH_SPEED = 2600f
private const val MAX_STRETCH = 0.2f
private val CRITICAL = SpringParams(1f, 320f)

/**
 * A pane's press, as it moves: its size, its squash and its lean. Read while drawing, so a press
 * redraws what it moves and recomposes nothing.
 */
@Stable
internal class PressMotion(private val scope: CoroutineScope) {
    val scale = Animatable(1f)
    val squash = Animatable(0f)
    val lean = Animatable(Offset.Zero, Offset.VectorConverter, Offset.VisibilityThreshold)

    fun press(motion: GlassMotion, reduced: Boolean) = go(pressTarget(motion, pressed = true, reduced), motion, reduced)

    fun release(motion: GlassMotion, reduced: Boolean) {
        go(pressTarget(motion, pressed = false, reduced), motion, reduced)
        scope.launch { lean.animateTo(Offset.Zero, leanSpring(reduced).spec(Offset.VisibilityThreshold)) }
    }

    fun leanTo(offset: Offset, reduced: Boolean) {
        scope.launch { lean.animateTo(offset, leanSpring(reduced).spec(Offset.VisibilityThreshold)) }
    }

    /** Where a point on the pane as drawn now would be on the pane at rest. */
    fun atRest(position: Offset, centre: Offset): Offset {
        val s = scale.value
        return (position - centre) * s + centre + lean.value
    }

    /** Reads everything that moves the pane, so whoever calls it while drawing redraws as it moves. */
    fun watch() {
        scale.value
        squash.value
        lean.value
    }

    private fun go(target: PressTarget, motion: GlassMotion, reduced: Boolean) {
        scope.launch { scale.animateTo(target.scale, pressSpring(motion, reduced).spec()) }
        scope.launch { squash.animateTo(target.squash, squashSpring(reduced).spec()) }
    }
}

/**
 * Glass that moves like glass under a finger: [motion] says how. Only how it is drawn changes, never
 * where it is laid out, and the finger is watched, not taken, so taps, long presses and drags still
 * belong to whatever the pane is part of. One tick as the finger comes down, unless [haptic] is off
 * (see [pressTicks]).
 *
 * For a pane of glass use `glass(motion = ...)`, which does this and keeps the wallpaper behind the
 * pane where it is on screen while the pane swells and leans. This is for anything else.
 */
@Composable
fun Modifier.glassPress(motion: GlassMotion, enabled: Boolean = true, haptic: Boolean = true): Modifier {
    val scope = rememberCoroutineScope()
    val state = remember { PressMotion(scope) }
    return pressMotion(state, motion, enabled, haptic)
}

/**
 * Whether a press ticks as the finger comes down. A button does, to say it was felt. A home screen
 * icon does not ([haptic] off): a finger lands on one at the start of nearly every swipe between
 * pages, and a page turned that way must not buzz.
 */
internal fun pressTicks(haptic: Boolean): Boolean = haptic

@Composable
internal fun Modifier.pressMotion(state: PressMotion, motion: GlassMotion, enabled: Boolean, haptic: Boolean = true): Modifier {
    if (!enabled) return this
    val reduced = LocalMotionLevel.current.reduced
    val haptics by rememberUpdatedState(rememberHaptics())
    return this
        .graphicsLayer {
            val s = state.scale.value
            val q = state.squash.value
            scaleX = s * (1f + q)
            scaleY = s * (1f - q)
            val lean = state.lean.value
            translationX = lean.x
            translationY = lean.y
        }
        .pointerInput(motion, reduced, haptic) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                state.press(motion, reduced)
                if (pressTicks(haptic)) haptics.tick()
                val leans = motion == GlassMotion.Lean && !reduced
                val centre = Offset(size.width / 2f, size.height / 2f)
                try {
                    if (leans) state.leanTo(leanOffset(state.atRest(down.position, centre), centre, density), reduced)
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        if (leans) state.leanTo(leanOffset(state.atRest(change.position, centre), centre, density), reduced)
                        // Something round it took the finger, a scroll or the home screen picking
                        // the widget up: the press is over.
                        val after = awaitPointerEvent(PointerEventPass.Final)
                        if (after.changes.any { it.id == down.id && it.isConsumed }) break
                    }
                } finally {
                    state.release(motion, reduced)
                }
            }
        }
}
