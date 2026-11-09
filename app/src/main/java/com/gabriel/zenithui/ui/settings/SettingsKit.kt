package com.gabriel.zenithui.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.IconShapes
import com.gabriel.zenithui.ui.common.BackButton
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.popIn
import com.gabriel.zenithui.ui.common.popOut
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.common.rollOver
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.common.wordsChange
import com.gabriel.zenithui.ui.theme.CardKind
import com.gabriel.zenithui.ui.theme.LocalApple
import com.gabriel.zenithui.ui.theme.LocalOnGlass
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.ZenithPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.solidCard
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// The parts Settings is built from, as an iPhone builds its own: groups of rows on solid cards,
// each named small and grey above its card and explained in grey under it; switches, segmented
// controls and sliders in the accent. Sheets use the same parts. A row shades under a finger
// rather than bouncing; every control animates its own change, and ticks under the finger when
// haptics are on.

/** A back arrow, when there is somewhere to go back to, and the name of where you are: a sheet's header. */
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

// ------------------------------------------------------------------------- groups, as iOS's

/** How far in from a card's edge the words of a row with a square start, and its hairline: 16 + 30 + 14. */
internal val ROW_TEXT_INSET = 60.dp

/** How far in from a card's edge the words of a row without a square start. */
internal val ROW_INSET = 16.dp

/** The colour of the squares on this page's rows: the colour of the group the page is. */
internal val LocalGroupTile = compositionLocalOf<Color?> { null }

/** The grey of a square that has no group's colour to take: Apple's own. */
private val PLAIN_TILE = Color(0xFF8E8E93)

/**
 * A group of rows as an iPhone sets them out: [header], the group's name, small and grey above
 * it; the rows on a solid card; and [footer], what the choice in it means, in grey under the card.
 * A footer that comes or goes unfolds and folds away. Sections stand further apart than the rows
 * in them, so a page reads as its groups. With [rise], it is the block that many down its page and
 * rises in after the ones above it.
 */
@Composable
internal fun Section(
    header: String? = null,
    footer: String? = null,
    modifier: Modifier = Modifier,
    rise: Int? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val reduced = LocalMotionLevel.current.reduced
    // The last words it had, to fold away with rather than an empty line. Not state: it is only
    // read as the footer is composed.
    val lastFooter = remember { arrayOf(footer) }
    if (footer != null) lastFooter[0] = footer
    Column(
        modifier
            .fillMaxWidth()
            .then(if (rise != null) Modifier.riseIn(rise) else Modifier)
            .padding(top = 12.dp),
    ) {
        if (header != null) GroupName(header)
        // On the card, words are a card's, even inside a sheet.
        CompositionLocalProvider(LocalOnGlass provides false) {
            Column(Modifier.fillMaxWidth().solidCard(CardKind.Grouped), content = content)
        }
        AnimatedVisibility(visible = footer != null, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
            Note(lastFooter[0].orEmpty(), Modifier.padding(start = 12.dp, end = 12.dp, top = 7.dp))
        }
    }
}

/**
 * A control that is not a row of its own, inside a card: a segmented control, a slider, a row of
 * tiles to choose from. [title], when it has one, stands above it in the words' colour, as the name
 * of a row does. [edgeToEdge] is for a row of tiles that scrolls: it runs to the card's edges and
 * keeps its own margin inside them.
 */
@Composable
internal fun ControlRow(title: String? = null, edgeToEdge: Boolean = false, content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = if (edgeToEdge) 0.dp else 16.dp, vertical = 12.dp),
    ) {
        if (title != null) {
            Text(title, color = LocalApple.current.words, fontSize = 16.sp)
            Spacer(Modifier.height(10.dp))
        }
        content()
    }
}

/**
 * Grey words where they stand: on a sheet's glass the glass's own grey, which holds up over any
 * wallpaper; on a page or a card, Apple's.
 */
@Composable
internal fun quietWords(): Color = if (LocalOnGlass.current) LocalPalette.current.onGlassMuted else LocalApple.current.secondary

/** The name of a group of rows, as an iPhone writes it over the card: small grey capitals. */
@Composable
internal fun GroupName(text: String, modifier: Modifier = Modifier) {
    Text(
        text.uppercase(),
        color = quietWords(),
        fontSize = 13.sp,
        letterSpacing = 0.3.sp,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = 7.dp),
    )
}

