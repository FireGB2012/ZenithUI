package com.gabriel.zenithui.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.Theme
import com.gabriel.zenithui.data.Themes
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.ThemeMiniature
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalTheme
import com.gabriel.zenithui.ui.theme.ZenithFonts
import com.gabriel.zenithui.ui.theme.shape
import androidx.compose.material.icons.rounded.Favorite

/**
 * Settings → Wallpaper & appearance → Theme: the four themes, each a card drawn in itself, its page,
 * a panel of its material and its accent, in its own font. A tap tries one at once, everything
 * cross-fading into it; the star makes it yours, the theme Zenith starts in. One you are only
 * trying lasts until Zenith next starts, so there is always a way back.
 */
@Composable
internal fun ThemeChoice(settings: ZenithSettings, onFavourite: (String) -> Unit) {
    val trying by Themes.trying.collectAsState()
    val active = Themes.active(settings.theme, trying)
    val favourite = Themes.byId(settings.theme)
    val appearanceDark = settings.isDark(isSystemInDarkTheme())
    val reduced = LocalMotionLevel.current.reduced
    val tick = rememberTick()

    fun tryOn(theme: Theme) {
        tick()
        // Your own theme is not something to try: it is what you are back in.
        Themes.trying.value = if (theme.id == settings.theme) null else theme.id
    }

    fun keep(theme: Theme) {
        tick()
        onFavourite(theme.id)
        Themes.trying.value = null
    }

    Column(Modifier.padding(14.dp)) {
        Caption("Theme")
        Spacer(Modifier.height(10.dp))
        Themes.all.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { theme ->
                    ThemeCard(
                        theme = theme,
                        dark = Themes.isDark(theme, appearanceDark),
                        font = ZenithFonts.forTheme(settings.font, theme.font).family,
                        shown = theme.id == active.id,
                        favourite = theme.id == favourite.id,
                        onTry = { tryOn(theme) },
                        onKeep = { keep(theme) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
        Note(
            if (active.id == favourite.id) {
                "${active.tagline}. The star is your theme: what Zenith starts in."
            } else {
                "Trying ${active.name}. Zenith starts in ${favourite.name}, your theme, until you star another."
            },
        )
    }
    AnimatedVisibility(visible = active.id != favourite.id, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
        Column {
            GroupDivider()
            ActionRow(
                icon = Icons.Rounded.Favorite,
                title = "Make ${active.name} my theme",
                subtitle = "Zenith starts in it from now on",
                onClick = { keep(active) },
            )
        }
    }
}

/**
 * One theme as a card, drawn in that theme whatever the page is in: its page from top to bottom, a
 * panel of its own material with a line of words in its font, and its accent as a button. The one
 * on screen has a ring in it; your own has a star.
 */
@Composable
private fun ThemeCard(
    theme: Theme,
    dark: Boolean,
    font: FontFamily,
    shown: Boolean,
    favourite: Boolean,
    onTry: () -> Unit,
    onKeep: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val page = LocalPalette.current
    val ring by animateColorAsState(if (shown) page.accent else Color.Transparent, tween(220), label = "themeRing")
    val cardShape = LocalTheme.current.shape(CARD)
    Column(
        modifier
            .pressBounce(0.96f)
            .clip(cardShape)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = "Try ${theme.name}",
                onClick = onTry,
            ),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(104.dp)
                .border(2.dp, ring, cardShape)
                .padding(3.dp),
        ) {
            // Drawn in its own theme: its material, corners, colours and font.
            ThemeMiniature(theme, dark, font, Modifier.fillMaxWidth().height(98.dp))
            // Yours: a star you can tap, on every card, filled on the one that is yours.
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.32f))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = if (favourite) "${theme.name} is your theme" else "Make ${theme.name} your theme",
                        onClick = onKeep,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (favourite) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    contentDescription = if (favourite) "Your theme" else "Make it your theme",
                    tint = if (favourite) Color(0xFFFFD60A) else Color.White,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                theme.name,
                color = page.onGlass,
                fontSize = 14.sp,
                fontWeight = if (shown) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (favourite) {
                Spacer(Modifier.width(5.dp))
                Text("Yours", color = page.onGlassMuted, fontSize = 12.sp)
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

private val CARD = RoundedCornerShape(20.dp)
