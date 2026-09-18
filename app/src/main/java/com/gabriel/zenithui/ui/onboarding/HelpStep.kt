package com.gabriel.zenithui.ui.onboarding

import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass

/**
 * The last screen of setup: the three gestures that unlock everything else. The full list lives
 * in Customise → How it works, where [TipList] shows it.
 */
@Composable
fun TipsStep() {
    val palette = LocalPalette.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
    ) {
        Spacer(Modifier.height(36.dp))
        ScreenTitle(
            eyebrow = "Before you go",
            headline = "Three things\nworth knowing.",
            lede = null,
            play = true,
        )
        Spacer(Modifier.height(32.dp))
        Column(verticalArrangement = Arrangement.spacedBy(26.dp)) {
            Gesture(1, "Swipe up", "for all your apps. Search sits at the bottom, right under your thumb.", order = 2)
            Gesture(2, "Hold an icon", "to move it. Drop it on another app to make a folder.", order = 3)
            Gesture(3, "Hold an empty spot", "for wallpaper, widgets and settings.", order = 4)
        }
        Spacer(Modifier.height(32.dp))
        Text(
            "The rest is in Customise, under How it works.",
            color = palette.onGlassMuted,
            fontSize = 13.sp,
            modifier = Modifier.rise(5),
        )
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun Gesture(number: Int, lead: String, rest: String, order: Int) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth().rise(order), verticalAlignment = Alignment.Top) {
        Box(
            Modifier
                .size(30.dp)
                .border(1.dp, palette.onGlass.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", color = palette.onGlass, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = palette.onGlass, fontWeight = FontWeight.SemiBold)) { append(lead) }
                append(" ")
                withStyle(SpanStyle(color = palette.onGlassMuted)) { append(rest) }
            },
            fontSize = 17.sp,
            lineHeight = 24.sp,
            modifier = Modifier.padding(top = 3.dp),
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
    Tip(Icons.Rounded.Tune, "Make it yours", "Palette, glass, grid size, icon shape and icon packs all live in Zenith settings."),
)

/** Every gesture, for the How it works sheet. */
@Composable
fun TipList(startDelay: Int = 0) {
    TIPS.forEachIndexed { index, tip ->
        TipCard(tip = tip, delayMillis = startDelay + staggerDelay(index) * 2)
    }
}

@Composable
private fun TipCard(tip: Tip, delayMillis: Int) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .appearIn(delayMillis = delayMillis, riseFrom = 26f)
            .glass(ZenithShapes.card, strength = 0.85f)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(40.dp)
                .glass(ZenithShapes.tile, strength = 1.1f),
            contentAlignment = Alignment.Center,
        ) {
            Icon(tip.icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.size(14.dp))
        Column(Modifier.weight(1f)) {
            Text(tip.title, color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(tip.body, color = palette.onGlassMuted, fontSize = 12.5.sp, lineHeight = 17.sp)
        }
    }
}
