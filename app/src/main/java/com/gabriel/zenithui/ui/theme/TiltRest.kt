package com.gabriel.zenithui.ui.theme

import androidx.compose.ui.geometry.Offset
import kotlin.math.pow

/**
 * How the tilt is read, kept apart from the sensor so that each rule can be checked on its own.
 *
 * The light on the glass ([LocalTilt]) follows which way the phone leans, however it is held. What
 * floats over the wallpaper ([LocalParallax]) follows which way it leans from the way it is being
 * held: the rest pose, which slowly follows the phone. So holding it as you normally do, sitting,
 * lying down or standing up, leaves everything in its place and only moving it floats things; hold
 * it at a new angle for a few seconds and that becomes the rest.
 */
object TiltRest {
    /**
     * How far the phone has to lean from its rest, in the tilt's own units (1 is lying on its side),
     * for things to float all the way: about 25 degrees as a phone is usually held, as in the
     * mockup. Past it they float no further.
     */
    const val RANGE = 0.35f

    /**
     * A change smaller than this is the sensor's own noise, or a spring all but settled, and is not
     * worth drawing anything again for: see [still].
     */
    const val DEAD_ZONE = 0.012f

    /**
     * The rest pose [dtSec] later, the phone having read [reading] all that time: it closes half of
     * the gap in every [halfLifeSec], however the time is cut up into frames.
     */
    fun follow(rest: Offset, reading: Offset, dtSec: Float, halfLifeSec: Float = 2.5f): Offset {
        if (dtSec <= 0f) return rest
        if (halfLifeSec <= 0f) return reading
        val kept = 0.5f.pow(dtSec / halfLifeSec)
        return reading + (rest - reading) * kept
    }

    /**
     * How far [reading] leans from [rest], as a share of [RANGE] on each axis and never more than all
     * of it either way: what things float by.
     */
    fun relative(reading: Offset, rest: Offset): Offset = Offset(
        ((reading.x - rest.x) / RANGE).coerceIn(-1f, 1f),
        ((reading.y - rest.y) / RANGE).coerceIn(-1f, 1f),
    )

    /**
     * True when [next] is within [deadZone] of [previous]: so small a change that nothing need be
     * drawn again for it. A phone lying still reads within it of itself, so it writes nothing.
     */
    fun still(previous: Offset, next: Offset, deadZone: Float = DEAD_ZONE): Boolean =
        (next - previous).getDistance() < deadZone

    /**
     * How far, in pixels, something standing [depthDp] above the wallpaper moves for [parallax]:
     * against the tilt, as something nearer to you than what is behind it seems to, and all of
     * [depthDp] when the phone leans the whole [RANGE].
     */
    fun parallaxShift(parallax: Offset, depthDp: Float, density: Float): Offset {
        val reach = depthDp * density
        // Adding 0 turns -0 into 0, so that no tilt gives exactly Offset.Zero: an Offset compares
        // the bits of its numbers, and -0 and 0 have different bits.
        return Offset(-parallax.x * reach + 0f, -parallax.y * reach + 0f)
    }
}
