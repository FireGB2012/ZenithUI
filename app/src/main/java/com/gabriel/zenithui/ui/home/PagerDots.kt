package com.gabriel.zenithui.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.theme.LocalPalette
import kotlin.math.ceil
import kotlin.math.roundToInt

/** A page dot, the gap between two, and how much longer the dot of the page in view is, in dp. */
private const val DOT = 6
private const val DOT_GAP = 6
private const val DOT_STRETCH = 16

/**
 * The dots under anything that turns in pages: one pill shared out between them, drawn rather
 * than laid out. As a page turns the pill stretches from its dot towards the next, and nothing
 * has to be measured again. [position] is where the pager is: a page number, with a fraction
 * while a page is turning.
 *
 * A page added grows its dot out of the end of the row, and one taken away draws its dot back in,
 * the row making room as it goes; what stands beside the dots moves over with it.
 */
@Composable
fun PagerDots(count: Int, position: () -> Float, modifier: Modifier = Modifier) {
    val ink = LocalPalette.current.onGlass
    val reduced = LocalMotionLevel.current.reduced
    val pages = count.coerceAtLeast(1)
    // How many dots there are, with a fraction while one is on its way in or out.
    val dots by animateFloatAsState(pages.toFloat(), if (reduced) snap() else Motion.page(), label = "dots")
    Canvas(modifier.size(width = (DOT_STRETCH + (DOT + DOT_GAP) * dots.coerceAtLeast(1f) - DOT_GAP).dp, height = DOT.dp)) {
        val at = position().coerceIn(0f, (pages - 1).toFloat())
        var x = 0f
        for (index in 0 until ceil(dots - 0.001f).toInt()) {
            val there = PageEffects.dotPresence(dots, index)
            val share = PageEffects.dotWidth(at, index)
            val width = (DOT + DOT_STRETCH * share).dp.toPx() * there
            val tall = size.height * there
            drawRoundRect(
                color = ink,
                topLeft = Offset(x, (size.height - tall) / 2f),
                size = Size(width, tall),
                cornerRadius = CornerRadius(tall / 2f),
                alpha = (0.28f + 0.57f * share) * there,
            )
            x += width + DOT_GAP.dp.toPx() * there
        }
    }
}

/**
 * Which page is in view, in the style chosen under Home screen: [PagerDots]; a line, a short bar
 * gliding along a track as the pages turn; the page's number out of how many, the number rolling
 * over as it changes; or nothing.
 */
@Composable
fun PageIndicator(style: String, count: Int, position: () -> Float, modifier: Modifier = Modifier) {
    val ink = LocalPalette.current.onGlass
    val reduced = LocalMotionLevel.current.reduced
    val pages = count.coerceAtLeast(1)
    when (style) {
        "none" -> Unit
        "line" -> {
            val shown by animateFloatAsState(pages.toFloat(), if (reduced) snap() else Motion.page(), label = "lineCount")
            Canvas(modifier.size(width = LINE_WIDTH.dp, height = 3.dp)) {
                val track = size.width
                val bar = track / shown.coerceAtLeast(1f)
                val at = position().coerceIn(0f, (pages - 1).toFloat())
                drawRoundRect(ink, size = size, cornerRadius = CornerRadius(size.height / 2f), alpha = 0.22f)
                drawRoundRect(ink, topLeft = Offset(at * bar, 0f), size = Size(bar, size.height), cornerRadius = CornerRadius(size.height / 2f), alpha = 0.85f)
            }
        }
        "numbers" -> {
            // Settled on the nearest page, so the number changes as a page passes half way.
            val page by remember { derivedStateOf { position().roundToInt().coerceIn(0, pages - 1) } }
            Row(modifier, verticalAlignment = Alignment.CenterVertically) {
                AnimatedContent(
                    targetState = page,
                    transitionSpec = { com.gabriel.zenithui.ui.common.rollOver(reduced, down = targetState < initialState) },
                    label = "pageNumber",
                ) { shown ->
                    Text("${shown + 1}", color = ink, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Text(" / $pages", color = ink.copy(alpha = 0.6f), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
        else -> PagerDots(count, position, modifier)
    }
}

private const val LINE_WIDTH = 64
