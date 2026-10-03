package com.gabriel.zenithui.ui.home

import android.app.role.RoleManager
import android.content.Intent
import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.GridDensity
import com.gabriel.zenithui.data.HomeHeader
import com.gabriel.zenithui.data.IconPackInfo
import com.gabriel.zenithui.data.IconPacks
import com.gabriel.zenithui.data.IconShapes
import com.gabriel.zenithui.data.PlusStatus
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.onboarding.Segmented
import com.gabriel.zenithui.ui.settings.ActionRow
import com.gabriel.zenithui.ui.settings.Caption
import com.gabriel.zenithui.ui.settings.ChoiceRow
import com.gabriel.zenithui.ui.settings.Group
import com.gabriel.zenithui.ui.settings.GroupDivider
import com.gabriel.zenithui.ui.settings.NavRow
import com.gabriel.zenithui.ui.settings.PaletteGrid
import com.gabriel.zenithui.ui.settings.PillSlider
import com.gabriel.zenithui.ui.settings.SettingsHeader
import com.gabriel.zenithui.ui.settings.ShapeChoice
import com.gabriel.zenithui.ui.settings.ToggleRow
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.Palettes
import com.gabriel.zenithui.ui.theme.ZenithPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private enum class Page(val title: String) {
    Root("Settings"),
    Look("Look"),
    Home("Home screen"),
    Plus("Zenith Plus"),
    Phone("Phone"),
}

