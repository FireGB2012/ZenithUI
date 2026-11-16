package com.gabriel.zenithui.ui.common

import androidx.compose.animation.core.SpringSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sqrt

class MotionTest {

    /** How far past its target a spring goes on its first swing, as a share of the way it came. */
    private fun overshoot(dampingRatio: Float): Double {
        val z = dampingRatio.toDouble()
        return if (z >= 1.0) 0.0 else exp(-PI * z / sqrt(1.0 - z * z))
    }

    /** How long one swing of a spring this stiff takes with nothing slowing it: Apple's "response". */
    private fun response(stiffness: Float): Double = 2 * PI / sqrt(stiffness.toDouble())

    /** Every spring the launcher shares, by name. */
    private val shared: Map<String, SpringSpec<*>> = mapOf(
        "snappy" to Motion.snappy<Float>(),
        "smooth" to Motion.smooth<Float>(),
        "bouncy" to Motion.bouncy<Float>(),
        "cell" to Motion.cell,
        "press" to Motion.press<Float>(),
        "lift" to Motion.lift<Float>(),
        "drop" to Motion.drop<Float>(),
        "open" to Motion.open<Float>(),
        "close" to Motion.close<Float>(),
        "sheet" to Motion.sheet<Float>(),
        "page" to Motion.page<Float>(),
        "rise" to Motion.rise<Float>(),
        "riseGently" to Motion.riseGently<Float>(),
        "roll" to Motion.roll,
        "edge" to Motion.edge,
    )

    @Test
    fun `every spring settles as Apple's do, with one small bounce at most`() {
        for ((name, spring) in shared) {
            val past = overshoot(spring.dampingRatio)
            // A tenth past its rest at most, and the swing back after that too small to see: a
            // spring that goes on wobbling reads as jelly, not as something liquid.
            assertTrue("$name goes past by $past", past <= 0.1)
            assertTrue("$name swings back by ${past * past}", past * past <= 0.01)
            // Quick enough to answer a finger, not so quick that it twitches: a response between a
            // fifth and half a second, as Apple's springs keep to.
            val takes = response(spring.stiffness)
            assertTrue("$name takes ${takes}s", takes in 0.2..0.5)
        }
    }

    @Test
    fun `nothing but a press is bouncier than Apple's bounciest spring`() {
        for ((name, spring) in shared - "press") {
            assertTrue("$name is damped at ${spring.dampingRatio}", spring.dampingRatio >= 0.7f)
        }
        // A press springs back with one small bounce, as the home screen's jelly icons do.
        assertTrue(Motion.press<Float>().dampingRatio >= 0.6f)
    }

    @Test
    fun `what would show an edge if it went past never goes past`() {
        for (spring in listOf(Motion.smooth<Float>(), Motion.sheet<Float>(), Motion.close<Float>(), Motion.page<Float>())) {
            assertTrue(spring.dampingRatio >= 0.9f)
        }
    }

    @Test
    fun `small flips are quicker than the luxurious spring and picked up things quicker than put down ones are slow`() {
        assertTrue(Motion.snappy<Float>().stiffness > Motion.bouncy<Float>().stiffness)
        assertTrue(Motion.press<Float>().stiffness >= Motion.drop<Float>().stiffness)
        // Things rising into place come in gently, never on a press's bounce.
        assertTrue(Motion.rise<Float>().dampingRatio > Motion.press<Float>().dampingRatio)
        assertEquals(Motion.snappy<Float>().dampingRatio, Motion.roll.dampingRatio, 0f)
    }
}
