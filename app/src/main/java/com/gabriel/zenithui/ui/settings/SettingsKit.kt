package com.gabriel.zenithui.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.snap
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.IconShapes
import com.gabriel.zenithui.ui.common.BackButton
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.common.rollOver
import com.gabriel.zenithui.ui.common.wordsChange
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// The parts Settings and its sheets are built from, in one version each. Every control animates
// its own change, and ticks under the finger when haptics are on.

/** A back arrow, when there is somewhere to go back to, and the name of where you are. */
@Composable
internal fun SettingsHeader(title: String, canGoBack: Boolean, onBack: () -> Unit) {
    val palette = LocalPalette.current
    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        AnimatedVisibility(visible = canGoBack, enter = fadeIn(tween(180)), exit = fadeOut(tween(120))) {
            BackButton(onBack, Modifier.padding(end = 6.dp))
        }
        AnimatedContent(
            targetState = title,
            transitionSpec = { fadeIn(tween(200, delayMillis = 60)).togetherWith(fadeOut(tween(100))) },
            label = "settingsTitle",
        ) { text ->
            Text(text, color = palette.onGlass, fontSize = 21.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.3).sp)
        }
    }
}

/**
 * What makes the blocks of a page rise in one after another as it arrives: a key that changes
 * with each arrival, or null when the page is simply there (coming back to the groups from a
 * page, they are already known and do not perform again).
 */
internal val LocalSettingsRise = compositionLocalOf<Any?> { null }

/** How long after the block above it a block sets off, and how many blocks get a turn of their own. */
private const val RISE_STEP_MS = 34
private const val RISE_TURNS = 9

/** A block of a page, [index] down from its top, rising into place after the ones above it. */
@Composable
internal fun Modifier.riseIn(index: Int): Modifier {
    val arrival = LocalSettingsRise.current ?: return this
    return appearIn(
        delayMillis = RISE_STEP_MS * index.coerceIn(0, RISE_TURNS),
        fromScale = 0.985f,
        riseFrom = 54f,
        replay = arrival,
        gentle = true,
    )
}

/**
 * A rounded group of rows, the way a settings page is usually set out. With [rise], it is the
 * block that many down its page and rises in after the ones above it.
 */
@Composable
internal fun Group(padding: Dp = 0.dp, rise: Int? = null, content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .then(if (rise != null) Modifier.riseIn(rise) else Modifier)
            .clip(RoundedCornerShape(22.dp))
            .background(palette.onGlass.copy(alpha = 0.07f))
            .padding(padding),
        content = content,
    )
}

@Composable
internal fun GroupDivider(inset: Dp = 64.dp) {
    Box(
        Modifier
            .padding(start = inset)
            .fillMaxWidth()
            .height(1.dp)
            .background(LocalPalette.current.onGlass.copy(alpha = 0.07f))
    )
}

/** The name of a control that has no row of its own, set above it. */
@Composable
internal fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = LocalPalette.current.onGlassMuted,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

/**
 * A line of small print under a control: what the choice just made means. Make another choice and
 * the words change where they stand, the page taking up the difference smoothly if there are now
 * more or fewer lines of them.
 */
@Composable
internal fun Note(text: String, modifier: Modifier = Modifier) {
    val reduced = LocalMotionLevel.current.reduced
    AnimatedContent(
        targetState = text,
        transitionSpec = { wordsChange(reduced) },
        modifier = modifier.padding(horizontal = 4.dp),
        label = "note",
    ) { words ->
        Text(words, color = LocalPalette.current.onGlassMuted, fontSize = 12.sp)
    }
}

/**
 * A row that opens a page: a coloured tile, what it is, how it is set, and a chevron. Under a
 * finger the tile swells and tips and the chevron leans the way you are about to go, unless
 * movement is reduced; when the setting it describes changes, the line saying so cross-fades to
 * the new one.
 */
