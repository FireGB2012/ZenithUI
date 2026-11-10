package com.gabriel.zenithui.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.Place
import com.gabriel.zenithui.data.WorldClocks
import com.gabriel.zenithui.ui.common.GlassButton
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.SectionLabel
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.onboarding.Segmented
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.inkOnTint
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** A box to type in, as the search in Settings is: ringed in the accent while in use. */
@Composable
private fun SheetField(value: String, placeholder: String, onChange: (String) -> Unit, search: Boolean = true) {
    val palette = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    val ring by animateColorAsState(if (focused) palette.accent.copy(alpha = 0.75f) else Color.Transparent, tween(220), label = "fieldRing")
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.onGlass.copy(alpha = 0.08f))
            .border(1.5.dp, ring, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (search) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.onGlassMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) Text(placeholder, color = palette.onGlassMuted, fontSize = 14.sp)
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(fontFamily = LocalZenithFont.current, color = palette.onGlass, fontSize = 14.sp),
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = KeyboardOptions(
                    capitalization = if (search) KeyboardCapitalization.None else KeyboardCapitalization.Sentences,
                    imeAction = if (search) ImeAction.Search else ImeAction.Done,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused },
            )
        }
    }
}

// ---------------------------------------------------------------------- world clock

/**
 * The World clock's cities: the ones chosen, in their order, each with a way off, and under them a
 * search over every city there is a time zone for. Four at most, as the widget shows at its largest.
 */
@Composable
fun WorldCitiesSheet(visible: Boolean, chosen: List<String>, onChange: (List<String>) -> Unit, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    var query by remember { mutableStateOf("") }
    val everywhere = remember { WorldClocks.all() }
    val words = query.trim().lowercase()
    val found = remember(words, chosen) {
        everywhere.filter { it !in chosen && (words.isEmpty() || words in WorldClocks.city(it).lowercase() || words in WorldClocks.region(it).lowercase()) }
    }
    val clock = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val full = chosen.size >= WorldClocks.MOST

    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "World clock", maxHeightFraction = 0.86f) {
        Text(
            "Up to four cities, in the order the widget shows them.",
            color = palette.onGlassMuted,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 4.dp).appearIn(delayMillis = staggerDelay(0), riseFrom = 16f),
        )
        Spacer(Modifier.height(10.dp))
        chosen.forEachIndexed { index, zone ->
            key(zone) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .appearIn(delayMillis = staggerDelay(index + 1), riseFrom = 16f)
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CityIcon(zone)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(WorldClocks.city(zone), color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text(WorldClocks.offset(zone, ZoneId.systemDefault(), Instant.now()), color = palette.onGlassMuted, fontSize = 12.sp)
                    }
                    Text(runCatching { Instant.now().atZone(ZoneId.of(zone)).format(clock) }.getOrDefault(""), color = palette.onGlass, fontSize = 15.sp)
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .size(34.dp)
                            .pressBounce()
                            .clip(RoundedCornerShape(50))
                            .clickable { onChange(chosen - zone) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.Close, contentDescription = "Take ${WorldClocks.city(zone)} off", tint = palette.onGlassMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        AnimatedVisibility(visible = !full, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
            Column {
                Spacer(Modifier.height(8.dp))
                SheetField(query, "Find a city", { query = it })
                Spacer(Modifier.height(6.dp))
            }
        }
        if (!full) {
            LazyColumn(Modifier.weight(1f, fill = false)) {
                items(found, key = { it }) { zone ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onChange(chosen + zone)
                                query = ""
                            }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(WorldClocks.city(zone), color = palette.onGlass, fontSize = 14.sp)
                            Text(WorldClocks.region(zone), color = palette.onGlassMuted, fontSize = 12.sp)
                        }
                        Text(WorldClocks.offset(zone, ZoneId.systemDefault(), Instant.now()), color = palette.onGlassMuted, fontSize = 12.sp)
                    }
                }
            }
        } else {
            Text("That is four. Take one off to choose another.", color = palette.onGlassMuted, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
        }
    }
}

// ---------------------------------------------------------------------- countdown

