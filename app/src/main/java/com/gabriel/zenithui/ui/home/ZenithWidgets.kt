package com.gabriel.zenithui.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.camera2.CameraManager
import android.os.BatteryManager
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.Battery2Bar
import androidx.compose.material.icons.rounded.Battery3Bar
import androidx.compose.material.icons.rounded.Battery4Bar
import androidx.compose.material.icons.rounded.Battery5Bar
import androidx.compose.material.icons.rounded.Battery6Bar
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BatteryFull
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.ToggleGrid
import com.gabriel.zenithui.data.ToggleNames
import com.gabriel.zenithui.data.WidgetForm
import com.gabriel.zenithui.data.Widgets
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.common.shake
import com.gabriel.zenithui.ui.settings.Toggle as OnOffSwitch
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.GlassMotion
import com.gabriel.zenithui.ui.theme.glassPress
import com.gabriel.zenithui.ui.theme.LocalNumberFont
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.CardKind
import com.gabriel.zenithui.ui.theme.inkOnTint
import com.gabriel.zenithui.ui.theme.solidCard
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** The shape a widget has been given: see [WidgetForm], where the lines between them are drawn. */
private fun formOf(width: Dp, height: Dp): WidgetForm = WidgetForm.of(width.value, height.value)

/**
 * What a widget's tile is filled with when it is not Apple's card: a colour of the widget's own
 * (see `InOwnColour`), or what a widget paints for itself, the weather's sky or the album's colour.
 * Unspecified for the card. Words on a filled tile are a shade stronger ([quiet]).
 */
internal val LocalWidgetFill = staticCompositionLocalOf { Color.Unspecified }

/**
 * Renders one of Zenith's built-in widgets into whatever space the grid gave it: a solid tile, as an
 * iPhone's widgets are, in Apple's card colour or the colour it was given ([LocalWidgetFill]).
 */
@Composable
fun ZenithWidget(
    type: String,
    userName: String,
    note: String,
    onNoteChange: (String) -> Unit,
    onSearch: () -> Unit,
    onAmbient: () -> Unit = {},
    /** Which controls the Quick Toggles widget shows. */
    toggles: List<String> = listOf("torch", "wifi", "bluetooth", "focus"),
    modifier: Modifier = Modifier,
) {
    val kind = Widgets.solid(type)
    if (kind == "search") {
        // A one line bar: it draws, and reacts, at exactly its own size.
        Box(modifier, contentAlignment = Alignment.Center) {
            SearchWidget(onSearch)
        }
        return
    }

    BoxWithConstraints(modifier.solidCard(CardKind.Widget, LocalWidgetFill.current)) {
        WidgetBody(kind, formOf(maxWidth, maxHeight), maxWidth, maxHeight, userName, note, onNoteChange, toggles)
    }
}

/** What a widget shows on its tile. */
@Composable
private fun WidgetBody(
    kind: String,
    form: WidgetForm,
    width: Dp,
    height: Dp,
    userName: String,
    note: String,
    onNoteChange: (String) -> Unit,
    toggles: List<String>,
) {
    when (kind) {
        "clock" -> ClockWidget(form, userName, width, height)
        "battery" -> BatteryWidget(form, width, height)
        "calendar" -> CalendarWidget(form, width, height)
        "notes" -> NotesWidget(form, note, onNoteChange)
        "toggles" -> TogglesWidget(form, toggles, width, height)
        "media" -> MediaWidget(form, width, height)
        "world" -> WorldClockWidget(form, width, height)
        "countdown" -> CountdownWidget(form, width, height)
        "photo" -> PhotoFrameWidget(form, width, height)
        "todo" -> TodoWidget(form, width, height)
        "aura" -> AuraWidget(form, width, height)
        "weather" -> WeatherWidget(form, width, height)
        else -> Box(Modifier.fillMaxSize())
    }
}

// ---------------------------------------------------------------------- shared pieces

/** A text size that fits the smaller of two limits. */
@Composable
internal fun fit(first: Dp, second: Dp = Dp.Infinity): TextUnit =
    with(LocalDensity.current) { minOf(first, second).coerceAtLeast(8.dp).toSp() }

/**
 * Big numbers set with no font padding, so they sit exactly where they are placed, in the font
 * numbers are set in. A number font with a weight of its own (Thin) sets every number in it,
 * unless [keepWeight]: glass letters need the body they ask for.
 */
@Composable
internal fun tight(size: TextUnit, weight: FontWeight = FontWeight.Light, keepWeight: Boolean = false): TextStyle {
    val numbers = LocalNumberFont.current
    return TextStyle(
        fontFamily = numbers.family,
        fontSize = size,
        lineHeight = size,
        fontWeight = if (keepWeight) weight else numbers.weight ?: weight,
        letterSpacing = (-0.02 * size.value).sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
}

/**
 * The colour of a widget's second line: the date under the time, "On battery", "25 days left".
 * Apple's quieter words on its card; a shade stronger on a tile of a colour, where they must stand
 * on whatever colour it is.
 */
@Composable
internal fun quiet(): Color =
    LocalPalette.current.onGlass.copy(alpha = if (LocalWidgetFill.current.isSpecified) 0.85f else 0.6f)

/**
 * The accent as ink on a tile. Apple's card is solid and the accent is already one that can be read
 * on it (`AppleColours.accent`); on a tile of a colour the accent is that tile's own ink.
 */
@Composable
internal fun accentInk(): Color = LocalPalette.current.accent

/** The small upper-case accent label that heads most widgets. */
@Composable
internal fun Caps(text: String, size: TextUnit = 11.sp, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = accentInk(),
        fontSize = size,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.4.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}

/**
 * A widget's headline number: in the accent, or in the words' own colour. Solid, as Apple sets
 * numbers on a widget; no gradient.
 */
@Composable
internal fun Figure(
    text: String,
    size: TextUnit,
    weight: FontWeight = FontWeight.Light,
    accent: Boolean = true,
    animated: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val style = tight(size, weight).copy(color = if (accent) palette.accent else palette.onGlass)
    if (animated) {
        AnimatedDigits(text, style, modifier)
    } else {
        Text(text, style = style, maxLines = 1, modifier = modifier)
    }
}

/** A small symbol in a widget, in the accent unless it is given a colour. */
@Composable
internal fun Symbol(icon: ImageVector, size: Dp, modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
    Icon(
        icon,
        contentDescription = null,
        tint = if (tint.isSpecified) tint else LocalPalette.current.accent,
        modifier = modifier.size(size),
    )
}

@Composable
internal fun rememberNow(): LocalDateTime {
    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            val millisToNextSecond = 1_000L - (System.currentTimeMillis() % 1_000L)
            delay(millisToNextSecond)
        }
    }
    return now
}

