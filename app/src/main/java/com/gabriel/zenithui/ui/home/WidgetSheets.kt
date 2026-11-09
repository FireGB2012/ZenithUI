package com.gabriel.zenithui.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.Place
import com.gabriel.zenithui.data.WorldClocks
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.ZenithButton
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.onboarding.Segmented
import com.gabriel.zenithui.ui.settings.CardRows
import com.gabriel.zenithui.ui.settings.ControlRow
import com.gabriel.zenithui.ui.settings.FieldBox
import com.gabriel.zenithui.ui.settings.FieldLook
import com.gabriel.zenithui.ui.settings.GroupDivider
import com.gabriel.zenithui.ui.settings.Note
import com.gabriel.zenithui.ui.settings.ROW_INSET
import com.gabriel.zenithui.ui.settings.Section
import com.gabriel.zenithui.ui.settings.rowPress
import com.gabriel.zenithui.ui.theme.ButtonKind
import com.gabriel.zenithui.ui.theme.LocalApple
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.inkOnTint
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// The sheets that set up one of Zenith's widgets: the World clock's cities, the Countdown's day and
// the Weather's place. As every sheet's insides now are, nothing in them is glass: rows on cards, a
// calendar on a card, boxes to type in, and under a card, in grey, what it means.

/**
 * A box to type in, as an iPhone draws one ([FieldBox]): for a [search], Apple's grey with a
 * magnifying glass and a round button that clears it, the keyboard offering Search; otherwise a
 * card of its own, the keyboard starting each sentence with a capital and offering Done.
 */
@Composable
private fun SheetField(value: String, placeholder: String, onChange: (String) -> Unit, search: Boolean = true) {
    FieldBox(
        value = value,
        onValueChange = onChange,
        placeholder = placeholder,
        look = if (search) FieldLook.Search else FieldLook.Card,
        keyboardOptions = KeyboardOptions(
            capitalization = if (search) KeyboardCapitalization.None else KeyboardCapitalization.Sentences,
            imeAction = if (search) ImeAction.Search else ImeAction.Done,
        ),
    )
}

// ---------------------------------------------------------------------- world clock

/**
 * The World clock's cities: the ones chosen, in their order, as the rows of a card, each with a way
 * off; and under them a search over every city there is a time zone for, whose answers are the rows
 * of a card of their own. Four at most, as the widget shows at its largest.
 */
@Composable
fun WorldCitiesSheet(visible: Boolean, chosen: List<String>, onChange: (List<String>) -> Unit, onDismiss: () -> Unit) {
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
        // The cities chosen, on a card, and under it in grey what the card is.
        Section(
            footer = "Up to four cities, in the order the widget shows them.",
            modifier = Modifier.appearIn(delayMillis = staggerDelay(0), riseFrom = 16f),
        ) {
            // A city taken off takes its hairline with it.
            CardRows {
                chosen.forEachIndexed { index, zone ->
                    key(zone) {
                        ChosenCity(
                            zone = zone,
                            clock = clock,
                            modifier = Modifier.appearIn(delayMillis = staggerDelay(index + 1), riseFrom = 16f),
                        ) { onChange(chosen - zone) }
                    }
                }
            }
        }
        AnimatedVisibility(visible = !full, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
            Column {
                Spacer(Modifier.height(12.dp))
                SheetField(query, "Find a city", { query = it })
            }
        }
        if (!full) {
            // Every other city, A to Z, on a card that scrolls: a tap puts it at the end of the list.
            Section(modifier = Modifier.weight(1f, fill = false)) {
                LazyColumn {
                    itemsIndexed(found, key = { _, zone -> zone }) { index, zone ->
                        Column {
                            if (index > 0) GroupDivider(inset = ROW_INSET)
                            FoundCity(zone) {
                                onChange(chosen + zone)
                                query = ""
                            }
                        }
                    }
                }
            }
        }
        AnimatedVisibility(visible = full, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
            Note("That is four. Take one off to choose another.", Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp))
        }
    }
}

/**
 * One of the cities chosen, as a row of a card: its sun or moon, its name and how far it is from
 * here, its time now in grey, and an X that takes it off the widget.
 */
@Composable
private fun ChosenCity(zone: String, clock: DateTimeFormatter, modifier: Modifier = Modifier, onRemove: () -> Unit) {
    val apple = LocalApple.current
    val city = WorldClocks.city(zone)
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CitySymbol(zone)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(city, color = apple.words, fontSize = 16.sp)
            Text(WorldClocks.offset(zone, ZoneId.systemDefault(), Instant.now()), color = apple.secondary, fontSize = 14.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(runCatching { Instant.now().atZone(ZoneId.of(zone)).format(clock) }.getOrDefault(""), color = apple.secondary, fontSize = 16.sp)
        TakeOff("Take $city off", onRemove)
    }
}

/**
 * A city that could be added, as a row of a card: its name, its part of the world in grey, and how
 * far it is from here. Under a finger it shades, as an iPhone's rows do.
 */
@Composable
private fun FoundCity(zone: String, onAdd: () -> Unit) {
    val apple = LocalApple.current
    val touch = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .rowPress(touch)
            .clickable(interactionSource = touch, indication = null, role = Role.Button, onClick = onAdd)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(WorldClocks.city(zone), color = apple.words, fontSize = 16.sp)
            Text(WorldClocks.region(zone), color = apple.secondary, fontSize = 14.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(WorldClocks.offset(zone, ZoneId.systemDefault(), Instant.now()), color = apple.secondary, fontSize = 14.sp)
    }
}

/**
 * A city's sun or moon, by whether it is day there now, in a row's grey. It takes the room of a
 * row's square, so the words after it, and the hairline under them, line up with Settings' rows.
 */
