package com.gabriel.zenithui.ui.onboarding

import android.graphics.Paint
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.gabriel.zenithui.ui.theme.LocalPalette
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Where the three lights sit in each part of setup, as fractions of the screen, and how lively
 * they are there. Moving between parts pans them slowly, like a camera, rather than cutting.
 */
enum class BackdropScene(val anchors: List<Offset>, val energy: Float) {
    Hello(listOf(Offset(0.50f, 0.36f), Offset(0.18f, 0.64f), Offset(0.84f, 0.68f)), 1f),
    Name(listOf(Offset(0.78f, 0.12f), Offset(0.12f, 0.34f), Offset(0.60f, 0.52f)), 0.75f),
    Stage(listOf(Offset(0.22f, 0.28f), Offset(0.86f, 0.56f), Offset(0.40f, 0.88f)), 0.75f),
    Build(listOf(Offset(0.46f, 0.46f), Offset(0.58f, 0.50f), Offset(0.50f, 0.60f)), 1f),
    Plus(listOf(Offset(0.30f, 0.08f), Offset(0.75f, 0.14f), Offset(0.50f, 0.30f)), 0.75f),
}

/**
 * Seconds of drift, shared by the lights and the greeting. It is only ever read while drawing, so
 * the whole screen moves without recomposing anything.
 */
@Stable
class BackdropClock {
    /**
     * Never wrapped: each reader folds it into its own period. Wrapping at any fixed number the
     * drift periods do not divide would make every light jump once an hour.
     */
    var seconds by mutableDoubleStateOf(0.0)
        internal set
    internal var speed = 1f
}

@Composable
fun rememberBackdropClock(running: Boolean): BackdropClock {
    val clock = remember { BackdropClock() }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(running, lifecycle) {
        if (!running) return@LaunchedEffect
        // Only while you can see it: light drifting behind a switched-off screen is just battery.
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            var last = withFrameNanos { it }
            while (true) {
                withFrameNanos { now ->
                    // Added up a frame at a time rather than read off a wall clock, so changing
                    // the speed never makes the lights jump to a new place.
                    val step = ((now - last) / 1_000_000_000.0).coerceIn(0.0, 0.1)
                    last = now
                    clock.seconds += step * clock.speed
                }
            }
        }
    }
    return clock
}

/** A ring of light spreading out from wherever you chose light or dark. */
@Stable
class BackdropPulse {
    internal val progress = Animatable(1f)
    internal var origin = Offset.Zero
    internal var colour by mutableStateOf(Color.Transparent)

