package com.gabriel.zenithui.ui.home

import android.os.SystemClock
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.gabriel.zenithui.Incoming
import com.gabriel.zenithui.WallpaperDirector
import com.gabriel.zenithui.ui.theme.SYSTEM_WALLPAPER_ZOOM
import com.gabriel.zenithui.ui.theme.systemWallpaperOffset
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.roundToInt

/** One picture on its way in, and how far in it has got. */
private class FadingPicture(val image: ImageBitmap, start: Float) {
    val alpha = Animatable(start)
}

/**
 * The wallpaper changing: the new picture drawn over the old wallpaper, exactly where Android will
 * show it, and faded in. Android cannot fade from one wallpaper to another, so Zenith does it here
 * and hands the picture over once it is fully in; then this layer fades away from in front of the
 * very same picture, and nothing is seen to happen.
 *
 * It draws only while a wallpaper is changing. The rest of the time it is not there at all.
 */
@Composable
internal fun WallpaperFade(incoming: Incoming?, wallpaperOffset: State<Offset>) {
    // More than one only while themes are being tapped through: each new picture comes in over
    // the one before, which goes once it can no longer be seen.
    val pictures = remember { mutableStateListOf<FadingPicture>() }
    LaunchedEffect(incoming?.image, incoming?.leaving) {
        when {
            incoming == null -> pictures.clear()
            incoming.leaving -> coroutineScope {
                pictures.toList().forEach { launch { it.alpha.animateTo(0f, tween(WallpaperDirector.FADE_OUT_MS)) } }
            }
            else -> {
                val top = pictures.firstOrNull { it.image === incoming.image } ?: run {
                    // A screen rebuilt half way through carries on from where the fade had got to.
                    val elapsed = (SystemClock.uptimeMillis() - incoming.since).coerceAtLeast(0L)
                    FadingPicture(incoming.image, (elapsed / WallpaperDirector.FADE_IN_MS.toFloat()).coerceIn(0f, 1f))
                        .also { pictures.add(it) }
                }
                val left = ((1f - top.alpha.value) * WallpaperDirector.FADE_IN_MS).roundToInt()
                if (left > 0) top.alpha.animateTo(1f, tween(left, easing = LinearEasing))
                pictures.removeAll { it !== top }
            }
        }
    }
    if (pictures.isEmpty()) return
    Canvas(Modifier.fillMaxSize()) {
        val shown = systemWallpaperOffset(wallpaperOffset.value)
        pictures.forEach { picture ->
            // The same sums the glass does to find the wallpaper behind it.
            val scale = max(size.width / picture.image.width, size.height / picture.image.height) * SYSTEM_WALLPAPER_ZOOM
            val width = (picture.image.width * scale).roundToInt()
            val height = (picture.image.height * scale).roundToInt()
            drawImage(
                image = picture.image,
                dstOffset = IntOffset(
                    (-(width - size.width) * shown.x.coerceIn(0f, 1f)).roundToInt(),
                    (-(height - size.height) * shown.y.coerceIn(0f, 1f)).roundToInt(),
                ),
                dstSize = IntSize(width, height),
                alpha = picture.alpha.value,
                filterQuality = FilterQuality.Medium,
            )
        }
    }
}
