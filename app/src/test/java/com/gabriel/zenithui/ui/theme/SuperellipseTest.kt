package com.gabriel.zenithui.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sqrt

class SuperellipseTest {

    @Test
    fun `straight out from the middle a superellipse reaches the edge of its square`() {
        assertEquals(1f, superellipseRadius(0f, 5f), 1e-4f)
        // And so it does on every side, whatever its exponent.
        for (exponent in listOf(2.9f, 5f)) for (side in 0..3) {
            assertEquals(1f, superellipseRadius(side * (PI / 2).toFloat(), exponent), 1e-4f)
        }
    }

    @Test
    fun `the iPhone's corners are fuller than the squircle's, and neither is a square's`() {
        val corner = (PI / 4).toFloat()
        val iphone = superellipseRadius(corner, 5f)
        val squircle = superellipseRadius(corner, 2.9f)
        assertTrue("iPhone $iphone, squircle $squircle", iphone > squircle)
        assertTrue(iphone < sqrt(2f))
        assertTrue(squircle < sqrt(2f))
    }

    @Test
    fun `the iPhone shape is a superellipse of exponent 5, and the squircle keeps its own`() {
        assertEquals(SquircleShape(5f), ZenithShapes.forIconShape("iphone"))
        // Icons saved under the squircle are cut as they always were.
        assertEquals(SquircleShape(2.9f), ZenithShapes.forIconShape("squircle"))
    }

    @Test
    fun `glass takes its squircle from the shape, and Zenith's own otherwise`() {
        assertEquals(5f, squircleExponent(SquircleShape(5f)), 0f)
        assertEquals(2.9f, squircleExponent(ZenithShapes.icon), 0f)
        // Every other outline the lens cannot follow exactly is still drawn as the squircle.
        assertEquals(2.9f, squircleExponent(ZenithShapes.forIconShape("clover")), 0f)
        assertEquals(2.9f, squircleExponent(RoundedCornerShape(4)), 0f)
    }
}
