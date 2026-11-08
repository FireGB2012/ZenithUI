package com.gabriel.zenithui.ui.settings

import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.data.AnimationPreset
import com.gabriel.zenithui.data.Badges
import com.gabriel.zenithui.data.DayParts
import com.gabriel.zenithui.data.DrawerSort
import com.gabriel.zenithui.data.GlassBody
import com.gabriel.zenithui.data.GridDensity
import com.gabriel.zenithui.data.HomeHeader
import com.gabriel.zenithui.data.IconPackInfo
import com.gabriel.zenithui.data.IconPacks
import com.gabriel.zenithui.data.IconShapes
import com.gabriel.zenithui.data.IconStyles
import com.gabriel.zenithui.data.LabelSizes
import com.gabriel.zenithui.data.NotificationDots
import com.gabriel.zenithui.data.PlusStatus
import com.gabriel.zenithui.data.WallStyle
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.rememberHaptics
import com.gabriel.zenithui.ui.common.shake
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.onboarding.PhonePreview
import com.gabriel.zenithui.ui.onboarding.Segmented
import com.gabriel.zenithui.ui.theme.Palettes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// What is on each page of Settings, set out as an iPhone sets out its own: Sections of rows on
// solid cards, a choice's name over its card and what it means under it. Every option sits in a
// Spot named after its entry in the SettingsCatalog, which is how a search result finds its row;
// every block says how far down the page it is, which is the order the blocks rise in as the page
// arrives. A hairline between two rows starts where the words of the row above it start.

/**
 * Your own home screen, small: the same code that draws the real one, so a change made below is
 * seen here as it is made. A picture only; it takes no touches. It rises in first, and when
 * something about how the home screen looks is changed a band of light crosses it once, so the eye
 * goes to where the change can be seen.
 */
@Composable
private fun MiniPhone(settings: ZenithSettings, preview: SettingsPreview) {
    val reduced = LocalMotionLevel.current.reduced
    val sweep = remember { Animatable(1f) }
    val look = listOf(
        settings.paletteId, settings.appearance, settings.gridDensity, settings.columns, settings.rows,
        settings.iconShape, settings.iconPack, settings.iconStyle, settings.glassBody,
        settings.showLabels, settings.labelSize, settings.labelShadow, settings.showDock, settings.searchBar, settings.header,
    )
    val shown = remember { arrayOf<List<Any>?>(null) }
    LaunchedEffect(look) {
        // Not as the page arrives, only when something is changed while it is on show.
        val changed = shown[0] != null && shown[0] != look
        shown[0] = look
        if (changed && !reduced) {
            sweep.snapTo(0f)
            sweep.animateTo(1f, tween(720, easing = FastOutSlowInEasing))
        }
    }
    Box(Modifier.riseIn(0)) {
        PhonePreview(
            items = preview.items,
            dock = settings.dock,
            appsByKey = preview.appsByKey,
            settings = settings,
            wallpaper = preview.wallpaper,
            screenWidth = preview.screenWidth,
            screenHeight = preview.screenHeight,
            shown = Int.MAX_VALUE,
            sweep = { sweep.value },
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .padding(bottom = 6.dp),
        )
    }
}

