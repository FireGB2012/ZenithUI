package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Test

class PageOrderTest {

    private fun app(id: String, page: Int) = AppCell(id, page, 1, 0, "com.$id", ".Main")
    private val layout = listOf(app("a", 0), app("b", 1), app("c", 2), app("d", 3))
    private fun pages(items: List<HomeItem>) = items.associate { it.id to it.page }

    @Test
    fun `a page moved later takes what is on it, and the ones it passes move back`() {
        assertEquals(mapOf("a" to 2, "b" to 0, "c" to 1, "d" to 3), pages(PageOrder.move(layout, 0, 2)))
    }

    @Test
    fun `a page moved earlier pushes the ones it passes on`() {
        assertEquals(mapOf("a" to 0, "b" to 2, "c" to 3, "d" to 1), pages(PageOrder.move(layout, 3, 1)))
        assertEquals(layout, PageOrder.move(layout, 2, 2))
    }

    @Test
    fun `what moves with its page keeps its place on it, offsets and all`() {
        val free = AppCell("f", 1, 2, 4, "com.f", ".Main", dx = 0.3f, dy = -0.2f)
        val moved = PageOrder.move(listOf(free), 1, 0).single() as AppCell
        assertEquals(free.copy(page = 0), moved)
    }

    @Test
    fun `a page taken away goes with all on it, and the pages after close up`() {
        val (kept, gone) = PageOrder.remove(layout + app("b2", 1), 1)
        assertEquals(setOf("b", "b2"), gone.map { it.id }.toSet())
        assertEquals(mapOf("a" to 0, "c" to 1, "d" to 2), pages(kept))
    }

    @Test
    fun `every page lands on a place of its own`() {
        for (from in 0..3) for (to in 0..3) {
            assertEquals("$from to $to", (0..3).toSet(), (0..3).map { PageOrder.newIndex(it, from, to) }.toSet())
        }
    }
}
