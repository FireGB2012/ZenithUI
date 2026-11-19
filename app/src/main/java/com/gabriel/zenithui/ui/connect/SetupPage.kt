package com.gabriel.zenithui.ui.connect

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.automirrored.rounded._360
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.connect.ConnectApply
import com.gabriel.zenithui.data.connect.ConnectFeed
import com.gabriel.zenithui.data.connect.ConnectSetup
import com.gabriel.zenithui.data.connect.Layer
import com.gabriel.zenithui.data.connect.SetupKind
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.theme.LocalLite
import com.gabriel.zenithui.ui.theme.LocalPalette
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sin

/**
 * A setup's own page, grown out of the card it was opened from: its wallpaper soft behind
 * everything, its phone in 3D to spin with a finger, and what applying it puts on, with Apply.
 */
@Composable
internal fun SetupPage(state: ConnectState) {
    val reduced = LocalMotionLevel.current.reduced
    val open = state.detail
    // The setup on show, kept while its page closes.
    val last = remember { arrayOfNulls<ConnectSetup>(1) }
    if (open != null) last[0] = open
    val setup = open ?: last[0] ?: return
    val shown = remember { Animatable(0f) }
    LaunchedEffect(state.detailRun, open == null) {
        if (open != null) {
            if (reduced) {
                shown.animateTo(1f, tween(150))
            } else {
                shown.snapTo(0f)
                shown.animateTo(1f, spring(dampingRatio = 0.86f, stiffness = 240f))
            }
        } else {
            shown.animateTo(0f, if (reduced) tween(150) else tween(360, easing = CubicBezierEasing(0.5f, 0f, 0.75f, 0f)))
        }
    }
    val showing by remember { derivedStateOf { shown.value > 0.001f } }
    if (open == null && !showing) return

    val page = pageBrush()
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = shown.value
                val from = state.detailFrom
                val scale = if (reduced) 1f else 0.28f + 0.72f * p
                scaleX = scale
                scaleY = scale
                alpha = (p * 1.4f).coerceIn(0f, 1f)
                transformOrigin = if (from != null && size.width > 0f && size.height > 0f) {
                    TransformOrigin((from.center.x / size.width).coerceIn(0f, 1f), (from.center.y / size.height).coerceIn(0f, 1f))
                } else {
                    TransformOrigin(0.5f, 0.4f)
                }
                clip = true
                shape = RoundedCornerShape(((1f - p) * 32f).coerceAtLeast(0f).dp)
            }
            .background(page)
            // Nothing under the page takes a touch while it is up.
            .pointerInput(Unit) { detectTapGestures { } },
    ) {
        val spin = remember { Spin() }
        BackdropHost(
            modifier = Modifier.fillMaxSize(),
            page = {
                Ambient(setup)
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(state.detailScroll),
                ) {
                    Spacer(Modifier.height(WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 56.dp))
                    Stage(setup, state, spin)
                    RotateHint(spin, Modifier.align(Alignment.CenterHorizontally).appearIn(delayMillis = 380, fromScale = 0.96f, riseFrom = 24f, replay = state.detailRun, gentle = true))
                    Spacer(Modifier.height(16.dp))
                    SetupSheet(setup, state, spin)
                }
            },
            chrome = { TopBar(setup, state) },
        )
    }
}

/** The setup's wallpaper behind the top of its page, fading into the page as it goes down. */
@Composable
private fun Ambient(setup: ConnectSetup) {
    val page = pageColour()
    val light = !setup.dark
    val shade = Color(0xFF0B0C12)
    Box(
        Modifier
            .fillMaxWidth()
            .height(660.dp)
            .wall(setup.wall),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to shade.copy(alpha = if (light) 0.6f else 0.12f),
                        0.45f to shade.copy(alpha = if (light) 0.72f else 0.32f),
                        0.94f to page,
                    ),
                ),
        )
    }
}

/** Back, what this is, and share: floating over the page in glass. */
@Composable
private fun TopBar(setup: ConnectSetup, state: ConnectState) {
    val clipboard = LocalClipboardManager.current
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundGlassButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", size = 44.dp, floating = true) { state.closeSetup() }
        Spacer(Modifier.weight(1f))
        Box(
            Modifier
                .height(32.dp)
                .connectGlass(RoundedCornerShape(50))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Overline("3D preview", colour = palette.onGlass.copy(alpha = 0.78f))
        }
        Spacer(Modifier.weight(1f))
        RoundGlassButton(Icons.Rounded.IosShare, "Share this setup", size = 44.dp, floating = true) {
            state.share(setup) { link ->
                runCatching { clipboard.setText(AnnotatedString(link)) }.isSuccess
            }
        }
    }
}