@Composable
internal fun ThemeOptions(settings: ZenithSettings, plus: PlusStatus, preview: SettingsPreview, actions: SettingsActions) {
    val change = actions.change
    // Every palette you can use right now: the six everyone has, the ones bought with aura, your
    // own once you have made one, and the one in your wallpaper.
    val options = remember(
        settings.unlockedThemes, settings.paletteId, settings.customAccent, settings.customAccentAlt, settings.customBackdrop,
        settings.wallAccent, settings.wallAccentAlt, settings.wallBackdrop, settings.wallDark,
    ) {
        buildList {
            addAll(Palettes.all)
            addAll(Palettes.aura.filter { it.id in settings.unlockedThemes })
            if (settings.paletteId == Palettes.CUSTOM) add(Palettes.custom(settings))
            add(Palettes.fromWallpaper(settings))
        }
    }
    val design = WallStyle.byName(settings.wallpaperStyle)

    MiniPhone(settings, preview)
    Section(
        header = "Colour",
        footer = when {
            settings.paletteId != Palettes.WALLPAPER -> "Colours the switches, the buttons, the highlights and Zenith's own wallpapers."
            design != null -> "The colours of the last photo you had as wallpaper. With a photo up, they follow it."
            else -> "Taken from your wallpaper, and changed along with it."
        },
        rise = 1,
    ) {
        Spot("palette") {
            ControlRow {
                PaletteGrid(
                    options = options,
                    selectedId = settings.paletteId,
                    onSelect = { option -> change { it.copy(paletteId = option.id) } },
                    onMake = actions.themeMaker,
                )
            }
        }
        GroupDivider(inset = ROW_INSET)
        Spot("dayPalettes") {
            ActionRow(
                icon = if (plus.active) Icons.Rounded.WbTwilight else Icons.Rounded.Lock,
                title = "By time of day",
                subtitle = when {
                    !plus.active && settings.dayPalettes.isEmpty() -> "Plus: a palette for dawn, day, dusk and night"
                    settings.dayPalettes.isEmpty() -> "A palette for dawn, day, dusk and night"
                    !plus.active -> "Kept, but resting: it changes the palette while you have Plus"
                    else -> DayParts.all.filter { it.id in settings.dayPalettes }.joinToString(" · ") { part ->
                        part.label + " " + Palettes.resolve(settings.copy(paletteId = settings.dayPalettes.getValue(part.id))).name
                    }
                },
                onClick = actions.dayPalettes,
            )
        }
    }
    Section(
        header = "Light or dark",
        footer = when (settings.appearance) {
            "auto" -> "Follows the phone: light by day and dark at night, if the phone is set that way."
            "light" -> "Pale panels and dark words, whatever the phone is set to."
            else -> "Dark panels and white words, whatever the phone is set to."
        },
        rise = 2,
    ) {
        Spot("appearance") {
            ControlRow {
                Segmented(
                    options = listOf("auto" to "Auto", "light" to "Light", "dark" to "Dark"),
                    selected = settings.appearance,
                    onSelect = { value -> change { it.copy(appearance = value) } },
                )
            }
        }
    }
    Section(header = "Font", rise = 3) {
        Spot("font") {
            ControlRow {
                FontChoice(settings.font) { id -> change { it.copy(font = id) } }
            }
        }
    }
    Section(header = "Numbers", footer = "The clocks, the battery, the countdown and the weather.", rise = 4) {
        Spot("numberFont") {
            ControlRow {
                NumberFontChoice(settings.numberFont, settings.font) { id -> change { it.copy(numberFont = id) } }
            }
        }
    }
    Section(rise = 5) {
        Spot("wallpaper") {
            ActionRow(
                icon = Icons.Rounded.Image,
                title = "Wallpaper",
                subtitle = if (design != null) "${design.label}, one of Zenith's own" else "Zenith's own, or one of your photos",
                onClick = actions.wallpaper,
            )
        }
        GroupDivider()
        Spot("wallpaperFollow") {
            ToggleRow(
                title = "Wallpaper follows theme",
                subtitle = if (design == null) {
                    "Works with Zenith's own wallpapers"
                } else {
                    "${design.label} repaints in the colour above, light or dark"
                },
                checked = settings.wallpaperFollow,
            ) { value -> change { it.copy(wallpaperFollow = value) } }
        }
    }
    Section(footer = "How strongly panels bend and catch the light. Lower is flatter and calmer.", rise = 6) {
        Spot("glass") {
            ControlRow {
                PillSlider(
                    label = "Glass",
                    value = settings.glass,
                    range = 0.2f..1.2f,
                    display = "${(settings.glass * 100).toInt()}%",
                ) { value -> change { it.copy(glass = value) } }
            }
        }
    }
    Section(header = "Glass panels", footer = GlassBody.byId(settings.glassBody).detail, rise = 7) {
        Spot("glassBody") {
            ControlRow {
                Segmented(
                    options = GlassBody.entries.map { it.id to it.label },
                    selected = GlassBody.byId(settings.glassBody).id,
                    onSelect = { value -> change { it.copy(glassBody = value) } },
                )
            }
        }
    }
    Section(rise = 8) {
        Spot("tiltShine") {
            ToggleRow(
                title = "Tilt shine",
                subtitle = "Light moves across the glass as you tilt the phone",
                checked = settings.glassShine,
            ) { value -> change { it.copy(glassShine = value) } }
        }
        GroupDivider(inset = ROW_INSET)
        Spot("glassStudio") {
            ActionRow(
                icon = if (plus.active) Icons.Rounded.Tune else Icons.Rounded.Lock,
                title = "Glass studio",
                subtitle = when {
                    !plus.active && settings.glassTuning.isDefault -> "Plus: bend, frost, rim light and where the light comes from"
                    settings.glassTuning.isDefault -> "Bend, frost, rim light and where the light comes from"
                    !plus.active -> "Kept, but resting: your glass comes back with Plus"
                    else -> "Your own glass. Reset puts Zenith's back"
                },
                onClick = actions.glassStudio,
            )
        }
    }
}

