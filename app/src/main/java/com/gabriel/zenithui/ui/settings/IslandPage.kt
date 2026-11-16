package com.gabriel.zenithui.ui.settings

import android.os.SystemClock
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.gabriel.zenithui.data.IslandKind
import com.gabriel.zenithui.data.IslandMoment
import com.gabriel.zenithui.data.IslandRules
import com.gabriel.zenithui.data.IslandSettings
import com.gabriel.zenithui.data.NotchAccess
import com.gabriel.zenithui.data.NotchFit
import com.gabriel.zenithui.data.NotificationDots
import com.gabriel.zenithui.data.NotchStatus
import com.gabriel.zenithui.data.Playing
import com.gabriel.zenithui.data.PlusStatus
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.island.IslandActions
import com.gabriel.zenithui.island.IslandCard
import com.gabriel.zenithui.island.IslandCompact
import com.gabriel.zenithui.island.IslandService
import com.gabriel.zenithui.island.IslandState
import com.gabriel.zenithui.island.rememberIslandGeometry
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.corners
import com.gabriel.zenithui.ui.theme.inkOnTint
import kotlinx.coroutines.delay

/**
 * Settings → Zenith Notch: the camera, stretched, over every app (Zenith Plus). At the top, the top
 * of this very phone — its camera where it is and as big as it is — with the Notch on it, the one
 * drawn over apps; under it, what it shows and when it keeps out of the way. Each of Android's two
 * permissions is explained in a sheet before Android's own page opens (see NotchAccessSheet).
 */
