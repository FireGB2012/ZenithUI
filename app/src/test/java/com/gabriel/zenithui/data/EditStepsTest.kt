package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditStepsTest {

    private fun app(id: String, page: Int, col: Int, row: Int, pkg: String = "com.$id") = AppCell(id, page, col, row, pkg, ".Main")
    private fun at(vararg items: HomeItem, dock: List<String> = emptyList(), pages: Int = 1) = Arrangement(items.toList(), dock, pages)

    private val mail = app("mail", 0, 0, 0)
    private val maps = app("maps", 0, 1, 0)
    private val clock = WidgetCell("clock", 0, 0, ROW_UNITS, "clock", 2, 2 * ROW_UNITS)

    private fun undo(step: EditStep, now: Arrangement) = EditSteps.undo(step, now, columns = 4, rows = 5)

    @Test
    fun `nothing changed is no step`() {
        val same = at(mail, maps)
        assertNull(EditSteps.of("Mail moved", same, same.copy()))
    }

    @Test
    fun `a move is taken back to where it was`() {
        val before = at(mail, maps)
        val after = at(mail.moved(0, 3, 4 * ROW_UNITS), maps)
        val step = EditSteps.of("Mail moved", before, after)!!
        assertEquals(before, undo(step, after))
    }

    @Test
    fun `what moved aside to make room goes back too`() {
        val before = at(mail, maps)
        // Mail dropped where Maps was, and Maps stepped aside.
        val after = at(mail.moved(0, 1, 0), maps.moved(0, 2, 0))
        val step = EditSteps.of("Mail moved", before, after)!!
        assertEquals(before.items.toSet(), undo(step, after).items.toSet())
    }

    @Test
    fun `something removed comes back as itself, where it was`() {
        val before = at(mail, maps, clock)
        val after = at(maps, clock)
        val back = undo(EditSteps.of("Mail removed", before, after)!!, after)
        assertEquals(mail, back.items.single { it.id == "mail" })
        assertEquals(3, back.items.size)
    }

    @Test
    fun `a folder made from two apps comes apart into them again`() {
        val before = at(mail, maps)
        val folder = FolderCell("f", 0, 1, 0, "Travel", listOf(mail.key, maps.key))
        val after = at(folder)
        val back = undo(EditSteps.of("Travel made", before, after)!!, after)
        assertEquals(before.items.toSet(), back.items.toSet())
    }

    @Test
    fun `an app put in a folder comes out of it again`() {
        val folder = FolderCell("f", 0, 2, 0, "Travel", listOf("com.trains/.Main"))
        val before = at(mail, folder)
        val after = at(folder.copy(keys = folder.keys + mail.key))
        val back = undo(EditSteps.of("Mail moved", before, after)!!, after)
        assertEquals(before.items.toSet(), back.items.toSet())
    }

    @Test
    fun `something added is taken off again`() {
        val before = at(mail)
        val after = at(mail, clock)
        assertEquals(before, undo(EditSteps.of("Clock added", before, after)!!, after))
    }

    @Test
    fun `what changed since, that the step did not touch, is left as it is now`() {
        val before = at(mail, maps, clock)
        val after = at(mail.moved(0, 3, 0), maps, clock)
        val step = EditSteps.of("Mail moved", before, after)!!
        // Meanwhile Maps went from the phone and the clock was turned to another colour.
        val recoloured = clock.copy(accent = 0xFF00FF00)
        val now = at(mail.moved(0, 3, 0), recoloured)
        val back = undo(step, now)
        assertEquals(setOf(mail, recoloured), back.items.toSet())
    }

    @Test
    fun `an app moved into the dock goes back onto the page and out of the dock`() {
        val before = at(mail, maps, dock = listOf("com.phone/.Main"))
        val after = at(maps, dock = listOf("com.phone/.Main", mail.key))
        val back = undo(EditSteps.of("Mail moved", before, after)!!, after)
        assertEquals(before.dock, back.dock)
        assertTrue(back.items.any { it.id == "mail" })
    }

    @Test
    fun `an app taken off the dock comes back to its place in it`() {
        val before = at(mail, dock = listOf("com.phone/.Main", "com.mail/.Main", "com.web/.Main"))
        val after = at(mail, dock = listOf("com.phone/.Main", "com.web/.Main"))
        val step = EditSteps.of("Mail removed", before, after)!!
        assertEquals(before, undo(step, after))
    }

    @Test
    fun `a dock the step left alone keeps what happened to it since`() {
        val before = at(mail, maps, dock = listOf("com.phone/.Main"))
        val after = at(mail.moved(0, 2, 0), maps, dock = listOf("com.phone/.Main"))
        val step = EditSteps.of("Mail moved", before, after)!!
        val now = after.copy(dock = emptyList())
        assertEquals(emptyList<String>(), undo(step, now).dock)
    }

    @Test
    fun `a page taken away comes back with what was on it`() {
        val onSecond = app("notes", 1, 0, 0)
        val onThird = app("camera", 2, 0, 0)
        val before = at(mail, onSecond, onThird, pages = 3)
        // The second page goes, and the third closes up into its place.
        val after = at(mail, onThird.moved(1, 0, 0), pages = 2)
        val back = undo(EditSteps.of("Page 2 taken away", before, after)!!, after)
        assertEquals(before.items.toSet(), back.items.toSet())
        assertEquals(3, back.pages)
    }

    @Test
    fun `something put back on a spot taken since goes to the first opening`() {
        val before = at(mail, maps)
        val after = at(maps)
        val step = EditSteps.of("Mail removed", before, after)!!
        val taker = app("camera", 0, 0, 0)
        val back = undo(step, at(maps, taker))
        val mailBack = back.items.single { it.id == "mail" }
        assertFalse(mailBack.col == 0 && mailBack.row == 0 && mailBack.page == 0)
        assertEquals(3, back.items.size)
    }

    @Test
    fun `the history takes back the newest change first`() {
        val first = at(mail, maps)
        val second = at(mail.moved(0, 3, 0), maps)
        val third = at(mail.moved(0, 3, 0))
        val history = EditHistory()
            .with(EditSteps.of("Mail moved", first, second)!!)
            .with(EditSteps.of("Maps removed", second, third)!!)
        assertEquals("Maps removed", history.newest?.label)
        assertEquals(1, history.more)
        val (step, rest) = history.takeNewest()!!
        assertEquals("Maps removed", step.label)
        assertEquals("Mail moved", rest.newest?.label)
        assertEquals(0, rest.more)
    }

    @Test
    fun `an empty history has nothing to take back`() {
        assertNull(EditHistory().takeNewest())
        assertNull(EditHistory().newest)
    }

    @Test
    fun `the history holds everything its steps could put back`() {
        val before = at(mail, clock)
        val after = at(mail)
        val history = EditHistory().with(EditSteps.of("Clock removed", before, after)!!)
        assertTrue(history.holds().any { it.id == "clock" })
    }
}
