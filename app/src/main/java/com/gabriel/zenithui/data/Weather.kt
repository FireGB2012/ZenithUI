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

/** A place to have the weather for, as the search finds it. */
@Immutable
data class Place(val name: String, val region: String, val lat: Double, val lon: Double)

/** One hour ahead, for the row of hours on the bigger Weather widgets. */
@Immutable
data class WeatherHour(val hour: String, val temperature: Double, val code: Int)

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

    /** The forecast in an answer from Open-Meteo's forecast, or null if it is not one. */
    fun forecast(json: String, place: String, lat: Double, lon: Double, units: String, now: Long): Forecast? = runCatching {
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
                // The hours after this one: ISO times in one zone sort as their text does.
                var start = 0
                while (start < times.length() && times.getString(start) <= currentTime) start++
                for (i in start until minOf(start + 5, times.length())) {
                    add(WeatherHour(times.getString(i).substringAfter('T').take(2), temps.getDouble(i), codes.getInt(i)))
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
        )
    }.getOrNull()

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
            f.hours.forEach { h -> put(JSONObject().apply { put("hour", h.hour); put("t", h.temperature); put("c", h.code) }) }
        })
    }.toString()

    fun fromJson(text: String): Forecast? = runCatching {
        val o = JSONObject(text)
        val hours = o.optJSONArray("hours")
        Forecast(
            place = o.getString("place"), lat = o.getDouble("lat"), lon = o.getDouble("lon"), units = o.optString("units", "c"),
            temperature = o.getDouble("temperature"), code = o.getInt("code"), day = o.optBoolean("day", true),
            high = o.getDouble("high"), low = o.getDouble("low"),
            hours = if (hours == null) emptyList() else (0 until hours.length()).map { i ->
                val h = hours.getJSONObject(i)
                WeatherHour(h.getString("hour"), h.getDouble("t"), h.getInt("c"))
            },
            fetchedAt = o.optLong("fetchedAt"),
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
        val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon" +
            "&current=temperature_2m,weather_code,is_day&hourly=temperature_2m,weather_code" +
            "&daily=temperature_2m_max,temperature_2m_min&forecast_days=2&timezone=auto$unit"
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
