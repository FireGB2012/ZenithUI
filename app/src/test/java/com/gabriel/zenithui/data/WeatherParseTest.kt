package com.gabriel.zenithui.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherParseTest {

    /** Cut down from a real answer of Open-Meteo's forecast, keeping its shape. */
    private val answer = """
        {"latitude":59.92,"longitude":10.76,"timezone":"Europe/Oslo",
         "current":{"time":"2026-10-31T14:00","interval":900,"temperature_2m":6.4,"weather_code":61,"is_day":1},
         "hourly":{"time":["2026-10-31T13:00","2026-10-31T14:00","2026-10-31T15:00","2026-10-31T16:00","2026-10-31T17:00","2026-10-31T18:00","2026-10-31T19:00"],
                   "temperature_2m":[6.0,6.4,5.9,5.1,4.4,-0.4,-1.2],
                   "weather_code":[61,61,3,3,2,0,0]},
         "daily":{"time":["2026-10-31","2026-11-01"],"temperature_2m_max":[7.8,6.1],"temperature_2m_min":[1.2,-0.6]}}
    """.trimIndent()

    @Test
    fun `a forecast is read from Open-Meteo's answer`() {
        val f = WeatherParse.forecast(answer, "Oslo", 59.91, 10.75, "c", now = 42L)!!
        assertEquals(6.4, f.temperature, 0.0)
        assertEquals(61, f.code)
        assertTrue(f.day)
        assertEquals(7.8, f.high, 0.0)
        assertEquals(1.2, f.low, 0.0)
        // The hours after this one, five of them.
        assertEquals(listOf("15", "16", "17", "18", "19"), f.hours.map { it.hour })
        assertEquals(42L, f.fetchedAt)
        assertTrue(f.isFor("Oslo", 59.91, 10.75, "c"))
        // Switched to Fahrenheit, it is not the forecast asked for any more.
        assertTrue(!f.isFor("Oslo", 59.91, 10.75, "f"))
    }

    @Test
    fun `a forecast survives being kept and read back`() {
        val f = WeatherParse.forecast(answer, "Oslo", 59.91, 10.75, "c", now = 42L)!!
        assertEquals(f, WeatherParse.fromJson(WeatherParse.toJson(f)))
    }

    @Test
    fun `anything that is not a forecast is nothing`() {
        assertNull(WeatherParse.forecast("""{"error":true,"reason":"Latitude must be in range"}""", "x", 0.0, 0.0, "c", 0L))
        assertNull(WeatherParse.forecast("not json", "x", 0.0, 0.0, "c", 0L))
    }

    @Test
    fun `places are read from Open-Meteo's search, each with where it is`() {
        val found = WeatherParse.places(
            """{"results":[
                {"id":3143244,"name":"Oslo","latitude":59.91273,"longitude":10.74609,"country":"Norway","admin1":"Oslo"},
                {"id":1,"name":"Oslo","latitude":48.19,"longitude":-97.13,"country":"United States","admin1":"Minnesota"}]}"""
        )
        assertEquals(2, found.size)
        assertEquals(Place("Oslo", "Oslo, Norway", 59.91273, 10.74609), found[0])
        assertEquals("Minnesota, United States", found[1].region)
        // Nothing found comes back as no results at all.
        assertTrue(WeatherParse.places("""{"generationtime_ms":0.3}""").isEmpty())
    }

    @Test
    fun `the sky is one of a few kinds, and temperatures are whole degrees`() {
        assertEquals(WeatherCodes.Kind.Clear, WeatherCodes.kind(0))
        assertEquals(WeatherCodes.Kind.Rain, WeatherCodes.kind(81))
        assertEquals(WeatherCodes.Kind.Snow, WeatherCodes.kind(75))
        assertEquals(WeatherCodes.Kind.Storm, WeatherCodes.kind(99))
        assertEquals("Clear", WeatherCodes.words(0, day = false))
        assertEquals("Sunny", WeatherCodes.words(0, day = true))
        assertEquals("6°", WeatherParse.degrees(6.4))
        assertEquals("−1°", WeatherParse.degrees(-0.6))
        assertEquals("0°", WeatherParse.degrees(-0.4))
    }
}
