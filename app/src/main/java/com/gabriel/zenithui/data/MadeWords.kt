package com.gabriel.zenithui.data

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** What the pieces of a made widget that tell you something say. Plain functions, so they are tried in tests. */
object MadeWords {
    private val H24_SECONDS = DateTimeFormatter.ofPattern("HH:mm:ss")
    private val H12_SECONDS = DateTimeFormatter.ofPattern("h:mm:ss")

    /** What stands where a number is not known yet. */
    const val NO_NUMBER = "–"

    /** The time at [at], in the version [style], in 24 hours or 12 as the phone is set ([h24]). */
    fun time(at: LocalDateTime, style: String, h24: Boolean): String = when (style) {
        PieceStyles.TIME_SECONDS -> at.format(if (h24) H24_SECONDS else H12_SECONDS)
        PieceStyles.HOUR -> if (h24) "%02d".format(Locale.ROOT, at.hour) else twelve(at.hour).toString()
        PieceStyles.MINUTES -> "%02d".format(Locale.ROOT, at.minute)
        // Asked for, AM or PM is given whichever way the phone's clock is set.
        PieceStyles.TIME_HALF -> ClockWords.time(at, h24 = false) + " " + ClockWords.half(at, h24 = false)
        else -> ClockWords.time(at, h24)
    }

    private fun twelve(hour: Int): Int = if (hour % 12 == 0) 12 else hour % 12

    /** [day] in the version [style]. */
    fun date(day: LocalDate, style: String, locale: Locale = Locale.getDefault()): String {
        val pattern = when (style) {
            PieceStyles.WEEKDAY -> "EEEE"
            PieceStyles.DAY_MONTH -> "d MMMM"
            PieceStyles.DATE_FULL -> "EEEE d MMMM"
            PieceStyles.DATE_NUMBERS -> "dd/MM/yyyy"
            PieceStyles.DAY_NUMBER -> "d"
            PieceStyles.MONTH -> "MMMM"
            else -> "EEE d MMM"
        }
        return day.format(DateTimeFormatter.ofPattern(pattern, locale))
    }

    fun battery(level: Int): String = "${level.coerceIn(0, 100)}%"

    /** The weather from [forecast] in the version [style]; dashes before there is one. The sky alone is drawn, not written. */
    fun weather(forecast: Forecast?, style: String): String {
        fun degrees(value: Double?): String = value?.let(WeatherParse::degrees) ?: "$NO_NUMBER°"
        return when (style) {
            PieceStyles.WEATHER_WORDS -> forecast?.let { WeatherCodes.words(it.code, it.day) } ?: "Weather"
            PieceStyles.WEATHER_HIGH_LOW -> "H:${degrees(forecast?.high)} L:${degrees(forecast?.low)}"
            PieceStyles.WEATHER_SKY -> ""
            else -> degrees(forecast?.temperature)
        }
    }

    /** The next alarm, as the clock writes it, or that there is none. */
    fun alarm(next: String?): String = next ?: "No alarm"
}
