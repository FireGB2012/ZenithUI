package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import kotlinx.coroutines.delay
import com.gabriel.zenithui.ui.theme.ButtonKind
import com.gabriel.zenithui.ui.common.ZenithButton
import com.gabriel.zenithui.ui.theme.LocalApple
import com.gabriel.zenithui.ui.theme.inkOnTint
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.Role

/** How far a piece of a screen travels as it arrives, and the beat between neighbours. */
private val RISE_DISTANCE = 14.dp
private const val RISE_STAGGER_MS = 60L

/**
 * A piece of a screen arriving: a short rise and a fade, critically damped so it lands without a
 * wobble. [order] staggers neighbours by a beat. With [play] false it is simply there — used when
 * you come back to a screen you have already seen, where replaying the entrance would feel slow.
 */
fun Modifier.rise(order: Int, play: Boolean = true): Modifier = composed {
    val progress = remember { Animatable(if (play) 0f else 1f) }
    val distance = with(LocalDensity.current) { RISE_DISTANCE.toPx() }
    LaunchedEffect(Unit) {
        if (progress.value >= 1f) return@LaunchedEffect
        delay(order * RISE_STAGGER_MS)
        progress.animateTo(1f, spring(dampingRatio = 1f, stiffness = 220f))
    }
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * distance
    }
}

/**
 * The same rise, driven by a flag: arrives when [visible] turns true and sinks away when it turns
 * false. Something that starts out visible is simply there.
 */
fun Modifier.reveal(visible: Boolean): Modifier = composed {
    val progress = remember { Animatable(if (visible) 1f else 0f) }
    val distance = with(LocalDensity.current) { RISE_DISTANCE.toPx() }
    LaunchedEffect(visible) {
        progress.animateTo(if (visible) 1f else 0f, spring(dampingRatio = 1f, stiffness = 220f))
    }
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * distance
    }
}

/** The small line over a heading that says where you are. */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = LocalApple.current.secondary,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Medium,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

/** Each screen's one big line, centred and set tight. */
@Composable
fun Headline(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = LocalApple.current.words,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-1.0).sp,
        textAlign = TextAlign.Center,
        modifier = modifier,
    )
}

@Composable
fun Lede(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = LocalApple.current.secondary,
        fontSize = 16.sp,
        lineHeight = 23.sp,
        textAlign = TextAlign.Center,
        modifier = modifier.widthIn(max = 320.dp),
    )
}

/**
 * The one button that moves you on, full width: Apple's main button, a solid pill of the accent
 * that squishes under a finger and wobbles back, its label swapping in place when what it will do
 * changes ([ZenithButton]).
 */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ZenithButton(text = text, onClick = onClick, modifier = modifier.fillMaxWidth(), kind = ButtonKind.Main)
}

/** The quiet way out, full width and easy to hit: words in the accent, as an iPhone's "Not Now" is. */
@Composable
fun QuietButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ZenithButton(text = text, onClick = onClick, modifier = modifier.fillMaxWidth(), kind = ButtonKind.Plain)
}

/** Where you are in setup: a few short marks in the words' colour, the current one longer and full. */
@Composable
fun StepMarks(current: Int, total: Int, modifier: Modifier = Modifier) {
    val words = LocalApple.current.words
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(total) { index ->
            val width by animateDpAsState(
                if (index == current) 22.dp else 10.dp,
                spring(dampingRatio = 1f, stiffness = 380f),
                label = "markWidth",
            )
            val color by animateColorAsState(
                when {
                    index == current -> words
                    index < current -> words.copy(alpha = 0.45f)
                    else -> words.copy(alpha = 0.18f)
                },
                tween(300),
                label = "markColor",
            )
            Box(
                Modifier
                    .width(width)
                    .height(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

/**
 * A choice you can turn on and off, as Apple's chips are: Apple's grey when off, filled with the
 * accent when on, its words in the ink the accent needs. To TalkBack it is a box to tick.
 */
@Composable
fun ChoiceChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val apple = LocalApple.current
    val accent = LocalPalette.current.accent
    val fill by animateColorAsState(
        if (selected) accent else apple.field,
        tween(220),
        label = "chipFill",
    )
    val ink by animateColorAsState(
        if (selected) inkOnTint(accent) else apple.words,
        tween(220),
        label = "chipInk",
    )
    Box(
        modifier
            .height(44.dp)
            .pressBounce(0.94f)
            .clip(RoundedCornerShape(50))
            .background(fill)
            .toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() }),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = ink, fontSize = 15.sp, fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium)
    }
}

/** A heading block, centred: the eyebrow, the headline, and an optional line under it. */
@Composable
fun ScreenTitle(eyebrow: String, headline: String, lede: String?, play: Boolean, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Eyebrow(eyebrow, Modifier.rise(0, play))
        Spacer(Modifier.height(6.dp))
        Headline(headline, Modifier.rise(1, play))
        if (lede != null) {
            Spacer(Modifier.height(10.dp))
            Lede(lede, Modifier.rise(2, play))
        }
    }
}
