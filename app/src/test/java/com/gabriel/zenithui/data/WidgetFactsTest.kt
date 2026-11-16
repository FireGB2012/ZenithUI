package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

class WidgetFactsTest {

    // ------------------------------------------------------------------ the clock

    @Test
    fun `the greeting follows the hour, and uses a name when there is one`() {
        assertEquals("Good morning, Gabriel", Greeting.of(8, "Gabriel"))
        assertEquals("Good afternoon", Greeting.of(13, ""))
        assertEquals("Good evening, Sam", Greeting.of(19, " Sam "))
        assertEquals("Good night", Greeting.of(23, "  "))
        assertEquals("Good night", Greeting.of(3, ""))
        assertEquals("Good morning", Greeting.of(5, ""))
    }

    @Test
    fun `a time is written the way the phone writes it`() {
        val at = LocalDateTime.of(2026, 11, 16, 9, 5)
        assertEquals("09:05", ClockWords.time(at, h24 = true))
        assertEquals("9:05", ClockWords.time(at, h24 = false))
        assertEquals("21:40", ClockWords.time(at.withHour(21).withMinute(40), h24 = true))
        assertEquals("9:40", ClockWords.time(at.withHour(21).withMinute(40), h24 = false))
        assertEquals("12:00", ClockWords.time(at.withHour(0).withMinute(0), h24 = false))
        assertEquals("PM", ClockWords.half(at.withHour(12), h24 = false))
        assertEquals("AM", ClockWords.half(at.withHour(0), h24 = false))
        assertNull(ClockWords.half(at, h24 = true))
    }

    @Test
    fun `an hour of the forecast is labelled as the phone tells the time`() {
        assertEquals("17", ClockWords.hour("17", h24 = true))
        assertEquals("5PM", ClockWords.hour("17", h24 = false))
        assertEquals("12AM", ClockWords.hour("00", h24 = false))
        assertEquals("12PM", ClockWords.hour("12", h24 = false))
        // Something unreadable is shown as it came.
        assertEquals("x", ClockWords.hour("x", h24 = false))
    }

    @Test
    fun `the next alarm says its day only when it is not today`() {
        val zone = ZoneId.of("Europe/London")
        val now = LocalDateTime.of(2026, 11, 16, 22, 0).atZone(zone).toInstant().toEpochMilli()
        fun at(day: Int, hour: Int) = LocalDateTime.of(2026, 11, day, hour, 0).atZone(zone).toInstant().toEpochMilli()
        assertEquals("23:00", ClockWords.alarm(at(16, 23), now, zone, h24 = true))
        assertEquals("Tomorrow 07:00", ClockWords.alarm(at(17, 7), now, zone, h24 = true))
        assertEquals("Tomorrow 7:00 AM", ClockWords.alarm(at(17, 7), now, zone, h24 = false))
        assertEquals("Thu 07:00", ClockWords.alarm(at(19, 7), now, zone, h24 = true, locale = java.util.Locale.UK))
        // An alarm more than a week off is not worth a place on the clock.
        assertNull(ClockWords.alarm(at(28, 7), now, zone, h24 = true))
        assertNull(ClockWords.alarm(now - 1, now, zone, h24 = true))
    }

    @Test
    fun `a clock face is a day face from six in the morning to six in the evening`() {
        assertTrue(ClockWords.dayFace(6))
        assertTrue(ClockWords.dayFace(17))
        assertFalse(ClockWords.dayFace(18))
        assertFalse(ClockWords.dayFace(5))
        assertFalse(ClockWords.dayFace(0))
    }

    // ------------------------------------------------------------------ the weather

    @Test
    fun `temperatures run from blue in the cold to red in the heat`() {
        val freezing = TemperatureScale.argbAt(-10.0)
        val mild = TemperatureScale.argbAt(18.0)
        val hot = TemperatureScale.argbAt(38.0)
        fun red(argb: Int) = (argb shr 16) and 0xFF
        fun blue(argb: Int) = argb and 0xFF
        assertTrue(blue(freezing) > red(freezing))
        assertTrue(red(hot) > blue(hot))
        // Somewhere between the two ends is neither.
        assertTrue(mild != freezing && mild != hot)
        // Past the ends it stays at the ends.
        assertEquals(TemperatureScale.argbAt(-40.0), TemperatureScale.argbAt(-80.0))
        assertEquals(TemperatureScale.argbAt(45.0), TemperatureScale.argbAt(60.0))
    }

