package com.gabriel.zenithui.ui.home.widgets

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.data.NotificationDots
import com.gabriel.zenithui.data.NowPlaying
import com.gabriel.zenithui.data.Playing
import com.gabriel.zenithui.data.TrackTime
import com.gabriel.zenithui.data.WidgetForm
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.home.LocalBare
import com.gabriel.zenithui.ui.home.LocalWidgetFill
import com.gabriel.zenithui.ui.home.LocalWidgetWorld
import com.gabriel.zenithui.ui.theme.LocalOnWallpaper
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.glassInk
import com.gabriel.zenithui.ui.theme.glassOnGlass
import com.gabriel.zenithui.ui.theme.inkOnTint
import kotlinx.coroutines.delay

/**
 * Music. The album's picture, standing on a soft shadow in its own colour, the song and who it is
 * by, how far in it is, and the controls: back and next as symbols, play and pause as a round button
 * of glass on glass, of the card's ink on a card. While it plays three little bars dance beside the
 * song, and stand still when it stops.
 *
 * - Mini: the picture, with play and pause on it.
 * - Strip: the picture, the song, and play and next.
 * - Square: the picture and play at the top, the song and how far in at the foot.
 * - Wide: the picture on the left; the song, how far in and the controls on the right.
 * - Tall: the picture across the top; the song, how far in and the controls under it.
 * - Big: the picture and the song side by side; how far in and the controls large under them.
 */
@Composable
internal fun MusicWidget(form: WidgetForm, width: Dp, height: Dp) {
    val context = LocalContext.current
    val world = LocalWidgetWorld.current
    LaunchedEffect(Unit) { NowPlaying.watch(context) }
    val playing by NowPlaying.now.collectAsState()
    val now = playing
    if (now == null) {
        val allowed = remember { NotificationDots.allowed(context) }
        Fitted(form, width, height) { _, _ ->
            Invite(
                form,
                if (allowed) "Nothing playing" else "See what's playing",
                if (allowed) "Play something and it shows here" else "Tap to let Zenith see your music",
                if (allowed) null else world?.let { { it.setUp("media") } },
            ) { size -> Glyph(Icons.Rounded.MusicNote, size, tint = hue(Hue.Red)) }
        }
        return
    }
    val glow = rememberArtColour(now.art)
    val app = rememberAppName(now.packageName)
    val spoken = "${now.title}${if (now.artist.isNotBlank()) " by ${now.artist}" else ""}, ${if (now.playing) "playing" else "paused"}"

    Fitted(form, width, height) { w, h ->
        val root = Modifier.fillMaxSize()
        when (form) {
            WidgetForm.Mini -> Box(root.padding(6.dp), contentAlignment = Alignment.Center) {
                Art(now, glow, Modifier.fillMaxSize(), corner = 14.dp, shadow = false)
                PlayButton(now, 26.dp, onArt = true)
            }

            WidgetForm.Strip -> Row(root.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Art(now, glow, Modifier.fillMaxHeight().aspectRatio(1f), corner = 8.dp, shadow = false)
                Spacer(Modifier.width(10.dp))
                Song(now, Modifier.weight(1f).says(spoken), title = Type.body, artist = Type.meta)
                Spacer(Modifier.width(6.dp))
                PlayButton(now, 34.dp)
                if (w >= 250.dp) Skip(Icons.Rounded.SkipNext, "Next", now.canSkip, 34.dp) { NowPlaying.next() }
            }

            WidgetForm.Square -> Column(root.padding(INSET)) {
                Row(verticalAlignment = Alignment.Top) {
                    Art(now, glow, Modifier.size(58.dp), corner = 12.dp)
                    Spacer(Modifier.weight(1f))
                    PlayButton(now, 40.dp)
                }
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Song(now, Modifier.weight(1f).says(spoken))
                    Spacer(Modifier.width(6.dp))
                    Bars(now.playing, glow, Modifier.size(14.dp))
                }
                Spacer(Modifier.height(9.dp))
                Progress(now, times = false)
            }

            WidgetForm.Wide -> Row(root.padding(INSET)) {
                Art(now, glow, Modifier.fillMaxHeight().aspectRatio(1f), corner = 14.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f).fillMaxHeight()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Label(app ?: "Now playing", modifier = Modifier.weight(1f))
                        Bars(now.playing, glow, Modifier.size(13.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Song(now, Modifier.says(spoken))
                    Spacer(Modifier.weight(1f))
                    Progress(now, times = true)
                    Spacer(Modifier.height(6.dp))
                    Controls(now, play = 38.dp, skip = 34.dp)
                }
            }

            WidgetForm.Tall -> Column(root.padding(INSET)) {
                Art(now, glow, Modifier.fillMaxWidth().aspectRatio(1f), corner = 14.dp)
                Spacer(Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Song(now, Modifier.weight(1f).says(spoken))
                    Spacer(Modifier.width(6.dp))
                    Bars(now.playing, glow, Modifier.size(14.dp))
                }
                Spacer(Modifier.weight(1f))
                Progress(now, times = true)
                Spacer(Modifier.height(10.dp))
                Controls(now, play = 44.dp, skip = 36.dp)
            }

            WidgetForm.Big -> Column(root.padding(INSET)) {
                Row {
                    Art(now, glow, Modifier.fillMaxWidth(0.58f).aspectRatio(1f), corner = 16.dp)
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Label(app ?: "Now playing", modifier = Modifier.weight(1f))
                            Bars(now.playing, glow, Modifier.size(14.dp))
                        }
                        Spacer(Modifier.height(6.dp))
                        Words(now.title, Type.headline, weight = FontWeight.SemiBold, maxLines = 3, modifier = Modifier.says(spoken))
                        if (now.artist.isNotBlank()) Words(now.artist, Type.body, color = soft(), maxLines = 2)
                    }
                }
                Spacer(Modifier.weight(1f))
                Progress(now, times = true)
                Spacer(Modifier.height(10.dp))
                Controls(now, play = 54.dp, skip = 42.dp)
            }
        }
    }
}