@Composable
private fun CitySymbol(zone: String) {
    Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
        Icon(
            if (WorldClocks.daytime(zone, Instant.now())) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay,
            contentDescription = null,
            tint = LocalApple.current.secondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

/**
 * The X at the end of a row, in a row's grey, that takes off what [description] names. As an
 * iPhone's buttons do, it dims while it is held rather than bouncing; its room to be touched is
 * wider than the X.
 */
@Composable
private fun TakeOff(description: String, onClick: () -> Unit) {
    val touch = remember { MutableInteractionSource() }
    val pressed by touch.collectIsPressedAsState()
    Box(
        Modifier
            .size(44.dp)
            .clickable(interactionSource = touch, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Rounded.Close,
            contentDescription = description,
            tint = LocalApple.current.secondary,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer { alpha = if (pressed) 0.35f else 1f },
        )
    }
}

// ---------------------------------------------------------------------- countdown

/**
 * What the Countdown counts down to: a name for it, on a card of its own, and the day, chosen on
 * the month on a card under it. Only days from today on can be chosen; set, the widget's ring
 * starts filling from today. The month is in Apple's colours: words and grey, today's number in the
 * accent, and the day chosen filled with the accent.
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
    val apple = LocalApple.current
    val accent = LocalPalette.current.accent
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
            // The room a group of rows has above it, so the name's card stands as the calendar's does.
            Spacer(Modifier.height(12.dp))
            SheetField(name, "What it is to: a holiday, a birthday", { name = it.take(40) }, search = false)
            Section {
                DatePicker(
                    state = state,
                    title = null,
                    headline = null,
                    showModeToggle = false,
                    // The card's own colour under it, and under the years, which cover the month
                    // while they are open. Days that cannot be chosen are Apple's faintest grey;
                    // today is marked as an iPhone marks it, its number in the accent with no ring.
                    colors = DatePickerDefaults.colors(
                        containerColor = apple.card,
                        titleContentColor = apple.words,
                        headlineContentColor = apple.words,
                        weekdayContentColor = apple.secondary,
                        subheadContentColor = apple.secondary,
                        navigationContentColor = apple.words,
                        yearContentColor = apple.words,
                        disabledYearContentColor = apple.faint,
                        currentYearContentColor = accent,
                        selectedYearContentColor = inkOnTint(accent),
                        selectedYearContainerColor = accent,
                        dayContentColor = apple.words,
                        disabledDayContentColor = apple.faint,
                        selectedDayContentColor = inkOnTint(accent),
                        selectedDayContainerColor = accent,
                        todayContentColor = accent,
                        todayDateBorderColor = Color.Transparent,
                        dividerColor = apple.separator,
                    ),
                )
            }
        }
        Row(Modifier.padding(top = 12.dp)) {
            if (day > 0) {
                ZenithButton(text = "Clear", onClick = onClear, modifier = Modifier.weight(1f), kind = ButtonKind.Grey)
                Spacer(Modifier.width(10.dp))
            }
            ZenithButton(
                text = "Count down",
                kind = ButtonKind.Main,
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
 * is, in grey under the choice of units: the place typed into the search goes to Open-Meteo's place
 * search, and the one chosen to its forecast; nothing else, and nothing at all until something is
 * typed. The places found are the rows of a card; stopping the weather is on a card of its own,
 * after the rest.
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
        Section(
            footer = "The place you search for goes to Open-Meteo, a free weather service, and so does the one you choose, " +
                "to get its forecast. Nothing else is sent, and nothing at all until you search.",
            modifier = Modifier.appearIn(delayMillis = staggerDelay(0), riseFrom = 16f),
        ) {
            ControlRow {
                Segmented(
                    options = listOf("c" to "Celsius", "f" to "Fahrenheit"),
                    selected = units,
                    onSelect = onUnits,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        SheetField(query, if (place.isBlank()) "Find your town or city" else "Now: $place. Find another", { query = it })
        val list = results
        when {
            list == null -> Unit
            list.isEmpty() -> Note("No place called “${query.trim()}” was found.", Modifier.padding(start = 12.dp, end = 12.dp, top = 12.dp))
            // On a card that scrolls, if there are more than there is room for.
            else -> Section(modifier = Modifier.weight(1f, fill = false)) {
                LazyColumn {
                    itemsIndexed(list, key = { _, found -> "${found.lat},${found.lon}" }) { index, found ->
                        Column(Modifier.appearIn(fromScale = 1f, riseFrom = 12f, gentle = true)) {
                            if (index > 0) GroupDivider()
                            FoundPlace(found) { onPick(found, units) }
                        }
                    }
                }
            }
        }
        if (place.isNotBlank()) {
            // The way to stop, in red, on a card of its own after the rest, as an iPhone sets it apart.
            Section(header = "Or") {
                MenuRow(Icons.Rounded.Close, "Stop getting the weather", "Nothing more is sent anywhere", danger = true) { onClear() }
            }
        }
    }
}

/**
 * A place the search found, as a row of a card: a pin, its name, and in grey where it is. Under a
 * finger it shades, as an iPhone's rows do.
 */
@Composable
private fun FoundPlace(found: Place, onPick: () -> Unit) {
    val apple = LocalApple.current
    val touch = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .rowPress(touch)
            .clickable(interactionSource = touch, indication = null, role = Role.Button, onClick = onPick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // In the room of a row's square, so the words, and the hairline under them, line up.
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.LocationOn, contentDescription = null, tint = apple.secondary, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(found.name, color = apple.words, fontSize = 16.sp)
            if (found.region.isNotBlank()) {
                Text(found.region, color = apple.secondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
