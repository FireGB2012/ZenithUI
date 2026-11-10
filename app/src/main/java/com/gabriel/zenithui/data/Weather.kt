package com.gabriel.zenithui.data

import android.content.Context
import androidx.compose.runtime.Immutable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** A place to have the weather for, as the search finds it. */
@Immutable
data class Place(val name: String, val region: String, val lat: Double, val lon: Double)

/**
 * One hour ahead, for the row of hours on the bigger Weather widgets. [day] is whether it falls in
 * daylight, so a clear night shows a moon; an hour kept from before Zenith asked is daylight, as all
 * of them were drawn then.
 */
@Immutable
data class WeatherHour(val hour: String, val temperature: Double, val code: Int, val day: Boolean = true)

/** One day ahead, for the days on the Large Weather widget: its weekday's short name, its sky, and its high and low. */
@Immutable
data class WeatherDay(val day: String, val code: Int, val high: Double, val low: Double)

/** The weather for one place, as last fetched. */
@Immutable
data class Forecast(
    val place: String,
    val lat: Double,
    val lon: Double,
    val units: String,
    val temperature: Double,
    val code: Int,
    val day: Boolean,
    val high: Double,
    val low: Double,
    val hours: List<WeatherHour>,
    val fetchedAt: Long,
    /**
     * The next days, from tomorrow, at most [WeatherParse.DAYS]. None in a forecast kept from before
     * Zenith asked for them, which still reads, until the next one is fetched.
     */
    val days: List<WeatherDay> = emptyList(),
) {
    /** Whether this is the forecast for what has been chosen, rather than for somewhere chosen before. */
    fun isFor(place: String, lat: Double, lon: Double, units: String): Boolean =
        this.place == place && this.lat == lat && this.lon == lon && this.units == units
}

/**
 * The sky in WMO's numbers, the ones Open-Meteo answers in, as the kinds Zenith draws: one symbol
 * and one word each.
 */
object WeatherCodes {
    enum class Kind { Clear, PartlyCloudy, Cloudy, Fog, Drizzle, Rain, Snow, Storm }

    fun kind(code: Int): Kind = when (code) {
        0 -> Kind.Clear
        1, 2 -> Kind.PartlyCloudy
        3 -> Kind.Cloudy
        45, 48 -> Kind.Fog
        51, 53, 55, 56, 57 -> Kind.Drizzle
        61, 63, 65, 66, 67, 80, 81, 82 -> Kind.Rain
        71, 73, 75, 77, 85, 86 -> Kind.Snow
        95, 96, 99 -> Kind.Storm
        else -> Kind.Cloudy
    }

    fun words(code: Int, day: Boolean): String = when (kind(code)) {
        Kind.Clear -> if (day) "Sunny" else "Clear"
        Kind.PartlyCloudy -> "Partly cloudy"
        Kind.Cloudy -> "Cloudy"
        Kind.Fog -> "Fog"
        Kind.Drizzle -> "Drizzle"
        Kind.Rain -> "Rain"
        Kind.Snow -> "Snow"
        Kind.Storm -> "Thunderstorm"
    }
}

/** Reading what Open-Meteo answers. Plain functions, so they are tried against real answers in tests. */
object WeatherParse {

    /** How many days ahead a forecast keeps: the Large Weather widget lists five, as an iPhone's does. */
    const val DAYS = 5

