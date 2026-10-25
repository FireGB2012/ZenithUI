package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import kotlin.math.abs

/**
 * A row of options with one picked. The pill under it does not slide across as a block: the edge
 * facing the way it is going sets off at once and fast, and the edge behind follows on a softer
 * spring, so it stretches out towards the option you tapped and draws itself up when it gets
 * there, the way a drop of something thick moves. With movement reduced it is simply there.
 *
 * Given [icons], one for each option, the row is a little taller and each option's symbol stands
 * before a larger name: for a choice that changes how everything else on the page looks, which
 * should be seen at a glance.
 */
@Composable
fun Segmented(
    options: List<Pair<String, String>>,
    selected: String,
    onSelect: (String) -> Unit,
    icons: List<ImageVector>? = null,
) {
    val palette = LocalPalette.current
    val large = icons != null
    val index = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val tick = rememberTick()
    val reduced = LocalMotionLevel.current.reduced
    // Where the pill stands, and which way it last went: right is 1.
    val travel = remember { intArrayOf(index, 1) }
    if (travel[0] != index) {
        travel[1] = if (index > travel[0]) 1 else -1
        travel[0] = index
    }
    val rightwards = travel[1] > 0

    Box(
        Modifier
            .fillMaxWidth()
            .height(if (large) 52.dp else 46.dp)
            .glass(ZenithShapes.pill, strength = 0.8f, material = GlassMaterial.Clear)
            .padding(4.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val itemWidth = maxWidth / options.size
            val left = animateDpAsState(
                targetValue = itemWidth * index,
                animationSpec = if (reduced) snap() else if (rightwards) PILL_TRAILS else PILL_LEADS,
                label = "segmentLeft",
            )
            val right = animateDpAsState(
                targetValue = itemWidth * (index + 1),
                animationSpec = if (reduced) snap() else if (rightwards) PILL_LEADS else PILL_TRAILS,
                label = "segmentRight",
            )
            // Dark words on a light accent, white on a deep one: dark on violet was hard to read.
            val activeInk = if (palette.accent.luminance() > 0.3f) Color(0xFF0A0B12) else Color.White
            val fill = palette.accent.copy(alpha = 0.9f)

            Box(
                Modifier
                    .fillMaxSize()
                    // Read while drawing, so the pill moves without the row being composed again.
                    .drawBehind {
                        val from = left.value.toPx()
                        val to = right.value.toPx()
                        drawRoundRect(
                            color = fill,
                            topLeft = Offset(minOf(from, to), 0f),
                            size = Size(abs(to - from), size.height),
                            cornerRadius = CornerRadius(size.height / 2f),
                        )
                    }
            )

            Row(Modifier.fillMaxSize()) {
                options.forEachIndexed { position, (id, label) ->
                    val active = position == index
                    val textColour by animateColorAsState(
                        targetValue = if (active) activeInk else palette.onGlassMuted,
                        animationSpec = tween(260),
                        label = "segmentText",
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pressBounce(0.94f)
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable {
                                if (!active) tick()
                                onSelect(id)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            icons?.getOrNull(position)?.let { icon ->
                                Icon(icon, contentDescription = null, tint = textColour, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(7.dp))
                            }
                            Text(
                                text = label,
                                color = textColour,
                                fontSize = if (large) 15.sp else 13.sp,
                                fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** The pill's leading edge: off at once, and there with next to nothing to spare. */
private val PILL_LEADS = spring<Dp>(dampingRatio = 0.86f, stiffness = 640f)

/** Its trailing edge: later, slower, and a little past where it stops before it comes back. */
private val PILL_TRAILS = spring<Dp>(dampingRatio = 0.7f, stiffness = 210f)
