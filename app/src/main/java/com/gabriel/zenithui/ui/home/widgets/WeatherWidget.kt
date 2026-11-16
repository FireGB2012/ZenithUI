package com.gabriel.zenithui.ui.home.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.ClockWords
import com.gabriel.zenithui.data.Forecast
import com.gabriel.zenithui.data.HourChart
import com.gabriel.zenithui.data.TemperatureScale
import com.gabriel.zenithui.data.WeatherCodes
import com.gabriel.zenithui.data.WeatherDay
import com.gabriel.zenithui.data.WeatherParse
import com.gabriel.zenithui.data.WidgetForm
import com.gabriel.zenithui.ui.home.AsManyAsFit
import com.gabriel.zenithui.ui.home.LocalBare
import com.gabriel.zenithui.ui.home.LocalWidgetWorld
import com.gabriel.zenithui.ui.home.wordShadow
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.glassInk

/**
 * The Weather. Now, large, with the sky drawn in its own colours, and the day's range as a bar from
 * its low to its high in the colours of the temperatures, blue in the frost to red in the heat, with
 * a dot where it is now. The bigger ones chart the next hours as a line through their temperatures,
 * and the Big one lists the next days, each a bar across the week's range.
 *
 * - Mini: the sky and the temperature.
 * - Strip: the sky, the temperature, the place and the sky in words, and the high and low.
 * - Square: the place and the temperature, the sky, and today's bar.
 * - Wide: now on the left, the next hours charted on the right.
 * - Tall: now and today's bar at the top, the next hours down the widget.
 * - Big: now, the next hours charted, and the next days.
 */
