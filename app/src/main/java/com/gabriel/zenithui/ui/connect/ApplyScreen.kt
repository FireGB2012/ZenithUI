package com.gabriel.zenithui.ui.connect

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.connect.ApplyStage
import com.gabriel.zenithui.data.connect.ConnectApply
import com.gabriel.zenithui.data.connect.ConnectSetup
import com.gabriel.zenithui.data.connect.StepState
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.rememberHaptics
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.common.wordsChange
import com.gabriel.zenithui.ui.theme.LocalLite
import com.gabriel.zenithui.ui.theme.LocalPalette
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

// One-tap apply: Connect hands a setup to Zenith, in three steps you can watch or leave. Over
// the dimmed setup page, Connect's icon passes it along a beam to Zenith's; the sheet under them
// counts it through. Everything on it is worked out from the time Apply was pressed (see
// ConnectApply.stage), so closed and opened again it is where it got to.

/** How the progress bar fills, as the prototype's does. */
private val BAR_EASE = CubicBezierEasing(0.4f, 0.1f, 0.3f, 1f)

/** How a beam dot crosses, and the sheen on Open Zenith: CSS's ease-in-out. */
private val EASE_IN_OUT = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

/** How long the burst over Zenith's icon lasts, and how many bits it throws. */
private const val BURST_MS = 950f
private const val BURST_BITS = 28

