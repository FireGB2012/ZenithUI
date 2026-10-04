package com.gabriel.zenithui.ui.settings

import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.data.GridDensity
import com.gabriel.zenithui.data.HomeHeader
import com.gabriel.zenithui.data.IconPackInfo
import com.gabriel.zenithui.data.IconPacks
import com.gabriel.zenithui.data.IconShapes
import com.gabriel.zenithui.data.PlusStatus
import com.gabriel.zenithui.data.WallStyle
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.shake
import com.gabriel.zenithui.ui.onboarding.PhonePreview
import com.gabriel.zenithui.ui.onboarding.Segmented
import com.gabriel.zenithui.ui.theme.Palettes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.runtime.rememberCoroutineScope
import com.gabriel.zenithui.ui.common.rememberHaptics
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import kotlinx.coroutines.launch

// What is on each page of Settings. Every option sits in a Spot named after its entry in the
// SettingsCatalog, which is how a search result finds its row; every block says how far down the
// page it is, which is the order the blocks rise in as the page arrives.

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
        settings.paletteId, settings.appearance, settings.gridDensity, settings.iconShape, settings.iconPack,
        settings.showLabels, settings.showDock, settings.searchBar, settings.header,
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
internal fun ThemeOptions(settings: ZenithSettings, preview: SettingsPreview, actions: SettingsActions) {
    val change = actions.change
    // Every palette you can use right now: the six everyone has, the ones bought with aura, and
    // your own once you have made one.
    val options = remember(settings.unlockedThemes, settings.paletteId, settings.customAccent, settings.customAccentAlt, settings.customBackdrop) {
        buildList {
            addAll(Palettes.all)
            addAll(Palettes.aura.filter { it.id in settings.unlockedThemes })
            if (settings.paletteId == Palettes.CUSTOM) add(Palettes.custom(settings))
        }
    }
    val design = WallStyle.byName(settings.wallpaperStyle)

    MiniPhone(settings, preview)
    Group(rise = 1) {
        Spot("palette") {
            Column(Modifier.padding(14.dp)) {
                Caption("Colour")
                Spacer(Modifier.height(12.dp))
                PaletteGrid(
                    options = options,
                    selectedId = settings.paletteId,
                    onSelect = { option -> change { it.copy(paletteId = option.id) } },
                    onMake = actions.themeMaker,
                )
            }
        }
    }
    Group(rise = 2) {
        Spot("appearance") {
            Column(Modifier.padding(14.dp)) {
                Caption("Light or dark")
                Spacer(Modifier.height(10.dp))
                Segmented(
                    options = listOf("auto" to "Auto", "light" to "Light", "dark" to "Dark"),
                    selected = settings.appearance,
                    onSelect = { value -> change { it.copy(appearance = value) } },
                )
                if (settings.appearance == "auto") {
                    Spacer(Modifier.height(8.dp))
                    Note("Follows the phone: light by day and dark at night, if the phone is set that way.")
                }
            }
        }
    }
    Group(rise = 3) {
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
    Group(rise = 4) {
        Spot("glass") {
            Column(Modifier.padding(14.dp)) {
                PillSlider(
                    label = "Glass",
                    value = settings.glass,
                    range = 0.2f..1.2f,
                    display = "${(settings.glass * 100).toInt()}%",
                ) { value -> change { it.copy(glass = value) } }
            }
        }
        Spot("tiltShine") {
            ToggleRow(
                title = "Tilt shine",
                subtitle = "Light moves across the glass as you tilt the phone",
                checked = settings.glassShine,
            ) { value -> change { it.copy(glassShine = value) } }
        }
    }
}

@Composable
internal fun HomeOptions(settings: ZenithSettings, plus: PlusStatus, preview: SettingsPreview, actions: SettingsActions) {
    val change = actions.change
    val context = LocalContext.current
    val packs by produceState(initialValue = emptyList<IconPackInfo>()) {
        value = withContext(Dispatchers.IO) { IconPacks.installed(context) }
    }
    /** How many items the last tidy moved; null until it has been run on this visit. */
    var tidied by remember { mutableStateOf<Int?>(null) }
    /** How many times it has been run: the wand turns once for each. */
    var tidies by remember { mutableIntStateOf(0) }

    MiniPhone(settings, preview)
    Group(rise = 1) {
        Spot("grid") {
            Column(Modifier.padding(14.dp)) {
                Caption("Grid")
                Spacer(Modifier.height(10.dp))
                Segmented(
                    options = GridDensity.entries.map { it.id to it.label },
                    selected = settings.gridDensity,
                    onSelect = { value -> change { it.copy(gridDensity = value) } },
                )
                Spacer(Modifier.height(8.dp))
                // How many cells that works out to depends on the screen, so it is reported, not chosen.
                Note("${settings.columns} across, ${settings.rows} down on this screen. Icons size themselves to fit.")
            }
        }
    }
    Group(rise = 2) {
        Spot("iconShape") {
            Column(Modifier.padding(14.dp)) {
                Caption("Icon shape")
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconShapes.ids.forEach { id ->
                        ShapeChoice(id = id, selected = id == settings.iconShape) { change { it.copy(iconShape = id) } }
                    }
                }
            }
        }
    }
    Group(rise = 3) {
        Spot("labels") {
            ToggleRow("Labels", "App names under the icons", settings.showLabels) { value -> change { it.copy(showLabels = value) } }
        }
        GroupDivider(inset = 16.dp)
        Spot("dock") {
            ToggleRow("Dock", "Its apps are kept while it is hidden", settings.showDock) { value -> change { it.copy(showDock = value) } }
        }
        GroupDivider(inset = 16.dp)
        Spot("searchBar") {
            ToggleRow("Search bar", "Above the dock, for your apps and the web", settings.searchBar) { value -> change { it.copy(searchBar = value) } }
        }
    }
    Group(rise = 4) {
        Spot("header") {
            Column(Modifier.padding(14.dp)) {
                Caption("Top of the page")
                Spacer(Modifier.height(10.dp))
                Segmented(
                    options = listOf(HomeHeader.NAME to "Name", HomeHeader.GREETING to "Greeting", HomeHeader.OFF to "Nothing"),
                    selected = settings.header,
                    onSelect = { value -> change { it.copy(header = value) } },
                )
            }
        }
    }
    Group(rise = 5) {
        Spot("freeform") {
            ToggleRow(
                title = "Free placement",
                subtitle = if (plus.active) "Put apps anywhere, size widgets however you like" else "Plus: put apps anywhere, any widget size",
                // Shown off without Plus even if it was left on, since it is not in effect.
                checked = settings.freeform && plus.active,
                onChange = actions.freeform,
            )
        }
        GroupDivider(inset = 16.dp)
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
    Group(rise = 6) {
        Spot("iconPack") {
            Caption("Icon pack", Modifier.padding(start = 12.dp, top = 14.dp, bottom = 4.dp))
            ChoiceRow("System icons", selected = settings.iconPack.isEmpty()) { change { it.copy(iconPack = "") } }
            packs.forEach { pack ->
                GroupDivider(inset = 52.dp)
                ChoiceRow(pack.label, selected = settings.iconPack == pack.packageName) { change { it.copy(iconPack = pack.packageName) } }
            }
            if (packs.isEmpty()) {
                Note(
                    "Install any Nova or ADW compatible icon pack and it shows up here.",
                    Modifier.padding(start = 12.dp, end = 12.dp, bottom = 14.dp),
                )
            }
        }
    }
}

@Composable
internal fun MotionOptions(settings: ZenithSettings, actions: SettingsActions) {
    val change = actions.change
    Group(rise = 0) {
        Spot("animations") {
            Column(Modifier.padding(14.dp)) {
                Caption("Animations")
                Spacer(Modifier.height(10.dp))
                Segmented(
                    options = listOf("full" to "Full", "reduced" to "Reduced"),
                    selected = if (settings.motion == "reduced") "reduced" else "full",
                    onSelect = { value -> change { it.copy(motion = value) } },
                )
                Spacer(Modifier.height(8.dp))
                Note(
                    if (settings.motion == "reduced") {
                        "Quick fades in place of springs. Nothing flies in, jiggles or tilts."
                    } else {
                        "Everything moves. The phone's own Remove animations switch still turns it down."
                    }
                )
            }
        }
    }
    Group(rise = 1) {
        Spot("openAnimation") {
            Column(Modifier.padding(14.dp)) {
                Caption("Opening apps")
                Spacer(Modifier.height(10.dp))
                Segmented(
                    options = listOf("colour" to "Colour", "glass" to "Liquid glass"),
                    selected = settings.openAnimation,
                    onSelect = { value -> change { it.copy(openAnimation = value) } },
                )
                Spacer(Modifier.height(8.dp))
                Note(
                    if (settings.openAnimation == "glass") {
                        "The icon grows into grey glass over a frosted home screen."
                    } else {
                        "The icon grows into a card in its own colour."
                    }
                )
            }
        }
    }
    Group(rise = 2) {
        Spot("pageEffect") {
            Column(Modifier.padding(14.dp)) {
                Caption("Page swipe")
                Spacer(Modifier.height(10.dp))
                Segmented(
                    options = listOf("depth" to "Depth", "slide" to "Slide", "cube" to "Cube", "stack" to "Stack"),
                    selected = settings.pageEffect,
                    onSelect = { value -> change { it.copy(pageEffect = value) } },
                )
                Spacer(Modifier.height(8.dp))
                Note(
                    when (settings.pageEffect) {
                        "slide" -> "Pages move side by side, nothing else."
                        "cube" -> "Pages turn about the edge they share, like the sides of a box."
                        "stack" -> "The page you leave shrinks and fades under the next."
                        else -> "Pages tilt back a little as they pass."
                    }
                )
            }
        }
    }
    Group(rise = 3) {
        Spot("haptics") {
            ToggleRow(
                title = "Haptics",
                subtitle = "A small tick when you pick something up, switch it or drop it",
                checked = settings.haptics,
            ) { value -> change { it.copy(haptics = value) } }
        }
    }
}

@Composable
internal fun AppsOptions(settings: ZenithSettings, plus: PlusStatus, actions: SettingsActions) {
    val change = actions.change
    Group(rise = 0) {
        Spot("appsView") {
            Column(Modifier.padding(14.dp)) {
                Caption("Swipe up for")
                Spacer(Modifier.height(10.dp))
                Segmented(
                    options = listOf("drawer" to "App drawer", "library" to "App Library"),
                    selected = settings.appsView,
                    onSelect = { value -> change { it.copy(appsView = value) } },
                )
                Spacer(Modifier.height(8.dp))
                Note(
                    if (settings.appsView == "library") {
                        "Swipe left past your last page. Suggestions, recently added, then folders by kind."
                    } else {
                        "Every app in one list, A to Z."
                    }
                )
            }
        }
    }
    Group(rise = 1) {
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
    Group(rise = 0) {
        Spot("backup") {
            ActionRow(
                icon = if (locked) Icons.Rounded.Lock else Icons.Rounded.Save,
                title = "Back up home screen",
                subtitle = "Save everything to a file",
                onClick = actions.backup,
            )
        }
        GroupDivider()
        Spot("restore") {
            ActionRow(
                icon = if (locked) Icons.Rounded.Lock else Icons.Rounded.Restore,
                title = "Restore from a backup",
                subtitle = "Replaces your current layout",
                onClick = actions.restore,
            )
        }
    }
    if (locked) Note("Backing up and restoring come with Zenith Plus.", Modifier.riseIn(1).padding(horizontal = 4.dp))

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
    Group(rise = if (account != null) 3 else 2) {
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
    val reduced = LocalMotionLevel.current.reduced
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

    Column(Modifier.riseIn(2)) {
        Caption("In your Zenith account", Modifier.padding(top = 6.dp, bottom = 10.dp))
        Group {
            Spot("cloudBackup") {
                ActionRow(
                    icon = if (locked) Icons.Rounded.Lock else Icons.Rounded.CloudUpload,
                    title = if (working == "up") "Backing up\u2026" else "Back up to your account",
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
                        working == "down" -> "Restoring\u2026"
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
        AnimatedVisibility(visible = said != null, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
            Note(said.orEmpty(), Modifier.padding(top = 10.dp))
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

    Group(rise = 0) {
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
    Group(rise = 1) {
        Spot("help") {
            ActionRow(
                icon = Icons.AutoMirrored.Rounded.HelpOutline,
                title = "How it works",
                subtitle = "Every gesture in one page",
                onClick = actions.help,
            )
        }
        GroupDivider()
        Spot("privacy") {
            ActionRow(
                icon = Icons.Rounded.PrivacyTip,
                title = "Privacy policy",
                subtitle = "No account, no analytics, nothing leaves your phone",
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
