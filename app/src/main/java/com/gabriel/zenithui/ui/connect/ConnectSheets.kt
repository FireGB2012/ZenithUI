package com.gabriel.zenithui.ui.connect

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.connect.ConnectSamples
import com.gabriel.zenithui.data.connect.ConnectStudio
import com.gabriel.zenithui.data.connect.DownloadState
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.common.shake
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalTheme
import com.gabriel.zenithui.ui.theme.glassOnGlass
import com.gabriel.zenithui.ui.theme.isGlass
import com.gabriel.zenithui.ui.theme.themePage
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

/**
 * A sheet over Connect, as iOS 26 floats one: a pane come up from the foot of the screen, inset a
 * little from its edges, over a dimmed page. Pulled down by its handle past a little way, or
 * thrown, it goes; let go short of that, it springs back.
 */
@Composable
internal fun ConnectSheet(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    tall: Boolean = false,
    /** What shows over the dim but under the sheet, coming and going with it: the apply screen's handoff. */
    above: (@Composable () -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduced = LocalMotionLevel.current.reduced
    val palette = LocalPalette.current
    val glass = LocalTheme.current.isGlass
    val shown = remember { MutableTransitionState(false) }
    shown.targetState = visible
    var pulled by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(visible) { if (visible) pulled = 0f }
    val away = with(LocalDensity.current) { 110.dp.toPx() }
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * (if (tall) 0.92f else 0.86f)
    val shape = RoundedCornerShape(34.dp)
    Box(modifier.fillMaxSize()) {
        AnimatedVisibility(
            visibleState = shown,
            enter = fadeIn(tween(if (reduced) 150 else 260)),
            exit = fadeOut(tween(if (reduced) 150 else 220)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (palette.dark) 0.5f else 0.32f))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onDismiss() },
            )
        }
        above?.invoke()
        AnimatedVisibility(
            visibleState = shown,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = if (reduced) fadeIn(tween(150)) else slideInVertically(spring(dampingRatio = 0.86f, stiffness = 300f)) { it } + fadeIn(tween(160)),
            exit = if (reduced) {
                fadeOut(tween(150))
            } else {
                slideOutVertically(tween(300, easing = CubicBezierEasing(0.5f, 0f, 0.75f, 0f))) { it } + fadeOut(tween(260))
            },
        ) {
            Column(
                Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(8.dp)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .heightIn(max = maxHeight)
                    .offset { IntOffset(0, pulled.roundToInt()) }
                    .then(
                        if (glass) {
                            Modifier
                                .clip(shape)
                                .background(if (palette.dark) Color(0xFF17171F) else Color(0xFFF9F9FB))
                                .glassOnGlass(shape, palette.dark)
                        } else {
                            Modifier.themePage(palette, shape).border(1.dp, palette.edge, shape)
                        },
                    )
                    // Taps on it are its own, never the dim's behind it.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
            ) {
                // The handle, which the sheet is pulled down by.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(26.dp)
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta -> pulled = (pulled + delta).coerceAtLeast(0f) },
                            onDragStopped = { velocity ->
                                if (pulled > away || velocity > 1600f) {
                                    onDismiss()
                                } else {
                                    animate(pulled, 0f, animationSpec = spring(dampingRatio = 0.8f, stiffness = 420f)) { value, _ -> pulled = value }
                                }
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .width(40.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(palette.onGlass.copy(alpha = 0.22f)),
                    )
                }
                Column(
                    Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    content = content,
                )
            }
        }
    }
}

/** A sheet's head: a small word over its title, and a way out. */
@Composable
internal fun SheetHead(overline: String, title: String, onClose: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Overline(overline)
            Text(title, color = palette.onGlass, fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp)
        }
        RoundGlassButton(Icons.Rounded.Close, "Close", size = 36.dp) { onClose() }
    }
}

/** A field's label over it. */
@Composable
private fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text, color = LocalPalette.current.onGlassMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = modifier)
}

