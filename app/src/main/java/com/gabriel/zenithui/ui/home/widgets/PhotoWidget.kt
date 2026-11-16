package com.gabriel.zenithui.ui.home.widgets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.data.HomeGeometry
import com.gabriel.zenithui.data.WidgetForm
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.home.LocalBare
import com.gabriel.zenithui.ui.home.LocalWidgetWorld
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The Photo frame: one of your photos, edge to edge, to the widget's own corners, as a photo widget
 * is. A different one each time you come back to the home screen, and the next at a tap, crossing
 * over slowly. It is the same at every size: the photo cut to the widget's shape.
 */
@Composable
internal fun PhotoWidget(form: WidgetForm, width: Dp, height: Dp) {
    val world = LocalWidgetWorld.current
    val photos = world?.settings?.framePhotos.orEmpty()
    val dir = world?.frameDir

    @Composable
    fun Ask() {
        Fitted(form, width, height) { _, _ ->
            Invite(form, "Choose your photos", "Tap to pick a few", world?.let { { it.setUp("photo") } }) { size ->
                Glyph(Icons.Rounded.PhotoLibrary, size, tint = hue(Hue.Yellow))
            }
        }
    }

    if (photos.isEmpty() || dir == null) {
        Ask()
        return
    }
    val reduced = LocalMotionLevel.current.reduced
    val bare = LocalBare.current
    var tapped by remember { mutableIntStateOf(0) }
    val name = photos[((world.visits + tapped) % photos.size + photos.size) % photos.size]
    val longest = with(LocalDensity.current) { maxOf(width, height).roundToPx() }
    val picture by produceState<Framed>(initialValue = Framed.Loading, name, longest) {
        value = withContext(Dispatchers.IO) { framePicture(java.io.File(dir, name), longest) }?.let { Framed.Ready(it) } ?: Framed.Gone
    }
    // A photo that cannot be read any more (its file gone, or spoilt) asks for photos again, rather
    // than leave the pane empty.
    if (picture == Framed.Gone) {
        Ask()
        return
    }
    // Edge to edge on a pane or a card, whose corners it takes; a little in from them with no pane,
    // so it does not sit on the wallpaper as a hard-cornered cut-out.
    val corner = HomeGeometry.WIDGET_CORNER_DP.dp
    AnimatedContent(
        targetState = (picture as? Framed.Ready)?.image,
        transitionSpec = {
            if (reduced) fadeIn(tween(Motion.REDUCED_MS)).togetherWith(fadeOut(tween(Motion.REDUCED_MS)))
            else fadeIn(tween(700)).togetherWith(fadeOut(tween(700)))
        },
        modifier = Modifier
            .fillMaxSize()
            .then(if (bare) Modifier.padding(2.dp) else Modifier)
            .clip(RoundedCornerShape(corner))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { tapped++ }
            .says("Photo"),
        label = "framePhoto",
    ) { image ->
        if (image != null) Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Box(Modifier.fillMaxSize())
    }
}

/** Where the photo is: still being read, read, or not to be read. */
private sealed interface Framed {
    data object Loading : Framed
    data object Gone : Framed
    class Ready(val image: ImageBitmap) : Framed
}

/** A photo from the frame's folder, decoded no bigger than [longest] pixels across. */
private fun framePicture(file: java.io.File, longest: Int): ImageBitmap? = runCatching {
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= longest.coerceAtLeast(64)) sample *= 2
    android.graphics.BitmapFactory.decodeFile(file.path, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
}.getOrNull()
