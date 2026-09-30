package com.gabriel.zenithui.data

import com.gabriel.zenithui.data.DropPlanner.Plan
import com.gabriel.zenithui.data.DropPlanner.Spot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.math.abs
import kotlin.random.Random

class DropPlannerTest {

    /** The Pixel 10a's home page: 4 by 5, a column about 2.9 row units wide, an icon tucks 1 unit. */
    private val pixel = DropPlanner.Page(columns = 4, unitRows = 20, tuck = 1, free = false, colWidthInRowUnits = 2.9f)

    private fun app(id: String, col: Int, row: Int, spanX: Int = 1, spanY: Int = ROW_UNITS, page: Int = 0) =
        AppCell(id, page, col, row, "pkg.$id", "Main", spanX = spanX, spanY = spanY)

    private fun widget(id: String, col: Int, row: Int, spanX: Int, spanY: Int, type: String = "clock") =
        WidgetCell(id, 0, col, row, type, spanX, spanY)

    // The page on the Pixel on 2026-09-28: the clock top left, Google under it, Gmail as a 3 by 2
    // super icon beside Google.
    private val clock = widget("clock", 0, 0, 2, 8)
    private val google = app("google", 0, 8)
    private val gmail = app("gmail", 1, 8, spanX = 3, spanY = 8)
    private val home = listOf(google, gmail, clock)

    private fun plan(items: List<HomeItem>, dragged: HomeItem, col: Float, row: Float, page: DropPlanner.Page = pixel) =
        DropPlanner.plan(items, dragged, col, row, page)

    /** The page after [plan], checked to be sound: nothing on top of anything, nothing off the page. */
    private fun applied(items: List<HomeItem>, dragged: HomeItem, plan: Plan, page: DropPlanner.Page = pixel): List<HomeItem> {
        val result = items.filter { it.id != dragged.id }.map { item ->
            plan.moves[item.id]?.let { item.moved(item.page, it.col, it.row) } ?: item
        } + dragged.moved(0, plan.target.col, plan.target.row)
        assertSound(result, page)
        return result
    }

    private fun assertSound(items: List<HomeItem>, page: DropPlanner.Page) {
        for (item in items) {
            assertTrue("$item is off the page", item.col >= 0 && item.row >= 0 &&
                item.col + item.spanX <= page.columns && item.row + item.spanY <= page.unitRows)
        }
        for (i in items.indices) for (j in i + 1 until items.size) {
            val a = items[i]
            val b = items[j]
            val aBottom = a.row + LayoutBuilder.blockedSpanY(a, page.tuck)
            val bBottom = b.row + LayoutBuilder.blockedSpanY(b, page.tuck)
            val overlap = a.col < b.col + b.spanX && b.col < a.col + a.spanX && a.row < bBottom && b.row < aBottom
            assertTrue("$a and $b overlap", !overlap)
        }
    }

    // ------------------------------------------------------------------ where it lands

    @Test
    fun `an icon let go on an empty spot lands there`() {
        val plan = plan(home, google, 2.1f, 0.3f)
        assertEquals(Plan(Spot(2, 0)), plan)
    }

    @Test
    fun `an icon let go back on its own spot stays there`() {
        assertEquals(Plan(Spot(0, 8)), plan(home, google, 0.1f, 8.3f))
    }

    @Test
    fun `an icon let go a little high under a widget tucks in snug under it`() {
        // Its top edge is still a little inside the clock; the opening just under the clock is meant.
        val plan = plan(listOf(clock, google), google, 0.1f, 6.4f)
        assertEquals(Spot(0, 7), plan?.target)
        assertTrue(plan!!.moves.isEmpty())
    }

    @Test
    fun `an icon under a widget stays in line with the icon beside it rather than tucking in`() {
        // Gmail's top is on row 8; tucked in on row 7, Google would sit a quarter row above it.
        assertEquals(Plan(Spot(0, 8)), plan(home, google, 0.1f, 6.4f))
    }

