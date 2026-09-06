package com.gabriel.zenithui.ui.home

import android.Manifest
import android.app.Activity
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
import com.gabriel.zenithui.data.CalendarEvent
import com.gabriel.zenithui.data.CalendarRepository
import com.gabriel.zenithui.ui.common.GlassButton
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.staggerDelay
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
fun AmbientScreen(userName: String, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val view = LocalView.current

    var permission by remember { mutableStateOf(CalendarRepository.hasPermission(context)) }
    var reload by remember { mutableStateOf(0) }
    val requestPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permission = granted
        reload += 1
    }

    // Keep the screen awake and dim it right down while the clock is showing.
    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val previous = window?.attributes?.screenBrightness
        window?.attributes = window.attributes?.apply { screenBrightness = 0.04f }
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

            AnimatedContent(
                targetState = time,
                transitionSpec = {
                    (slideInVertically(spring(dampingRatio = 0.6f, stiffness = 170f)) { it / 3 } + fadeIn(tween(500)))
                        .togetherWith(slideOutVertically(tween(600)) { -it / 3 } + fadeOut(tween(360)))
                },
                label = "ambientClock",
            ) { value ->
                Text(
                    text = value,
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 92.sp,
                    fontWeight = FontWeight.Thin,
                    letterSpacing = (-4).sp,
                )
            }

            Text(
                text = date,
                color = Color.White.copy(alpha = 0.55f),
                fontSize = 17.sp,
                fontWeight = FontWeight.Light,
            )
            if (userName.isNotBlank()) {
                Text(
                    text = greeting(now.hour, userName),
                    color = Color.White.copy(alpha = 0.35f),
                    fontSize = 14.sp,
                )
            }

            Spacer(Modifier.height(44.dp))

            when {
                !permission -> Column(Modifier.appearIn()) {
                    Text(
                        text = "Show today's events here",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 15.sp,
                    )
                    Spacer(Modifier.height(14.dp))
                    GlassButton(
                        text = "Allow calendar",
                        onClick = { requestPermission.launch(Manifest.permission.READ_CALENDAR) },
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
