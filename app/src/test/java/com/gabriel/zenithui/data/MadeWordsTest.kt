package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.Locale

/** What the time, date, battery and weather pieces of a made widget say. */
class MadeWordsTest {

    private val afternoon = LocalDateTime.of(2026, 11, 18, 12, 45, 30)
    private val morning = LocalDateTime.of(2026, 11, 18, 9, 5, 7)
    private val evening = LocalDateTime.of(2026, 11, 18, 21, 5, 0)
    private val midnight = LocalDateTime.of(2026, 11, 18, 0, 30, 0)

    private fun time(at: LocalDateTime, style: String, h24: Boolean) = MadeWords.time(at, style, h24)

    @Test
    fun `the time as the phone tells it`() {
        assertEquals("12:45", time(afternoon, PieceStyles.TIME, h24 = true))
        assertEquals("09:05", time(morning, PieceStyles.TIME, h24 = true))
        assertEquals("9:05", time(morning, PieceStyles.TIME, h24 = false))
        assertEquals("9:05", time(evening, PieceStyles.TIME, h24 = false))
        assertEquals("21:05", time(evening, PieceStyles.TIME, h24 = true))
        assertEquals("12:30", time(midnight, PieceStyles.TIME, h24 = false))
    }

    @Test
    fun `with seconds, the hour or the minutes alone, and with AM or PM`() {
        assertEquals("12:45:30", time(afternoon, PieceStyles.TIME_SECONDS, h24 = true))
        assertEquals("9:05:07", time(morning, PieceStyles.TIME_SECONDS, h24 = false))
        assertEquals("09", time(morning, PieceStyles.HOUR, h24 = true))
        assertEquals("9", time(evening, PieceStyles.HOUR, h24 = false))
        assertEquals("12", time(midnight, PieceStyles.HOUR, h24 = false))
        assertEquals("05", time(morning, PieceStyles.MINUTES, h24 = false))
        assertEquals("45", time(afternoon, PieceStyles.MINUTES, h24 = true))
        // Asked for, AM or PM is given whichever way the phone's clock is set.
        assertEquals("12:45 PM", time(afternoon, PieceStyles.TIME_HALF, h24 = true))
        assertEquals("9:05 AM", time(morning, PieceStyles.TIME_HALF, h24 = false))
        assertEquals("12:30 AM", time(midnight, PieceStyles.TIME_HALF, h24 = false))
        // A version this Zenith does not know is the time as the phone tells it.
        assertEquals("21:05", time(evening, "sundial", h24 = true))
    }

    @Test
    fun `the date in each of its versions`() {
        val day = LocalDate.of(2026, 11, 18)
        fun date(style: String) = MadeWords.date(day, style, Locale.UK)
        assertEquals("Wed 18 Nov", date(PieceStyles.DATE_SHORT))
        assertEquals("Wednesday", date(PieceStyles.WEEKDAY))
        assertEquals("18 November", date(PieceStyles.DAY_MONTH))
        assertEquals("Wednesday 18 November", date(PieceStyles.DATE_FULL))
        assertEquals("18/11/2026", date(PieceStyles.DATE_NUMBERS))
        assertEquals("18", date(PieceStyles.DAY_NUMBER))
        assertEquals("November", date(PieceStyles.MONTH))
        assertEquals("Wed 18 Nov", date("tomorrow"))
    }

    @Test
    fun `the battery as a number`() {
        assertEquals("87%", MadeWords.battery(87))
        assertEquals("100%", MadeWords.battery(140))
        assertEquals("0%", MadeWords.battery(-3))
    }

    @Test
    fun `the weather as the temperature, the sky in words, both, or the day's high and low`() {
        val forecast = Forecast(
            place = "Oslo", lat = 59.9, lon = 10.7, units = "c",
            temperature = 11.6, code = 3, day = true, high = 14.2, low = -2.6,
            hours = emptyList(), fetchedAt = 0L,
        )
        fun weather(style: String) = MadeWords.weather(forecast, style)
        assertEquals("12°", weather(PieceStyles.WEATHER_TEMPERATURE))
        assertEquals("Cloudy", weather(PieceStyles.WEATHER_WORDS))
        assertEquals("12°", weather(PieceStyles.WEATHER_BOTH))
        assertEquals("H:14° L:−3°", weather(PieceStyles.WEATHER_HIGH_LOW))
        // Before there is a forecast, a dash where the number goes.
        assertEquals("–°", MadeWords.weather(null, PieceStyles.WEATHER_TEMPERATURE))
        assertEquals("Weather", MadeWords.weather(null, PieceStyles.WEATHER_WORDS))
        assertEquals("H:–° L:–°", MadeWords.weather(null, PieceStyles.WEATHER_HIGH_LOW))
    }

    @Test
    fun `the next alarm, or that there is none`() {
        assertEquals("Tomorrow 7:30 AM", MadeWords.alarm("Tomorrow 7:30 AM"))
        assertEquals("No alarm", MadeWords.alarm(null))
    }
}
