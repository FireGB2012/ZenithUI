package com.gabriel.zenithui.ui.home.widgets

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Power
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.BatteryFacts
import com.gabriel.zenithui.data.BatteryTone
import com.gabriel.zenithui.data.WidgetForm
import com.gabriel.zenithui.ui.home.LocalBare
import com.gabriel.zenithui.ui.home.LocalWidgetWorld
import com.gabriel.zenithui.ui.theme.glassInk

/**
 * Power. The charge as a ring round its percentage, green, yellow in battery saver and red when it is
 * low, as an iPhone's battery is, with a light running round it while it charges. The bigger ones say
 * what it is doing in words, how long it has to go to full, and what the phone knows about the
 * battery: where its power comes from, how warm it is, how it is, and its voltage.
 *
 * - Mini: the ring round the percentage.
 * - Strip: a battery filled as far as the charge, the percentage, and what it is doing.
 * - Square: the ring round the percentage, and what it is doing under it.
 * - Wide: the ring on the left; what it is doing and the battery's state on the right.
 * - Tall: the ring at the top, what it is doing, and the battery's state at the foot.
 * - Big: what it is doing, the ring large, and four tiles of the battery's state.
 */
@Composable
internal fun PowerWidget(form: WidgetForm, width: Dp, height: Dp) {
    val battery = rememberBattery()
    val units = LocalWidgetWorld.current?.settings?.weatherUnits ?: "c"
    val tone = BatteryFacts.tone(battery.level, battery.charging, battery.saver)
    val colour = when (tone) {
        BatteryTone.Good -> hue(Hue.Green)
        BatteryTone.Saver -> hue(Hue.Yellow)
        BatteryTone.Low -> hue(Hue.Red)
    }
    val percent = if (battery.level < 0) "--" else "${battery.level}%"
    val status = BatteryFacts.status(battery.level, battery.charging, battery.saver, battery.toFull)
    val headline = when {
        status == "Charged" -> "Charged"
        battery.charging -> "Charging"
        else -> status
    }
    val source = BatteryFacts.source(battery.plugged)
    val temperature = BatteryFacts.temperature(battery.temperature, units)
    // Under the headline, the one more thing worth knowing: how long to full, where the power comes
    // from, or how warm it is.
    val detail = when {
        battery.charging && status != headline -> status
        battery.charging -> source
        else -> temperature ?: source
    }
    // Where the battery's state is set out below, only how long it has to go is worth saying again.
    val toGo = status.takeIf { battery.charging && it != headline }
    val fraction = battery.level.coerceIn(0, 100) / 100f
    val glint = battery.charging && status != "Charged"
    val spoken = "Battery $percent, $headline" + (if (detail != headline) ", $detail" else "")

    Fitted(form, width, height) { w, _ ->
        val root = Modifier.fillMaxSize().says(spoken)
        when (form) {
            WidgetForm.Mini -> Box(root.padding(7.dp), contentAlignment = Alignment.Center) {
                Gauge(fraction, colour, 4.5.dp, Modifier.fillMaxSize(), glint = glint) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (battery.charging) Glyph(Icons.Rounded.Bolt, 10.dp, tint = colour)
                        Hero("${battery.level.coerceAtLeast(0)}", 15.sp, weight = FontWeight.SemiBold, rolling = true)
                    }
                }
            }

            WidgetForm.Strip -> Row(root.padding(horizontal = INSET_STRIP), verticalAlignment = Alignment.CenterVertically) {
                if (w >= 250.dp) {
                    BatteryShape(fraction, colour, battery.charging, Modifier.width(32.dp).height(16.dp))
                    Spacer(Modifier.width(12.dp))
                    Hero(percent, 26.sp, rolling = true)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Words(headline, Type.body, weight = FontWeight.SemiBold)
                        Words(detail, Type.meta, color = soft())
                    }
                } else {
                    // Narrow: the percentage over what it is doing, beside the battery.
                    BatteryShape(fraction, colour, battery.charging, Modifier.width(28.dp).height(14.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Hero(percent, 22.sp, weight = FontWeight.Normal, rolling = true)
                        Words(headline, Type.meta, weight = FontWeight.SemiBold, color = soft())
                    }
                }
            }

            WidgetForm.Square -> Column(root.padding(INSET), horizontalAlignment = Alignment.CenterHorizontally) {
                Gauge(fraction, colour, 9.dp, Modifier.weight(1f).aspectRatio(1f, matchHeightConstraintsFirst = true), glint = glint) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (battery.charging) Glyph(Icons.Rounded.Bolt, 16.dp, tint = colour)
                        Hero(percent, 26.sp, weight = FontWeight.Normal, rolling = true)
                    }
                }
                Spacer(Modifier.height(10.dp))
                Words(if (battery.charging && status != headline) status else headline, Type.body, weight = FontWeight.SemiBold)
            }

            WidgetForm.Wide -> Row(root.padding(INSET), verticalAlignment = Alignment.CenterVertically) {
                Gauge(fraction, colour, 11.dp, Modifier.fillMaxHeight().aspectRatio(1f), glint = glint) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (battery.charging) Glyph(Icons.Rounded.Bolt, 18.dp, tint = colour)
                        Hero(percent, 30.sp, weight = FontWeight.Normal, rolling = true)
                    }
                }
                Spacer(Modifier.width(18.dp))
                Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
                    Words(headline, Type.headline, weight = FontWeight.SemiBold)
                    if (toGo != null) Words(toGo, Type.body, color = soft())
                    Spacer(Modifier.height(10.dp))
                    Hairline()
                    Spacer(Modifier.height(4.dp))
                    StatLine(Icons.Rounded.Power, "Power", source)
                    if (temperature != null) StatLine(Icons.Rounded.Thermostat, "Temperature", temperature)
                    StatLine(Icons.Rounded.HealthAndSafety, "Health", BatteryFacts.health(battery.health))
                }
            }

            WidgetForm.Tall -> Column(root.padding(INSET), horizontalAlignment = Alignment.CenterHorizontally) {
                Gauge(fraction, colour, 11.dp, Modifier.fillMaxWidth(0.92f).aspectRatio(1f), glint = glint) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (battery.charging) Glyph(Icons.Rounded.Bolt, 18.dp, tint = colour)
                        Hero(percent, 30.sp, weight = FontWeight.Normal, rolling = true)
                    }
                }
                Spacer(Modifier.height(14.dp))
                Words(headline, Type.headline, weight = FontWeight.SemiBold)
                if (toGo != null) Words(toGo, Type.body, color = soft())
                Spacer(Modifier.weight(1f))
                Column(Modifier.fillMaxWidth().well().padding(horizontal = 10.dp, vertical = 6.dp)) {
                    StatLine(Icons.Rounded.Power, "Power", source)
                    if (temperature != null) StatLine(Icons.Rounded.Thermostat, "Temp", temperature)
                    StatLine(Icons.Rounded.HealthAndSafety, "Health", BatteryFacts.health(battery.health))
                }
            }

            WidgetForm.Big -> Column(root.padding(INSET)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Label("Battery")
                        Words(headline, Type.headline, weight = FontWeight.SemiBold)
                    }
                    if (toGo != null) Words(toGo, Type.body, color = soft())
                }
                Box(Modifier.weight(1f).fillMaxWidth().padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Gauge(fraction, colour, 14.dp, Modifier.fillMaxHeight().aspectRatio(1f, matchHeightConstraintsFirst = true), glint = glint) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (battery.charging) Glyph(Icons.Rounded.Bolt, 22.dp, tint = colour)
                            Hero(percent, 42.sp, weight = FontWeight.Normal, rolling = true)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatTile(Icons.Rounded.Power, "Power", source, Modifier.weight(1f))
                    StatTile(Icons.Rounded.Thermostat, "Temp", temperature ?: "—", Modifier.weight(1f))
                    StatTile(Icons.Rounded.HealthAndSafety, "Health", BatteryFacts.health(battery.health), Modifier.weight(1f))
                    StatTile(Icons.Rounded.ElectricBolt, "Voltage", BatteryFacts.voltage(battery.voltage) ?: "—", Modifier.weight(1f))
                }
            }
        }
    }
}

