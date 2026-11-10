package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Hello, in a handful of languages. Danish is in there because Zenith was made in Denmark. */
val HELLO_WORDS = listOf("hey there", "hola", "bonjour", "ciao", "hallo", "olá", "hej", "こんにちは")

/** "Hey there!", or with a name, "Hola, Gabriel!" — whichever word was on screen when you moved on. */
fun greetingFor(index: Int, name: String): String {
    val word = HELLO_WORDS[index.mod(HELLO_WORDS.size)].replaceFirstChar { it.uppercase() }
    val first = name.trim()
    return if (first.isEmpty()) "$word!" else "$word, $first!"
}

/**
 * The colours that flow through the greeting: the words' own ink, all three, so the greeting is
 * in plain words, as an iPhone's is. Kept as a list so the flow through the letters stays as it is.
 */
@Composable
fun greetingColours(): List<Color> {
    val palette = LocalPalette.current
    return List(3) { palette.onGlass }
}

/** A line in the greeting's colour, held still. Used to say hello again on the name screen. */
@Composable
fun GreetingLine(text: String, modifier: Modifier = Modifier) {
    val colours = greetingColours()
    Text(
        text = text,
        style = TextStyle(
            fontFamily = LocalZenithFont.current,
            brush = Brush.linearGradient(colours),
            fontSize = 20.sp,
            lineHeight = 26.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.3).sp,
            textAlign = TextAlign.Center,
        ),
        modifier = modifier,
    )
}

private const val WORD_ENTER_MS = 600
private const val WORD_HOLD_MS = 1400L
private const val WORD_EXIT_MS = 320
/** The next word starts this far into the last one leaving, so the two briefly cross. */
private const val WORD_OVERLAP_MS = 200L
private const val FIRST_WORD_DELAY_MS = 350L
private const val SUBLINE_AT_MS = 1000L
private const val READY_AT_MS = 1500L
private const val LARGEST_WORD_SP = 64
private const val SMALLEST_WORD_SP = 36
/** Room around each word so its blur is not cut off at the edge of its layer. */
private val WORD_PADDING = 16.dp

/**
 * The first thing you see: "hey there", written large, cycling through other languages while the
 * light rises behind it. Nothing to fill in; the button arrives after a beat, and a tap anywhere
 * brings it sooner. Tapping the word itself moves on to the next one.
 */
