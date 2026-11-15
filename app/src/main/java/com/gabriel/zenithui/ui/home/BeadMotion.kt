package com.gabriel.zenithui.ui.home

import kotlin.math.PI

/**
 * How edit mode's glass beads move, and the soap bubble something taken off the home screen pops
 * into: plain arithmetic over time, read while drawing. Each is a fraction of its own length, 0 at
 * the start and 1 at the end, so a frame never has to recompose anything to move them.
 */
internal object BeadMotion {

    /** How long a bead takes to well up out of its corner, and to sink back into it. */
    const val WELL_UP_MS = 550
    const val SINK_MS = 300

    /** How long what pops takes to swell and go; its ring starts a moment in and outlasts it. */
    const val POP_MS = 200
    const val RING_DELAY_MS = 80
    const val RING_MS = 360

    /** How long the droplets take to fly off, or, on Undo, to fly back in. */
    const val DROPLETS_MS = 430

    /** How long something brought back by Undo takes to form again in its place. */
    const val BACK_MS = 640

    /** How long the ring takes to close in on a spot as something forms again there. */
    const val RING_IN_MS = 300

    /** How many droplets fly back in as something forms again. */
    const val BACK_DROPLETS = 6

    /** A bead as it is drawn: how seen it is, how far off its rest it is in dp, and how stretched. */
    data class Pose(val alpha: Float, val dx: Float, val dy: Float, val scaleX: Float, val scaleY: Float)

    /**
     * The bead welling up out of the corner it sits on, as a drop of water does: tiny and tucked into
     * the corner, then out, wide, tall, and still.
     */
    fun wellUp(t: Float): Pose {
        val p = t.coerceIn(0f, 1f)
        return Pose(
            alpha = keyframes(p, WELL_TIMES, floatArrayOf(0f, 1f, 1f, 1f, 1f)),
            dx = keyframes(p, WELL_TIMES, floatArrayOf(5f, 2f, 0f, 0f, 0f)),
            dy = keyframes(p, WELL_TIMES, floatArrayOf(5f, 2f, 0f, 0f, 0f)),
            scaleX = keyframes(p, WELL_TIMES, floatArrayOf(0.12f, 0.6f, 1.13f, 0.96f, 1f)),
            scaleY = keyframes(p, WELL_TIMES, floatArrayOf(0.12f, 0.78f, 0.92f, 1.05f, 1f)),
        )
    }

    private val WELL_TIMES = floatArrayOf(0f, 0.35f, 0.62f, 0.82f, 1f)

    /** Sinking back into the corner, as edit mode ends: smaller, quicker, and gone. */
    fun sink(t: Float): Pose {
        val p = t.coerceIn(0f, 1f)
        // Slow to start and quick to finish, as a drop drawn back in.
        val e = p * p * p
        val s = 1f - e
        return Pose(alpha = 1f - ((p - 0.2f) / 0.8f).coerceIn(0f, 1f), dx = 0f, dy = 0f, scaleX = s, scaleY = s)
    }

    /** What pops: a quick swell, then it shrinks to nothing as its bubble bursts. */
    fun popScale(t: Float): Float = keyframes(t.coerceIn(0f, 1f), POP_TIMES, floatArrayOf(1f, 1.14f, 0.15f))

    fun popAlpha(t: Float): Float = keyframes(t.coerceIn(0f, 1f), POP_TIMES, floatArrayOf(1f, 1f, 0f))

    /** How much brighter it is as it goes, as a bubble catches the light just as it bursts. */
    fun popGlow(t: Float): Float = keyframes(t.coerceIn(0f, 1f), POP_TIMES, floatArrayOf(1f, 1.3f, 1.6f))

    private val POP_TIMES = floatArrayOf(0f, 0.45f, 1f)

    /** Something brought back by Undo: unseen while its droplets fly in, then forming with a wobble. */
    fun backScale(t: Float): Float = keyframes(t.coerceIn(0f, 1f), BACK_TIMES, floatArrayOf(0.15f, 0.15f, 1.12f, 0.97f, 1f))

    fun backAlpha(t: Float): Float = keyframes(t.coerceIn(0f, 1f), BACK_TIMES, floatArrayOf(0f, 0f, 1f, 1f, 1f))

    private val BACK_TIMES = floatArrayOf(0f, 0.42f, 0.7f, 0.86f, 1f)

    /**
     * How long something takes to come up to its full wiggle as the wave of light reaches it, the
     * jolt it gives as it does, and how long its wiggle takes to die away as the calmer wave passes.
     */
    const val WAKE_MS = 200
    const val JOLT_MS = 380
    const val CALM_MS = 520

    /** Waking, it gives a small rock with a soft squash: a little wide, then a little tall, then still. */
    fun joltX(t: Float, widget: Boolean): Float {
        val k = if (widget) WIDGET_JOLT else 1f
        return keyframes(t.coerceIn(0f, 1f), JOLT_TIMES, floatArrayOf(1f, 1f + 0.07f * k, 1f - 0.025f * k, 1f))
    }

    fun joltY(t: Float, widget: Boolean): Float {
        val k = if (widget) WIDGET_JOLT else 1f
        return keyframes(t.coerceIn(0f, 1f), JOLT_TIMES, floatArrayOf(1f, 1f - 0.06f * k, 1f + 0.03f * k, 1f))
    }

    private val JOLT_TIMES = floatArrayOf(0f, 1f / 3f, 2f / 3f, 1f)

    /** A widget jolts this much as hard as an icon: it is big, and the same squash would look like a wobble. */
    private const val WIDGET_JOLT = 0.3f

    /** A smaller spray when several pop in a row, so a run of removals does not turn to confetti. */
    fun droplets(msSinceLastPop: Long): Int = if (msSinceLastPop < 1000) 3 else 6

    /**
     * One droplet: which way it flies (radians), how far past the bubble's edge (0..1 of its
     * reach) and how big it is (0..1 of its range). Each of [n] goes its own way round the circle,
     * the same way every time.
     */
    data class Droplet(val angle: Float, val reach: Float, val size: Float)

    fun droplet(i: Int, n: Int): Droplet = Droplet(
        angle = (i.toFloat() / n) * TWO_PI + scatter(i, 1) * 0.8f - 0.4f,
        reach = scatter(i, 2),
        size = scatter(i, 3),
    )

    /** A number in 0..1 that is the same for the same [i] and [salt], and different between them. */
    private fun scatter(i: Int, salt: Int): Float {
        var h = i * 73856093 xor salt * 19349663
        h = h xor (h ushr 13)
        h *= 0x5bd1e995
        h = h xor (h ushr 15)
        return (h and 0xFFFF) / 65535f
    }

    /** Eased between [values] at [times]; each stretch eases out, as a drop slows into its shape. */
    private fun keyframes(t: Float, times: FloatArray, values: FloatArray): Float {
        for (k in 1 until times.size) {
            if (t <= times[k]) {
                val span = times[k] - times[k - 1]
                val f = if (span <= 0f) 1f else ((t - times[k - 1]) / span).coerceIn(0f, 1f)
                val eased = 1f - (1f - f) * (1f - f)
                return values[k - 1] + (values[k] - values[k - 1]) * eased
            }
        }
        return values.last()
    }

    private const val TWO_PI = (2.0 * PI).toFloat()
}