/**
 * What the Countdown counts down to: a name for it, and the day, chosen on the month. Only days
 * from today on can be chosen; set, the widget's ring starts filling from today.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountdownSheet(
    visible: Boolean,
    title: String,
    day: Long,
    onSet: (String, Long) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    var name by remember(title) { mutableStateOf(title) }
    val today = remember { LocalDate.now() }
    val startMillis = remember(day) {
        (if (day > 0) LocalDate.ofEpochDay(day) else today.plusDays(30)).atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    }
    val state = rememberDatePickerState(
        initialSelectedDateMillis = startMillis,
        selectableDates = remember(today) {
            object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis / DAY_MS >= today.toEpochDay()
                override fun isSelectableYear(year: Int): Boolean = year >= today.year
            }
        },
    )

    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Countdown", maxHeightFraction = 0.92f) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            SheetField(name, "What it is to: a holiday, a birthday", { name = it.take(40) }, search = false)
            DatePicker(
                state = state,
                title = null,
                headline = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.Transparent,
                    titleContentColor = palette.onGlass,
                    headlineContentColor = palette.onGlass,
                    weekdayContentColor = palette.onGlassMuted,
                    subheadContentColor = palette.onGlassMuted,
                    navigationContentColor = palette.onGlass,
                    yearContentColor = palette.onGlass,
                    currentYearContentColor = palette.accent,
                    selectedYearContentColor = inkOnTint(palette.accent),
                    selectedYearContainerColor = palette.accent,
                    dayContentColor = palette.onGlass,
                    disabledDayContentColor = palette.onGlass.copy(alpha = 0.3f),
                    selectedDayContentColor = inkOnTint(palette.accent),
                    selectedDayContainerColor = palette.accent,
                    todayContentColor = palette.accent,
                    todayDateBorderColor = palette.accent,
                ),
            )
        }
        Row(Modifier.padding(top = 6.dp)) {
            if (day > 0) {
                GlassButton(text = "Clear", onClick = onClear, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(10.dp))
            }
            GlassButton(
                text = "Count down",
                filled = true,
                enabled = state.selectedDateMillis != null,
                onClick = { state.selectedDateMillis?.let { onSet(name, it / DAY_MS) } },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private const val DAY_MS = 86_400_000L

// ---------------------------------------------------------------------- weather

/**
 * Where the Weather widget's forecast is for. Says plainly what is sent and where before anything
 * is: the place typed into the search goes to Open-Meteo's place search, and the one chosen to its
 * forecast; nothing else, and nothing at all until something is typed.
 */
@Composable
fun WeatherPlaceSheet(
    visible: Boolean,
    place: String,
    units: String,
    search: suspend (String) -> List<Place>,
    onPick: (Place, String) -> Unit,
    onUnits: (String) -> Unit,
    onClear: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    var query by remember { mutableStateOf("") }
    // Asked a moment after typing stops, not for every letter.
    val results by produceState<List<Place>?>(initialValue = null, query) {
        if (query.trim().length < 2) {
            value = null
            return@produceState
        }
        delay(350)
        value = search(query)
    }

    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Weather", maxHeightFraction = 0.86f) {
        Text(
            "The place you search for goes to Open-Meteo, a free weather service, and so does the one you choose, " +
                "to get its forecast. Nothing else is sent, and nothing at all until you search.",
            color = palette.onGlassMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 4.dp).appearIn(delayMillis = staggerDelay(0), riseFrom = 16f),
        )
        Spacer(Modifier.height(12.dp))
        Box(Modifier.appearIn(delayMillis = staggerDelay(1), riseFrom = 16f)) {
            Segmented(
                options = listOf("c" to "Celsius", "f" to "Fahrenheit"),
                selected = units,
                onSelect = onUnits,
            )
        }
        Spacer(Modifier.height(12.dp))
        SheetField(query, if (place.isBlank()) "Find your town or city" else "Now: $place. Find another", { query = it })
        Spacer(Modifier.height(6.dp))
        val list = results
        when {
            list == null -> Unit
            list.isEmpty() -> Text("No place called “${query.trim()}” was found.", color = palette.onGlassMuted, fontSize = 13.sp, modifier = Modifier.padding(8.dp))
            else -> LazyColumn(Modifier.weight(1f, fill = false)) {
                items(list, key = { "${it.lat},${it.lon}" }) { found ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .appearIn(fromScale = 1f, riseFrom = 12f, gentle = true)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onPick(found, units) }
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = palette.onGlassMuted, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(found.name, color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            if (found.region.isNotBlank()) Text(found.region, color = palette.onGlassMuted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
        if (place.isNotBlank()) {
            SectionLabel("Or", Modifier.padding(start = 8.dp, top = 12.dp, bottom = 4.dp))
            MenuRow(Icons.Rounded.Close, "Stop getting the weather", "Nothing more is sent anywhere", danger = true) { onClear() }
        }
    }
}
