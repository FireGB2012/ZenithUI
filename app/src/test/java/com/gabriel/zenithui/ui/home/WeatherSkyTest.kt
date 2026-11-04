package com.gabriel.zenithui.ui.home

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.gabriel.zenithui.data.WeatherCodes
import com.gabriel.zenithui.ui.theme.contrast
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherSkyTest {

    private val kinds = WeatherCodes.Kind.entries

    @Test
    fun `white words stand four and a half to one on every sky, by day and by night`() {
        for (kind in kinds) {
            for (day in listOf(true, false)) {
                val sky = WeatherSky.of(kind, day)
                assertTrue("$kind day=$day top", contrast(Color.White, sky.top) >= 4.5f)
                assertTrue("$kind day=$day bottom", contrast(Color.White, sky.bottom) >= 4.5f)
            }
        }
    }

    @Test
    fun `a clear day is blue`() {
        val sky = WeatherSky.of(WeatherCodes.Kind.Clear, day = true)
        for (stop in listOf(sky.top, sky.bottom)) assertTrue(stop.blue > stop.red + 0.3f)
    }

    @Test
    fun `night is darker than day, whatever the weather`() {
        for (kind in kinds) {
            val day = WeatherSky.of(kind, day = true)
            val night = WeatherSky.of(kind, day = false)
            assertTrue("$kind top", night.top.luminance() < day.top.luminance())
            assertTrue("$kind bottom", night.bottom.luminance() < day.bottom.luminance())
        }
    }

    @Test
    fun `cloud and fog are grey`() {
        for (kind in listOf(WeatherCodes.Kind.Cloudy, WeatherCodes.Kind.Fog)) {
            val stop = WeatherSky.of(kind, day = true).bottom
            val spread = maxOf(stop.red, stop.green, stop.blue) - minOf(stop.red, stop.green, stop.blue)
            assertTrue("$kind: $spread", spread < 0.12f)
        }
    }

    @Test
    fun `rain is darker than cloud`() {
        val rain = WeatherSky.of(WeatherCodes.Kind.Rain, day = true).bottom
        val cloud = WeatherSky.of(WeatherCodes.Kind.Cloudy, day = true).bottom
        assertTrue(rain.luminance() < cloud.luminance())
    }

    @Test
    fun `a weather code finds its sky`() {
        assertTrue(WeatherSky.of(0, day = true) == WeatherSky.of(WeatherCodes.Kind.Clear, day = true))
        assertTrue(WeatherSky.of(63, day = false) == WeatherSky.of(WeatherCodes.Kind.Rain, day = false))
    }
}