/**
 * A card of rows with nothing over it or under it. [padding] is for a card that holds something
 * other than rows. With [rise], it is the block that many down its page.
 */
@Composable
internal fun Group(padding: Dp = 0.dp, rise: Int? = null, content: @Composable ColumnScope.() -> Unit) {
    CompositionLocalProvider(LocalOnGlass provides false) {
        Column(
            Modifier
                .fillMaxWidth()
                .then(if (rise != null) Modifier.riseIn(rise) else Modifier)
                .solidCard(CardKind.Grouped)
                .padding(padding),
            content = content,
        )
    }
}

/**
 * The hairline between two rows of a card. As an iPhone's does, it starts under the words of the
 * rows ([inset]) and runs to the card's far edge.
 */
@Composable
internal fun GroupDivider(inset: Dp = ROW_TEXT_INSET) {
    val hairline = with(LocalDensity.current) { 1f.toDp() }
    Box(
        Modifier
            .padding(start = inset)
            .fillMaxWidth()
            .height(hairline)
            .background(LocalApple.current.separator)
    )
}

/**
 * The rows of a card, one under another, with Apple's hairline between each two that show,
 * starting under their words ([inset]) and running to the card's far edge. Rows that come and go,
 * as a menu's do, need no dividers of their own: see [hairlinesAt]. A pressed row's shade covers
 * the line along its top, as an iPhone's does.
 */
@Composable
internal fun CardRows(modifier: Modifier = Modifier, inset: Dp = ROW_TEXT_INSET, content: @Composable () -> Unit) {
    val line = LocalApple.current.separator
    var lines by remember { mutableStateOf(emptyList<Int>()) }
    Layout(
        content = content,
        modifier = modifier.drawBehind {
            val start = inset.toPx()
            val left = if (layoutDirection == LayoutDirection.Ltr) start else 0f
            for (y in lines) drawRect(line, topLeft = Offset(left, y.toFloat()), size = Size(size.width - start, 1f))
        },
    ) { measurables, constraints ->
        val rows = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
        val width = if (constraints.hasBoundedWidth) constraints.maxWidth else rows.maxOfOrNull { it.width } ?: 0
        val height = rows.sumOf { it.height }
        val at = hairlinesAt(rows.map { it.height })
        layout(width.coerceIn(constraints.minWidth, constraints.maxWidth), height.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            var y = 0
            for (row in rows) {
                row.placeRelative(0, y)
                y += row.height
            }
            // Written, not read: the lines are only drawn, and the same list again changes nothing.
            lines = at
        }
    }
}

/**
 * Where the hairlines of a card of rows go, given each row's height from the top down: along the
 * top of every row that shows except the first. A row folded away to nothing has none of its own,
 * so a row that comes and goes never leaves two lines together, or one along the card's edge.
 */
internal fun hairlinesAt(heights: List<Int>): List<Int> {
    val lines = mutableListOf<Int>()
    var top = 0
    var seen = false
    for (height in heights) {
        if (height > 0) {
            if (seen) lines += top
            seen = true
        }
        top += height
    }
    return lines
}

/** The name of a control that has no row of its own, set above it. */
@Composable
internal fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = quietWords(),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        modifier = modifier.padding(horizontal = 4.dp),
    )
}

/**
 * Small grey print: what the choice just made means, as an iPhone writes it under a card. Make
 * another choice and the words change where they stand, the page taking up the difference
 * smoothly if there are now more or fewer lines of them.
 */
@Composable
internal fun Note(text: String, modifier: Modifier = Modifier) {
    val reduced = LocalMotionLevel.current.reduced
    val secondary = quietWords()
    AnimatedContent(
        targetState = text,
        transitionSpec = { wordsChange(reduced) },
        modifier = modifier.padding(horizontal = 4.dp),
        label = "note",
    ) { words ->
        Text(words, color = secondary, fontSize = 13.sp, lineHeight = 18.sp)
    }
}

/** What a box to type in stands on: a card of its own, Apple's grey (a search), or the card it is a row of. */
internal enum class FieldLook { Card, Search, Row }

