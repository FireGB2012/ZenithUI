package com.gabriel.zenithui.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.theme.LocalApple
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.inkOnTint
import com.gabriel.zenithui.ui.theme.NumberFont
import com.gabriel.zenithui.ui.theme.ZenithFonts
import kotlinx.coroutines.launch

/**
 * One typeface to choose: a grey tile showing [sample] set in it, its name under it. The chosen
 * one is ringed in the accent and its tile takes a little of it, as Apple marks a choice; the
 * tiles pop up one after another as the page arrives.
 */
@Composable
private fun FontTile(name: String, sample: String, family: FontFamily, weight: FontWeight, selected: Boolean, turn: Int, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val apple = LocalApple.current
    val tick = rememberTick()
    val arrival = LocalSettingsRise.current
    val pop = if (arrival != null) {
        Modifier.appearIn(delayMillis = 140 + 26 * turn.coerceAtMost(12), fromScale = 0.4f, riseFrom = 0f, replay = arrival)
    } else {
        Modifier
    }
    val ring by animateColorAsState(if (selected) palette.accent else Color.Transparent, tween(220), label = "fontRing")
    val fill by animateColorAsState(if (selected) palette.accent.copy(alpha = 0.16f) else apple.field, tween(220), label = "fontFill")
    val inset by animateDpAsState(if (selected) 4.dp else 0.dp, spring(dampingRatio = 0.7f, stiffness = 420f), label = "fontInset")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .then(pop)
            .pressBounce(0.9f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (!selected) tick()
                onClick()
            },
    ) {
        Box(
            Modifier
                .size(width = 66.dp, height = 54.dp)
                .border(2.dp, ring, RoundedCornerShape(18.dp))
                .padding(inset)
                .clip(RoundedCornerShape(18.dp - inset))
                .background(fill),
            contentAlignment = Alignment.Center,
        ) {
            Text(sample, color = apple.words, fontFamily = family, fontWeight = weight, fontSize = 20.sp, maxLines = 1)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            name,
            color = if (selected) apple.words else apple.secondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** A row of [tiles] to scroll through, the chosen one brought into view as the page opens. */
@Composable
private fun TileRow(count: Int, selectedIndex: Int, tile: @Composable (Int) -> Unit) {
    val state = rememberLazyListState()
    LaunchedEffect(Unit) { if (selectedIndex > 2) state.scrollToItem((selectedIndex - 1).coerceAtLeast(0)) }
    LazyRow(state = state, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        itemsIndexed(List(count) { it }) { index, _ -> tile(index) }
    }
}

/** The typeface everything is set in. */
@Composable
internal fun FontChoice(selectedId: String, onSelect: (String) -> Unit) {
    val fonts = ZenithFonts.all
    val selected = ZenithFonts.byId(selectedId).id
    TileRow(fonts.size, fonts.indexOfFirst { it.id == selected }) { index ->
        val font = fonts[index]
        FontTile(font.label, "Aa", font.family, FontWeight.Medium, selected = font.id == selected, turn = index) { onSelect(font.id) }
    }
}

/** The typeface the clocks and other big numbers are set in. */
@Composable
internal fun NumberFontChoice(selectedId: String, textFont: String, onSelect: (String) -> Unit) {
    val options = ZenithFonts.forNumbers
    val selected = options.firstOrNull { it.first == selectedId }?.first ?: ZenithFonts.SAME
    val text = ZenithFonts.byId(textFont)
    TileRow(options.size, options.indexOfFirst { it.first == selected }) { index ->
        val (id, label) = options[index]
        val look: NumberFont = ZenithFonts.numbers(id, text)
        FontTile(label, "12:45", look.family, look.weight ?: FontWeight.Light, selected = id == selected, turn = index) { onSelect(id) }
    }
}

/** What each page turn is called, in the order [com.gabriel.zenithui.ui.home.PageEffects.styles] has them. */
private val TURN_NAMES = mapOf(
    "depth" to "Depth", "slide" to "Slide", "cube" to "Cube", "stack" to "Stack",
    "carousel" to "Carousel", "wheel" to "Wheel", "flip" to "Flip", "fold" to "Fold",
)

/**
 * The page turns, each a tile with two little pages caught part way through it, drawn by the very
 * transform the home screen uses. Tapping one chooses it and plays it through once.
 */
@Composable
internal fun PageTurnChoice(selected: String, onSelect: (String) -> Unit) {
    val styles = com.gabriel.zenithui.ui.home.PageEffects.styles
    TileRow(styles.size, styles.indexOf(selected).coerceAtLeast(0)) { index ->
        val style = styles[index]
        PageTurnTile(style, TURN_NAMES[style] ?: style, selected = style == selected, turn = index) { onSelect(style) }
    }
}

@Composable
private fun PageTurnTile(style: String, name: String, selected: Boolean, turn: Int, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val apple = LocalApple.current
    val tick = rememberTick()
    val reduced = com.gabriel.zenithui.ui.common.LocalMotionLevel.current.reduced
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // At rest a little over a third of the way through a turn, where each style looks most itself.
    val along = remember { androidx.compose.animation.core.Animatable(REST_TURN) }
    val arrival = LocalSettingsRise.current
    val pop = if (arrival != null) {
        Modifier.appearIn(delayMillis = 140 + 26 * turn.coerceAtMost(12), fromScale = 0.4f, riseFrom = 0f, replay = arrival)
    } else {
        Modifier
    }
    val ring by animateColorAsState(if (selected) palette.accent else Color.Transparent, tween(220), label = "turnRing")
    val fill by animateColorAsState(if (selected) palette.accent.copy(alpha = 0.16f) else apple.field, tween(220), label = "turnFill")
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .then(pop)
            .pressBounce(0.9f)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                if (!selected) tick()
                onClick()
                if (!reduced) {
                    scope.launch {
                        along.snapTo(0f)
                        along.animateTo(1f, tween(620, easing = androidx.compose.animation.core.FastOutSlowInEasing))
                        along.snapTo(0f)
                        along.animateTo(REST_TURN, spring(dampingRatio = 0.8f, stiffness = 300f))
                    }
                }
            },
    ) {
        Box(
            Modifier
                .size(width = 66.dp, height = 54.dp)
                .border(2.dp, ring, RoundedCornerShape(18.dp))
                .clip(RoundedCornerShape(18.dp))
                .background(fill),
            contentAlignment = Alignment.Center,
        ) {
            val page = apple.words.copy(alpha = 0.18f)
            val dot = palette.accent
            for (which in 0..1) {
                Box(
                    Modifier
                        .size(width = 30.dp, height = 40.dp)
                        .graphicsLayer {
                            // The page's distance from the middle, as the pager has it: one page wide apart.
                            val offset = along.value - which
                            val t = com.gabriel.zenithui.ui.home.PageEffects.transform(style, offset)
                            val gap = size.width * 1.25f
                            translationX = -offset * gap + t.shiftX * gap
                            alpha = t.alpha
                            scaleX = t.scale * t.squash
                            scaleY = t.scale
                            rotationY = t.rotationY
                            rotationZ = t.rotationZ
                            transformOrigin = TransformOrigin(t.pivotX, t.pivotY)
                            cameraDistance = 8f * density
                        }
                        .clip(RoundedCornerShape(7.dp))
                        .background(page),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.size(9.dp).clip(RoundedCornerShape(3.dp)).background(if (which == 0) dot else apple.secondary))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            name,
            color = if (selected) apple.words else apple.secondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )
    }
}

private const val REST_TURN = 0.38f

/**
 * A choice among more options than a segmented row has room for: Apple's grey capsules that wrap
 * onto a second line, the chosen one filling with the accent, its words in the ink it needs.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
internal fun ChoiceChips(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    val palette = LocalPalette.current
    val apple = LocalApple.current
    val tick = rememberTick()
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { (id, label) ->
            val on = id == selected
            val fill by animateColorAsState(if (on) palette.accent else apple.grey, tween(220), label = "chipFill")
            Text(
                label,
                color = if (on) inkOnTint(palette.accent) else apple.words,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .pressBounce(0.94f)
                    .clip(RoundedCornerShape(50))
                    .background(fill)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        if (!on) tick()
                        onSelect(id)
                    }
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            )
        }
    }
}
