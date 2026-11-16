package com.gabriel.zenithui.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.IconStyles
import com.gabriel.zenithui.data.WidgetLooks
import com.gabriel.zenithui.ui.common.AppIcon
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.common.LocalIconStyle
import com.gabriel.zenithui.ui.theme.corners
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.wallPaneInk

/** What lies on the card under its words, in their ink (see [wallPaneInk]): faint, and when chosen less so. */
private val fill: Color @Composable @ReadOnlyComposable get() = wallPaneInk().copy(alpha = 0.13f)
private val fillStrong: Color @Composable @ReadOnlyComposable get() = wallPaneInk().copy(alpha = 0.22f)

/**
 * Customise: a card of glass at the foot of the home screen, with the home screen itself above it
 * changing as you choose. How the icons are drawn, how the widgets are, larger icons with no names
 * under them, a dimmer wallpaper, and the ways on to the wallpapers, the bedside clock, How It Works
 * and Settings. It takes the place of the Customise sheet, and is reached from the brush in edit
 * mode's bar.
 *
 * It stands on the wallpaper, so it is the same clear glass in Light and in Dark. A touch on the home
 * screen above it, or Back, puts it away.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CustomiseCard(
    visible: Boolean,
    iconStyle: String,
    widgetLook: String,
    /** Names are hidden and the icons are as large as the grid lets them be. */
    large: Boolean,
    dim: Boolean,
    /** An app on the home screen, for the three styles to be shown on; none on an empty one. */
    sampleKey: String?,
    iconShape: Shape,
    /** Shown only while they apply: the new look not yet on, Zenith not yet the home app. */
    showNewLook: Boolean,
    showMakeHome: Boolean,
    onIconStyle: (String) -> Unit,
    onWidgetLook: (String) -> Unit,
    onLarge: (Boolean) -> Unit,
    onDim: (Boolean) -> Unit,
    onWallpaper: () -> Unit,
    onClock: () -> Unit,
    onHelp: () -> Unit,
    onSettings: () -> Unit,
    onNewLook: () -> Unit,
    onMakeHome: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val reduced = LocalMotionLevel.current.reduced
    val up = remember { Animatable(0f) }
    var there by remember { mutableStateOf(false) }
    LaunchedEffect(visible) {
        if (visible) {
            there = true
            if (reduced) up.animateTo(1f, tween(Motion.REDUCED_MS)) else up.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 340f))
        } else {
            if (reduced) up.animateTo(0f, tween(Motion.REDUCED_MS)) else up.animateTo(0f, spring(dampingRatio = 1f, stiffness = 420f))
            there = false
        }
    }
    if (!there && !visible) return

    BackHandler(enabled = visible) { onDismiss() }
    Box(modifier.fillMaxSize()) {
        // A touch on the home screen above the card puts the card away, and does nothing else.
        if (visible) {
            Box(
                Modifier.fillMaxSize().pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false).consume()
                        onDismiss()
                    }
                },
            )
        }
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(10.dp)
                .widthIn(max = 520.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    // Up from the foot of the screen, or simply in with movement reduced.
                    alpha = (up.value * 3f).coerceIn(0f, 1f)
                    if (!reduced) translationY = size.height * 0.6f * (1f - up.value)
                }
                .bodyWithoutWallpaper(40.dp)
                .glass(corners(40.dp), strength = 1.2f, solid = 0.5f, tracking = { up.value })
                // A touch on the card between its controls is the card's own.
                .pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume() } }
                .semantics { paneTitle = "Customise" }
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 18.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RoundToggle(Icons.Rounded.LightMode, "Dim the wallpaper", on = dim) { onDim(!dim) }
                Text(
                    "Customise",
                    color = wallPaneInk(),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    maxLines = 1,
                )
                RoundToggle(if (large) Icons.Rounded.GridView else Icons.Rounded.Apps, "Larger icons, with no names", on = large) { onLarge(!large) }
            }

            // Each style on a real icon from this home screen, as it will look.
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp, start = 6.dp, end = 6.dp),
                horizontalArrangement = Arrangement.SpaceAround,
            ) {
                for (style in listOf(IconStyles.ORIGINAL, IconStyles.GLASS, IconStyles.TINTED)) {
                    StyleTile(
                        style = style,
                        chosen = IconStyles.of(iconStyle) == style,
                        sampleKey = sampleKey,
                        shape = iconShape,
                        onClick = { onIconStyle(style) },
                    )
                }
            }

            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Widgets", color = wallPaneInk(), fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Row(Modifier.clip(corners(15.dp)).background(fill).padding(3.dp)) {
                    for (look in listOf(WidgetLooks.COLOUR, WidgetLooks.GLASS)) {
                        val on = WidgetLooks.of(widgetLook) == look
                        val tick = rememberTick()
                        Box(
                            Modifier
                                .clip(corners(12.dp))
                                .background(if (on) fillStrong else Color.Transparent)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton) {
                                    if (!on) {
                                        tick()
                                        onWidgetLook(look)
                                    }
                                }
                                .semantics { stateDescription = if (on) "Chosen" else "Not chosen" }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(WidgetLooks.label(look), color = wallPaneInk(), fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }

            // The ways on, all of them in sight: they wrap onto a second line rather than run off
            // the card's edge, where the last of them was cut in half. They put the card away as
            // they go.
            FlowRow(
                Modifier.fillMaxWidth().padding(top = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (showNewLook) Capsule("Use the New Look", onNewLook)
                if (showMakeHome) Capsule("Make Zenith Your Home Screen", onMakeHome)
                Capsule("Wallpaper", onWallpaper)
                Capsule("Bedside Clock", onClock)
                Capsule("How It Works", onHelp)
                Capsule("All Settings", onSettings)
            }
        }
    }
}

/** A round button that is either on (the words' ink, with the symbol cut out of it) or off. */
@Composable
private fun RoundToggle(icon: ImageVector, name: String, on: Boolean, onClick: () -> Unit) {
    val tick = rememberTick()
    val ink = wallPaneInk()
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (on) ink else fill)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Switch) {
                tick()
                onClick()
            }
            .semantics { stateDescription = if (on) "On" else "Off" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = name, tint = if (on) (if (ink.luminance() > 0.5f) Color.Black else Color.White) else ink, modifier = Modifier.size(22.dp))
    }
}

/** One icon style: a real icon drawn that way, and its name, in a capsule when it is the one chosen. */
@Composable
private fun StyleTile(style: String, chosen: Boolean, sampleKey: String?, shape: Shape, onClick: () -> Unit) {
    val tick = rememberTick()
    Column(
        Modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton) {
                if (!chosen) {
                    tick()
                    onClick()
                }
            }
            .semantics { stateDescription = if (chosen) "Chosen" else "Not chosen" },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(54.dp), contentAlignment = Alignment.Center) {
            if (sampleKey != null) {
                CompositionLocalProvider(LocalIconStyle provides style) {
                    AppIcon(key = sampleKey, label = "", size = 54.dp, shape = shape)
                }
            } else {
                Box(Modifier.size(54.dp).clip(shape).background(fillStrong))
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            IconStyles.label(style),
            color = wallPaneInk(),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier
                .clip(corners(12.dp))
                .background(if (chosen) fillStrong else Color.Transparent)
                .padding(horizontal = 11.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun Capsule(words: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(corners(16.dp))
            .background(fill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(words, color = wallPaneInk(), fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
