package com.gabriel.zenithui.ui.onboarding

import android.os.SystemClock
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.OptimizeJob
import com.gabriel.zenithui.data.Optimizing
import com.gabriel.zenithui.data.OptimizeWords
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.themed
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.max

/** How often the time left is worked out again: often enough to count down, not so often it flickers. */
private const val LEFT_TICK_MS = 500L

/** The bar: a channel of glass, and the accent filling it a little inside its rim. */
private val BAR_HEIGHT = 12.dp
private val BAR_INSET = 2.dp

/** One pass of the light along the filled part of the bar, while there is still work to do. */
private const val SHEEN_MS = 1600

/** Figures that keep their width, so "42%" does not shuffle sideways as it counts. */
private val TABULAR = TextStyle(fontFeatureSettings = "tnum")

/**
 * "Optimizing Zenith", on setup's card: the job in hand in words ("Caching app icons… 42 of 118"),
 * a bar of the accent filling a channel of glass as the work is done, and the share done in whole
 * percent with the time left, both worked out from the work itself ([Optimizing.fraction],
 * [Optimizing.leftMs]). Nothing on it is a clock made to look like progress: the bar moves when a
 * job does, and only then. A light runs along what is filled while work goes on, so a long job of
 * one step (the glass) still shows it is alive; with movement reduced it does not.
 *
 * [progress] is read here and nowhere else, since it changes with every icon; it holds null for
 * the moment before the work has started. [warmUp] is drawn on the card too, out of sight, given the
 * job in hand: setup's screen drawing the glass and the blur once is one of the jobs.
 */
@Composable
internal fun OptimizeCard(
    progress: StateFlow<Optimizing?>,
    glassy: Boolean,
    reduced: Boolean,
    modifier: Modifier = Modifier,
    warmUp: @Composable (OptimizeJob?) -> Unit = {},
) {
    val palette = LocalPalette.current
    val at by progress.collectAsState()
    // Before the first job reports, everything is still to do on a phone of no apps yet.
    val shown = at ?: remember { Optimizing.start(apps = 0, startedAtMs = SystemClock.elapsedRealtime()) }
    val now by produceState(SystemClock.elapsedRealtime()) {
        while (true) {
            delay(LEFT_TICK_MS)
            value = SystemClock.elapsedRealtime()
        }
    }
    val fill = animateFloatAsState(
        shown.fraction,
        if (reduced) snap() else spring(dampingRatio = 1f, stiffness = 140f),
        label = "optimizeFill",
    )
    val working = !shown.finished && !reduced
    val sheen = if (working) {
        rememberInfiniteTransition(label = "optimizeSheen")
            .animateFloat(0f, 1f, infiniteRepeatable(tween(SHEEN_MS, easing = LinearEasing)), label = "sheen")
    } else {
        remember { mutableFloatStateOf(-1f) }
    }
    val line = OptimizeWords.line(shown, glassy)
    val left = if (shown.finished) "Ready" else OptimizeWords.left(shown.leftMs(now))

    Box(modifier.fillMaxWidth()) {
        warmUp(shown.current?.job)
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .semantics(mergeDescendants = true) { }
                .progressSemantics(shown.fraction),
        ) {
            // A new job comes in from below; the count within one changes in place.
            AnimatedContent(
                targetState = shown.current?.job to line,
                contentKey = { it.first },
                transitionSpec = {
                    (fadeIn(tween(220, delayMillis = 60)) + slideInVertically(spring(dampingRatio = 1f, stiffness = 380f)) { it / 2 })
                        .togetherWith(fadeOut(tween(120)) + slideOutVertically(tween(160)) { -it / 3 })
                        .using(SizeTransform(clip = false))
                },
                label = "optimizeLine",
            ) { (_, words) ->
                Text(
                    text = words,
                    style = TABULAR,
                    color = palette.onGlass,
                    fontSize = 16.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = (-0.2).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(12.dp))
            ProgressChannel(fill = fill, sheen = sheen, accent = palette.accent)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = OptimizeWords.percent(shown),
                    style = TABULAR,
                    color = palette.onGlass,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = left,
                    style = TABULAR,
                    color = palette.onGlassMuted,
                    fontSize = 13.sp,
                    maxLines = 1,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

/**
 * A channel of glass with the accent filling it to [fill], in the theme's own corners, lit along
 * its top as glass is, and with [sheen] (0 to 1, or below 0 for none) running a light along what is
 * filled. Both are read while drawing, so the bar moves without anything being composed again.
 */
@Composable
private fun ProgressChannel(fill: State<Float>, sheen: State<Float>, accent: Color) {
    val shape = themed(ZenithShapes.pill)
    Box(
        Modifier
            .fillMaxWidth()
            .height(BAR_HEIGHT)
            .glass(ZenithShapes.pill, strength = 0.7f, material = GlassMaterial.Clear, shadow = false)
            .padding(BAR_INSET)
            .drawWithCache {
                val lit = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = 0.38f), Color.White.copy(alpha = 0f)),
                    startY = 0f,
                    endY = size.height * 0.7f,
                )
                val band = size.height * 6f
                val light = Brush.horizontalGradient(
                    listOf(Color.White.copy(alpha = 0f), Color.White.copy(alpha = 0.32f), Color.White.copy(alpha = 0f)),
                    startX = 0f,
                    endX = band,
                )
                val path = Path()
                onDrawBehind {
                    val p = fill.value.coerceIn(0f, 1f)
                    if (p <= 0.001f) return@onDrawBehind
                    // Never narrower than it is tall, so its first sliver is a round drop, not a line.
                    val width = max(size.height, size.width * p)
                    val outline = shape.createOutline(Size(width, size.height), layoutDirection, this)
                    drawOutline(outline, accent)
                    drawOutline(outline, lit)
                    val s = sheen.value
                    if (s >= 0f) {
                        path.reset()
                        path.addOutline(outline)
                        clipPath(path) {
                            translate(left = -band + (width + band) * s) {
                                drawRect(light, topLeft = Offset.Zero, size = Size(band, size.height))
                            }
                        }
                    }
                }
            },
    )
}