/**
 * How the 3D phone is turned, in degrees about across (x) and up (y), and how it is moving: under
 * the finger, carrying on after it with what speed it was let go at, easing to one of the set
 * views when tapped, or swaying gently by itself once left alone a while (not in Lite, nor with
 * movement reduced).
 */
@Stable
private class Spin {
    var x by mutableFloatStateOf(8f)
    var y by mutableFloatStateOf(-24f)
    var vx = 0f
    var vy = 0f
    var dragging = false
    var target: Pair<Float, Float>? = null
    var touched = 0L
    var view = 0

    /** Bumped by anything that sets it moving, to wake it up from rest. */
    var pokes by mutableIntStateOf(0)

    fun reset(now: Long) {
        x = 8f
        y = -24f
        vx = 0f
        vy = 0f
        target = null
        view = 0
        touched = now - 1200L
    }

    fun next(now: Long) {
        view = (view + 1) % VIEWS.size
        target = VIEWS[view]
        touched = now
        pokes++
    }

    fun turnTo(at: Pair<Float, Float>, now: Long) {
        target = at
        touched = now
        pokes++
    }

    /** Whether it is still with nothing to carry it on. */
    fun resting(): Boolean = !dragging && target == null && vx == 0f && vy == 0f

    /** One frame, [dt] milliseconds after the last, at [now]. */
    fun step(now: Long, dt: Float, sway: Boolean) {
        val frames = (dt / 16.667f).coerceIn(0f, 4f)
        if (!dragging) {
            val to = target
            if (to != null) {
                val k = 1f - (1f - 0.12f).pow(frames)
                x += (to.first - x) * k
                y += (to.second - y) * k
                if (abs(to.first - x) + abs(to.second - y) < 0.3f) target = null
            } else {
                y += vy * frames
                x += vx * frames
                val keep = 0.93f.pow(frames)
                vy *= keep
                vx *= keep
                if (abs(vx) < 0.01f) vx = 0f
                if (abs(vy) < 0.01f) vy = 0f
                if (sway && now - touched > 2400L) {
                    val k = 1f - (1f - 0.018f).pow(frames)
                    y += ((-18f + sin(now / 1700f) * 16f) - y) * k
                    x += ((8f + sin(now / 2300f) * 4f) - x) * k
                }
            }
        }
        x = x.coerceIn(-35f, 35f)
        y = y.coerceIn(-80f, 80f)
    }

    companion object {
        val VIEWS = listOf(8f to -24f, 0f to 0f, 10f to 34f, -8f to -52f)
    }
}

/** The stage the phone stands on, which turns it under a finger. */
@Composable
private fun Stage(setup: ConnectSetup, state: ConnectState, spin: Spin) {
    val reduced = LocalMotionLevel.current.reduced
    val sway = !LocalLite.current && !reduced
    LaunchedEffect(state.detailRun) {
        spin.reset(state.clock())
        var last = -1L
        while (true) {
            if (spin.resting() && !sway) {
                // Nothing to move it: wait, without asking for frames, for something that does.
                val seen = spin.pokes
                snapshotFlow { spin.pokes }.first { it != seen }
                last = -1L
                continue
            }
            val step: (Long) -> Unit = { t ->
                val dt = if (last < 0L) 16.667f else (t - last).toFloat()
                last = t
                spin.step(state.clock(), dt, sway)
            }
            // Only swaying by itself, it is an animation with no end, which is how it asks for its
            // frames: whatever pauses those (a test does) pauses it.
            if (spin.resting()) withInfiniteAnimationFrameMillis(step) else withFrameMillis(step)
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(400.dp)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        spin.dragging = true
                        spin.target = null
                        spin.vx = 0f
                        spin.vy = 0f
                        spin.pokes++
                    },
                    onDragEnd = {
                        spin.dragging = false
                        spin.touched = state.clock()
                        spin.pokes++
                    },
                    onDragCancel = {
                        spin.dragging = false
                        spin.touched = state.clock()
                        spin.pokes++
                    },
                ) { change, amount ->
                    change.consume()
                    val dx = amount.x / density
                    val dy = amount.y / density
                    spin.y = (spin.y + dx * 0.5f).coerceIn(-80f, 80f)
                    spin.x = (spin.x - dy * 0.35f).coerceIn(-35f, 35f)
                    spin.vy = dx * 0.5f
                    spin.vx = -dy * 0.35f
                    spin.touched = state.clock()
                }
            }
            .pointerInput(Unit) { detectTapGestures { spin.next(state.clock()) } },
        contentAlignment = Alignment.Center,
    ) {
        // The soft shadow it stands over.
        Canvas(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 6.dp)
                .size(width = 200.dp, height = 30.dp),
        ) {
            drawOval(
                Brush.radialGradient(
                    listOf(Color.Black.copy(alpha = 0.5f), Color.Black.copy(alpha = 0f)),
                    center = center,
                    radius = size.width / 2f,
                ),
                topLeft = Offset.Zero,
                size = size,
            )
        }
        Phone3D(setup, state.layersOn, spin, Modifier.appearIn(delayMillis = 120, fromScale = 0.9f, riseFrom = 30f, replay = state.detailRun, gentle = true))
    }
}

