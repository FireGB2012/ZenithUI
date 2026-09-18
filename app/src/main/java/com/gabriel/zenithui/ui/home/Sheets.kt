package com.gabriel.zenithui.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.VisibilityOff
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
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.FolderCell
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.IconPackInfo
import com.gabriel.zenithui.data.PlusStatus
import com.gabriel.zenithui.data.IconPacks
import com.gabriel.zenithui.data.GridDensity
import com.gabriel.zenithui.data.IconShapes
import com.gabriel.zenithui.data.AppCell
import com.gabriel.zenithui.data.WidgetCell
import com.gabriel.zenithui.data.ROW_UNITS
import com.gabriel.zenithui.data.Widgets
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.common.SectionLabel
import com.gabriel.zenithui.ui.onboarding.Segmented
import com.gabriel.zenithui.ui.onboarding.TipList
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.Palettes
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * maxHeightFraction

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(260)),
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
            enter = slideInVertically(Motion.smooth()) { it } + fadeIn(tween(220)),
            exit = slideOutVertically(tween(320)) { it } + fadeOut(tween(260)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .padding(10.dp)
                    // A tinted floor under the glass, so busy icons behind the sheet do not show
                    // through its lower rows.
                    .clip(RoundedCornerShape(34.dp))
                    .background(palette.backdrop.last().copy(alpha = 0.85f))
                    .glass(RoundedCornerShape(34.dp), strength = 1.2f, solid = 1f)
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
    /** "normal", "wide" (two columns) or "tall" (two rows). */
    onIconSize: (size: String) -> Unit,
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
                SectionLabel("Icon size")
                Segmented(
                    options = listOf("normal" to "1×1", "wide" to "Wide 2×1", "tall" to "Tall 1×2"),
                    selected = when {
                        item.spanX >= 2 -> "wide"
                        item.spanY > ROW_UNITS -> "tall"
                        else -> "normal"
                    },
                    // The sheet stays open, so you see the icon change behind it and can switch back.
                    onSelect = onIconSize,
                )
                Spacer(Modifier.height(10.dp))
                MenuRow(Icons.AutoMirrored.Rounded.OpenInNew, "Open") { onOpen(item.key); onDismiss() }
                MenuRow(Icons.Rounded.Info, "App info") { onAppInfo(item.key); onDismiss() }
                MenuRow(Icons.Rounded.Home, "Remove from home", danger = true) { onRemove(); onDismiss() }
                MenuRow(Icons.Rounded.Delete, "Uninstall", danger = true) { onUninstall(item.key); onDismiss() }
            }

            is FolderCell -> {
                MenuRow(Icons.Rounded.Edit, "Rename folder") { onRename(item.id); }
                MenuRow(Icons.Rounded.Home, "Remove from home", danger = true) { onRemove(); onDismiss() }
            }

            is WidgetCell -> {
                if (item.type == "toggles") {
                    MenuRow(Icons.Rounded.Tune, "Choose buttons", "Pick which controls it shows") { onChooseToggles() }
                }
                MenuRow(Icons.Rounded.OpenInFull, "Resize", "Drag the handles to change its size") { onResize() }
                MenuRow(Icons.Rounded.Home, "Remove widget", danger = true) { onRemove(); onDismiss() }
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
            QuickTile(Icons.Rounded.Dashboard, "Widgets", index = 0, modifier = Modifier.weight(1f)) { onWidgets() }
            QuickTile(Icons.Rounded.Image, "Wallpaper", index = 1, modifier = Modifier.weight(1f)) { onWallpaper() }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile(Icons.Rounded.Tune, "Settings", index = 2, modifier = Modifier.weight(1f)) { onSettings() }
            QuickTile(Icons.Rounded.Bedtime, "Bedside clock", index = 3, modifier = Modifier.weight(1f)) { onAmbient() }
        }
        Spacer(Modifier.height(8.dp))
        if (!isHome) {
            MenuRow(Icons.Rounded.Home, "Make Zenith your home screen", index = 4) { onDefaultLauncher(); onDismiss() }
        }
        MenuRow(Icons.Rounded.HelpOutline, "How it works", "Every gesture in one page", index = 5) { onHelp() }
    }
}

/** A big square-ish button: the icon in the corner, the name underneath, lit with the accent. */
@Composable
private fun QuickTile(icon: ImageVector, label: String, index: Int, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Column(
        modifier
            .height(96.dp)
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
        Text(label, color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.Medium)
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

@Composable
fun WidgetPickerSheet(
    visible: Boolean,
    onAdd: (String) -> Boolean,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    var message by remember { mutableStateOf<String?>(null) }

    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Add a widget") {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Widgets.all.forEachIndexed { index, spec ->
                MenuRow(
                    icon = Icons.Rounded.Add,
                    label = spec.title,
                    subtitle = spec.description,
                    index = index,
                ) {
                    message = if (onAdd(spec.type)) null else "No room left — free up a cell first."
                    if (message == null) onDismiss()
                }
            }
            message?.let {
                Text(it, color = palette.onGlassMuted, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
            }
        }
    }
}

@Composable
fun FolderSheet(
    visible: Boolean,
    folder: FolderCell,
    appsByKey: Map<String, AppInfo>,
    settings: ZenithSettings,
    onOpen: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    var title by remember(folder.id) { mutableStateOf(folder.title) }
    // Saving on every keystroke rewrites the whole layout as you type; wait for a pause.
    LaunchedEffect(title) {
        kotlinx.coroutines.delay(400)
        if (title.isNotBlank() && title != folder.title) onRename(title)
    }

    ZenithSheet(visible = visible, onDismiss = onDismiss) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            BasicTextField(
                value = title,
                onValueChange = { title = it.replace("\n", "") },
                singleLine = true,
                textStyle = TextStyle(color = palette.onGlass, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                cursorBrush = SolidColor(palette.accent),
                modifier = Modifier.weight(1f),
            )
            Text("${folder.keys.size} apps", color = palette.onGlassMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(settings.columns),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.heightIn(max = 340.dp),
        ) {
            items(folder.keys, key = { it }) { key ->
                Box(
                    Modifier.pointerInput(key) {
                        detectTapGestures(
                            onTap = { onOpen(key); onDismiss() },
                            onLongPress = { onRemoveApp(key) },
                        )
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    AppTile(
                        key = key,
                        label = appsByKey[key]?.label ?: "",
                        iconSize = STANDARD_ICON_SIZE,
                        showLabel = true,
                        shape = ZenithShapes.forIconShape(settings.iconShape),
                    )
                }
            }
        }
        Text(
            "Long press an app to move it back onto the home screen.",
            color = palette.onGlassMuted,
            fontSize = 11.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