@Composable
internal fun ApplyScreen(state: ConnectState, onOpenZenith: () -> Unit) {
    val reduced = LocalMotionLevel.current.reduced
    val lite = LocalLite.current
    val palette = LocalPalette.current
    val accent = LocalConnectAccent.current
    val haptics = rememberHaptics()
    val open = state.applying
    // The setup on show, kept while the sheet goes.
    val last = remember { arrayOfNulls<ConnectSetup>(1) }
    if (open != null) last[0] = open
    val setup = open ?: last[0] ?: return

    // Milliseconds since Apply, counted each frame while the sheet is up and something is still to come.
    val elapsed: State<Long> = produceState(state.applyElapsed(), open != null, state.applyStartedAt) {
        value = state.applyElapsed()
        while (open != null && value <= ConnectApply.DONE_MS) {
            withFrameMillis { }
            value = state.applyElapsed()
        }
    }
    // What the sheet says changes a few times only, so only those changes recompose it; the bar
    // reads the time as it draws.
    val stage by remember(setup) { derivedStateOf { ConnectApply.stage(elapsed.value, setup).copy(bar = 0f) } }

    // Finished while you watched: Zenith's icon pops, a burst comes off it, and the phone confirms.
    // Not when you come back to it already done.
    val watching = remember(state.applyStartedAt) { booleanArrayOf(false) }
    if (!stage.done && open != null) watching[0] = true
    val burst = remember { Animatable(1f) }
    val pop = remember { Animatable(1f) }
    LaunchedEffect(stage.done, state.applyStartedAt) {
        if (!stage.done || !watching[0]) return@LaunchedEffect
        watching[0] = false
        haptics.confirm()
        if (reduced) return@LaunchedEffect
        launchPop(pop)
        if (!lite) {
            burst.snapTo(0f)
            burst.animateTo(1f, tween(BURST_MS.toInt(), easing = LinearEasing))
        }
    }

    ConnectSheet(
        visible = open != null,
        onDismiss = state::leaveApply,
        above = {
            AnimatedVisibility(
                visible = open != null,
                enter = if (reduced) {
                    fadeIn(tween(Motion.REDUCED_MS))
                } else {
                    fadeIn(tween(450, delayMillis = 120, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f))) +
                        scaleIn(spring(dampingRatio = 0.8f, stiffness = 260f), initialScale = 0.96f) +
                        slideInVertically(spring(dampingRatio = 0.8f, stiffness = 260f)) { -it / 12 }
                },
                exit = fadeOut(tween(if (reduced) Motion.REDUCED_MS else 200)),
            ) {
                Handoff(stage, pop, burst, applyStartedAt = state.applyStartedAt)
            }
        },
    ) {
        Overline(
            if (stage.done) "Applied" else "One-tap apply",
            colour = if (stage.done) liveGreen() else accent,
        )
        Spacer(Modifier.height(6.dp))
        Text(setup.name, color = palette.onGlass, fontSize = 24.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(6.dp))
        AnimatedContent(targetState = stage.done, transitionSpec = { wordsChange(reduced) }, label = "applySub") { done ->
            Text(
                if (done) "${setup.name} is live on your Zenith home screen." else "Zenith is building it now. You can leave. It finishes in the background.",
                color = palette.onGlassMuted,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        }
        Spacer(Modifier.height(16.dp))
        val titles = ConnectApply.steps(setup.kind)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            StepRow(stage.steps[0], titles[0]) {
                Text(ConnectApply.assets(setup, state.applyLayers), color = palette.onGlassMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            StepRow(stage.steps[1], titles[1]) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SmallSwatches(setup.palette, shown = stage.swatches)
                    Spacer(Modifier.width(8.dp))
                    Text("Accent + 4 tones", color = palette.onGlassMuted, fontSize = 13.sp, maxLines = 1)
                }
            }
            StepRow(stage.steps[2], titles[2]) {
                Text(stage.line, color = palette.onGlassMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(6.dp))
                ProgressLine(
                    progress = { BAR_EASE.transform(ConnectApply.stage(elapsed.value, setup).bar) },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(quietFill())
                .border(1.dp, quietEdge(), RoundedCornerShape(18.dp))
                .padding(start = 14.dp, top = 10.dp, end = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(quietFill()), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = palette.onGlass, modifier = Modifier.size(17.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text("Your old layout is saved in Vault.", color = palette.onGlass.copy(alpha = 0.8f), fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            GhostButton("Undo", height = 36.dp) { state.undoApply() }
        }
        Spacer(Modifier.height(16.dp))
        AccentButton(
            "Open Zenith",
            icon = Icons.AutoMirrored.Rounded.OpenInNew,
            enabled = stage.done,
            height = 56.dp,
            modifier = Modifier
                .fillMaxWidth()
                .sheen(on = stage.done && !reduced && !lite),
        ) { onOpenZenith() }
        Spacer(Modifier.height(4.dp))
        val tick = rememberTick()
        Box(
            Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(RoundedCornerShape(50))
                .clickable(role = Role.Button) {
                    tick()
                    state.backToDiscover(reduced)
                },
            contentAlignment = Alignment.Center,
        ) {
            Text("Back to Discover", color = palette.onGlass.copy(alpha = 0.72f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Zenith's icon pops: up a seventh and back, as it takes the setup. */
private suspend fun launchPop(pop: Animatable<Float, *>) {
    pop.snapTo(1f)
    pop.animateTo(
        1f,
        keyframes {
            durationMillis = 700
            1.14f at 280 using CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f)
            1f at 700
        },
    )
}

/**
 * Connect's icon, a beam, and Zenith's: dots run along the beam while the setup goes across, and
 * it lights up green when it is in. The burst comes off Zenith's icon then.
 */
@Composable
private fun Handoff(stage: ApplyStage, pop: Animatable<Float, *>, burst: Animatable<Float, *>, applyStartedAt: Long) {
    val palette = LocalPalette.current
    val accent = LocalConnectAccent.current
    val reduced = LocalMotionLevel.current.reduced
    val green = liveGreen()
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // Where Zenith's icon's middle is, and where the burst's canvas is, both from the root.
    var zAt by remember { mutableStateOf(Offset.Unspecified) }
    var canvasAt by remember { mutableStateOf(Offset.Zero) }
    val lit by animateFloatAsState(if (stage.done) 1f else 0f, tween(400), label = "beamLit")
    val dots = rememberInfiniteTransition(label = "beam")
    val travel by dots.animateFloat(0f, 1f, infiniteRepeatable(tween(1500, easing = LinearEasing)), label = "beamTravel")
    val label = palette.onGlass.copy(alpha = 0.82f)
    val bits = remember(applyStartedAt) { burstBits(applyStartedAt) }
    val bitColours = listOf(accent, green, Color(0xFFF6F6F9))
    Box(
        Modifier
            .fillMaxWidth()
            .padding(top = top + 24.dp)
            .height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.Top) {
            HandoffApp("Zenith Connect", label) {
                Box(
                    Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFF2A2170), Color(0xFF0B0A24))))
                        .border(1.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(22.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    ConnectMark(Color.White, Modifier.size(36.dp))
                }
            }
            Canvas(Modifier.size(width = 80.dp, height = 72.dp)) {
                val y = size.height / 2f
                val dash = 6.dp.toPx()
                val thick = 2.dp.toPx()
                val from = 2.dp.toPx()
                val to = size.width - 2.dp.toPx()
                // The beam: dashed while it waits and runs, a solid green line with a glow once it is in.
                val off = palette.onGlass.copy(alpha = 0.35f * (1f - lit))
                var x = from
                while (x < to) {
                    drawRect(off, topLeft = Offset(x, y - thick / 2f), size = Size(minOf(dash, to - x), thick))
                    x += dash * 2f
                }
                if (lit > 0f) {
                    drawRoundRect(green.copy(alpha = 0.35f * lit), topLeft = Offset(from, y - thick * 2.5f), size = Size(to - from, thick * 5f), cornerRadius = CornerRadius(thick * 2.5f))
                    drawRoundRect(green.copy(alpha = lit), topLeft = Offset(from, y - thick / 2f), size = Size(to - from, thick), cornerRadius = CornerRadius(thick / 2f))
                }
                if (!stage.done && !reduced) {
                    val r = 6.dp.toPx()
                    val span = 68.dp.toPx()
                    for (i in 0 until 3) {
                        val phase = ((travel - i / 3f) % 1f + 1f) % 1f
                        val along = EASE_IN_OUT.transform(phase)
                        val alpha = when {
                            phase < 0.15f -> phase / 0.15f
                            phase > 0.85f -> (1f - phase) / 0.15f
                            else -> 1f
                        }
                        val centre = Offset(r + along * span, y)
                        drawCircle(accent.copy(alpha = 0.3f * alpha), radius = r * 1.9f, center = centre)
                        drawCircle(accent.copy(alpha = alpha), radius = r, center = centre)
                    }
                }
            }
            HandoffApp("Zenith", label) {
                Box(
                    Modifier
                        .size(72.dp)
                        .onGloballyPositioned { zAt = it.positionInRoot() + Offset(it.size.width / 2f, it.size.height / 2f) }
                        .graphicsLayer {
                            scaleX = pop.value
                            scaleY = pop.value
                        }
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (palette.dark) Color(0xFFF6F6F9) else Color(0xFF16161E)),
                    contentAlignment = Alignment.Center,
                ) {
                    Text("Z", color = if (palette.dark) Color(0xFF0B0C12) else Color(0xFFF6F6F9), fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        // The burst, over everything here.
        Canvas(
            Modifier
                .matchParentSize()
                .onGloballyPositioned { canvasAt = it.positionInRoot() },
        ) {
            val e = burst.value
            if (e >= 1f || zAt == Offset.Unspecified) return@Canvas
            val origin = zAt - canvasAt
            // As the prototype's: moved a step each frame, slowing sideways and falling.
            val frames = e * BURST_MS / (1000f / 60f)
            val drift = (1f - 0.985f.pow(frames)) / (1f - 0.985f)
            val unit = 1.dp.toPx()
            bits.forEachIndexed { index, bit ->
                val x = origin.x + bit.vx * drift * unit
                val y = origin.y + (bit.vy * frames + 0.07f * frames * (frames - 1f) / 2f) * unit
                drawCircle(
                    bitColours[index % 3].copy(alpha = 1f - e),
                    radius = bit.r * (1f - e * 0.5f) * unit,
                    center = Offset(x, y),
                )
            }
        }
    }
}

/** One of the two icons, with its name under it. */
@Composable
private fun HandoffApp(name: String, colour: Color, tile: @Composable () -> Unit) {
    Column(Modifier.width(104.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        tile()
        Spacer(Modifier.height(10.dp))
        Text(name, color = colour, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
    }
}

/** A bit of the burst: which way it flies, in dp a frame, and how big it is. */
private class BurstBit(val vx: Float, val vy: Float, val r: Float)

/** The burst's bits, the same each time for one apply. */
private fun burstBits(seed: Long): List<BurstBit> {
    val random = java.util.Random(seed)
    return List(BURST_BITS) {
        val angle = random.nextFloat() * 2f * PI.toFloat()
        val speed = 1.8f + random.nextFloat() * 3.4f
        BurstBit(cos(angle) * speed, sin(angle) * speed - 1.2f, 1.5f + random.nextFloat() * 2.4f)
    }
}

/**
 * A step of the apply: faint while it waits, outlined in the accent with a spinner while it runs,
 * a green tick once done. [detail] is what goes under its title.
 */
@Composable
private fun StepRow(step: StepState, titles: Pair<String, String>, detail: @Composable () -> Unit) {
    val palette = LocalPalette.current
    val accent = LocalConnectAccent.current
    val shape = RoundedCornerShape(18.dp)
    val faded by animateFloatAsState(if (step == StepState.WAITING) 0.5f else 1f, tween(350), label = "stepFade")
    val edge = when (step) {
        StepState.RUNNING -> accent.copy(alpha = 0.45f)
        else -> quietEdge()
    }
    Row(
        Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = faded }
            .clip(shape)
            .background(quietFill())
            .border(1.dp, edge, shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepMark(step)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (step == StepState.DONE) titles.second else titles.first,
                color = palette.onGlass,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            detail()
        }
    }
}

/** The round mark at a step's start: empty, spinning, or ticked. */
@Composable
private fun StepMark(step: StepState) {
    val palette = LocalPalette.current
    val accent = LocalConnectAccent.current
    val green = liveGreen()
    val reduced = LocalMotionLevel.current.reduced
    val face = when (step) {
        StepState.WAITING -> quietFill()
        StepState.RUNNING -> accent.copy(alpha = 0.16f)
        StepState.DONE -> green.copy(alpha = 0.16f)
    }
    val tickScale by animateFloatAsState(
        if (step == StepState.DONE) 1f else 0.3f,
        if (reduced) tween(Motion.REDUCED_MS) else spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "tickScale",
    )
    Box(Modifier.size(32.dp).clip(CircleShape).background(face), contentAlignment = Alignment.Center) {
        if (step == StepState.RUNNING) {
            val turn = rememberInfiniteTransition(label = "spinner")
            val angle by turn.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "spin")
            Canvas(Modifier.size(17.dp)) {
                val stroke = 2.3.dp.toPx()
                rotate(if (reduced) 0f else angle) {
                    drawArc(
                        accent,
                        startAngle = -90f,
                        sweepAngle = 270f,
                        useCenter = false,
                        topLeft = Offset(stroke / 2f, stroke / 2f),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(stroke, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                    )
                }
            }
        }
        if (step == StepState.DONE) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = green,
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        scaleX = tickScale
                        scaleY = tickScale
                    },
            )
        }
        if (step == StepState.WAITING) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(palette.onGlass.copy(alpha = 0.3f)))
        }
    }
}

/** The setup's five colours, small, each popping in as it is put on. */
@Composable
private fun SmallSwatches(palette: List<Long>, shown: Int) {
    val reduced = LocalMotionLevel.current.reduced
    val ring = LocalPalette.current.onGlass.copy(alpha = 0.18f)
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        palette.take(5).forEachIndexed { index, colour ->
            val scale by animateFloatAsState(
                if (index < shown) 1f else 0f,
                if (reduced) tween(Motion.REDUCED_MS) else spring(dampingRatio = 0.5f, stiffness = 520f),
                label = "swatchIn",
            )
            Box(
                Modifier
                    .size(10.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(CircleShape)
                    .background(Color(colour))
                    .border(1.dp, ring, CircleShape),
            )
        }
    }
}

/**
 * A light that sweeps across a button now and then, as the prototype's Open Zenith has once it can
 * be pressed: across in the first half of each turn, then a rest.
 */
@Composable
private fun Modifier.sheen(on: Boolean): Modifier {
    if (!on) return this
    val turn = rememberInfiniteTransition(label = "sheen")
    val at by turn.animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(2600, easing = LinearEasing), initialStartOffset = StartOffset(400)),
        label = "sheenAt",
    )
    val shape = RoundedCornerShape(50)
    return drawWithContent {
        drawContent()
        val k = at / 0.55f
        if (k >= 1f) return@drawWithContent
        val x = size.width * (-1.2f + 2.4f * EASE_IN_OUT.transform(k))
        val outline = shape.createOutline(size, layoutDirection, this)
        val path = Path().apply { addOutline(outline) }
        clipPath(path) {
            drawRect(
                Brush.linearGradient(
                    listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0f)),
                    start = Offset(x + size.width * 0.3f, 0f),
                    end = Offset(x + size.width * 0.7f, size.height),
                ),
            )
        }
    }
}
