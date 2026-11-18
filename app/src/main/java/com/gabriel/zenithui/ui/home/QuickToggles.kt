package com.gabriel.zenithui.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gabriel.zenithui.data.SystemPanels
import com.gabriel.zenithui.data.ToggleGrid
import com.gabriel.zenithui.data.ToggleNames
import com.gabriel.zenithui.data.ToggleState
import com.gabriel.zenithui.data.ToggleStates
import com.gabriel.zenithui.data.Torch
import com.gabriel.zenithui.data.WidgetForm
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.common.shake
import com.gabriel.zenithui.ui.settings.Toggle as OnOffSwitch
import com.gabriel.zenithui.ui.theme.GlassMotion
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.ZenithPalette
import com.gabriel.zenithui.ui.theme.themed
import com.gabriel.zenithui.ui.theme.glassPress
import com.gabriel.zenithui.ui.theme.inkOnTint
import com.gabriel.zenithui.ui.theme.inDark

// ---------------------------------------------------------------------- the controls

/**
 * The colour a control lights up in when it is on, after Control Center's: Wi-Fi, Bluetooth and the
 * rest of the radios blue, mobile data and the hotspot green, airplane mode orange, Do Not Disturb
 * indigo, battery saver yellow, and the torch white, as a torch is ([lit]).
 */
internal enum class Tone { Blue, Green, Orange, Indigo, Yellow, Light }

/** One control the Quick Toggles widget can show. */
@Immutable
private data class ToggleOption(
    val id: String,
    /** Its name, as the sheet and a tile with room for it have it. */
    val label: String,
    /** Its name on a tile too narrow for [label]. */
    val short: String,
    val detail: String,
    val icon: ImageVector,
    val tone: Tone,
)

/**
 * Everything the Quick Toggles widget can hold. A launcher is not allowed to flip most of these by
 * itself, so apart from the torch each one opens the phone's own switch for it — the quick panel
 * where Android has one, otherwise the settings page — and shows whether it is on wherever Android
 * lets a launcher read it ([ToggleStates]).
 */
private val TOGGLE_OPTIONS = listOf(
    ToggleOption("torch", "Torch", "Torch", "Turns the flashlight on and off", Icons.Rounded.FlashlightOn, Tone.Light),
    ToggleOption("wifi", "Wi-Fi", "Wi-Fi", "Opens the Wi-Fi panel", Icons.Rounded.Wifi, Tone.Blue),
    ToggleOption("bluetooth", "Bluetooth", "Bluetooth", "Opens Bluetooth settings", Icons.Rounded.Bluetooth, Tone.Blue),
    ToggleOption("data", "Mobile data", "Data", "Opens the internet panel", Icons.Rounded.SignalCellularAlt, Tone.Green),
    ToggleOption("airplane", "Airplane mode", "Airplane", "Opens airplane mode settings", Icons.Rounded.AirplanemodeActive, Tone.Orange),
    ToggleOption("focus", "Do Not Disturb", "Focus", "Opens Do Not Disturb", Icons.Rounded.DoNotDisturbOn, Tone.Indigo),
    ToggleOption("location", "Location", "Location", "Opens location settings", Icons.Rounded.LocationOn, Tone.Blue),
    ToggleOption("hotspot", "Hotspot", "Hotspot", "Opens hotspot settings", Icons.Rounded.WifiTethering, Tone.Green),
    ToggleOption("saver", "Battery saver", "Low Power", "Opens battery saver settings", Icons.Rounded.BatterySaver, Tone.Yellow),
    ToggleOption("volume", "Volume", "Volume", "Opens the volume panel", Icons.AutoMirrored.Rounded.VolumeUp, Tone.Blue),
    ToggleOption("nfc", "NFC", "NFC", "Opens NFC settings", Icons.Rounded.Nfc, Tone.Blue),
    ToggleOption("display", "Display", "Display", "Opens display settings", Icons.Rounded.BrightnessMedium, Tone.Blue),
)

/** A control as the widget draws it: what it is, what its setting is now, and what a tap does. */
@Immutable
private data class QuickToggle(val option: ToggleOption, val state: ToggleState, val onClick: () -> Unit) {
    val label: String get() = option.label
    val on: Boolean get() = state.on == true

