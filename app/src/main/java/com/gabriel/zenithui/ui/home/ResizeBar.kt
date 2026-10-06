package com.gabriel.zenithui.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.WidgetCell
import com.gabriel.zenithui.data.WidgetSize
import com.gabriel.zenithui.data.WidgetSizes
import com.gabriel.zenithui.data.Widgets
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberHaptics
import com.gabriel.zenithui.ui.common.shake
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.glass

/**
 * What can be done to the thing being resized without pulling its corner or dragging it: for a
 * widget, each of the sizes the gallery offers, one tap apiece; for anything, an arrow to move it
 * a cell at a time; and a way out. It sits at the foot of the screen, over the dock, which is not
 * part of what is being arranged.
 *
 * [onSize] and [onNudge] answer whether there was room. When there was not the bar shakes its
 * head and nothing moves.
 */
@Composable
fun ResizeBar(
    item: HomeItem,
    columns: Int,
    rows: Int,
    onSize: (WidgetSize) -> Boolean,
    onNudge: (dCol: Int, dRows: Int) -> Boolean,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val haptics = rememberHaptics()
    val spec = (item as? WidgetCell)?.let { Widgets.byType(it.type) }
    val sizes = remember(spec, columns, rows) { spec?.let { WidgetSizes.options(it, columns, rows) }.orEmpty() }
    /** Counts the taps that were refused: each one shakes the bar. */
    var refused by remember { mutableIntStateOf(0) }
    fun answer(done: Boolean) {
        if (done) haptics.tick() else refused++
    }

    Column(
        modifier
            .fillMaxWidth()
            .appearIn(fromScale = 0.92f, riseFrom = 24f)
            .shake(refused)
            .glass(RoundedCornerShape(28.dp), strength = 1.2f, solid = 0.45f)
            // A tap on the bar itself is not a tap on the page, which would end the resizing.
            .pointerInput(Unit) { detectTapGestures { } }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        // A widget with only one size it can be has nothing to choose between.
        if (sizes.size > 1) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                sizes.forEach { size ->
                    SizeChip(
                        name = size.name,
                        cells = "${size.spanX} × ${size.rows}",
                        selected = item.spanX == size.spanX && item.spanY == size.spanY,
                    ) { answer(onSize(size)) }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Move", color = palette.onGlassMuted, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(start = 6.dp))
            Spacer(Modifier.width(10.dp))
            ArrowButton(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, "Move left") { answer(onNudge(-1, 0)) }
            ArrowButton(Icons.Rounded.KeyboardArrowUp, "Move up") { answer(onNudge(0, -1)) }
            ArrowButton(Icons.Rounded.KeyboardArrowDown, "Move down") { answer(onNudge(0, 1)) }
            ArrowButton(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Move right") { answer(onNudge(1, 0)) }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .height(44.dp)
                    .pressBounce(0.94f)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Brush.horizontalGradient(listOf(palette.accent, palette.accentAlt)))
                    .clickable(onClick = onDone)
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Done", color = if (palette.dark) Color(0xFF0A0B12) else Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** One of a widget's sizes: its name over how many cells that is, lit when it is the size in use. */
@Composable
private fun SizeChip(name: String, cells: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val ink = if (selected) (if (palette.dark) Color(0xFF0A0B12) else Color.White) else palette.onGlass
    Column(
        Modifier
            .pressBounce(0.94f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) palette.accent.copy(alpha = 0.92f) else palette.onGlass.copy(alpha = 0.09f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(name, color = ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        Text(cells, color = ink.copy(alpha = 0.72f), fontSize = 12.sp, maxLines = 1)
    }
}

@Composable
private fun ArrowButton(icon: ImageVector, says: String, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Box(
        Modifier
            .padding(horizontal = 3.dp)
            .size(44.dp)
            .pressBounce(0.88f)
            .clip(CircleShape)
            .background(palette.onGlass.copy(alpha = 0.10f))
            .clickable(onClickLabel = says, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = says, tint = palette.onGlass, modifier = Modifier.size(24.dp))
    }
}
