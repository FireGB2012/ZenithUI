package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IconLookTest {

    @Test
    fun `dots count only what Android would badge`() {
        val counts = Badges.tally(
            listOf(
                Badges.Seen("mail"),
                Badges.Seen("mail", number = 3),
                // Music playing and a download in progress are not something to read.
                Badges.Seen("music", ongoing = true),
                // The heading over a bundle is counted through its members.
                Badges.Seen("chat", summary = true),
                Badges.Seen("chat"),
                // An app that asked for no badge gets none.
                Badges.Seen("quiet", canBadge = false),
            )
        )
        assertEquals(mapOf("mail" to 4, "chat" to 1), counts)
    }

    @Test
    fun `a folder's dot is all its apps' together, each app once`() {
        val counts = mapOf("com.a" to 2, "com.b" to 5)
        assertEquals(7, Badges.total(listOf("com.a/A", "com.b/B", "com.c/C"), counts))
        // Two screens of one app in a folder are still one app.
        assertEquals(2, Badges.total(listOf("com.a/A", "com.a/Settings"), counts))
        assertEquals(5, Badges.forKey("com.b/B", counts))
        assertEquals(0, Badges.forKey("com.c/C", counts))
    }

    @Test
    fun `a counting dot never runs past two figures and a plus`() {
        assertEquals("7", Badges.text(7))
        assertEquals("99", Badges.text(99))
        assertEquals("99+", Badges.text(100))
    }

    @Test
    fun `an unknown choice reads as the one everybody has`() {
        assertEquals(IconStyles.ORIGINAL, IconStyles.of("holographic"))
        assertEquals(Badges.DOT, Badges.of(""))
        assertEquals(LabelSizes.MEDIUM, LabelSizes.of("huge"))
        assertEquals(11f, LabelSizes.sp(LabelSizes.MEDIUM))
    }

    @Test
    fun `every choice has its own name, and sizes go up in order`() {
        assertEquals(IconStyles.ids.size, IconStyles.ids.map(IconStyles::label).toSet().size)
        assertEquals(Badges.ids.size, Badges.ids.map(Badges::label).toSet().size)
        assertEquals(LabelSizes.ids, LabelSizes.ids.sortedBy(LabelSizes::sp))
        IconStyles.ids.forEach { assertTrue(IconStyles.detail(it).isNotBlank()) }
    }

    @Test
    fun `every icon shape has a name of its own`() {
        assertEquals(8, IconShapes.ids.size)
        assertEquals(IconShapes.ids.size, IconShapes.ids.map(IconShapes::label).toSet().size)
        // The ones there before keep the names they had.
        assertEquals("Squircle", IconShapes.label("squircle"))
        assertNotEquals("Squircle", IconShapes.label("clover"))
    }
}
