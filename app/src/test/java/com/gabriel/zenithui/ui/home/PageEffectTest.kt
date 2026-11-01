package com.gabriel.zenithui.ui.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PageEffectTest {

    private fun at(style: String, offset: Float) = PageEffects.transform(style, offset)

    @Test
    fun `there are eight styles, and depth is the first`() {
        assertEquals(listOf("depth", "slide", "cube", "stack", "carousel", "wheel", "flip", "fold"), PageEffects.styles)
    }

    @Test
    fun `at rest every style is the plain page`() {
        for (style in PageEffects.styles) {
            assertEquals(style, PageTransform(), at(style, 0f).copy(parallax = false, pivotX = 0.5f, pivotY = 0.5f))
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
    fun `going back, stack sinks the page being left, not the one on the left`() {
        // Half way back from page 2 to page 1: page 2 lies to the right and is being left.
        val left = PageEffects.transform("stack", -0.5f, leaving = true)
        assertEquals(-0.5f, left.shiftX, 0.0001f)
        assertEquals(0.5f, left.alpha, 0.0001f)
        assertEquals(0.94f, left.scale, 0.0001f)
        // Page 1, where you are going, comes over it whole and clear.
        assertEquals(PageTransform(), PageEffects.transform("stack", 0.5f, leaving = false))
        // And it is drawn over the page being left.
        assertTrue(PageEffects.over(leaving = false) > PageEffects.over(leaving = true))
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
    fun `the pill under a row of options stretches across two on its way and draws itself up`() {
        fun span(position: Float) = PageEffects.pillSpan(position, count = 5)
        // At rest it lies under one option exactly.
        assertEquals(1f to 2f, span(1f))
        assertEquals(4f to 5f, span(4f))
        // The leading edge is there by half way; the trailing one follows.
        assertEquals(1f to 2.5f, span(1.25f))
        assertEquals(1f to 3f, span(1.5f))
        assertEquals(1.5f to 3f, span(1.75f))
        // And it is where it came to rest the moment it gets there, with no jump.
        val nearly = span(1.9999f)
        assertEquals(2f, nearly.first, 0.001f)
        assertEquals(3f, nearly.second, 0.001f)
        // Never past either end of the row.
        assertEquals(0f to 1f, span(-0.6f))
        assertEquals(4f to 5f, span(7f))
        assertEquals(0f to 1f, PageEffects.pillSpan(0f, count = 1))
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

    @Test
    fun `carousel turns each page to face the middle and draws it in`() {
        val left = at("carousel", 0.5f)
        val right = at("carousel", -0.5f)
        assertEquals(-19f, left.rotationY, 0.0001f)
        assertEquals(19f, right.rotationY, 0.0001f)
        // Closer in than the pager has it, and smaller, the further it has gone.
        assertTrue(left.shiftX > 0f && right.shiftX < 0f)
        assertTrue(at("carousel", 1f).scale < left.scale)
    }

    @Test
    fun `wheel swings pages round a point far under the screen`() {
        val left = at("wheel", 0.5f)
        assertTrue("about a point below the page", left.pivotY > 1f)
        assertTrue("a page gone left tips anticlockwise", left.rotationZ < 0f)
        assertTrue(at("wheel", -0.5f).rotationZ > 0f)
        assertEquals(0f, left.rotationY, 0f)
    }

    @Test
    fun `flip turns a page over where it stands, the next on its back`() {
        // It stays in the middle while it turns: the pager's own movement is taken back.
        assertEquals(0.3f, at("flip", 0.3f).shiftX, 0.0001f)
        assertEquals(-54f, at("flip", 0.3f).rotationY, 0.0001f)
        // Seen until edge on, and then not: only one face at a time.
        assertEquals(1f, at("flip", 0.49f).alpha, 0f)
        assertEquals(0f, at("flip", 0.51f).alpha, 0f)
        assertEquals(1f, at("flip", -0.2f).alpha, 0f)
        assertEquals(0f, at("flip", -0.8f).alpha, 0f)
    }

    @Test
    fun `fold narrows each page into the crease it shares with the next`() {
        val left = at("fold", 0.5f)
        assertEquals(0.5f, left.squash, 0.0001f)
        assertEquals(1f, left.pivotX, 0f)
        assertEquals(0f, at("fold", -0.5f).pivotX, 0f)
        assertEquals(0f, at("fold", 1f).squash, 0.0001f)
    }

    @Test
    fun `a page far off is drawn as if just gone, whatever the style`() {
        for (style in PageEffects.styles) assertEquals(style, at(style, 1f), at(style, 3f))
    }
}