@Composable
internal fun NavRow(icon: ImageVector, tile: List<Color>, title: String, summary: String, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val touch = remember { MutableInteractionSource() }
    val pressed by touch.collectIsPressedAsState()
    val reduced = LocalMotionLevel.current.reduced
    // Read only where things are drawn, so a press redraws two layers and composes nothing again.
    val lean = animateFloatAsState(if (pressed && !reduced) 1f else 0f, Motion.press(), label = "navLean")
    Row(
        Modifier
            .fillMaxWidth()
            .pressBounce(0.98f)
            .clickable(interactionSource = touch, indication = LocalIndication.current, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .graphicsLayer {
                    val grown = 1f + 0.16f * lean.value
                    scaleX = grown
                    scaleY = grown
                    rotationZ = -9f * lean.value
                }
                .clip(RoundedCornerShape(11.dp))
                .background(Brush.linearGradient(tile)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = palette.onGlass, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            AnimatedContent(
                targetState = summary,
                transitionSpec = { fadeIn(tween(220, delayMillis = 60)).togetherWith(fadeOut(tween(120))) },
                label = "navSummary",
            ) { line ->
                Text(line, color = palette.onGlassMuted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Icon(
            Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = palette.onGlass,
            modifier = Modifier
                .size(22.dp)
                .graphicsLayer {
                    // Faint at rest; it darkens as it leans.
                    alpha = 0.35f + 0.4f * lean.value.coerceIn(0f, 1f)
                    translationX = 5.dp.toPx() * lean.value
                },
        )
    }
}

/** A row that does something: open an Android screen, a Plus feature, or start over. */
@Composable
internal fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    danger: Boolean = false,
    status: String? = null,
    modifier: Modifier = Modifier,
    /** Each time this goes up the icon turns once round: something was done. */
    spin: Int = 0,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val tint by animateColorAsState(if (danger) palette.destructive else palette.onGlass, tween(200), label = "actionTint")
    val turned by animateFloatAsState(
        targetValue = 360f * spin,
        animationSpec = if (reduced) snap() else spring(dampingRatio = 0.62f, stiffness = 110f),
        label = "actionSpin",
    )
    Row(
        modifier
            .fillMaxWidth()
            .pressBounce(0.98f)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(tint.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier
                    .size(19.dp)
                    .graphicsLayer { rotationZ = turned },
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            AnimatedContent(
                targetState = title,
                transitionSpec = { fadeIn(tween(180)).togetherWith(fadeOut(tween(100))) },
                label = "actionTitle",
            ) { text ->
                Text(text, color = tint, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            if (subtitle != null) {
                AnimatedContent(
                    targetState = subtitle,
                    transitionSpec = { fadeIn(tween(200, delayMillis = 40)).togetherWith(fadeOut(tween(100))) },
                    label = "actionSubtitle",
                ) { line ->
                    Text(line, color = palette.onGlassMuted, fontSize = 13.sp)
                }
            }
        }
        AnimatedVisibility(
            visible = status != null,
            enter = if (reduced) fadeIn(tween(Motion.REDUCED_MS)) else fadeIn(tween(160)) + scaleIn(Motion.snappy(), initialScale = 0.5f),
            exit = fadeOut(tween(120)),
        ) {
            // A row's value in grey, as iOS shows one: it says how things stand, it is not a button.
            Text(status.orEmpty(), color = palette.onGlassMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/**
 * A setting that is on or off. The whole row is the switch, not just the little pill: a finger
 * anywhere on it is felt by the knob, and when what the switch means changes with it, the line
 * saying so changes where it stands.
 */
@Composable
internal fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    val reduced = LocalMotionLevel.current.reduced
    val touch = remember { MutableInteractionSource() }
    val pressed by touch.collectIsPressedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(interactionSource = touch, indication = null) {
                tick()
                onChange(!checked)
            }
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = palette.onGlass, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            if (subtitle != null) {
                AnimatedContent(
                    targetState = subtitle,
                    transitionSpec = { wordsChange(reduced) },
                    label = "toggleSubtitle",
                ) { line ->
                    Text(line, color = palette.onGlassMuted, fontSize = 13.sp)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Toggle(checked, pressed)
    }
}

/**
 * The switch itself: the knob springs across and the track fills with iOS's green, as an iPhone's
 * switches do. While it is [pressed] the knob spreads out towards the side it is about to go to, as
 * something soft does under a finger, and draws itself up again as it gets there.
 */
@Composable
internal fun Toggle(checked: Boolean, pressed: Boolean = false) {
    val palette = LocalPalette.current
    val progress by animateFloatAsState(
        if (checked) 1f else 0f,
        spring(dampingRatio = 0.72f, stiffness = 520f),
        label = "toggle",
    )
    val spread by animateFloatAsState(if (pressed) 1f else 0f, spring(dampingRatio = 0.7f, stiffness = 600f), label = "toggleSpread")
    val track = lerp(palette.onGlass.copy(alpha = 0.16f), palette.switchOn, progress.coerceIn(0f, 1f))
    Box(
        Modifier
            .size(width = 50.dp, height = 30.dp)
            .clip(CircleShape)
            .background(track)
            .padding(3.dp),
    ) {
        val wide = 24.dp + 6.dp * spread
        Box(
            Modifier
                // Its outer edge stays where it is and it spreads inwards, so it never leaves the track.
                .offset(x = (44.dp - wide) * progress)
                .size(width = wide, height = 24.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/**
 * A slider you can grab anywhere: the whole bar is the control, it fills with the accent as the
 * value grows, and the name and number sit inside it. Tapping jumps, dragging follows the finger,
 * and every tenth of the way gives a tick.
 */
@Composable
internal fun PillSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    display: String,
    modifier: Modifier = Modifier,
    onChange: (Float) -> Unit,
) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    var width by remember { mutableFloatStateOf(1f) }
    val latest by rememberUpdatedState(onChange)
    val span = range.endInclusive - range.start
    val fraction = ((value - range.start) / span).coerceIn(0f, 1f)
    // A tap glides the fill to where you touched; a drag is followed almost exactly.
    val shown = animateFloatAsState(fraction, spring(dampingRatio = 1f, stiffness = 1400f), label = "slider")
    var step by remember { mutableIntStateOf((fraction * 10).roundToInt()) }
    // In your hand the bar swells a touch and its fill brightens, and the number it is being set
    // to stands out; let go and it all settles back. Read only where it is drawn.
    var held by remember { mutableStateOf(false) }
    val grip = animateFloatAsState(if (held) 1f else 0f, Motion.press(), label = "sliderGrip")

    fun setFrom(x: Float) {
        val at = (x / width).coerceIn(0f, 1f)
        val now = (at * 10).roundToInt()
        if (now != step) {
            step = now
            tick()
        }
        latest(range.start + at * span)
    }

    Box(
        modifier
            .fillMaxWidth()
            .height(52.dp)
            .graphicsLayer {
                val grown = 1f + 0.012f * grip.value
                scaleX = grown
                scaleY = grown
            }
            .clip(RoundedCornerShape(16.dp))
            .background(palette.onGlass.copy(alpha = 0.08f))
            // The fill is drawn, not laid out, so following a finger composes nothing again.
            .drawBehind {
                drawRect(
                    color = palette.accent,
                    size = Size(size.width * shown.value.coerceIn(0f, 1f), size.height),
                    alpha = 0.45f + 0.17f * grip.value.coerceIn(0f, 1f),
                )
            }
            .onSizeChanged { width = it.width.toFloat().coerceAtLeast(1f) }
            // Only watches, for as long as a finger is down: the tap and the drag are below.
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    held = true
                    try {
                        do {
                            val event = awaitPointerEvent()
                        } while (event.changes.any { it.pressed })
                    } finally {
                        held = false
                    }
                }
            }
            .pointerInput(range) { detectTapGestures { setFrom(it.x) } }
            .pointerInput(range) {
                detectHorizontalDragGestures(onDragStart = { setFrom(it.x) }) { change, _ ->
                    change.consume()
                    setFrom(change.position.x)
                }
            },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(label, color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(
                display,
                color = palette.onGlass.copy(alpha = 0.8f),
                fontSize = 14.sp,
                modifier = Modifier.graphicsLayer {
                    val grown = 1f + 0.12f * grip.value
                    scaleX = grown
                    scaleY = grown
                    // From its own end of the bar, so it grows inwards and is never cut off.
                    transformOrigin = TransformOrigin(1f, 0.5f)
                },
            )
        }
    }
}

/**
 * A number that goes up and down a step at a time: what it counts, the number, and a minus and a
 * plus either side of it. At the end of its range that button dims and does nothing.
 */
@Composable
internal fun StepperRow(title: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    val reduced = LocalMotionLevel.current.reduced
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = palette.onGlass, fontSize = 16.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        listOf(-1, 1).forEach { step ->
            val next = value + step
            val can = next in range
            if (step == 1) {
                // One more and the number rolls up; one fewer and it rolls down.
                AnimatedContent(
                    targetState = value,
                    transitionSpec = { rollOver(reduced, down = targetState < initialState) },
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.width(44.dp),
                    label = "stepperNumber",
                ) { number ->
                    Text(
                        "$number",
                        color = palette.onGlass,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(44.dp),
                    )
                }
            }
            Box(
                Modifier
                    .size(44.dp)
                    .pressBounce(0.9f, enabled = can)
                    .clip(CircleShape)
                    .background(palette.onGlass.copy(alpha = if (can) 0.10f else 0.04f))
                    .clickable(enabled = can, onClickLabel = if (step < 0) "One fewer" else "One more") {
                        tick()
                        onChange(next)
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (step < 0) Icons.Rounded.Remove else Icons.Rounded.Add,
                    contentDescription = if (step < 0) "Fewer $title" else "More $title",
                    tint = palette.onGlass.copy(alpha = if (can) 1f else 0.3f),
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

/** One option of several, marked with a filled dot. */
@Composable
internal fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    val ring by animateColorAsState(
        if (selected) palette.accent else palette.onGlass.copy(alpha = 0.25f),
        tween(220),
        label = "choiceRing",
    )
    val dot by animateDpAsState(if (selected) 10.dp else 0.dp, spring(dampingRatio = 0.6f, stiffness = 500f), label = "choiceDot")
    Row(
        Modifier
            .fillMaxWidth()
            .pressBounce(0.98f)
            .clickable {
                if (!selected) tick()
                onClick()
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(22.dp).border(2.dp, ring, CircleShape), contentAlignment = Alignment.Center) {
            Box(Modifier.size(dot).clip(CircleShape).background(palette.accent))
        }
        Spacer(Modifier.width(14.dp))
        Text(label, color = palette.onGlass, fontSize = 15.sp)
    }
}

/**
 * Every palette you can use, as dots that wrap onto as many lines as they need, so none is cut
 * off at the edge. [onMake], when given, adds a last dot that opens the theme maker.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun PaletteGrid(
    options: List<ZenithPalette>,
    selectedId: String,
    onSelect: (ZenithPalette) -> Unit,
    onMake: (() -> Unit)? = null,
) {
    FlowRow(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        options.forEachIndexed { index, option ->
            PaletteDot(option.name, listOf(option.accent, option.accentAlt), selected = option.id == selectedId, turn = index) { onSelect(option) }
        }
        if (onMake != null) {
            val palette = LocalPalette.current
            PaletteDot(
                "Your own",
                listOf(palette.onGlass.copy(alpha = 0.14f), palette.onGlass.copy(alpha = 0.14f)),
                selected = false,
                turn = options.size,
                icon = Icons.Rounded.Add,
                onClick = onMake,
            )
        }
    }
}

@Composable
private fun PaletteDot(name: String, colours: List<Color>, selected: Boolean, turn: Int, icon: ImageVector? = null, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    // As the page arrives the dots pop up one after another, like a row of lights coming on.
    val arrival = LocalSettingsRise.current
    val pop = if (arrival != null) {
        Modifier.appearIn(delayMillis = 140 + 26 * turn.coerceAtMost(12), fromScale = 0.4f, riseFrom = 0f, replay = arrival)
    } else {
        Modifier
    }
    val ring by animateColorAsState(if (selected) palette.onGlass else Color.Transparent, tween(220), label = "dotRing")
    val inner by animateDpAsState(if (selected) 34.dp else 40.dp, spring(dampingRatio = 0.7f, stiffness = 420f), label = "dotInner")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(58.dp)
            .then(pop)
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
                    .background(Brush.linearGradient(colours)),
                contentAlignment = Alignment.Center,
            ) {
                if (icon != null) Icon(icon, contentDescription = null, tint = palette.onGlass, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            name,
            color = if (selected) palette.onGlass else palette.onGlassMuted,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** An icon shape shown as the shape itself, so there is nothing to imagine. */
@Composable
internal fun ShapeChoice(id: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    val fill by animateColorAsState(
        if (selected) palette.accent else palette.onGlass.copy(alpha = 0.16f),
        tween(220),
        label = "shapeFill",
    )
    val grow by animateFloatAsState(if (selected) 1f else 0.9f, spring(dampingRatio = 0.6f, stiffness = 420f), label = "shapeGrow")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .pressBounce(0.9f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (!selected) tick()
                onClick()
            },
    ) {
        Box(
            Modifier
                .size(50.dp)
                // Scaled rather than resized, so picking one never nudges its neighbours.
                .graphicsLayer {
                    scaleX = grow
                    scaleY = grow
                }
                .clip(ZenithShapes.forIconShape(id))
                .background(fill)
        )
        Spacer(Modifier.height(6.dp))
        Text(
            IconShapes.label(id),
            color = if (selected) palette.onGlass else palette.onGlassMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

// ------------------------------------------------------------------------------ found by search

/** The id of the option a search has just led to, for its row to show itself; null otherwise. */
internal val LocalSettingsHighlight = compositionLocalOf<String?> { null }

/**
 * Marks where one option of the [SettingsCatalog] is on its page. When a search leads here, the
 * page scrolls it into view and it pulses twice, so the eye lands on what was asked for.
 */
@Composable
internal fun Spot(id: String, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.pulseWhen(LocalSettingsHighlight.current == id), content = content)
}

@OptIn(ExperimentalFoundationApi::class)
internal fun Modifier.pulseWhen(active: Boolean): Modifier = composed {
    if (!active) return@composed this
    // A flash of the words' ink, not of a colour: it only says "here".
    val ink = LocalPalette.current.onGlass
    val glow = remember { Animatable(0f) }
    val place = remember { BringIntoViewRequester() }
    LaunchedEffect(Unit) {
        // Once the page has finished sliding in.
        delay(320)
        place.bringIntoView()
        repeat(2) {
            glow.animateTo(1f, tween(240))
            glow.animateTo(0f, tween(460))
        }
    }
    bringIntoViewRequester(place).drawWithContent {
        drawContent()
        if (glow.value > 0f) drawRect(ink, alpha = glow.value * 0.22f)
    }
}
