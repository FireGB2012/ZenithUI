package com.gabriel.zenithui.ui.home.widgets

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gabriel.zenithui.data.BatteryFacts
import com.gabriel.zenithui.data.BatteryTone
import com.gabriel.zenithui.data.Box as PieceBox
import com.gabriel.zenithui.data.ClockWords
import com.gabriel.zenithui.data.Forecast
import com.gabriel.zenithui.data.Greeting
import com.gabriel.zenithui.data.MadeBack
import com.gabriel.zenithui.data.MadeWidget
import com.gabriel.zenithui.data.MadeWords
import com.gabriel.zenithui.data.Piece
import com.gabriel.zenithui.data.PieceAlign
import com.gabriel.zenithui.data.PieceKinds
import com.gabriel.zenithui.data.PiecePlace
import com.gabriel.zenithui.data.PieceStyles
import com.gabriel.zenithui.data.Pieces
import com.gabriel.zenithui.ui.home.LocalBare
import com.gabriel.zenithui.ui.home.LocalWidgetWorld
import com.gabriel.zenithui.ui.home.WidgetColours
import com.gabriel.zenithui.ui.home.WidgetShape
import com.gabriel.zenithui.ui.home.WordsOnCard
import com.gabriel.zenithui.ui.home.WordsOnWallpaper
import com.gabriel.zenithui.ui.home.bareShadow
import com.gabriel.zenithui.ui.home.colourCard
import com.gabriel.zenithui.ui.theme.LocalNumberFont
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.ZenithFonts
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.glassInk
import com.gabriel.zenithui.ui.theme.glassOnGlass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/*
 * A widget you made in the widget maker (data/MadeWidgets.kt), drawn at whatever size it is given:
 * on the home screen, in a stack, in the gallery and on the maker's stage, by this one piece of
 * code. See specs/2026-11-18-widget-maker-design.md.
 *
 * Each piece has a point, as a share of the widget's width and height, and lines up on it; its size
 * is a share of the widget's shorter side, so the same design reads the same on any phone and at
 * any size it is pulled to. Words too wide for the widget are drawn smaller until they fit. Nothing
 * moves by itself but the clock, which ticks once a minute, or once a second for a piece that shows
 * seconds, so Lite has nothing to stop.
 */

/** True on the maker's stage: a piece with nothing to show shows what it is, so it can still be found and picked. */
internal val LocalInMaker = staticCompositionLocalOf { false }

/**
 * [design], standing on what it stands on: the widgets' own pane of glass (tinted with its colour
 * when it has one), a card in its colour, or nothing but the wallpaper. A design that has been
 * deleted draws nothing. [interactive] lights the pane under a finger, as the other widgets' panes
 * are lit. [onLaidOut] hears where every piece is, in pixels of the widget, each time they are placed.
 */
@Composable
internal fun MadeWidgetView(
    design: MadeWidget?,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    onLaidOut: ((Map<String, PieceBox>) -> Unit)? = null,
) {
    if (design == null) {
        Box(modifier)
        return
    }
    when (design.back) {
        MadeBack.CLEAR -> CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.merge(TextStyle(shadow = bareShadow())),
        ) {
            MadePieces(design, modifier, onLaidOut)
        }

        MadeBack.COLOUR -> {
            val dark = LocalPalette.current.dark
            val fill = remember(design.colour, dark) {
                WidgetColours.made(design.colour.takeIf { it != 0L }?.let { Color(it) }, dark)
            }
            WordsOnCard(fill) {
                // The card's own ink, whatever the phone's appearance: white words on a blue card in
                // Light as in Dark, as the colour was chosen for.
                CompositionLocalProvider(LocalPalette provides LocalPalette.current.copy(ink = fill.ink)) {
                    MadePieces(design, modifier.colourCard(fill), onLaidOut)
                }
            }
        }

        MadeBack.GLASS -> {
            val tint = if (design.colour != 0L) Color(design.colour) else Color.Unspecified
            WordsOnWallpaper {
                MadePieces(
                    design,
                    modifier
                        .glass(WidgetShape, tint = tint, strength = 1f, interactive = interactive, wallpaperTint = true)
                        .clip(WidgetShape),
                    onLaidOut,
                )
            }
        }
    }
}

