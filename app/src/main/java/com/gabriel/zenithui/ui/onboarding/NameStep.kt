package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.themed
import kotlinx.coroutines.delay

/**
 * Hello again, in whichever language was on screen, and a place to answer it. Stop typing for a
 * moment and setup answers back: "Nice to meet you, Gabriel." pops up under the field with a tick
 * and a ring of light from behind it. Everything hangs from the top, and the answer has its place
 * kept, so the keyboard coming up moves only the button and the answer arriving moves nothing.
 * Leaving it blank is fine — the button turns into a skip.
 */
@Composable
fun NameStep(
    name: String,
    onNameChange: (String) -> Unit,
    onDone: () -> Unit,
    greeting: Int,
    playIntro: Boolean,
    /** True once this screen has finished arriving; asking for the keyboard mid-transition stutters both. */
    canFocus: Boolean,
    /** Setup has just said nice to meet you, its words centred here: for the ring of light behind them. */
    onGreeted: (Offset) -> Unit,
) {
    val palette = LocalPalette.current
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }
    val tick = rememberTick()
    val greetedHere by rememberUpdatedState(onGreeted)
    val answer = remember { Spot() }
    // Coming back with a name already given, it has been said already: no second hello.
    var greeted by remember { mutableStateOf(name.trim()) }
    LaunchedEffect(name) {
        val first = name.trim()
        if (first.isEmpty()) {
            greeted = ""
            return@LaunchedEffect
        }
        if (first == greeted) return@LaunchedEffect
        // Only once you stop typing: an answer to every letter would be a chatter.
        delay(GREET_AFTER_MS)
        val fresh = greeted.isEmpty()
        greeted = first
        if (fresh) {
            tick()
            greetedHere(answer.centre)
        }
    }

    LaunchedEffect(canFocus) {
        if (!canFocus) return@LaunchedEffect
        delay(120)
        runCatching { focus.requestFocus() }
        keyboard?.show()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        GreetingLine(greetingFor(greeting, ""), Modifier.rise(0, playIntro))
        Spacer(Modifier.height(8.dp))
        Headline("What should we\ncall you?", Modifier.rise(1, playIntro))
        Spacer(Modifier.height(32.dp))

        val ring by animateColorAsState(
            if (focused) palette.accent else Color.Transparent,
            tween(240),
            label = "fieldRing",
        )
        BasicTextField(
            value = name,
            onValueChange = { if (it.length <= 24) onNameChange(it.replace("\n", "")) },
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = LocalZenithFont.current,
                color = palette.onGlass,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.4).sp,
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(palette.accent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { onDone() }),
            modifier = Modifier
                .fillMaxWidth()
                .rise(2, playIntro)
                .focusRequester(focus)
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .glass(ZenithShapes.pill, strength = 0.9f)
                        .border(1.5.dp, ring, themed(ZenithShapes.pill))
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (name.isEmpty()) {
                        Text(
                            "Your first name",
                            color = palette.onGlass.copy(alpha = 0.3f),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.4).sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                    inner()
                }
            },
        )

        Spacer(Modifier.height(14.dp))
        NiceToMeetYou(
            name = greeted,
            modifier = Modifier
                .fillMaxWidth()
                .height(ANSWER_HEIGHT)
                .onGloballyPositioned { answer.centre = it.boundsInRoot().center },
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Only used to say hello. It stays on this phone.",
            color = palette.onGlassMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.rise(3, playIntro),
        )
    }
}

/** How long after the last letter setup answers, and the room its answer always has. */
private const val GREET_AFTER_MS = 650L
private val ANSWER_HEIGHT = 30.dp

/**
 * Setup's answer to your name: "Nice to meet you, Gabriel." The first time it pops up with a little
 * bounce, the way a face lights up; a name changed after that fades in over the old one, and a name
 * taken away sinks out. Its room is kept either way, so nothing round it moves.
 */
@Composable
private fun NiceToMeetYou(name: String, modifier: Modifier = Modifier) {
    val pop = remember { Animatable(if (name.isEmpty()) 0f else 1f) }
    val rise = with(LocalDensity.current) { 10.dp.toPx() }
    // The last name said, kept while the line sinks away so it does not go blank first.
    var said by remember { mutableStateOf(name) }
    LaunchedEffect(name) {
        if (name.isEmpty()) {
            pop.animateTo(0f, tween(180))
        } else {
            said = name
            pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 420f))
        }
    }
    Box(
        modifier.graphicsLayer {
            val p = pop.value
            alpha = p.coerceIn(0f, 1f)
            // Past 1 on the way in, so it swells a touch and settles: the bounce.
            val scale = 0.82f + 0.18f * p
            scaleX = scale
            scaleY = scale
            translationY = (1f - p) * rise
        },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = said,
            transitionSpec = { fadeIn(tween(220, delayMillis = 60)).togetherWith(fadeOut(tween(140))) },
            label = "niceToMeetYou",
        ) { first ->
            GreetingLine(if (first.isEmpty()) "" else "Nice to meet you, $first.")
        }
    }
}