/** One thing about the battery as a line: its symbol and what it is on the left, its value on the right. */
@Composable
private fun StatLine(icon: ImageVector, label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
        Glyph(icon, 14.dp, tint = soft())
        Spacer(Modifier.width(6.dp))
        Words(label, Type.meta, color = soft(), modifier = Modifier.weight(1f))
        Words(value, Type.meta, weight = FontWeight.SemiBold)
    }
}

/** One thing about the battery as a tile: its symbol, what it is and its value, one under another. */
@Composable
private fun StatTile(icon: ImageVector, label: String, value: String, modifier: Modifier) {
    Column(modifier.well().padding(horizontal = 9.dp, vertical = 9.dp)) {
        Glyph(icon, 15.dp, tint = soft())
        Spacer(Modifier.height(6.dp))
        Label(label)
        Words(value, Type.body, weight = FontWeight.SemiBold)
    }
}

/**
 * A battery lying on its side, filled as far as [fraction] in [colour], with its little cap; a bolt
 * through it while it charges. In a "No glass" widget the fill is glass.
 */
@Composable
private fun BatteryShape(fraction: Float, colour: Color, charging: Boolean, modifier: Modifier) {
    val bare = LocalBare.current
    val outline = faint(0.55f)
    val filled = com.gabriel.zenithui.ui.home.rememberFilled(fraction)
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().then(if (bare) Modifier.glassInk(bevel = 1.5.dp) else Modifier)) {
            val cap = size.width * 0.08f
            val body = Size(size.width - cap - 1.dp.toPx(), size.height)
            val line = 1.3.dp.toPx()
            val round = CornerRadius(size.height * 0.28f)
            drawRoundRect(if (bare) Color.White else outline, Offset(line / 2f, line / 2f), Size(body.width - line, body.height - line), round, style = Stroke(line))
            drawRoundRect(
                if (bare) Color.White else outline,
                Offset(body.width + 1.dp.toPx(), size.height * 0.32f),
                Size(cap, size.height * 0.36f),
                CornerRadius(cap),
            )
            val inset = line + 1.5.dp.toPx()
            val inner = Size((body.width - inset * 2f) * filled.value.coerceIn(0.04f, 1f), body.height - inset * 2f)
            drawRoundRect(if (bare) Color.White else colour, Offset(inset, inset), inner, CornerRadius(size.height * 0.16f))
        }
        if (charging) Glyph(Icons.Rounded.Bolt, 12.dp, tint = Color.White, modifier = Modifier.padding(end = 2.dp))
    }
}