@Composable
internal fun IslandOptions(settings: ZenithSettings, plus: PlusStatus, actions: SettingsActions) {
    val context = LocalContext.current
    val reduced = LocalMotionLevel.current.reduced
    val island = settings.island
    fun change(transform: (IslandSettings) -> IslandSettings) = actions.change { it.copy(island = transform(it.island)) }
    // Android's two permissions, read again each time the page comes back from Android's settings.
    var overApps by remember { mutableStateOf(IslandService.allowed(context)) }
    var listening by remember { mutableStateOf(NotificationDots.allowed(context)) }
    LifecycleResumeEffect(Unit) {
        overApps = IslandService.allowed(context)
        listening = NotificationDots.allowed(context)
        onPauseOrDispose { }
    }

    Box(Modifier.riseIn(0)) { IslandPreview(island) }
    Group(rise = 1) {
        Spot("island") {
            ToggleRow(
                title = "Zenith Notch",
                subtitle = when {
                    !plus.active -> "Plus: your camera, stretched, over every app"
                    island.on && !overApps -> "On, waiting for leave to show over other apps"
                    island.on -> "Round the camera, over every app"
                    else -> "Off"
                },
                checked = island.on && plus.active,
            ) { on ->
                if (!plus.active) {
                    actions.plus()
                } else {
                    change { it.copy(on = on) }
                    if (on && !overApps) actions.notchAccess(NotchAccess.OVER_APPS)
                }
            }
        }
        AnimatedVisibility(visible = island.on && plus.active && !overApps, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
            Column {
                GroupDivider()
                ActionRow(
                    icon = Icons.Rounded.Layers,
                    title = "Let it show over other apps",
                    subtitle = "Android asks you once: here's why, and how",
                    onClick = { actions.notchAccess(NotchAccess.OVER_APPS) },
                )
            }
        }
        AnimatedVisibility(visible = island.on && (island.music || island.live || island.notifications) && !listening, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
            Column {
                GroupDivider()
                ActionRow(
                    icon = Icons.Rounded.NotificationsActive,
                    title = "Let it see notifications",
                    subtitle = "For music, calls, timers and messages: here's why, and how",
                    onClick = { actions.notchAccess(NotchAccess.NOTIFICATIONS) },
                )
            }
        }
    }
    Group(rise = 2) {
        Spot("islandShows") {
            Column(Modifier.padding(start = 14.dp, end = 14.dp, top = 14.dp)) { Caption("What it shows") }
            ToggleRow("Music", "The cover, and bars in its colour. Tap for the song", island.music) { on -> change { it.copy(music = on) } }
            GroupDivider()
            ToggleRow("Calls and timers", "A call's time in green, a timer counting in orange", island.live) { on -> change { it.copy(live = on) } }
            GroupDivider()
            ToggleRow("Notifications", "The app and who it is from. Tap to read it", island.notifications) { on -> change { it.copy(notifications = on) } }
            GroupDivider()
            ToggleRow("Charger", "How full, for a moment as you plug in", island.charging) { on -> change { it.copy(charging = on) } }
            GroupDivider()
            ToggleRow("Ring and silent", "For a moment, as you change it", island.ringer) { on -> change { it.copy(ringer = on) } }
        }
    }
    Group(rise = 3) {
        Spot("islandAlways") {
            ToggleRow(
                "Battery and status at rest",
                "A small pill round the camera with your battery and Do Not Disturb or silent. Off, it's only the camera",
                island.always,
            ) { on -> change { it.copy(always = on) } }
        }
    }
    Group(rise = 4) {
        Spot("islandFit") {
            val fit = island.fit
            fun refit(transform: (NotchFit) -> NotchFit) = change { it.copy(fit = transform(it.fit)) }
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Caption("Size and position")
                PillSlider("Width", fit.width, NotchFit.WIDTH, share(fit.width)) { v -> refit { NotchFit.of(v, it.height, it.x, it.y) } }
                PillSlider("Height", fit.height, NotchFit.HEIGHT, share(fit.height)) { v -> refit { NotchFit.of(it.width, v, it.x, it.y) } }
                PillSlider("Left and right", fit.x, NotchFit.X, nudge(fit.x, "right", "left")) { v -> refit { NotchFit.of(it.width, it.height, v, it.y) } }
                PillSlider("Up and down", fit.y, NotchFit.Y, nudge(fit.y, "down", "up")) { v -> refit { NotchFit.of(it.width, it.height, it.x, v) } }
                Note("It stays at the top, round the camera: a little bigger or smaller, a few dp either way, to sit just right on this phone.")
            }
        }
        // The way back, only while there is something to go back from; its arrow turns once round.
        var resets by remember { mutableIntStateOf(0) }
        AnimatedVisibility(visible = !island.fit.isDefault, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
            Column {
                GroupDivider()
                ActionRow(
                    icon = Icons.Rounded.RestartAlt,
                    title = "Reset to default",
                    subtitle = "As it's made for this phone's camera",
                    spin = resets,
                    onClick = {
                        change { it.copy(fit = NotchFit()) }
                        resets++
                    },
                )
            }
        }
    }
    Group(rise = 5) {
        Spot("islandWhen") {
            ToggleRow("Out of the way in full screen", "A video or a game has the whole screen to itself", island.hideFullScreen) { on -> change { it.copy(hideFullScreen = on) } }
            GroupDivider()
            ToggleRow("Out of the way sideways", "While the phone is on its side", island.hideSideways) { on -> change { it.copy(hideSideways = on) } }
        }
    }
    Note(
        "It only shows what the phone's own music and notifications already show. Nothing is kept, and nothing leaves the phone.",
        Modifier.riseIn(6).padding(horizontal = 14.dp),
    )
}

/** "112%" from 1.12: a share of the Notch's own size. */
private fun share(value: Float): String = "${Math.round(value * 100f)}%"

/** "On the camera", or "3 dp right" from 3 and "3 dp left" from -3. */
private fun nudge(value: Float, more: String, less: String): String {
    val dp = Math.round(value)
    return when {
        dp == 0 -> "On the camera"
        dp > 0 -> "$dp dp $more"
        else -> "${-dp} dp $less"
    }
}