/** A box to type in, in the page's quiet glass, outlined in the accent while typing and in red when something is missing. */
@Composable
internal fun InputBox(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    lines: Int = 1,
    error: Boolean = false,
) {
    val palette = LocalPalette.current
    val accent = LocalConnectAccent.current
    val shape = RoundedCornerShape(if (lines > 1) 18.dp else 50.dp)
    Box(
        modifier
            .fillMaxWidth()
            .heightIn(min = if (lines > 1) (22 * lines + 28).dp else 50.dp)
            .clip(shape)
            .background(quietFill())
            .border(if (error) 1.5.dp else 1.dp, if (error) palette.destructive else quietEdge(), shape)
            .padding(horizontal = 16.dp, vertical = if (lines > 1) 14.dp else 0.dp),
        contentAlignment = if (lines > 1) Alignment.TopStart else Alignment.CenterStart,
    ) {
        if (value.isEmpty()) {
            Text(placeholder, color = palette.onGlassMuted, fontSize = 16.sp, lineHeight = 22.sp)
        }
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = lines == 1,
            minLines = lines,
            textStyle = TextStyle(color = palette.onGlass, fontSize = 16.sp, lineHeight = 22.sp),
            cursorBrush = SolidColor(accent),
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * The Creator Studio: what Zenith has on its home screen now, captured, with what goes in it, a
 * name, a line about it, up to five tags, and whether it goes on Discover or only to a link.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun StudioSheet(state: ConnectState, handle: String) {
    val palette = LocalPalette.current
    ConnectSheet(visible = state.studioOpen, onDismiss = state::closeStudio, tall = true) {
        SheetHead("Creator Studio", "Publish a setup", onClose = state::closeStudio)
        Spacer(Modifier.height(16.dp))
        Capture(state)
        Spacer(Modifier.height(20.dp))
        FieldLabel("Name")
        Spacer(Modifier.height(8.dp))
        InputBox(
            value = state.studioName,
            onChange = {
                state.studioName = it.take(ConnectStudio.MAX_NAME)
                state.nameError = false
            },
            placeholder = "Name your setup",
            error = state.nameError,
            modifier = Modifier.shake(state.nameShake),
        )
        AnimatedVisibility(visible = state.nameError) {
            Text(
                ConnectStudio.nameProblem("") ?: "",
                color = palette.destructive,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        FieldLabel("Description")
        Spacer(Modifier.height(8.dp))
        InputBox(
            value = state.studioAbout,
            onChange = { state.studioAbout = it.take(280) },
            placeholder = "What makes it yours? Widgets, fonts, the vibe.",
            lines = 3,
        )
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            FieldLabel("Tags", Modifier.weight(1f))
            Text(
                "${state.studioTags.size} / ${ConnectStudio.MAX_TAGS}",
                color = palette.onGlassMuted,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.shake(state.tagShake),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier
                .padding(horizontal = 0.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ConnectStudio.TAGS.forEach { tag ->
                ToggleChip(tag, on = tag in state.studioTags, showMark = false) { state.toggleTag(tag) }
            }
        }
        Spacer(Modifier.height(16.dp))
        FieldLabel("Visibility")
        Spacer(Modifier.height(8.dp))
        Segmented(listOf("Public", "Unlisted"), chosen = if (state.studioPublic) 0 else 1) { state.studioPublic = it == 0 }
        Spacer(Modifier.height(22.dp))
        val busy = state.publish == DownloadState.BUSY
        val progress by produceState(0f, busy, state.publishStartedAt) {
            value = 0f
            while (busy) {
                withFrameMillis { }
                value = ((state.clock() - state.publishStartedAt).toFloat() / ConnectStudio.PUBLISH_MS).coerceIn(0f, 1f)
                if (value >= 1f) break
            }
        }
        AccentButton(
            ConnectStudio.cta(state.studioPublic, state.publish),
            icon = if (state.publish == DownloadState.DONE) Icons.Rounded.Check else Icons.Rounded.Upload,
            progress = if (busy) progress else null,
            done = state.publish == DownloadState.DONE,
            height = 56.dp,
            modifier = Modifier.fillMaxWidth(),
        ) { state.publish(handle) }
    }
}

/** What was captured from Zenith: a little phone with a light running down it as it is read, and what goes in. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Capture(state: ConnectState) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val accent = LocalConnectAccent.current
    val clock = remember(state.studioRun) { LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm", Locale.US)) }
    val scan = remember { Animatable(1f) }
    LaunchedEffect(state.studioRun) {
        if (reduced) return@LaunchedEffect
        scan.snapTo(0f)
        scan.animateTo(1f, tween(1200, delayMillis = 200, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .connectCard(RoundedCornerShape(22.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 84.dp, height = 170.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF07070D))
                .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(16.dp))
                .padding(3.dp),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(13.dp))
                    .wall("afterglow")
                    .padding(start = 7.dp, end = 7.dp, top = 12.dp, bottom = 7.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(clock, color = Color(0xFFF6F6F9), fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 13.sp)
                Box(Modifier.fillMaxWidth().height(20.dp).clip(RoundedCornerShape(7.dp)).background(Color.White.copy(alpha = 0.18f)))
                MiniGrid(Color.White.copy(alpha = 0.22f))
                Spacer(Modifier.weight(1f))
                Box(Modifier.fillMaxWidth().height(14.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.18f)))
            }
            // The light that reads the screen, top to bottom.
            Canvas(Modifier.fillMaxSize()) {
                val p = scan.value
                if (p >= 1f) return@Canvas
                val y = size.height * p
                drawRect(
                    Brush.verticalGradient(
                        listOf(accent.copy(alpha = 0f), accent.copy(alpha = 0.55f), accent.copy(alpha = 0f)),
                        startY = y - 24f,
                        endY = y + 6f,
                    ),
                    topLeft = Offset(0f, (y - 24f).coerceAtLeast(0f)),
                    size = androidx.compose.ui.geometry.Size(size.width, 30f),
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Overline("Captured from Zenith", colour = liveGreen())
            Text("Home screen · 3 pages · just now", color = palette.onGlassMuted, fontSize = 13.sp)
            Spacer(Modifier.height(2.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ConnectStudio.PARTS.forEachIndexed { index, part ->
                    ToggleChip(
                        part,
                        on = part in state.studioParts,
                        height = 34.dp,
                        modifier = Modifier.appearIn(delayMillis = 1250 + 70 * index, fromScale = 0.9f, riseFrom = 12f, replay = state.studioRun),
                    ) { state.togglePart(part) }
                }
            }
        }
    }
}

/** Eight little icons in two rows, as a tiny home screen has. */
@Composable
internal fun MiniGrid(colour: Color, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                repeat(4) {
                    Box(Modifier.weight(1f).height(12.dp).clip(RoundedCornerShape(4.dp)).background(colour))
                }
            }
        }
    }
}

/** Two or more choices side by side on one pill, the chosen one on a lifted capsule that slides across. */
@Composable
internal fun Segmented(options: List<String>, chosen: Int, modifier: Modifier = Modifier, onPick: (Int) -> Unit) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val tick = rememberTick()
    val place = remember { Animatable(chosen.toFloat()) }
    LaunchedEffect(chosen) {
        if (reduced) place.snapTo(chosen.toFloat()) else place.animateTo(chosen.toFloat(), spring(dampingRatio = 0.8f, stiffness = 420f))
    }
    val lifted = if (palette.dark) Color.White.copy(alpha = 0.16f) else Color.White
    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(50))
            .background(quietFill())
            .padding(4.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width / options.size
            drawRoundRect(
                lifted,
                topLeft = Offset(place.value * w, 0f),
                size = androidx.compose.ui.geometry.Size(w, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(size.height / 2f),
            )
        }
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { index, option ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton) {
                            if (index != chosen) {
                                tick()
                                onPick(index)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        option,
                        color = if (index == chosen) palette.onGlass else palette.onGlassMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

/** The colour Connect wears for its buttons and highlights: Zenith's own blue, or one of four of the prototype's. */
@Composable
internal fun AccentSheet(state: ConnectState) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    ConnectSheet(visible = state.accentOpen, onDismiss = { state.accentOpen = false }) {
        SheetHead("Look", "Accent color", onClose = { state.accentOpen = false })
        Spacer(Modifier.height(10.dp))
        Text(
            "Connect wears it for its buttons and highlights. Zenith's own colours stay as they are.",
            color = palette.onGlassMuted,
            fontSize = 14.sp,
            lineHeight = 19.sp,
        )
        Spacer(Modifier.height(20.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            ConnectSamples.accents.forEach { choice ->
                val colour = choice.colour?.let { Color(it) } ?: palette.accent
                val on = choice == state.accent
                Column(
                    Modifier
                        .weight(1f)
                        .pressBounce(0.9f)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, role = Role.RadioButton) {
                            tick()
                            state.chooseAccent(choice)
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(54.dp)
                            .border(2.5.dp, if (on) palette.onGlass else Color.Transparent, CircleShape)
                            .padding(5.dp)
                            .clip(CircleShape)
                            .background(colour)
                            .popOn(on),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (on) Icon(Icons.Rounded.Check, contentDescription = null, tint = inkOn(colour), modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        choice.name,
                        color = if (on) palette.onGlass else palette.onGlassMuted,
                        fontSize = 12.sp,
                        fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
            }
        }
        Spacer(Modifier.height(22.dp))
        AccentButton("Done", height = 52.dp, modifier = Modifier.fillMaxWidth()) { state.accentOpen = false }
    }
}