/**
 * Settings as a few short pages instead of one long scroll. The first page is a table of contents
 * that also tells you how things are set right now, so you rarely need to open a page to find out.
 * Pages slide in from the side the way you go, and back (the arrow, a swipe down, or the system
 * back) always climbs one level before it closes anything.
 */
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
    onWallpaper: () -> Unit,
    onDismiss: () -> Unit,
) {
    var page by remember { mutableStateOf(Page.Root) }
    // Opening settings again always starts at the top.
    LaunchedEffect(visible) { if (visible) page = Page.Root }
    BackHandler(enabled = visible && page != Page.Root) { page = Page.Root }

    ZenithSheet(visible = visible, onDismiss = onDismiss, maxHeightFraction = 0.86f) {
        SettingsHeader(
            title = page.title,
            canGoBack = page != Page.Root,
            onBack = { page = Page.Root },
        )
        Spacer(Modifier.height(10.dp))
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                val deeper = targetState != Page.Root
                val slide = spring(dampingRatio = 1f, stiffness = 420f, visibilityThreshold = androidx.compose.ui.unit.IntOffset(1, 1))
                (fadeIn(tween(220, delayMillis = 40)) +
                    slideInHorizontally(slide) { (it * (if (deeper) 0.22f else -0.22f)).toInt() })
                    .togetherWith(
                        fadeOut(tween(120)) +
                            slideOutHorizontally(slide) { (it * (if (deeper) -0.16f else 0.16f)).toInt() }
                    )
                    // The sheet grows and shrinks to fit the page, on a spring, rather than jumping.
                    .using(SizeTransform(clip = false) { _, _ -> spring(dampingRatio = 1f, stiffness = 380f, visibilityThreshold = IntSize(1, 1)) })
            },
            label = "settingsPage",
        ) { current ->
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (current) {
                    Page.Root -> RootPage(settings, plus, onOpen = { page = it }, onAura = onAura)
                    Page.Look -> LookPage(settings, onChange)
                    Page.Home -> HomePageSettings(settings, onChange)
                    Page.Plus -> PlusPage(settings, plus, onFreeform, onThemeMaker, onHiddenApps, onBackup, onRestore, onPlus)
                    Page.Phone -> PhonePage(onReset = { onReset(); onDismiss() }, onWallpaper = onWallpaper)
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

// -------------------------------------------------------------------------------------- pages

@Composable
private fun RootPage(settings: ZenithSettings, plus: PlusStatus, onOpen: (Page) -> Unit, onAura: () -> Unit) {
    val appearance = when (settings.appearance) {
        "light" -> "light"
        "dark" -> "dark"
        else -> "follows the phone"
    }
    val context = LocalContext.current
    val isHome = remember { context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME) == true }

    Group {
        NavRow(
            icon = Icons.Rounded.Palette,
            tile = listOf(Color(0xFF8B7CFF), Color(0xFF5B8CFF)),
            title = "Look",
            summary = "${Palettes.resolve(settings).name}, $appearance",
            onClick = { onOpen(Page.Look) },
        )
        GroupDivider()
        NavRow(
            icon = Icons.Rounded.GridView,
            tile = listOf(Color(0xFF2FC9A6), Color(0xFF1C9C8B)),
            title = "Home screen",
            summary = "${settings.columns} × ${settings.rows}, ${IconShapes.label(settings.iconShape).lowercase()} icons",
            onClick = { onOpen(Page.Home) },
        )
    }
    Group {
        NavRow(
            icon = Icons.Rounded.LocalFireDepartment,
            tile = listOf(Color(0xFFFF9F5A), Color(0xFFFF6A4D)),
            title = "Aura",
            summary = if (settings.streak > 0) {
                "${settings.aura} aura, ${settings.streak} day streak"
            } else {
                "${settings.aura} aura"
            },
            onClick = onAura,
        )
        GroupDivider()
        NavRow(
            icon = Icons.Rounded.AutoAwesome,
            tile = listOf(Color(0xFFFFD36B), Color(0xFFF5A623)),
            title = "Zenith Plus",
            summary = when {
                plus.lifetime -> "Yours for good"
                plus.subscribed -> "Subscribed"
                plus.trialDaysLeft > 1 -> "${plus.trialDaysLeft} days of your trial left"
                plus.trialDaysLeft == 1 -> "Last day of your trial"
                else -> "Theme maker, hidden apps, backup"
            },
            onClick = { onOpen(Page.Plus) },
        )
    }
    Group {
        NavRow(
            icon = Icons.Rounded.PhoneAndroid,
            tile = listOf(Color(0xFF9AA3B2), Color(0xFF6B7384)),
            title = "Phone",
            summary = if (isHome) "Zenith is your home screen" else "Zenith is not your home screen yet",
            onClick = { onOpen(Page.Phone) },
        )
    }
}

@Composable
private fun LookPage(settings: ZenithSettings, onChange: ((ZenithSettings) -> ZenithSettings) -> Unit) {
    val palette = LocalPalette.current
    // Every palette you can use right now: the six everyone has, the ones bought with aura, and
    // your own while it is the one in use.
    val options = remember(settings.unlockedThemes, settings.paletteId) {
        buildList {
            addAll(Palettes.all)
            addAll(Palettes.aura.filter { it.id in settings.unlockedThemes })
            if (settings.paletteId == Palettes.CUSTOM) add(Palettes.custom(settings))
        }
    }
    Group(padding = 14.dp) {
        Caption("Colour")
        Spacer(Modifier.height(10.dp))
        PaletteGrid(options, selectedId = settings.paletteId, onSelect = { option -> onChange { it.copy(paletteId = option.id) } })
        Spacer(Modifier.height(14.dp))
        Segmented(
            options = listOf("auto" to "Auto", "light" to "Light", "dark" to "Dark"),
            selected = settings.appearance,
            onSelect = { value -> onChange { it.copy(appearance = value) } },
        )
    }
    Group {
        ToggleRow(
            title = "Wallpaper follows theme",
            subtitle = if (settings.wallpaperStyle.isEmpty()) {
                "Works with Zenith's own wallpapers; pick one under Phone"
            } else {
                "Repaints in the palette above, light or dark"
            },
            checked = settings.wallpaperFollow,
        ) { value -> onChange { it.copy(wallpaperFollow = value) } }
    }
    Group(padding = 14.dp) {
        PillSlider(
            label = "Glass",
            value = settings.glass,
            range = 0.2f..1.2f,
            display = "${(settings.glass * 100).toInt()}%",
        ) { value -> onChange { it.copy(glass = value) } }
    }
    Group {
        ToggleRow(
            title = "Tilt shine",
            subtitle = "Light moves across the glass as you tilt the phone",
            checked = settings.glassShine,
        ) { value -> onChange { it.copy(glassShine = value) } }
    }
    Group(padding = 14.dp) {
        Caption("Opening apps")
        Spacer(Modifier.height(10.dp))
        Segmented(
            options = listOf("colour" to "Colour", "glass" to "Liquid glass"),
            selected = settings.openAnimation,
            onSelect = { value -> onChange { it.copy(openAnimation = value) } },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (settings.openAnimation == "glass") {
                "The icon grows into grey glass over a frosted home screen."
            } else {
                "The icon grows into a card in its own colour."
            },
            color = palette.onGlassMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}

@Composable
private fun HomePageSettings(settings: ZenithSettings, onChange: ((ZenithSettings) -> ZenithSettings) -> Unit) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val packs by produceState(initialValue = emptyList<IconPackInfo>()) {
        value = withContext(Dispatchers.IO) { IconPacks.installed(context) }
    }

    Group(padding = 14.dp) {
        Caption("Grid")
        Spacer(Modifier.height(10.dp))
        Segmented(
            options = GridDensity.entries.map { it.id to it.label },
            selected = settings.gridDensity,
            onSelect = { value -> onChange { it.copy(gridDensity = value) } },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            // How many cells that works out to depends on the screen, so it is reported, not chosen.
            text = "${settings.columns} across, ${settings.rows} down on this screen. Icons size themselves to fit.",
            color = palette.onGlassMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }

    Group(padding = 14.dp) {
        Caption("Icon shape")
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconShapes.ids.forEach { id ->
                ShapeChoice(id = id, selected = id == settings.iconShape) {
                    onChange { it.copy(iconShape = id) }
                }
            }
        }
    }

    Group {
        ToggleRow("Labels", "App names under the icons", settings.showLabels) { value ->
            onChange { it.copy(showLabels = value) }
        }
        GroupDivider()
        ToggleRow("Dock", "Its apps are kept while it is hidden", settings.showDock) { value ->
            onChange { it.copy(showDock = value) }
        }
        GroupDivider()
        ToggleRow("Search bar", "Above the dock, for your apps and the web", settings.searchBar) { value ->
            onChange { it.copy(searchBar = value) }
        }
    }

    Group(padding = 14.dp) {
        Caption("Top of the page")
        Spacer(Modifier.height(10.dp))
        Segmented(
            options = listOf(HomeHeader.NAME to "Name", HomeHeader.GREETING to "Greeting", HomeHeader.OFF to "Nothing"),
            selected = settings.header,
            onSelect = { value -> onChange { it.copy(header = value) } },
        )
    }

    Group(padding = 14.dp) {
        Caption("Swipe up for")
        Spacer(Modifier.height(10.dp))
        Segmented(
            options = listOf("drawer" to "App drawer", "library" to "App Library"),
            selected = settings.appsView,
            onSelect = { value -> onChange { it.copy(appsView = value) } },
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (settings.appsView == "library") {
                "Swipe left past your last page. Suggestions, recently added, then folders by kind."
            } else {
                "Every app in one list, A to Z."
            },
            color = palette.onGlassMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }

    Group {
        Caption("Icon pack", Modifier.padding(start = 16.dp, top = 14.dp, bottom = 4.dp))
        ChoiceRow("System icons", selected = settings.iconPack.isEmpty()) { onChange { it.copy(iconPack = "") } }
        packs.forEach { pack ->
            GroupDivider()
            ChoiceRow(pack.label, selected = settings.iconPack == pack.packageName) {
                onChange { it.copy(iconPack = pack.packageName) }
            }
        }
        if (packs.isEmpty()) {
            Text(
                "Install any Nova or ADW compatible icon pack and it shows up here.",
                color = palette.onGlassMuted,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
            )
        }
    }
}

@Composable
private fun PlusPage(
    settings: ZenithSettings,
    plus: PlusStatus,
    onFreeform: (Boolean) -> Unit,
    onThemeMaker: () -> Unit,
    onHiddenApps: () -> Unit,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onPlus: () -> Unit,
) {
    val palette = LocalPalette.current
    val locked = !plus.active
    Group {
        NavRow(
            icon = Icons.Rounded.AutoAwesome,
            tile = listOf(Color(0xFFFFD36B), Color(0xFFF5A623)),
            title = when {
                plus.lifetime -> "Plus is yours for good"
                plus.subscribed -> "Subscribed, ${plus.monthlyPrice} a month"
                plus.trialDaysLeft > 1 -> "${plus.trialDaysLeft} days of your free trial left"
                plus.trialDaysLeft == 1 -> "Last day of your free trial"
                else -> "Try Plus free for 7 days"
            },
            summary = if (plus.purchased) "Manage or restore purchases" else "${plus.monthlyPrice} a month or ${plus.lifetimePrice} once",
            onClick = onPlus,
        )
    }
    Group {
        ActionRow(
            icon = if (locked) Icons.Rounded.Lock else Icons.Rounded.Palette,
            title = "Theme maker",
            subtitle = "Build a palette of your own",
            onClick = onThemeMaker,
        )
        GroupDivider()
        ActionRow(
            icon = if (locked) Icons.Rounded.Lock else Icons.Rounded.VisibilityOff,
            title = "Hidden apps",
            subtitle = if (settings.hiddenApps.isEmpty()) "Keep chosen apps out of the drawer" else "${settings.hiddenApps.size} hidden",
            onClick = onHiddenApps,
        )
        GroupDivider()
        ToggleRow(
            title = "Free placement",
            subtitle = if (locked) "Plus: put apps anywhere, any widget size" else "Put apps anywhere, size widgets however you like",
            // Shown off without Plus even if it was left on, since it is not in effect.
            checked = settings.freeform && plus.active,
            onChange = onFreeform,
        )
    }
    Group {
        ActionRow(
            icon = if (locked) Icons.Rounded.Lock else Icons.Rounded.Save,
            title = "Back up home screen",
            subtitle = "Save everything to a file",
            onClick = onBackup,
        )
        GroupDivider()
        ActionRow(
            icon = if (locked) Icons.Rounded.Lock else Icons.Rounded.Restore,
            title = "Restore from a backup",
            subtitle = "Replaces your current layout",
            onClick = onRestore,
        )
    }
    if (locked) {
        Text(
            "Locked items open Zenith Plus.",
            color = palette.onGlassMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}

@Composable
private fun PhonePage(onReset: () -> Unit, onWallpaper: () -> Unit) {
    val context = LocalContext.current
    // Read fresh each time the page is shown, so coming back from Android's settings is up to date.
    val isHome = remember { context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME) == true }
    val keptLoaded = remember {
        context.getSystemService(android.os.PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    Group {
        ActionRow(
            icon = Icons.Rounded.Home,
            title = "Default home app",
            subtitle = if (isHome) "Zenith is your home screen" else "Tap to make Zenith your home screen",
            status = if (isHome) "On" else null,
        ) {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
        GroupDivider()
        // Android may close any app in the background, a launcher included; then coming home has
        // to start Zenith again. Exempting it from battery optimisation stops that.
        ActionRow(
            icon = Icons.Rounded.BatteryChargingFull,
            title = "Keep Zenith loaded",
            subtitle = if (keptLoaded) "Android will not put Zenith to sleep" else "Open Battery and choose Unrestricted",
            status = if (keptLoaded) "On" else null,
        ) {
            runCatching {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:${context.packageName}"))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
        }
        GroupDivider()
        ActionRow(icon = Icons.Rounded.Image, title = "Wallpaper", subtitle = "Zenith's own, or one of your photos", onClick = onWallpaper)
    }

    // Starting over wipes the home screen, so it asks twice. The second tap has to come within a
    // few seconds of the first, and the row says exactly what will happen before it happens.
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(armed) {
        if (armed) {
            delay(4_000)
            armed = false
        }
    }
    Group {
        ActionRow(
            icon = Icons.Rounded.RestartAlt,
            title = if (armed) "Tap again to start over" else "Run setup again",
            subtitle = if (armed) "Your layout and settings will be cleared" else "Start from the welcome screen",
            danger = true,
        ) {
            if (armed) onReset() else armed = true
        }
    }
}