/** What the pieces of one widget read, read once for all of them, and only what its pieces need. */
@Immutable
private class PieceWorld(
    val back: MadeBack,
    /** The widget's shorter side, and the room words have across it, in pixels. */
    val shorter: Float,
    val room: Float,
    val width: Float,
    val height: Float,
    val now: State<LocalDateTime>,
    val h24: Boolean,
    val battery: BatteryInfo?,
    val forecast: Forecast?,
    val nextAlarm: Long?,
    val name: String,
    val photos: File?,
)

@Composable
private fun MadePieces(design: MadeWidget, modifier: Modifier, onLaidOut: ((Map<String, PieceBox>) -> Unit)?) {
    val pieces = design.pieces
    val kinds = remember(pieces) { pieces.mapTo(HashSet()) { it.kind } }
    val seconds = remember(pieces) {
        pieces.any {
            (it.kind == PieceKinds.TIME && it.style == PieceStyles.TIME_SECONDS) ||
                (it.kind == PieceKinds.DIAL && it.style == PieceStyles.DIAL_SECONDS)
        }
    }
    val now = rememberClock(seconds)
    val world = LocalWidgetWorld.current
    val h24 = if (PieceKinds.TIME in kinds || PieceKinds.ALARM in kinds) rememberH24() else true
    val battery = if (PieceKinds.BATTERY in kinds) rememberBattery() else null
    val alarm = if (PieceKinds.ALARM in kinds) rememberNextAlarm() else null
    val settings = world?.settings
    // The forecast for the place the Weather widget is set to, as that widget reads it.
    val forecast = world?.forecast?.takeIf {
        settings != null && it.isFor(settings.weatherPlace, settings.weatherLat, settings.weatherLon, settings.weatherUnits)
    }
    val density = LocalDensity.current
    BoxWithConstraints(modifier) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val inset = with(density) { INSET.toPx() }
        val room = max(width - inset * 2f, width * 0.5f)
        val pieceWorld = remember(design.back, width, height, now, h24, battery, forecast, alarm, settings?.userName, world?.madeDir) {
            PieceWorld(
                back = design.back,
                shorter = min(width, height),
                room = room,
                width = width,
                height = height,
                now = now,
                h24 = h24,
                battery = battery,
                forecast = forecast,
                nextAlarm = alarm,
                name = settings?.userName.orEmpty(),
                photos = world?.madeDir,
            )
        }
        Layout(
            content = {
                pieces.forEach { piece ->
                    key(piece.id) { PieceView(piece, pieceWorld) }
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) { measurables, constraints ->
            val w = constraints.maxWidth
            val h = constraints.maxHeight
            val placeables = measurables.mapIndexed { index, measurable ->
                val piece = pieces[index]
                if (PieceKinds.isBox(piece.kind)) {
                    measurable.measure(Constraints.fixed((piece.w * w).roundToInt().coerceAtLeast(1), (piece.h * h).roundToInt().coerceAtLeast(1)))
                } else {
                    measurable.measure(Constraints())
                }
            }
            layout(w, h) {
                val boxes = if (onLaidOut != null) HashMap<String, PieceBox>(pieces.size) else null
                placeables.forEachIndexed { index, placeable ->
                    val piece = pieces[index]
                    val left = PiecePlace.left(piece.x * w, placeable.width.toFloat(), piece.align)
                    val top = PiecePlace.top(piece.y * h, placeable.height.toFloat())
                    placeable.place(left.roundToInt(), top.roundToInt())
                    boxes?.put(piece.id, PieceBox(left, top, placeable.width.toFloat(), placeable.height.toFloat()))
                }
                if (boxes != null) onLaidOut?.invoke(boxes)
            }
        }
    }
}

/**
 * The time, read again at the turn of each minute, or of each second when a piece shows seconds,
 * and whenever the home screen comes back, since a phone asleep does not count the time it sleeps.
 */
@Composable
private fun rememberClock(seconds: Boolean): State<LocalDateTime> {
    fun read(): LocalDateTime = LocalDateTime.now().withNano(0).let { if (seconds) it else it.withSecond(0) }
    val state = remember { mutableStateOf(read()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { state.value = read() }
    LaunchedEffect(seconds) {
        while (true) {
            state.value = read()
            val millis = System.currentTimeMillis()
            delay(if (seconds) 1_000L - millis % 1_000L else 60_000L - millis % 60_000L)
        }
    }
    return state
}

/** One piece, as its kind draws it, made of glass when it is. */
@Composable
private fun PieceView(piece: Piece, world: PieceWorld) {
    val look = if (piece.alpha < 1f) Modifier.alpha(piece.alpha) else Modifier
    Box(look) {
        if (piece.glass) {
            // Glass is what a No glass widget's pieces are made of: the same switch makes these.
            CompositionLocalProvider(LocalBare provides true) { PieceBody(piece, world) }
        } else {
            PieceBody(piece, world)
        }
    }
}

@Composable
private fun PieceBody(piece: Piece, world: PieceWorld) {
    when (piece.kind) {
        PieceKinds.TIME -> {
            val now by world.now
            WordsPiece(MadeWords.time(now, piece.style, world.h24), piece, world, numbers = true)
        }

        PieceKinds.DATE -> {
            val now by world.now
            val numbers = piece.style == PieceStyles.DAY_NUMBER || piece.style == PieceStyles.DATE_NUMBERS
            WordsPiece(MadeWords.date(now.toLocalDate(), piece.style), piece, world, numbers = numbers)
        }

        PieceKinds.TEXT -> {
            val empty = piece.text.isBlank()
            if (empty && !LocalInMaker.current) return
            Box(if (empty) Modifier.alpha(0.4f) else Modifier) {
                WordsPiece(if (empty) "Your words" else piece.text, piece, world, numbers = false)
            }
        }

        PieceKinds.GREETING -> {
            val now by world.now
            WordsPiece(Greeting.of(now.hour, world.name), piece, world, numbers = false)
        }

        PieceKinds.ALARM -> {
            // Read with the clock, so an alarm that has gone off leaves the widget at the turn of the minute.
            val now by world.now
            val zone = ZoneId.systemDefault()
            val next = world.nextAlarm?.let { ClockWords.alarm(it, now.atZone(zone).toInstant().toEpochMilli(), zone, world.h24) }
            WordsPiece(MadeWords.alarm(next), piece, world, numbers = false)
        }

        PieceKinds.BATTERY -> BatteryPiece(piece, world)
        PieceKinds.DIAL -> {
            val now by world.now
            val side = with(LocalDensity.current) { (piece.size * world.shorter).toDp() }
            Dial(
                time = now,
                face = dialFaceAt(now.hour),
                modifier = Modifier.size(side),
                numerals = piece.style == PieceStyles.DIAL_NUMERALS,
                seconds = piece.style == PieceStyles.DIAL_SECONDS,
            )
        }

        PieceKinds.WEATHER -> WeatherPiece(piece, world)
        PieceKinds.SYMBOL -> SymbolPiece(piece, world)
        PieceKinds.SHAPE -> ShapePiece(piece, world)
        PieceKinds.PHOTO -> PhotoPiece(piece, world)
    }
}

/**
 * Words: set in the piece's font, weight and size, lined up as the piece is, and drawn smaller
 * until they fit when they are wider than the widget has room for. [numbers] sets them in the
 * numbers' font with figures all one width, so a time that changes keeps its place.
 */
@Composable
private fun WordsPiece(text: String, piece: Piece, world: PieceWorld, numbers: Boolean, fitted: Boolean = true) {
    val density = LocalDensity.current
    val glass = LocalBare.current
    val px = piece.size * world.shorter
    val family = when {
        piece.font.isNotEmpty() -> ZenithFonts.byId(piece.font).family
        numbers -> LocalNumberFont.current.family
        else -> LocalZenithFont.current
    }
    val colour = when {
        piece.colour != 0L -> Color(piece.colour)
        glass -> Color.White
        else -> ink()
    }
    val size = with(density) { px.toSp() }
    val style = TextStyle(
        color = colour,
        fontFamily = family,
        fontSize = size,
        fontWeight = FontWeight(piece.weight.coerceIn(100, 900)),
        lineHeight = size * 1.12f,
        letterSpacing = if (numbers) (-0.02).em else 0.em,
        textAlign = when (piece.align) {
            PieceAlign.START -> TextAlign.Left
            PieceAlign.CENTER -> TextAlign.Center
            PieceAlign.END -> TextAlign.Right
        },
        fontFeatureSettings = if (numbers) "tnum" else null,
        shadow = if (glass) null else LocalTextStyle.current.shadow,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
    val measurer = rememberTextMeasurer(cacheSize = 2)
    val natural = remember(text, style, measurer, fitted) {
        if (!fitted) 0f else measurer.measure(text, style, softWrap = false, maxLines = Pieces.MOST_LINES).size.width.toFloat()
    }
    val fit = PiecePlace.fit(natural, world.room)
    val shown = if (fit < 1f) style.copy(fontSize = style.fontSize * fit, lineHeight = style.lineHeight * fit) else style
    Text(
        text,
        style = shown,
        softWrap = false,
        maxLines = Pieces.MOST_LINES,
        modifier = if (glass) Modifier.glassInk(bevel = with(density) { (px * fit * 0.07f).toDp() }) else Modifier,
    )
}

/** The battery as a ring with the number in it, as the number alone, or as a bar. Green, yellow on saver, red when low. */
@Composable
private fun BatteryPiece(piece: Piece, world: PieceWorld) {
    val battery = world.battery ?: BatteryInfo()
    val level = battery.level
    val fraction = if (level < 0) 0f else level / 100f
    val colour = if (piece.colour != 0L) {
        Color(piece.colour)
    } else {
        when (BatteryFacts.tone(level, battery.charging, battery.saver)) {
            BatteryTone.Good -> hue(Hue.Green)
            BatteryTone.Saver -> hue(Hue.Yellow)
            BatteryTone.Low -> hue(Hue.Red)
        }
    }
    val percent = if (level < 0) "${MadeWords.NO_NUMBER}%" else MadeWords.battery(level)
    val density = LocalDensity.current
    val px = piece.size * world.shorter
    when (piece.style) {
        PieceStyles.BATTERY_NUMBER -> WordsPiece(percent, piece, world, numbers = true)
        PieceStyles.BATTERY_BAR -> with(density) {
            Meter(
                fraction = fraction,
                colour = colour,
                modifier = Modifier.width((px * 1.6f).toDp()),
                thickness = max(4.dp.toPx(), px * 0.1f).toDp(),
            )
        }

        else -> with(density) {
            Gauge(
                fraction = fraction,
                colour = colour,
                stroke = max(3.dp.toPx(), px * 0.09f).toDp(),
                modifier = Modifier.size(px.toDp()),
            ) {
                Hero(percent, (px * 0.24f).toSp(), weight = FontWeight(piece.weight.coerceIn(100, 900)))
            }
        }
    }
}

/** The weather where the Weather widget is set to: its sky, its temperature, both, the sky in words, or the day's high and low. */
@Composable
private fun WeatherPiece(piece: Piece, world: PieceWorld) {
    val forecast = world.forecast
    val code = forecast?.code ?: CLOUDY
    val day = forecast?.day ?: true
    val density = LocalDensity.current
    val px = piece.size * world.shorter
    when (piece.style) {
        PieceStyles.WEATHER_SKY -> WeatherGlyph(code, day, with(density) { px.toDp() })
        PieceStyles.WEATHER_BOTH -> Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(with(density) { (px * 0.18f).toDp() }),
        ) {
            WeatherGlyph(code, day, with(density) { (px * 1.15f).toDp() })
            WordsPiece(MadeWords.weather(forecast, PieceStyles.WEATHER_TEMPERATURE), piece, world, numbers = true, fitted = false)
        }

        else -> WordsPiece(MadeWords.weather(forecast, piece.style), piece, world, numbers = piece.style != PieceStyles.WEATHER_WORDS)
    }
}

/** Open-Meteo's number for a cloudy sky: what the sky shows before there is a forecast. */
private const val CLOUDY = 3

@Composable
private fun SymbolPiece(piece: Piece, world: PieceWorld) {
    val glass = LocalBare.current
    val side = with(LocalDensity.current) { (piece.size * world.shorter).toDp() }
    Icon(
        symbolIcon(piece.style),
        contentDescription = null,
        tint = when {
            piece.colour != 0L -> Color(piece.colour)
            glass -> Color.White
            else -> ink()
        },
        modifier = Modifier
            .size(side)
            .then(if (glass) Modifier.glassInk(bevel = side * 0.09f) else Modifier),
    )
}

/** The icon for each of [PieceStyles.symbols]. */
internal fun symbolIcon(name: String): ImageVector = when (name) {
    "star" -> Icons.Rounded.Star
    "bolt" -> Icons.Rounded.Bolt
    "sun" -> Icons.Rounded.WbSunny
    "moon" -> Icons.Rounded.NightsStay
    "cloud" -> Icons.Rounded.Cloud
    "flame" -> Icons.Rounded.LocalFireDepartment
    "leaf" -> Icons.Rounded.Eco
    "drop" -> Icons.Rounded.WaterDrop
    "music" -> Icons.Rounded.MusicNote
    "coffee" -> Icons.Rounded.LocalCafe
    "home" -> Icons.Rounded.Home
    "bell" -> Icons.Rounded.Notifications
    "camera" -> Icons.Rounded.PhotoCamera
    "game" -> Icons.Rounded.SportsEsports
    "rocket" -> Icons.Rounded.RocketLaunch
    "sparkle" -> Icons.Rounded.AutoAwesome
    "pin" -> Icons.Rounded.Place
    "smile" -> Icons.Rounded.SentimentSatisfied
    "check" -> Icons.Rounded.CheckCircle
    else -> Icons.Rounded.Favorite
}

/** An oval, for a circle pulled out of round. */
private val Oval = GenericShape { size, _ -> addOval(androidx.compose.ui.geometry.Rect(Offset.Zero, size)) }

/** A shape's outline: a rectangle with corners as round as it asks, or an oval. */
private fun shapeOf(piece: Piece): Shape = when (piece.style) {
    PieceStyles.SHAPE_CIRCLE, PieceStyles.SHAPE_RING -> Oval
    else -> RoundedCornerShape(percent = (piece.corner.coerceIn(0f, 1f) * 50f).roundToInt())
}

/** A shape, filling the box the layout gives it: solid, a ring, or a pane of glass. */
@Composable
private fun ShapePiece(piece: Piece, world: PieceWorld) {
    val palette = LocalPalette.current
    val glass = LocalBare.current
    val shape = shapeOf(piece)
    val colour = if (piece.colour != 0L) Color(piece.colour) else palette.accent
    when {
        piece.style == PieceStyles.SHAPE_RING -> Canvas(
            Modifier
                .fillMaxSize()
                .then(if (glass) Modifier.glassInk(bevel = 2.dp) else Modifier),
        ) {
            val stroke = max(2.dp.toPx(), size.minDimension * 0.08f)
            drawOval(
                if (glass) Color.White else colour,
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke),
            )
        }

        glass && world.back == MadeBack.CLEAR -> Box(
            Modifier
                .fillMaxSize()
                .glass(shape, tint = if (piece.colour != 0L) colour else Color.Unspecified, strength = 1f, wallpaperTint = true),
        )

        glass -> Box(Modifier.fillMaxSize().glassOnGlass(shape, dark = palette.dark))
        else -> Box(Modifier.fillMaxSize().background(colour, shape))
    }
}

/** A photo from your own, cut to the piece's box with its corners, or a frame waiting for one. */
@Composable
private fun PhotoPiece(piece: Piece, world: PieceWorld) {
    val shape = shapeOf(piece.copy(style = PieceStyles.SHAPE_RECT))
    val dir = world.photos
    val name = piece.text
    // Decoded a size up from what it is drawn at, so pulling it bigger reads the file again only now and then.
    val wanted = max(piece.w * world.width, piece.h * world.height)
    val longest = (ceil(wanted / DECODE_STEP) * DECODE_STEP).toInt().coerceIn(DECODE_STEP.toInt(), DECODE_MOST)
    val picture by produceState<ImageBitmap?>(null, dir, name, longest) {
        value = if (dir == null || name.isEmpty()) null else withContext(Dispatchers.IO) { framePicture(File(dir, name), longest) }
    }
    val image = picture
    Box(Modifier.fillMaxSize().clip(shape), contentAlignment = Alignment.Center) {
        if (image != null && name.isNotEmpty()) {
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            val ink = ink()
            Box(Modifier.fillMaxSize().background(ink.copy(alpha = 0.12f)))
            val side = with(LocalDensity.current) { (min(piece.w * world.width, piece.h * world.height) * 0.34f).toDp() }
            Icon(Icons.Rounded.Image, contentDescription = null, tint = ink.copy(alpha = 0.55f), modifier = Modifier.size(side))
        }
    }
}

/** The steps a photo is decoded at, in pixels, and the most it is decoded at: about a screen. */
private const val DECODE_STEP = 256f
private const val DECODE_MOST = 2048