    /** Its symbol, which for the ringer and the brightness says how they are set. */
    val icon: ImageVector
        get() = when {
            option.id == "volume" && state.note == "Vibrate" -> Icons.Rounded.Vibration
            option.id == "volume" && state.note == "Silent" -> Icons.AutoMirrored.Rounded.VolumeOff
            option.id == "display" && state.note == "Auto" -> Icons.Rounded.BrightnessAuto
            else -> option.icon
        }
}

/**
 * The colour [this] lights up in: Control Center's own, in its Dark or Light shade. The torch takes
 * the words' own ink, white in Dark and Zenith's near-black in Light, where a white torch would be
 * lost on a white card; a "No glass" symbol is white while it is off, so a [bare] torch lights up
 * yellow, as a lamp does.
 */
internal fun Tone.lit(dark: Boolean, bare: Boolean = false): Color = when (this) {
    Tone.Blue -> if (dark) Color(0xFF0A84FF) else Color(0xFF007AFF)
    Tone.Green -> if (dark) Color(0xFF30D158) else Color(0xFF34C759)
    Tone.Orange -> if (dark) Color(0xFFFF9F0A) else Color(0xFFFF9500)
    Tone.Indigo -> if (dark) Color(0xFF5E5CE6) else Color(0xFF5856D6)
    Tone.Yellow -> if (dark) Color(0xFFFFD60A) else Color(0xFFFFCC00)
    Tone.Light -> when {
        bare -> Color(0xFFFFD60A)
        dark -> Color.White
        else -> Color(0xFF10131A)
    }
}

/** [tone]'s colour on [palette], taken towards a widget's own colour as far as it has come in. */
private fun Tone.colour(palette: ZenithPalette, bare: Boolean = false): Color {
    val base = lit(palette.dark, bare)
    return if (palette.ownColour > 0f) lerp(base, palette.accent, palette.ownColour.coerceAtMost(1f)) else base
}

/** Fills lighter than this take a dark symbol: Control Center's yellow and the white torch. */
private const val LIGHT_FILL = 0.6f

/**
 * The symbol on a lit control: white, as Control Center has it on its blue, green, orange and
 * indigo, which are lighter than [inkOnTint] would put white on; dark only on a fill too light for
 * white to be seen on at all.
 */
internal fun litInk(lit: Color): Color = if (lit.luminance() > LIGHT_FILL) inkOnTint(lit) else Color.White

/** A round button or a tile's disc while it is off: a faint coat of the ink, like Control Center's clear glass. */
private fun offDisc(palette: ZenithPalette): Color = palette.onGlass.copy(alpha = if (palette.dark) 0.14f else 0.08f)

/** A tile's own body, fainter than its disc so the disc reads as the switch. */
private fun tileBody(palette: ZenithPalette): Color = palette.onGlass.copy(alpha = if (palette.dark) 0.10f else 0.06f)

private val TILE_NAME = TextStyle(fontSize = 14.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold)
private val TILE_WORDS = TextStyle(fontSize = 12.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium)
private val CARD_NAME = TextStyle(fontSize = 16.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold)
private val CARD_WORDS = TextStyle(fontSize = 13.sp, lineHeight = 16.sp, fontWeight = FontWeight.Medium)

/** Between a tile's disc and its name, and from its words to its far end. */
private val TILE_DISC_GAP = 10.dp
private val TILE_END = 14.dp

/** Inside a card, all round. */
private val CARD_PAD = 14.dp

/** A card's corners: rounder than the widget's would allow, as Control Center's own modules are. */
private val CARD_SHAPE = RoundedCornerShape(18.dp)

/**
 * How large a control's symbol is. Testers asked for them a little larger, to be known at a glance:
 * each is now about a tenth larger than it was, and still leaves a ring of its disc round it, the
 * disc's colour being how a control says it is on.
 */
internal object ToggleSymbols {
    /** How much of a round button its symbol fills: half. */
    const val BUTTON = 0.5f
    val SMALLEST = 19.dp
    val LARGEST = 32.dp

    /** How much of a tile's disc its symbol fills. */
    const val TILE = 0.58f

    /** A card's disc, and its symbol. */
    val CARD_DISC = 44.dp
    val CARD = 26.dp

    /** The same for a "No glass" control, where the symbol is the whole button. */
    const val BARE = 0.76f

