package com.gabriel.zenithui.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Celebration
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Dehaze
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WbCloudy
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AuraThemes
import com.gabriel.zenithui.data.Countdown
import com.gabriel.zenithui.data.Forecast
import com.gabriel.zenithui.data.NotificationDots
import com.gabriel.zenithui.data.NowPlaying
import com.gabriel.zenithui.data.Playing
import com.gabriel.zenithui.data.Todos
import com.gabriel.zenithui.data.WeatherCodes
import com.gabriel.zenithui.data.WeatherParse
import com.gabriel.zenithui.data.WidgetForm
import com.gabriel.zenithui.data.WorldClocks
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.glassInk
import com.gabriel.zenithui.ui.theme.inkOnTint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * What Zenith's widgets read beyond the room they have: your settings, a way to change them, the
 * forecast, how many times the home screen has been come back to (for the photo frame), and a way
 * to ask the home screen for what only it can show — a picker, a sheet — by the widget's kind.
 */
@Stable
class WidgetWorld(
    val settings: ZenithSettings,
    val change: ((ZenithSettings) -> ZenithSettings) -> Unit,
    val forecast: Forecast?,
    val visits: Int,
    val setUp: (String) -> Unit,
    /** Where the Photo frame's pictures are kept. */
    val frameDir: java.io.File?,
)

val LocalWidgetWorld = compositionLocalOf<WidgetWorld?> { null }

/** The settings a widget reads: the real ones on the home screen, Zenith's own anywhere else. */
@Composable
private fun widgetSettings(): ZenithSettings = LocalWidgetWorld.current?.settings ?: ZenithSettings()

