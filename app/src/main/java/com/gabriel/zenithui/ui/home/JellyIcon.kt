package com.gabriel.zenithui.ui.home

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import com.gabriel.zenithui.ui.theme.GlassMotion
import com.gabriel.zenithui.ui.theme.PressLight
import com.gabriel.zenithui.ui.theme.glassPress

/**
 * How an icon on the home screen answers a finger, the home screen's fifth twist: it squishes and
 * lights up from under the finger, then springs back with a wobble ([GlassMotion.Jelly], on the
 * springs every jelly press has, and a small, steady press with movement reduced). Home icons always
 * do this, whatever Pressing is set to; the drawer and a folder's panel keep Pressing.
 *
 * It does not tick as the finger comes down: a finger lands on an icon at the start of most swipes
 * between pages, and a page turned that way must not buzz. A swipe or a carry that takes the finger
 * lets go of the press, so no icon is left squashed. The light is drawn over the icon's own pixels,
 * inside [shape], and only while it shows.
 *
 * It goes after `launchOrigin`, which then measures the icon at rest, so an app still opens out of
 * its icon however squashed the icon was as it was tapped.
 */
@Composable
fun Modifier.jellyIcon(shape: Shape): Modifier {
    val scope = rememberCoroutineScope()
    val light = remember { PressLight(scope) }
    return this
        .glassPress(GlassMotion.Jelly, haptic = false)
        .pointerInput(light) {
            // Watches the finger without taking it, as the press does: the tap, the hold and the
            // carry are still the icon's.
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                light.press(down.position)
                try {
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        light.at = change.position
                        // A swipe between pages, or the page picking the icon up, has taken the finger.
                        val after = awaitPointerEvent(PointerEventPass.Final)
                        if (after.changes.any { it.id == down.id && it.isConsumed }) break
                    }
                } finally {
                    light.release()
                }
            }
        }
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            onDrawWithContent {
                drawContent()
                // Read here, while drawing, so the light comes and goes without composing anything.
                val touch = light.touch(size)
                if (touch.strength > 0f) {
                    // Wider than a pane's, since an icon is small: from under the fingertip across
                    // most of the icon as it spreads.
                    val reach = size.maxDimension * JELLY_LIGHT_BASE + touch.reach * JELLY_LIGHT_SPREAD
                    drawOutline(
                        outline,
                        Brush.radialGradient(
                            0f to Color.White.copy(alpha = JELLY_LIGHT * touch.strength),
                            1f to Color.Transparent,
                            center = touch.at,
                            radius = reach.coerceAtLeast(1f),
                        ),
                    )
                }
            }
        }
}

/** How bright the light under a finger on an icon is at its brightest, as white. */
private const val JELLY_LIGHT = 0.42f

/**
 * How far it reaches: [JELLY_LIGHT_BASE] of the icon at once, and [JELLY_LIGHT_SPREAD] times the
 * reach the finger's light on a pane has, which grows as the finger stays.
 */
private const val JELLY_LIGHT_BASE = 0.3f
private const val JELLY_LIGHT_SPREAD = 1.4f
