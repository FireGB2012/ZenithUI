package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass

/** A row of options with one picked; the pill slides across to whichever you tap. */
@Composable
fun Segmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    val palette = LocalPalette.current
    val index = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)
    val tick = rememberTick()

    Box(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .glass(ZenithShapes.pill, strength = 0.8f, material = GlassMaterial.Clear)
            .padding(4.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val itemWidth = maxWidth / options.size
            // The pill slides to the option you picked and settles with a small bounce.
            val slide = animateDpAsState(itemWidth * index, Motion.snappy(), label = "segment")
            // Dark words on a light accent, white on a deep one: dark on violet was hard to read.
            val activeInk = if (palette.accent.luminance() > 0.3f) Color(0xFF0A0B12) else Color.White

            Box(
                Modifier
                    // Read while placing, so the slide moves the pill without recomposing the row.
                    .offset { IntOffset(slide.value.roundToPx(), 0) }
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(palette.accent.copy(alpha = 0.9f))
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
                        Text(
                            text = label,
                            color = textColour,
                            fontSize = 13.sp,
                            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}
