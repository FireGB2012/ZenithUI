package com.gabriel.zenithui.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.GlassButton
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass

private data class Tip(val icon: ImageVector, val title: String, val body: String)

private val TIPS = listOf(
    Tip(Icons.Rounded.Apps, "Swipe up for everything", "All your apps live one swipe up. The search bar sits at the bottom and searches apps or Google."),
    Tip(Icons.Rounded.DragIndicator, "Hold and drag to arrange", "Long press any icon and drag it to any free cell, on any page."),
    Tip(Icons.Rounded.Folder, "Drop to make a folder", "Drop one app onto another and Zenith makes a folder. Drag to the top to remove."),
    Tip(Icons.Rounded.OpenInFull, "Resize your widgets", "Long press a widget, tap Resize, then drag its handles. It snaps to the grid."),
    Tip(Icons.Rounded.Dashboard, "Add more any time", "Long press an empty spot for wallpaper, widgets and settings."),
    Tip(Icons.Rounded.Tune, "Make it yours", "Palette, glass, grid size, icon shape and icon packs all live in Zenith settings."),
)

/** The last stop in onboarding, and the same content the Customise sheet can show later. */
@Composable
fun HelpStep(name: String, onDone: () -> Unit) {
    val palette = LocalPalette.current

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(40.dp))
        Text(
            text = if (name.isBlank()) "Here is how it works" else "How it works, ${name.trim()}",
            color = palette.onGlass,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.6).sp,
            modifier = Modifier.appearIn(),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Six gestures and you know the whole launcher.",
            color = palette.onGlassMuted,
            fontSize = 14.sp,
            modifier = Modifier.appearIn(delayMillis = 60),
        )
        Spacer(Modifier.height(18.dp))

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TipList(startDelay = 120)
            Spacer(Modifier.height(4.dp))
        }

        GlassButton(
            text = "Start using Zenith",
            onClick = onDone,
            filled = true,
            modifier = Modifier
                .fillMaxWidth()
                .appearIn(delayMillis = 380),
        )
        Spacer(Modifier.height(24.dp))
    }
}

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