@Composable
internal fun HomeOptions(settings: ZenithSettings, plus: PlusStatus, preview: SettingsPreview, actions: SettingsActions) {
    val change = actions.change
    val context = LocalContext.current
    val reduced = LocalMotionLevel.current.reduced
    val packs by produceState(initialValue = emptyList<IconPackInfo>()) {
        value = withContext(Dispatchers.IO) { IconPacks.installed(context) }
    }
    /** How many items the last tidy moved; null until it has been run on this visit. */
    var tidied by remember { mutableStateOf<Int?>(null) }
    /** How many times it has been run: the wand turns once for each. */
    var tidies by remember { mutableIntStateOf(0) }
    val custom = settings.gridDensity == GridDensity.CUSTOM
    // For a preset, how many cells that works out to depends on the screen, so it is reported,
    // not chosen.
    val counts = "${settings.columns} across, ${settings.rows} down"
    val allowed = rememberNotificationAccess()
    val dots = Badges.of(settings.badges)

    MiniPhone(settings, preview)
    Section(
        header = "Grid",
        footer = when {
            custom -> "Your own grid: $counts. Icons size themselves to fit, and anything left without room moves to the closest free spot."
            settings.gridDensity == GridDensity.Compact.id -> "Smaller cells, so more fits on a page: $counts on this screen."
            settings.gridDensity == GridDensity.Roomy.id -> "Larger cells with more room round each icon: $counts on this screen."
            else -> "$counts on this screen. Icons size themselves to fit."
        },
        rise = 1,
    ) {
        Spot("grid") {
            ControlRow {
                Segmented(
                    options = GridDensity.entries.map { it.id to it.label } + (GridDensity.CUSTOM to "Custom"),
                    selected = if (custom) GridDensity.CUSTOM else GridDensity.byId(settings.gridDensity).id,
                    onSelect = { value ->
                        change {
                            // A grid of your own starts as the grid you have, so choosing Custom
                            // moves nothing until a number is changed.
                            if (value == GridDensity.CUSTOM && (it.gridColumns <= 0 || it.gridRows <= 0)) {
                                it.copy(gridDensity = value, gridColumns = it.columns, gridRows = it.rows)
                            } else {
                                it.copy(gridDensity = value)
                            }
                        }
                    },
                )
            }
            // The two numbers of a grid of your own open out under the choice, and fold away
            // again when one of Zenith's is chosen instead.
            AnimatedVisibility(visible = custom, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
                Column {
                    // Set straight onto the page as well as kept: the grid follows the finger
                    // rather than the moment the screen is next measured.
                    GroupDivider(inset = ROW_INSET)
                    StepperRow("Columns", settings.columns, GridDensity.columnChoices(preview.screenWidth.value)) { n ->
                        change { it.copy(gridColumns = n, columns = n) }
                    }
                    GroupDivider(inset = ROW_INSET)
                    StepperRow("Rows", settings.rows, GridDensity.rowChoices(preview.screenHeight.value)) { n ->
                        change { it.copy(gridRows = n, rows = n) }
                    }
                }
            }
        }
    }
    Section(header = "Icon shape", footer = "The outline every app icon and folder is cut to.", rise = 2) {
        Spot("iconShape") {
            ControlRow {
                IconShapes.ids.chunked(4).forEachIndexed { line, ids ->
                    if (line > 0) Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        ids.forEach { id ->
                            ShapeChoice(id = id, selected = id == settings.iconShape) { change { it.copy(iconShape = id) } }
                        }
                    }
                }
            }
        }
    }
    Section(
        header = "Icon style",
        footer = IconStyles.detail(settings.iconStyle) +
            if (settings.iconStyle != IconStyles.ORIGINAL) " An icon you chose for an app stays as you chose it." else "",
        rise = 3,
    ) {
        Spot("iconStyle") {
            ControlRow {
                Segmented(
                    options = IconStyles.ids.map { it to IconStyles.label(it) },
                    selected = IconStyles.of(settings.iconStyle),
                    onSelect = { value -> change { it.copy(iconStyle = value) } },
                )
            }
        }
    }
    Section(rise = 4) {
        Spot("labels") {
            ToggleRow("Labels", "App names under the icons", settings.showLabels) { value -> change { it.copy(showLabels = value) } }
        }
        // How the names look opens out under the switch, and folds away with the names.
        AnimatedVisibility(visible = settings.showLabels, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
            Column {
                GroupDivider(inset = ROW_INSET)
                Spot("labelSize") {
                    ControlRow(title = "Name size") {
                        Segmented(
                            options = LabelSizes.ids.map { it to LabelSizes.label(it) },
                            selected = LabelSizes.of(settings.labelSize),
                            onSelect = { value -> change { it.copy(labelSize = value) } },
                        )
                    }
                }
                GroupDivider(inset = ROW_INSET)
                Spot("labelShadow") {
                    ToggleRow("Name shadow", "A soft shadow under the names, for a busy wallpaper", settings.labelShadow) { value ->
                        change { it.copy(labelShadow = value) }
                    }
                }
            }
        }
    }
    Section(
        footer = if (settings.showDock) {
            when (settings.dockStyle) {
                "floating" -> "A pill of glass that hugs its icons, however few."
                "none" -> "The icons on the wallpaper, with nothing behind them."
                else -> "A bar of glass the width of the page."
            }
        } else {
            null
        },
        rise = 5,
    ) {
        Spot("dock") {
            ToggleRow(
                "Dock",
                if (settings.showDock) "Favourite apps at the foot of every page" else "Hidden. Its apps are kept for when it is back",
                settings.showDock,
            ) { value -> change { it.copy(showDock = value) } }
        }
        AnimatedVisibility(visible = settings.showDock, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
            Column {
                GroupDivider(inset = ROW_INSET)
                Spot("dockStyle") {
                    ControlRow {
                        Segmented(
                            options = listOf("glass" to "Glass bar", "floating" to "Floating", "none" to "Icons only"),
                            selected = settings.dockStyle,
                            onSelect = { value -> change { it.copy(dockStyle = value) } },
                        )
                    }
                }
            }
        }
    }
    Section(rise = 6) {
        Spot("searchBar") {
            ToggleRow("Search bar", "Above the dock, for your apps and the web", settings.searchBar) { value -> change { it.copy(searchBar = value) } }
        }
    }
    Section(
        header = "Notification dots",
        footer = when {
            dots == Badges.OFF -> "No dots on icons."
            !allowed -> "Android has to let Zenith see notifications first."
            dots == Badges.COUNT -> "A dot with how many are waiting, on the app's icon and on its folder."
            else -> "A dot on an app, and on its folder, while it has something to read."
        },
        rise = 7,
    ) {
        Spot("badges") {
            ControlRow {
                Segmented(
                    options = Badges.ids.map { it to Badges.label(it) },
                    selected = dots,
                    onSelect = { value -> change { it.copy(badges = value) } },
                )
            }
            AnimatedVisibility(visible = dots != Badges.OFF && !allowed, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
                Column {
                    GroupDivider(inset = ROW_INSET)
                    ActionRow(
                        icon = Icons.Rounded.NotificationsActive,
                        title = "Let Zenith see notifications",
                        subtitle = "Only to count them. Nothing in them is kept or sent",
                        onClick = { NotificationDots.ask(context) },
                    )
                }
            }
        }
    }
    Section(
        header = "Top of the page",
        footer = when (settings.header) {
            HomeHeader.GREETING -> "A greeting for the time of day, with your name, above the first row."
            HomeHeader.OFF -> "Nothing is written above the first row."
            HomeHeader.CLOCK -> "A large clock in the numbers' font, the date under it."
            HomeHeader.DATE -> "The day and the date, large."
            HomeHeader.GLANCE -> "The date, and the weather where the Weather widget is set to."
            else -> "Your name, small, above the first row."
        },
        rise = 8,
    ) {
        Spot("header") {
            ControlRow {
                ChoiceChips(
                    options = listOf(
                        HomeHeader.NAME to "Name", HomeHeader.GREETING to "Greeting", HomeHeader.CLOCK to "Clock",
                        HomeHeader.DATE to "Date", HomeHeader.GLANCE to "At a glance", HomeHeader.OFF to "Nothing",
                    ),
                    selected = settings.header,
                    onSelect = { value -> change { it.copy(header = value) } },
                )
            }
        }
    }
    Section(
        header = "Which page you are on",
        footer = when (settings.indicator) {
            "line" -> "A short bar gliding along a line as the pages turn."
            "numbers" -> "The page's number, out of how many there are."
            "none" -> "Nothing. Pinch out for every page at once."
            else -> "A dot for each page; the one in view stretches."
        },
        rise = 9,
    ) {
        Spot("indicator") {
            ControlRow {
                Segmented(
                    options = listOf("dots" to "Dots", "line" to "Line", "numbers" to "Numbers", "none" to "None"),
                    selected = settings.indicator,
                    onSelect = { value -> change { it.copy(indicator = value) } },
                )
            }
        }
    }
    Section(
        header = "Folders",
        footer = when (settings.folderStyle) {
            "stack" -> "The first three apps fanned like cards. A folder's own colour is in its menu."
            "cover" -> "The first app large, the next two small at its corner."
            else -> "Four apps, two by two. A folder's own colour is in its menu."
        },
        rise = 10,
    ) {
        Spot("folderStyle") {
            ControlRow {
                Segmented(
                    options = listOf("grid" to "Grid", "stack" to "Stack", "cover" to "Cover"),
                    selected = settings.folderStyle,
                    onSelect = { value -> change { it.copy(folderStyle = value) } },
                )
            }
        }
    }
    Section(rise = 11) {
        Spot("lockLayout") {
            ToggleRow(
                "Lock layout",
                if (settings.lockLayout) "Nothing can be moved, resized or removed. Menus offer to unlock" else "Hold an item to move it, as usual",
                settings.lockLayout,
            ) { value -> change { it.copy(lockLayout = value) } }
        }
        GroupDivider(inset = ROW_INSET)
        Spot("hideStatusBar") {
            ToggleRow(
                "Hide the status bar",
                "Swipe down from the very top to see it for a moment",
                settings.hideStatusBar,
            ) { value -> change { it.copy(hideStatusBar = value) } }
        }
    }
    Section(rise = 12) {
        Spot("freeform") {
            ToggleRow(
                title = "Free placement",
                subtitle = if (plus.active) "Put apps anywhere, size widgets however you like" else "Plus: put apps anywhere, any widget size",
                // Shown off without Plus even if it was left on, since it is not in effect.
                checked = settings.freeform && plus.active,
                onChange = actions.freeform,
            )
        }
        GroupDivider(inset = ROW_INSET)
        Spot("tidy") {
            ActionRow(
                icon = Icons.Rounded.AutoFixHigh,
                title = "Tidy up",
                subtitle = when (val moved = tidied) {
                    null -> "Put anything out of line back in its place"
                    0 -> "Everything was already in place"
                    1 -> "Moved 1 item"
                    else -> "Moved $moved items"
                },
                status = if (tidied != null) "Done" else null,
                spin = tidies,
            ) {
                tidied = actions.tidy()
                tidies++
            }
        }
    }
    Section(
        header = "Icon pack",
        footer = "Redraws your apps' icons in a set from an icon pack app." +
            if (packs.isEmpty()) " Install any Nova or ADW compatible icon pack and it shows up here." else "",
        rise = 13,
    ) {
        Spot("iconPack") {
            ChoiceRow("System icons", selected = settings.iconPack.isEmpty()) { change { it.copy(iconPack = "") } }
            packs.forEach { pack ->
                GroupDivider(inset = ROW_INSET)
                ChoiceRow(pack.label, selected = settings.iconPack == pack.packageName) { change { it.copy(iconPack = pack.packageName) } }
            }
        }
    }
}

