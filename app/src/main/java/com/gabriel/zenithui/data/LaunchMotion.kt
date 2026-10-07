package com.gabriel.zenithui.data

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Where things are part way through an app opening or closing. Plain arithmetic, apart from any
 * screen, so the launch card, the preview in Settings and the tests all work from the same sums.
 */
object LaunchMotion {

    /** The card at one moment: its edges and how round its corners are, in pixels. */
    data class Frame(val left: Float, val top: Float, val right: Float, val bottom: Float, val radius: Float) {
        val width: Float get() = right - left
        val height: Float get() = bottom - top
        val centreX: Float get() = (left + right) / 2f
        val centreY: Float get() = (top + bottom) / 2f
    }

    /**
     * The card between the icon it came from and the whole screen. [x] and [y] say how far its
     * middle has travelled across and down and [size] how far it has grown, each 0 at the icon
     * and 1 at the screen, and each free to run a little past either end as a spring does. The
     * path bows upward by [arc] pixels at its half way point, and not at all at either end, so
     * the card still starts exactly on the icon and ends exactly on the screen.
     */
    fun frame(
        fromLeft: Float, fromTop: Float, fromRight: Float, fromBottom: Float,
        screenWidth: Float, screenHeight: Float,
        x: Float, y: Float, size: Float,
        arc: Float,
        startRadius: Float, endRadius: Float,
    ): Frame {
        val grown = size.coerceAtLeast(0f)
        val width = mix(fromRight - fromLeft, screenWidth, grown)
        val height = mix(fromBottom - fromTop, screenHeight, grown)
        val travelled = ((x + y) / 2f).coerceIn(0f, 1f)
        val centreX = mix((fromLeft + fromRight) / 2f, screenWidth / 2f, x)
        val centreY = mix((fromTop + fromBottom) / 2f, screenHeight / 2f, y) - arc * sin(PI.toFloat() * travelled)
        return Frame(
            left = centreX - width / 2f,
            top = centreY - height / 2f,
            right = centreX + width / 2f,
            bottom = centreY + height / 2f,
            radius = mix(startRadius, endRadius, grown.coerceAtMost(1f)),
        )
    }

    /**
     * How far something [dx], [dy] away from where it all starts is pushed out of the way, across
     * and down, when the thing at the start is pushed [amount]. The nearest are pushed most and
     * it falls away with distance, to a quarter at twice [reach]; the thing at the start itself,
     * having no direction to go in, stays put.
     */
    fun push(dx: Float, dy: Float, amount: Float, reach: Float): Pair<Float, Float> {
        val distance = hypot(dx, dy)
        if (distance < 1f || amount == 0f || reach <= 0f) return 0f to 0f
        val near = distance / reach
        val strength = amount / (1f + 0.75f * near * near)
        return dx / distance * strength to dy / distance * strength
    }

    /**
     * How fast something at rest on a spring has to be sent off for it to get [distance] away
     * before the spring brings it back: what a knock is. Starting it there instead would be a
     * jump; starting it moving is a push. [stiffness] and [damping] are the spring's own.
     */
    fun knockVelocity(distance: Float, stiffness: Float, damping: Float): Float {
        if (distance == 0f || stiffness <= 0f) return 0f
        val pull = sqrt(stiffness)
        val reached = if (damping < 0.999f) {
            val swing = sqrt(1f - damping * damping)
            exp(-damping / swing * atan2(swing, damping))
        } else {
            // One that does not swing at all still gets out before it comes back: to 1/e of the way.
            exp(-1f)
        }
        return distance * pull / reached
    }

    /**
     * The point the home screen zooms about, as a share of its width or height: the middle of
     * the screen when [towards] is 0, the icon's own place when it is 1.
     */
    fun pivot(icon: Float, screen: Float, towards: Float): Float {
        if (screen <= 0f) return 0.5f
        return mix(0.5f, (icon / screen).coerceIn(0f, 1f), towards.coerceIn(0f, 1f))
    }

    private fun mix(from: Float, to: Float, amount: Float) = from + (to - from) * amount
}
