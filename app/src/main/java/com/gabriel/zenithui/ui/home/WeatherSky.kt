package com.gabriel.zenithui.ui.home

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.gabriel.zenithui.data.WeatherCodes

/**
 * The sky a Weather tile is painted in, as the iPhone's weather widget is: a deep blue on a clear
 * day, navy on a clear night, grey under cloud and fog, slate in rain, a cold blue-grey in snow
 * and dark slate in a storm, each a little lighter at the foot than at the head. Every one of them
 * is deep enough for white words to stand four and a half to one on it, top and bottom.
 */
object WeatherSky {

    @Immutable
    data class Sky(val top: Color, val bottom: Color)

    fun of(code: Int, day: Boolean): Sky = of(WeatherCodes.kind(code), day)

    fun of(kind: WeatherCodes.Kind, day: Boolean): Sky = when (kind) {
        WeatherCodes.Kind.Clear -> if (day) sky(0xFF164E9C, 0xFF2A6FC0) else sky(0xFF0A1630, 0xFF1B2C55)
        WeatherCodes.Kind.PartlyCloudy -> if (day) sky(0xFF2E5A8A, 0xFF4A7299) else sky(0xFF141D30, 0xFF26324B)
        WeatherCodes.Kind.Cloudy -> if (day) sky(0xFF4E5864, 0xFF66707C) else sky(0xFF22272F, 0xFF343A44)
        WeatherCodes.Kind.Fog -> if (day) sky(0xFF555C66, 0xFF69707A) else sky(0xFF2A2E35, 0xFF3A3F47)
        WeatherCodes.Kind.Drizzle -> if (day) sky(0xFF3F5266, 0xFF56687C) else sky(0xFF1A222D, 0xFF2A3545)
        WeatherCodes.Kind.Rain -> if (day) sky(0xFF34465A, 0xFF4A5C70) else sky(0xFF161D27, 0xFF253040)
        WeatherCodes.Kind.Snow -> if (day) sky(0xFF4C6380, 0xFF607590) else sky(0xFF1F2836, 0xFF313E52)
        WeatherCodes.Kind.Storm -> if (day) sky(0xFF2B3140, 0xFF3E4658) else sky(0xFF12151C, 0xFF222733)
    }

    private fun sky(top: Long, bottom: Long) = Sky(Color(top), Color(bottom))
}