/**
 * Whether Android has let Zenith see notifications, looked at again each time Settings comes back
 * to the front — which is how it hears that the switch on Android's own page was turned on.
 */
@Composable
private fun rememberNotificationAccess(): Boolean {
    val context = LocalContext.current
    val lifecycle = androidx.lifecycle.compose.LocalLifecycleOwner.current.lifecycle
    var allowed by remember { mutableStateOf(NotificationDots.allowed(context)) }
    DisposableEffect(lifecycle) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) allowed = NotificationDots.allowed(context)
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return allowed
}

@Composable
internal fun MotionOptions(settings: ZenithSettings, actions: SettingsActions, onAdvanced: () -> Unit) {
    val change = actions.change
    Section(
        header = "Animations",
        footer = if (settings.motion == "reduced") {
            "Quick fades in place of springs. Nothing flies in, jiggles or tilts."
        } else {
            "Everything moves. The phone's own Remove animations switch still turns it down."
        },
        rise = 0,
    ) {
        Spot("animations") {
            ControlRow {
                Segmented(
                    options = listOf("full" to "Full", "reduced" to "Reduced"),
                    selected = if (settings.motion == "reduced") "reduced" else "full",
                    onSelect = { value -> change { it.copy(motion = value) } },
                )
            }
        }
    }
    Section(
        header = "Opening apps",
        footer = if (settings.openAnimation == "glass") {
            "The icon grows into grey glass over a frosted home screen."
        } else {
            "The icon grows into a card in its own colour."
        },
        rise = 1,
    ) {
        Spot("openAnimation") {
            ControlRow {
                Segmented(
                    options = listOf("colour" to "Colour", "glass" to "Liquid glass"),
                    selected = settings.openAnimation,
                    onSelect = { value -> change { it.copy(openAnimation = value) } },
                )
            }
        }
    }
    Section(
        header = "Page swipe",
        footer = when (settings.pageEffect) {
            "slide" -> "Pages move side by side, nothing else."
            "cube" -> "Pages turn about the edge they share, like the sides of a box."
            "stack" -> "The page you leave shrinks and fades under the next."
            "carousel" -> "Pages go round as on a turntable, each turning to face you."
            "wheel" -> "Pages swing round a great wheel under the screen."
            "flip" -> "A page turns over where it stands, the next on its back."
            "fold" -> "Pages fold in at the crease between them, like a leaflet."
            else -> "Pages tilt back a little as they pass."
        },
        rise = 2,
    ) {
        Spot("pageEffect") {
            ControlRow {
                PageTurnChoice(settings.pageEffect) { value -> change { it.copy(pageEffect = value) } }
            }
        }
    }
    Section(
        header = "Coming home",
        footer = when (settings.arrival) {
            "below" -> "Everything rises into place, the foot of the page first."
            "cascade" -> "Everything drops in, a column at a time from the left."
            else -> "Everything comes in from round the middle, the middle first."
        },
        rise = 3,
    ) {
        Spot("arrival") {
            ControlRow {
                Segmented(
                    options = listOf("middle" to "Middle", "below" to "Below", "cascade" to "Cascade"),
                    selected = settings.arrival,
                    onSelect = { value -> change { it.copy(arrival = value) } },
                )
            }
        }
    }
    Section(
        header = "Opening the drawer",
        footer = when (settings.drawerArrival) {
            "grow" -> "It grows up out of the foot of the screen as you swipe."
            "glass" -> "It clears into view where it is, like glass."
            else -> "It slides up from the foot of the screen under your finger."
        },
        rise = 4,
    ) {
        Spot("drawerArrival") {
            ControlRow {
                Segmented(
                    options = listOf("rise" to "Rise", "grow" to "Grow", "glass" to "Glass"),
                    selected = settings.drawerArrival,
                    onSelect = { value -> change { it.copy(drawerArrival = value) } },
                )
            }
        }
    }
    Section(
        header = "Pressing",
        footer = when (settings.press) {
            "squish" -> "What you press gives like something soft, and wobbles back."
            "ripple" -> "Light spreads across what you press, out from your finger."
            else -> "What you press dips under your finger and springs back."
        },
        rise = 5,
    ) {
        Spot("press") {
            ControlRow {
                Segmented(
                    options = listOf("bounce" to "Bounce", "squish" to "Squish", "ripple" to "Ripple"),
                    selected = settings.press,
                    onSelect = { value -> change { it.copy(press = value) } },
                )
            }
        }
    }
    Section(rise = 6) {
        Spot("haptics") {
            ToggleRow(
                title = "Haptics",
                subtitle = "A small tick when you pick something up, switch it or drop it",
                checked = settings.haptics,
            ) { value -> change { it.copy(haptics = value) } }
        }
    }
    Section(rise = 7) {
        Spot("advancedMotion") {
            val style = AnimationPreset.byId(settings.animation.preset).label
            ActionRow(
                icon = Icons.Rounded.Tune,
                title = "Advanced animation settings",
                subtitle = (if (settings.animation.custom) "$style, with your own changes. " else "$style. ") +
                    "How apps open and close, number by number",
                onClick = onAdvanced,
            )
        }
    }
}

