package com.gabriel.zenithui.ui.home

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.ui.theme.LocalPalette

/**
 * A widget's size drawn as the room it takes on the page: a little grid as many cells across as
 * the page has columns and as many down as the tallest size on offer has rows, with the cells this
 * size covers filled in. Side by side, Small, Medium, Wide, Tall and Large then look as different
 * as they are, and Wide is plainly the width of the page.
 *
 * [lit] takes the filled cells from the [ink] to the accent, 0 to 1. It is read while drawing, so a
 * shape can light up as the size it stands for comes on show without anything being composed
 * again. On something already lit in the accent, give it that thing's own ink instead.
 */
@Composable
fun SizeShape(
    spanX: Int,
    rows: Int,
    columns: Int,
    maxRows: Int,
    modifier: Modifier = Modifier,
    ink: Color = Color.Unspecified,
    lit: () -> Float = { 0f },
) {
    val palette = LocalPalette.current
    val drawn = if (ink.isSpecified) ink else palette.onGlass
    val accent = palette.accent
    Canvas(modifier) {
        val across = columns.coerceAtLeast(1)
        val down = maxRows.coerceAtLeast(rows).coerceAtLeast(1)
        val gap = 2.dp.toPx()
        val cell = minOf((size.width - gap * (across - 1)) / across, (size.height - gap * (down - 1)) / down)
        if (cell <= 0f) return@Canvas
        // Centred in the room it was given, whatever the grid's proportions.
        val left = (size.width - (cell * across + gap * (across - 1))) / 2f
        val top = (size.height - (cell * down + gap * (down - 1))) / 2f
        val filled = lerp(drawn.copy(alpha = 0.7f), accent, lit().coerceIn(0f, 1f))
        val empty = drawn.copy(alpha = 0.14f)
        val corner = CornerRadius(cell * 0.3f)
        for (row in 0 until down) {
            for (column in 0 until across) {
                drawRoundRect(
                    color = if (column < spanX && row < rows) filled else empty,
                    topLeft = Offset(left + column * (cell + gap), top + row * (cell + gap)),
                    size = Size(cell, cell),
                    cornerRadius = corner,
                )
            }
        }
    }
}