/** What Android says about the battery, as the widget reads it. */
@Immutable
internal data class BatteryInfo(
    val level: Int = -1,
    val charging: Boolean = false,
    val plugged: Int = 0,
    val temperature: Int = Int.MIN_VALUE,
    val health: Int = BatteryManager.BATTERY_HEALTH_UNKNOWN,
    val voltage: Int = 0,
    val saver: Boolean = false,
    /** How long to full, in milliseconds: 0 when full, below 0 when Android does not know. */
    val toFull: Long = -1L,
)

/**
 * The battery as Android last said: read from its sticky broadcast when the widget appears and each
 * time it changes, and battery saver from its own.
 */
@Composable
internal fun rememberBattery(): BatteryInfo {
    val context = LocalContext.current
    var info by remember { mutableStateOf(BatteryInfo()) }
    DisposableEffect(context) {
        val power = context.getSystemService(PowerManager::class.java)
        val manager = context.getSystemService(BatteryManager::class.java)
        var last: Intent? = null
        fun read(intent: Intent?) {
            if (intent != null && intent.action == Intent.ACTION_BATTERY_CHANGED) last = intent
            val battery = last ?: return
            val scale = battery.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val status = battery.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            info = BatteryInfo(
                level = battery.getIntExtra(BatteryManager.EXTRA_LEVEL, -1).let { if (it < 0) -1 else it * 100 / scale },
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
                plugged = battery.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0),
                temperature = battery.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, Int.MIN_VALUE),
                health = battery.getIntExtra(BatteryManager.EXTRA_HEALTH, BatteryManager.BATTERY_HEALTH_UNKNOWN),
                voltage = battery.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0),
                saver = runCatching { power?.isPowerSaveMode == true }.getOrDefault(false),
                toFull = runCatching { manager?.computeChargeTimeRemaining() ?: -1L }.getOrDefault(-1L),
            )
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) = read(intent)
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
        }
        read(runCatching { context.registerReceiver(receiver, filter) }.getOrNull())
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }
    return info
}
