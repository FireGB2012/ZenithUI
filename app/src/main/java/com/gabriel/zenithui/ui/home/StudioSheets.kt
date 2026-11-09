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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.R
import com.gabriel.zenithui.data.DayParts
import com.gabriel.zenithui.data.GlassTuning
import com.gabriel.zenithui.data.LookSchedule
import com.gabriel.zenithui.ui.common.ZenithButton
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.onboarding.Segmented
import com.gabriel.zenithui.ui.settings.ControlRow
import com.gabriel.zenithui.ui.settings.GroupDivider
import com.gabriel.zenithui.ui.settings.Note
import com.gabriel.zenithui.ui.settings.PillSlider
import com.gabriel.zenithui.ui.settings.ROW_INSET
import com.gabriel.zenithui.ui.settings.Section
import com.gabriel.zenithui.ui.settings.ToggleRow
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.LocalApple
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithPalette
import com.gabriel.zenithui.ui.theme.contrast
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.inkOnTint
import com.gabriel.zenithui.ui.theme.lightFrom
import kotlin.math.atan2
import kotlin.math.roundToInt

/**
 * The Glass studio (Zenith Plus): the glass made your own, every change seen at once — on the
 * three panes at the top, on this sheet, and on the home screen behind it. The three panes are the
 * one place a sheet holds glass, standing on its own glass rather than on a card, since glass is
 * what the studio tunes; the sliders and switches are on solid cards, as Settings' are. Reset puts
 * Zenith's glass back.
 */