    fun button(size: Dp): Dp = (size * BUTTON).coerceIn(SMALLEST, LARGEST)

    fun tile(disc: Dp): Dp = disc * TILE
}

/** A tile's disc: most of its height, inside the tile's round end. */
private fun tileDisc(tileHeight: Dp): Dp = (tileHeight * 0.6f).coerceIn(30.dp, 40.dp)

// ---------------------------------------------------------------------- the widget

/**
 * The Quick Toggles widget, set out on a grid as Control Center is ([ToggleGrid]): round buttons,
 * tiles with names where there is room for them, and a button for the rest when not all fit. Each
 * control lights up in its own colour while its setting is on.
 */
@Composable
internal fun TogglesWidget(
    @Suppress("UNUSED_PARAMETER") form: WidgetForm,
    chosen: List<String>,
    width: Dp,
    height: Dp,
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { Torch.watch(context) }
    val torchOn by Torch.on.collectAsState()

    val options = remember(chosen) {
        chosen.mapNotNull { id -> TOGGLE_OPTIONS.firstOrNull { it.id == id } }.ifEmpty { TOGGLE_OPTIONS.take(1) }
    }
    val grid = remember(width, height, options.size) { ToggleGrid.of(width.value, height.value, options.size) }
    val shownIds = remember(grid, options) { grid.slots.filter { it.control >= 0 }.map { options[it.control].id } }
    val states = rememberToggleStates(shownIds, torchOn)
    val names = rememberTileNames(grid, options)

    Box(Modifier.fillMaxSize().padding(grid.pad.dp)) {
        for (slot in grid.slots) {
            Box(
                Modifier
                    .offset(grid.x(slot).dp, grid.y(slot).dp)
                    .size(grid.width(slot).dp, grid.cellHeight.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (slot.kind == ToggleGrid.Kind.More) {
                    MoreButton(grid.more, grid.button.dp)
                } else {
                    val option = options[slot.control]
                    val toggle = QuickToggle(option, states[option.id] ?: ToggleState.Unknown, actionFor(context, option.id))
                    val name = if (names.short.getOrElse(names.index(slot)) { false }) option.short else option.label
                    when (slot.kind) {
                        ToggleGrid.Kind.Button -> ToggleButton(toggle, grid.button.dp)
                        ToggleGrid.Kind.Tile -> ToggleTile(toggle, name, names.scale, grid.tile.dp, Modifier.fillMaxWidth())
                        else -> ToggleCard(toggle, name, names.scale, Modifier.fillMaxSize())
                    }
                }
            }
        }
    }
}

/** What a tap on control [id] does. */
private fun actionFor(context: Context, id: String): () -> Unit = when (id) {
    "torch" -> { { Torch.toggle(context) } }
    "wifi" -> { { context.startActivityIntent(Settings.Panel.ACTION_WIFI, Settings.ACTION_WIFI_SETTINGS) } }
    "bluetooth" -> { { context.startActivityIntent(Settings.ACTION_BLUETOOTH_SETTINGS) } }
    "data" -> { { context.startActivityIntent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY, Settings.ACTION_WIRELESS_SETTINGS) } }
    "airplane" -> { { context.startActivityIntent(Settings.ACTION_AIRPLANE_MODE_SETTINGS) } }
    "focus" -> { { context.startActivityIntent("android.settings.ZEN_MODE_SETTINGS", Settings.ACTION_SOUND_SETTINGS) } }
    "location" -> { { context.startActivityIntent(Settings.ACTION_LOCATION_SOURCE_SETTINGS) } }
    "hotspot" -> { { context.startActivityIntent("android.settings.TETHER_SETTINGS", Settings.ACTION_WIRELESS_SETTINGS) } }
    "saver" -> { { context.startActivityIntent(Settings.ACTION_BATTERY_SAVER_SETTINGS) } }
    "volume" -> { { context.startActivityIntent(Settings.Panel.ACTION_VOLUME, Settings.ACTION_SOUND_SETTINGS) } }
    "nfc" -> { { context.startActivityIntent(Settings.ACTION_NFC_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS) } }
    else -> { { context.startActivityIntent(Settings.ACTION_DISPLAY_SETTINGS) } }
}

