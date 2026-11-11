package com.gabriel.zenithui.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import com.gabriel.zenithui.ui.theme.glass

private val FILL = Color.White.copy(alpha = 0.13f)
private val FILL_STRONG = Color.White.copy(alpha = 0.22f)

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
                .glass(RoundedCornerShape(40.dp), strength = 1.2f, solid = 0.5f, tracking = { up.value })
                // A touch on the card between its controls is the card's own.
                .pointerInput(Unit) { awaitEachGesture { awaitFirstDown().consume() } }
                .semantics { paneTitle = "Customise" }
                .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 18.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RoundToggle(Icons.Rounded.LightMode, "Dim the wallpaper", on = dim) { onDim(!dim) }
                Text(
                    "Customise",
                    color = Color.White,
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
                Text("Widgets", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Row(Modifier.clip(RoundedCornerShape(15.dp)).background(FILL).padding(3.dp)) {
                    for (look in listOf(WidgetLooks.COLOUR, WidgetLooks.GLASS)) {
                        val on = WidgetLooks.of(widgetLook) == look
                        val tick = rememberTick()
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (on) FILL_STRONG else Color.Transparent)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton) {
                                    if (!on) {
                                        tick()
                                        onWidgetLook(look)
                                    }
                                }
                                .semantics { stateDescription = if (on) "Chosen" else "Not chosen" }
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                        ) {
                            Text(WidgetLooks.label(look), color = Color.White, fontSize = 13.5.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
            }

            // The ways on. They put the card away as they go.
            Row(
                Modifier.fillMaxWidth().padding(top = 14.dp).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
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

/** A round button that is either on (white, with a dark symbol) or off. */
@Composable
private fun RoundToggle(icon: ImageVector, name: String, on: Boolean, onClick: () -> Unit) {
    val tick = rememberTick()
    Box(
        Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (on) Color.White else FILL)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.Switch) {
                tick()
                onClick()
            }
            .semantics { stateDescription = if (on) "On" else "Off" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = name, tint = if (on) Color.Black else Color.White, modifier = Modifier.size(22.dp))
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
                Box(Modifier.size(54.dp).clip(shape).background(FILL_STRONG))
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            IconStyles.label(style),
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (chosen) FILL_STRONG else Color.Transparent)
                .padding(horizontal = 11.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun Capsule(words: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(FILL)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(words, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}
