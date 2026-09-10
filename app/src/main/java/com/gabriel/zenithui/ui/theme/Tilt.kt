package com.gabriel.zenithui.ui.theme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Which way the phone is leaning, as a unit-ish vector: x is roll (right is positive), y is pitch
 * (leaning away from you is positive). Zero means flat on to your face, or no sensor at all.
 *
 * It is a [State] rather than a plain value on purpose. Every glass surface reads it inside its
 * draw lambda, so a new sensor reading repaints the screen without recomposing a single node —
 * which is the difference between a shine that costs nothing and one that costs the whole frame.
 */
val LocalTilt = staticCompositionLocalOf<State<Offset>> { mutableStateOf(Offset.Zero) }

/** How far the light is allowed to travel: past this the highlight has left the panel anyway. */
private const val LIMIT = 1f

/** Spring constants, in the same units as the tilt itself. Soft, with a little overshoot. */
private const val STIFFNESS = 55f
private const val DAMPING = 8.5f

/** Gravity, so a reading can be turned into a fraction of "fully on its side". */
private const val G = 9.81f

/**
 * Reads the accelerometer and returns the tilt, with the light given some mass.
 *
 * The raw signal is noisy and instant; a highlight that copies it exactly looks like a twitch
 * rather than a reflection. So the reading is a target that a spring chases: the shine lags a
 * little going in, overshoots a little coming to rest, and settles. That lag is the whole effect.
 */
@Composable
fun rememberTilt(enabled: Boolean): State<Offset> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val tilt = remember { mutableStateOf(Offset.Zero) }

    DisposableEffect(enabled, lifecycleOwner) {
        if (!enabled) {
            tilt.value = Offset.Zero
            return@DisposableEffect onDispose { }
        }

        val sensors = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val sensor = sensors?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensors == null || sensor == null) {
            // A phone with no accelerometer keeps a perfectly still, perfectly fine pane of glass.
            return@DisposableEffect onDispose { }
        }

        var velocity = Offset.Zero
        var lastNanos = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val target = Offset(
                    x = (-event.values[0] / G).coerceIn(-LIMIT, LIMIT),
                    y = ((event.values[1] / G) - 1f).coerceIn(-LIMIT, LIMIT),
                )
                // Seconds since the last reading, clamped: coming back from a pause must not
                // arrive as one enormous step that flings the highlight off the panel.
                val dt = if (lastNanos == 0L) 0f else ((event.timestamp - lastNanos) / 1e9f)
                lastNanos = event.timestamp
                if (dt <= 0f) return
                val step = dt.coerceAtMost(0.05f)

                val current = tilt.value
                val acceleration = (target - current) * STIFFNESS - velocity * DAMPING
                velocity += acceleration * step
                tilt.value = current + velocity * step
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        fun start() {
            lastNanos = 0L
            sensors.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }

        fun stop() {
            sensors.unregisterListener(listener)
            velocity = Offset.Zero
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

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stop()
        }
    }

    return tilt
}