@Composable
internal fun WeatherWidget(form: WidgetForm, width: Dp, height: Dp) {
    val world = LocalWidgetWorld.current
    val settings = world?.settings
    if (settings == null || settings.weatherPlace.isBlank()) {
        Fitted(form, width, height) { _, _ ->
            Invite(form, "Choose a place", "Tap to see its weather here", world?.let { { it.setUp("weather") } }) { size ->
                WeatherGlyph(0, true, size)
            }
        }
        return
    }
    val forecast = world.forecast?.takeIf { it.isFor(settings.weatherPlace, settings.weatherLat, settings.weatherLon, settings.weatherUnits) }
    if (forecast == null) {
        Fitted(form, width, height) { _, _ ->
            Invite(form, "Getting the weather", settings.weatherPlace, null) { size -> WeatherGlyph(2, true, size) }
        }
        return
    }
    val h24 = rememberH24()
    val temp = WeatherParse.degrees(forecast.temperature)
    val sky = WeatherCodes.words(forecast.code, forecast.day)
    val high = WeatherParse.degrees(forecast.high)
    val low = WeatherParse.degrees(forecast.low)
    val spoken = "${forecast.place}, $temp, $sky, high $high, low $low"

    Fitted(form, width, height) { w, _ ->
        val root = Modifier.fillMaxSize().says(spoken)
        when (form) {
            WidgetForm.Mini -> Column(
                root.padding(INSET_MINI),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                WeatherGlyph(forecast.code, forecast.day, 24.dp)
                Spacer(Modifier.height(1.dp))
                Hero(temp, 18.sp, weight = FontWeight.Medium, rolling = true)
            }

            WidgetForm.Strip -> Row(root.padding(horizontal = INSET_STRIP), verticalAlignment = Alignment.CenterVertically) {
                WeatherGlyph(forecast.code, forecast.day, 30.dp)
                Spacer(Modifier.width(10.dp))
                Hero(temp, 28.sp, rolling = true)
                Spacer(Modifier.width(12.dp))
                if (w >= 250.dp) {
                    Column(Modifier.weight(1f)) {
                        Place(forecast.place, Type.body)
                        Words(sky, Type.meta, color = soft())
                    }
                }
                Column(horizontalAlignment = if (w >= 250.dp) Alignment.End else Alignment.Start) {
                    Words("H $high", Type.meta, weight = FontWeight.SemiBold, tabular = true)
                    Words("L $low", Type.meta, color = soft(), tabular = true)
                }
            }

            WidgetForm.Square -> Column(root.padding(INSET)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Place(forecast.place, Type.headline)
                        Hero(temp, 46.sp, rolling = true)
                    }
                    WeatherGlyph(forecast.code, forecast.day, 34.dp)
                }
                Spacer(Modifier.weight(1f))
                Words(sky, Type.body, weight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                TodayBar(forecast)
            }

            WidgetForm.Wide -> Row(root.padding(INSET)) {
                Column(Modifier.width(118.dp).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                    Place(forecast.place, Type.headline)
                    Hero(temp, 46.sp, rolling = true)
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            WeatherGlyph(forecast.code, forecast.day, 16.dp)
                            Spacer(Modifier.width(5.dp))
                            Words(sky, Type.body, weight = FontWeight.SemiBold)
                        }
                        Spacer(Modifier.height(2.dp))
                        Words("H $high  L $low", Type.meta, color = soft())
                    }
                }
                Spacer(Modifier.width(10.dp))
                HoursChart(forecast, h24, Modifier.weight(1f).fillMaxHeight())
            }

            WidgetForm.Tall -> Column(root.padding(INSET)) {
                Place(forecast.place, Type.headline)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Hero(temp, 46.sp, rolling = true, modifier = Modifier.weight(1f))
                    WeatherGlyph(forecast.code, forecast.day, 38.dp)
                }
                Spacer(Modifier.height(4.dp))
                Words(sky, Type.body, weight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                TodayBar(forecast)
                Spacer(Modifier.height(14.dp))
                Hairline()
                Spacer(Modifier.height(4.dp))
                HoursList(forecast, h24, Modifier.weight(1f).fillMaxWidth())
            }

            WidgetForm.Big -> Column(root.padding(INSET)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Place(forecast.place, Type.headline)
                        Hero(temp, 50.sp, rolling = true)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        WeatherGlyph(forecast.code, forecast.day, 38.dp)
                        Spacer(Modifier.height(4.dp))
                        Words(sky, Type.body, weight = FontWeight.SemiBold)
                        Words("H $high  L $low", Type.meta, color = soft())
                    }
                }
                Spacer(Modifier.height(10.dp))
                HoursChart(forecast, h24, Modifier.fillMaxWidth().height(if (forecast.days.isEmpty()) 150.dp else 100.dp))
                if (forecast.days.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Hairline()
                    Spacer(Modifier.height(8.dp))
                    NextDays(forecast, Modifier.weight(1f).fillMaxWidth())
                }
            }
        }
    }
}

/** The place, with the little arrow that says it is the one chosen. */
@Composable
private fun Place(place: String, size: androidx.compose.ui.unit.TextUnit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Words(place, size, weight = FontWeight.SemiBold, modifier = Modifier.weight(1f, fill = false))
        Spacer(Modifier.width(3.dp))
        Glyph(Icons.Rounded.NearMe, (size.value * 0.72f).dp)
    }
}

/** A temperature in the forecast's units as the colour it is on the scale. */
private fun Forecast.colourOf(value: Double): Color = Color(TemperatureScale.argbAt(TemperatureScale.celsius(value, units)))

/** Today's low and high either side of a bar in their colours, with a dot where it is now. */
@Composable
private fun TodayBar(forecast: Forecast) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Words(WeatherParse.degrees(forecast.low), Type.meta, color = soft(), tabular = true)
        Spacer(Modifier.width(7.dp))
        RangeBar(
            from = forecast.colourOf(forecast.low),
            to = forecast.colourOf(forecast.high),
            span = 0f to 1f,
            now = TemperatureScale.span(forecast.temperature, forecast.temperature, forecast.low, forecast.high).first,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(7.dp))
        Words(WeatherParse.degrees(forecast.high), Type.meta, weight = FontWeight.SemiBold, tabular = true)
    }
}