    /**
     * The forecast in an answer from Open-Meteo's forecast, or null if it is not one. The next days'
     * names are their weekdays' short names in [locale].
     */
    fun forecast(
        json: String,
        place: String,
        lat: Double,
        lon: Double,
        units: String,
        now: Long,
        locale: Locale = Locale.getDefault(),
    ): Forecast? = runCatching {
        val root = JSONObject(json)
        val current = root.getJSONObject("current")
        val currentTime = current.optString("time")
        val daily = root.getJSONObject("daily")
        val hourly = root.optJSONObject("hourly")
        val hours = buildList {
            if (hourly != null) {
                val times = hourly.getJSONArray("time")
                val temps = hourly.getJSONArray("temperature_2m")
                val codes = hourly.getJSONArray("weather_code")
                // Whether each hour is in daylight; an answer without it has every hour in daylight.
                val daylight = hourly.optJSONArray("is_day")
                // The hours after this one: ISO times in one zone sort as their text does.
                var start = 0
                while (start < times.length() && times.getString(start) <= currentTime) start++
                for (i in start until minOf(start + 5, times.length())) {
                    add(
                        WeatherHour(
                            times.getString(i).substringAfter('T').take(2),
                            temps.getDouble(i),
                            codes.getInt(i),
                            day = daylight?.optInt(i, 1) != 0,
                        )
                    )
                }
            }
        }
        Forecast(
            place = place,
            lat = lat,
            lon = lon,
            units = units,
            temperature = current.getDouble("temperature_2m"),
            code = current.getInt("weather_code"),
            day = current.optInt("is_day", 1) == 1,
            high = daily.getJSONArray("temperature_2m_max").getDouble(0),
            low = daily.getJSONArray("temperature_2m_min").getDouble(0),
            hours = hours,
            fetchedAt = now,
            days = days(daily, currentTime.substringBefore('T'), locale),
        )
    }.getOrNull()

    /**
     * The days after [today] (an ISO date) in an answer's daily forecast, [DAYS] at most, each named
     * by its weekday in [locale]. A day with anything missing is left out, and an answer without the
     * days' skies has none: the days are extra, and must never cost the forecast for now.
     */
    private fun days(daily: JSONObject, today: String, locale: Locale): List<WeatherDay> = runCatching {
        val dates = daily.getJSONArray("time")
        val codes = daily.getJSONArray("weather_code")
        val highs = daily.getJSONArray("temperature_2m_max")
        val lows = daily.getJSONArray("temperature_2m_min")
        buildList {
            for (i in 0 until dates.length()) {
                if (size == DAYS) break
                val date = dates.optString(i)
                // Today is the widget's top line, so the list starts tomorrow. ISO dates sort as
                // their text does, as the hours do.
                if (date <= today) continue
                val weekday = runCatching { LocalDate.parse(date).dayOfWeek }.getOrNull() ?: continue
                val code = codes.optInt(i, -1)
                val high = highs.optDouble(i)
                val low = lows.optDouble(i)
                if (code < 0 || high.isNaN() || low.isNaN()) continue
                add(WeatherDay(weekday.getDisplayName(TextStyle.SHORT, locale), code, high, low))
            }
        }
    }.getOrDefault(emptyList())

    /** The places in an answer from Open-Meteo's place search. None when nothing was found. */
    fun places(json: String): List<Place> = runCatching {
        val results = JSONObject(json).optJSONArray("results") ?: return emptyList()
        (0 until results.length()).mapNotNull { i ->
            val o = results.optJSONObject(i) ?: return@mapNotNull null
            val name = o.optString("name").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val region = listOf(o.optString("admin1"), o.optString("country")).filter { it.isNotBlank() }.distinct().joinToString(", ")
            Place(name, region, o.optDouble("latitude"), o.optDouble("longitude"))
        }
    }.getOrDefault(emptyList())

    /** A forecast as kept between runs, so the widget has something to show before it is fetched again. */
    fun toJson(f: Forecast): String = JSONObject().apply {
        put("place", f.place); put("lat", f.lat); put("lon", f.lon); put("units", f.units)
        put("temperature", f.temperature); put("code", f.code); put("day", f.day)
        put("high", f.high); put("low", f.low); put("fetchedAt", f.fetchedAt)
        put("hours", org.json.JSONArray().apply {
            f.hours.forEach { h -> put(JSONObject().apply { put("hour", h.hour); put("t", h.temperature); put("c", h.code); put("d", h.day) }) }
        })
        put("days", org.json.JSONArray().apply {
            f.days.forEach { d -> put(JSONObject().apply { put("day", d.day); put("c", d.code); put("hi", d.high); put("lo", d.low) }) }
        })
    }.toString()