/** The song and who it is by, one over the other, the song changing over with a fade. */
@Composable
private fun Song(now: Playing, modifier: Modifier = Modifier, title: androidx.compose.ui.unit.TextUnit = Type.headline, artist: androidx.compose.ui.unit.TextUnit = Type.body) {
    Column(modifier) {
        AnimatedContent(now.title, transitionSpec = { fadeIn(tween(260, delayMillis = 60)).togetherWith(fadeOut(tween(140))) }, label = "song") { shown ->
            Words(shown, title, weight = FontWeight.SemiBold)
        }
        if (now.artist.isNotBlank()) Words(now.artist, artist, color = soft())
    }
}

/**
 * The album's picture, crossing over to the next song's, in a rounded square that stands on a soft
 * shadow straight down in the picture's own colour. A note on a well when there is none.
 */
@Composable
private fun Art(now: Playing, glow: Color?, modifier: Modifier, corner: Dp, shadow: Boolean = true) {
    val reduced = LocalMotionLevel.current.reduced
    val bare = LocalBare.current
    val lit = if (shadow && glow != null && !bare) {
        Modifier.drawBehind {
            drawIntoCanvas { canvas ->
                val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                    color = android.graphics.Color.argb(4, 0, 0, 0)
                    setShadowLayer(14.dp.toPx(), 0f, 6.dp.toPx(), glow.copy(alpha = 0.55f).toArgb())
                }
                val r = corner.toPx()
                canvas.nativeCanvas.drawRoundRect(0f, 0f, size.width, size.height, r, r, paint)
            }
        }
    } else {
        Modifier
    }
    AnimatedContent(
        targetState = now.art,
        transitionSpec = {
            if (reduced) fadeIn(tween(Motion.REDUCED_MS)).togetherWith(fadeOut(tween(Motion.REDUCED_MS)))
            else (fadeIn(tween(420)) + scaleIn(initialScale = 0.94f)).togetherWith(fadeOut(tween(300)))
        },
        modifier = modifier.then(lit).clip(RoundedCornerShape(corner)),
        label = "albumArt",
    ) { art ->
        if (art != null) {
            Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Box(Modifier.fillMaxSize().well(corner), contentAlignment = Alignment.Center) {
                Glyph(Icons.Rounded.MusicNote, 22.dp, tint = soft())
            }
        }
    }
}

