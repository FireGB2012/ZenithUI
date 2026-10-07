package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchMotionTest {

    // An icon 168 pixels square, its top left at 120, 900, on a 1344 by 2992 screen.
    private fun frame(x: Float, y: Float, size: Float, arc: Float = 0f) =
        LaunchMotion.frame(120f, 900f, 288f, 1068f, 1344f, 2992f, x, y, size, arc, 40f, 84f)

    @Test
    fun `the card starts exactly on the icon and ends exactly on the screen`() {
        assertEquals(LaunchMotion.Frame(120f, 900f, 288f, 1068f, 40f), frame(0f, 0f, 0f))
        assertEquals(LaunchMotion.Frame(0f, 0f, 1344f, 2992f, 84f), frame(1f, 1f, 1f))
        // Whatever the arc: it is nothing at either end.
        assertEquals(frame(0f, 0f, 0f), frame(0f, 0f, 0f, arc = 60f))
        val full = frame(1f, 1f, 1f, arc = 60f)
        assertEquals(0f, full.top, 0.01f)
        assertEquals(2992f, full.bottom, 0.01f)
    }

    @Test
    fun `half way, it is half way`() {
        val half = frame(0.5f, 0.5f, 0.5f)
        assertEquals((204f + 672f) / 2f, half.centreX, 0.01f)
        assertEquals((984f + 1496f) / 2f, half.centreY, 0.01f)
        assertEquals((168f + 1344f) / 2f, half.width, 0.01f)
        assertEquals((168f + 2992f) / 2f, half.height, 0.01f)
        assertEquals(62f, half.radius, 0.01f)
    }

    @Test
    fun `across, down and growth each go their own way`() {
        val acrossFirst = frame(1f, 0f, 0f)
        assertEquals(672f, acrossFirst.centreX, 0.01f)
        assertEquals(984f, acrossFirst.centreY, 0.01f)
        assertEquals(168f, acrossFirst.width, 0.01f)
        val downFirst = frame(0f, 1f, 0f)
        assertEquals(204f, downFirst.centreX, 0.01f)
        assertEquals(1496f, downFirst.centreY, 0.01f)
    }

    @Test
    fun `the path bows upward by the arc at its half way point, and dips with one below nothing`() {
        val straight = frame(0.5f, 0.5f, 0.5f)
        assertEquals(straight.centreY - 60f, frame(0.5f, 0.5f, 0.5f, arc = 60f).centreY, 0.01f)
        assertEquals(straight.centreY + 30f, frame(0.5f, 0.5f, 0.5f, arc = -30f).centreY, 0.01f)
        // Less of it on the way there.
        val early = frame(0.25f, 0.25f, 0.25f, arc = 60f).centreY - frame(0.25f, 0.25f, 0.25f).centreY
        assertTrue(early < 0f && early > -60f)
        // And it changes nothing across, or the size.
        assertEquals(straight.centreX, frame(0.5f, 0.5f, 0.5f, arc = 60f).centreX, 0.01f)
        assertEquals(straight.width, frame(0.5f, 0.5f, 0.5f, arc = 60f).width, 0.01f)
    }

    @Test
    fun `a spring may carry it a little past either end`() {
        val past = frame(1.05f, 1.05f, 1.04f)
        assertTrue(past.width > 1344f && past.height > 2992f)
        assertTrue(past.centreX > 672f)
        // Its corners are never rounder than the screen's.
        assertEquals(84f, past.radius, 0.01f)
        // It never gets smaller than the icon it came from, but its middle may swing past it.
        val under = frame(-0.05f, -0.05f, -0.05f)
        assertEquals(168f, under.width, 0.01f)
        assertEquals(40f, under.radius, 0.01f)
        assertTrue(under.centreX < 204f)
    }

    @Test
    fun `what is nearest is pushed furthest, straight away from where it starts`() {
        val (nearX, nearY) = LaunchMotion.push(100f, 0f, 30f, 450f)
        val (farX, _) = LaunchMotion.push(900f, 0f, 30f, 450f)
        assertTrue(nearX > farX && farX > 0f)
        assertEquals(0f, nearY, 0.0001f)
        assertTrue(nearX <= 30f)
        // At twice the reach, a quarter of it.
        assertEquals(30f / 4f, farX, 0.01f)
        // Up and to the left goes up and to the left, by as much each way.
        val (leftX, upY) = LaunchMotion.push(-200f, -200f, 30f, 450f)
        assertTrue(leftX < 0f && upY < 0f)
        assertEquals(leftX, upY, 0.0001f)
    }

    @Test
    fun `the thing itself is not pushed, and nothing is pushed by nothing`() {
        assertEquals(0f to 0f, LaunchMotion.push(0f, 0f, 30f, 450f))
        assertEquals(0f to 0f, LaunchMotion.push(100f, 50f, 0f, 450f))
        assertEquals(0f to 0f, LaunchMotion.push(100f, 50f, 30f, 0f))
        // Knocked back the other way is pulled in.
        assertTrue(LaunchMotion.push(100f, 0f, -30f, 450f).first < 0f)
    }

    @Test
    fun `a knock is hard enough to send a thing as far as asked and no further`() {
        // Step a spring through time from rest with the speed asked for, and see how far it gets.
        fun furthest(distance: Float, stiffness: Float, damping: Float): Float {
            var x = 0f
            var v = LaunchMotion.knockVelocity(distance, stiffness, damping)
            var most = 0f
            val dt = 0.0002f
            val friction = 2f * damping * kotlin.math.sqrt(stiffness)
            repeat(20_000) {
                v += (-stiffness * x - friction * v) * dt
                x += v * dt
                if (x > most) most = x
            }
            return most
        }
        assertEquals(36f, furthest(36f, 228f, 0.5f), 0.4f)
        assertEquals(36f, furthest(36f, 600f, 0.8f), 0.4f)
        assertEquals(12f, furthest(12f, 228f, 0.3f), 0.2f)
        // One that does not swing still gets there.
        assertEquals(36f, furthest(36f, 228f, 1f), 0.4f)
        // Nothing asked for is no knock at all, and knocking the other way goes the other way.
        assertEquals(0f, LaunchMotion.knockVelocity(0f, 228f, 0.5f), 0f)
        assertTrue(LaunchMotion.knockVelocity(-36f, 228f, 0.5f) < 0f)
    }

    @Test
    fun `the home screen zooms about its middle, or the icon, or anywhere between`() {
        assertEquals(0.5f, LaunchMotion.pivot(204f, 1344f, 0f), 0.0001f)
        assertEquals(204f / 1344f, LaunchMotion.pivot(204f, 1344f, 1f), 0.0001f)
        assertEquals((0.5f + 204f / 1344f) / 2f, LaunchMotion.pivot(204f, 1344f, 0.5f), 0.0001f)
        // Never off the screen, and the middle before the screen has a size.
        assertEquals(1f, LaunchMotion.pivot(5000f, 1344f, 1f), 0.0001f)
        assertEquals(0f, LaunchMotion.pivot(-50f, 1344f, 2f), 0.0001f)
        assertEquals(0.5f, LaunchMotion.pivot(204f, 0f, 1f), 0.0001f)
    }
}
