package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class WallStyleTest {

    @Test
    fun `there are forty-one designs, sorted onto five shelves`() {
        assertEquals(41, WallStyle.entries.size)
        val shelves = WallStyle.entries.groupBy { it.group }.mapValues { it.value.size }
        assertEquals(
            mapOf(WallGroup.Depth to 8, WallGroup.Soft to 9, WallGroup.Lines to 9, WallGroup.Shapes to 8, WallGroup.Scenes to 7),
            shelves,
        )
    }

    @Test
    fun `the Depth shelf is the newest, first in the picker, and six of its eight are for everyone`() {
        assertEquals(WallGroup.Depth, WallGroup.entries.first())
        assertEquals(
            listOf("Iris", "Layers", "Satin", "Marbles", "Liquid", "Twist", "Loop", "Eclipse"),
            WallStyle.entries.take(8).map { it.name },
        )
        assertEquals(listOf("Loop", "Eclipse"), WallStyle.entries.filter { it.group == WallGroup.Depth && it.plus }.map { it.name })
    }

    @Test
    fun `the eleven designs added for the testers are for everyone, and come before a shelf's Plus ones`() {
        val added = listOf("Dawn", "Haze", "Bokeh", "Rays", "Spiral", "Weave", "Tiles", "Dots", "Hex", "City", "Forest")
        added.forEach { assertEquals(it, false, WallStyle.byName(it)!!.plus) }
        // On every shelf, once a Plus design has been passed there are only Plus designs left.
        WallGroup.entries.forEach { group ->
            val shelf = WallStyle.entries.filter { it.group == group }
            assertEquals(group.name, shelf.sortedBy { it.plus }, shelf)
        }
        // And a shelf is all in one run: the picker's "All" keeps the shelves together.
        assertEquals(WallStyle.entries.toList(), WallStyle.entries.sortedBy { it.group.ordinal })
    }

    @Test
    fun `nine designs are for Plus, and the eight that were always free still are`() {
        assertEquals(
            setOf("Silk", "Halo", "Ribbons", "Prism", "Bloom", "Horizon", "Nebula", "Loop", "Eclipse"),
            WallStyle.entries.filter { it.plus }.map { it.name }.toSet(),
        )
        listOf("Glow", "Calm", "Waves", "Orbit", "Facets", "Bands", "Dunes", "Aurora").forEach {
            assertEquals(it, false, WallStyle.byName(it)!!.plus)
        }
    }

    @Test
    fun `every shelf has designs anyone can use`() {
        WallGroup.entries.forEach { group ->
            assertEquals(group.name, true, WallStyle.entries.any { it.group == group && !it.plus })
        }
    }

    @Test
    fun `a design is found by the name it is remembered under`() {
        WallStyle.entries.forEach { assertSame(it, WallStyle.byName(it.name)) }
        assertNull(WallStyle.byName(""))
        assertNull(WallStyle.byName("Sparkles"))
        // The name, not the label and not in another case: it is what the settings hold.
        assertNull(WallStyle.byName("glow"))
    }

    @Test
    fun `labels are all different, so the picker never shows two alike`() {
        assertEquals(WallStyle.entries.size, WallStyle.entries.map { it.label }.toSet().size)
    }
}