/**
 * Play or pause, the one turning into the other. On the wallpaper's glass it is glass lying in the
 * pane; on a card, a disc of the card's ink; in a "No glass" widget, a ring of glass round the
 * symbol. Over the picture it is a dark disc, which reads on any picture.
 */
@Composable
private fun PlayButton(now: Playing, size: Dp, onArt: Boolean = false) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val bare = LocalBare.current
    val card = LocalWidgetFill.current
    val onGlass = !bare && card == null && LocalOnWallpaper.current && !onArt
    val disc: Modifier
    val glyph: Color
    when {
        onArt -> {
            disc = Modifier.background(Color.Black.copy(alpha = 0.42f), CircleShape)
            glyph = Color.White
        }
        bare -> {
            // Glass ink is one colour, so a filled disc would swallow its symbol: a ring of glass
            // round a glass symbol instead.
            disc = Modifier.glassInk(bevel = size * 0.06f).border(2.dp, Color.White, CircleShape)
            glyph = Color.White
        }
        card != null -> {
            disc = Modifier.background(palette.onGlass, CircleShape)
            glyph = inkOnTint(palette.onGlass)
        }
        onGlass -> {
            // Glass lying in the widget's glass: a pane of its own would show the wallpaper through
            // a hole in it. On the wallpaper the glass is the same in Light and Dark, so it lies in
            // dark glass in both.
            disc = Modifier.glassOnGlass(CircleShape, dark = true, interactive = true)
            glyph = palette.onGlass
        }
        else -> {
            disc = Modifier.background(palette.onGlass.copy(alpha = 0.16f), CircleShape)
            glyph = palette.onGlass
        }
    }
    Box(
        Modifier
            .size(size)
            .pressBounce(0.9f)
            .then(disc)
            .clip(CircleShape)
            .clickable { NowPlaying.playPause() },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = now.playing,
            transitionSpec = {
                if (reduced) fadeIn(tween(Motion.REDUCED_MS)).togetherWith(fadeOut(tween(Motion.REDUCED_MS)))
                else (fadeIn(tween(160)) + scaleIn(initialScale = 0.5f)).togetherWith(fadeOut(tween(100)))
            },
            label = "playPause",
        ) { going ->
            Icon(
                if (going) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (going) "Pause" else "Play",
                tint = glyph,
                modifier = Modifier.size(size * 0.56f),
            )
        }
    }
}

/** Back or next, a symbol on its own with room round it to be pressed. */
@Composable
private fun Skip(icon: ImageVector, says: String, enabled: Boolean, size: Dp, onClick: () -> Unit) {
    Box(
        Modifier
            .size(size)
            .pressBounce(0.86f, enabled = enabled)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Glyph(icon, size * 0.66f, tint = if (enabled) ink() else faint(0.35f), says = says)
    }
}

/** Back, play and next, spread across the room with play in the middle. */
@Composable
private fun Controls(now: Playing, play: Dp, skip: Dp) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        Skip(Icons.Rounded.SkipPrevious, "Back", now.canGoBack, skip) { NowPlaying.previous() }
        PlayButton(now, play)
        Skip(Icons.Rounded.SkipNext, "Next", now.canSkip, skip) { NowPlaying.next() }
    }
}

/**
 * How far into the song it is: a bar that moves on a second at a time while it plays, with, given
 * [times], how far in under its left end and how long is left under its right.
 */
