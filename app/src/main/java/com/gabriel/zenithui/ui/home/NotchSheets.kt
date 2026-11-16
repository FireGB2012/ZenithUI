package com.gabriel.zenithui.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.NotchAccess
import com.gabriel.zenithui.ui.common.GlassButton
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.corners
import com.gabriel.zenithui.ui.theme.glass

/**
 * Before Android's page for one of the Notch's two permissions opens: what it is for, in plain words,
 * what Zenith does and does not do with it, and the steps on Android's page, which only names the
 * permission. Asked here, once, with a picture of the Notch it unlocks, rather than by dropping
 * somebody into a list of switches.
 */
@Composable
internal fun NotchAccessSheet(visible: Boolean, which: NotchAccess, onOpen: () -> Unit, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    val words = when (which) {
        NotchAccess.OVER_APPS -> NotchWords(
            title = "Show the Notch over your apps",
            why = "Android keeps apps from drawing over one another unless you say so. The Notch sits round your " +
                "camera while you use other apps, so Zenith needs this one permission to put it there.",
            promise = "It only draws the Notch itself. It can't see or read the apps under it, and it never takes a " +
                "tap meant for them.",
            steps = listOf("Tap Open settings", "Find Zenith in the list", "Turn on Allow display over other apps"),
        )
        NotchAccess.NOTIFICATIONS -> NotchWords(
            title = "Let the Notch see what's playing",
            why = "Music, calls, timers and messages reach the Notch through Android's notification access, the same " +
                "access the dots on your icons use. That's how it shows the song, the cover and the buttons to play, " +
                "skip and go back.",
            promise = "It reads them here on your phone, only to show them. Nothing is kept and nothing is sent anywhere.",
            steps = listOf("Tap Open settings", "Choose Zenith", "Turn on notification access and confirm"),
        )
    }
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = words.title) {
        NotchPicture(which, Modifier.appearIn(delayMillis = staggerDelay(0), riseFrom = 18f, fromScale = 0.94f))
        Spacer(Modifier.height(16.dp))
        Text(
            words.why,
            color = palette.onGlass,
            fontSize = 15.sp,
            lineHeight = 21.sp,
            modifier = Modifier.appearIn(delayMillis = staggerDelay(1), riseFrom = 18f),
        )
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.appearIn(delayMillis = staggerDelay(2), riseFrom = 18f),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = palette.onGlassMuted, modifier = Modifier.padding(top = 2.dp).size(15.dp))
            Spacer(Modifier.width(8.dp))
            Text(words.promise, color = palette.onGlassMuted, fontSize = 13.5.sp, lineHeight = 19.sp)
        }
        Spacer(Modifier.height(16.dp))
        Column(
            Modifier.appearIn(delayMillis = staggerDelay(3), riseFrom = 18f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            words.steps.forEachIndexed { index, step ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(22.dp).clip(CircleShape).background(palette.accent.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${index + 1}", color = palette.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(step, color = palette.onGlass, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.appearIn(delayMillis = staggerDelay(4), riseFrom = 18f),
        ) {
            GlassButton(text = "Not now", onClick = onDismiss, modifier = Modifier.weight(1f))
            GlassButton(
                text = "Open settings",
                filled = true,
                onClick = {
                    onOpen()
                    onDismiss()
                },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private class NotchWords(val title: String, val why: String, val promise: String, val steps: List<String>)

/**
 * The top of a phone in glass, with the Notch round its camera showing what the permission brings:
 * the battery and a status icon over other apps, or the music playing.
 */
@Composable
private fun NotchPicture(which: NotchAccess, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Box(
        modifier
            .fillMaxWidth()
            .height(84.dp)
            .glass(corners(26.dp), strength = 0.9f),
        contentAlignment = Alignment.TopCenter,
    ) {
        Row(
            Modifier.padding(top = 18.dp).height(30.dp).clip(RoundedCornerShape(50)).background(Color.Black).padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (which) {
                NotchAccess.OVER_APPS -> {
                    Icon(Icons.Rounded.DarkMode, contentDescription = null, tint = Color(0xFFBF5AF2), modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(34.dp))
                    Text("82%", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif)
                    Spacer(Modifier.width(5.dp))
                    Canvas(Modifier.size(width = 21.dp, height = 10.dp)) {
                        val stroke = 1.2.dp.toPx()
                        drawRoundRect(
                            Color.White.copy(alpha = 0.45f),
                            topLeft = Offset(stroke / 2, stroke / 2),
                            size = Size(size.width - 3.dp.toPx(), size.height - stroke),
                            cornerRadius = CornerRadius(3.dp.toPx()),
                            style = Stroke(stroke),
                        )
                        val inset = 2.2.dp.toPx()
                        drawRoundRect(
                            Color.White,
                            topLeft = Offset(inset, inset),
                            size = Size((size.width - 3.dp.toPx() - inset * 2) * 0.82f, size.height - inset * 2),
                            cornerRadius = CornerRadius(1.6.dp.toPx()),
                        )
                    }
                }
                NotchAccess.NOTIFICATIONS -> {
                    Box(
                        Modifier.size(20.dp).clip(corners(6.dp)).background(Color(0xFFFF5E8A).copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color(0xFFFF5E8A), modifier = Modifier.size(13.dp))
                    }
                    Spacer(Modifier.width(34.dp))
                    Canvas(Modifier.size(width = 18.dp, height = 14.dp)) {
                        val bar = size.width / 7f
                        val step = (size.width - bar) / 3f
                        listOf(0.5f, 1f, 0.7f, 0.4f).forEachIndexed { index, level ->
                            val tall = size.height * level
                            drawRoundRect(
                                Color(0xFFFF5E8A),
                                topLeft = Offset(index * step, (size.height - tall) / 2f),
                                size = Size(bar, tall),
                                cornerRadius = CornerRadius(bar / 2f),
                            )
                        }
                    }
                }
            }
        }
        // The camera, in the middle of the Notch, as it is on the phone.
        Box(Modifier.padding(top = 24.dp).size(18.dp).clip(CircleShape).background(Color(0xFF0A0A0E)))
        Text(
            if (which == NotchAccess.OVER_APPS) "Over any app" else "Now playing",
            color = palette.onGlassMuted,
            fontSize = 12.sp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
        )
    }
}