/**
 * A box to type in, as an iPhone draws one: on a card of its own, as a row of the card it is on, or,
 * for a search, in Apple's grey with a magnifying glass. The words are the words' colour, what goes
 * there is said in grey until something is typed, and the cursor is the accent. Once something is
 * in a search, a grey round button clears it. What may be typed, and what the keyboard offers, are
 * the caller's: [onValueChange] can filter, [keyboardOptions] and [keyboardActions] are passed on.
 */
@Composable
internal fun FieldBox(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    look: FieldLook = FieldLook.Card,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    fontSize: TextUnit = 16.sp,
    textAlign: TextAlign = TextAlign.Start,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    /** Told when the box gains the keyboard, and when it loses it. */
    onFocusChanged: (Boolean) -> Unit = {},
) {
    val apple = LocalApple.current
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val search = look == FieldLook.Search
    Row(
        modifier
            .fillMaxWidth()
            .then(
                when (look) {
                    FieldLook.Card -> Modifier.solidCard(CardKind.Grouped)
                    FieldLook.Search -> Modifier.clip(RoundedCornerShape(10.dp)).background(apple.field)
                    FieldLook.Row -> Modifier
                }
            )
            .heightIn(min = if (search) 40.dp else 48.dp)
            .padding(start = if (search) 10.dp else 16.dp, end = if (search) 6.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (search) {
            Icon(Icons.Rounded.Search, contentDescription = null, tint = apple.secondary, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(7.dp))
        }
        Box(
            Modifier
                .weight(1f)
                .padding(vertical = if (singleLine) 0.dp else 13.dp),
            contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart,
        ) {
            if (value.isEmpty()) {
                Text(
                    placeholder,
                    color = apple.secondary,
                    fontSize = fontSize,
                    textAlign = textAlign,
                    maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                textStyle = TextStyle(fontFamily = LocalZenithFont.current, color = apple.words, fontSize = fontSize, textAlign = textAlign),
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                visualTransformation = visualTransformation,
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { onFocusChanged(it.isFocused) },
            )
        }
        if (search) {
            AnimatedVisibility(
                visible = value.isNotEmpty(),
                enter = popIn(reduced, millis = 140),
                exit = popOut(reduced, millis = 100),
            ) {
                Icon(
                    Icons.Rounded.Cancel,
                    contentDescription = "Clear",
                    tint = apple.secondary,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable { onValueChange("") }
                        .padding(5.dp),
                )
            }
        }
    }
}

/**
 * Apple's answer to a finger on a row: while it is held the row shades, and the shade lifts as it
 * is let go. Nothing moves, so it is the same with movement reduced. [touch] is the row's own,
 * shared with its click.
 */
@Composable
internal fun Modifier.rowPress(touch: MutableInteractionSource, enabled: Boolean = true): Modifier {
    val pressed by touch.collectIsPressedAsState()
    val colour = LocalApple.current.pressed
    val shade by animateFloatAsState(
        targetValue = if (pressed && enabled) 1f else 0f,
        animationSpec = if (pressed && enabled) snap() else tween(320),
        label = "rowShade",
    )
    return drawBehind { if (shade > 0f) drawRect(colour, alpha = shade) }
}

/** The small square at the start of a row, as an iPhone's Settings has: a symbol in white on a colour. */
@Composable
private fun Tile(icon: ImageVector, colour: Color, turned: () -> Float = { 0f }) {
    Box(
        Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(colour),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(19.dp)
                .graphicsLayer { rotationZ = turned() },
        )
    }
}

/** The chevron at the end of a row that opens something. */
@Composable
internal fun Chevron() {
    Icon(
        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
        contentDescription = null,
        tint = LocalApple.current.faint,
        modifier = Modifier.size(22.dp),
    )
}

/**
 * A row that opens a page: the group's square, its name, how it is set in grey under the name, and
 * a chevron. Under a finger it shades; when the setting it describes changes, the grey line
 * cross-fades to the new one.
 */
@Composable
internal fun NavRow(icon: ImageVector, tile: Color, title: String, summary: String, onClick: () -> Unit) {
    val apple = LocalApple.current
    val touch = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .rowPress(touch)
            .clickable(interactionSource = touch, indication = null, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(start = 16.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tile(icon, tile)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = apple.words, fontSize = 16.sp)
            AnimatedContent(
                targetState = summary,
                transitionSpec = { fadeIn(tween(220, delayMillis = 60)).togetherWith(fadeOut(tween(120))) },
                label = "navSummary",
            ) { line ->
                Text(line, color = apple.secondary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(6.dp))
        Chevron()
    }
}

/**
 * A row that does something: open an Android screen, a Plus feature, or start over. Its square is
 * in the colour of the group it is in ([LocalGroupTile]) unless it is given one of its own, and a
 * row that cannot be undone is red, square and words. [status] says at its end what it has done.
 */
@Composable
internal fun ActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    danger: Boolean = false,
    status: String? = null,
    modifier: Modifier = Modifier,
    /** Each time this goes up the symbol turns once round: something was done. */
    spin: Int = 0,
    /** The square's colour, where it should not be the group's. */
    tile: Color? = null,
    onClick: () -> Unit,
) {
    val apple = LocalApple.current
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val touch = remember { MutableInteractionSource() }
    val words by animateColorAsState(if (danger) apple.danger else apple.words, tween(200), label = "actionWords")
    val square = if (danger) apple.danger else tile ?: LocalGroupTile.current ?: PLAIN_TILE
    val turned = animateFloatAsState(
        targetValue = 360f * spin,
        animationSpec = if (reduced) snap() else spring(dampingRatio = 0.62f, stiffness = 110f),
        label = "actionSpin",
    )
    Row(
        modifier
            .fillMaxWidth()
            .rowPress(touch)
            .clickable(interactionSource = touch, indication = null, onClick = onClick)
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tile(icon, square, turned = { turned.value })
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            AnimatedContent(
                targetState = title,
                transitionSpec = { fadeIn(tween(180)).togetherWith(fadeOut(tween(100))) },
                label = "actionTitle",
            ) { text ->
                Text(text, color = words, fontSize = 16.sp)
            }
            if (subtitle != null) {
                AnimatedContent(
                    targetState = subtitle,
                    transitionSpec = { fadeIn(tween(200, delayMillis = 40)).togetherWith(fadeOut(tween(100))) },
                    label = "actionSubtitle",
                ) { line ->
                    Text(line, color = apple.secondary, fontSize = 14.sp)
                }
            }
        }
        AnimatedVisibility(
            visible = status != null,
            enter = if (reduced) fadeIn(tween(Motion.REDUCED_MS)) else fadeIn(tween(160)) + scaleIn(Motion.snappy(), initialScale = 0.5f),
            exit = fadeOut(tween(120)),
        ) {
            Text(status.orEmpty(), color = palette.accent, fontSize = 15.sp, modifier = Modifier.padding(start = 8.dp))
        }
    }
}

