package com.gabriel.zenithui.ui.theme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.hypot
import kotlin.math.sqrt
import kotlinx.coroutines.channels.Channel

/**
 * Which way the phone is leaning, as a unit-ish vector: x is roll (right is positive), y is pitch
 * (leaning away from you is positive). Zero means flat on to your face, or no sensor at all.
 *
 * It is a [State] rather than a plain value on purpose. Every glass surface reads it inside its
 * draw lambda, so a new sensor reading repaints the screen without recomposing a single node —
 * which is the difference between a shine that costs nothing and one that costs the whole frame.
 */
val LocalTilt = staticCompositionLocalOf<State<Offset>> { mutableStateOf(Offset.Zero) }

/**
 * Which way the phone leans from the way it is being held (see [TiltRest]), -1 to 1 on each axis:
 * what the icons, widgets and dock float by, a few dp against it, so the wallpaper reads as deeper.
 * Zero with movement reduced or Tilt shine off. A [State] read only while drawing or in a layer, as
 * [LocalTilt] is, so things float without anything being composed again.
 */
val LocalParallax: ProvidableCompositionLocal<State<Offset>> = staticCompositionLocalOf { mutableStateOf(Offset.Zero) }

/**
 * What the one tilt sensor gives the launcher: [light], which way the phone leans, for the light on
 * the glass ([LocalTilt]); and [parallax], which way it leans from how it is being held, for what
 * floats over the wallpaper ([LocalParallax]).
 */
@Stable
class TiltReadings internal constructor() {
    internal val leaning = mutableStateOf(Offset.Zero)
    internal val floating = mutableStateOf(Offset.Zero)
    val light: State<Offset> get() = leaning
    val parallax: State<Offset> get() = floating
}

/** How far the light is allowed to travel: past this the highlight has left the panel anyway. */
private const val LIMIT = 1f

/** Spring constants, in the same units as the tilt itself. Soft, with a little overshoot. */
private const val STIFFNESS = 55f
private const val DAMPING = 8.5f

/** How fast the spring swings when nothing slows it, for telling how far it could still go. */
private val SWING = sqrt(STIFFNESS)

/**
 * The longest step the spring takes at once, in seconds: a frame that comes late, or the first
 * after a pause, must not arrive as one enormous step that flings the highlight off the panel.
 */
private const val LONGEST_STEP = 0.05f

/** Gravity, so a reading can be turned into a fraction of "fully on its side". */
private const val G = 9.81f

/**
 * Reads the accelerometer and returns the tilt, with the light given some mass, and how far that
 * floats things from the way the phone is being held.
 *
 * The raw signal is noisy and instant; a highlight that copies it exactly looks like a twitch
 * rather than a reflection. So the reading is a target that a spring chases: the shine lags a
 * little going in, overshoots a little coming to rest, and settles. That lag is the whole effect.
 * The spring is stepped on the frame clock, between readings, so what floats glides with every
 * frame rather than stepping at the sensor's rate.
 *
 * Nothing is written that would change what is drawn by less than [TiltRest.DEAD_ZONE], and once
 * the spring has settled and the rest pose has caught up, the frame loop waits for the phone to
 * move: a phone lying still writes nothing, so nothing is drawn again. With [floats] off (movement
 * reduced) [TiltReadings.parallax] stays at zero and the light still follows the phone.
 */
