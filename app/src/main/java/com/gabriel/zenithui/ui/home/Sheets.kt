package com.gabriel.zenithui.ui.home

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.HelpOutline
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.FolderCell
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.AppCell
import com.gabriel.zenithui.data.WidgetCell
import com.gabriel.zenithui.data.Widgets
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.onboarding.TipList
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.glass

/** Bottom-anchored glass sheet: springs up from the edge, fades its scrim, slides back out. */
@Composable
fun ZenithSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    title: String? = null,
    /** How much of the screen the sheet may take. Settings, with its pages, gets more. */
    maxHeightFraction: Float = 0.72f,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * maxHeightFraction

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(if (reduced) Motion.REDUCED_MS else 300)),
            exit = fadeOut(tween(if (reduced) Motion.REDUCED_MS else 260)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.35f))
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { onDismiss() },
            )
        }

        AnimatedVisibility(
            visible = visible,
            // With movement reduced a sheet does not travel: it is there, and then it is not.
            enter = if (reduced) fadeIn(tween(Motion.REDUCED_MS)) else slideInVertically(Motion.smooth()) { it } + fadeIn(tween(220)),
            exit = if (reduced) fadeOut(tween(Motion.REDUCED_MS)) else slideOutVertically(tween(320)) { it } + fadeOut(tween(260)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .padding(10.dp)
                    // The regular material provides a readable body without a second opaque fill.
                    .glass(RoundedCornerShape(34.dp), strength = 1.2f, solid = 0.65f)
                    .clickable(indication = null, interactionSource = remember { MutableInteractionSource() }) { }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { _, delta -> if (delta > 30f) onDismiss() }
                    }
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
            ) {
                Box(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(palette.onGlass.copy(alpha = 0.25f))
                )
                if (title != null) {
                    Spacer(Modifier.height(14.dp))
                    Text(title, color = palette.onGlass, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                content()
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun MenuRow(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    danger: Boolean = false,
    index: Int = 0,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val tint = if (danger) Color(0xFFFF6B6B) else palette.onGlass
    Row(
        Modifier
            .fillMaxWidth()
            .appearIn(delayMillis = staggerDelay(index), riseFrom = 22f)
            .pressBounce(0.97f)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(tint.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                Text(subtitle, color = palette.onGlassMuted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun ItemMenuSheet(
    visible: Boolean,
    item: HomeItem,
    appsByKey: Map<String, AppInfo>,
    onOpen: (String) -> Unit,
    onAppInfo: (String) -> Unit,
    onRename: (String) -> Unit,
    onResize: () -> Unit,
    onRemove: () -> Unit,
    onUninstall: (String) -> Unit,
    onChooseToggles: () -> Unit = {},
    /** Opens Customise: on a full page, this is the easy way there. */
    onCustomize: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    val title = when (item) {
        is AppCell -> appsByKey[item.key]?.label ?: item.packageName
        is FolderCell -> item.title
        is WidgetCell -> Widgets.byType(item.type)?.title ?: "Widget"
    }

    ZenithSheet(visible = visible, onDismiss = onDismiss, title = title) {
        when (item) {
            is AppCell -> {
                MenuRow(Icons.Rounded.OpenInFull, "Resize and move", "Pull its corner to any size, or move it a cell at a time") { onResize() }
                MenuRow(Icons.AutoMirrored.Rounded.OpenInNew, "Open") { onOpen(item.key); onDismiss() }
                MenuRow(Icons.Rounded.Info, "App info", "Storage, permissions and notifications, in the phone's settings") { onAppInfo(item.key); onDismiss() }
                MenuRow(Icons.Rounded.Home, "Remove from home", "The app stays installed, and in the drawer", danger = true) { onRemove(); onDismiss() }
                MenuRow(Icons.Rounded.Delete, "Uninstall", "Takes the app off the phone", danger = true) { onUninstall(item.key); onDismiss() }
            }

            is FolderCell -> {
                MenuRow(Icons.Rounded.Edit, "Rename folder") { onRename(item.id); }
                MenuRow(Icons.Rounded.Home, "Remove from home", "Its apps stay installed, and in the drawer", danger = true) { onRemove(); onDismiss() }
            }

            is WidgetCell -> {
                if (Widgets.baseType(item.type) == "toggles") {
                    MenuRow(Icons.Rounded.Tune, "Choose buttons", "Pick which controls it shows") { onChooseToggles() }
                }
                MenuRow(Icons.Rounded.OpenInFull, "Resize and move", "Pull its corner, pick a size, or move it a cell at a time") { onResize() }
                MenuRow(Icons.Rounded.Home, "Remove widget", "It can be added again from Widgets", danger = true) { onRemove(); onDismiss() }
            }
        }
        MenuRow(Icons.Rounded.Tune, "Customise home screen", "Widgets, wallpaper and settings") { onCustomize() }
    }
}

@Composable
fun CustomizeSheet(
    visible: Boolean,
    onWallpaper: () -> Unit,
    onHelp: () -> Unit,
    onAmbient: () -> Unit,
    onWidgets: () -> Unit,
    onSettings: () -> Unit,
    onDefaultLauncher: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val isHome = remember(visible) {
        context.getSystemService(android.app.role.RoleManager::class.java)
            ?.isRoleHeld(android.app.role.RoleManager.ROLE_HOME) == true
    }
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Customise") {
        // The four things people come here for, as big targets; the rest underneath.
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile(Icons.Rounded.Dashboard, "Widgets", "Add a clock, calendar, note, battery or toggles", index = 0, modifier = Modifier.weight(1f)) { onWidgets() }
            QuickTile(Icons.Rounded.Image, "Wallpaper", "Zenith's own designs, or a photo of yours", index = 1, modifier = Modifier.weight(1f)) { onWallpaper() }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile(Icons.Rounded.Tune, "Settings", "Colours, grid, glass, motion and backup", index = 2, modifier = Modifier.weight(1f)) { onSettings() }
            QuickTile(Icons.Rounded.Bedtime, "Bedside clock", "A dim full-screen clock for the night", index = 3, modifier = Modifier.weight(1f)) { onAmbient() }
        }
        Spacer(Modifier.height(8.dp))
        if (!isHome) {
            MenuRow(Icons.Rounded.Home, "Make Zenith your home screen", "So the home button always comes back here", index = 4) { onDefaultLauncher(); onDismiss() }
        }
        MenuRow(Icons.Rounded.HelpOutline, "How it works", "Every gesture in one page", index = 5) { onHelp() }
    }
}

/**
 * A big square-ish button: the icon in the corner, the name underneath with a line saying what is
 * behind it, lit with the accent.
 */
@Composable
private fun QuickTile(icon: ImageVector, label: String, detail: String, index: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Column(
        modifier
            .height(128.dp)
            .appearIn(delayMillis = staggerDelay(index), riseFrom = 18f)
            .pressBounce(0.95f)
            .clip(RoundedCornerShape(22.dp))
            .background(palette.onGlass.copy(alpha = 0.07f))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(palette.accent.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = palette.accent, modifier = Modifier.size(20.dp))
        }
        Column {
            Text(label, color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(detail, color = palette.onGlassMuted, fontSize = 12.sp, lineHeight = 16.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
fun HelpSheet(visible: Boolean, onDismiss: () -> Unit) {
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "How it works") {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TipList()
        }
    }
}