/** The phone itself: its body, its screen with the setup's wallpaper, and its widgets and icons a little in front of it. */
@Composable
private fun Phone3D(setup: ConnectSetup, layersOn: Set<Layer>, spin: Spin, modifier: Modifier = Modifier) {
    val available = ConnectApply.available(setup)
    val tone = Tone.of(setup.dark)
    val wallOn = Layer.WALLPAPER in available && Layer.WALLPAPER in layersOn
    val widgetsOn = Layer.WIDGETS in available && Layer.WIDGETS in layersOn
    val iconsOn = Layer.ICONS in available && Layer.ICONS in layersOn
    val wall by animateFloatAsState(if (wallOn) 1f else 0f, tween(450), label = "p3Wall")
    val widgets by animateFloatAsState(if (widgetsOn) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "p3Widgets")
    val icons by animateFloatAsState(if (iconsOn) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 300f), label = "p3Icons")
    Box(
        modifier
            .size(width = 168.dp, height = 342.dp)
            .graphicsLayer {
                cameraDistance = 14f * density
                rotationX = spin.x
                rotationY = -spin.y
            },
    ) {
        // The body, in its black frame.
        Box(
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(32.dp))
                .background(Color(0xFF07070D))
                .border(1.5.dp, Color.White.copy(alpha = 0.18f), RoundedCornerShape(32.dp)),
        )
        // The screen: the setup's wallpaper, or yours where it is left off.
        Box(
            Modifier
                .fillMaxSize()
                .padding(5.dp)
                .clip(RoundedCornerShape(27.dp))
                .background(Brush.verticalGradient(listOf(Color(0xFF2A2B36), Color(0xFF17181F)))),
        ) {
            Box(Modifier.fillMaxSize().graphicsLayer { alpha = wall }.wall(setup.wall))
            Text(
                "YOUR WALLPAPER",
                color = Color(0x73F6F6F9),
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .graphicsLayer { alpha = 1f - wall },
            )
        }
        val screenInk = if (wallOn) tone else Tone.Dark
        if (setup.kind != SetupKind.ICONS) {
            WidgetsLayer(
                setup,
                screenInk,
                Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 28.dp)
                    .fillMaxWidth()
                    .depth(spin, 2f + 32f * widgets, yours = 1f - widgets),
            )
        }
        IconsLayer(
            setup,
            screenInk,
            Modifier
                .align(Alignment.BottomCenter)
                .padding(start = 18.dp, end = 18.dp, bottom = 16.dp)
                .fillMaxWidth()
                .depth(spin, 2f + 18f * icons, yours = 1f - icons),
        )
    }
}

/**
 * A layer of the phone [dp] in front of its screen: it slides against the screen as the phone
 * turns, as something nearer would. Left off ([yours] towards 1), it sinks back onto the screen
 * and greys over as what you have now.
 */
private fun Modifier.depth(spin: Spin, dp: Float, yours: Float): Modifier = this
    .graphicsLayer {
        val near = dp.dp.toPx()
        translationX = near * sin(Math.toRadians(spin.y.toDouble())).toFloat()
        translationY = -near * sin(Math.toRadians(spin.x.toDouble())).toFloat()
        alpha = 1f - 0.5f * yours.coerceIn(0f, 1f)
        compositingStrategy = CompositingStrategy.Offscreen
    }
    .drawWithContent {
        drawContent()
        val grey = yours.coerceIn(0f, 1f)
        if (grey > 0.01f) drawRect(Color.Gray.copy(alpha = grey), blendMode = BlendMode.Saturation)
    }