// ---------------------------------------------------------------------- clock

@Composable
private fun ClockWidget(form: WidgetForm, userName: String, width: Dp, height: Dp) {
    val palette = LocalPalette.current
    val now = rememberNow()
    val hours = remember(now.hour) { now.format(DateTimeFormatter.ofPattern("HH")) }
    val minutes = remember(now.minute) { now.format(DateTimeFormatter.ofPattern("mm")) }
    val time = "$hours:$minutes"
    val weekday = remember(now.dayOfYear) { now.format(DateTimeFormatter.ofPattern("EEEE")) }
    val shortDay = remember(now.dayOfYear) { now.format(DateTimeFormatter.ofPattern("EEE")) }
    val date = remember(now.dayOfYear) { now.format(DateTimeFormatter.ofPattern("d MMMM")) }
    val greeting = when (now.hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        in 18..22 -> "Good evening"
        else -> "Still up"
    }
    val hello = if (userName.isBlank()) greeting else "$greeting, $userName"

    when (form) {
        // Just the time, as large as it will go.
        WidgetForm.Mini -> Box(Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) {
            Figure(time, fit(width * 0.22f, height * 0.34f), animated = true)
        }

        // A line: time, then the day beside it.
        WidgetForm.Strip -> Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Figure(time, fit(height * 0.4f, width * 0.13f), animated = true)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Caps(shortDay)
                Text(date, color = quiet(), fontSize = 13.sp, maxLines = 1)
            }
        }

        // Hours stacked over minutes, like a lock screen.
        WidgetForm.Square -> Column(
            Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Caps(weekday)
            // As big as they will go, less what the day above them and the date below need, so a
            // short widget keeps all three.
            val size = fit(width * 0.27f, minOf(height * 0.19f, (height - 80.dp) / 2))
            Column {
                Figure(hours, size, FontWeight.Medium, accent = false, animated = true)
                Figure(minutes, size, FontWeight.Light, animated = true)
            }
            Text(date, color = quiet(), fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }

        // The time big on the left, the day and a greeting on the right.
        WidgetForm.Wide -> Row(
            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Figure(time, fit(height * 0.32f, width * 0.13f), animated = true)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Caps(weekday, 12.sp)
                Text(date, color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                Text(hello, color = quiet(), fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }

        // An analogue face, with the digital time underneath.
        WidgetForm.Tall -> Column(
            Modifier.fillMaxSize().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Caps(weekday)
            AnalogFace(now, Modifier.fillMaxWidth(0.78f).aspectRatio(1f))
            Figure(time, fit(width * 0.15f, 24.dp), FontWeight.Medium, accent = false, animated = true)
            Text(date, color = quiet(), fontSize = 12.sp, maxLines = 1)
        }

        // A large face, the full date above it and a greeting below.
        WidgetForm.Big -> Column(
            Modifier.fillMaxSize().padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Caps(weekday, 12.sp)
                    Text(date, color = palette.onGlass, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                }
                Figure(time, 22.sp, FontWeight.Medium, animated = true)
            }
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                AnalogFace(now, Modifier.fillMaxHeight(0.8f).aspectRatio(1f, matchHeightConstraintsFirst = true))
            }
            Text(hello, color = quiet(), fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun AnimatedDigits(text: String, style: TextStyle, modifier: Modifier = Modifier) {
    val reduced = LocalMotionLevel.current.reduced
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            // With movement reduced the new figure fades in where the old one was.
            if (reduced) fadeIn(tween(150)).togetherWith(fadeOut(tween(150))) else (slideInVertically(spring(dampingRatio = 0.6f, stiffness = 180f)) { it / 3 } + fadeIn(tween(420)))
                .togetherWith(slideOutVertically(tween(560)) { -it / 3 } + fadeOut(tween(320)))
        },
        modifier = modifier,
        label = "digits",
    ) { value ->
        Text(value, style = style, maxLines = 1)
    }
}

@Composable
internal fun AnalogFace(now: LocalDateTime, modifier: Modifier) {
    val palette = LocalPalette.current
    Canvas(modifier) {
        val radius = size.minDimension / 2f
        val middle = center
        drawCircle(palette.onGlass.copy(alpha = 0.06f), radius, middle)
        drawCircle(palette.onGlass.copy(alpha = 0.16f), radius, middle, style = Stroke(1.dp.toPx()))
        for (tick in 0 until 60) {
            val major = tick % 5 == 0
            val angle = Math.toRadians(tick * 6.0 - 90.0)
            val outer = radius * 0.9f
            val inner = if (major) radius * 0.76f else radius * 0.84f
            drawLine(
                color = palette.onGlass.copy(alpha = if (major) 0.7f else 0.2f),
                start = Offset(middle.x + (cos(angle) * inner).toFloat(), middle.y + (sin(angle) * inner).toFloat()),
                end = Offset(middle.x + (cos(angle) * outer).toFloat(), middle.y + (sin(angle) * outer).toFloat()),
                strokeWidth = (if (major) 2.dp else 1.dp).toPx(),
                cap = StrokeCap.Round,
            )
        }
        val minute = now.minute + now.second / 60f
        val hour = (now.hour % 12) + minute / 60f
        val hands = listOf(
            Triple(hour * 30f, radius * 0.5f, Pair(4.dp, palette.onGlass)),
            Triple(minute * 6f, radius * 0.72f, Pair(3.dp, palette.accent)),
            Triple(now.second * 6f, radius * 0.8f, Pair(1.dp, palette.accentAlt)),
        )
        for ((degrees, length, look) in hands) {
            val angle = Math.toRadians(degrees - 90.0)
            drawLine(
                color = look.second,
                start = middle,
                end = Offset(middle.x + (cos(angle) * length).toFloat(), middle.y + (sin(angle) * length).toFloat()),
                strokeWidth = look.first.toPx(),
                cap = StrokeCap.Round,
            )
        }
        drawCircle(palette.accent, 3.5.dp.toPx(), middle)
    }
}

// ---------------------------------------------------------------------- battery

@Immutable
private data class BatteryState(val level: Int, val charging: Boolean) {
    val fraction: Float get() = level.coerceIn(0, 100) / 100f
    val percent: String get() = if (level < 0) "--" else "$level%"
    val status: String
        get() = when {
            charging -> "Charging"
            level in 0..20 -> "Low battery"
            else -> "On battery"
        }
}

@Composable
private fun rememberBattery(): BatteryState {
    val context = LocalContext.current
    var level by remember { mutableIntStateOf(-1) }
    var charging by remember { mutableStateOf(false) }
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                intent ?: return
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
                level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) * 100 / scale
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                    status == BatteryManager.BATTERY_STATUS_FULL
            }
        }
        val sticky = context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        sticky?.let { receiver.onReceive(context, it) }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return BatteryState(level, charging)
}

