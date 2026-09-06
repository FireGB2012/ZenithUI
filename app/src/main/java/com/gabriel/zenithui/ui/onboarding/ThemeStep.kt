package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.GlassButton
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.SectionLabel
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.Palettes
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass

@Composable
fun ThemeStep(
    name: String,
    paletteId: String,
    appearance: String,
    glass: Float,
    onPalette: (String) -> Unit,
    onAppearance: (String) -> Unit,
    onGlass: (Float) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    val palette = LocalPalette.current

    Column(
        Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(46.dp))
        Text(
            text = if (name.isBlank()) "Pick your light" else "$name, pick your light",
            color = palette.onGlass,
            fontSize = 28.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.6).sp,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "Everything below updates live.",
            color = palette.onGlassMuted,
            fontSize = 14.sp,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            HomePreview(modifier = Modifier.fillMaxHeight())
        }

        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionLabel("Palette")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 2.dp),
            ) {
                items(Palettes.all, key = { it.id }) { option ->
                    PaletteChip(
                        name = option.name,
                        colors = listOf(option.accent, option.accentAlt),
                        selected = option.id == paletteId,
                        onClick = { onPalette(option.id) },
                    )
                }
            }

            SectionLabel("Appearance")
            Segmented(
                options = listOf("auto" to "Auto", "light" to "Light", "dark" to "Dark"),
                selected = appearance,
                onSelect = onAppearance,
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel("Glass", Modifier.width(62.dp))
                Slider(
                    value = glass,
                    onValueChange = onGlass,
                    valueRange = 0.2f..1.2f,
                    colors = SliderDefaults.colors(
                        thumbColor = palette.accent,
                        activeTrackColor = palette.accent,
                        inactiveTrackColor = palette.onGlass.copy(alpha = 0.18f),
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GlassButton(text = "Back", onClick = onBack)
            GlassButton(text = "Continue", onClick = onContinue, filled = true, modifier = Modifier.weight(1f))
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PaletteChip(name: String, colors: List<Color>, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val ring by animateColorAsState(
        if (selected) palette.onGlass.copy(alpha = 0.9f) else Color.Transparent,
        label = "ring",
    )
    val size by animateDpAsState(if (selected) 62.dp else 54.dp, spring(), label = "size")

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(66.dp)
                .border(2.dp, ring, RoundedCornerShape(percent = 50))
                .padding(4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                Modifier
                    .size(size)
                    .pressBounce(0.9f)
                    .clip(RoundedCornerShape(percent = 50))
                    .background(Brush.linearGradient(colors))
                    .clickable(onClick = onClick)
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = name,
            color = if (selected) palette.onGlass else palette.onGlassMuted,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
fun Segmented(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    val palette = LocalPalette.current
    val index = options.indexOfFirst { it.first == selected }.coerceAtLeast(0)

    Box(
        Modifier
            .fillMaxWidth()
            .height(46.dp)
            .glass(ZenithShapes.pill, strength = 0.8f)
            .padding(4.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val itemWidth = maxWidth / options.size
            // The pill slides to the option you picked and settles with a small bounce.
            val slide by animateDpAsState(itemWidth * index, Motion.snappy(), label = "segment")

            Box(
                Modifier
                    .offset(x = slide)
                    .width(itemWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(percent = 50))
                    .background(palette.accent.copy(alpha = 0.9f))
            )

            Row(Modifier.fillMaxSize()) {
                options.forEachIndexed { position, (id, label) ->
                    val active = position == index
                    val textColour by animateColorAsState(
                        targetValue = if (active) Color(0xFF0A0B12) else palette.onGlassMuted,
                        animationSpec = tween(260),
                        label = "segmentText",
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .pressBounce(0.94f)
                            .clip(RoundedCornerShape(percent = 50))
                            .clickable { onSelect(id) },
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

/** A miniature of the home screen so the palette choice is not abstract. */
@Composable
private fun HomePreview(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Box(
        modifier
            .aspectRatio(0.52f)
            .clip(RoundedCornerShape(30.dp))
            .background(Brush.verticalGradient(palette.backdrop))
            .border(1.dp, palette.onGlass.copy(alpha = 0.16f), RoundedCornerShape(30.dp))
            .padding(12.dp),
    ) {
        Column(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .glass(RoundedCornerShape(18.dp), strength = 1f)
                    .padding(10.dp),
            ) {
                Column {
                    Text("9:41", color = palette.onGlass, fontSize = 22.sp, fontWeight = FontWeight.Light)
                    Text("Tuesday 12", color = palette.onGlassMuted, fontSize = 9.sp)
                }
            }
            Spacer(Modifier.height(12.dp))
            repeat(3) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    repeat(4) {
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(ZenithShapes.icon)
                                .background(palette.onGlass.copy(alpha = 0.16f))
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.weight(1f))
            Row(
                Modifier
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(20.dp), strength = 1.1f)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(4) {
                    Box(
                        Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(ZenithShapes.icon)
                            .background(
                                Brush.linearGradient(
                                    listOf(palette.accent.copy(alpha = 0.75f), palette.accentAlt.copy(alpha = 0.5f))
                                )
                            )
                    )
                }
            }
        }
    }
}
