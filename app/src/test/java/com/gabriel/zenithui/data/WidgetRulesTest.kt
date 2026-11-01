package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class WidgetRulesTest {

    @Test
    fun `what is still to do comes first, and a tick drops a thing below it`() {
        var list = Todos.add(emptyList(), "bread")
        list = Todos.add(list, "  post  ")
        list = Todos.add(list, "call Mum")
        assertEquals(listOf("bread", "post", "call Mum"), list.map { it.text })
        list = Todos.toggle(list, 0)
        assertEquals(listOf("post", "call Mum", "bread"), list.map { it.text })
        assertTrue(list.last().done)
        // Something new goes in before what is done.
        list = Todos.add(list, "milk")
        assertEquals(listOf("post", "call Mum", "milk", "bread"), list.map { it.text })
        // Taken back, it rejoins what is left, at the end of it.
        list = Todos.toggle(list, 3)
        assertEquals(listOf("post", "call Mum", "milk", "bread"), list.map { it.text })
        assertEquals(4, Todos.left(list))
    }

    @Test
    fun `nothing blank goes on the list, and it stops at twenty`() {
        assertTrue(Todos.add(emptyList(), "   ").isEmpty())
        val full = (1..Todos.MOST).fold(emptyList<TodoItem>()) { list, n -> Todos.add(list, "thing $n") }
        assertEquals(Todos.MOST, full.size)
        assertEquals(full, Todos.add(full, "one more"))
        val ticked = Todos.toggle(full, 0)
        assertEquals(Todos.MOST - 1, Todos.clearDone(ticked).size)
    }

    @Test
    fun `a city is named as people say it`() {
        assertEquals("New York", WorldClocks.city("America/New_York"))
        assertEquals("America", WorldClocks.region("America/New_York"))
        assertEquals("Buenos Aires", WorldClocks.city("America/Argentina/Buenos_Aires"))
    }

    @Test
    fun `how far a city is from here is said in hours, and in days when the date differs`() {
        // Noon in London in winter: Tokyo is nine hours on, the same day; Los Angeles eight back.
        val noon = Instant.parse("2026-01-15T12:00:00Z")
        val london = ZoneId.of("Europe/London")
        assertEquals("+9 h", WorldClocks.offset("Asia/Tokyo", london, noon))
        assertEquals("−8 h", WorldClocks.offset("America/Los_Angeles", london, noon))
        assertEquals("Same time", WorldClocks.offset("Europe/Lisbon", london, noon))
        assertEquals("+5 h 30", WorldClocks.offset("Asia/Kolkata", london, noon))
        // Late in the evening it is already tomorrow in Tokyo.
        assertEquals("+9 h, tomorrow", WorldClocks.offset("Asia/Tokyo", london, Instant.parse("2026-01-15T20:00:00Z")))
        assertTrue(WorldClocks.daytime("Europe/London", noon))
        assertFalse(WorldClocks.daytime("Asia/Tokyo", Instant.parse("2026-01-15T14:00:00Z")))
    }

    @Test
    fun `every city on offer is a real zone with a name`() {
        val all = WorldClocks.all()
        assertTrue(all.size > 100)
        assertTrue(all.all { runCatching { ZoneId.of(it) }.isSuccess && WorldClocks.city(it).isNotBlank() })
        assertTrue(WorldClocks.DEFAULT.all { it in all })
    }

    @Test
    fun `a countdown counts days to go, the day itself, and days since`() {
        val today = LocalDate.of(2026, 10, 31)
        val christmas = LocalDate.of(2026, 12, 25).toEpochDay()
        assertEquals(Countdown.Count.ToGo(55), Countdown.count(christmas, today))
        assertEquals(Countdown.Count.Today, Countdown.count(today.toEpochDay(), today))
        assertEquals(Countdown.Count.Since(1), Countdown.count(today.toEpochDay() - 1, today))
        assertEquals(Countdown.Count.NotSet, Countdown.count(0L, today))
        assertEquals("days to Christmas", Countdown.words(Countdown.Count.ToGo(55), "Christmas"))
        assertEquals("day to the day", Countdown.words(Countdown.Count.ToGo(1), ""))
        assertEquals("Christmas is today", Countdown.words(Countdown.Count.Today, "Christmas"))
    }

    @Test
    fun `a countdown's ring is as full as the wait is gone`() {
        val from = LocalDate.of(2026, 10, 1).toEpochDay()
        val day = LocalDate.of(2026, 10, 11).toEpochDay()
        assertEquals(0f, Countdown.progress(from, day, LocalDate.of(2026, 10, 1)))
        assertEquals(0.5f, Countdown.progress(from, day, LocalDate.of(2026, 10, 6)))
        assertEquals(1f, Countdown.progress(from, day, LocalDate.of(2026, 10, 20)))
        // Set on the day itself, it is already full rather than dividing by nothing.
        assertEquals(1f, Countdown.progress(day, day, LocalDate.of(2026, 10, 11)))
    }
}
