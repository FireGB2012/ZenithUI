package com.gabriel.zenithui.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.popIn
import com.gabriel.zenithui.ui.common.popOut
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.glass

/**
 * A pill of glass in Search's place, in the band between the page and the dock, when something has
 * been taken off the home screen: what it was, and Undo, round which a ring runs down for as long as
 * it can still be put back. Tapping Undo puts it back; when the time is up the pill gives way to
 * Search again ([bandSwapIn], [bandSwapOut]).
 *
 * While the home screen is edited it is not [timed]: it stands above edit mode's bar with the last
 * change made, and how many [more] are behind it, for as long as edit mode lasts, and each tap takes
 * back one more.
 */
@Composable
fun UndoPill(
    what: String?,
    key: Any?,
    millis: Long,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
    more: Int = 0,
    timed: Boolean = true,
) {
    val reduced = LocalMotionLevel.current.reduced
    // Kept while the pill goes, so its words do not empty out on its way.
    val shown = remember { arrayOf("", "") }
    if (what != null) {
        shown[0] = what
        shown[1] = if (more > 0) "+$more" else ""
    }
    AnimatedVisibility(
        visible = what != null,
        enter = bandSwapIn(reduced),
        exit = bandSwapOut(reduced),
        // A little taller than the band it stands in: it keeps its own height, centred on the band,
        // rather than being squeezed into it.
        modifier = modifier.wrapContentHeight(unbounded = true),
    ) {
        val palette = LocalPalette.current
        val tick = rememberTick()
        val left = remember(key) { Animatable(1f) }
        LaunchedEffect(key, timed) {
            if (timed && !reduced) left.animateTo(0f, tween(millis.toInt(), easing = LinearEasing))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .widthIn(max = 360.dp)
                .glass(RoundedCornerShape(50), solid = 0.4f)
                .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Text(
                shown[0],
                color = palette.onGlass,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (shown[1].isNotEmpty()) {
                // How many changes there are behind this one, each a tap of Undo away.
                Spacer(Modifier.width(6.dp))
                Text(shown[1], color = palette.onGlass.copy(alpha = 0.6f), fontSize = 14.sp, maxLines = 1)
            }
            Spacer(Modifier.width(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .pressBounce(0.9f)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        tick()
                        onUndo()
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                    // The time still left to put it back, running down; edited, there is no hurry.
                    if (timed) Canvas(Modifier.size(26.dp)) {
                        val stroke = 2.dp.toPx()
                        drawArc(
                            color = palette.accent,
                            startAngle = -90f,
                            sweepAngle = 360f * left.value,
                            useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                            topLeft = androidx.compose.ui.geometry.Offset(stroke / 2f, stroke / 2f),
                            size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                        )
                    }
                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null, tint = palette.accent, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("Undo", color = palette.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Search and Undo giving way to each other in the band, where they stand: the one going shrinks a
 * little as it fades, and the one coming grows from that size. With movement reduced, a fade.
 */
internal fun bandSwapIn(reduced: Boolean): EnterTransition = popIn(reduced, from = BAND_SWAP_SCALE, millis = 200)

internal fun bandSwapOut(reduced: Boolean): ExitTransition = popOut(reduced, to = BAND_SWAP_SCALE, millis = 140)

/** How small Search or Undo is as it comes or goes, so the two are seen to trade places. */
private const val BAND_SWAP_SCALE = 0.86f
