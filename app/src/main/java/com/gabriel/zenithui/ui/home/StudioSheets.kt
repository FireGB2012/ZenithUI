package com.gabriel.zenithui.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.R
import com.gabriel.zenithui.data.GlassTuning
import com.gabriel.zenithui.ui.common.GlassButton
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.SectionLabel
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.onboarding.Segmented
import com.gabriel.zenithui.ui.settings.PillSlider
import com.gabriel.zenithui.ui.settings.ToggleRow
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.lightFrom
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * The Glass studio (Zenith Plus): the glass made your own, every change seen at once — on the
 * three panes at the top, on this sheet, and on the home screen behind it. Reset puts Zenith's
 * glass back.
 */
@Composable
fun GlassStudioSheet(visible: Boolean, tuning: GlassTuning, onChange: (GlassTuning) -> Unit, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Glass studio", maxHeightFraction = 0.88f) {
        // Three panes of the home screen's own glass: an icon, a bar, a drop.
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).appearIn(delayMillis = staggerDelay(0), riseFrom = 14f),
        ) {
            Box(Modifier.size(64.dp).glass(RoundedCornerShape(20.dp), material = GlassMaterial.Clear))
            Box(Modifier.size(width = 120.dp, height = 46.dp).glass(RoundedCornerShape(50)))
            Box(Modifier.size(56.dp).glass(CircleShape, material = GlassMaterial.Clear))
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            Column(Modifier.appearIn(delayMillis = staggerDelay(1), riseFrom = 14f)) {
                PillSlider("Bend", tuning.bend, GlassTuning.BEND, times(tuning.bend)) { onChange(tuning.copy(bend = it)) }
                Spacer(Modifier.height(10.dp))
                PillSlider("Frost", tuning.frost, GlassTuning.FROST, signed(tuning.frost)) { onChange(tuning.copy(frost = it)) }
                Spacer(Modifier.height(10.dp))
                PillSlider("Rim light", tuning.rim, GlassTuning.RIM, times(tuning.rim)) { onChange(tuning.copy(rim = it)) }
                Spacer(Modifier.height(10.dp))
                PillSlider("Rainbow edge", tuning.rainbow, GlassTuning.RAINBOW, times(tuning.rainbow)) { onChange(tuning.copy(rainbow = it)) }
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.appearIn(delayMillis = staggerDelay(2), riseFrom = 14f)) {
                SectionLabel("Light", Modifier.padding(start = 6.dp, bottom = 8.dp))
                Segmented(
                    options = listOf("zenith" to "Zenith's", "fixed" to "Your own"),
                    selected = if (tuning.lightAngle == null) "zenith" else "fixed",
                    onSelect = { choice ->
                        onChange(tuning.copy(lightAngle = if (choice == "zenith") null else tuning.lightAngle ?: GlassTuning.ZENITH_LIGHT))
                    },
                )
                AnimatedVisibility(
                    visible = tuning.lightAngle != null,
                    enter = unfoldIn(LocalMotionLevel.current.reduced),
                    exit = unfoldOut(LocalMotionLevel.current.reduced),
                ) {
                    Row(Modifier.fillMaxWidth().padding(top = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                        LightDial(tuning.lightAngle ?: GlassTuning.ZENITH_LIGHT) { onChange(tuning.copy(lightAngle = it)) }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            "Drag round the dial: the light falls on every pane from where the dot is.",
                            color = palette.onGlassMuted,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(6.dp))
                ToggleRow(
                    title = "Light follows tilt",
                    subtitle = "With Tilt shine on, the light swings as you tip the phone",
                    checked = tuning.lightTilt,
                ) { onChange(tuning.copy(lightTilt = it)) }
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassButton(text = "Reset to Zenith's glass", enabled = !tuning.isDefault, onClick = { onChange(GlassTuning()) }, modifier = Modifier.fillMaxWidth())
    }
}

private fun times(value: Float) = "%.1f×".format(value)

private fun signed(value: Float) = when {
    value > 0.005f -> "+%.1f".format(value)
    value < -0.005f -> "−%.1f".format(-value)
    else -> "As made"
}

/**
 * A dial for where the light comes from: a ring with the light as a dot on it, and the shine it
 * throws across a little pane in the middle. Drag round, or tap a point on it.
 */
@Composable
private fun LightDial(angle: Float, onChange: (Float) -> Unit) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    val shown by animateFloatAsState(angle, spring(dampingRatio = 0.8f, stiffness = 600f), label = "lightAngle")
    fun angleAt(at: Offset, size: Float): Float {
        val degrees = Math.toDegrees(atan2((at.y - size / 2f).toDouble(), (at.x - size / 2f).toDouble())).toFloat()
        // Settles on every fifteen degrees, so the light can be put straight back where it was.
        return ((degrees + 360f) % 360f / 15f).roundToInt() * 15f % 360f
    }
    Canvas(
        Modifier
            .size(96.dp)
            .pointerInput(Unit) {
                detectTapGestures { at ->
                    tick()
                    onChange(angleAt(at, size.width.toFloat()))
                }
            }
            .pointerInput(Unit) {
                var last = Float.NaN
                detectDragGestures { change, _ ->
                    val next = angleAt(change.position, size.width.toFloat())
                    if (next != last) {
                        tick()
                        last = next
                        onChange(next)
                    }
                }
            },
    ) {
        val r = size.minDimension / 2f
        val ring = r - 8.dp.toPx()
        drawCircle(palette.onGlass.copy(alpha = 0.12f), radius = ring, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
        val light = lightFrom(Offset.Zero, GlassTuning(lightAngle = shown))
        val length = kotlin.math.hypot(light.x, light.y)
        val dot = Offset(center.x + light.x / length * ring, center.y + light.y / length * ring)
        // The pane in the middle, lit from the dot's side.
        val pane = r * 0.62f
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.06f)),
                start = Offset(center.x + light.x / length * pane / 2f, center.y + light.y / length * pane / 2f),
                end = Offset(center.x - light.x / length * pane / 2f, center.y - light.y / length * pane / 2f),
            ),
            topLeft = Offset(center.x - pane / 2f, center.y - pane / 2f),
            size = androidx.compose.ui.geometry.Size(pane, pane),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(pane * 0.3f),
        )
        drawCircle(palette.accent.copy(alpha = 0.35f), radius = 11.dp.toPx(), center = dot)
        drawCircle(palette.accent, radius = 6.dp.toPx(), center = dot)
    }
}

