package com.gabriel.zenithui.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BeadMotionTest {

    private val eps = 0.0001f

    @Test
    fun `a bead wells up out of its corner, small and unseen at first, and comes to rest`() {
        val start = BeadMotion.wellUp(0f)
        assertEquals(0f, start.alpha, eps)
        assertTrue(start.scaleX < 0.2f && start.scaleY < 0.2f)
        // It starts tucked into the corner of what it sits on, below and to the right of where it rests.
        assertTrue(start.dx > 0f && start.dy > 0f)
        val end = BeadMotion.wellUp(1f)
        assertEquals(BeadMotion.Pose(1f, 0f, 0f, 1f, 1f), end)
    }

    @Test
    fun `on its way it swells past its size, wide then tall, like a drop settling`() {
        val wide = (0..100).map { BeadMotion.wellUp(it / 100f) }
        assertTrue(wide.any { it.scaleX > 1.05f && it.scaleY < 1f })
        assertTrue(wide.any { it.scaleY > 1.02f && it.scaleX < 1f })
        // Never out of the range a drop of glass could stretch to.
        assertTrue(wide.all { it.scaleX in 0f..1.2f && it.scaleY in 0f..1.2f && it.alpha in 0f..1f })
    }

    @Test
    fun `what pops swells a little, then is gone`() {
        assertEquals(1f, BeadMotion.popScale(0f), eps)
        assertTrue((0..100).any { BeadMotion.popScale(it / 100f) > 1.1f })
        assertTrue(BeadMotion.popScale(1f) < 0.2f)
        assertEquals(0f, BeadMotion.popAlpha(1f), eps)
    }

    @Test
    fun `what pops brightens as it goes, as a bubble catches the light`() {
        assertEquals(1f, BeadMotion.popGlow(0f), eps)
        val glow = (0..100).map { BeadMotion.popGlow(it / 100f) }
        assertTrue(glow.zipWithNext().all { (a, b) -> b >= a })
        assertEquals(1.6f, BeadMotion.popGlow(1f), eps)
    }

    @Test
    fun `what comes back on Undo forms again with a wobble and rests at its size`() {
        assertEquals(0f, BeadMotion.backAlpha(0f), eps)
        assertTrue((0..100).any { BeadMotion.backScale(it / 100f) > 1.08f })
        assertTrue((0..100).any { BeadMotion.backScale(it / 100f) in 0.9f..0.99f })
        assertEquals(1f, BeadMotion.backScale(1f), eps)
        assertEquals(1f, BeadMotion.backAlpha(1f), eps)
    }

    @Test
    fun `woken by the wave, a thing gives a little jolt, wide then tall, and comes to rest`() {
        assertEquals(1f, BeadMotion.joltX(0f, widget = false), eps)
        assertEquals(1f, BeadMotion.joltY(0f, widget = false), eps)
        assertEquals(1f, BeadMotion.joltX(1f, widget = false), eps)
        assertEquals(1f, BeadMotion.joltY(1f, widget = false), eps)
        val steps = (0..100).map { it / 100f }
        val wide = steps.indexOfFirst { BeadMotion.joltX(it, false) > 1.05f && BeadMotion.joltY(it, false) < 0.97f }
        val tall = steps.indexOfFirst { BeadMotion.joltY(it, false) > 1.02f && BeadMotion.joltX(it, false) < 1f }
        assertTrue(wide >= 0 && tall > wide)
    }

    @Test
    fun `a widget's jolt is gentler than an icon's`() {
        val steps = (0..100).map { it / 100f }
        val icon = steps.maxOf { kotlin.math.abs(BeadMotion.joltX(it, widget = false) - 1f) }
        val widget = steps.maxOf { kotlin.math.abs(BeadMotion.joltX(it, widget = true) - 1f) }
        assertTrue(widget < icon * 0.5f)
    }

    @Test
    fun `several pops in a row throw fewer droplets each`() {
        assertEquals(6, BeadMotion.droplets(msSinceLastPop = 5_000))
        assertEquals(3, BeadMotion.droplets(msSinceLastPop = 300))
    }

    @Test
    fun `droplets fly every way round, each its own, and the same each time`() {
        val drops = (0 until 6).map { BeadMotion.droplet(it, 6) }
        assertEquals(drops, (0 until 6).map { BeadMotion.droplet(it, 6) })
        // Spread round the circle, each further round than the one before.
        val angles = drops.map { it.angle }
        assertTrue(angles.zipWithNext().all { (a, b) -> b > a })
        assertTrue(drops.all { it.reach in 0f..1f && it.size in 0f..1f })
    }
}
