package com.gabriel.zenithui.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.data.GlassLevels

/**
 * Draws, once and out of sight, what the home screen draws the first time a pane of glass, a sheet
 * or the drawer comes up: in Liquid Glass the lens over the wallpaper's softened copies ([levels]),
 * in another theme its own panel, and the blur behind a sheet at the theme's strength. A phone
 * works out how to draw each of these the first time it is asked to, which on the home screen is a
 * frame dropped as the first sheet rises; drawn here, during setup, they are ready for it.
 *
 * [onWarm] is called once, two frames after they were first drawn, by when they have been through
 * the GPU and not only written down for it.
 */
@Composable
fun GraphicsWarmUp(levels: GlassLevels?, onWarm: () -> Unit, modifier: Modifier = Modifier) {
    val theme = LocalTheme.current
    val warmed by rememberUpdatedState(onWarm)
    // Set while drawing, read by the frames that follow: plain memory, so nothing redraws on it.
    val drawn = remember { BooleanArray(1) }
    val lens = rememberLens(if (theme.isGlass) levels else null)
    LaunchedEffect(Unit) {
        while (!drawn[0]) withFrameNanos { }
        repeat(2) { withFrameNanos { } }
        warmed()
    }
    CompositionLocalProvider(
        LocalGlassLevels provides levels,
        LocalBackdrop provides levels?.frost,
        LocalLens provides lens,
        // As the home screen's panes stand: on the wallpaper.
        LocalOnWallpaper provides true,
    ) {
        Box(
            modifier
                .size(WARM_SIZE)
                // All but invisible: drawn, so the phone has drawn it, but not to be seen.
                .graphicsLayer { alpha = WARM_ALPHA }
                .drawWithContent {
                    drawContent()
                    drawn[0] = true
                },
        ) {
            Box(Modifier.size(WARM_SIZE).glass(RoundedCornerShape(12.dp)))
            // Android 12 and up blur behind a sheet and the drawer; before that nothing blurs.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && theme.blurBehind > 0f) {
                Box(
                    Modifier
                        .size(WARM_SIZE)
                        .blur(theme.blurBehind.dp)
                        .background(Brush.linearGradient(listOf(Color.White, Color.Black))),
                )
            }
        }
    }
}

private val WARM_SIZE = 24.dp
private const val WARM_ALPHA = 0.01f
