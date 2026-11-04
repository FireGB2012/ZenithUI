package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSizesTest {

    private val every = Widgets.placeable

    private fun sizes(spec: WidgetSpec, columns: Int = 4, rows: Int = 5) =
        WidgetSizes.options(spec, columns, rows).map { Triple(it.name, it.spanX, it.rows) }

    @Test
    fun `on an ordinary grid a widget comes small, medium, wide, tall and large`() {
        assertEquals(
            listOf(
                Triple("Small", 2, 1), Triple("Medium", 2, 2), Triple("Wide", 4, 2),
                Triple("Tall", 2, 3), Triple("Large", 4, 3),
            ),
            sizes(Widgets.Clock),
        )
        // Power goes down to a single cell.
        assertEquals(Triple("Small", 1, 1), sizes(Widgets.Battery).first())
    }

    @Test
    fun `a widget whose smallest size is its ordinary one is not offered it twice`() {
        // The calendar needs two rows to be a calendar at all.
        assertEquals(listOf("Medium", "Wide", "Tall", "Large"), sizes(Widgets.Calendar).map { it.first })
    }

    @Test
    fun `every size is one the widget and the grid can both take, and none is offered twice`() {
        for (spec in every) for ((columns, rows) in listOf(3 to 4, 4 to 4, 4 to 5, 5 to 5, 5 to 6, 6 to 7, 2 to 2)) {
            val options = WidgetSizes.options(spec, columns, rows)
            val where = "${spec.type} on $columns x $rows"
            assertTrue(where, options.isNotEmpty())
            assertEquals(where, options.size, options.map { it.spanX to it.rows }.toSet().size)
            assertEquals(where, options.size, options.map { it.name }.toSet().size)
            for (size in options) {
                assertTrue("$where ${size.name}", size.spanX >= spec.minSpanX || size.spanX == columns)
                assertTrue("$where ${size.name}", size.spanX <= columns.coerceAtLeast(spec.minSpanX))
                assertTrue("$where ${size.name}", size.spanY >= spec.minSpanY)
                assertTrue("$where ${size.name}", size.spanY <= (rows * ROW_UNITS).coerceAtLeast(spec.minSpanY))
                assertEquals("whole rows only", 0, size.spanY % ROW_UNITS)
            }
        }
    }

    @Test
    fun `the size a widget is usually placed at is always there, and is the one shown first`() {
        for (spec in every) for ((columns, rows) in listOf(3 to 4, 4 to 5, 5 to 6)) {
            val options = WidgetSizes.options(spec, columns, rows)
            val (spanX, spanY) = Widgets.defaultSpan(spec, columns, rows)
            val chosen = options[WidgetSizes.startIndex(options)]
            assertEquals("${spec.type} on $columns x $rows", WidgetSizes.MEDIUM, chosen.name)
            assertEquals(spanX, chosen.spanX)
            assertEquals(spanY, chosen.spanY)
        }
    }

    @Test
    fun `on a narrow grid wide means the whole width of it`() {
        assertEquals(Triple("Wide", 3, 2), sizes(Widgets.Notes, columns = 3, rows = 4).first { it.first == "Wide" })
        assertEquals(Triple("Large", 3, 3), sizes(Widgets.Notes, columns = 3, rows = 4).first { it.first == "Large" })
    }
}