/**
 * One colour to choose: a dot, ringed when it is the one, with a tick on it, and its name under
 * it. [colours] null is "none": an empty ring.
 */
@Composable
private fun Swatch(label: String, colours: List<Color>?, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    val ring by animateColorAsState(if (selected) palette.onGlass else Color.Transparent, tween(220), label = "swatchRing")
    val inner by animateDpAsState(if (selected) 34.dp else 40.dp, spring(dampingRatio = 0.7f, stiffness = 420f), label = "swatchInner")
    val mark by animateFloatAsState(if (selected) 1f else 0f, spring(dampingRatio = 0.55f, stiffness = 500f), label = "swatchTick")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(58.dp)
            .pressBounce(0.9f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (!selected) tick()
                onClick()
            },
    ) {
        Box(Modifier.size(46.dp).border(2.dp, ring, CircleShape), contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(inner)
                    .clip(CircleShape)
                    .then(
                        if (colours == null) Modifier.border(1.5.dp, palette.onGlass.copy(alpha = 0.3f), CircleShape)
                        else Modifier.background(Brush.linearGradient(colours))
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (mark > 0.01f) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        tint = if (colours == null) palette.onGlass else Color.White,
                        modifier = Modifier.size(18.dp).scale(mark),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = if (selected) palette.onGlass else palette.onGlassMuted, fontSize = 12.sp, maxLines = 1)
    }
}

/** The colours a widget can have of its own: bright enough to glow on glass, light or dark. */
internal val WIDGET_COLOURS = listOf(
    0xFFFF6B8BL to "Rose", 0xFFFF8A5BL to "Coral", 0xFFFFC14DL to "Amber", 0xFFB8E05AL to "Lime",
    0xFF4FE0A8L to "Mint", 0xFF3FD0D6L to "Teal", 0xFF4FB4FFL to "Sky", 0xFF5B7CFFL to "Blue",
    0xFF8B6CFFL to "Violet", 0xFFD66CFFL to "Orchid", 0xFFFF5FD2L to "Magenta", 0xFFE8EDF5L to "Frost",
)

/** A colour of its own for one of Zenith's widgets, or none: it changes as you tap. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetColourSheet(visible: Boolean, current: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Colour") {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().appearIn(delayMillis = staggerDelay(0), riseFrom = 14f),
        ) {
            // None is the plain look, so it is the empty ring rather than a colour.
            Swatch("None", null, selected = current == 0L) { onPick(0L) }
            WIDGET_COLOURS.forEach { (argb, name) ->
                Swatch(name, listOf(Color(argb.toInt()), Color(argb.toInt())), selected = current == argb) { onPick(argb) }
            }
        }
        Spacer(Modifier.height(12.dp))
        GlassButton(text = "Done", filled = true, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}

/** The fonts Zenith carries, who made them, and the licence they are shared under, in full. */
@Composable
fun FontLicenceSheet(visible: Boolean, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    val text = remember {
        runCatching { context.resources.openRawResource(R.raw.font_licences).use { it.readBytes().decodeToString() } }.getOrDefault("")
    }
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Fonts", maxHeightFraction = 0.86f) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            Text(text, color = palette.onGlass.copy(alpha = 0.86f), fontSize = 12.sp, lineHeight = 17.sp, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}