    @Test
    fun `a widget let go a unit off a row lines up with it`() {
        val plan = plan(home, clock, 2.0f, 0.8f)
        assertEquals(Plan(Spot(2, 0)), plan)
    }

    @Test
    fun `an icon beside a widget keeps to the rows of the page, not the widget's tuck row`() {
        // Nothing is above column 3, so row 7 would sit it a quarter row off every other icon.
        val stray = app("stray", 3, 16)
        val plan = plan(listOf(clock, stray), stray, 3.0f, 7.1f)
        assertEquals(Plan(Spot(3, 8)), plan)
    }

    @Test
    fun `an icon sits snug under an icon that is itself tucked under a widget`() {
        val tucked = app("tucked", 0, 7)
        val next = app("next", 3, 16)
        val plan = plan(listOf(clock, tucked, next), next, 0.0f, 11.2f)
        assertEquals(Plan(Spot(0, 11)), plan)
    }

    @Test
    fun `an icon never lands between rows`() {
        // Let go halfway between two rows on an empty part of the page.
        val plan = plan(home, google, 3.0f, 14.0f)
        assertEquals(0, plan!!.target.row % ROW_UNITS)
    }

    @Test
    fun `free placement lands on any row unit`() {
        val plan = plan(home, google, 2.0f, 1.2f, pixel.copy(free = true))
        assertEquals(Plan(Spot(2, 1)), plan)
    }

    // ------------------------------------------------------------------ making room

    @Test
    fun `an icon let go on the left of another takes its place and the row makes room`() {
        val a = app("a", 0, 8)
        val b = app("b", 1, 8)
        val c = app("c", 2, 8)
        val d = app("d", 3, 0)
        val items = listOf(a, b, c, d)
        val plan = plan(items, d, 0.8f, 8.1f)!!
        assertEquals(Spot(1, 8), plan.target)
        assertEquals(mapOf("b" to Spot(2, 8), "c" to Spot(3, 8)), plan.moves)
        applied(items, d, plan)
    }

    @Test
    fun `an icon let go on the right of another pushes it the way there is room`() {
        val a = app("a", 0, 8)
        val b = app("b", 1, 8)
        val c = app("c", 2, 8)
        val d = app("d", 3, 0)
        val items = listOf(a, b, c, d)
        // Pushing b left would push a off the page, so the row makes room to the right instead.
        val plan = plan(items, d, 1.3f, 8.0f)!!
        assertEquals(Spot(1, 8), plan.target)
        assertEquals(mapOf("b" to Spot(2, 8), "c" to Spot(3, 8)), plan.moves)
    }

    @Test
    fun `an icon moved within a full row swaps along it`() {
        val a = app("a", 0, 8)
        val b = app("b", 1, 8)
        val c = app("c", 2, 8)
        val d = app("d", 3, 8)
        val items = listOf(a, b, c, d)
        // d onto b: b and c slide right into the spot d left.
        val plan = plan(items, d, 0.8f, 8.0f)!!
        assertEquals(Spot(1, 8), plan.target)
        assertEquals(mapOf("b" to Spot(2, 8), "c" to Spot(3, 8)), plan.moves)
        applied(items, d, plan)
    }

    @Test
    fun `a widget let go over the super icon moves it out of the way`() {
        val plan = plan(home, clock, 2.0f, 12.0f)
        assertNotNull(plan)
        assertEquals(Spot(2, 12), plan!!.target)
        assertEquals(setOf("gmail"), plan.moves.keys)
        applied(home, clock, plan)
    }

    @Test
    fun `a super icon let go over the widget lands there`() {
        val plan = plan(home, gmail, 0.0f, 0.0f)
        assertNotNull(plan)
        assertEquals(Spot(0, 0), plan!!.target)
        assertTrue("clock" in plan.moves)
        applied(home, gmail, plan)
    }

    @Test
    fun `an icon let go on the end of a super icon slides it into the spot the icon left`() {
        val plan = plan(home, google, 2.5f, 10.0f)!!
        assertEquals(Spot(3, 8), plan.target)
        assertEquals(mapOf("gmail" to Spot(0, 8)), plan.moves)
        applied(home, google, plan)
    }

