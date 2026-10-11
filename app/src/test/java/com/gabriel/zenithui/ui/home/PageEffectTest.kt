package com.gabriel.zenithui.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageEffectTest {

    private fun at(style: String, offset: Float) = PageEffects.transform(style, offset)

    @Test
    fun `there are four styles, and depth is the first`() {
        assertEquals(listOf("depth", "slide", "cube", "stack"), PageEffects.styles)
    }

    @Test
    fun `at rest every style is the plain page`() {
        for (style in PageEffects.styles) {
            assertEquals(style, PageTransform(), at(style, 0f).copy(parallax = false, pivotX = 0.5f))
        }
    }

    @Test
    fun `depth sinks a page back and fades it as it leaves, either way`() {
        for (side in listOf(-1f, 1f)) {
            val half = at("depth", 0.5f * side)
            assertEquals(0.97f, half.scale, 0.0001f)
            assertEquals(0.8f, half.alpha, 0.0001f)
            val gone = at("depth", side)
            assertEquals(0.94f, gone.scale, 0.0001f)
            assertEquals(0.6f, gone.alpha, 0.0001f)
            assertEquals(0f, gone.rotationY, 0f)
            assertEquals(0f, gone.shiftX, 0f)
        }
        assertTrue(at("depth", 0.3f).parallax)
    }

    @Test
    fun `slide does nothing at all`() {
        for (offset in listOf(-1f, -0.5f, 0f, 0.5f, 1f)) assertEquals(PageTransform(), at("slide", offset))
    }

    @Test
    fun `cube turns each page about the edge it shares with the other`() {
        // A page lying to the left turns about its right edge; one to the right, about its left.
        val left = at("cube", 0.5f)
        assertEquals(-45f, left.rotationY, 0.0001f)
        assertEquals(1f, left.pivotX, 0f)
        val right = at("cube", -0.5f)
        assertEquals(45f, right.rotationY, 0.0001f)
        assertEquals(0f, right.pivotX, 0f)
        // Edge on by the time it has gone, and never further.
        assertEquals(-90f, at("cube", 1f).rotationY, 0.0001f)
        assertEquals(90f, at("cube", -1f).rotationY, 0.0001f)
        assertEquals(-90f, at("cube", 2.5f).rotationY, 0.0001f)
        assertFalse(left.parallax)
    }

    @Test
    fun `stack holds the page on the left where it was, smaller and fainter, under the next`() {
        val under = at("stack", 0.5f)
        // Moved right by as much as the swipe moved it left: it has not gone anywhere.
        assertEquals(0.5f, under.shiftX, 0.0001f)
        assertTrue(under.scale < 1f)
        assertTrue(under.alpha < 1f)
        assertEquals(0f, at("stack", 1f).alpha, 0.0001f)
        // The page coming over the top is simply itself.
        assertEquals(PageTransform(), at("stack", -0.5f))
        assertEquals(PageTransform(), at("stack", -1f))
    }

    @Test
    fun `a style nobody knows is depth`() {
        assertEquals(at("depth", 0.4f), at("origami", 0.4f))
        assertEquals(at("depth", -0.4f), at("", -0.4f))
    }

    @Test
    fun `a page further than one page away is drawn as one page away`() {
        for (style in PageEffects.styles) {
            assertEquals(style, at(style, 1f), at(style, 3f))
            assertEquals(style, at(style, -1f), at(style, -3f))
        }
    }

    @Test
    fun `the dots share one pill between them as the page turns`() {
        // A quarter of the way from the second page to the third.
        assertEquals(0f, PageEffects.dotWidth(1.25f, 0), 0.0001f)
        assertEquals(0.75f, PageEffects.dotWidth(1.25f, 1), 0.0001f)
        assertEquals(0.25f, PageEffects.dotWidth(1.25f, 2), 0.0001f)
        assertEquals(0f, PageEffects.dotWidth(1.25f, 3), 0.0001f)
        // At rest one dot has all of it.
        assertEquals(1f, PageEffects.dotWidth(2f, 2), 0f)
        assertEquals(0f, PageEffects.dotWidth(2f, 1), 0f)
    }

    @Test
    fun `a page added grows its dot out of the end of the row`() {
        // Two dots, part of the way to being three.
        assertEquals(1f, PageEffects.dotPresence(2.4f, 0), 0f)
        assertEquals(1f, PageEffects.dotPresence(2.4f, 1), 0f)
        assertEquals(0.4f, PageEffects.dotPresence(2.4f, 2), 0.0001f)
        assertEquals(0f, PageEffects.dotPresence(2.4f, 3), 0f)
        // At rest every dot is wholly there, and there is no dot too many.
        for (index in 0 until 3) assertEquals(1f, PageEffects.dotPresence(3f, index), 0f)
        assertEquals(0f, PageEffects.dotPresence(3f, 3), 0f)
    }
}