    @Test
    fun `fahrenheit is read as celsius for the colours`() {
        assertEquals(0.0, TemperatureScale.celsius(32.0, "f"), 0.001)
        assertEquals(100.0, TemperatureScale.celsius(212.0, "f"), 0.001)
        assertEquals(21.0, TemperatureScale.celsius(21.0, "c"), 0.001)
    }

    @Test
    fun `a day's bar is lit from its low to its high across the week's range`() {
        val (from, to) = TemperatureScale.span(low = 10.0, high = 20.0, coldest = 0.0, warmest = 40.0)
        assertEquals(0.25f, from, 0.001f)
        assertEquals(0.5f, to, 0.001f)
        // A week of one temperature lights the whole bar.
        assertEquals(0f to 1f, TemperatureScale.span(5.0, 5.0, 5.0, 5.0))
    }

    // ------------------------------------------------------------------ the battery

    @Test
    fun `the battery is green, yellow in battery saver, and red when low`() {
        assertEquals(BatteryTone.Good, BatteryFacts.tone(80, charging = false, saver = false))
        assertEquals(BatteryTone.Saver, BatteryFacts.tone(80, charging = false, saver = true))
        assertEquals(BatteryTone.Low, BatteryFacts.tone(20, charging = false, saver = false))
        // Charging is never red: it is on its way back up.
        assertEquals(BatteryTone.Good, BatteryFacts.tone(8, charging = true, saver = false))
        // Saver says so whatever the charge.
        assertEquals(BatteryTone.Saver, BatteryFacts.tone(8, charging = false, saver = true))
    }

    @Test
    fun `a length of time is said in hours and minutes`() {
        assertEquals("1 h 5 min", BatteryFacts.duration(65 * 60_000L))
        assertEquals("40 min", BatteryFacts.duration(40 * 60_000L))
        assertEquals("2 h", BatteryFacts.duration(120 * 60_000L))
        assertEquals("1 min", BatteryFacts.duration(20_000L))
        assertNull(BatteryFacts.duration(-1L))
        assertNull(BatteryFacts.duration(0L))
    }

    @Test
    fun `the battery says what it is doing in one line`() {
        assertEquals("Charging", BatteryFacts.status(50, charging = true, saver = false, toFull = -1L))
        assertEquals("Full in 40 min", BatteryFacts.status(70, charging = true, saver = false, toFull = 40 * 60_000L))
        assertEquals("Charged", BatteryFacts.status(100, charging = true, saver = false, toFull = 0L))
        // A phone that cannot say how long is left says nothing of being full until it is.
        assertEquals("Charging", BatteryFacts.status(76, charging = true, saver = false, toFull = 0L))
        assertEquals("Battery saver on", BatteryFacts.status(60, charging = false, saver = true, toFull = -1L))
        assertEquals("Low battery", BatteryFacts.status(15, charging = false, saver = false, toFull = -1L))
        assertEquals("On battery", BatteryFacts.status(60, charging = false, saver = false, toFull = -1L))
    }

    @Test
    fun `where the power comes from and how the battery is, in words`() {
        assertEquals("USB", BatteryFacts.source(2))
        assertEquals("Charger", BatteryFacts.source(1))
        assertEquals("Wireless", BatteryFacts.source(4))
        assertEquals("Battery", BatteryFacts.source(0))
        assertEquals("Good", BatteryFacts.health(2))
        assertEquals("Too hot", BatteryFacts.health(3))
        assertEquals("Too cold", BatteryFacts.health(7))
        assertEquals("Unknown", BatteryFacts.health(99))
    }

    @Test
    fun `the battery's temperature follows the weather's units`() {
        assertEquals("31°C", BatteryFacts.temperature(312, "c"))
        assertEquals("88°F", BatteryFacts.temperature(312, "f"))
        assertNull(BatteryFacts.temperature(Int.MIN_VALUE, "c"))
    }

    @Test
    fun `the battery's voltage is said in volts`() {
        assertEquals("4.12 V", BatteryFacts.voltage(4120))
        assertEquals("3.80 V", BatteryFacts.voltage(3800))
        assertNull(BatteryFacts.voltage(0))
        assertNull(BatteryFacts.voltage(-1))
    }

    // ------------------------------------------------------------------ the day's events