    /**
     * A forecast kept by [toJson], or null if it is not one. One kept before the hours knew daylight
     * and before there were days reads too, with every hour in daylight and no days.
     */
    fun fromJson(text: String): Forecast? = runCatching {
        val o = JSONObject(text)
        val hours = o.optJSONArray("hours")
        val days = o.optJSONArray("days")
        Forecast(
            place = o.getString("place"), lat = o.getDouble("lat"), lon = o.getDouble("lon"), units = o.optString("units", "c"),
            temperature = o.getDouble("temperature"), code = o.getInt("code"), day = o.optBoolean("day", true),
            high = o.getDouble("high"), low = o.getDouble("low"),
            hours = if (hours == null) emptyList() else (0 until hours.length()).map { i ->
                val h = hours.getJSONObject(i)
                WeatherHour(h.getString("hour"), h.getDouble("t"), h.getInt("c"), h.optBoolean("d", true))
            },
            fetchedAt = o.optLong("fetchedAt"),
            days = if (days == null) emptyList() else (0 until days.length()).map { i ->
                val d = days.getJSONObject(i)
                WeatherDay(d.getString("day"), d.getInt("c"), d.getDouble("hi"), d.getDouble("lo"))
            },
        )
    }.getOrNull()

    /** A temperature as the widget writes it: whole degrees, with the sign a minus needs. */
    fun degrees(value: Double): String {
        val whole = Math.round(value).toInt()
        return if (whole < 0) "−${-whole}°" else "$whole°"
    }
}

/**
 * The Weather widget's forecast. Nothing is fetched until a place has been chosen; from then on,
 * whenever the home screen is shown and the last forecast is more than half an hour old, the
 * place's latitude and longitude — and nothing else — go to Open-Meteo, a free weather service,
 * and its answer is kept on the phone.
 */
class WeatherRepository(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("weather", Context.MODE_PRIVATE)

    private val _forecast = MutableStateFlow(prefs.getString(KEY, null)?.let(WeatherParse::fromJson))
    val forecast: StateFlow<Forecast?> = _forecast.asStateFlow()

    /** Fetches the forecast for the chosen place, unless the one kept is for it and recent enough. */
    suspend fun refresh(place: String, lat: Double, lon: Double, units: String, force: Boolean = false) {
        if (place.isBlank()) return
        val kept = _forecast.value
        val now = System.currentTimeMillis()
        if (!force && kept != null && kept.isFor(place, lat, lon, units) && now - kept.fetchedAt < STALE_MS) return
        val unit = if (units == "f") "&temperature_unit=fahrenheit" else ""
        // Only the place goes, as its latitude and longitude; the rest says what to answer with:
        // now, the hours and whether each is in daylight, and today and the next days.
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,weather_code,is_day&hourly=temperature_2m,weather_code,is_day" +
            "&daily=weather_code,temperature_2m_max,temperature_2m_min&forecast_days=${WeatherParse.DAYS + 1}&timezone=auto$unit"
        val text = get(url) ?: return
        val fresh = WeatherParse.forecast(text, place, lat, lon, units, now) ?: return
        _forecast.value = fresh
        prefs.edit().putString(KEY, WeatherParse.toJson(fresh)).apply()
    }

    /** Places called [query], for choosing one. Sends only what was typed. */
    suspend fun search(query: String): List<Place> {
        val words = query.trim()
        if (words.length < 2) return emptyList()
        val url = "https://geocoding-api.open-meteo.com/v1/search?count=8&language=en&format=json&name=" +
            URLEncoder.encode(words, "UTF-8")
        return get(url)?.let(WeatherParse::places).orEmpty()
    }

    private suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.connectTimeout = 8_000
            connection.readTimeout = 8_000
            connection.setRequestProperty("User-Agent", "ZenithUI")
            try {
                if (connection.responseCode !in 200..299) null else connection.inputStream.use { it.readBytes().decodeToString() }
            } finally {
                connection.disconnect()
            }
        }.getOrNull()
    }

    private companion object {
        const val KEY = "forecast"
        const val STALE_MS = 30 * 60 * 1000L
    }
}