@Composable
private fun BatteryWidget(form: WidgetForm, width: Dp, height: Dp) {
    val palette = LocalPalette.current
    val battery = rememberBattery()
    // The bolt lights up while charging.
    val symbol = Icons.Rounded.Bolt
    val symbolTint = if (battery.charging) palette.accent else palette.onGlassMuted

    when (form) {
        // A thin ring with the number inside.
        WidgetForm.Mini -> Box(Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.Center) {
            Ring(battery.fraction, 4.dp, Modifier.fillMaxSize())
            Figure("${battery.level.coerceAtLeast(0)}", fit(width * 0.21f, height * 0.21f), FontWeight.Medium, accent = false, animated = true)
        }

        // A charge bar with the percentage at the end.
        WidgetForm.Strip -> Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Symbol(symbol, 18.dp, tint = symbolTint)
            Spacer(Modifier.width(10.dp))
            Capsule(battery.fraction, Modifier.weight(1f).height(9.dp))
            Spacer(Modifier.width(12.dp))
            Text(battery.percent, color = palette.onGlass, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }

        // The ring.
        WidgetForm.Square -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Ring(battery.fraction, 7.dp, Modifier.fillMaxSize())
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Figure(battery.percent, fit(width * 0.15f, 24.dp), FontWeight.Medium, accent = false, animated = true)
                Spacer(Modifier.height(4.dp))
                Text(battery.status, color = quiet(), fontSize = 12.sp)
            }
        }

        // A ring on the left; the number, the state and a segmented gauge on the right.
        WidgetForm.Wide -> Row(
            Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.fillMaxHeight(0.8f).aspectRatio(1f), contentAlignment = Alignment.Center) {
                Ring(battery.fraction, 6.dp, Modifier.fillMaxSize())
                Symbol(symbol, 22.dp, tint = symbolTint)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Caps("Battery")
                Figure(battery.percent, fit(height * 0.2f, 28.dp), animated = true)
                Spacer(Modifier.height(6.dp))
                Text(battery.status, color = quiet(), fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Segments(battery.fraction, Modifier.fillMaxWidth())
            }
        }

        // A tall capsule that fills from the bottom like liquid.
        WidgetForm.Tall -> Column(
            Modifier.fillMaxSize().padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Caps("Battery")
            Spacer(Modifier.height(10.dp))
            // It fills from the bottom as it appears, the way the ring runs round.
            val level = rememberFilled(battery.fraction)
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(0.42f)
                    .clip(RoundedCornerShape(percent = 30))
                    .background(palette.onGlass.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .heightShare { level.value.coerceAtLeast(0.04f) }
                        .background(palette.accent),
                )
                Text(battery.percent, style = tight(fit(width * 0.11f, 20.dp), FontWeight.SemiBold).copy(color = palette.onGlass))
            }
            Spacer(Modifier.height(10.dp))
            Text(battery.status, color = quiet(), fontSize = 13.sp)
        }

        // A large ring with the number in the middle and a gauge below.
        WidgetForm.Big -> Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Caps("Battery", 12.sp, Modifier.weight(1f))
                Text(battery.status, color = quiet(), fontSize = 13.sp)
            }
            Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                Ring(battery.fraction, 10.dp, Modifier.fillMaxSize(0.8f))
                Figure(battery.percent, fit(width * 0.1f, height * 0.085f), animated = true)
            }
            Segments(battery.fraction, Modifier.fillMaxWidth())
        }
    }
}

/** The charge as an arc of the accent over a faint track, as an iPhone's battery widget draws it. */
@Composable
internal fun Ring(fraction: Float, stroke: Dp, modifier: Modifier) {
    val palette = LocalPalette.current
    val filled = rememberFilled(fraction)
    fun DrawScope.arc(sweep: Float, colour: Color) {
        val width = stroke.toPx()
        val side = size.minDimension - width
        drawArc(
            color = colour,
            startAngle = 130f, sweepAngle = sweep, useCenter = false,
            topLeft = Offset((size.width - side) / 2f, (size.height - side) / 2f), size = Size(side, side),
            style = Stroke(width = width, cap = StrokeCap.Round),
        )
    }
    Canvas(modifier) {
        arc(280f, palette.onGlass.copy(alpha = 0.12f))
        arc((280f * filled.value).coerceIn(0f, 280f), palette.accent)
    }
}

