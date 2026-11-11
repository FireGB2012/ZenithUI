package com.gabriel.zenithui.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
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
import com.gabriel.zenithui.ui.common.LocalLabelStyle
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.labelShadow
import com.gabriel.zenithui.ui.common.symbolShadowColour
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.GlassMotion
import com.gabriel.zenithui.ui.theme.LocalOnWallpaper
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import kotlin.math.ceil

/**
 * Search on the home screen: a small pill of clear glass in the band between the page and the dock,
 * with a magnifier and the word in white, as on an iPhone. Tapping it opens search, its glass
 * stretching into the search field ([SearchField]).
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
    // The shadow the names on the wallpaper have, darker with Name shadow on: the word, the
    // magnifier beside it, and the names are all white on the same pictures.
    val shadow = labelShadow(onWallpaper = true, legacyShadow = LocalLabelStyle.current.shadow, density = density)
    // How many dots there are, with a fraction while a page is being added or taken away, so its dot
    // grows out of the end of the row or is drawn back into it. Read only while drawing.
    val reduced = LocalMotionLevel.current.reduced
    val dotCount = animateFloatAsState(pageCount.toFloat(), if (reduced) snap() else Motion.page(), label = "pillDots")
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
                    // Made from the wallpaper: a little of its colour where the pill sits, glowing under it.
                    wallpaperTint = true,
                )
                .clickable(role = Role.Button, onClick = onOpen)
                .drawBehind {
                    val shown = dots().coerceIn(0f, 1f)
                    if (shown > 0.001f) pageDots(dotCount.value, position(), shown)
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
                Box {
                    // The word's shadow under the magnifier too: a copy of it in the shadow's colour,
                    // as far down, so the symbol reads wherever the word does.
                    if (shadow != null) {
                        Icon(
                            Icons.Rounded.Search,
                            contentDescription = null,
                            tint = symbolShadowColour(shadow),
                            modifier = Modifier
                                .size(16.dp)
                                .graphicsLayer {
                                    translationX = shadow.offset.x
                                    translationY = shadow.offset.y
                                },
                        )
                    }
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(6.dp))
                // White in Light and in Dark, with the names' shadow, so the word reads on a pale
                // wallpaper through glass that only mists it.
                Text(
                    text = "Search",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    style = LocalTextStyle.current.merge(TextStyle(shadow = shadow)),
                )
            }
        }
    }
}

/**
 * The page dots in the pill, [shown] of the way in: one for each of [count] pages, the dot of the
 * page at [position] long, sharing its length with the next one's while a page turns. [count] has a
 * fraction while a page is being added or taken away, and that page's dot is as much there as the
 * fraction says ([PageEffects.dotPresence]), the row making room for it as it comes. They grow in
 * from half their size, and the row is made smaller rather than cut off where it is wider than the
 * pill.
 */
private fun DrawScope.pageDots(count: Float, position: Float, shown: Float) {
    val dots = ceil(count - 0.001f).toInt()
    if (dots < 1) return
    val dot = DOT.toPx()
    val gap = DOT_GAP.toPx()
    val stretch = DOT_STRETCH.toPx()
    val at = position.coerceIn(0f, (dots - 1).toFloat())
    // The long dot's length is shared out between two dots while a page turns, so the row's length
    // changes only as a dot comes or goes.
    var row = 0f
    for (index in 0 until dots) {
        val there = PageEffects.dotPresence(count, index)
        row += (dot + stretch * PageEffects.dotWidth(at, index)) * there + (if (index > 0) gap * there else 0f)
    }
    val room = size.width - 2 * DOT_INSET.toPx()
    val fit = if (row > room && room > 0f) room / row else 1f
    scale(fit * (0.5f + 0.5f * shown)) {
        var x = (size.width - row) / 2f
        for (index in 0 until dots) {
            val there = PageEffects.dotPresence(count, index)
            if (index > 0) x += gap * there
            val share = PageEffects.dotWidth(at, index)
            val width = (dot + stretch * share) * there
            val tall = dot * there
            drawRoundRect(
                color = Color.White,
                topLeft = Offset(x, (size.height - tall) / 2f),
                size = Size(width, tall),
                cornerRadius = CornerRadius(tall / 2f),
                alpha = shown * (0.45f + 0.55f * share) * there,
            )
            x += width
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