    suspend fun fire(at: Offset, colour: Color) {
        origin = at
        this.colour = colour
        progress.snapTo(0f)
        progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
}

@Composable
fun rememberPulse(): BackdropPulse = remember { BackdropPulse() }

private val SUNRISE = CubicBezierEasing(0.2f, 0f, 0f, 1f)
private val ANCHOR_SETTLE = Offset(0.0005f, 0.0005f)

/** Size of each light as a share of the screen's width, and how strongly it shows. */
private val ORB_RADIUS = floatArrayOf(0.90f, 0.70f, 0.60f)
private val ORB_ALPHA_DARK = floatArrayOf(0.15f, 0.11f, 0.09f)
private val ORB_ALPHA_LIGHT = floatArrayOf(0.12f, 0.10f, 0.07f)

/** Drift periods in seconds, across and down, for each light; none share a beat. */
private val ORB_PERIOD = arrayOf(doubleArrayOf(23.0, 29.0), doubleArrayOf(31.0, 37.0), doubleArrayOf(41.0, 19.0))
private val ORB_PHASE = floatArrayOf(0f, 2.1f, 4.2f)
private const val TAU = 6.2831855f

/** The pulse ring is built once at this radius and scaled, so it never needs a new shader per frame. */
private const val PULSE_UNIT = 1000f

/**
 * The light behind setup: a neutral page, black to iOS's dark grey or its pale grey to white, with
 * three soft white lights drifting over it. The first time setup opens they rise up from below the
 * screen, like a sunrise.
 */
@Composable
fun OnboardingBackdrop(
    scene: BackdropScene,
    clock: BackdropClock,
    pulse: BackdropPulse,
    sunrise: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val anchors = remember {
        List(3) { index ->
            val target = scene.anchors[index]
            val start = when {
                !sunrise -> target
                index == 0 -> Offset(0.5f, 1.25f)
                else -> Offset(target.x, target.y + 0.15f)
            }
            Animatable(start, Offset.VectorConverter, ANCHOR_SETTLE)
        }
    }
    val intro = remember { Animatable(if (sunrise) 0f else 1f) }
    val energy = remember { Animatable(scene.energy) }
    // Whether the next scene change is the sunrise. Plain memory, not state: nothing redraws on it.
    val rising = remember { booleanArrayOf(sunrise) }

    LaunchedEffect(scene) {
        val first = rising[0]
        rising[0] = false
        scene.anchors.forEachIndexed { index, target ->
            launch {
                anchors[index].animateTo(
                    target,
                    if (first) {
                        tween(2200, easing = SUNRISE)
                    } else {
                        spring(dampingRatio = 1f, stiffness = 50f, visibilityThreshold = ANCHOR_SETTLE)
                    },
                )
            }
        }
        launch { energy.animateTo(scene.energy, tween(800)) }
    }
    // Its own effect, so moving on before the sunrise has finished does not leave the light dim.
    LaunchedEffect(Unit) {
        if (intro.value < 1f) intro.animateTo(1f, tween(1200, easing = LinearOutSlowInEasing))
    }
    LaunchedEffect(clock) {
        snapshotFlow { energy.value }.collect { clock.speed = 0.6f + 0.4f * it }
    }

    val dark = palette.dark
    // White light, with no colour in it: Zenith has one look, and the lights are only light.
    val lights = List(3) { Color.White }
    val strength = if (dark) ORB_ALPHA_DARK else ORB_ALPHA_LIGHT
    val backdrop = palette.backdrop

    Spacer(
        modifier
            .fillMaxSize()
            .graphicsLayer()
            .drawWithCache {
                val width = size.width
                val height = size.height
                val base = Brush.verticalGradient(backdrop)
                // Framework paints with dithering: a big soft gradient on a dark screen bands
                // into visible rings without it.
                val paints = List(3) { index ->
                    val colour = lights[index]
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
                        shader = RadialGradient(
                            0f, 0f, ORB_RADIUS[index] * width,
                            intArrayOf(colour.toArgb(), colour.copy(alpha = 0.45f).toArgb(), colour.copy(alpha = 0f).toArgb()),
                            floatArrayOf(0f, 0.45f, 1f),
                            Shader.TileMode.CLAMP,
                        )
                    }
                }
                val ring = pulse.colour
                val pulsePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
                    shader = RadialGradient(
                        0f, 0f, PULSE_UNIT,
                        intArrayOf(
                            ring.copy(alpha = 0f).toArgb(),
                            ring.copy(alpha = 0f).toArgb(),
                            ring.toArgb(),
                            ring.copy(alpha = 0f).toArgb(),
                        ),
                        floatArrayOf(0f, 0.6f, 0.9f, 1f),
                        Shader.TileMode.CLAMP,
                    )
                }
                val vignette = Brush.radialGradient(
                    0f to Color.Transparent,
                    0.55f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.30f),
                    center = Offset(width / 2f, height / 2f),
                    radius = hypot(width, height) / 2f,
                )
                val pulseAlpha = if (dark) 0.32f else 0.22f

                onDrawBehind {
                    drawRect(base)
                    val seconds = clock.seconds
                    val glow = intro.value * (0.6f + 0.4f * energy.value)
                    drawIntoCanvas { canvas ->
                        val native = canvas.nativeCanvas
                        for (index in 0 until 3) {
                            val anchor = anchors[index].value
                            val period = ORB_PERIOD[index]
                            val phase = ORB_PHASE[index]
                            // Folded into each period first, so the angle stays small and exact.
                            val across = ((seconds / period[0]) % 1.0).toFloat()
                            val down = ((seconds / period[1]) % 1.0).toFloat()
                            val x = (anchor.x + 0.08f * sin(TAU * across + phase)) * width
                            val y = (anchor.y + 0.06f * cos(TAU * down + phase)) * height
                            val paint = paints[index]
                            paint.alpha = (strength[index] * glow * 255f).toInt().coerceIn(0, 255)
                            native.save()
                            native.translate(x, y)
                            native.drawCircle(0f, 0f, ORB_RADIUS[index] * width, paint)
                            native.restore()
                        }
                        val spread = pulse.progress.value
                        if (spread < 1f) {
                            val reach = spread * hypot(width, height) / PULSE_UNIT
                            pulsePaint.alpha = (pulseAlpha * (1f - spread) * 255f).toInt().coerceIn(0, 255)
                            native.save()
                            native.translate(pulse.origin.x, pulse.origin.y)
                            native.scale(reach, reach)
                            native.drawCircle(0f, 0f, PULSE_UNIT, pulsePaint)
                            native.restore()
                        }
                    }
                    if (dark) drawRect(vignette)
                }
            }
    )
}