/**
 * A bar across a range of temperatures: a faint track, lit along [span] (fractions from its left
 * end) from colour [from] to [to], and, with [now], a dot at that fraction. In a "No glass" widget
 * the lit part is a rod of glass.
 */
@Composable
private fun RangeBar(from: Color, to: Color, span: Pair<Float, Float>, now: Float?, modifier: Modifier) {
    val bare = LocalBare.current
    val track = faint()
    val dotRing = if (bare) Color.Transparent else Color.Black.copy(alpha = 0.35f)
    Canvas(modifier.height(10.dp).then(if (bare) Modifier.glassInk(bevel = 2.dp) else Modifier)) {
        val h = 5.dp.toPx()
        val top = (size.height - h) / 2f
        val round = CornerRadius(h / 2f)
        if (!bare) drawRoundRect(track, Offset(0f, top), Size(size.width, h), round)
        val start = size.width * span.first
        val length = (size.width * (span.second - span.first)).coerceAtLeast(h)
        val left = start.coerceAtMost(size.width - length)
        drawRoundRect(
            brush = if (bare) Brush.horizontalGradient(listOf(Color.White, Color.White)) else Brush.horizontalGradient(listOf(from, to), startX = left, endX = left + length),
            topLeft = Offset(left, top),
            size = Size(length, h),
            cornerRadius = round,
        )
        if (now != null) {
            val x = (size.width * now).coerceIn(h / 2f, size.width - h / 2f)
            drawCircle(dotRing, h * 0.95f, Offset(x, size.height / 2f))
            drawCircle(Color.White, h * 0.7f, Offset(x, size.height / 2f))
        }
    }
}

/** Now and the next hours as their hour, their sky and their temperature, one under another. */
@Composable
private fun HoursList(forecast: Forecast, h24: Boolean, modifier: Modifier) {
    val rows = remember(forecast, h24) {
        listOf(Triple("Now", forecast.code to forecast.day, forecast.temperature)) +
            forecast.hours.map { Triple(ClockWords.hour(it.hour, h24), it.code to it.day, it.temperature) }
    }
    AsManyAsFit(rows, modifier, gap = 2.dp) { (label, sky, temperature) ->
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Words(label, Type.body, weight = FontWeight.Medium, color = if (label == "Now") ink() else soft(), modifier = Modifier.width(46.dp))
            WeatherGlyph(sky.first, sky.second, 20.dp)
            Spacer(Modifier.weight(1f))
            Words(WeatherParse.degrees(temperature), Type.body, weight = FontWeight.SemiBold, tabular = true)
        }
    }
}

/**
 * Now and the next hours as a chart: across the top each hour and its sky, and under them a line
 * through their temperatures, each a dot with its temperature over it, the line coloured as each
 * temperature is. Now's dot is white.
 */