@Composable
internal fun AppsOptions(settings: ZenithSettings, plus: PlusStatus, actions: SettingsActions) {
    val change = actions.change
    Section(
        header = "Swipe up for",
        footer = if (settings.appsView == "library") {
            "Swipe left past your last page. Suggestions, recently added, then folders by kind."
        } else {
            "Every app in one list, A to Z."
        },
        rise = 0,
    ) {
        Spot("appsView") {
            ControlRow {
                Segmented(
                    options = listOf("drawer" to "App drawer", "library" to "App Library"),
                    selected = settings.appsView,
                    onSelect = { value -> change { it.copy(appsView = value) } },
                )
            }
        }
    }
    Section(header = "Order", footer = DrawerSort.detail(settings.drawerSort), rise = 1) {
        Spot("drawerSort") {
            ControlRow {
                Segmented(
                    options = DrawerSort.ids.map { it to DrawerSort.short(it) },
                    selected = DrawerSort.of(settings.drawerSort),
                    onSelect = { value -> change { it.copy(drawerSort = value) } },
                )
            }
        }
    }
    Section(
        header = "Columns",
        footer = if (settings.drawerColumns <= 0) {
            "As many across as the home screen: ${settings.columns}."
        } else {
            "${settings.drawerColumns} apps across, whatever the home screen has."
        },
        rise = 2,
    ) {
        Spot("drawerColumns") {
            ControlRow {
                Segmented(
                    options = listOf("0" to "Auto", "3" to "3", "4" to "4", "5" to "5", "6" to "6"),
                    selected = settings.drawerColumns.coerceIn(0, 6).toString().let { if (it == "1" || it == "2") "0" else it },
                    onSelect = { value -> change { it.copy(drawerColumns = value.toInt()) } },
                )
            }
        }
    }
    Section(header = "Tabs", rise = 3) {
        Spot("drawerTabs") {
            ControlRow {
                Note(
                    when (val n = settings.drawerTabs.size) {
                        0 -> "Make one with the + along the top of the drawer: Games, Work, Friends. Hold a tab to change it."
                        1 -> "1 tab of your own, along the top of the drawer. Hold it to change it."
                        else -> "$n tabs of your own, along the top of the drawer. Hold one to change it."
                    }
                )
            }
        }
    }
    Section(rise = 4) {
        Spot("hiddenApps") {
            ActionRow(
                icon = if (plus.active) Icons.Rounded.VisibilityOff else Icons.Rounded.Lock,
                title = "Hidden apps",
                subtitle = when {
                    !plus.active -> "Plus: keep chosen apps out of the drawer"
                    settings.hiddenApps.isEmpty() -> "Keep chosen apps out of the drawer"
                    else -> "${settings.hiddenApps.size} hidden"
                },
                onClick = actions.hiddenApps,
            )
        }
    }
}