@Composable
fun rememberTilt(enabled: Boolean, floats: Boolean = true): TiltReadings {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val readings = remember { TiltReadings() }

    LaunchedEffect(enabled, floats, lifecycleOwner) {
        if (!enabled) {
            readings.leaning.value = Offset.Zero
            readings.floating.value = Offset.Zero
            return@LaunchedEffect
        }
        if (!floats) readings.floating.value = Offset.Zero

        val sensors = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensors == null || sensor == null) {
            // A phone with no accelerometer keeps a perfectly still, perfectly fine pane of glass.
            return@LaunchedEffect
        }

        val motion = TiltMotion(readings, floats)
        // Wakes the frame loop below when the phone moves while it is waiting.
        val wake = Channel<Unit>(Channel.CONFLATED)

        // Readings arrive on the main thread, where the frame loop runs too, so the two share
        // plain values with no locking.
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val reading = Offset(
                    x = (-event.values[0] / G).coerceIn(-LIMIT, LIMIT),
                    y = ((event.values[1] / G) - 1f).coerceIn(-LIMIT, LIMIT),
                )
                if (motion.read(reading)) wake.trySend(Unit)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        fun start() {
            motion.restart()
            // Often enough that the spring always has a fresh target between the frames it steps on.
            sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
        }

        fun stop() {
            sensors.unregisterListener(listener)
        }

        // The sensor runs only while the launcher is actually on screen. A home screen is the one
        // app that is always technically open, so leaving it registered would be a battery leak.
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> start()
                Lifecycle.Event.ON_PAUSE -> stop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) start()

        try {
            while (true) {
                if (motion.parked) wake.receive()
                withFrameNanos { now -> motion.frame(now) }
            }
        } finally {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stop()
        }
    }

    return readings
}

/**
 * The light's spring and the rest pose, stepped on the frame clock between readings. Its own numbers
 * are plain values, not state: only a change of more than [TiltRest.DEAD_ZONE] to what is drawn is
 * written to [readings].
 */
private class TiltMotion(private val readings: TiltReadings, private val floats: Boolean) {
    /** The latest reading: where the spring is heading. */
    private var target = Offset.Zero
    private var position = readings.light.value
    private var velocity = Offset.Zero
    /** How the phone is being held: see [TiltRest.follow]. */
    private var rest = Offset.Zero
    /** The next reading is the first since the sensor started. */
    private var fresh = true
    private var lastFrame = 0L

    /** Waiting for the phone to move: nothing that is drawn can change until it does. */
    var parked = true
        private set

    /** The reading it had settled at when it began waiting. */
    private var parkedAt = Offset.Zero

    /** The sensor is starting again: its first reading is where everything is. */
    fun restart() {
        fresh = true
    }

    /** A new [reading]. True when the frame loop is waiting and has to be woken for it. */
    fun read(reading: Offset): Boolean {
        target = reading
        val moved = if (fresh) {
            fresh = false
            // However the phone was held when the launcher last looked, this is how it is held now:
            // the light starts here rather than swinging over from there, and so does the rest, so
            // nothing floats just because the launcher came back.
            position = reading
            velocity = Offset.Zero
            rest = reading
            true
        } else {
            !TiltRest.still(parkedAt, reading)
        }
        if (!parked || !moved) return false
        parked = false
        return true
    }

    /** One frame, at [now] on the frame clock. */
    fun frame(now: Long) {
        val elapsed = if (lastFrame == 0L) 0f else ((now - lastFrame) / 1e9f).coerceAtLeast(0f)
        lastFrame = now
        if (elapsed > 0f) {
            val step = elapsed.coerceAtMost(LONGEST_STEP)
            val acceleration = (target - position) * STIFFNESS - velocity * DAMPING
            velocity += acceleration * step
            position += velocity * step
            rest = TiltRest.follow(rest, target, elapsed)
        }
        val float = if (floats) TiltRest.relative(position, rest) else Offset.Zero

        // How far the spring could still swing from where it is heading: where it is, and how far
        // its speed would carry it. A damped spring never swings further than that, so once it is
        // inside the dead zone, and the rest has caught up with the phone, nothing drawn can move
        // until the phone does. It is put down exactly there, and waits.
        val swing = hypot((target - position).getDistance(), velocity.getDistance() / SWING)
        if (swing < TiltRest.DEAD_ZONE && TiltRest.still(Offset.Zero, float)) {
            position = target
            velocity = Offset.Zero
            rest = target
            readings.leaning.value = target
            readings.floating.value = Offset.Zero
            parked = true
            parkedAt = target
            lastFrame = 0L
            return
        }

        if (!TiltRest.still(readings.light.value, position)) readings.leaning.value = position
        if (!TiltRest.still(readings.parallax.value, float)) readings.floating.value = float
    }
}
