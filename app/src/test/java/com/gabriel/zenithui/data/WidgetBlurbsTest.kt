package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetBlurbsTest {

    @Test
    fun `every widget in the gallery says what it is for, in a few words`() {
        Widgets.placeable.forEach { spec ->
            val blurb = WidgetBlurbs.of(spec.type)
            assertNotNull(spec.type, blurb)
            // Two lines at most on a card half the page wide.
            assertTrue("${spec.type}: $blurb", blurb!!.length in 8..WidgetBlurbs.LONGEST)
        }
    }

    @Test
    fun `each says something of its own`() {
        val blurbs = Widgets.placeable.map { WidgetBlurbs.of(it.type) }
        assertEquals(blurbs.size, blurbs.toSet().size)
    }

    @Test
    fun `a no glass widget is for the same thing as the widget it is a version of`() {
        Widgets.noGlass.forEach { assertEquals(WidgetBlurbs.of(Widgets.baseType(it.type)), WidgetBlurbs.of(it.type)) }
    }

    @Test
    fun `a widget this Zenith does not know has none`() {
        assertNull(WidgetBlurbs.of("hologram"))
        assertNull(WidgetBlurbs.of("search"))
    }
}
