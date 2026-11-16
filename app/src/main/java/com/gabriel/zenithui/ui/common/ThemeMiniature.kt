package com.gabriel.zenithui.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.Theme
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalTheme
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.palette
import com.gabriel.zenithui.ui.theme.shape

/**
 * A theme drawn small, in itself whatever the page around it is drawn in: its page from top to
 * bottom, a panel of its own material with a line of words in [font], and its accent as a button,
 * all in its own corners. Settings' theme cards and setup's are both this, so a theme looks the same
 * wherever it is offered. [compact] is setup's, four to a row; the caller gives it its size.
 */
@Composable
fun ThemeMiniature(
    theme: Theme,
    dark: Boolean,
    font: FontFamily,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val its = remember(theme, dark) { theme.palette(dark) }
    CompositionLocalProvider(LocalTheme provides theme, LocalPalette provides its) {
        Box(
            modifier
                .clip(theme.shape(if (compact) COMPACT_PAGE else PAGE))
                .background(Brush.verticalGradient(its.backdrop.let { if (it.size == 1) it + it else it }))
                .padding(if (compact) 7.dp else 9.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .glass(RoundedCornerShape(if (compact) 9.dp else 12.dp), strength = 0.9f, solid = 0.5f)
                    .padding(horizontal = if (compact) 7.dp else 10.dp, vertical = if (compact) 5.dp else 8.dp),
            ) {
                Text(
                    "Aa",
                    color = its.ink,
                    fontFamily = font,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = if (compact) 14.sp else 17.sp,
                    lineHeight = if (compact) 17.sp else 22.sp,
                )
                Spacer(Modifier.height(if (compact) 4.dp else 5.dp))
                Box(
                    Modifier
                        .fillMaxWidth(0.7f)
                        .height(if (compact) 4.dp else 5.dp)
                        .clip(theme.shape(RoundedCornerShape(3.dp)))
                        .background(its.ink.copy(alpha = 0.28f)),
                )
            }
            Box(
                Modifier
                    .align(Alignment.BottomEnd)
                    .size(width = if (compact) 28.dp else 46.dp, height = if (compact) 12.dp else 20.dp)
                    .glass(ZenithShapes.pill, tint = its.accent, tinted = true, strength = 1f),
            )
        }
    }
}

private val PAGE = RoundedCornerShape(17.dp)
private val COMPACT_PAGE = RoundedCornerShape(13.dp)