@Composable
internal fun BackupOptions(plus: PlusStatus, actions: SettingsActions, onClose: () -> Unit, onAccount: () -> Unit) {
    val locked = !plus.active
    Section(footer = if (locked) "Backing up and restoring come with Zenith Plus." else null, rise = 0) {
        Spot("backup") {
            ActionRow(
                icon = if (locked) Icons.Rounded.Lock else Icons.Rounded.Save,
                title = "Back up home screen",
                subtitle = "Your layout, widgets and settings, saved to a file you keep",
                onClick = actions.backup,
            )
        }
        GroupDivider()
        Spot("restore") {
            ActionRow(
                icon = if (locked) Icons.Rounded.Lock else Icons.Rounded.Restore,
                title = "Restore from a backup",
                subtitle = "Puts a saved file back. Replaces your current layout and settings",
                onClick = actions.restore,
            )
        }
    }

    val account = actions.account
    if (account != null) CloudBackup(locked, account, onPlus = actions.plus, onAccount = onAccount)

    // Starting over wipes the home screen, so it asks twice. The second tap has to come within a
    // few seconds of the first, and the row says exactly what will happen before it happens.
    var armed by remember { mutableStateOf(false) }
    /** Counts the times it has been armed: the row shakes its head once for each. */
    var warnings by remember { mutableIntStateOf(0) }
    LaunchedEffect(armed) {
        if (armed) {
            delay(4_000)
            armed = false
        }
    }
    Section(rise = if (account != null) 3 else 2) {
        Spot("reset") {
            ActionRow(
                icon = Icons.Rounded.RestartAlt,
                title = if (armed) "Tap again to start over" else "Run setup again",
                subtitle = if (armed) "Your layout and settings will be cleared" else "Start from the welcome screen",
                danger = true,
                modifier = Modifier.shake(warnings),
            ) {
                if (armed) {
                    actions.reset()
                    onClose()
                } else {
                    armed = true
                    warnings++
                }
            }
        }
    }
}