    @Test
    fun `an event's times are written the way the phone tells the time`() {
        val zone = ZoneId.of("Europe/London")
        fun at(hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 11, 16, hour, minute).atZone(zone).toInstant().toEpochMilli()
        assertEquals("All day", EventWords.span(0L, 0L, allDay = true, zone = zone, h24 = true))
        assertEquals("17:30–18:15", EventWords.span(at(17, 30), at(18, 15), allDay = false, zone = zone, h24 = true))
        // In 12 hours the half of the day is said once when both ends are in it, and at each end when not.
        assertEquals("5:30–6:15 PM", EventWords.span(at(17, 30), at(18, 15), allDay = false, zone = zone, h24 = false))
        assertEquals("11:30 AM–12:15 PM", EventWords.span(at(11, 30), at(12, 15), allDay = false, zone = zone, h24 = false))
    }

    @Test
    fun `how long until an event, said as a person would`() {
        val now = 1_000_000_000L
        fun minutes(m: Long) = now + m * 60_000L
        assertEquals("Now", EventWords.until(minutes(-10), minutes(20), now))
        assertEquals("Now", EventWords.until(now + 20_000L, minutes(30), now))
        assertEquals("In 5 min", EventWords.until(minutes(5), minutes(30), now))
        assertEquals("In 1 h 15 min", EventWords.until(minutes(75), minutes(90), now))
        // Further off, the hours are enough.
        assertEquals("In 4 h", EventWords.until(minutes(4 * 60 + 20), minutes(5 * 60), now))
        // One that is over has nothing to say.
        assertNull(EventWords.until(minutes(-60), minutes(-5), now))
    }

    @Test
    fun `when an event is, from the fullest way to say it to the shortest`() {
        val zone = ZoneId.of("Europe/London")
        fun at(hour: Int, minute: Int = 0) = LocalDateTime.of(2026, 11, 16, hour, minute).atZone(zone).toInstant().toEpochMilli()
        val now = at(17, 12)
        // Soon: when, and how long until; at the least, how long until.
        assertEquals(
            listOf("5:30–6:15 PM · In 18 min", "5:30 PM · In 18 min", "In 18 min"),
            EventWords.lines(at(17, 30), at(18, 15), allDay = false, nowMillis = now, zone = zone, h24 = false),
        )
        // Under way: till when.
        assertEquals(listOf("Now · till 18:15", "Now"), EventWords.lines(at(17, 0), at(18, 15), allDay = false, nowMillis = now, zone = zone, h24 = true))
        // Hours off, the time says more than the wait.
        assertEquals(
            listOf("21:00–22:00 · In 3 h", "21:00–22:00", "21:00"),
            EventWords.lines(at(21, 0), at(22, 0), allDay = false, nowMillis = now, zone = zone, h24 = true),
        )
        assertEquals(listOf("All day"), EventWords.lines(0L, 0L, allDay = true, nowMillis = now, zone = zone, h24 = true))
        // One that is over is only when it was.
        assertEquals(listOf("5:30–6:15 PM"), EventWords.lines(at(17, 30), at(18, 15), allDay = false, nowMillis = at(19, 0), zone = zone, h24 = false))
    }

    // ------------------------------------------------------------------ the world clock

    @Test
    fun `a city is shortened to three letters, or the first letters of its words`() {
        assertEquals("LON", CityCode.of("London"))
        assertEquals("TOK", CityCode.of("Tokyo"))
        assertEquals("NY", CityCode.of("New York"))
        assertEquals("HCM", CityCode.of("Ho Chi Minh"))
        assertEquals("SJ", CityCode.of("St Johns"))
        assertEquals("", CityCode.of("  "))
    }

    // ------------------------------------------------------------------ the hours' chart

    @Test
    fun `the hours are drawn from the coldest at the foot to the warmest at the top`() {
        assertEquals(listOf(0f, 0.5f, 1f), HourChart.levels(listOf(10.0, 15.0, 20.0)))
        assertEquals(listOf(1f, 0f), HourChart.levels(listOf(3.0, -1.0)))
        // Hours all at one temperature run level through the middle.
        assertEquals(listOf(0.5f, 0.5f), HourChart.levels(listOf(7.0, 7.0)))
        assertEquals(emptyList<Float>(), HourChart.levels(emptyList()))
    }

    // ------------------------------------------------------------------ music

