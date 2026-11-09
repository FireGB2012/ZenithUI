package com.gabriel.zenithui.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.SwipeUp
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.settings.Section
import com.gabriel.zenithui.ui.settings.CardRows
import com.gabriel.zenithui.ui.theme.LocalApple

/**
 * The last stop of setup, under the finished phone: the three moves that unlock everything else.
 * The full list lives in Customise, under How it works, where [TipList] shows it.
 */
@Composable
fun QuickTips(play: Boolean) {
    val palette = LocalPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp)
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        QuickTip(Icons.Rounded.SwipeUp, "Swipe up", "for all your apps and search.", order = 0, play = play)
        QuickTip(Icons.Rounded.TouchApp, "Hold an icon", "to move it, or drop it on another for a folder.", order = 1, play = play)
        QuickTip(Icons.Rounded.Wallpaper, "Hold an empty spot", "for wallpaper, widgets and settings.", order = 2, play = play)
        Text(
            "The rest is in Customise, under How it works.",
            color = palette.onGlassMuted,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp)
                .rise(3, play),
        )
    }
}

@Composable
private fun QuickTip(icon: ImageVector, lead: String, rest: String, order: Int, play: Boolean) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth().rise(order, play), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(40.dp)
                .glass(CircleShape, strength = 0.9f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = palette.onGlass, fontWeight = FontWeight.SemiBold)) { append(lead) }
                append(" ")
                withStyle(SpanStyle(color = palette.onGlassMuted)) { append(rest) }
            },
            fontSize = 15.sp,
            lineHeight = 21.sp,
        )
    }
}

private data class Tip(val icon: ImageVector, val title: String, val body: String)

private val TIPS = listOf(
    Tip(Icons.Rounded.Apps, "Swipe up for everything", "All your apps live one swipe up. The search bar sits at the bottom and searches apps or Google."),
    Tip(Icons.Rounded.DragIndicator, "Hold and drag to arrange", "Long press any icon and drag it to any free cell, or down into the dock. Hold it against the side of the screen to carry it to the next page."),
    Tip(Icons.Rounded.Add, "More pages when you need them", "Tap the + beside the page dots for another page. Pages you leave empty tidy themselves away."),
    Tip(Icons.Rounded.Folder, "Drop to make a folder", "Drop one app onto another and Zenith makes a folder. Drag to the top to remove."),
    Tip(Icons.Rounded.OpenInFull, "Resize your widgets", "Long press a widget, tap Resize, then drag its handles. It snaps to the grid."),
    Tip(Icons.Rounded.Dashboard, "Add more any time", "Long press an empty spot for wallpaper, widgets and settings."),
    Tip(Icons.Rounded.Gesture, "Gestures your way", "Swipe down for notifications and pinch in for Customise. Double tap, two fingers, pinch out and the home button can do what you like, under Settings → Gestures."),
    Tip(Icons.Rounded.Edit, "Any app, any name", "Long press an app and tap Edit for a name, an icon from an icon pack or a photo of your own. Its shortcuts are at the top of the same menu."),
    Tip(Icons.Rounded.Tune, "Make it yours", "Palette, glass, grid size, icon shape, icon style and icon packs all live in Zenith settings."),
)

/** Every gesture, for the How it works sheet. */
@Composable
fun TipList(startDelay: Int = 0) {
    // One card of rows, as an iPhone lists things, each tip coming up a beat behind the one above.
    Section {
        CardRows {
            TIPS.forEachIndexed { index, tip ->
                TipRow(tip = tip, delayMillis = startDelay + staggerDelay(index) * 2)
            }
        }
    }
}

/** One tip: its symbol in the accent, what to do, and how, in grey. */
@Composable
private fun TipRow(tip: Tip, delayMillis: Int) {
    val apple = LocalApple.current
    val accent = LocalPalette.current.accent
    Row(
        Modifier
            .fillMaxWidth()
            .appearIn(delayMillis = delayMillis, riseFrom = 14f)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(Modifier.size(30.dp), contentAlignment = Alignment.Center) {
            Icon(tip.icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(tip.title, color = apple.words, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.size(2.dp))
            Text(tip.body, color = apple.secondary, fontSize = 13.sp, lineHeight = 18.sp)
        }
    }
}