@Composable
private fun WidgetsLayer(setup: ConnectSetup, tone: Tone, modifier: Modifier = Modifier) {
    val now by produceState(LocalTime.now()) {
        while (true) {
            delay(10_000)
            value = LocalTime.now()
        }
    }
    val date = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.getDefault())) }
    val shape = RoundedCornerShape(14.dp)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text(
                now.format(DateTimeFormatter.ofPattern("HH:mm", Locale.US)),
                color = tone.ink,
                fontSize = 30.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.9).sp,
                lineHeight = 30.sp,
            )
            Text(date, color = tone.sub, fontSize = 10.sp, lineHeight = 12.sp)
        }
        Row(
            Modifier
                .padding(top = 6.dp)
                .fillMaxWidth()
                .height(48.dp)
                .clip(shape)
                .background(tone.glass)
                .border(1.dp, tone.line, shape)
                .padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(28.dp).clip(RoundedCornerShape(8.dp)).wall(setup.wall))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Box(Modifier.fillMaxWidth(0.78f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(tone.ink.copy(alpha = 0.85f)))
                Box(Modifier.fillMaxWidth(0.48f).height(4.dp).clip(RoundedCornerShape(2.dp)).background(tone.ink.copy(alpha = 0.45f)))
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(shape)
                    .background(tone.glass)
                    .border(1.dp, tone.line, shape)
                    .padding(horizontal = 9.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(tone.ink.copy(alpha = 0.85f)))
                Text("14°", color = tone.ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 12.sp)
            }
            Column(
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .clip(shape)
                    .background(tone.glass)
                    .border(1.dp, tone.line, shape)
                    .padding(horizontal = 9.dp, vertical = 7.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Steps", color = tone.sub, fontSize = 9.sp, lineHeight = 10.sp)
                Text("6,240", color = tone.ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 12.sp)
            }
        }
    }
}

private val GRID_GLYPHS = listOf(
    Icons.Outlined.Phone, Icons.Outlined.ChatBubbleOutline, Icons.Outlined.CameraAlt, Icons.Outlined.MusicNote,
    Icons.Outlined.Place, Icons.Outlined.MailOutline, Icons.Outlined.Schedule, Icons.Outlined.PlayArrow,
)
private val DOCK_GLYPHS = listOf(Icons.Outlined.Phone, Icons.Outlined.ChatBubbleOutline, Icons.Outlined.Explore, Icons.Outlined.CameraAlt)

@Composable
private fun IconsLayer(setup: ConnectSetup, tone: Tone, modifier: Modifier = Modifier) {
    val pack = setup.kind == SetupKind.ICONS
    val face = if (pack) tone.iconBg else tone.glass
    val ink = if (pack) tone.iconInk else tone.ink
    Column(modifier, verticalArrangement = Arrangement.spacedBy(14.dp)) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val gap = 10.dp
            val side = (maxWidth - gap * 3) / 4
            val shape = RoundedCornerShape(side * setup.iconRound.coerceIn(0f, 0.5f))
            Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                GRID_GLYPHS.chunked(4).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                        row.forEach { glyph -> AppGlyph(glyph, face, tone.line, ink, shape, Modifier.size(side)) }
                    }
                }
            }
        }
        Row(
            Modifier
                .fillMaxWidth()
                .height(34.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(tone.glass)
                .border(1.dp, tone.line, RoundedCornerShape(17.dp))
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val shape = RoundedCornerShape((20f * setup.iconRound.coerceIn(0f, 0.5f)).dp)
            DOCK_GLYPHS.forEach { glyph -> AppGlyph(glyph, face, null, ink, shape, Modifier.size(20.dp)) }
        }
    }
}