/**
 * The top of this phone, at its own size and scaled to fit: its camera where it is, its status
 * bar as tall as it is, an app under it, and the island on it — the one drawn over apps, run by the
 * same rules. Tap the island to open it, as on the phone.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IslandPreview(island: IslandSettings) {
    val palette = LocalPalette.current
    val geometry = rememberIslandGeometry().copy(fit = island.fit)
    // While its size and position are being set the pill shows, even if it is off at rest, so
    // there is something to see move; a moment after the last change it goes back to what it was.
    var fitting by remember { mutableStateOf(false) }
    var lastFit by remember { mutableStateOf(island.fit) }
    LaunchedEffect(island.fit) {
        if (island.fit == lastFit) return@LaunchedEffect
        lastFit = island.fit
        fitting = true
        delay(FITTING_MS)
        fitting = false
    }
    var musicOn by remember { mutableStateOf(false) }
    var quietOn by remember { mutableStateOf(false) }
    var callFrom by remember { mutableLongStateOf(0L) }
    var playing by remember { mutableStateOf(true) }
    var moments by remember { mutableStateOf(emptyList<IslandMoment>()) }
    var expanded by remember { mutableStateOf(false) }
    var cardUp by remember { mutableStateOf(false) }
    var held by remember { mutableStateOf<Pair<IslandKind, IslandMoment?>?>(null) }
    // The clock the island shows by: on the second for a call, and as each moment ends.
    val now by produceState(System.currentTimeMillis(), moments, callFrom) {
        while (true) {
            value = System.currentTimeMillis()
            val next = listOfNotNull(
                IslandRules.nextChange(moments, value),
                if (callFrom > 0L) value + 1000L - Math.floorMod(value - callFrom, 1000L) else null,
            ).minOrNull() ?: break
            delay((next - value).coerceAtLeast(16L))
        }
    }
    val call = if (callFrom > 0L) {
        NotificationDots.Live("demo", "", "Mum", callFrom, countDown = false, call = true, shown = null, postedAt = callFrom, open = null)
    } else {
        null
    }
    val picked = IslandRules.pick(island.copy(always = island.always || fitting), musicOn && playing, call != null, moments, now)
    val showing = if (expanded) held ?: picked else picked
    LaunchedEffect(showing?.first) { if (showing?.first?.opens != true) expanded = false }
    val music = Playing(
        packageName = "", title = "Midnight City", artist = "M83", art = null, playing = playing,
        position = 84_000L, positionAt = SystemClock.elapsedRealtime(), speed = 1f, duration = 243_000L, canSkip = true, canGoBack = true,
    )
    val kind = showing?.first
    val state = IslandState(
        kind = kind,
        moment = showing?.second,
        music = music,
        musicColour = Color(0xFFFF5E8A),
        live = call,
        liveText = call?.let { IslandRules.clock(it.base, false, now) }.orEmpty(),
        level = 64,
        status = if (quietOn) listOf(NotchStatus.QUIET) else emptyList(),
        appName = if (kind == IslandKind.LIVE) "Phone" else "Messages",
        secondary = IslandRules.secondary(island, musicOn && playing, call != null, kind),
    )
    fun moment(kind: IslandKind) {
        expanded = false
        val at = System.currentTimeMillis()
        val added = when (kind) {
            IslandKind.NOTIFICATION -> IslandMoment(kind, at, title = "Sam", text = "Are we still on for tonight? I can bring the film if you sort the food.")
            IslandKind.RINGER -> IslandMoment(kind, at, text = if ((at / 1000) % 2 == 0L) "silent" else "vibrate")
            else -> IslandMoment(kind, at, level = 64)
        }
        moments = IslandRules.prune(moments + added, at)
    }
    val tall = geometry.cardTop + 180.dp

    Column(Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val scale = (maxWidth / geometry.width).coerceAtMost(1f)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(tall * scale)
                    .clip(corners(30.dp)),
            ) {
                Box(
                    Modifier.layout { measurable, _ ->
                        val placeable = measurable.measure(Constraints.fixed(geometry.width.roundToPx(), tall.roundToPx()))
                        layout((placeable.width * scale).toInt(), (placeable.height * scale).toInt()) {
                            placeable.placeWithLayer(0, 0) {
                                scaleX = scale
                                scaleY = scale
                                transformOrigin = TransformOrigin(0f, 0f)
                            }
                        }
                    },
                ) {
                    // An app under it: a backdrop and a few lines, so the island is seen over something.
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(palette.backdrop.first(), palette.backdrop.last()))))
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = geometry.statusBar + 14.dp)) {
                        listOf(0.9f, 0.6f, 0.75f).forEach { share ->
                            Box(Modifier.fillMaxWidth(share).height(16.dp).clip(corners(8.dp)).background(palette.onGlass.copy(alpha = 0.10f)))
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    // The status bar, as tall as this phone's, in the page's ink as a phone's is over
                    // an app: white over Dark's page, dark over Light's, where white would be lost.
                    val bar = palette.onGlass
                    Row(
                        Modifier.fillMaxWidth().height(geometry.statusBar).padding(horizontal = 22.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // In the phone's own sans, as a status bar is, whatever font the home screen is in.
                        Text("9:41", color = bar, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, fontFamily = FontFamily.SansSerif)
                        Spacer(Modifier.weight(1f))
                        Icon(Icons.Rounded.SignalCellularAlt, contentDescription = null, tint = bar, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Rounded.Wifi, contentDescription = null, tint = bar, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(5.dp))
                        Box(Modifier.size(width = 20.dp, height = 10.dp).clip(corners(3.dp)).background(bar.copy(alpha = 0.9f)))
                    }
                    Box(
                        Modifier.fillMaxWidth().offset(x = geometry.centreX - geometry.width / 2, y = geometry.top),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        IslandCompact(
                            state = state,
                            geometry = geometry,
                            expanded = expanded,
                            onTap = {
                                if (kind?.opens == true) {
                                    held = kind to showing.second
                                    expanded = true
                                    cardUp = true
                                }
                            },
                            onHold = {},
                        )
                    }
                    if (cardUp) {
                        IslandCard(
                            state = state,
                            geometry = geometry,
                            open = expanded,
                            from = geometry.root,
                            actions = IslandActions(
                                close = { expanded = false },
                                openNotification = { expanded = false },
                                openLive = { expanded = false },
                                openMusic = { expanded = false },
                                playPause = { playing = !playing },
                            ),
                            onClosed = {
                                cardUp = false
                                held = null
                            },
                        )
                    }
                    // The camera itself: a lens through a hole in the screen, over everything drawn.
                    if (geometry.hole > 0.dp) {
                        Canvas(Modifier.offset(geometry.holeX - geometry.hole / 2, geometry.holeY - geometry.hole / 2).size(geometry.hole)) {
                            drawCircle(Color(0xFF020203))
                            drawCircle(Color(0xFF16171D), radius = size.minDimension * 0.30f)
                            drawCircle(Color(0xFF0A0A0E), radius = size.minDimension * 0.20f)
                            drawCircle(Color.White.copy(alpha = 0.22f), radius = size.minDimension * 0.05f, center = Offset(size.width * 0.42f, size.height * 0.40f))
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            DemoChip("Music", musicOn) {
                musicOn = !musicOn
                expanded = false
            }
            DemoChip("Call", callFrom > 0L) {
                callFrom = if (callFrom > 0L) 0L else System.currentTimeMillis()
                expanded = false
            }
            DemoChip("Notification", false) { moment(IslandKind.NOTIFICATION) }
            DemoChip("Silent", false) { moment(IslandKind.RINGER) }
            DemoChip("Charger", false) { moment(IslandKind.PLUGGED) }
            DemoChip("Do Not Disturb", quietOn) { quietOn = !quietOn }
        }
    }
}

/** How long the pill stays in the preview after its size or position last changed. */
private const val FITTING_MS = 1_800L

/** A button under the preview: lit while what it stands for is going on. */
@Composable
private fun DemoChip(label: String, on: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    val fill by animateColorAsState(if (on) palette.accent else palette.onGlass.copy(alpha = 0.08f), tween(220), label = "demoChip")
    Text(
        label,
        color = if (on) inkOnTint(palette.accent) else palette.onGlass,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        modifier = Modifier
            .pressBounce(0.94f)
            .clip(RoundedCornerShape(50))
            .background(fill)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                tick()
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 9.dp),
    )
}
