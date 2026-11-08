package com.gabriel.zenithui.ui.home

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.text.format.DateFormat
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gabriel.zenithui.data.CalendarAccess
import com.gabriel.zenithui.data.CalendarEvent
import com.gabriel.zenithui.data.CalendarRepository
import com.gabriel.zenithui.data.findActivity
import com.gabriel.zenithui.ui.common.ZenithButton
import com.gabriel.zenithui.ui.theme.ButtonKind
import com.gabriel.zenithui.ui.theme.GlassMotion
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.theme.LocalNumberFont
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * A bedside clock. Android does not let an app draw on the system always-on display, so this is
 * the next best thing: a dimmed full screen clock that keeps the display awake while it is open.
 */
@Composable
fun AmbientScreen(
    userName: String,
    /** "Not now" to today's events, remembered from an earlier visit. */
    calendarDeclined: Boolean,
    onCalendarDeclined: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val view = LocalView.current

    var permission by remember { mutableStateOf(CalendarRepository.hasPermission(context)) }
    var reload by remember { mutableStateOf(0) }
    // Whether Android's question has been put on this visit and refused, and whether it would
    // still be shown if it were put again. After a second refusal Android stops showing it, and
    // a button that then did nothing would look broken.
    var refused by remember { mutableStateOf(false) }
    var canAskAgain by remember { mutableStateOf(true) }
    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permission = granted
        refused = !granted
        canAskAgain = context.findActivity()?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.READ_CALENDAR)
        } ?: true
        if (granted) onCalendarDeclined(false)
        reload += 1
    }
    // It may have been allowed, or taken away again, in the phone's settings while this was open.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val now = CalendarRepository.hasPermission(context)
        if (now != permission) {
            permission = now
            reload += 1
        }
    }
    val access = CalendarAccess.of(permission, calendarDeclined, refused, canAskAgain)

    // Keep the screen awake and dim it right down while the clock is showing. Not while it is
    // saying what the calendar is for, though: small print cannot be read on a screen that dim.
    val reading = access == CalendarAccess.Offer || access == CalendarAccess.Blocked
    DisposableEffect(reading) {
        val window = (view.context as? Activity)?.window
        val previous = window?.attributes?.screenBrightness
        if (!reading) window?.attributes = window.attributes?.apply { screenBrightness = 0.04f }
        view.keepScreenOn = true
        onDispose {
            view.keepScreenOn = false
            window?.attributes = window.attributes?.apply {
                screenBrightness = previous ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            }
        }
    }

    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            delay(1_000L - (System.currentTimeMillis() % 1_000L))
        }
    }
    val events by produceState(initialValue = emptyList<CalendarEvent>(), permission, reload, now.hour) {
        value = CalendarRepository.today(context)
    }

    val twentyFourHour = remember { DateFormat.is24HourFormat(context) }
    val timePattern = if (twentyFourHour) "HH:mm" else "h:mm"
    val time = remember(now.minute, twentyFourHour) { now.format(DateTimeFormatter.ofPattern(timePattern)) }
    val date = remember(now.dayOfYear) { now.format(DateTimeFormatter.ofPattern("EEEE d MMMM")) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 56.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Spacer(Modifier.height(40.dp))

            // The time, then the day, then the greeting, each settling up into place as the
            // room goes dark: slowly, and without a bounce, as befits a bedside.
            AnimatedContent(
                targetState = time,
                transitionSpec = {
                    (slideInVertically(spring(dampingRatio = 0.6f, stiffness = 170f)) { it / 3 } + fadeIn(tween(500)))
                        .togetherWith(slideOutVertically(tween(600)) { -it / 3 } + fadeOut(tween(360)))
                },
                modifier = Modifier.appearIn(delayMillis = 120, fromScale = 0.96f, riseFrom = 60f, gentle = true),
                label = "ambientClock",
            ) { value ->
                Text(
                    text = value,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 92.sp,
                    fontFamily = LocalNumberFont.current.family,
                    fontWeight = FontWeight.Thin,
                    letterSpacing = (-4).sp,
                )
            }

            Text(
                text = date,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 17.sp,
                fontWeight = FontWeight.Light,
                modifier = Modifier.appearIn(delayMillis = 260, fromScale = 1f, riseFrom = 40f, gentle = true),
            )
            if (userName.isNotBlank()) {
                Text(
                    text = greeting(now.hour, userName),
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 14.sp,
                    modifier = Modifier.appearIn(delayMillis = 380, fromScale = 1f, riseFrom = 40f, gentle = true),
                )
            }

            Spacer(Modifier.height(44.dp))

            when {
                // What it is for and what is read, before Android asks: its own question says
                // only "access your calendar".
                access == CalendarAccess.Offer -> CalendarOffer(
                    title = "Today's events, under the clock",
                    lines = listOf(
                        "Zenith can list what is on your calendar today: each event's name, its time and its colour.",
                        "It only reads them, each time this clock opens. Nothing is changed, kept or sent anywhere.",
                        "The clock works without it, and you can take it back in the phone's settings.",
                    ),
                    action = "Allow calendar",
                    onAction = { requestPermission.launch(Manifest.permission.READ_CALENDAR) },
                    onNotNow = { onCalendarDeclined(true) },
                )

                access == CalendarAccess.Blocked -> CalendarOffer(
                    title = "Calendar is switched off for Zenith",
                    lines = listOf(
                        "Android has stopped asking, so it has to be allowed in the phone's settings: Permissions, then Calendar, then Allow.",
                        "Zenith would only read today's events to list them here. Nothing is changed, kept or sent anywhere.",
                    ),
                    action = "Open settings",
                    onAction = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                    },
                    onNotNow = {
                        refused = false
                        onCalendarDeclined(true)
                    },
                )

                access == CalendarAccess.Declined -> Row(Modifier.appearIn(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Today's events are off.", color = Color.White.copy(alpha = 0.45f), fontSize = 14.sp)
                    Text(
                        text = "Turn on",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .pressBounce(0.94f)
                            .clip(RoundedCornerShape(50))
                            .clickable { onCalendarDeclined(false) }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }

                events.isEmpty() -> Text(
                    text = "Nothing on your calendar today.",
                    color = Color.White.copy(alpha = 0.45f),
                    fontSize = 15.sp,
                    modifier = Modifier.appearIn(),
                )

                else -> Column(
                    Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    events.take(8).forEachIndexed { index, event ->
                        EventRow(event = event, delayMillis = staggerDelay(index))
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            Text(
                text = "Tap anywhere to go back",
                color = Color.White.copy(alpha = 0.22f),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .appearIn(delayMillis = 500),
            )
        }
    }
}

/**
 * The offer of today's events: what it is, the reasons in plain lines, the button that goes on to
 * Android's question (or to the phone's settings) and a way to say no that is remembered.
 */
@Composable
private fun CalendarOffer(
    title: String,
    lines: List<String>,
    action: String,
    onAction: () -> Unit,
    onNotNow: () -> Unit,
) {
    Column(Modifier.appearIn()) {
        Text(title, color = Color.White.copy(alpha = 0.85f), fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))
        lines.forEach { line ->
            Text(line, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp, lineHeight = 19.sp)
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            ZenithButton(text = action, onClick = onAction, kind = ButtonKind.Grey)
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Not now",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 14.sp,
                modifier = Modifier
                    .pressBounce(0.94f)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onNotNow)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
private fun EventRow(event: CalendarEvent, delayMillis: Int) {
    val palette = LocalPalette.current
    val accent = if (event.colour != 0) Color(event.colour) else palette.accent
    val formatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    val start = remember(event.startMillis) {
        java.time.Instant.ofEpochMilli(event.startMillis)
            .atZone(java.time.ZoneId.systemDefault())
            .format(formatter)
    }

    Row(
        Modifier
            .fillMaxWidth()
            .appearIn(delayMillis = delayMillis, riseFrom = 18f)
            .glass(ZenithShapes.card, strength = 0.5f, opacity = 0.5f)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .width(3.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(50))
                .background(accent.copy(alpha = 0.8f))
        )
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = event.title,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                text = if (event.allDay) "All day" else start,
                color = Color.White.copy(alpha = 0.4f),
                fontSize = 12.sp,
            )
        }
    }
}

private fun greeting(hour: Int, name: String) = when (hour) {
    in 5..11 -> "Good morning, $name"
    in 12..17 -> "Good afternoon, $name"
    in 18..22 -> "Good evening, $name"
    else -> "Sleep well, $name"
}
