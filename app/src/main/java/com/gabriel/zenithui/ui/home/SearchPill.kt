package com.gabriel.zenithui.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.HomeGeometry
import com.gabriel.zenithui.ui.common.labelShadow
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.GlassMotion
import com.gabriel.zenithui.ui.theme.LocalOnWallpaper
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass

/**
 * Search on the home screen: a small pill of clear glass in the band between the page and the dock,
 * with a magnifier and the word in white, as on an iPhone. Tapping it opens search.
 *
 * While the pages turn its words can give way to the page dots, the one for the page in view long:
 * [dots] says how far, 0 for the words and 1 for the dots, and [position] is where the pager is, a
 * page number with a fraction while a page turns. Both are read only while drawing, so a turning
 * page redraws the pill and recomposes nothing, and with [dots] at 0 the pager is not looked at.
 *
 * [tracking] reads whatever moves the pill with a layer, such as its arrival, so the wallpaper in
 * its glass stays where it is on screen while the pill moves.
 */
@Composable
fun SearchPill(
    onOpen: () -> Unit,
    dots: () -> Float,
    pageCount: Int,
    position: () -> Float,
    modifier: Modifier = Modifier,
    tracking: (() -> Unit)? = null,
) {
    val density = LocalDensity.current.density
    // The pill stands on the wallpaper: the same clear glass in Light and in Dark.
    CompositionLocalProvider(LocalOnWallpaper provides true) {
        Box(
            modifier
                .height(HomeGeometry.SEARCH_PILL_HEIGHT_DP.dp)
                .widthIn(min = PILL_WIDTH)
                .glass(
                    ZenithShapes.pill,
                    material = GlassMaterial.Clear,
                    interactive = true,
                    // Clear glass over the wallpaper leans towards the finger, as Zenith's other clear
                    // glass does.
                    motion = GlassMotion.Lean,
                    tracking = tracking,
                )
                .clickable(role = Role.Button, onClick = onOpen)
                .drawBehind {
                    val shown = dots().coerceIn(0f, 1f)
                    if (shown > 0.001f) pageDots(pageCount, position(), shown)
                }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.graphicsLayer {
                    // The words shrink away as the dots come, and come back as they go.
                    val gone = dots().coerceIn(0f, 1f)
                    alpha = 1f - gone
                    val s = 1f - 0.5f * gone
                    scaleX = s
                    scaleY = s
                },
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                // White whatever the palette, with the soft shadow the names on the wallpaper have, so
                // the word reads on a pale wallpaper through glass that only mists it.
                Text(
                    text = "Search",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    style = LocalTextStyle.current.merge(TextStyle(shadow = labelShadow(onWallpaper = true, legacyShadow = false, density = density))),
                )
            }
        }
    }
}

/**
 * The page dots in the pill, [shown] of the way in: one for each of [count] pages, the dot of the
 * page at [position] long, sharing its length with the next one's while a page turns. They grow in
 * from half their size, and the row is made smaller rather than cut off where it is wider than the
 * pill.
 */
private fun DrawScope.pageDots(count: Int, position: Float, shown: Float) {
    if (count < 1) return
    val dot = DOT.toPx()
    val gap = DOT_GAP.toPx()
    val stretch = DOT_STRETCH.toPx()
    val at = position.coerceIn(0f, (count - 1).toFloat())
    // The long dot's length is shared out between two dots while a page turns, so the row's length
    // never changes.
    val row = count * dot + stretch + (count - 1) * gap
    val room = size.width - 2 * DOT_INSET.toPx()
    val fit = if (row > room && room > 0f) room / row else 1f
    scale(fit * (0.5f + 0.5f * shown)) {
        var x = (size.width - row) / 2f
        val top = (size.height - dot) / 2f
        for (index in 0 until count) {
            val share = PageEffects.dotWidth(at, index)
            val width = dot + stretch * share
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(x, top),
                size = Size(width, dot),
                cornerRadius = CornerRadius(dot / 2f),
                alpha = shown * (0.45f + 0.55f * share),
            )
            x += width + gap
        }
    }
}

/** The pill is never narrower than this, so the words and the dots have the same pill to stand in. */
private val PILL_WIDTH = 108.dp

/** A page dot, the gap between two, and how much longer the page in view's dot is. */
private val DOT = 7.dp
private val DOT_GAP = 6.dp
private val DOT_STRETCH = 13.dp

/** The least room left between the row of dots and the ends of the pill. */
private val DOT_INSET = 12.dp
