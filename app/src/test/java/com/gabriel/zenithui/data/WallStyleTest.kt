package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class WallStyleTest {

    @Test
    fun `there are twenty-two designs, sorted onto four shelves`() {
        assertEquals(22, WallStyle.entries.size)
        val shelves = WallStyle.entries.groupBy { it.group }.mapValues { it.value.size }
        assertEquals(
            mapOf(WallGroup.Soft to 6, WallGroup.Lines to 6, WallGroup.Shapes to 5, WallGroup.Scenes to 5),
            shelves,
        )
    }

    @Test
    fun `seven designs are for Plus, and the eight that were always free still are`() {
        assertEquals(
            setOf("Silk", "Halo", "Ribbons", "Prism", "Bloom", "Horizon", "Nebula"),
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
