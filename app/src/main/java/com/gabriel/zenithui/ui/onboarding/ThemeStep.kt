package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.Theme
import com.gabriel.zenithui.data.ThemeTone
import com.gabriel.zenithui.data.Themes
import com.gabriel.zenithui.ui.common.ThemeMiniature
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithFonts
import com.gabriel.zenithui.ui.theme.themed

/**
 * Setup's look: the four themes as small cards, each drawn in itself, and Light or Dark under them.
 * A tap on a card turns the whole of setup into that theme there and then (ZenithTheme cross-fades
 * it), the phone above it included, so what you pick is what you see before you have built anything;
 * the card tells the backdrop where to start its ring of light. Aurora is always dark, and says so,
 * while Light and Dark are kept for the others.
 */
@Composable
internal fun ThemeControls(
    themeId: String,
    appearance: String,
    font: String,
    appearanceDark: Boolean,
    reduced: Boolean,
    /** The first time the step is seen the cards come in one after another; after that they are there. */
    play: Boolean,
    onTheme: (Theme, Offset) -> Unit,
    onAppearance: (String, Offset) -> Unit,
) {
    val tick = rememberTick()
    val segment = remember { Spot() }
    val picked = Themes.byId(themeId)
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Themes.all.forEachIndexed { index, theme ->
                ThemeSwatch(
                    theme = theme,
                    dark = Themes.isDark(theme, appearanceDark),
                    font = ZenithFonts.forTheme(font, theme.font).family,
                    picked = theme.id == picked.id,
                    onPick = { origin ->
                        if (theme.id != picked.id) {
                            tick()
                            onTheme(theme, origin)
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .rise(index, play && !reduced),
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(
            Modifier
                .rise(Themes.all.size, play && !reduced)
                .onGloballyPositioned { segment.centre = it.boundsInRoot().center },
        ) {
            Segmented(
                options = listOf("auto" to "Auto", "light" to "Light", "dark" to "Dark"),
                selected = appearance,
                onSelect = { onAppearance(it, segment.centre) },
            )
        }
        AnimatedVisibility(visible = picked.tone == ThemeTone.DARK, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
            Text(
                text = "${picked.name} is always dark. This is kept for the other themes.",
                color = LocalPalette.current.onGlassMuted,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

/**
 * One theme as a card a quarter of the row wide: the theme drawn small ([ThemeMiniature]) with a
 * ring of the accent round the one picked, and its name under it.
 */
@Composable
private fun ThemeSwatch(
    theme: Theme,
    dark: Boolean,
    font: androidx.compose.ui.text.font.FontFamily,
    picked: Boolean,
    onPick: (Offset) -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = LocalPalette.current
    val spot = remember { Spot() }
    val ring by animateColorAsState(if (picked) page.accent else Color.Transparent, tween(220), label = "swatchRing")
    val shape = themed(SWATCH)
    Column(
        modifier
            .pressBounce(0.94f)
            .clip(shape)
            .selectable(
                selected = picked,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.RadioButton,
                onClick = { onPick(spot.centre) },
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(SWATCH_HEIGHT)
                .onGloballyPositioned { spot.centre = it.boundsInRoot().center }
                .border(2.dp, ring, shape)
                .padding(3.dp),
        ) {
            ThemeMiniature(theme, dark, font, Modifier.fillMaxSize(), compact = true)
        }
        Spacer(Modifier.height(5.dp))
        // Small enough that "Liquid Glass" fits a quarter of a narrow phone on one line.
        Text(
            text = theme.name,
            color = if (picked) page.onGlass else page.onGlassMuted,
            fontSize = 11.sp,
            letterSpacing = (-0.1).sp,
            fontWeight = if (picked) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private val SWATCH = RoundedCornerShape(16.dp)
private val SWATCH_HEIGHT = 70.dp