@Composable
private fun AppGlyph(glyph: ImageVector, face: Color, line: Color?, ink: Color, shape: Shape, modifier: Modifier) {
    Box(
        modifier
            .aspectRatio(1f)
            .clip(shape)
            .background(face)
            .then(if (line != null) Modifier.border(1.dp, line, shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(glyph, contentDescription = null, tint = ink, modifier = Modifier.fillMaxSize(0.46f))
    }
}

/** "Drag or tap to rotate", which turns the phone to its next view. */
@Composable
private fun RotateHint(spin: Spin, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    val reduced = LocalMotionLevel.current.reduced
    var turns by remember { mutableIntStateOf(0) }
    val turn by animateFloatAsState(turns * 180f, if (reduced) tween(0) else tween(500, easing = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)), label = "hintTurn")
    Row(
        modifier
            .height(44.dp)
            .pressBounce(0.95f)
            .inPageGlass(RoundedCornerShape(50), interactive = true)
            .clickable(role = Role.Button) {
                tick()
                turns++
                spin.next(android.os.SystemClock.uptimeMillis())
            }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Rounded._360, contentDescription = null, tint = palette.onGlass.copy(alpha = 0.75f), modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = turn })
        Spacer(Modifier.width(8.dp))
        Text("Drag or tap to rotate", color = palette.onGlass.copy(alpha = 0.75f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** What the setup is, who made it, what applying it puts on, its colours, and Save and Apply. */
@Composable
private fun SetupSheet(setup: ConnectSetup, state: ConnectState, spin: Spin) {
    val palette = LocalPalette.current
    val accent = LocalConnectAccent.current
    val reduced = LocalMotionLevel.current.reduced
    val available = ConnectApply.available(setup)
    val shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    val face = if (palette.dark) Color(0xFF14141F).copy(alpha = 0.94f) else Color.White.copy(alpha = 0.96f)
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(face)
            .border(1.dp, if (palette.dark) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f), shape)
            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 28.dp + bottom),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        val run = state.detailRun
        Column(Modifier.appearIn(220, fromScale = 0.98f, riseFrom = 40f, replay = run, gentle = true), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Overline(ConnectApply.kindLine(setup), colour = accent)
            Text(setup.name, color = palette.onGlass, fontSize = 26.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.4).sp, lineHeight = 30.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(setup.by.first().uppercase(), Color(0xFF2A2150), Color(0xFFF6F6F9), 24.dp)
                Spacer(Modifier.width(8.dp))
                Text("@${setup.by}", color = palette.onGlass, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                Dot()
                Icon(Icons.Rounded.Star, contentDescription = null, tint = palette.onGlassMuted, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
                Text(setup.rating?.let(ConnectFeed::rating) ?: "New", color = palette.onGlassMuted, fontSize = 14.sp)
                Dot()
                Text(
                    if (setup.applies > 0) "${ConnectFeed.count(setup.applies)} applies" else "just published",
                    color = palette.onGlassMuted,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
            }
        }
        Column(Modifier.appearIn(280, fromScale = 0.98f, riseFrom = 40f, replay = run, gentle = true), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("What gets applied", color = palette.onGlassMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Layer.entries.filter { it in available }.forEach { layer ->
                    ToggleChip(
                        ConnectApply.label(layer, setup),
                        on = layer in state.layersOn,
                        height = 44.dp,
                        modifier = Modifier.weight(1f),
                    ) {
                        state.toggleLayer(layer)
                        // Turned to show the layers apart.
                        spin.turnTo(12f to -42f, state.clock())
                    }
                }
            }
        }
        Row(
            Modifier.appearIn(340, fromScale = 0.98f, riseFrom = 40f, replay = run, gentle = true),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Swatches(setup.palette, size = 24.dp)
            Spacer(Modifier.width(12.dp))
            Text("Theme colors sync to Zenith's accents", color = palette.onGlassMuted, fontSize = 13.sp, lineHeight = 17.sp)
        }
        Row(
            Modifier.appearIn(400, fromScale = 0.98f, riseFrom = 40f, replay = run, gentle = true),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SaveButton(saved = setup.id in state.saved) { state.save(setup) }
            Spacer(Modifier.width(10.dp))
            AccentButton(
                "Apply to Zenith",
                icon = Icons.Rounded.Bolt,
                enabled = ConnectApply.canApply(setup, state.layersOn),
                height = 56.dp,
                modifier = Modifier.weight(1f),
            ) { state.startApply(reduced) }
        }
    }
}

@Composable
private fun Dot() {
    Text(" · ", color = LocalPalette.current.onGlassMuted, fontSize = 14.sp)
}

/** Save to Vault: a bookmark that fills in the accent once saved. */
@Composable
private fun SaveButton(saved: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val accent = LocalConnectAccent.current
    val tick = rememberTick()
    Box(
        Modifier
            .size(56.dp)
            .pressBounce(0.9f)
            .clip(CircleShape)
            .background(quietFill())
            .border(1.dp, if (saved) accent.copy(alpha = 0.5f) else quietEdge(), CircleShape)
            .clickable(role = Role.Button, onClickLabel = if (saved) "Saved to Vault" else "Save to Vault") {
                tick()
                onClick()
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            if (saved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
            contentDescription = if (saved) "Saved to Vault" else "Save to Vault",
            tint = if (saved) accent else palette.onGlass,
            modifier = Modifier.size(24.dp).popOn(saved),
        )
    }
}