    @Test
    fun `how far into a song is said in minutes and seconds`() {
        assertEquals("0:00", TrackTime.of(0L))
        assertEquals("0:00", TrackTime.of(-5L))
        assertEquals("1:35", TrackTime.of(95_000L))
        assertEquals("4:03", TrackTime.of(243_400L))
        assertEquals("1:02:03", TrackTime.of(3_723_000L))
    }

    // ------------------------------------------------------------------ the calendar

    @Test
    fun `the week runs from the first day the phone's week starts on`() {
        val wednesday = LocalDate.of(2026, 11, 18)
        val monday = CalendarFacts.week(wednesday, DayOfWeek.MONDAY)
        assertEquals(LocalDate.of(2026, 11, 16), monday.first())
        assertEquals(7, monday.size)
        val sunday = CalendarFacts.week(wednesday, DayOfWeek.SUNDAY)
        assertEquals(LocalDate.of(2026, 11, 15), sunday.first())
        assertTrue(wednesday in sunday)
    }

    @Test
    fun `a month is set out in whole weeks, with blanks before the first and after the last`() {
        val weeks = CalendarFacts.month(YearMonth.of(2026, 11), DayOfWeek.MONDAY)
        // 1 November 2026 is a Sunday: six blanks before it in a week from Monday.
        assertEquals(6, weeks.first().takeWhile { it == null }.size)
        assertEquals(LocalDate.of(2026, 11, 1), weeks.first()[6])
        assertTrue(weeks.all { it.size == 7 })
        assertEquals(30, weeks.flatten().count { it != null })
        assertEquals(LocalDate.of(2026, 11, 30), weeks.last().filterNotNull().last())
        // From Sunday it starts on the first column.
        assertEquals(LocalDate.of(2026, 11, 1), CalendarFacts.month(YearMonth.of(2026, 11), DayOfWeek.SUNDAY).first()[0])
    }

    // ------------------------------------------------------------------ the countdown

    @Test
    fun `a short wait is shown a dot a day, a longer one a dot a week`() {
        val days = CountdownDots.of(totalDays = 30, goneDays = 12)
        assertEquals(CountdownDots.Unit.Day, days.unit)
        assertEquals(30, days.total)
        assertEquals(12, days.gone)
        val weeks = CountdownDots.of(totalDays = 200, goneDays = 70)
        assertEquals(CountdownDots.Unit.Week, weeks.unit)
        assertEquals(29, weeks.total)
        assertEquals(10, weeks.gone)
        val months = CountdownDots.of(totalDays = 1500, goneDays = 300)
        assertEquals(CountdownDots.Unit.Month, months.unit)
        assertTrue(months.total <= CountdownDots.MOST)
        // A bigger field takes more dots: twelve weeks as days rather than weeks.
        val roomy = CountdownDots.of(totalDays = 84, goneDays = 44, most = 120)
        assertEquals(CountdownDots.Unit.Day, roomy.unit)
        assertEquals(84, roomy.total)
        assertEquals(44, roomy.gone)
        assertEquals(CountdownDots.Unit.Week, CountdownDots.of(totalDays = 84, goneDays = 44).unit)
        // Nothing gone past the end, and never none at all.
        assertEquals(30, CountdownDots.of(30, 99).gone)
        assertEquals(1, CountdownDots.of(0, 0).total)
    }

    // ------------------------------------------------------------------ the streak

    @Test
    fun `the week shows the streak's days behind today, and nothing ahead`() {
        val wednesday = LocalDate.of(2026, 11, 18)
        val week = StreakWeek.of(streak = 2, today = wednesday, firstDay = DayOfWeek.MONDAY)
        assertEquals(
            listOf(StreakWeek.Day.Missed, StreakWeek.Day.Kept, StreakWeek.Day.Today, StreakWeek.Day.Ahead, StreakWeek.Day.Ahead, StreakWeek.Day.Ahead, StreakWeek.Day.Ahead),
            week,
        )
        val long = StreakWeek.of(streak = 40, today = wednesday, firstDay = DayOfWeek.MONDAY)
        assertEquals(StreakWeek.Day.Kept, long[0])
        // With no streak today is still today, just not kept yet.
        assertEquals(StreakWeek.Day.Open, StreakWeek.of(0, wednesday, DayOfWeek.MONDAY)[2])
    }
}