@Composable
fun HelloStep(
    startIndex: Int,
    firstTime: Boolean,
    animate: Boolean,
    clock: BackdropClock,
    onWord: (Int) -> Unit,
    onReady: () -> Unit,
) {
    val palette = LocalPalette.current
    var sublineShown by remember { mutableStateOf(!firstTime || !animate) }
    val ready by rememberUpdatedState(onReady)

    LaunchedEffect(Unit) {
        if (firstTime && animate) {
            delay(SUBLINE_AT_MS)
            sublineShown = true
            delay(READY_AT_MS - SUBLINE_AT_MS)
        }
        ready()
    }

    Box(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    sublineShown = true
                    ready()
                }
            },
        contentAlignment = BiasAlignment(0f, -0.18f),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .clearAndSetSemantics { contentDescription = "Hey there. Welcome to Zenith." },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GreetingWords(
                startIndex = startIndex,
                delayFirst = firstTime,
                animate = animate,
                clock = clock,
                onWord = onWord,
                onTap = {
                    sublineShown = true
                    ready()
                },
            )
            Text(
                text = "Welcome to Zenith.",
                color = palette.onGlassMuted,
                fontSize = 17.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
                // The word block keeps room for its blur below it; this takes the gap back to 12dp.
                modifier = Modifier
                    .padding(top = 12.dp - WORD_PADDING / 2)
                    .reveal(sublineShown),
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** One of the two places a word can be, so one can leave while the next arrives. */
@Stable
private class WordSlot {
    var index by mutableIntStateOf(-1)
    /** 0 → 1 while arriving, 1 → 2 while leaving. */
    val progress = Animatable(0f)
}

@Composable
private fun GreetingWords(
    startIndex: Int,
    delayFirst: Boolean,
    animate: Boolean,
    clock: BackdropClock,
    onWord: (Int) -> Unit,
    onTap: () -> Unit,
) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val slots = remember { listOf(WordSlot(), WordSlot()) }
    val skip = remember { Channel<Unit>(Channel.CONFLATED) }
    val wordChanged by rememberUpdatedState(onWord)
    val first = remember { startIndex.mod(HELLO_WORDS.size) }

    LaunchedEffect(animate) {
        if (!animate) {
            slots[0].index = first
            slots[0].progress.snapTo(1f)
            return@LaunchedEffect
        }
        if (delayFirst) delay(FIRST_WORD_DELAY_MS)
        var slot = 0
        var index = first
        while (true) {
            val current = slots[slot]
            current.index = index
            current.progress.snapTo(0f)
            wordChanged(index)
            // A tap that landed while the last word was leaving is spent; this one gets its full turn.
            skip.tryReceive()
            current.progress.animateTo(1f, tween(WORD_ENTER_MS, easing = Motion.emphasized))
            withTimeoutOrNull(WORD_HOLD_MS) { skip.receive() }
            launch { current.progress.animateTo(2f, tween(WORD_EXIT_MS, easing = FastOutLinearInEasing)) }
            delay(WORD_OVERLAP_MS)
            slot = 1 - slot
            index = (index + 1) % HELLO_WORDS.size
        }
    }

    val colours = greetingColours()
    val baseStyle = TextStyle(
        fontFamily = LocalZenithFont.current,
        color = palette.onGlass,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.034).em,
        textAlign = TextAlign.Center,
    )
    // Blur steps built once: 0, 2, 4 … 14dp. Asking for a new effect every frame would allocate.
    val blurs = remember(density) {
        List(8) { step ->
            if (step == 0) null else with(density) {
                val radius = (step * 2).dp.toPx()
                BlurEffect(radius, radius, TileMode.Decal)
            }
        }
    }
    val lift = with(density) { 10.dp.toPx() }
    val sink = with(density) { 8.dp.toPx() }
    val blockHeight = with(density) { (LARGEST_WORD_SP.sp.toPx() * 1.25f).toDp() } + WORD_PADDING * 2

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(blockHeight)
            .pressBounce(0.96f)
            .pointerInput(Unit) {
                detectTapGestures {
                    skip.trySend(Unit)
                    onTap()
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // Each word as large as fits on one line, so "こんにちは" and a big font setting still fit.
        val room = with(density) { (maxWidth - WORD_PADDING * 2).roundToPx() }
        val sizes: List<TextUnit> = remember(room, density.fontScale, measurer) {
            HELLO_WORDS.map { word ->
                var size = LARGEST_WORD_SP
                while (size > SMALLEST_WORD_SP) {
                    val measured = measurer.measure(word, baseStyle.copy(fontSize = size.sp), maxLines = 1, softWrap = false)
                    if (measured.size.width <= room) break
                    size -= 2
                }
                size.sp
            }
        }

        slots.forEach { slot ->
            key(slot) {
                val index = slot.index
                if (index >= 0) {
                    Text(
                        text = HELLO_WORDS[index],
                        style = baseStyle.copy(fontSize = sizes[index], lineHeight = sizes[index] * 1.1f),
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .graphicsLayer {
                                val v = slot.progress.value
                                val blurDp: Float
                                if (v <= 1f) {
                                    alpha = v
                                    val scale = 1.06f - 0.06f * v
                                    scaleX = scale
                                    scaleY = scale
                                    translationY = (1f - v) * lift
                                    blurDp = 12f * (1f - v)
                                } else {
                                    val q = v - 1f
                                    alpha = 1f - q
                                    val scale = 1f - 0.03f * q
                                    scaleX = scale
                                    scaleY = scale
                                    translationY = -q * sink
                                    blurDp = 10f * q
                                }
                                renderEffect = blurs[(blurDp / 2f).let { kotlin.math.round(it).toInt() }.coerceIn(0, blurs.lastIndex)]
                                // Its own layer, so the colour can be poured into the letters only.
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithCache {
                                val length = size.width * 1.5f
                                val brush = Brush.linearGradient(
                                    colours,
                                    start = Offset.Zero,
                                    end = Offset(length, 0f),
                                    tileMode = TileMode.Mirror,
                                )
                                // A mirrored gradient repeats every two lengths; flowing one full
                                // repeat every six seconds makes the loop seamless.
                                val cycle = length * 2f
                                onDrawWithContent {
                                    drawContent()
                                    val shift = ((clock.seconds % 6.0) / 6.0).toFloat() * cycle
                                    translate(left = -shift) {
                                        drawRect(brush, topLeft = Offset(shift, 0f), size = size, blendMode = BlendMode.SrcIn)
                                    }
                                }
                            }
                            .padding(WORD_PADDING),
                    )
                }
            }
        }
    }
}