@Composable
fun GlassStudioSheet(visible: Boolean, tuning: GlassTuning, onChange: (GlassTuning) -> Unit, onDismiss: () -> Unit) {
    val apple = LocalApple.current
    val reduced = LocalMotionLevel.current.reduced
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Glass studio", maxHeightFraction = 0.88f) {
        // Three panes of the home screen's own glass: an icon, a bar, a drop. They stand on the
        // sheet's glass, not on a card, so they show the wallpaper as the home screen's glass does.
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).appearIn(delayMillis = staggerDelay(0), riseFrom = 14f),
        ) {
            Box(Modifier.size(64.dp).glass(RoundedCornerShape(20.dp), material = GlassMaterial.Clear))
            Box(Modifier.size(width = 120.dp, height = 46.dp).glass(RoundedCornerShape(50)))
            Box(Modifier.size(56.dp).glass(CircleShape, material = GlassMaterial.Clear))
        }
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            // The glass itself: one card, a slider to a row.
            Section(modifier = Modifier.appearIn(delayMillis = staggerDelay(1), riseFrom = 14f)) {
                ControlRow {
                    PillSlider("Bend", tuning.bend, GlassTuning.BEND, times(tuning.bend)) { onChange(tuning.copy(bend = it)) }
                }
                GroupDivider(inset = ROW_INSET)
                ControlRow {
                    PillSlider("Frost", tuning.frost, GlassTuning.FROST, signed(tuning.frost)) { onChange(tuning.copy(frost = it)) }
                }
                GroupDivider(inset = ROW_INSET)
                ControlRow {
                    PillSlider("Rim light", tuning.rim, GlassTuning.RIM, times(tuning.rim)) { onChange(tuning.copy(rim = it)) }
                }
                GroupDivider(inset = ROW_INSET)
                ControlRow {
                    PillSlider("Rainbow edge", tuning.rainbow, GlassTuning.RAINBOW, times(tuning.rainbow)) { onChange(tuning.copy(rainbow = it)) }
                }
                GroupDivider(inset = ROW_INSET)
                ControlRow {
                    PillSlider("Theme in the glass", tuning.tint, GlassTuning.TINT, "+${(tuning.tint * 100).roundToInt()}%") { onChange(tuning.copy(tint = it)) }
                }
            }
            // Where the light comes from: Zenith's, or your own on a dial, and whether it follows tilt.
            Section(header = "Light", modifier = Modifier.appearIn(delayMillis = staggerDelay(2), riseFrom = 14f)) {
                ControlRow {
                    Segmented(
                        options = listOf("zenith" to "Zenith's", "fixed" to "Your own"),
                        selected = if (tuning.lightAngle == null) "zenith" else "fixed",
                        onSelect = { choice ->
                            onChange(tuning.copy(lightAngle = if (choice == "zenith") null else tuning.lightAngle ?: GlassTuning.ZENITH_LIGHT))
                        },
                    )
                }
                // The dial opens out under the choice while the light is your own, and folds away
                // again when Zenith's is chosen.
                AnimatedVisibility(
                    visible = tuning.lightAngle != null,
                    enter = unfoldIn(reduced),
                    exit = unfoldOut(reduced),
                ) {
                    Column {
                        GroupDivider(inset = ROW_INSET)
                        ControlRow {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                LightDial(tuning.lightAngle ?: GlassTuning.ZENITH_LIGHT) { onChange(tuning.copy(lightAngle = it)) }
                                Spacer(Modifier.width(16.dp))
                                Text(
                                    "Drag round the dial: the light falls on every pane from where the dot is.",
                                    color = apple.secondary,
                                    fontSize = 13.sp,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
                GroupDivider(inset = ROW_INSET)
                ToggleRow(
                    title = "Light follows tilt",
                    subtitle = "With Tilt shine on, the light swings as you tip the phone",
                    checked = tuning.lightTilt,
                ) { onChange(tuning.copy(lightTilt = it)) }
            }
        }
        Spacer(Modifier.height(12.dp))
        ZenithButton(text = "Reset to Zenith's glass", enabled = !tuning.isDefault, onClick = { onChange(GlassTuning()) }, modifier = Modifier.fillMaxWidth())
    }
}

private fun times(value: Float) = "%.1f×".format(value)

private fun signed(value: Float) = when {
    value > 0.005f -> "+%.1f".format(value)
    value < -0.005f -> "−%.1f".format(-value)
    else -> "As made"
}

/**
 * A dial for where the light comes from, in Apple's colours for the card it stands on: a grey ring
 * with the light as a dot of the accent on it, and in the middle a little pane, the grey of a card
 * on a card, brightest on the side the light falls from. Drag round, or tap a point on it.
 */
@Composable
private fun LightDial(angle: Float, onChange: (Float) -> Unit) {
    val palette = LocalPalette.current
    val apple = LocalApple.current
    val tick = rememberTick()
    // The touches below are set up once, so they reach for the latest change to make, not the one
    // there was when the dial first appeared: that one would put back whatever else has been moved
    // on the sheet since.
    val latest by rememberUpdatedState(onChange)
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
                    latest(angleAt(at, size.width.toFloat()))
                }
            }
            .pointerInput(Unit) {
                var last = Float.NaN
                detectDragGestures { change, _ ->
                    val next = angleAt(change.position, size.width.toFloat())
                    if (next != last) {
                        tick()
                        last = next
                        latest(next)
                    }
                }
            },
    ) {
        val r = size.minDimension / 2f
        val ring = r - 8.dp.toPx()
        drawCircle(apple.faint, radius = ring, style = Stroke(2.dp.toPx()))
        val light = lightFrom(Offset.Zero, GlassTuning(lightAngle = shown))
        val length = kotlin.math.hypot(light.x, light.y)
        val dot = Offset(center.x + light.x / length * ring, center.y + light.y / length * ring)
        // The pane in the middle, lit from the dot's side, with a hairline round it so that its lit
        // side still shows on a white card.
        val pane = r * 0.62f
        val paneAt = Offset(center.x - pane / 2f, center.y - pane / 2f)
        val paneSize = Size(pane, pane)
        val paneCorner = CornerRadius(pane * 0.3f)
        drawRoundRect(apple.raised, topLeft = paneAt, size = paneSize, cornerRadius = paneCorner)
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color.White.copy(alpha = if (apple.dark) 0.55f else 1f), Color.White.copy(alpha = 0f)),
                start = Offset(center.x + light.x / length * pane / 2f, center.y + light.y / length * pane / 2f),
                end = Offset(center.x - light.x / length * pane / 2f, center.y - light.y / length * pane / 2f),
            ),
            topLeft = paneAt,
            size = paneSize,
            cornerRadius = paneCorner,
        )
        drawRoundRect(apple.separator, topLeft = paneAt, size = paneSize, cornerRadius = paneCorner, style = Stroke(1.dp.toPx()))
        drawCircle(palette.accent.copy(alpha = 0.35f), radius = 11.dp.toPx(), center = dot)
        drawCircle(palette.accent, radius = 6.dp.toPx(), center = dot)
    }
}

/**
 * A palette for each part of the day (Zenith Plus): each part on a card of its own, named with its
 * hours over it, and on the card the palettes to choose from in a row, None first. A part with none
 * leaves the palette as it is.
 */
