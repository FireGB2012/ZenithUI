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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/** Renders one of Zenith's built-in widgets into whatever space the grid gave it. */
@Composable
fun ZenithWidget(
    type: String,
    userName: String,
    note: String,
    onNoteChange: (String) -> Unit,
    onSearch: () -> Unit,
    onAmbient: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (type == "search") {
        // A one line bar: it draws, and reacts, at exactly its own size.
        Box(modifier, contentAlignment = Alignment.Center) {
            SearchWidget(onSearch)
        }
        return
    }

    val palette = LocalPalette.current
    Box(
        modifier
            .glass(ZenithShapes.card, strength = 1f)
            .clip(ZenithShapes.card)
            // Two soft pools of the theme's colours under the content, so every widget carries a
            // little of the palette instead of being plain grey glass.
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(palette.accent.copy(alpha = 0.26f), Color.Transparent),
                        center = Offset(size.width * 0.12f, size.height * 0.05f),
                        radius = size.maxDimension * 0.75f,
                    ),
                    radius = size.maxDimension * 0.75f,
                    center = Offset(size.width * 0.12f, size.height * 0.05f),
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        listOf(palette.accentAlt.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.95f, size.height),
                        radius = size.maxDimension * 0.6f,
                    ),
                    radius = size.maxDimension * 0.6f,
                    center = Offset(size.width * 0.95f, size.height),
                )
            }
    ) {
        when (type) {
            "clock" -> ClockWidget(userName)
            "battery" -> BatteryWidget()
            "calendar" -> CalendarWidget()
            "notes" -> NotesWidget(note, onNoteChange)
            "toggles" -> TogglesWidget()
            else -> Box(Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun rememberNow(): LocalDateTime {
    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            val millisToNextSecond = 1_000L - (System.currentTimeMillis() % 1_000L)
            delay(millisToNextSecond)
        }
    }
    return now
}

@Composable
private fun ClockWidget(userName: String) {
    val palette = LocalPalette.current
    val now = rememberNow()
    val time = remember(now.hour, now.minute) { now.format(DateTimeFormatter.ofPattern("HH:mm")) }
    val longDate = remember(now.dayOfYear) { now.format(DateTimeFormatter.ofPattern("EEEE d MMMM")) }
    val shortDate = remember(now.dayOfYear) { now.format(DateTimeFormatter.ofPattern("EEE d MMM")) }
    val greeting = when (now.hour) {
        in 5..11 -> "Good morning"
        in 12..17 -> "Good afternoon"
        in 18..22 -> "Good evening"
        else -> "Still up"
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxWidth < 260.dp
        val room = maxHeight > 140.dp
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = if (compact) 14.dp else 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = now.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.getDefault()).uppercase(),
                color = palette.accent,
                fontSize = if (compact) 10.sp else 11.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.6.sp,
                maxLines = 1,
            )
            AnimatedContent(
                targetState = time,
                transitionSpec = {
                    (slideInVertically(spring(dampingRatio = 0.6f, stiffness = 180f)) { it / 3 } + fadeIn(tween(420)))
                        .togetherWith(slideOutVertically(tween(560)) { -it / 3 } + fadeOut(tween(320)))
                },
                label = "clock",
            ) { value ->
                // The time fades from the text colour into the accent, like light through glass.
                Text(
                    text = value,
                    style = TextStyle(
                        brush = Brush.linearGradient(listOf(palette.onGlass, palette.accent, palette.accentAlt)),
                        fontSize = if (compact) 40.sp else 58.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = (-1.5).sp,
                    ),
                    maxLines = 1,
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (compact) shortDate else longDate,
                color = palette.onGlass.copy(alpha = 0.82f),
                fontSize = if (compact) 12.sp else 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (userName.isNotBlank() && room) {
                Text(
                    text = if (compact) "Hi, $userName" else "$greeting, $userName",
                    color = palette.onGlassMuted,
                    fontSize = if (compact) 11.sp else 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun BatteryWidget() {
    val palette = LocalPalette.current
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

    val fraction = (level.coerceAtLeast(0)) / 100f

    Box(Modifier.fillMaxSize().padding(14.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 9.dp.toPx()
            val arcSize = Size(size.minDimension - stroke, size.minDimension - stroke)
            val topLeft = Offset((size.width - arcSize.width) / 2f, (size.height - arcSize.height) / 2f)
            drawArc(
                color = palette.onGlass.copy(alpha = 0.12f),
                startAngle = 130f, sweepAngle = 280f, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                brush = Brush.linearGradient(listOf(palette.accent, palette.accentAlt)),
                startAngle = 130f, sweepAngle = 280f * fraction, useCenter = false,
                topLeft = topLeft, size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = if (level < 0) "--" else "$level%",
                color = palette.onGlass,
                fontSize = 22.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = if (charging) "Charging" else "Battery",
                color = palette.onGlassMuted,
                fontSize = 11.sp,
            )
        }
    }
}

@Composable
private fun CalendarWidget() {
    val palette = LocalPalette.current
    val today = remember { LocalDate.now() }
    val firstOfMonth = remember(today) { today.withDayOfMonth(1) }
    val leading = remember(firstOfMonth) { (firstOfMonth.dayOfWeek.value - 1).coerceAtLeast(0) }
    val daysInMonth = remember(today) { today.lengthOfMonth() }

    val remaining = ChronoUnit.DAYS.between(today, today.withDayOfMonth(daysInMonth))
    val cells = leading + daysInMonth
    val weeks = (cells + 6) / 7

    // Numbers are drawn with no font padding and their line trimmed to the glyphs, so a digit
    // sits in the exact middle of its cell — and of the dot behind today.
    val dayStyle = TextStyle(
        fontSize = 9.5.sp,
        lineHeight = 10.sp,
        textAlign = TextAlign.Center,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )

    @Composable
    fun monthGrid(modifier: Modifier) {
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
                            // Every cell takes an equal seventh, and the dot is centred in it.
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (dayNumber in 1..daysInMonth) {
                                    Box(
                                        Modifier
                                            .size(18.dp)
                                            .clip(RoundedCornerShape(percent = 50))
                                            .then(
                                                if (isToday) {
                                                    Modifier.background(
                                                        Brush.linearGradient(listOf(palette.accent, palette.accentAlt))
                                                    )
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
                                                    isToday -> Color(0xFF0A0B12)
                                                    dow >= 5 -> palette.onGlass.copy(alpha = 0.5f)
                                                    else -> palette.onGlass.copy(alpha = 0.8f)
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

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val wide = maxWidth > 250.dp
        if (wide) {
            // Room for both: today in large type on the left, the month on the right.
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(0.8f)) {
                    Text(
                        text = today.format(DateTimeFormatter.ofPattern("EEEE")).uppercase(),
                        color = palette.accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.4.sp,
                        maxLines = 1,
                    )
                    Text(
                        text = today.dayOfMonth.toString(),
                        style = TextStyle(
                            brush = Brush.linearGradient(listOf(palette.onGlass, palette.accent)),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.Light,
                        ),
                    )
                    Text(
                        text = today.format(DateTimeFormatter.ofPattern("MMMM")),
                        color = palette.onGlass.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = if (remaining <= 0) "Last day" else "$remaining days left",
                        color = palette.onGlassMuted,
                        fontSize = 11.sp,
                    )
                }
                Spacer(Modifier.size(10.dp))
                monthGrid(Modifier.weight(1.2f).fillMaxHeight())
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp, vertical = 10.dp),
            ) {
                Text(
                    text = today.format(DateTimeFormatter.ofPattern("MMMM")).uppercase(),
                    color = palette.accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                )
                monthGrid(Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

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
private fun NotesWidget(note: String, onNoteChange: (String) -> Unit) {
    val palette = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    var fieldBounds by remember { mutableStateOf(Rect.Zero) }
    // A note scrolled off with its page, or removed, must not leave a stale area behind.
    DisposableEffect(Unit) { onDispose { if (focused) HomeTypingArea.bounds = null } }
    Column(
        Modifier
            .fillMaxSize()
            .padding(14.dp),
    ) {
        Text("NOTE", color = palette.accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = note,
            onValueChange = onNoteChange,
            textStyle = TextStyle(color = palette.onGlass, fontSize = 13.sp, lineHeight = 18.sp),
            cursorBrush = SolidColor(palette.accent),
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { coords ->
                    fieldBounds = coords.boundsInRoot()
                    if (focused) HomeTypingArea.bounds = fieldBounds
                }
                .onFocusChanged { state ->
                    focused = state.isFocused
                    HomeTypingArea.bounds = if (state.isFocused) fieldBounds else null
                },
            decorationBox = { inner ->
                Box(Modifier.fillMaxSize()) {
                    if (note.isEmpty()) {
                        Text(
                            "Tap to jot something down",
                            color = palette.onGlass.copy(alpha = 0.32f),
                            fontSize = 13.sp,
                        )
                    }
                    inner()
                }
            },
        )
    }
}

@Composable
private fun TogglesWidget() {
    val context = LocalContext.current
    var torchOn by remember { mutableStateOf(false) }

    val toggles = listOf<Triple<ImageVector, String, () -> Unit>>(
        Triple(Icons.Rounded.FlashlightOn, "Torch") {
            val manager = context.getSystemService(CameraManager::class.java)
            runCatching {
                val id = manager.cameraIdList.firstOrNull { camId ->
                    manager.getCameraCharacteristics(camId)
                        .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                }
                if (id != null) {
                    torchOn = !torchOn
                    manager.setTorchMode(id, torchOn)
                }
            }
            Unit
        },
        Triple(Icons.Rounded.Wifi, "Wi-Fi") { context.startActivityIntent(Settings.ACTION_WIFI_SETTINGS) },
        Triple(Icons.Rounded.Bluetooth, "Bluetooth") { context.startActivityIntent(Settings.ACTION_BLUETOOTH_SETTINGS) },
        Triple(Icons.Rounded.DoNotDisturbOn, "Focus") {
            context.startActivityIntent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
        },
    )

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val singleRow = maxWidth > maxHeight * 1.7f
        val buttonSize = if (singleRow) 46.dp else 50.dp

        @Composable
        fun button(index: Int) {
            val (icon, label, action) = toggles[index]
            ToggleButton(
                icon = icon,
                label = label,
                active = index == 0 && torchOn,
                size = buttonSize,
                onClick = action,
            )
        }

        if (singleRow) {
            Row(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                toggles.indices.forEach { button(it) }
            }
        } else {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                listOf(0, 2).forEach { start ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        button(start)
                        button(start + 1)
                    }
                }
            }
        }
    }
}

private fun Context.startActivityIntent(action: String) {
    runCatching { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

@Composable
private fun ToggleButton(
    icon: ImageVector,
    label: String,
    active: Boolean,
    size: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val background by animateColorAsState(
        targetValue = if (active) palette.accent.copy(alpha = 0.9f) else palette.onGlass.copy(alpha = 0.10f),
        animationSpec = tween(420),
        label = "toggleBg",
    )
    val tint by animateColorAsState(
        targetValue = if (active) Color(0xFF0A0B12) else palette.onGlass,
        animationSpec = tween(420),
        label = "toggleTint",
    )
    Box(
        Modifier
            .size(size)
            .pressBounce(0.86f)
            .clip(RoundedCornerShape(percent = 50))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(size * 0.44f),
        )
    }
}

@Composable
private fun SearchWidget(onSearch: () -> Unit) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(SEARCH_BAR_HEIGHT)
            .pressBounce(0.97f)
            .glass(ZenithShapes.pill, strength = 1.1f)
            .clickable(onClick = onSearch)
            .padding(start = 18.dp, end = SEARCH_BAR_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.onGlassMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(10.dp))
        Text(
            text = "Search apps or the web",
            color = palette.onGlassMuted,
            fontSize = 14.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                // Sized to the bar so the circle sits flush inside its rounded end.
                .size(SEARCH_BAR_HEIGHT - SEARCH_BAR_INSET * 2)
                .pressBounce(0.88f)
                .clip(RoundedCornerShape(percent = 50))
                .background(palette.accent.copy(alpha = 0.9f))
                .clickable { openWebSearch(context) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.TravelExplore,
                contentDescription = "Search the web",
                tint = Color(0xFF0A0B12),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** The visible height of the search bar; its cell only reacts to touches inside this. */
val SEARCH_BAR_HEIGHT = 50.dp

/** Gap between the bar's edge and the round web-search button. */
private val SEARCH_BAR_INSET = 6.dp

/** Opens the device search app ready for a query, or Google in a browser. */
private fun openWebSearch(context: Context) {
    val search = Intent(Intent.ACTION_WEB_SEARCH).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val browser = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(search) }
        .onFailure { runCatching { context.startActivity(browser) } }
}