private fun Context.startActivityIntent(action: String, fallback: String? = null) {
    runCatching { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure {
            if (fallback != null) runCatching { startActivity(Intent(fallback).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
}

/**
 * Whether each of [ids] is on, read again whenever Android says one of them changed, whenever a
 * setting it keeps for one changes, and whenever the home screen comes back, which is how a change
 * made in a panel the widget opened is seen.
 */
@Composable
private fun rememberToggleStates(ids: List<String>, torchOn: Boolean): Map<String, ToggleState> {
    val context = LocalContext.current
    var changes by remember { mutableIntStateOf(0) }
    DisposableEffect(ids) {
        val app = context.applicationContext
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                changes++
            }
        }
        val actions = ToggleStates.broadcasts(ids)
        // Only Android's own broadcasts, which no other app can send.
        val listening = actions.isNotEmpty() && runCatching {
            ContextCompat.registerReceiver(app, receiver, IntentFilter().apply { actions.forEach(::addAction) }, ContextCompat.RECEIVER_EXPORTED)
        }.isSuccess
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                changes++
            }
        }
        ToggleStates.watched(ids).forEach { uri -> runCatching { app.contentResolver.registerContentObserver(uri, false, observer) } }
        onDispose {
            if (listening) runCatching { app.unregisterReceiver(receiver) }
            runCatching { app.contentResolver.unregisterContentObserver(observer) }
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { changes++ }
    return remember(ids, changes, torchOn) { ids.associateWith { ToggleStates.read(context, it, torchOn) } }
}

/** Which names the widget's tiles or cards take, and the one size they are all set at ([ToggleNames]). */
private class TileNames(private val order: Map<ToggleGrid.Slot, Int>, val short: List<Boolean>, val scale: Float) {
    fun index(slot: ToggleGrid.Slot): Int = order[slot] ?: -1
}

@Composable
private fun rememberTileNames(grid: ToggleGrid, options: List<ToggleOption>): TileNames {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    // Measured in the font they are drawn in, or a wider font would not fit where it was measured to.
    val font = LocalZenithFont.current
    return remember(grid, options, density.density, density.fontScale, font) {
        val named = grid.slots.filter { it.kind == ToggleGrid.Kind.Tile || it.kind == ToggleGrid.Kind.Card }
        if (named.isEmpty()) return@remember TileNames(emptyMap(), emptyList(), 1f)
        val card = named.first().kind == ToggleGrid.Kind.Card
        val style = (if (card) CARD_NAME else TILE_NAME).copy(fontFamily = font)
        fun widthOf(text: String) = with(density) {
            measurer.measure(text, style, softWrap = false, maxLines = 1).size.width.toDp().value
        }
        val across = grid.width(named.first())
        val room = if (card) {
            across - CARD_PAD.value * 2
        } else {
            val disc = tileDisc(grid.tile.dp).value
            across - (grid.tile - disc) / 2f - disc - TILE_DISC_GAP.value - TILE_END.value
        }
        val fit = ToggleNames.fit(named.map { ToggleNames.Widths(widthOf(options[it.control].label), widthOf(options[it.control].short)) }, room)
        TileNames(named.withIndex().associate { (index, slot) -> slot to index }, fit.short, fit.scale)
    }
}

/**
 * A control's disc, the round button itself or the one on a tile: a faint coat while it is off, and
 * its own colour poured in from the middle as it comes on, which drains away again as it goes off.
 * It stays round, so on a tile it keeps the curve of the tile's end.
 */
@Composable
private fun LitDisc(toggle: QuickToggle, size: Dp, symbol: Dp, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val lit = toggle.option.tone.colour(palette)
    val fill by animateFloatAsState(
        targetValue = if (toggle.on) 1f else 0f,
        animationSpec = if (reduced) tween(160) else spring(dampingRatio = 0.86f, stiffness = 260f),
        label = "discFill",
    )
    val ink by animateColorAsState(
        targetValue = if (toggle.on) litInk(lit) else palette.onGlass,
        animationSpec = tween(if (reduced) 160 else 280),
        label = "discInk",
    )
    val off = offDisc(palette)
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .drawBehind {
                drawRect(off)
                if (fill > 0.001f) drawCircle(lit, radius = this.size.minDimension / 2f * fill)
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(toggle.icon, contentDescription = null, tint = ink, modifier = Modifier.size(symbol))
    }
}

/** A "No glass" control's symbol: glass, clear while it is off and in its own colour while it is on. */
@Composable
private fun BareSymbol(toggle: QuickToggle, size: Dp) {
    val palette = LocalPalette.current
    val ink by animateColorAsState(
        targetValue = if (toggle.on) toggle.option.tone.colour(palette.inDark(), bare = true) else Color.White,
        animationSpec = tween(320),
        label = "bareInk",
    )
    Symbol(toggle.icon, size, tint = ink)
}

@Composable
private fun ToggleButton(toggle: QuickToggle, size: Dp) {
    val bare = LocalBare.current
    Box(
        Modifier
            .size(size)
            // Leans towards the finger, and swells a little, as the clear glass round it does.
            .glassPress(GlassMotion.Lean)
            .clip(RoundedCornerShape(percent = 50))
            .semantics {
                contentDescription = toggle.label
                toggle.state.words?.let { stateDescription = it }
            }
            .clickable(role = if (toggle.option.id == "torch") Role.Switch else Role.Button, onClick = toggle.onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (bare) BareSymbol(toggle, size * ToggleSymbols.BARE) else LitDisc(toggle, size, ToggleSymbols.button(size))
    }
}

/**
 * A control with its name: its disc at the round end of a capsule, and beside it the name and
 * whether it is on, which changes over with a short fade as the setting does.
 */
@Composable
private fun ToggleTile(toggle: QuickToggle, name: String, scale: Float, height: Dp, modifier: Modifier) {
    val palette = LocalPalette.current
    val bare = LocalBare.current
    val disc = tileDisc(height)
    Row(
        modifier
            .height(height)
            .glassPress(GlassMotion.Lean)
            .clip(RoundedCornerShape(percent = 50))
            .then(if (bare) Modifier else Modifier.background(tileBody(palette)))
            .clickable(role = if (toggle.option.id == "torch") Role.Switch else Role.Button, onClick = toggle.onClick)
            .padding(start = (height - disc) / 2, end = TILE_END),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (bare) BareSymbol(toggle, disc * 0.8f) else LitDisc(toggle, disc, ToggleSymbols.tile(disc))
        Spacer(Modifier.width(TILE_DISC_GAP))
        NameAndState(toggle, name, TILE_NAME, TILE_WORDS, scale)
    }
}

/** A control with room to stand tall: its disc at the top, its name and whether it is on at the foot. */
@Composable
private fun ToggleCard(toggle: QuickToggle, name: String, scale: Float, modifier: Modifier) {
    val palette = LocalPalette.current
    val bare = LocalBare.current
    Column(
        modifier
            .glassPress(GlassMotion.Lean)
            .clip(themed(CARD_SHAPE))
            .then(if (bare) Modifier else Modifier.background(tileBody(palette)))
            .clickable(role = if (toggle.option.id == "torch") Role.Switch else Role.Button, onClick = toggle.onClick)
            .padding(CARD_PAD),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        if (bare) BareSymbol(toggle, ToggleSymbols.CARD_DISC * 0.8f) else LitDisc(toggle, ToggleSymbols.CARD_DISC, ToggleSymbols.CARD)
        NameAndState(toggle, name, CARD_NAME, CARD_WORDS, scale)
    }
}

@Composable
private fun NameAndState(toggle: QuickToggle, name: String, nameStyle: TextStyle, wordsStyle: TextStyle, scale: Float) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val base = LocalTextStyle.current
    Column {
        Text(
            name,
            style = base.merge(nameStyle.scaled(scale)).copy(color = palette.onGlass, fontFamily = LocalZenithFont.current),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        val words = toggle.state.words ?: return@Column
        AnimatedContent(
            targetState = words,
            transitionSpec = {
                if (reduced) {
                    fadeIn(tween(160)) togetherWith fadeOut(tween(120))
                } else {
                    (fadeIn(tween(240)) + slideInVertically(tween(240)) { it / 3 }) togetherWith
                        (fadeOut(tween(160)) + slideOutVertically(tween(160)) { -it / 3 })
                }
            },
            label = "toggleWords",
        ) { now ->
            Text(
                now,
                style = base.merge(wordsStyle.scaled(scale)).copy(color = quiet(), fontFamily = LocalZenithFont.current),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun TextStyle.scaled(scale: Float): TextStyle =
    if (scale >= 1f) this else copy(fontSize = fontSize * scale, lineHeight = lineHeight * scale)

/**
 * The last button of a widget without room for every control chosen: how many more there are, and
 * a tap opens the phone's own quick settings, where all of them are.
 */
@Composable
private fun MoreButton(count: Int, size: Dp) {
    val context = LocalContext.current
    val palette = LocalPalette.current
    val bare = LocalBare.current
    Box(
        Modifier
            .size(size)
            .glassPress(GlassMotion.Lean)
            .clip(RoundedCornerShape(percent = 50))
            .then(if (bare) Modifier else Modifier.background(offDisc(palette)))
            .semantics { contentDescription = "$count more, in quick settings" }
            .clickable(role = Role.Button) {
                if (!SystemPanels.quickSettings(context)) context.startActivityIntent(Settings.ACTION_SETTINGS)
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "+$count",
            style = LocalTextStyle.current.merge(
                TextStyle(
                    fontSize = (size.value * 0.3f).coerceIn(12f, 17f).sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (bare) Color.White else palette.onGlass,
                    fontFamily = LocalZenithFont.current,
                ),
            ),
            maxLines = 1,
        )
    }
}

// ---------------------------------------------------------------------- choosing them

/**
 * Where you choose what the Quick Toggles widget shows. The widget rearranges itself to fit
 * whatever it holds, at whatever size it is, so any mix works.
 */
@Composable
fun TogglesSheet(
    visible: Boolean,
    selected: List<String>,
    onChange: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Quick Toggles") {
        Text(
            "Choose what the widget shows. Each one lights up while it is on. Where there is room the " +
                "first ones get tiles with their names, and if not all of them fit, the last button opens the rest.",
            color = palette.onGlassMuted,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(8.dp))
        Column(Modifier.verticalScroll(rememberScrollState())) {
            TOGGLE_OPTIONS.forEach { option ->
                val on = option.id in selected
                ToggleChoice(option, on) {
                    val next = if (on) selected - option.id else selected + option.id
                    // Always at least one control, and always in the catalogue's order.
                    if (next.isNotEmpty()) onChange(TOGGLE_OPTIONS.map { it.id }.filter { it in next })
                    next.isNotEmpty()
                }
            }
        }
    }
}

/**
 * One control the widget could show. Chosen, its disc takes the accent, and gives a small bounce
 * as it does; the switch beside it is the one Settings has, worked by the whole row. [onFlip]
 * answers whether it was flipped: the last one left cannot be taken away, and shakes its head.
 */
@Composable
private fun ToggleChoice(option: ToggleOption, on: Boolean, onFlip: () -> Boolean) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    val reduced = LocalMotionLevel.current.reduced
    val touch = remember { MutableInteractionSource() }
    val pressed by touch.collectIsPressedAsState()
    var refused by remember { mutableIntStateOf(0) }
    val disc by animateColorAsState(
        targetValue = if (on) palette.accent.copy(alpha = 0.9f) else palette.onGlass.copy(alpha = 0.10f),
        animationSpec = tween(260),
        label = "choiceDisc",
    )
    val ink by animateColorAsState(if (on) inkOnTint(palette.accent) else palette.onGlass, tween(260), label = "choiceInk")
    // Not as the list arrives: only when this one is chosen or let go while it is on show.
    val bounce = remember { Animatable(1f) }
    val shownAs = remember { booleanArrayOf(on) }
    LaunchedEffect(on) {
        if (shownAs[0] == on) return@LaunchedEffect
        shownAs[0] = on
        if (reduced) return@LaunchedEffect
        bounce.snapTo(0.86f)
        // One small bounce and still, as the glass springs back.
        bounce.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 420f))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .shake(refused)
            .clickable(interactionSource = touch, indication = null) {
                if (onFlip()) tick() else refused++
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .graphicsLayer {
                    scaleX = bounce.value
                    scaleY = bounce.value
                }
                .clip(RoundedCornerShape(percent = 50))
                .background(disc),
            contentAlignment = Alignment.Center,
        ) {
            Icon(option.icon, contentDescription = null, tint = ink, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(option.label, color = palette.onGlass, fontSize = 15.sp)
            Text(option.detail, color = palette.onGlassMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        OnOffSwitch(on, pressed)
    }
}
