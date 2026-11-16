package com.gabriel.zenithui.ui.home.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.CityCode
import com.gabriel.zenithui.data.ClockWords
import com.gabriel.zenithui.data.WidgetForm
import com.gabriel.zenithui.data.WorldClocks
import com.gabriel.zenithui.ui.home.LocalWidgetWorld
import com.gabriel.zenithui.ui.home.rememberNow
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * The World Clock: the time in the cities you chose, each a face of its own, paper where it is day
 * there and night where it is night, with the city and how far it is from yours. Where there is no
 * room for faces, the times in figures.
 *
 * - Mini: the first city's face, its name shortened on it.
 * - Strip: the cities in a row, each its short name over its time.
 * - Square: a city to a line, how far from yours, and its time.
 * - Wide: the cities side by side, each a face over its name.
 * - Tall: a city to a row, each its face, its name and its time.
 * - Big: four faces, two by two, each with its city, time and how far.
 */
@Composable
internal fun WorldWidget(form: WidgetForm, width: Dp, height: Dp) {
    val settings = LocalWidgetWorld.current?.settings
    val zones = remember(settings?.worldClocks) {
        (settings?.worldClocks ?: WorldClocks.DEFAULT).take(WorldClocks.MOST)
            .filter { runCatching { ZoneId.of(it) }.isSuccess }
            .ifEmpty { listOf(ZoneId.systemDefault().id) }
    }
    val tick = rememberNow()
    val h24 = rememberH24()
    val instant = remember(tick) { Instant.now() }
    val here = remember { ZoneId.systemDefault() }
    val cities = remember(zones, tick, h24) {
        zones.map { zone ->
            val at = LocalDateTime.ofInstant(instant, ZoneId.of(zone))
            City(
                name = WorldClocks.city(zone),
                code = CityCode.of(WorldClocks.city(zone)),
                at = at,
                time = ClockWords.time(at, h24),
                half = ClockWords.half(at, h24),
                gap = WorldClocks.offset(zone, here, instant),
            )
        }
    }
    val spoken = cities.joinToString(", ") { "${it.name} ${it.time}${it.half?.let { h -> " $h" } ?: ""}" }

    Fitted(form, width, height) { w, h ->
        val root = Modifier.fillMaxSize().says(spoken)
        when (form) {
            WidgetForm.Mini -> Box(root.padding(5.dp)) {
                val city = cities.first()
                Dial(city.at, dialFaceAt(city.at.hour), Modifier.fillMaxSize(), label = city.code)
            }

            WidgetForm.Strip -> Row(root.padding(horizontal = INSET_STRIP), verticalAlignment = Alignment.CenterVertically) {
                val shown = cities.take(if (w >= 250.dp) 4 else 2)
                shown.forEachIndexed { i, city ->
                    if (i > 0) Box(Modifier.width(1.dp).height(26.dp).background(faint(0.2f)))
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Label(city.code)
                        Row(verticalAlignment = Alignment.Bottom) {
                            Words(city.time, Type.headline, weight = FontWeight.SemiBold, tabular = true)
                            if (city.half != null) Words(city.half, Type.caps, weight = FontWeight.Bold, color = soft(), modifier = Modifier.padding(start = 2.dp, bottom = 2.dp))
                        }
                    }
                }
            }

            WidgetForm.Square -> Column(root.padding(horizontal = INSET, vertical = 10.dp), verticalArrangement = Arrangement.SpaceEvenly) {
                cities.take(3).forEachIndexed { i, city ->
                    if (i > 0) Hairline()
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Words(city.name, Type.body, weight = FontWeight.SemiBold)
                            Gap(city)
                        }
                        Spacer(Modifier.width(6.dp))
                        CityTime(city, 20.sp)
                    }
                }
            }

            WidgetForm.Wide -> Row(root.padding(INSET), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                val shown = cities.take(4)
                val face = minOf((w - INSET * 2 - 12.dp * (shown.size - 1)) / shown.size, h - INSET * 2 - 36.dp)
                shown.forEach { city ->
                    Column(Modifier.width(face), horizontalAlignment = Alignment.CenterHorizontally) {
                        Dial(city.at, dialFaceAt(city.at.hour), Modifier.size(face))
                        Spacer(Modifier.height(7.dp))
                        Words(city.name, Type.meta, weight = FontWeight.SemiBold, align = TextAlign.Center)
                        Words(city.gap.substringBefore(','), Type.caps, weight = FontWeight.Medium, color = soft(), align = TextAlign.Center)
                    }
                }
            }

            WidgetForm.Tall -> Column(root.padding(INSET), verticalArrangement = Arrangement.SpaceBetween) {
                cities.take(4).forEach { city ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Dial(city.at, dialFaceAt(city.at.hour), Modifier.size(56.dp), seconds = false)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Words(city.name, Type.body, weight = FontWeight.SemiBold)
                            Row(verticalAlignment = Alignment.Bottom) {
                                Words(city.time, Type.meta, weight = FontWeight.SemiBold, tabular = true)
                                if (city.half != null) Words(" ${city.half}", Type.caps, weight = FontWeight.Bold, color = soft())
                            }
                            Gap(city)
                        }
                    }
                }
            }

            WidgetForm.Big -> Column(root.padding(INSET), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                cities.take(4).chunked(2).forEach { pair ->
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { city ->
                            Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Dial(city.at, dialFaceAt(city.at.hour), Modifier.weight(1f).aspectRatio(1f, matchHeightConstraintsFirst = true))
                                Spacer(Modifier.height(8.dp))
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Words(city.name, Type.body, weight = FontWeight.SemiBold, modifier = Modifier.weight(1f, fill = false))
                                    Spacer(Modifier.width(6.dp))
                                    Words(city.time + (city.half?.let { " $it" } ?: ""), Type.meta, weight = FontWeight.Medium, color = soft(), tabular = true)
                                }
                                Gap(city)
                            }
                        }
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

/** One of the cities, as the widget shows it. */
private data class City(val name: String, val code: String, val at: LocalDateTime, val time: String, val half: String?, val gap: String)

/** How far a city is from yours, and the day there when it is not yours: as much of that as fits. */
@Composable
private fun Gap(city: City) {
    FirstThatFits(listOf(city.gap, city.gap.substringBefore(',')).distinct(), Type.caps, color = soft())
}

/** A city's time in figures, with AM or PM after it, smaller, where the phone uses them. */
@Composable
private fun CityTime(city: City, size: androidx.compose.ui.unit.TextUnit) {
    Row(verticalAlignment = Alignment.Bottom) {
        Hero(city.time, size, weight = FontWeight.Normal)
        if (city.half != null) Words(city.half, Type.caps, weight = FontWeight.Bold, color = soft(), modifier = Modifier.padding(start = 2.dp, bottom = 1.dp))
    }
}