/** A widget's own little message when it has nothing to show yet, and what a tap on it does. */
@Composable
private fun SetUpPrompt(icon: ImageVector, words: String, form: WidgetForm, onClick: (() -> Unit)?) {
    Column(
        Modifier
            .fillMaxSize()
            .then(if (onClick != null) Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick) else Modifier)
            .padding(10.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Symbol(icon, if (form == WidgetForm.Mini || form == WidgetForm.Strip) 22.dp else 30.dp)
        if (form != WidgetForm.Mini) {
            Spacer(Modifier.height(6.dp))
            Text(words, color = quiet(), fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

// ---------------------------------------------------------------------- now playing

@Composable
internal fun MediaWidget(form: WidgetForm, width: Dp, height: Dp) {
    val context = LocalContext.current
    val world = LocalWidgetWorld.current
    LaunchedEffect(Unit) { NowPlaying.watch(context) }
    val playing by NowPlaying.now.collectAsState()
    val now = playing
    if (now == null) {
        val allowed = remember { NotificationDots.allowed(context) }
        SetUpPrompt(
            Icons.Rounded.MusicNote,
            if (allowed) "Nothing playing" else "Tap to let Zenith see what is playing",
            form,
            if (allowed) null else ({ world?.setUp("media") }),
        )
        return
    }
    val pad = if (form == WidgetForm.Mini) 8.dp else 14.dp
    when (form) {
        WidgetForm.Mini -> Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
            Art(now, Modifier.fillMaxSize())
            PlayButton(now, size = minOf(width, height) * 0.36f, solid = true)
        }
        WidgetForm.Strip -> Row(Modifier.fillMaxSize().padding(horizontal = pad, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Art(now, Modifier.fillMaxHeight().aspectRatio(1f))
            Spacer(Modifier.width(10.dp))
            SongLines(now, Modifier.weight(1f))
            PlayButton(now, size = 34.dp)
        }
        WidgetForm.Square -> Column(Modifier.fillMaxSize().padding(pad)) {
            Art(now, Modifier.size(minOf(height * 0.42f, 64.dp)))
            Spacer(Modifier.weight(1f))
            SongLines(now)
            Spacer(Modifier.height(8.dp))
            Controls(now, Modifier.fillMaxWidth())
        }
        WidgetForm.Wide -> Row(Modifier.fillMaxSize().padding(pad), verticalAlignment = Alignment.CenterVertically) {
            Art(now, Modifier.fillMaxHeight().aspectRatio(1f))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.SpaceBetween) {
                SongLines(now)
                Progress(now)
                Controls(now, Modifier.fillMaxWidth())
            }
        }
        else -> Column(Modifier.fillMaxSize().padding(pad), horizontalAlignment = Alignment.CenterHorizontally) {
            Art(now, Modifier.weight(1f).aspectRatio(1f, matchHeightConstraintsFirst = true))
            Spacer(Modifier.height(10.dp))
            SongLines(now, Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            Progress(now)
            Spacer(Modifier.height(6.dp))
            Controls(now, Modifier.fillMaxWidth())
        }
    }
}

/** The album's picture, crossing over to the next song's as it starts; a note on a tile when there is none. */
@Composable
private fun Art(now: Playing, modifier: Modifier) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    AnimatedContent(
        targetState = now.title to now.art,
        transitionSpec = {
            if (reduced) fadeIn(tween(Motion.REDUCED_MS)).togetherWith(fadeOut(tween(Motion.REDUCED_MS)))
            else (fadeIn(tween(420)) + scaleIn(initialScale = 0.9f)).togetherWith(fadeOut(tween(300)))
        },
        modifier = modifier.clip(RoundedCornerShape(14.dp)),
        label = "albumArt",
    ) { (_, art) ->
        if (art != null) {
            Image(art, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.linearGradient(listOf(palette.accent.copy(alpha = 0.55f), palette.accentAlt.copy(alpha = 0.4f)))),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = Color.White.copy(alpha = 0.9f), modifier = Modifier.fillMaxSize(0.4f))
            }
        }
    }
}

@Composable
private fun SongLines(now: Playing, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Column(modifier) {
        AnimatedContent(targetState = now.title, transitionSpec = { fadeIn(tween(260, delayMillis = 60)).togetherWith(fadeOut(tween(140))) }, label = "song") { title ->
            Text(title, color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (now.artist.isNotBlank()) {
            Text(now.artist, color = quiet(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

/** How far into the song it is: a bar that moves on a second at a time while it plays. */
@Composable
private fun Progress(now: Playing) {
    val palette = LocalPalette.current
    if (now.duration <= 0) return
    val at by produceState(initialValue = now.positionNow(), now) {
        while (true) {
            value = now.positionNow()
            if (!now.playing) break
            delay(1_000)
        }
    }
    val share = (at.toFloat() / now.duration).coerceIn(0f, 1f)
    val shown by animateFloatAsState(share, tween(900), label = "songProgress")
    Box(
        Modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(palette.onGlass.copy(alpha = 0.14f)),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(shown)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(palette.accent, palette.accentAlt))),
        )
    }
}

@Composable
private fun Controls(now: Playing, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        ControlIcon(Icons.Rounded.SkipPrevious, "Back", enabled = now.canGoBack) { NowPlaying.previous() }
        PlayButton(now, size = 40.dp, solid = true)
        ControlIcon(Icons.Rounded.SkipNext, "Next", enabled = now.canSkip) { NowPlaying.next() }
    }
}

@Composable
private fun ControlIcon(icon: ImageVector, says: String, enabled: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val bare = LocalBare.current
    Box(
        Modifier
            .size(36.dp)
            .pressBounce(0.86f)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = says,
            tint = if (bare) Color.White else palette.onGlass.copy(alpha = if (enabled) 0.9f else 0.3f),
            modifier = Modifier.size(24.dp).then(if (bare) Modifier.glassInk(bevel = 2.dp) else Modifier),
        )
    }
}

/** Play or pause; the one becomes the other with a little turn, as it is pressed. */
@Composable
private fun PlayButton(now: Playing, size: Dp, solid: Boolean = false) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    Box(
        Modifier
            .size(size)
            .pressBounce(0.86f)
            .clip(CircleShape)
            .then(if (solid) Modifier.background(Brush.linearGradient(listOf(palette.accent, palette.accentAlt))) else Modifier.background(palette.onGlass.copy(alpha = 0.12f)))
            .clickable { NowPlaying.playPause() },
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = now.playing,
            transitionSpec = {
                if (reduced) fadeIn(tween(Motion.REDUCED_MS)).togetherWith(fadeOut(tween(Motion.REDUCED_MS)))
                else (fadeIn(tween(160)) + scaleIn(Motion.snappy(), initialScale = 0.4f)).togetherWith(fadeOut(tween(100)))
            },
            label = "playPause",
        ) { going ->
            Icon(
                if (going) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (going) "Pause" else "Play",
                tint = if (solid) inkOnTint(palette.accent) else palette.onGlass,
                modifier = Modifier.size(size * 0.55f),
            )
        }
    }
}

// ---------------------------------------------------------------------- world clock

@Composable
internal fun WorldClockWidget(form: WidgetForm, width: Dp, height: Dp) {
    val palette = LocalPalette.current
    val zones = widgetSettings().worldClocks.take(WorldClocks.MOST).ifEmpty { listOf(ZoneId.systemDefault().id) }
    val tick = rememberNow()
    val instant = remember(tick) { Instant.now() }
    val here = remember { ZoneId.systemDefault() }
    val clock = remember { DateTimeFormatter.ofPattern("HH:mm") }
    fun timeIn(zone: String): String = runCatching { instant.atZone(ZoneId.of(zone)).format(clock) }.getOrDefault("--:--")
    fun faceIn(zone: String): LocalDateTime = runCatching { LocalDateTime.ofInstant(instant, ZoneId.of(zone)) }.getOrDefault(tick)
    val pad = if (form == WidgetForm.Mini) 10.dp else 14.dp
    when (form) {
        WidgetForm.Mini -> Column(Modifier.fillMaxSize().padding(pad), verticalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkySymbol(WorldClocks.daytime(zones.first(), instant), 14.dp)
                Spacer(Modifier.width(4.dp))
                Caps(WorldClocks.city(zones.first()), size = 10.sp)
            }
            Figure(timeIn(zones.first()), fit(height * 0.34f, width * 0.24f), animated = true)
        }
        WidgetForm.Strip -> Row(Modifier.fillMaxSize().padding(horizontal = pad), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            zones.take(3).forEach { zone ->
                Column {
                    Caps(WorldClocks.city(zone), size = 10.sp)
                    Text(timeIn(zone), style = tight(fit(height * 0.34f, 30.dp)), color = palette.onGlass)
                }
            }
        }
        WidgetForm.Square, WidgetForm.Tall -> Column(Modifier.fillMaxSize().padding(pad), verticalArrangement = Arrangement.SpaceEvenly) {
            val shown = zones.take(if (form == WidgetForm.Tall) 4 else 3)
            shown.forEach { zone ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (form == WidgetForm.Tall) {
                        AnalogFace(faceIn(zone), Modifier.size(minOf(42.dp, height / (shown.size * 1.6f))))
                        Spacer(Modifier.width(10.dp))
                    } else {
                        SkySymbol(WorldClocks.daytime(zone, instant), 16.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(WorldClocks.city(zone), color = palette.onGlass, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(WorldClocks.offset(zone, here, instant), color = quiet(), fontSize = 11.sp, maxLines = 1)
                    }
                    Text(timeIn(zone), style = tight(18.sp, FontWeight.Medium), color = palette.onGlass)
                }
            }
        }
        else -> Row(Modifier.fillMaxSize().padding(pad), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            zones.forEach { zone ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    AnalogFace(faceIn(zone), Modifier.size(minOf(height * 0.5f, width / (zones.size + 0.6f))))
                    Spacer(Modifier.height(6.dp))
                    Text(WorldClocks.city(zone), color = palette.onGlass, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(timeIn(zone), color = quiet(), fontSize = 12.sp)
                    if (form == WidgetForm.Big) Text(WorldClocks.offset(zone, here, instant), color = quiet(), fontSize = 10.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun SkySymbol(day: Boolean, size: Dp) = Symbol(if (day) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay, size)

// ---------------------------------------------------------------------- countdown

@Composable
internal fun CountdownWidget(form: WidgetForm, width: Dp, height: Dp) {
    val world = LocalWidgetWorld.current
    val settings = widgetSettings()
    val tick = rememberNow()
    val today = remember(tick.toLocalDate()) { tick.toLocalDate() }
    val count = Countdown.count(settings.countdownDay, today)
    if (count == Countdown.Count.NotSet) {
        SetUpPrompt(Icons.Rounded.Event, "Tap to set a date to count down to", form) { world?.setUp("countdown") }
        return
    }
    val days = when (count) {
        is Countdown.Count.ToGo -> count.days
        is Countdown.Count.Since -> count.days
        else -> 0L
    }
    val words = Countdown.words(count, settings.countdownTitle)
    val progress = Countdown.progress(settings.countdownFrom, settings.countdownDay, today)
    val date = remember(settings.countdownDay) { LocalDate.ofEpochDay(settings.countdownDay).format(DateTimeFormatter.ofPattern("EEE d MMMM")) }
    val pad = if (form == WidgetForm.Mini) 10.dp else 14.dp

    @Composable
    fun Number(size: androidx.compose.ui.unit.TextUnit) {
        if (count == Countdown.Count.Today) Symbol(Icons.Rounded.Celebration, with(LocalDensity.current) { size.toDp() })
        else Figure(days.toString(), size, animated = true)
    }

    when (form) {
        WidgetForm.Mini -> Column(Modifier.fillMaxSize().padding(pad), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Number(fit(height * 0.42f, width * 0.42f))
            Caps(if (count == Countdown.Count.Today) "today" else "days", size = 10.sp)
        }
        WidgetForm.Strip -> Row(Modifier.fillMaxSize().padding(horizontal = pad), verticalAlignment = Alignment.CenterVertically) {
            Number(fit(height * 0.5f, 44.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Caps(settings.countdownTitle.ifBlank { "Countdown" })
                Text(words, color = quiet(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        WidgetForm.Wide -> Row(Modifier.fillMaxSize().padding(pad), verticalAlignment = Alignment.CenterVertically) {
            CountRing(progress, Modifier.fillMaxHeight().aspectRatio(1f)) { Number(fit(height * 0.26f)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Caps(settings.countdownTitle.ifBlank { "Countdown" })
                Spacer(Modifier.height(4.dp))
                Text(words, color = LocalPalette.current.onGlass, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 2)
                Text(date, color = quiet(), fontSize = 12.sp)
            }
        }
        else -> Column(Modifier.fillMaxSize().padding(pad), horizontalAlignment = Alignment.CenterHorizontally) {
            Caps(settings.countdownTitle.ifBlank { "Countdown" })
            Spacer(Modifier.height(6.dp))
            CountRing(progress, Modifier.weight(1f).aspectRatio(1f, matchHeightConstraintsFirst = true)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Number(fit(minOf(width, height) * 0.2f))
                    if (form == WidgetForm.Big && count is Countdown.Count.ToGo && days >= 14) {
                        Text("${days / 7} weeks", color = quiet(), fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(words, color = quiet(), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (form != WidgetForm.Square) Text(date, color = quiet(), fontSize = 11.sp)
        }
    }
}

/** The ring round a countdown's number, drawn round as far as the wait has gone. */
@Composable
private fun CountRing(progress: Float, modifier: Modifier, content: @Composable () -> Unit) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Ring(progress, stroke = 6.dp, modifier = Modifier.fillMaxSize())
        content()
    }
}

// ---------------------------------------------------------------------- photo frame

@Composable
internal fun PhotoFrameWidget(form: WidgetForm, width: Dp, height: Dp) {
    val world = LocalWidgetWorld.current
    val photos = widgetSettings().framePhotos
    val bare = LocalBare.current
    val reduced = LocalMotionLevel.current.reduced
    if (photos.isEmpty() || world?.frameDir == null) {
        SetUpPrompt(Icons.Rounded.PhotoLibrary, "Tap to choose your photos", form) { world?.setUp("photo") }
        return
    }
    // A different one each time the home screen is come back to, and the next at a tap.
    var tapped by remember { mutableIntStateOf(0) }
    val name = photos[((world.visits + tapped) % photos.size + photos.size) % photos.size]
    val density = LocalDensity.current
    val longest = with(density) { maxOf(width, height).roundToPx() }
    val picture by produceState<ImageBitmap?>(initialValue = null, name, longest) {
        value = withContext(Dispatchers.IO) { framePicture(java.io.File(world.frameDir, name), longest) }
    }
    AnimatedContent(
        targetState = picture,
        transitionSpec = {
            if (reduced) fadeIn(tween(Motion.REDUCED_MS)).togetherWith(fadeOut(tween(Motion.REDUCED_MS)))
            else fadeIn(tween(700)).togetherWith(fadeOut(tween(700)))
        },
        modifier = Modifier
            .fillMaxSize()
            .padding(if (bare) 0.dp else 6.dp)
            .clip(RoundedCornerShape(if (bare) 20.dp else 18.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { tapped++ },
        label = "framePhoto",
    ) { image ->
        if (image != null) Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Box(Modifier.fillMaxSize())
    }
}

/** A photo from the frame's folder, decoded no bigger than [longest] pixels across. */
private fun framePicture(file: java.io.File, longest: Int): ImageBitmap? = runCatching {
    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
    android.graphics.BitmapFactory.decodeFile(file.path, bounds)
    var sample = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= longest.coerceAtLeast(64)) sample *= 2
    android.graphics.BitmapFactory.decodeFile(file.path, android.graphics.BitmapFactory.Options().apply { inSampleSize = sample })?.asImageBitmap()
}.getOrNull()

// ---------------------------------------------------------------------- to-do

@Composable
internal fun TodoWidget(form: WidgetForm, width: Dp, height: Dp) {
    val world = LocalWidgetWorld.current
    val todos = widgetSettings().todos
    val palette = LocalPalette.current
    val left = Todos.left(todos)
    fun change(next: List<com.gabriel.zenithui.data.TodoItem>) = world?.change?.invoke { it.copy(todos = next) }
    val pad = if (form == WidgetForm.Mini) 10.dp else 14.dp
    when (form) {
        WidgetForm.Mini -> Column(Modifier.fillMaxSize().padding(pad), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Figure(left.toString(), fit(height * 0.42f, width * 0.42f), animated = true)
            Caps(if (left == 1) "to do" else "to do", size = 10.sp)
        }
        WidgetForm.Strip -> Row(Modifier.fillMaxSize().padding(horizontal = pad), verticalAlignment = Alignment.CenterVertically) {
            val next = todos.indexOfFirst { !it.done }
            if (next < 0) {
                Symbol(Icons.Rounded.Check, 20.dp)
                Spacer(Modifier.width(10.dp))
                Text(if (todos.isEmpty()) "Nothing to do yet" else "All done", color = quiet(), fontSize = 13.sp)
            } else {
                Tick(done = false) { change(Todos.toggle(todos, next)) }
                Spacer(Modifier.width(10.dp))
                Text(todos[next].text, color = palette.onGlass, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (left > 1) Text("+${left - 1}", color = quiet(), fontSize = 12.sp)
            }
        }
        else -> Column(Modifier.fillMaxSize().padding(pad)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Caps("To do", modifier = Modifier.weight(1f))
                AnimatedContent(targetState = left, transitionSpec = { com.gabriel.zenithui.ui.common.rollOver(false, down = targetState < initialState) }, label = "todoLeft") { n ->
                    Text(if (n == 0) "done" else "$n left", color = quiet(), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }
            }
            Spacer(Modifier.height(6.dp))
            // Not scrolled: the widget shows as many as it has room for, and the list moves itself
            // when one is ticked and drops below the rest.
            LazyColumn(Modifier.weight(1f), userScrollEnabled = false) {
                itemsIndexed(todos, key = { index, item -> item.text + "#" + todos.take(index).count { it.text == item.text } }) { index, item ->
                    Row(
                        Modifier
                            .animateItem()
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Tick(done = item.done) { change(Todos.toggle(todos, index)) }
                        Spacer(Modifier.width(8.dp))
                        val ink by animateColorAsState(if (item.done) quiet().copy(alpha = 0.55f) else palette.onGlass, tween(260), label = "todoInk")
                        Text(
                            item.text,
                            color = ink,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(fontFamily = LocalZenithFont.current, textDecoration = if (item.done) TextDecoration.LineThrough else TextDecoration.None),
                        )
                    }
                }
            }
            AddLine { text -> change(Todos.add(todos, text)) }
            if (form == WidgetForm.Big && todos.any { it.done }) {
                Text(
                    "Clear what is done",
                    color = accentInk(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .clickable { change(Todos.clearDone(todos)) },
                )
            }
        }
    }
}

/** A round tick: empty, or filling with the accent and drawing its check as it is ticked. */
@Composable
private fun Tick(done: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val fill by animateFloatAsState(if (done) 1f else 0f, if (reduced) tween(Motion.REDUCED_MS) else Motion.snappy(), label = "tick")
    // White on the blue, and dark on a widget's own colour where that is pale.
    val checkInk = inkOnTint(palette.accent)
    Box(
        Modifier
            .size(20.dp)
            .pressBounce(0.8f)
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            drawCircle(palette.onGlass.copy(alpha = 0.45f * (1f - fill)), r - 1.dp.toPx(), style = Stroke(1.5.dp.toPx()))
            drawCircle(lerp(palette.accent, palette.accentAlt, 0.3f), r * fill.coerceIn(0f, 1f))
            if (fill > 0.05f) {
                // The check draws itself in from its foot as the circle fills.
                val draw = fill.coerceIn(0f, 1f)
                val a = Offset(size.width * 0.28f, size.height * 0.52f)
                val b = Offset(size.width * 0.44f, size.height * 0.68f)
                val c = Offset(size.width * 0.74f, size.height * 0.36f)
                val first = (draw * 2f).coerceAtMost(1f)
                drawLine(checkInk, a, a + (b - a) * first, 2.dp.toPx(), StrokeCap.Round)
                if (draw > 0.5f) drawLine(checkInk, b, b + (c - b) * ((draw - 0.5f) * 2f), 2.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}

/** The line at the foot of the list for writing a new one, which is added with the keyboard's Done. */
@Composable
private fun AddLine(onAdd: (String) -> Unit) {
    val palette = LocalPalette.current
    var text by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    var fieldBounds by remember { mutableStateOf(androidx.compose.ui.geometry.Rect.Zero) }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { if (focused) HomeTypingArea.bounds = null } }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
        Icon(Icons.Rounded.Add, contentDescription = null, tint = quiet(), modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (text.isEmpty()) Text("Add something", color = quiet(), fontSize = 13.sp)
            BasicTextField(
                value = text,
                onValueChange = { text = it.take(80) },
                singleLine = true,
                textStyle = TextStyle(fontFamily = LocalZenithFont.current, color = palette.onGlass, fontSize = 13.sp),
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    onAdd(text)
                    text = ""
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    // While it is being typed in, a touch outside it ends the typing, as the note's does.
                    .onGloballyPositioned { coords ->
                        fieldBounds = coords.boundsInRoot()
                        if (focused) HomeTypingArea.bounds = fieldBounds
                    }
                    .onFocusChanged { state ->
                        focused = state.isFocused
                        HomeTypingArea.bounds = if (state.isFocused) fieldBounds else null
                    },
            )
        }
    }
}

// ---------------------------------------------------------------------- aura

@Composable
internal fun AuraWidget(form: WidgetForm, width: Dp, height: Dp) {
    val settings = widgetSettings()
    val palette = LocalPalette.current
    val streak = settings.streak
    val pad = if (form == WidgetForm.Mini) 10.dp else 14.dp
    val next = remember(settings.unlockedThemes) { AuraThemes.next(settings.unlockedThemes) }
    when (form) {
        WidgetForm.Mini -> Column(Modifier.fillMaxSize().padding(pad), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Symbol(Icons.Rounded.LocalFireDepartment, minOf(height, width) * 0.3f)
            Figure(streak.toString(), fit(height * 0.32f, width * 0.32f), animated = true)
        }
        WidgetForm.Strip -> Row(Modifier.fillMaxSize().padding(horizontal = pad), verticalAlignment = Alignment.CenterVertically) {
            Symbol(Icons.Rounded.LocalFireDepartment, 24.dp)
            Spacer(Modifier.width(8.dp))
            Figure(streak.toString(), fit(height * 0.5f, 40.dp), animated = true)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Caps(if (streak == 1) "day streak" else "day streak")
                Text("${settings.aura} aura", color = quiet(), fontSize = 12.sp)
            }
        }
        WidgetForm.Wide -> Row(Modifier.fillMaxSize().padding(pad), verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Symbol(Icons.Rounded.LocalFireDepartment, 28.dp)
                Figure(streak.toString(), fit(height * 0.36f), animated = true)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Caps("This week")
                Spacer(Modifier.height(6.dp))
                WeekDots(streak)
                Spacer(Modifier.height(8.dp))
                Text("${settings.aura} aura", color = palette.onGlass, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
        else -> Column(Modifier.fillMaxSize().padding(pad), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceEvenly) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Symbol(Icons.Rounded.LocalFireDepartment, 22.dp)
                Spacer(Modifier.width(6.dp))
                Caps("Streak")
            }
            Figure(streak.toString(), fit(minOf(width, height) * 0.28f), animated = true)
            Text(if (streak == 1) "day in a row" else "days in a row", color = quiet(), fontSize = 12.sp)
            WeekDots(streak)
            if (form == WidgetForm.Big || form == WidgetForm.Tall) {
                Text("${settings.aura} aura", color = palette.onGlass, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                if (next != null && form == WidgetForm.Big) {
                    val share = (settings.aura.toFloat() / next.price).coerceIn(0f, 1f)
                    val filled = rememberFilled(share)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${next.name} at ${next.price}", color = quiet(), fontSize = 11.sp)
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.width(width * 0.6f).height(4.dp).clip(CircleShape).background(palette.onGlass.copy(alpha = 0.14f))) {
                            Box(Modifier.fillMaxHeight().fillMaxWidth(filled.value).clip(CircleShape).background(Brush.horizontalGradient(listOf(palette.accent, palette.accentAlt))))
                        }
                    }
                }
            }
        }
    }
}

/** The last seven days as dots, as many lit as the streak covers, filling one after another. */
@Composable
private fun WeekDots(streak: Int) {
    val palette = LocalPalette.current
    val lit = streak.coerceIn(0, 7)
    val filled = rememberFilled(lit / 7f)
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        for (i in 0 until 7) {
            val on = (filled.value * 7f - i).coerceIn(0f, 1f)
            Box(
                Modifier
                    .size(9.dp)
                    .clip(CircleShape)
                    .background(lerp(palette.onGlass.copy(alpha = 0.16f), palette.accent, on)),
            )
        }
    }
}

// ---------------------------------------------------------------------- weather

@Composable
internal fun WeatherWidget(form: WidgetForm, width: Dp, height: Dp) {
    val world = LocalWidgetWorld.current
    val settings = widgetSettings()
    val palette = LocalPalette.current
    if (settings.weatherPlace.isBlank()) {
        SetUpPrompt(Icons.Rounded.WbSunny, "Tap to choose a place", form) { world?.setUp("weather") }
        return
    }
    val forecast = world?.forecast?.takeIf { it.isFor(settings.weatherPlace, settings.weatherLat, settings.weatherLon, settings.weatherUnits) }
    if (forecast == null) {
        SetUpPrompt(Icons.Rounded.Cloud, "Getting the weather for ${settings.weatherPlace}", form, null)
        return
    }
    val temp = WeatherParse.degrees(forecast.temperature)
    val words = WeatherCodes.words(forecast.code, forecast.day)
    val pad = if (form == WidgetForm.Mini) 10.dp else 14.dp
    when (form) {
        WidgetForm.Mini -> Column(Modifier.fillMaxSize().padding(pad), verticalArrangement = Arrangement.SpaceBetween) {
            Symbol(skyIcon(forecast.code, forecast.day), minOf(width, height) * 0.3f)
            Figure(temp, fit(height * 0.36f, width * 0.32f), animated = true)
        }
        WidgetForm.Strip -> Row(Modifier.fillMaxSize().padding(horizontal = pad), verticalAlignment = Alignment.CenterVertically) {
            Symbol(skyIcon(forecast.code, forecast.day), 26.dp)
            Spacer(Modifier.width(10.dp))
            Figure(temp, fit(height * 0.5f, 40.dp), animated = true)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Caps(forecast.place)
                Text(words, color = quiet(), fontSize = 12.sp, maxLines = 1)
            }
        }
        WidgetForm.Square -> Column(Modifier.fillMaxSize().padding(pad)) {
            Caps(forecast.place)
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Figure(temp, fit(height * 0.3f, width * 0.3f), animated = true)
                Spacer(Modifier.width(6.dp))
                Symbol(skyIcon(forecast.code, forecast.day), 28.dp)
            }
            Spacer(Modifier.weight(1f))
            Text(words, color = palette.onGlass, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text("H ${WeatherParse.degrees(forecast.high)}  L ${WeatherParse.degrees(forecast.low)}", color = quiet(), fontSize = 12.sp)
        }
        WidgetForm.Wide -> Row(Modifier.fillMaxSize().padding(pad), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(0.9f)) {
                Caps(forecast.place)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Figure(temp, fit(height * 0.38f), animated = true)
                    Spacer(Modifier.width(6.dp))
                    Symbol(skyIcon(forecast.code, forecast.day), 26.dp)
                }
                Text("$words · H ${WeatherParse.degrees(forecast.high)} L ${WeatherParse.degrees(forecast.low)}", color = quiet(), fontSize = 12.sp, maxLines = 1)
            }
            Hours(forecast, Modifier.weight(1.1f), across = true)
        }
        else -> Column(Modifier.fillMaxSize().padding(pad)) {
            Caps(forecast.place)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Figure(temp, fit(height * 0.18f, width * 0.24f), animated = true)
                Spacer(Modifier.width(8.dp))
                Symbol(skyIcon(forecast.code, forecast.day), 30.dp)
            }
            Text("$words · H ${WeatherParse.degrees(forecast.high)} L ${WeatherParse.degrees(forecast.low)}", color = quiet(), fontSize = 12.sp, maxLines = 1)
            Spacer(Modifier.height(10.dp))
            Hours(forecast, Modifier.weight(1f).fillMaxWidth(), across = form == WidgetForm.Big)
        }
    }
}

/** The next hours: each its hour, its sky and its temperature, side by side or one under another. */
@Composable
private fun Hours(forecast: Forecast, modifier: Modifier, across: Boolean) {
    val palette = LocalPalette.current
    if (across) {
        Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            forecast.hours.forEach { hour ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(hour.hour, color = quiet(), fontSize = 11.sp)
                    Spacer(Modifier.height(4.dp))
                    Symbol(skyIcon(hour.code, true), 18.dp)
                    Spacer(Modifier.height(4.dp))
                    Text(WeatherParse.degrees(hour.temperature), color = palette.onGlass, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    } else {
        Column(modifier, verticalArrangement = Arrangement.SpaceEvenly) {
            forecast.hours.forEach { hour ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(hour.hour, color = quiet(), fontSize = 12.sp, modifier = Modifier.width(28.dp))
                    Symbol(skyIcon(hour.code, true), 18.dp)
                    Spacer(Modifier.weight(1f))
                    Text(WeatherParse.degrees(hour.temperature), color = palette.onGlass, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

private fun skyIcon(code: Int, day: Boolean): ImageVector = when (WeatherCodes.kind(code)) {
    WeatherCodes.Kind.Clear -> if (day) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
    WeatherCodes.Kind.PartlyCloudy -> if (day) Icons.Rounded.WbCloudy else Icons.Rounded.NightsStay
    WeatherCodes.Kind.Cloudy -> Icons.Rounded.Cloud
    WeatherCodes.Kind.Fog -> Icons.Rounded.Dehaze
    WeatherCodes.Kind.Drizzle -> Icons.Rounded.Grain
    WeatherCodes.Kind.Rain -> Icons.Rounded.Umbrella
    WeatherCodes.Kind.Snow -> Icons.Rounded.AcUnit
    WeatherCodes.Kind.Storm -> Icons.Rounded.FlashOn
}

/** A city's sun or moon, for lists outside the widget. */
@Composable
internal fun CityIcon(zone: String) = Icon(
    if (WorldClocks.daytime(zone, Instant.now())) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay,
    contentDescription = null,
    tint = LocalPalette.current.onGlassMuted,
    modifier = Modifier.size(18.dp),
)