/**
 * The backup kept in the account: one, which each new one takes the place of. Sending it is one
 * tap. Fetching it replaces the home screen, so it asks twice, the way starting over does.
 */
@Composable
private fun CloudBackup(locked: Boolean, account: AccountActions, onPlus: () -> Unit, onAccount: () -> Unit) {
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val signed = account.signed
    val savedAt = signed?.account?.backupAt
    /** "up" or "down" while the server is being talked to. */
    var working by remember { mutableStateOf<String?>(null) }
    var said by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableIntStateOf(0) }
    var fetched by remember { mutableIntStateOf(0) }
    var armed by remember { mutableStateOf(false) }
    // Another phone may have saved one since this phone last looked.
    LaunchedEffect(Unit) { account.refresh() }
    LaunchedEffect(armed) {
        if (armed) {
            delay(4_000)
            armed = false
        }
    }

    /** False when the tap went to getting Plus or to signing in instead, or something is already under way. */
    fun ready(): Boolean = when {
        working != null -> false
        locked -> {
            onPlus()
            false
        }
        signed == null -> {
            onAccount()
            false
        }
        else -> true
    }

    Section(header = "In your Zenith account", footer = said, rise = 2) {
        Spot("cloudBackup") {
            ActionRow(
                icon = if (locked) Icons.Rounded.Lock else Icons.Rounded.CloudUpload,
                title = if (working == "up") "Backing up…" else "Back up to your account",
                subtitle = when {
                    signed == null -> "Sign in to your account first"
                    savedAt != null -> "Last saved ${moment(savedAt)}"
                    else -> "Nothing saved there yet"
                },
                spin = sent,
            ) {
                if (!ready()) return@ActionRow
                working = "up"
                said = null
                scope.launch {
                    val refused = try {
                        account.backUp()
                    } finally {
                        working = null
                    }
                    said = refused ?: "Your home screen is saved in your account."
                    if (refused == null) {
                        sent++
                        haptics.confirm()
                    }
                }
            }
        }
        GroupDivider()
        Spot("cloudRestore") {
            ActionRow(
                icon = if (locked) Icons.Rounded.Lock else Icons.Rounded.CloudDownload,
                title = when {
                    working == "down" -> "Restoring…"
                    armed -> "Tap again to restore"
                    else -> "Restore from your account"
                },
                subtitle = if (armed) "Your layout and settings will be replaced" else "Replaces your current layout",
                spin = fetched,
            ) {
                if (!ready()) return@ActionRow
                if (!armed) {
                    armed = true
                    said = null
                    return@ActionRow
                }
                armed = false
                working = "down"
                scope.launch {
                    val refused = try {
                        account.restore()
                    } finally {
                        working = null
                    }
                    said = refused ?: "Your home screen is back as it was saved."
                    if (refused == null) {
                        fetched++
                        haptics.confirm()
                    }
                }
            }
        }
    }
}