/**
 * How full a gauge is drawn on its way to [fraction]. It springs there: up from empty when it
 * first appears, and from wherever it was whenever the charge changes. Every shape the charge is
 * shown as runs on this, so they all fill the same way. With movement reduced it is simply there.
 */
@Composable
internal fun rememberFilled(fraction: Float): Animatable<Float, AnimationVector1D> {
    val reduced = LocalMotionLevel.current.reduced
    val filled = remember { Animatable(if (reduced) fraction else 0f) }
    LaunchedEffect(fraction, reduced) {
        if (reduced) filled.snapTo(fraction) else filled.animateTo(fraction, spring(dampingRatio = 0.62f, stiffness = 90f))
    }
    return filled
}

/** As wide as [share] of the room it is given. Read while it is measured, so filling up composes nothing again. */
private fun Modifier.widthShare(share: () -> Float): Modifier = layout { measurable, constraints ->
    val width = (constraints.maxWidth * share().coerceIn(0f, 1f)).roundToInt()
    val placeable = measurable.measure(constraints.copy(minWidth = width, maxWidth = width))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

/** The same, upwards. */
private fun Modifier.heightShare(share: () -> Float): Modifier = layout { measurable, constraints ->
    val height = (constraints.maxHeight * share().coerceIn(0f, 1f)).roundToInt()
    val placeable = measurable.measure(constraints.copy(minHeight = height, maxHeight = height))
    layout(placeable.width, placeable.height) { placeable.place(0, 0) }
}

@Composable
private fun Capsule(fraction: Float, modifier: Modifier) {
    val palette = LocalPalette.current
    val filled = rememberFilled(fraction)
    Box(modifier.clip(RoundedCornerShape(percent = 50)).background(palette.onGlass.copy(alpha = 0.12f))) {
        Box(
            Modifier
                .fillMaxHeight()
                .widthShare { filled.value.coerceAtLeast(0.03f) }
                .clip(RoundedCornerShape(percent = 50))
                .background(palette.accent),
        )
    }
}

@Composable
private fun Segments(fraction: Float, modifier: Modifier) {
    val palette = LocalPalette.current
    // They light one after another as the gauge runs up, never one more than the charge is worth.
    val filled = rememberFilled(fraction)
    val lit by remember(fraction) { derivedStateOf { (minOf(filled.value, fraction) * 10f + 0.001f).toInt() } }
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(10) { index ->
            val colour by animateColorAsState(
                targetValue = if (index < lit) palette.accent else palette.onGlass.copy(alpha = 0.12f),
                animationSpec = tween(200),
                label = "segment",
            )
            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(percent = 50)).background(colour))
        }
    }
}

// ---------------------------------------------------------------------- calendar