@Composable
fun DayPaletteSheet(
    visible: Boolean,
    options: List<ZenithPalette>,
    chosen: Map<String, String>,
    onChange: (Map<String, String>) -> Unit,
    onDismiss: () -> Unit,
) {
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "By time of day", maxHeightFraction = 0.86f) {
        // On the sheet's glass, over the cards, in the glass's own grey; lined up with the names
        // of the cards under it.
        Note(
            "As each part of the day begins, its palette comes on. One you choose by hand stays until the next part.",
            Modifier.padding(horizontal = 12.dp),
        )
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            DayParts.all.forEachIndexed { index, part ->
                val current = chosen[part.id]
                Section(
                    header = "${part.label} · ${LookSchedule.clock(part.start)}–${LookSchedule.clock(DayParts.until(part))}",
                    modifier = Modifier.appearIn(delayMillis = staggerDelay(index), riseFrom = 14f),
                ) {
                    ControlRow(edgeToEdge = true) {
                        // It runs to the edges of its card, the first and last colours starting
                        // where the card's rows do.
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                        ) {
                            item(key = "none") {
                                Swatch(label = "None", colours = null, selected = current == null) { onChange(chosen - part.id) }
                            }
                            items(options, key = { it.id }) { option ->
                                Swatch(label = option.name, colours = listOf(option.accent, option.accentAlt), selected = current == option.id) {
                                    onChange(chosen + (part.id to option.id))
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ZenithButton(text = "Turn it off", enabled = chosen.isNotEmpty(), onClick = { onChange(emptyMap()) }, modifier = Modifier.fillMaxWidth())
    }
}

/**
 * How far each colour of a dot must stand from the card under it for the dot to need no edge.
 * Below this, as with Frost's near-white or Graphite's pale end on a white card, a hairline goes
 * round the dot so that it can still be seen.
 */
private const val PALE_EDGE = 1.3f

/**
 * One colour to choose, on a card: a dot, ringed in the accent when it is the one, with a tick on it
 * in the ink its colour needs, and its name under it. A dot with a colour too close to the card's
 * own is edged with a hairline. [colours] null is "none": an empty ring.
 */
@Composable
private fun Swatch(label: String, colours: List<Color>?, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val apple = LocalApple.current
    val tick = rememberTick()
    // The colour in the middle of the dot, where the tick sits: what the tick's ink is judged by.
    val middle = colours?.let { lerp(it.first(), it.last(), 0.5f) }
    val pale = colours != null && colours.any { contrast(it, apple.card) < PALE_EDGE }
    val ring by animateColorAsState(if (selected) palette.accent else Color.Transparent, tween(220), label = "swatchRing")
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
                        if (colours == null) {
                            Modifier.border(1.5.dp, apple.faint, CircleShape)
                        } else {
                            Modifier
                                .background(Brush.linearGradient(colours))
                                .then(if (pale) Modifier.border(1.dp, apple.faint, CircleShape) else Modifier)
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (mark > 0.01f) {
                    Icon(
                        Icons.Rounded.Check,
                        contentDescription = null,
                        // White on blue, near-black on amber or Frost; on the empty ring, the words' colour.
                        tint = if (middle == null) apple.words else inkOnTint(middle),
                        modifier = Modifier.size(18.dp).scale(mark),
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(label, color = if (selected) apple.words else apple.secondary, fontSize = 12.sp, maxLines = 1)
    }
}

/**
 * The colours a widget or a folder can have of its own, each clear in the light and the dark: a
 * widget's tile is painted in it, its words in the ink it needs, and a folder's glass takes it in.
 */
private val WIDGET_COLOURS = listOf(
    0xFFFF6B8BL to "Rose", 0xFFFF8A5BL to "Coral", 0xFFFFC14DL to "Amber", 0xFFB8E05AL to "Lime",
    0xFF4FE0A8L to "Mint", 0xFF3FD0D6L to "Teal", 0xFF4FB4FFL to "Sky", 0xFF5B7CFFL to "Blue",
    0xFF8B6CFFL to "Violet", 0xFFD66CFFL to "Orchid", 0xFFFF5FD2L to "Magenta", 0xFFE8EDF5L to "Frost",
)

/** A colour of its own for one of Zenith's widgets, or the theme's, from a card of colours: it changes as you tap. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WidgetColourSheet(visible: Boolean, current: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Colour") {
        Section(modifier = Modifier.appearIn(delayMillis = staggerDelay(0), riseFrom = 14f)) {
            ControlRow {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Swatch("Theme", listOf(palette.accent, palette.accentAlt), selected = current == 0L) { onPick(0L) }
                    WIDGET_COLOURS.forEach { (argb, name) ->
                        Swatch(name, listOf(Color(argb.toInt()), Color(argb.toInt())), selected = current == argb) { onPick(argb) }
                    }
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        ZenithButton(text = "Done", filled = true, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
    }
}

/** The fonts Zenith carries, who made them, and the licence they are shared under, in full, on a card. */
@Composable
fun FontLicenceSheet(visible: Boolean, onDismiss: () -> Unit) {
    val apple = LocalApple.current
    val context = LocalContext.current
    val text = remember {
        runCatching { context.resources.openRawResource(R.raw.font_licences).use { it.readBytes().decodeToString() } }.getOrDefault("")
    }
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Fonts", maxHeightFraction = 0.86f) {
        Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
            Section {
                Text(
                    text,
                    color = apple.words,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                )
            }
        }
    }
}