    // ------------------------------------------------------------------ never far away

    @Test
    fun `when nothing can make room the icon goes nowhere rather than across the page`() {
        // A full-width widget over rows full of icons, with one opening left in the far corner.
        val board = widget("board", 0, 0, 4, 8, type = "notes")
        val icons = (0 until 4).flatMap { c -> listOf(8, 12, 16).map { r -> app("i$c$r", c, r) } }
            .filterNot { it.col == 3 && it.row == 16 }
        val newcomer = app("new", 0, 0, page = 1)
        assertNull(plan(icons + board, newcomer, 1.0f, 1.0f))
    }

    @Test
    fun `an icon let go on a widget with nowhere to go takes the opening beside it`() {
        val board = widget("board", 0, 0, 4, 8, type = "notes")
        val icons = (0 until 4).flatMap { c -> listOf(8, 12, 16).map { r -> app("i$c$r", c, r) } }
            .filterNot { it.col == 2 && it.row == 8 }
        val newcomer = app("new", 0, 0, page = 1)
        val plan = plan(icons + board, newcomer, 2.0f, 5.0f)
        assertEquals(Plan(Spot(2, 8), exact = false), plan)
    }

    @Test
    fun `no drop on the Pixel's home page ever lands far from where it was let go`() {
        for (dragged in home) {
            var y = -1.0f
            while (y <= pixel.unitRows - dragged.spanY + 1f) {
                var x = -0.4f
                while (x <= pixel.columns - dragged.spanX + 0.4f) {
                    val plan = plan(home, dragged, x, y)
                    if (plan != null) {
                        val cx = x.coerceIn(0f, (pixel.columns - dragged.spanX).toFloat())
                        val cy = y.coerceIn(0f, (pixel.unitRows - dragged.spanY).toFloat())
                        val across = abs(plan.target.col - cx)
                        val down = abs(plan.target.row - cy)
                        if (across > 1.25f || down > 1.25f * ROW_UNITS) {
                            fail("${dragged.id} let go at ($x, $y) lands at ${plan.target}")
                        }
                        applied(home, dragged, plan)
                    }
                    x += 0.1f
                }
                y += 0.25f
            }
        }
    }

    @Test
    fun `every plan on random pages leaves a sound page`() {
        val random = Random(7)
        repeat(400) { round ->
            val items = mutableListOf<HomeItem>()
            val grid = PageGrid(pixel.columns, pixel.unitRows)
            repeat(random.nextInt(2, 12)) { n ->
                val candidate: HomeItem = when (random.nextInt(4)) {
                    0 -> widget("w$n", 0, 0, random.nextInt(1, 5), random.nextInt(2, 13), if (random.nextBoolean()) "clock" else "search")
                    1 -> app("s$n", 0, 0, spanX = random.nextInt(1, 3), spanY = ROW_UNITS * random.nextInt(1, 3))
                    else -> app("a$n", 0, 0)
                }
                val step = if (candidate is WidgetCell) 1 else ROW_UNITS
                val slot = grid.findSlot(candidate.spanX, LayoutBuilder.blockedSpanY(candidate, pixel.tuck), step)
                if (slot != null && slot.second + candidate.spanY <= pixel.unitRows) {
                    grid.occupy(slot.first, slot.second, candidate.spanX, LayoutBuilder.blockedSpanY(candidate, pixel.tuck))
                    items += candidate.moved(0, slot.first, slot.second)
                }
            }
            if (items.isEmpty()) return@repeat
            val dragged = items[random.nextInt(items.size)]
            val x = random.nextFloat() * (pixel.columns - dragged.spanX + 0.8f) - 0.4f
            val y = random.nextFloat() * (pixel.unitRows - dragged.spanY + 2f) - 1f
            val plan = plan(items, dragged, x, y) ?: return@repeat
            try {
                applied(items, dragged, plan)
            } catch (e: AssertionError) {
                throw AssertionError("round $round: ${dragged.id} at ($x, $y) on $items gave $plan", e)
            }
        }
    }
}
