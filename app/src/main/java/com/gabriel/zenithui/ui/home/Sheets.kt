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
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalPalette.current
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.72f

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
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Customise") {
        MenuRow(Icons.Rounded.Image, "Wallpaper", "Pick a new backdrop", index = 0) { onWallpaper(); onDismiss() }
        MenuRow(
            icon = Icons.Rounded.Bedtime,
            label = "Bedside clock",
            subtitle = "Dimmed clock with today's calendar",
            index = 1,
        ) { onAmbient() }
        MenuRow(Icons.Rounded.Dashboard, "Widgets", "Add a Zenith widget to this page") { onWidgets() }
        MenuRow(Icons.Rounded.Tune, "Zenith settings", "Grid, glass, icons and themes") { onSettings() }
        MenuRow(Icons.Rounded.Home, "Set as default launcher") { onDefaultLauncher(); onDismiss() }
        MenuRow(Icons.Rounded.HelpOutline, "How it works", "Every gesture in one page") { onHelp() }
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
                        iconSize = settings.iconSize.dp,
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

@Composable
fun SettingsSheet(
    visible: Boolean,
    settings: ZenithSettings,
    plus: PlusStatus,
    onChange: ((ZenithSettings) -> ZenithSettings) -> Unit,
    onAura: () -> Unit,
    onFreeform: (Boolean) -> Unit,
    onThemeMaker: () -> Unit,
    onHiddenApps: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onPlus: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val packs by produceState(initialValue = emptyList<IconPackInfo>()) {
        value = withContext(Dispatchers.IO) { IconPacks.installed(context) }
    }

    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Zenith settings") {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionLabel("Palette")
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScrollRow(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Palettes.all.forEach { option ->
                    val selected = option.id == settings.paletteId
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val swatch by animateDpAsState(
                            targetValue = if (selected) 46.dp else 40.dp,
                            animationSpec = Motion.bouncy(),
                            label = "swatch",
                        )
                        Box(
                            Modifier
                                .size(swatch)
                                .pressBounce(0.9f)
                                .clip(RoundedCornerShape(percent = 50))
                                .background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        listOf(option.accent, option.accentAlt)
                                    )
                                )
                                .clickable { onChange { it.copy(paletteId = option.id) } }
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            option.name,
                            color = if (selected) palette.onGlass else palette.onGlassMuted,
                            fontSize = 10.sp,
                        )
                    }
                }
            }

            if (settings.paletteId == Palettes.CUSTOM) {
                Text(
                    "Using your own theme. Edit it under Zenith Plus below.",
                    color = palette.onGlassMuted,
                    fontSize = 12.sp,
                )
            }

            SectionLabel("Appearance")
            Segmented(
                options = listOf("auto" to "Auto", "light" to "Light", "dark" to "Dark"),
                selected = settings.appearance,
                onSelect = { value -> onChange { it.copy(appearance = value) } },
            )

            LabeledSlider(
                label = "Glass",
                value = settings.glass,
                range = 0.2f..1.2f,
                display = "${(settings.glass * 100).toInt()}%",
            ) { value -> onChange { it.copy(glass = value) } }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tilt shine", color = palette.onGlass, fontSize = 15.sp)
                    Text(
                        "Light slides across the glass as you move the phone.",
                        color = palette.onGlassMuted,
                        fontSize = 12.sp,
                    )
                }
                Switch(
                    checked = settings.glassShine,
                    onCheckedChange = { value -> onChange { it.copy(glassShine = value) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = palette.accent,
                    ),
                )
            }

            Text("Opening apps", color = palette.onGlass, fontSize = 15.sp)
            Segmented(
                options = listOf("colour" to "Colour", "glass" to "Liquid glass"),
                selected = settings.openAnimation,
                onSelect = { value -> onChange { it.copy(openAnimation = value) } },
            )
            Text(
                text = if (settings.openAnimation == "glass") {
                    "The icon grows into grey glass over a frosted home screen."
                } else {
                    "The icon grows into a card in its own colour."
                },
                color = palette.onGlassMuted,
                fontSize = 12.sp,
            )

            SectionLabel("Grid")
            Segmented(
                options = GridDensity.entries.map { it.id to it.label },
                selected = settings.gridDensity,
                onSelect = { value -> onChange { it.copy(gridDensity = value) } },
            )
            Text(
                // How many cells that works out to depends on the screen, so it is reported
                // rather than chosen.
                text = "${settings.columns} columns · ${settings.rows} rows on this screen",
                color = palette.onGlassMuted,
                fontSize = 12.sp,
            )

            LabeledSlider(
                label = "Icon size",
                value = settings.iconSize.toFloat(),
                range = 40f..76f,
                display = "${settings.iconSize} dp",
            ) { value -> onChange { it.copy(iconSize = value.toInt()) } }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Show labels", color = palette.onGlass, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Switch(
                    checked = settings.showLabels,
                    onCheckedChange = { value -> onChange { it.copy(showLabels = value) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = palette.accent,
                    ),
                )
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Show dock", color = palette.onGlass, fontSize = 15.sp)
                    Text(
                        "The bar of apps along the bottom. Its apps are kept while it is hidden.",
                        color = palette.onGlassMuted,
                        fontSize = 12.sp,
                    )
                }
                Switch(
                    checked = settings.showDock,
                    onCheckedChange = { value -> onChange { it.copy(showDock = value) } },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = palette.accent,
                    ),
                )
            }

            SectionLabel("Swipe up for")
            Segmented(
                options = listOf("drawer" to "App drawer", "library" to "App Library"),
                selected = settings.appsView,
                onSelect = { value -> onChange { it.copy(appsView = value) } },
            )
            Text(
                text = if (settings.appsView == "library") {
                    "Swipe left past your last page. Suggestions, recently added, then folders by kind."
                } else {
                    "Every app in one list, A to Z."
                },
                color = palette.onGlassMuted,
                fontSize = 12.sp,
            )

            SectionLabel("Icon shape")
            Segmented(
                options = IconShapes.ids.map { it to IconShapes.label(it) },
                selected = settings.iconShape,
                onSelect = { value -> onChange { it.copy(iconShape = value) } },
            )

            SectionLabel("Icon pack")
            IconPackRow(
                label = "System icons",
                selected = settings.iconPack.isEmpty(),
                onClick = { onChange { it.copy(iconPack = "") } },
            )
            packs.forEach { pack ->
                IconPackRow(
                    label = pack.label,
                    selected = settings.iconPack == pack.packageName,
                    onClick = { onChange { it.copy(iconPack = pack.packageName) } },
                )
            }
            if (packs.isEmpty()) {
                Text(
                    "No icon packs installed. Install any Nova or ADW compatible pack and it shows up here.",
                    color = palette.onGlassMuted,
                    fontSize = 12.sp,
                )
            }

            SectionLabel("Aura")
            MenuRow(
                icon = Icons.Rounded.LocalFireDepartment,
                label = "Aura and themes",
                subtitle = if (settings.streak > 0) {
                    "${settings.aura} aura · ${settings.streak} day streak"
                } else {
                    "${settings.aura} aura · open daily to build a streak"
                },
            ) { onAura() }

            SectionLabel("Zenith Plus")
            MenuRow(
                icon = Icons.Rounded.AutoAwesome,
                label = if (plus.purchased) "Zenith Plus" else if (plus.inTrial) "Zenith Plus — free trial" else "Zenith Plus",
                subtitle = when {
                    plus.lifetime -> "Unlocked for good. Thank you."
                    plus.subscribed -> "Subscribed, ${plus.monthlyPrice} a month"
                    plus.trialDaysLeft > 1 -> "${plus.trialDaysLeft} days left of your free trial"
                    plus.trialDaysLeft == 1 -> "Last day of your free trial"
                    else -> "${plus.monthlyPrice} a month or ${plus.lifetimePrice} for good"
                },
            ) { onPlus() }
            MenuRow(
                icon = if (plus.active) Icons.Rounded.Palette else Icons.Rounded.Lock,
                label = "Theme maker",
                subtitle = "Build a palette of your own",
            ) { onThemeMaker() }
            MenuRow(
                icon = if (plus.active) Icons.Rounded.VisibilityOff else Icons.Rounded.Lock,
                label = "Hidden apps",
                subtitle = if (settings.hiddenApps.isEmpty()) {
                    "Keep chosen apps out of the drawer"
                } else {
                    "${settings.hiddenApps.size} hidden"
                },
            ) { onHiddenApps() }
            MenuRow(
                icon = if (plus.active) Icons.Rounded.Save else Icons.Rounded.Lock,
                label = "Back up home screen",
                subtitle = "Save everything to a file",
            ) { onBackup() }
            MenuRow(
                icon = if (plus.active) Icons.Rounded.Restore else Icons.Rounded.Lock,
                label = "Restore from a backup",
                subtitle = "Replaces your current layout",
            ) { onRestore() }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Free placement", color = palette.onGlass, fontSize = 15.sp)
                    Text(
                        text = if (plus.active) {
                            "Put apps anywhere and size widgets however you like."
                        } else {
                            "Plus: put apps anywhere, any widget size."
                        },
                        color = palette.onGlassMuted,
                        fontSize = 12.sp,
                    )
                }
                Switch(
                    // Shown off without Plus even if it was left on, since it is not in effect.
                    checked = settings.freeform && plus.active,
                    onCheckedChange = onFreeform,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = palette.accent,
                    ),
                )
            }

            SectionLabel("Device")
            // Android may close any app in the background, a launcher included; then coming home
            // has to start Zenith again. Exempting it from battery optimisation stops that.
            val powerManager = remember { context.getSystemService(android.os.PowerManager::class.java) }
            val keptLoaded = powerManager?.isIgnoringBatteryOptimizations(context.packageName) == true
            MenuRow(
                icon = Icons.Rounded.RestartAlt,
                label = "Keep Zenith loaded",
                subtitle = if (keptLoaded) {
                    "On. Android will not put Zenith to sleep."
                } else {
                    "Off. Open Battery and choose Unrestricted."
                },
            ) {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
            MenuRow(Icons.Rounded.Home, "Set as default launcher") {
                runCatching {
                    context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
            MenuRow(Icons.Rounded.Palette, "Change wallpaper") {
                runCatching {
                    context.startActivity(
                        Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
            MenuRow(Icons.Rounded.RestartAlt, "Run first-time setup again", danger = true) {
                onReset()
                onDismiss()
            }
        }
    }
}

@Composable
private fun IconPackRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val dot by animateColorAsState(
            targetValue = if (selected) palette.accent else palette.onGlass.copy(alpha = 0.12f),
            animationSpec = tween(320),
            label = "packDot",
        )
        val dotSize by animateDpAsState(if (selected) 18.dp else 14.dp, Motion.snappy(), label = "packDotSize")
        Box(
            Modifier
                .size(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(dotSize)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(dot)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(label, color = palette.onGlass, fontSize = 14.sp)
    }
}

@Composable
private fun LabeledSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    display: String,
    onChange: (Float) -> Unit,
) {
    val palette = LocalPalette.current
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = palette.onGlass, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(display, color = palette.onGlassMuted, fontSize = 12.sp)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = palette.accent,
                activeTrackColor = palette.accent,
                inactiveTrackColor = palette.onGlass.copy(alpha = 0.18f),
            ),
        )
    }
}

@Composable
private fun Modifier.horizontalScrollRow(): Modifier =
    this.then(Modifier.horizontalScroll(rememberScrollState()))
