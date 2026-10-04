package com.gabriel.zenithui.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.ui.theme.LocalPalette

/** A page dot, the gap between two, and how much longer the dot of the page in view is, in dp. */
private const val DOT = 6
private const val DOT_GAP = 6
private const val DOT_STRETCH = 16

/**
 * The dots under anything that turns in pages: one pill shared out between them, drawn rather
 * than laid out. As a page turns the pill stretches from its dot towards the next, and nothing
 * has to be measured again. [position] is where the pager is: a page number, with a fraction
 * while a page is turning.
 */
@Composable
fun PagerDots(count: Int, position: () -> Float, modifier: Modifier = Modifier) {
    val ink = LocalPalette.current.onGlass
    val pages = count.coerceAtLeast(1)
    Canvas(modifier.size(width = (DOT * pages + DOT_GAP * (pages - 1) + DOT_STRETCH).dp, height = DOT.dp)) {
        val at = position().coerceIn(0f, (pages - 1).toFloat())
        val radius = CornerRadius(size.height / 2f)
        var x = 0f
        for (index in 0 until pages) {
            val share = PageEffects.dotWidth(at, index)
            val width = (DOT + DOT_STRETCH * share).dp.toPx()
            drawRoundRect(
                color = ink,
                topLeft = Offset(x, 0f),
                size = Size(width, size.height),
                cornerRadius = radius,
                alpha = 0.28f + 0.57f * share,
            )
            x += width + DOT_GAP.dp.toPx()
        }
    }
}