@Composable
private fun HoursChart(forecast: Forecast, h24: Boolean, modifier: Modifier) {
    val hours = remember(forecast, h24) {
        listOf(Triple("Now", forecast.code to forecast.day, forecast.temperature)) +
            forecast.hours.take(5).map { Triple(ClockWords.hour(it.hour, h24), it.code to it.day, it.temperature) }
    }
    val bare = LocalBare.current
    val measurer = rememberTextMeasurer(cacheSize = 16)
    val family = LocalZenithFont.current
    val ink = ink()
    val shadow = wordShadow()
    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            hours.forEach { (label, _, _) ->
                Words(label, Type.meta, weight = FontWeight.Medium, color = if (label == "Now") ink else soft(), align = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(5.dp))
        Row(Modifier.fillMaxWidth()) {
            hours.forEach { (_, sky, _) ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { WeatherGlyph(sky.first, sky.second, 20.dp) }
            }
        }
        Spacer(Modifier.height(2.dp))
        val levels = remember(hours) { HourChart.levels(hours.map { it.third }) }
        val colours = remember(hours, forecast.units) { hours.map { forecast.colourOf(it.third) } }
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .then(if (bare) Modifier.glassInk(bevel = 1.5.dp) else Modifier)
                .drawWithCache {
                    val style = TextStyle(fontFamily = family, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = ink, shadow = shadow)
                    val labels = hours.map { measurer.measure(WeatherParse.degrees(it.third), style) }
                    val step = size.width / hours.size
                    val labelRoom = (labels.maxOfOrNull { it.size.height } ?: 0) + 4.dp.toPx()
                    val dot = 3.5.dp.toPx()
                    val top = labelRoom + dot
                    val bottom = size.height - dot - 2.dp.toPx()
                    val points = levels.mapIndexed { i, level -> Offset(step * (i + 0.5f), bottom - (bottom - top).coerceAtLeast(0f) * level) }
                    val line = Path().apply {
                        if (points.isNotEmpty()) moveTo(points[0].x, points[0].y)
                        // A curve through the points, as smooth as a Catmull-Rom spline.
                        for (i in 0 until points.size - 1) {
                            val p0 = points[(i - 1).coerceAtLeast(0)]
                            val p1 = points[i]
                            val p2 = points[i + 1]
                            val p3 = points[(i + 2).coerceAtMost(points.size - 1)]
                            cubicTo(
                                p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f,
                                p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f,
                                p2.x, p2.y,
                            )
                        }
                    }
                    val stops = if (points.size > 1) {
                        points.mapIndexed { i, p -> (p.x - points.first().x) / (points.last().x - points.first().x) to (if (bare) Color.White else colours[i]) }.toTypedArray()
                    } else {
                        arrayOf(0f to Color.White, 1f to Color.White)
                    }
                    val brush = Brush.horizontalGradient(*stops, startX = points.firstOrNull()?.x ?: 0f, endX = points.lastOrNull()?.x ?: size.width)
                    onDrawBehind {
                        drawPath(line, brush, style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round))
                        points.forEachIndexed { i, p ->
                            if (i == 0) {
                                drawCircle(Color.White, dot * 1.2f, p)
                                drawCircle(if (bare) Color.White else colours[i], dot * 0.6f, p)
                            } else {
                                drawCircle(if (bare) Color.White else colours[i], dot, p)
                            }
                            val label = labels[i]
                            drawText(label, topLeft = Offset(p.x - label.size.width / 2f, p.y - dot - 3.dp.toPx() - label.size.height))
                        }
                    }
                }
        )
    }
}

/**
 * The next days, one to a row: the day, its sky, its low, a bar across the week from the coldest of
 * the days to the warmest lit from its low to its high, and its high. As many as there is room for.
 */
@Composable
private fun NextDays(forecast: Forecast, modifier: Modifier) {
    val days = forecast.days
    val coldest = days.minOf { it.low }
    val warmest = days.maxOf { it.high }
    AsManyAsFit(days, modifier, gap = 3.dp) { day -> DayRow(forecast, day, coldest, warmest) }
}

@Composable
private fun DayRow(forecast: Forecast, day: WeatherDay, coldest: Double, warmest: Double) {
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Words(day.day, Type.body, weight = FontWeight.SemiBold, modifier = Modifier.width(44.dp))
        Box(Modifier.width(28.dp), contentAlignment = Alignment.Center) { WeatherGlyph(day.code, true, 20.dp) }
        Words(WeatherParse.degrees(day.low), Type.body, color = soft(), align = TextAlign.End, tabular = true, modifier = Modifier.width(38.dp))
        RangeBar(
            from = forecast.colourOf(day.low),
            to = forecast.colourOf(day.high),
            span = TemperatureScale.span(day.low, day.high, coldest, warmest),
            now = null,
            modifier = Modifier.weight(1f).padding(horizontal = 9.dp),
        )
        Words(WeatherParse.degrees(day.high), Type.body, weight = FontWeight.SemiBold, align = TextAlign.End, tabular = true, modifier = Modifier.width(34.dp))
    }
}