@Composable
private fun CalendarWidget(form: WidgetForm, width: Dp, height: Dp) {
    val palette = LocalPalette.current
    val today = remember { LocalDate.now() }
    val daysInMonth = remember(today) { today.lengthOfMonth() }
    val remaining = ChronoUnit.DAYS.between(today, today.withDayOfMonth(daysInMonth))
    val weekday = today.format(DateTimeFormatter.ofPattern("EEEE"))
    val month = today.format(DateTimeFormatter.ofPattern("MMMM"))
    val left = if (remaining <= 0) "Last day of the month" else "$remaining days left"
    val day = "${today.dayOfMonth}"

    when (form) {
        // The day of the week over today's date.
        WidgetForm.Mini -> Column(
            Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Caps(today.format(DateTimeFormatter.ofPattern("EEE")), 9.sp)
            Spacer(Modifier.height(2.dp))
            Figure(day, fit(width * 0.36f, height * 0.34f), FontWeight.Medium, accent = false)
        }

        // The date large, the rest of it in a line beside.
        WidgetForm.Strip -> Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Figure(day, fit(height * 0.42f, 30.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Caps(weekday)
                Text(month, color = quiet(), fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }

        // Weekday, a big date and the month, like a tear-off calendar.
        WidgetForm.Square -> Column(
            Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Caps(weekday)
            Figure(day, fit(width * 0.32f, height * 0.28f))
            Column {
                Text(month, color = palette.onGlass, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(left, color = quiet(), fontSize = 12.sp)
            }
        }

        // Today on the left, the month on the right.
        WidgetForm.Wide -> Row(
            Modifier.fillMaxSize().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(0.8f)) {
                Caps(weekday, 12.sp)
                Figure(day, fit(height * 0.26f, 38.dp))
                Spacer(Modifier.height(6.dp))
                Text(month, color = palette.onGlass, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(left, color = quiet(), fontSize = 12.sp)
            }
            Spacer(Modifier.width(10.dp))
            MonthGrid(today, 10.5.sp, 18.dp, Modifier.weight(1.2f).fillMaxHeight())
        }

        // The month, headed by today.
        WidgetForm.Tall -> Column(Modifier.fillMaxSize().padding(12.dp)) {
            Caps(month, 12.sp, Modifier.padding(start = 4.dp))
            Figure(day, fit(width * 0.22f, 32.dp), modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp))
            MonthGrid(today, 11.sp, 20.dp, Modifier.weight(1f).fillMaxWidth())
            Text(left, color = quiet(), fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
        }

        // A full month with room to breathe.
        WidgetForm.Big -> Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Caps(weekday, 12.sp)
                    Text("${today.dayOfMonth} $month", color = palette.onGlass, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                }
                Text(left, color = quiet(), fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            MonthGrid(today, 13.sp, 26.dp, Modifier.weight(1f).fillMaxWidth())
        }
    }
}

@Composable
private fun MonthGrid(today: LocalDate, fontSize: TextUnit, dot: Dp, modifier: Modifier) {
    val palette = LocalPalette.current
    val firstOfMonth = today.withDayOfMonth(1)
    val leading = (firstOfMonth.dayOfWeek.value - 1).coerceAtLeast(0)
    val daysInMonth = today.lengthOfMonth()
    val weeks = (leading + daysInMonth + 6) / 7

    // Numbers are drawn with no font padding and their line trimmed to the glyphs, so a digit
    // sits in the exact middle of its cell — and of the dot behind today.
    val dayStyle = TextStyle(
        fontFamily = LocalZenithFont.current,
        fontSize = fontSize,
        lineHeight = fontSize,
        textAlign = TextAlign.Center,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )

    // Today's dot grows into its place with a ring drawn round it, once, as the calendar appears.
    val reduced = LocalMotionLevel.current.reduced
    val drawnIn = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (!reduced) drawnIn.animateTo(1f, tween(760, easing = FastOutSlowInEasing))
    }

    Column(modifier) {
        Row(Modifier.fillMaxWidth()) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { letter ->
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(letter, style = dayStyle.copy(color = palette.onGlassMuted, fontWeight = FontWeight.SemiBold))
                }
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceEvenly) {
            repeat(weeks) { week ->
                Row(Modifier.fillMaxWidth()) {
                    repeat(7) { dow ->
                        val dayNumber = week * 7 + dow - leading + 1
                        val isToday = dayNumber == today.dayOfMonth
                        Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                            if (dayNumber in 1..daysInMonth) {
                                Box(
                                    Modifier
                                        .size(dot)
                                        .then(
                                            if (isToday) {
                                                Modifier
                                                    .drawBehind {
                                                        val drawn = drawnIn.value
                                                        if (drawn < 1f) {
                                                            val out = 3.dp.toPx()
                                                            drawArc(
                                                                color = palette.accent,
                                                                startAngle = -90f,
                                                                sweepAngle = 360f * drawn,
                                                                useCenter = false,
                                                                topLeft = Offset(-out, -out),
                                                                size = Size(size.width + 2f * out, size.height + 2f * out),
                                                                alpha = 1f - drawn * drawn * drawn,
                                                                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round),
                                                            )
                                                        }
                                                    }
                                                    .graphicsLayer {
                                                        val grown = 0.55f + 0.45f * drawnIn.value
                                                        scaleX = grown
                                                        scaleY = grown
                                                    }
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .then(
                                            if (isToday) {
                                                Modifier
                                                    .clip(RoundedCornerShape(percent = 50))
                                                    .background(palette.accent)
                                            } else {
                                                Modifier
                                            }
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = dayNumber.toString(),
                                        style = dayStyle.copy(
                                            color = when {
                                                isToday -> inkOnTint(palette.accent)
                                                dow >= 5 -> palette.onGlass.copy(alpha = 0.62f)
                                                else -> palette.onGlass.copy(alpha = 0.92f)
                                            },
                                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------- notes

/**
 * Where on screen the home-screen text field you are typing in sits, or null when you are not
 * typing. The home screen reads it to end typing when a touch lands anywhere else. There is only
 * ever one home screen, so a single shared instance is enough.
 */
class TypingArea {
    var bounds by mutableStateOf<Rect?>(null)
}

val HomeTypingArea = TypingArea()

@Composable
private fun NotesWidget(form: WidgetForm, note: String, onNoteChange: (String) -> Unit) {
    val palette = LocalPalette.current
    val today = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault())) }

    /** The heading most forms share. */
    @Composable
    fun Heading(modifier: Modifier = Modifier) {
        Caps("Note", modifier = modifier)
    }

    when (form) {
        // An icon and a couple of lines.
        WidgetForm.Mini -> Column(Modifier.fillMaxSize().padding(8.dp)) {
            Symbol(Icons.Rounded.EditNote, 16.dp)
            NoteField(note, onNoteChange, 11.sp, 14.sp, singleLine = false, ruled = false, placeholder = "Note", modifier = Modifier.weight(1f))
        }

        // A single line to jot on.
        WidgetForm.Strip -> Row(
            Modifier.fillMaxSize().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Symbol(Icons.Rounded.EditNote, 20.dp)
            Spacer(Modifier.width(10.dp))
            NoteField(note, onNoteChange, 14.sp, 18.sp, singleLine = true, ruled = false, placeholder = "Jot something down", modifier = Modifier.weight(1f))
        }

        // A little card.
        WidgetForm.Square -> Column(Modifier.fillMaxSize().padding(14.dp)) {
            Heading()
            Spacer(Modifier.height(6.dp))
            NoteField(note, onNoteChange, 14.sp, 19.sp, singleLine = false, ruled = false, placeholder = "Tap to jot something down", modifier = Modifier.weight(1f))
        }

        // A coloured margin down the side, like a sticky note.
        WidgetForm.Wide -> Row(Modifier.fillMaxSize().padding(14.dp)) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(palette.accent),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Heading()
                Spacer(Modifier.height(6.dp))
                NoteField(note, onNoteChange, 14.sp, 20.sp, singleLine = false, ruled = false, placeholder = "Tap to jot something down", modifier = Modifier.weight(1f))
            }
        }

        // Ruled paper.
        WidgetForm.Tall -> Column(Modifier.fillMaxSize().padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Heading(Modifier.weight(1f))
                Text(today, color = quiet(), fontSize = 12.sp)
            }
            Spacer(Modifier.height(8.dp))
            NoteField(note, onNoteChange, 14.sp, 22.sp, singleLine = false, ruled = true, placeholder = "Tap to write", modifier = Modifier.weight(1f))
        }

        // A notepad page with a heading.
        WidgetForm.Big -> Column(Modifier.fillMaxSize().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Notes", color = palette.onGlass, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text(today, color = quiet(), fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            NoteField(note, onNoteChange, 15.sp, 24.sp, singleLine = false, ruled = true, placeholder = "Tap to write", modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun NoteField(
    note: String,
    onNoteChange: (String) -> Unit,
    fontSize: TextUnit,
    lineHeight: TextUnit,
    singleLine: Boolean,
    ruled: Boolean,
    placeholder: String,
    modifier: Modifier,
) {
    val palette = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    var fieldBounds by remember { mutableStateOf(Rect.Zero) }
    // A note scrolled off with its page, or removed, must not leave a stale area behind.
    DisposableEffect(Unit) { onDispose { if (focused) HomeTypingArea.bounds = null } }
    val ruleColour = palette.onGlass.copy(alpha = 0.08f)

    BasicTextField(
        value = note,
        onValueChange = { onNoteChange(if (singleLine) it.replace("\n", " ") else it) },
        singleLine = singleLine,
        textStyle = TextStyle(fontFamily = LocalZenithFont.current, color = palette.onGlass, fontSize = fontSize, lineHeight = lineHeight),
        cursorBrush = SolidColor(palette.accent),
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (ruled) {
                    Modifier.drawBehind {
                        val step = lineHeight.toPx()
                        var y = step
                        while (y < size.height) {
                            drawLine(ruleColour, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                            y += step
                        }
                    }
                } else {
                    Modifier
                }
            )
            .onGloballyPositioned { coords ->
                fieldBounds = coords.boundsInRoot()
                if (focused) HomeTypingArea.bounds = fieldBounds
            }
            .onFocusChanged { state ->
                focused = state.isFocused
                HomeTypingArea.bounds = if (state.isFocused) fieldBounds else null
            },
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth()) {
                if (note.isEmpty()) {
                    Text(
                        placeholder,
                        color = palette.onGlass.copy(alpha = 0.5f),
                        fontSize = fontSize,
                        maxLines = if (singleLine) 1 else 3,
                        softWrap = !singleLine,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                inner()
            }
        },
    )
}

// ---------------------------------------------------------------------- toggles

/** The largest a named tile gets, however much room the widget has. */
private val MAX_TILE_WIDTH = 168.dp
private val MAX_TILE_HEIGHT = 118.dp

/**
 * How much of a round toggle its symbol fills. Testers found the symbols hard to tell apart at
 * a glance at 46%; 54% is about what Android's own quick settings give theirs.
 */
private const val TOGGLE_SYMBOL = 0.54f

/** The name under a round toggle: the size of the label under an app, and a little heavier. */
private val TOGGLE_NAME = 12.sp
private val TOGGLE_NAME_LINE = 15.sp
private val TOGGLE_NAME_STYLE = TextStyle(
    fontSize = TOGGLE_NAME,
    lineHeight = TOGGLE_NAME_LINE,
    fontWeight = FontWeight.Medium,
    textAlign = TextAlign.Center,
)

/** Between a round toggle and its name. */
private val TOGGLE_NAME_GAP = 5.dp

/** The smallest a round toggle may get to make room for a second line of name under it. */
private val TOGGLE_NAMED_BUTTON = 38.dp

/** One control the Quick Toggles widget can show. */
@Immutable
private data class ToggleOption(val id: String, val label: String, val detail: String, val icon: ImageVector)

/**
 * Everything the Quick Toggles widget can hold. A launcher is not allowed to flip most of these by
 * itself, so apart from the torch each one opens the phone's own switch for it — the quick panel
 * where Android has one, otherwise the settings page.
 */
private val TOGGLE_OPTIONS = listOf(
    ToggleOption("torch", "Torch", "Turns the flashlight on and off", Icons.Rounded.FlashlightOn),
    ToggleOption("wifi", "Wi-Fi", "Opens the Wi-Fi panel", Icons.Rounded.Wifi),
    ToggleOption("bluetooth", "Bluetooth", "Opens Bluetooth settings", Icons.Rounded.Bluetooth),
    ToggleOption("data", "Mobile data", "Opens the internet panel", Icons.Rounded.SignalCellularAlt),
    ToggleOption("airplane", "Airplane mode", "Opens airplane mode settings", Icons.Rounded.AirplanemodeActive),
    ToggleOption("focus", "Do Not Disturb", "Opens Do Not Disturb", Icons.Rounded.DoNotDisturbOn),
    ToggleOption("location", "Location", "Opens location settings", Icons.Rounded.LocationOn),
    ToggleOption("hotspot", "Hotspot", "Opens hotspot settings", Icons.Rounded.WifiTethering),
    ToggleOption("saver", "Battery saver", "Opens battery saver settings", Icons.Rounded.BatterySaver),
    ToggleOption("volume", "Volume", "Opens the volume panel", Icons.Rounded.GraphicEq),
    ToggleOption("nfc", "NFC", "Opens NFC settings", Icons.Rounded.Nfc),
    ToggleOption("display", "Display", "Opens display settings", Icons.Rounded.BrightnessMedium),
)

@Immutable
private data class Toggle(val option: ToggleOption, val active: Boolean, val onClick: () -> Unit) {
    val label: String get() = option.label
    val icon: ImageVector get() = option.icon
}

@Composable
private fun TogglesWidget(form: WidgetForm, chosen: List<String>, width: Dp, height: Dp) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    LaunchedEffect(Unit) { com.gabriel.zenithui.data.Torch.watch(context) }
    val torchOn by com.gabriel.zenithui.data.Torch.on.collectAsState()

    fun actionFor(id: String): () -> Unit = when (id) {
        "torch" -> { { com.gabriel.zenithui.data.Torch.toggle(context) } }
        "wifi" -> { { context.startActivityIntent(Settings.Panel.ACTION_WIFI, Settings.ACTION_WIFI_SETTINGS) } }
        "bluetooth" -> { { context.startActivityIntent(Settings.ACTION_BLUETOOTH_SETTINGS) } }
        "data" -> { { context.startActivityIntent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY, Settings.ACTION_WIRELESS_SETTINGS) } }
        "airplane" -> { { context.startActivityIntent(Settings.ACTION_AIRPLANE_MODE_SETTINGS) } }
        "focus" -> { { context.startActivityIntent("android.settings.ZEN_MODE_SETTINGS", Settings.ACTION_SOUND_SETTINGS) } }
        "location" -> { { context.startActivityIntent(Settings.ACTION_LOCATION_SOURCE_SETTINGS) } }
        "hotspot" -> { { context.startActivityIntent("android.settings.TETHER_SETTINGS", Settings.ACTION_WIRELESS_SETTINGS) } }
        "saver" -> { { context.startActivityIntent(Settings.ACTION_BATTERY_SAVER_SETTINGS) } }
        "volume" -> { { context.startActivityIntent(Settings.Panel.ACTION_VOLUME, Settings.ACTION_SOUND_SETTINGS) } }
        "nfc" -> { { context.startActivityIntent(Settings.ACTION_NFC_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS) } }
        else -> { { context.startActivityIntent(Settings.ACTION_DISPLAY_SETTINGS) } }
    }

    val options = chosen
        .mapNotNull { id -> TOGGLE_OPTIONS.firstOrNull { it.id == id } }
        .ifEmpty { TOGGLE_OPTIONS.take(1) }

    // Lay the chosen controls out on whichever grid gives each of them the most room at this size,
    // as many as can still be pressed: see ToggleGrid, which the widget showcase reads as well.
    val grid = remember(width, height, options.size) { ToggleGrid.of(width.value, height.value, options.size) }
    val pad = if (form == WidgetForm.Mini) 6.dp else 12.dp
    val toggles = options.take(grid.shown)
        .map { option -> Toggle(option, option.id == "torch" && torchOn, actionFor(option.id)) }
    val columns = grid.columns
    val cellWidth = grid.cellWidth.dp
    val cellHeight = grid.cellHeight.dp
    val tiles = grid.style == ToggleGrid.Style.Tiles
    val list = grid.style == ToggleGrid.Style.List
    val named = grid.style == ToggleGrid.Style.Named

    // A name under a button is set like the label under an app. The longest decides for them all,
    // so every button in the widget stays level: see ToggleNames for the rule.
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val labels = toggles.map { it.label }
    val nameLine = with(density) { TOGGLE_NAME_LINE.toDp() }
    // Measured in the font they are drawn in, or a wider font would not fit where it was measured to.
    val nameFont = LocalZenithFont.current
    val names = remember(labels, cellWidth, cellHeight, density.density, density.fontScale, nameFont) {
        fun widthOf(text: String) = with(density) {
            measurer.measure(text, TOGGLE_NAME_STYLE.copy(fontFamily = nameFont), softWrap = false, maxLines = 1).size.width.toDp().value
        }
        ToggleNames.fit(
            widestName = labels.maxOf { widthOf(it) },
            widestWord = labels.flatMap { it.split(' ') }.maxOf { widthOf(it) },
            cell = (cellWidth - 2.dp).value,
            roomForTwo = (cellHeight - TOGGLE_NAME_GAP - nameLine * 2) * 0.9f >= TOGGLE_NAMED_BUTTON,
        )
    }
    val nameStyle = TOGGLE_NAME_STYLE.copy(
        fontFamily = nameFont,
        fontSize = TOGGLE_NAME * names.scale,
        lineHeight = TOGGLE_NAME_LINE * names.scale,
        color = palette.onGlass,
    )
    // Buttons never grow past a comfortable size.
    val button = when {
        list -> minOf(cellHeight * 0.72f, 42.dp)
        named -> minOf(cellWidth * 0.6f, (cellHeight - TOGGLE_NAME_GAP - nameLine * names.scale * names.lines) * 0.9f, 46.dp)
        else -> minOf(cellWidth, cellHeight) * 0.74f
    }.coerceAtMost(46.dp)

    Column(Modifier.fillMaxSize().padding(pad)) {
        toggles.chunked(columns).forEach { line ->
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                line.forEach { toggle ->
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = if (list) Alignment.CenterStart else Alignment.Center) {
                        when {
                            tiles -> ToggleTile(
                                toggle,
                                Modifier
                                    .sizeIn(maxWidth = MAX_TILE_WIDTH, maxHeight = MAX_TILE_HEIGHT)
                                    .fillMaxSize()
                                    .padding(4.dp),
                                // A short tile has room for the name only: its second line would
                                // be cut through the middle.
                                detail = grid.detail,
                            )
                            list -> Row(verticalAlignment = Alignment.CenterVertically) {
                                ToggleButton(toggle, button)
                                Spacer(Modifier.width(10.dp))
                                Text(toggle.label, color = palette.onGlass, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            named -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ToggleButton(toggle, button)
                                Spacer(Modifier.height(TOGGLE_NAME_GAP))
                                // Every name takes the same number of lines, so the buttons line up.
                                Text(
                                    toggle.label,
                                    style = nameStyle,
                                    minLines = names.lines,
                                    maxLines = names.lines,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            else -> ToggleButton(toggle, button)
                        }
                    }
                }
                // Keep the last line's controls in their columns rather than spreading them out.
                repeat(columns - line.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Where you choose what the Quick Toggles widget shows. The widget rearranges itself to fit
 * whatever it holds, at whatever size it is, so any mix works.
 */
@Composable
fun TogglesSheet(
    visible: Boolean,
    selected: List<String>,
    onChange: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Quick Toggles") {
        Text(
            "Choose what the widget shows. It rearranges itself to fit, whatever its size.",
            color = palette.onGlassMuted,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(8.dp))
        Column(Modifier.verticalScroll(rememberScrollState())) {
            TOGGLE_OPTIONS.forEach { option ->
                val on = option.id in selected
                ToggleChoice(option, on) {
                    val next = if (on) selected - option.id else selected + option.id
                    // Always at least one control, and always in the catalogue's order.
                    if (next.isNotEmpty()) onChange(TOGGLE_OPTIONS.map { it.id }.filter { it in next })
                    next.isNotEmpty()
                }
            }
        }
    }
}

/**
 * One control the widget could show. Chosen, its disc takes the accent, and gives a small bounce
 * as it does; the switch beside it is the one Settings has, worked by the whole row. [onFlip]
 * answers whether it was flipped: the last one left cannot be taken away, and shakes its head.
 */
@Composable
private fun ToggleChoice(option: ToggleOption, on: Boolean, onFlip: () -> Boolean) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    val reduced = LocalMotionLevel.current.reduced
    val touch = remember { MutableInteractionSource() }
    val pressed by touch.collectIsPressedAsState()
    var refused by remember { mutableIntStateOf(0) }
    val disc by animateColorAsState(
        targetValue = if (on) palette.accent.copy(alpha = 0.9f) else palette.onGlass.copy(alpha = 0.10f),
        animationSpec = tween(260),
        label = "choiceDisc",
    )
    val ink by animateColorAsState(if (on) Color(0xFF0A0B12) else palette.onGlass, tween(260), label = "choiceInk")
    // Not as the list arrives: only when this one is chosen or let go while it is on show.
    val bounce = remember { Animatable(1f) }
    val shownAs = remember { booleanArrayOf(on) }
    LaunchedEffect(on) {
        if (shownAs[0] == on) return@LaunchedEffect
        shownAs[0] = on
        if (reduced) return@LaunchedEffect
        bounce.snapTo(0.8f)
        bounce.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 420f))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .shake(refused)
            .clickable(interactionSource = touch, indication = null) {
                if (onFlip()) tick() else refused++
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .graphicsLayer {
                    scaleX = bounce.value
                    scaleY = bounce.value
                }
                .clip(RoundedCornerShape(percent = 50))
                .background(disc),
            contentAlignment = Alignment.Center,
        ) {
            Icon(option.icon, contentDescription = null, tint = ink, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(option.label, color = palette.onGlass, fontSize = 15.sp)
            Text(option.detail, color = palette.onGlassMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        OnOffSwitch(on, pressed)
    }
}

private fun Context.startActivityIntent(action: String, fallback: String? = null) {
    runCatching { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure {
            if (fallback != null) runCatching { startActivity(Intent(fallback).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
}

@Composable
private fun ToggleButton(toggle: Toggle, size: Dp) {
    val palette = LocalPalette.current
    val background by animateColorAsState(
        targetValue = if (toggle.active) palette.accent.copy(alpha = 0.9f) else palette.onGlass.copy(alpha = 0.10f),
        animationSpec = tween(420),
        label = "toggleBg",
    )
    val tint by animateColorAsState(
        targetValue = if (toggle.active) inkOnTint(palette.accent) else palette.onGlass,
        animationSpec = tween(420),
        label = "toggleTint",
    )
    // On, it squares up a little; off, it is a circle again. The shape says it as well as the colour.
    val corner by animateIntAsState(
        targetValue = if (toggle.active) 34 else 50,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 260f),
        label = "toggleShape",
    )
    Box(
        Modifier
            .size(size)
            // Leans towards the finger, and keeps squaring up when it is on.
            .glassPress(GlassMotion.Lean)
            .clip(RoundedCornerShape(percent = corner.coerceIn(0, 50)))
            .background(background)
            .clickable(onClick = toggle.onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(toggle.icon, contentDescription = toggle.label, tint = tint, modifier = Modifier.size(size * TOGGLE_SYMBOL))
    }
}

@Composable
private fun ToggleTile(toggle: Toggle, modifier: Modifier, detail: Boolean) {
    val palette = LocalPalette.current
    val background by animateColorAsState(
        targetValue = if (toggle.active) palette.accent.copy(alpha = 0.9f) else palette.onGlass.copy(alpha = 0.08f),
        animationSpec = tween(420),
        label = "tileBg",
    )
    val ink = if (toggle.active) inkOnTint(palette.accent) else palette.onGlass
    Column(
        modifier
            .pressBounce(0.94f)
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .clickable(onClick = toggle.onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(toggle.icon, contentDescription = null, tint = ink, modifier = Modifier.size(26.dp))
        TileText(toggle, ink, detail)
    }
}

@Composable
private fun ColumnScope.TileText(toggle: Toggle, ink: Color, detail: Boolean) {
    Text(toggle.label, color = ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    if (!detail) return
    Text(
        text = if (toggle.option.id == "torch") (if (toggle.active) "On" else "Off") else "Open",
        color = ink.copy(alpha = 0.8f),
        fontSize = 12.sp,
        maxLines = 1,
    )
}

// ---------------------------------------------------------------------- search

/**
 * Search above the dock, as on an iPhone: a small pill of glass that says Search, and beside it a
 * round glass button that goes straight to the web. Switched on in Settings → Home screen.
 */
@Composable
fun HomeSearchBar(onSearch: () -> Unit, modifier: Modifier = Modifier) {
    // In the middle of the row it is given, as the iPhone's sits in the middle above its dock.
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { SearchPill(onSearch) }
}

/** The same, where a page from before still has search as a widget of its own. */
@Composable
private fun SearchWidget(onSearch: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { SearchPill(onSearch) }
}

@Composable
private fun SearchPill(onSearch: () -> Unit) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            Modifier
                .height(SEARCH_PILL_HEIGHT)
                .glass(ZenithShapes.pill, strength = 1.1f, material = GlassMaterial.Clear, motion = GlassMotion.Flex)
                .clickable(onClick = onSearch)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.onGlass, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Search", color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1)
        }
        Box(
            Modifier
                .size(SEARCH_PILL_HEIGHT)
                .glass(RoundedCornerShape(percent = 50), strength = 1.1f, material = GlassMaterial.Clear, motion = GlassMotion.Lean)
                .clickable { openWebSearch(context) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.TravelExplore, contentDescription = "Search the web", tint = palette.onGlass, modifier = Modifier.size(20.dp))
        }
    }
}

/** The visible height of the search row; its cell only reacts to touches inside this. */
val SEARCH_BAR_HEIGHT = 50.dp

/** The Search pill and its round button, inside that row. */
private val SEARCH_PILL_HEIGHT = 42.dp

/** Opens the device search app ready for a query, or Google in a browser. */
private fun openWebSearch(context: Context) {
    val search = Intent(Intent.ACTION_WEB_SEARCH).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val browser = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(search) }
        .onFailure { runCatching { context.startActivity(browser) } }
}