@Composable
private fun Progress(now: Playing, times: Boolean) {
    if (now.duration <= 0) return
    val at by produceState(initialValue = now.positionNow(), now) {
        while (true) {
            value = now.positionNow()
            if (!now.playing) break
            delay(1_000)
        }
    }
    val share = (at.toFloat() / now.duration).coerceIn(0f, 1f)
    val bare = LocalBare.current
    val track = faint(0.2f)
    val lit = if (bare) Color.White else ink()
    Column(Modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(4.dp).then(if (bare) Modifier.glassInk(bevel = 1.5.dp) else Modifier)) {
            val h = size.height
            if (!bare) drawRoundRect(track, cornerRadius = CornerRadius(h / 2f))
            drawRoundRect(lit, size = Size((size.width * share).coerceAtLeast(h), h), cornerRadius = CornerRadius(h / 2f))
        }
        if (times) {
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth()) {
                Words(TrackTime.of(at), Type.caps, weight = FontWeight.SemiBold, color = soft(), tabular = true, modifier = Modifier.weight(1f))
                Words("−" + TrackTime.of(now.duration - at), Type.caps, weight = FontWeight.SemiBold, color = soft(), tabular = true)
            }
        }
    }
}

/**
 * Three little bars that rise and fall while something plays, in the picture's colour made light
 * enough to read; still, and low, when it is paused or movement is reduced.
 */
@Composable
private fun Bars(playing: Boolean, glow: Color?, modifier: Modifier) {
    val reduced = LocalMotionLevel.current.reduced
    val bare = LocalBare.current
    val colour = when {
        bare -> Color.White
        LocalWidgetFill.current != null || glow == null -> ink()
        else -> lerp(glow, Color.White, 0.45f)
    }
    val moving = playing && !reduced
    // One turn every few seconds, each bar going up and down a different whole number of times in it,
    // out of step, so they never all rest at once and the loop has no seam.
    val turn = if (moving) {
        rememberInfiniteTransition(label = "bars").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(3_000, easing = LinearEasing), RepeatMode.Restart),
            label = "turn",
        ).value
    } else {
        null
    }
    val heights = BARS.map { bar ->
        when {
            turn != null -> 0.3f + 0.7f * (0.5f + 0.5f * kotlin.math.sin(2f * Math.PI.toFloat() * (turn * bar.beats + bar.from)))
            playing -> bar.rest
            else -> 0.25f
        }
    }
    Canvas(modifier.then(if (bare) Modifier.glassInk(bevel = 1.dp) else Modifier)) {
        val gap = size.width * 0.18f
        val bar = (size.width - gap * 2f) / 3f
        heights.forEachIndexed { i, share ->
            val h = (size.height * share).coerceAtLeast(bar)
            drawRoundRect(colour, Offset(i * (bar + gap), size.height - h), Size(bar, h), CornerRadius(bar / 2f))
        }
    }
}

/** A bar of the sound: how tall it stands still, how many times it rises in a turn, and where it starts. */
private class Bar(val rest: Float, val beats: Int, val from: Float)

private val BARS = listOf(Bar(0.55f, 4, 0f), Bar(1f, 3, 0.35f), Bar(0.7f, 5, 0.7f))

/** The picture's colour on average, for the soft light under it; null with no picture. */
@Composable
private fun rememberArtColour(art: ImageBitmap?): Color? = remember(art) {
    art ?: return@remember null
    runCatching {
        val one = android.graphics.Bitmap.createScaledBitmap(art.asAndroidBitmap().copy(android.graphics.Bitmap.Config.ARGB_8888, false), 1, 1, true)
        Color(one.getPixel(0, 0)).copy(alpha = 1f)
    }.getOrNull()
}

/** The name of the app that is playing, as the phone shows it; null if it cannot be found. */
@Composable
private fun rememberAppName(packageName: String): String? {
    val context = LocalContext.current
    return remember(packageName) {
        runCatching {
            val pm = context.packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
        }.getOrNull()
    }
}