@Composable
internal fun AboutOptions(actions: SettingsActions) {
    val context = LocalContext.current
    // Read fresh each time the page is shown, so coming back from Android's settings is up to date.
    val isHome = remember { context.getSystemService(RoleManager::class.java)?.isRoleHeld(RoleManager.ROLE_HOME) == true }
    val keptLoaded = remember {
        context.getSystemService(PowerManager::class.java)?.isIgnoringBatteryOptimizations(context.packageName) == true
    }
    val version = remember {
        runCatching { context.packageManager.getPackageInfo(context.packageName, 0).versionName }.getOrNull().orEmpty()
    }

    Section(rise = 0) {
        Spot("defaultHome") {
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
        }
        GroupDivider()
        // Android may close any app in the background, a launcher included; then coming home has
        // to start Zenith again. Exempting it from battery optimisation stops that.
        Spot("keepLoaded") {
            ActionRow(
                icon = Icons.Rounded.BatteryChargingFull,
                title = "Keep Zenith loaded",
                subtitle = if (keptLoaded) "Android will not put Zenith to sleep" else "Open Battery and choose Unrestricted",
                status = if (keptLoaded) "On" else null,
            ) {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
            }
        }
    }
    Section(rise = 1) {
        Spot("help") {
            ActionRow(
                icon = Icons.AutoMirrored.Rounded.HelpOutline,
                title = "How it works",
                subtitle = "Every gesture in one page",
                onClick = actions.help,
            )
        }
        GroupDivider()
        Spot("fonts") {
            ActionRow(
                icon = Icons.Rounded.TextFields,
                title = "Fonts",
                subtitle = "Inter and JetBrains Mono, under the SIL Open Font Licence",
                onClick = actions.fontLicences,
            )
        }
        GroupDivider()
        Spot("privacy") {
            ActionRow(
                icon = Icons.Rounded.PrivacyTip,
                title = "Privacy policy",
                subtitle = "No analytics, no ads; what leaves the phone, and only when you ask",
            ) {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }
        }
        GroupDivider()
        Spot("version") {
            ActionRow(icon = Icons.Rounded.Info, title = "Version", subtitle = "Zenith UI $version") { }
        }
    }
}

private const val PRIVACY_POLICY = "https://firegb2012.github.io/zenith-privacy/"