/**
 * A setting that is on or off. The whole row is the switch, not just the switch itself: a finger
 * anywhere on it is felt by the knob, and when what the switch means changes with it, the grey
 * line saying so changes where it stands.
 */
@Composable
internal fun ToggleRow(title: String, subtitle: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    val apple = LocalApple.current
    val tick = rememberTick()
    val reduced = LocalMotionLevel.current.reduced
    val touch = remember { MutableInteractionSource() }
    val pressed by touch.collectIsPressedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .toggleable(value = checked, interactionSource = touch, indication = null, role = Role.Switch) { on ->
                tick()
                onChange(on)
            }
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = apple.words, fontSize = 16.sp)
            if (subtitle != null) {
                AnimatedContent(
                    targetState = subtitle,
                    transitionSpec = { wordsChange(reduced) },
                    label = "toggleSubtitle",
                ) { line ->
                    Text(line, color = apple.secondary, fontSize = 14.sp)
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Toggle(checked, pressed)
    }
}

/**
 * The switch itself, as an iPhone's: a white knob that springs across a grey track, and the track
 * fills with the accent. While it is [pressed] the knob stretches out towards the side it is
 * about to go to, and draws itself up again as it gets there.
 */
@Composable
internal fun Toggle(checked: Boolean, pressed: Boolean = false) {
    val apple = LocalApple.current
    val accent = LocalPalette.current.accent
    val reduced = LocalMotionLevel.current.reduced
    val progress by animateFloatAsState(
        if (checked) 1f else 0f,
        if (reduced) tween(Motion.REDUCED_MS) else spring(dampingRatio = 0.78f, stiffness = 560f),
        label = "toggle",
    )
    val spread by animateFloatAsState(
        if (pressed && !reduced) 1f else 0f,
        spring(dampingRatio = 0.8f, stiffness = 650f),
        label = "toggleSpread",
    )
    val track = lerp(apple.grey, accent, progress.coerceIn(0f, 1f))
    Box(
        Modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(CircleShape)
            .background(track)
            .padding(2.dp),
    ) {
        val wide = 27.dp + 7.dp * spread
        Box(
            Modifier
                // Its outer edge stays where it is and it stretches inwards, so it never leaves the track.
                .offset(x = (47.dp - wide) * progress)
                .size(width = wide, height = 27.dp)
                .shadow(2.dp, CircleShape)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

/** The knob of a slider: round and white, as an iPhone's. */
private val KNOB = 28.dp

/**
 * A slider as an iPhone draws one, under its name and its value: a thin grey track, filled with
 * the accent up to a round white knob. Tapping glides the knob to the finger, dragging follows it,
 * and every tenth of the way gives a tick; the knob lands under the finger ([SliderGeometry]).
 * While it is held the knob swells a touch.
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
    val apple = LocalApple.current
    val accent = LocalPalette.current.accent
    val tick = rememberTick()
    val latest by rememberUpdatedState(onChange)
    var width by remember { mutableFloatStateOf(1f) }
    val knob = with(LocalDensity.current) { KNOB.toPx() }
    val fraction = SliderGeometry.fraction(value, range)
    // A tap glides the knob to where you touched; a drag is followed almost exactly.
    val shown = animateFloatAsState(fraction, spring(dampingRatio = 1f, stiffness = 1400f), label = "slider")
    var step by remember { mutableIntStateOf((fraction * 10).roundToInt()) }
    var held by remember { mutableStateOf(false) }
    val grip = animateFloatAsState(if (held) 1f else 0f, Motion.press(), label = "sliderGrip")

    fun setFrom(x: Float) {
        val at = SliderGeometry.fractionAt(x, width, knob)
        val now = (at * 10).roundToInt()
        if (now != step) {
            step = now
            tick()
        }
        latest(SliderGeometry.value(at, range))
    }

    Column(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, color = apple.words, fontSize = 16.sp, modifier = Modifier.weight(1f))
            Text(display, color = apple.secondary, fontSize = 15.sp)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(KNOB + 12.dp)
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
                }
                // The track and its fill are drawn, not laid out, so following a finger composes
                // nothing again.
                .drawBehind {
                    val thick = 4.dp.toPx()
                    val top = (size.height - thick) / 2f
                    val corner = CornerRadius(thick / 2f)
                    drawRoundRect(apple.grey, topLeft = Offset(0f, top), size = Size(size.width, thick), cornerRadius = corner)
                    val to = SliderGeometry.knobCentre(shown.value, size.width, knob)
                    drawRoundRect(accent, topLeft = Offset(0f, top), size = Size(to, thick), cornerRadius = corner)
                },
            contentAlignment = Alignment.CenterStart,
        ) {
            Box(
                Modifier
                    .size(KNOB)
                    .graphicsLayer {
                        translationX = SliderGeometry.knobCentre(shown.value, width, knob) - knob / 2f
                        val grown = 1f + 0.1f * grip.value
                        scaleX = grown
                        scaleY = grown
                    }
                    .shadow(3.dp, CircleShape)
                    .background(Color.White, CircleShape)
            )
        }
    }
}

/**
 * Where a slider's knob is drawn for a value, and which value is under a finger. As on an iPhone
 * the knob never hangs over the ends of its track: its middle travels from a knob's radius inside
 * one end to a knob's radius inside the other, and a touch is read along that same stretch, so the
 * knob lands under the finger. Widths are in pixels.
 */
internal object SliderGeometry {

    /** The middle of the knob, [fraction] of the way along a track [width] across. */
    fun knobCentre(fraction: Float, width: Float, knob: Float): Float {
        val travel = width - knob
        if (travel <= 0f) return width / 2f
        return knob / 2f + travel * fraction.coerceIn(0f, 1f)
    }

    /** How far along, 0 to 1, a touch at [x] puts the knob. */
    fun fractionAt(x: Float, width: Float, knob: Float): Float {
        val travel = width - knob
        if (travel <= 0f) return 0f
        return ((x - knob / 2f) / travel).coerceIn(0f, 1f)
    }

    /** How far along [range] a [value] is, 0 to 1. */
    fun fraction(value: Float, range: ClosedFloatingPointRange<Float>): Float {
        val span = range.endInclusive - range.start
        if (span <= 0f) return 0f
        return ((value - range.start) / span).coerceIn(0f, 1f)
    }

    /** The value [fraction] of the way along [range]. */
    fun value(fraction: Float, range: ClosedFloatingPointRange<Float>): Float =
        range.start + (range.endInclusive - range.start) * fraction.coerceIn(0f, 1f)
}

/**
 * A number that goes up and down a step at a time, as an iPhone's stepper does: what it counts,
 * the number, and a grey pill holding a minus and a plus. At the end of its range that half dims
 * and does nothing.
 */
@Composable
internal fun StepperRow(title: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    val apple = LocalApple.current
    val tick = rememberTick()
    val reduced = LocalMotionLevel.current.reduced
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = apple.words, fontSize = 16.sp, modifier = Modifier.weight(1f))
        // One more and the number rolls up; one fewer and it rolls down.
        AnimatedContent(
            targetState = value,
            transitionSpec = { rollOver(reduced, down = targetState < initialState) },
            contentAlignment = Alignment.Center,
            label = "stepperNumber",
        ) { number ->
            Text(
                "$number",
                color = apple.secondary,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(40.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Row(
            Modifier
                .height(34.dp)
                .clip(CircleShape)
                .background(apple.field),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            listOf(-1, 1).forEachIndexed { index, step ->
                if (index == 1) Box(Modifier.width(1.dp).height(18.dp).background(apple.separator))
                val next = value + step
                val can = next in range
                val touch = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .width(48.dp)
                        .fillMaxHeight()
                        .rowPress(touch, enabled = can)
                        .clickable(
                            interactionSource = touch,
                            indication = null,
                            enabled = can,
                            onClickLabel = if (step < 0) "One fewer" else "One more",
                        ) {
                            tick()
                            onChange(next)
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (step < 0) Icons.Rounded.Remove else Icons.Rounded.Add,
                        contentDescription = if (step < 0) "Fewer $title" else "More $title",
                        tint = apple.words.copy(alpha = if (can) 1f else 0.3f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

/** One option of several, as an iPhone lists them: its name, and a check in the accent by the one chosen. */
@Composable
internal fun ChoiceRow(label: String, selected: Boolean, onClick: () -> Unit) {
    val apple = LocalApple.current
    val accent = LocalPalette.current.accent
    val tick = rememberTick()
    val reduced = LocalMotionLevel.current.reduced
    val touch = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .rowPress(touch)
            .selectable(selected = selected, interactionSource = touch, indication = null, role = Role.RadioButton) {
                if (!selected) tick()
                onClick()
            }
            .heightIn(min = 48.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = apple.words, fontSize = 16.sp, modifier = Modifier.weight(1f))
        AnimatedVisibility(visible = selected, enter = popIn(reduced, millis = 160), exit = popOut(reduced, millis = 100)) {
            Icon(Icons.Rounded.Check, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        }
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
            val grey = LocalApple.current.grey
            PaletteDot(
                "Your own",
                listOf(grey, grey),
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
    val apple = LocalApple.current
    val tick = rememberTick()
    // As the page arrives the dots pop up one after another, like a row of lights coming on.
    val arrival = LocalSettingsRise.current
    val pop = if (arrival != null) {
        Modifier.appearIn(delayMillis = 140 + 26 * turn.coerceAtMost(12), fromScale = 0.4f, riseFrom = 0f, replay = arrival)
    } else {
        Modifier
    }
    val ring by animateColorAsState(if (selected) apple.words else Color.Transparent, tween(220), label = "dotRing")
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
                if (icon != null) Icon(icon, contentDescription = null, tint = apple.words, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            name,
            color = if (selected) apple.words else apple.secondary,
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
    val apple = LocalApple.current
    val palette = LocalPalette.current
    val tick = rememberTick()
    val fill by animateColorAsState(
        if (selected) palette.accent else apple.grey,
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
            color = if (selected) apple.words else apple.secondary,
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
    val accent = LocalPalette.current.accent
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
        if (glow.value > 0f) drawRect(accent, alpha = glow.value * 0.22f)
    }
}
