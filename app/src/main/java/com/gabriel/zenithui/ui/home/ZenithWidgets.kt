package com.gabriel.zenithui.ui.home

import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.DoNotDisturbOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.rounded.AirplanemodeActive
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.SignalCellularAlt
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.graphicsLayer
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.HomeGeometry
import com.gabriel.zenithui.data.ToggleGrid
import com.gabriel.zenithui.data.ToggleNames
import com.gabriel.zenithui.data.WidgetForm
import com.gabriel.zenithui.data.WidgetLooks
import com.gabriel.zenithui.data.Widgets
import com.gabriel.zenithui.ui.common.labelShadow
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.common.shake
import com.gabriel.zenithui.ui.settings.Toggle as OnOffSwitch
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.GlassMotion
import com.gabriel.zenithui.ui.theme.glassPress
import com.gabriel.zenithui.ui.theme.LocalNumberFont
import com.gabriel.zenithui.ui.theme.LocalOnWallpaper
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalWidgetLook
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.WALLPAPER_VEIL
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.addOutline
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.glassInk
import com.gabriel.zenithui.ui.theme.inkOnTint
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.gabriel.zenithui.ui.home.widgets.CalendarWidget
import com.gabriel.zenithui.ui.home.widgets.ClockWidget
import com.gabriel.zenithui.ui.home.widgets.CountdownWidget
import com.gabriel.zenithui.ui.home.widgets.MusicWidget
import com.gabriel.zenithui.ui.home.widgets.NotesWidget
import com.gabriel.zenithui.ui.home.widgets.PhotoWidget
import com.gabriel.zenithui.ui.home.widgets.PowerWidget
import com.gabriel.zenithui.ui.home.widgets.StreakWidget
import com.gabriel.zenithui.ui.home.widgets.TodoWidget
import com.gabriel.zenithui.ui.home.widgets.WeatherWidget
import com.gabriel.zenithui.ui.home.widgets.WorldWidget
import kotlinx.coroutines.delay
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime

/** The shape a widget has been given: see [WidgetForm], where the lines between them are drawn. */
private fun formOf(width: Dp, height: Dp): WidgetForm = WidgetForm.of(width.value, height.value)

/**
 * True inside a "No glass" widget. There is no pane: the widget sits straight on the wallpaper,
 * its words white with a soft shadow, and its big numbers, rings and icons made of glass.
 */
internal val LocalBare = staticCompositionLocalOf { false }

/**
 * The corners of every widget on the home screen, whatever its size: Zenith's own in either look,
 * and other apps' too. See [HomeGeometry.WIDGET_CORNER_DP].
 */
internal val WidgetShape = RoundedCornerShape(HomeGeometry.WIDGET_CORNER_DP.dp)

/**
 * A widget's own colour as InOwnColour eases it in and out: [colour], and how far it has come in,
 * [mix], from 0 for none to 1. The colour look fills its card with it; the glass look tints its pane.
 */
@Immutable
internal data class OwnColour(val colour: Color, val mix: Float) {
    /** Whether any of a colour has come in yet. */
    val shows: Boolean get() = mix > 0f && colour.isSpecified

    companion object {
        val None = OwnColour(Color.Unspecified, 0f)
    }
}

internal val LocalOwnColour = compositionLocalOf { OwnColour.None }

/**
 * The card of the colour widget being drawn, or null for one on glass or straight on the wallpaper:
 * its second lines are the card's own quiet shade ([quiet]), worked out to read on it.
 */
internal val LocalWidgetFill = staticCompositionLocalOf<WidgetFill?> { null }

/** Renders one of Zenith's built-in widgets into whatever space the grid gave it. */
@Composable
fun ZenithWidget(
    type: String,
    userName: String,
    note: String,
    onNoteChange: (String) -> Unit,
    onSearch: () -> Unit,
    onAmbient: () -> Unit = {},
    /** Which controls the Quick Toggles widget shows. */
    toggles: List<String> = listOf("torch", "wifi", "bluetooth", "focus"),
    modifier: Modifier = Modifier,
) {
    val kind = Widgets.baseType(type)
    if (kind == "search") {
        // A one line bar: it draws, and reacts, at exactly its own size.
        Box(modifier, contentAlignment = Alignment.Center) {
            WordsOnWallpaper { SearchWidget(onSearch) }
        }
        return
    }

    val palette = LocalPalette.current
    if (type.startsWith(Widgets.BARE_PREFIX)) {
        // Straight on the wallpaper, words take the ink the labels under the icons have: white in
        // the dark, near-black in the light, where Zenith's own wallpapers are pale and white
        // words would be lost. Each gets a soft edge of the opposite tone to stand on.
        val shadow = with(LocalDensity.current) {
            if (palette.dark) {
                Shadow(Color.Black.copy(alpha = 0.72f), Offset(0f, 1.dp.toPx()), 5.dp.toPx())
            } else {
                Shadow(Color.White.copy(alpha = 0.9f), Offset.Zero, 5.dp.toPx())
            }
        }
        CompositionLocalProvider(
            LocalBare provides true,
            LocalTextStyle provides LocalTextStyle.current.merge(TextStyle(shadow = shadow)),
        ) {
            BoxWithConstraints(modifier) {
                WidgetBody(kind, formOf(maxWidth, maxHeight), maxWidth, maxHeight, userName, note, onNoteChange, toggles)
            }
        }
        return
    }

    // One switch for every one of Zenith's widgets: a card in colours of its own, as iOS's widgets
    // are, or a pane of the wallpaper's glass. A colour somebody chose is the card in the one and
    // tints the pane in the other.
    val own = LocalOwnColour.current
    if (LocalWidgetLook.current == WidgetLooks.COLOUR) {
        val fill = rememberFill(kind, own)
        BoxWithConstraints(modifier.colourCard(fill)) {
            WordsOnCard(fill) {
                WidgetBody(kind, formOf(maxWidth, maxHeight), maxWidth, maxHeight, userName, note, onNoteChange, toggles)
            }
        }
        return
    }

    // A widget with a colour of its own is on a home page, whose glass has the wallpaper's grey veil,
    // so its colour comes in from that grey and goes back to it.
    val tint = if (own.shows) lerp(WALLPAPER_VEIL, own.colour, own.mix.coerceAtMost(1f)) else Color.Unspecified
    BoxWithConstraints(
        modifier
            // Glass made from the wallpaper, or, for a widget with a colour of its own, tinted with that.
            .glass(WidgetShape, tint = tint, strength = 1f, interactive = true, wallpaperTint = true)
            .clip(WidgetShape)
    ) {
        WordsOnWallpaper {
            WidgetBody(kind, formOf(maxWidth, maxHeight), maxWidth, maxHeight, userName, note, onNoteChange, toggles)
        }
    }
}

/**
 * The card a widget of [kind] wears in the colour look, in Light or Dark as the page is: its kind's
 * own colours, the weather's sky as it is now, and a colour of its own eased in over them.
 */
@Composable
private fun rememberFill(kind: String, own: OwnColour): WidgetFill {
    val dark = LocalPalette.current.dark
    val sky = if (kind == Widgets.Weather.type) skyNow() else Sky.Day
    return remember(kind, dark, sky, own) {
        val plain = WidgetColours.of(kind, dark, sky)
        if (!own.shows) plain else plain.towards(WidgetColours.of(kind, dark, sky, own.colour), own.mix.coerceAtMost(1f))
    }
}

/**
 * Part of the way from this card to [other]: its colours blend, and its words are the nearer card's,
 * so they are always one of the two inks a palette carries, and change over halfway.
 */
private fun WidgetFill.towards(other: WidgetFill, fraction: Float): WidgetFill {
    val words = if (fraction < 0.5f) this else other
    return words.copy(top = lerp(top, other.top, fraction), bottom = lerp(bottom, other.bottom, fraction))
}

/**
 * The sky over the weather widget's place, for its card in the colour look: the forecast's, read as
 * WeatherWidget reads it, or a clear day until there is a forecast for the place chosen.
 */
@Composable
private fun skyNow(): Sky {
    val world = LocalWidgetWorld.current ?: return Sky.Day
    val settings = world.settings
    val forecast = world.forecast
        ?.takeIf { it.isFor(settings.weatherPlace, settings.weatherLat, settings.weatherLon, settings.weatherUnits) }
        ?: return Sky.Day
    return WidgetColours.skyFor(forecast.code, forecast.day)
}

/**
 * A colour widget's card: [fill]'s colours from top to foot, standing on a soft shadow, with what is
 * drawn on it kept inside its corners.
 */
private fun Modifier.colourCard(fill: WidgetFill): Modifier = this
    .cardShadow(WidgetShape)
    .background(
        if (fill.top == fill.bottom) SolidColor(fill.top) else Brush.verticalGradient(listOf(fill.top, fill.bottom)),
        WidgetShape,
    )
    .clip(WidgetShape)

/**
 * The soft shadow a colour widget stands on, in two parts as a pane of glass's is: a wide, faint one
 * that lifts it off the wallpaper, and a close one under its edge that says it is solid. It is the
 * same in Light and in Dark, as the wallpaper it falls on is.
 */
private fun Modifier.cardShadow(shape: Shape): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val path = android.graphics.Path().apply { addOutline(outline) }
    fun layer(blur: Dp, fall: Dp, alpha: Float) = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.BLACK
        setShadowLayer(blur.toPx(), CARD_SHADOW_FALL.x * fall.toPx(), CARD_SHADOW_FALL.y * fall.toPx(), Color.Black.copy(alpha = alpha).toArgb())
    }
    val shadows = listOf(layer(18.dp, 7.dp, 0.16f), layer(3.dp, 1.5.dp, 0.14f))
    onDrawBehind {
        drawIntoCanvas {
            val canvas = it.nativeCanvas
            canvas.save()
            // Only the shadow is wanted: the card is drawn over where the path itself would be.
            canvas.clipOutPath(path)
            for (paint in shadows) canvas.drawPath(path, paint)
            canvas.restore()
        }
    }
}

/** Which way a card's shadow falls: down and a little to the right, as glass's shadows fall from the light. */
private val CARD_SHADOW_FALL = Offset(0.41f, 0.91f)

/**
 * Words on a colour widget's card: in the card's ink and its quiet shade, with its accent for what can
 * be pressed. Its decoration is in the ink too, as the card itself wears the colour, and nothing on it
 * stands on the wallpaper, so its words need no shadow.
 */
@Composable
private fun WordsOnCard(fill: WidgetFill, content: @Composable () -> Unit) {
    val palette = LocalPalette.current
    val onCard = remember(palette, fill) {
        palette.copy(
            // A palette's words are white when it is dark and Zenith's near-black when it is light,
            // and a card's ink is always one of the two.
            dark = fill.ink.luminance() > 0.5f,
            accent = fill.accent,
            accentAlt = fill.accent,
            ownColour = 0f,
        )
    }
    CompositionLocalProvider(
        LocalPalette provides onCard,
        LocalWidgetFill provides fill,
        LocalOnWallpaper provides false,
        content = content,
    )
}

/**
 * Words on a widget's glass where that glass stands on the wallpaper ([LocalOnWallpaper]): white,
 * with the soft shadow the names under the icons have, in Light as in Dark, since the glass is the
 * same clear glass in both. Anywhere else, a widget shown in a sheet say, its words follow the mode.
 */
@Composable
private fun WordsOnWallpaper(content: @Composable () -> Unit) {
    if (LocalOnWallpaper.current) {
        CompositionLocalProvider(
            LocalPalette provides LocalPalette.current.copy(dark = true),
            LocalTextStyle provides LocalTextStyle.current.merge(
                TextStyle(shadow = labelShadow(onWallpaper = true, legacyShadow = false, density = LocalDensity.current.density))
            ),
            content = content,
        )
    } else {
        content()
    }
}

/** What a widget shows, the same whether it sits on a pane or straight on the wallpaper. */
@Composable
private fun WidgetBody(
    kind: String,
    form: WidgetForm,
    width: Dp,
    height: Dp,
    userName: String,
    note: String,
    onNoteChange: (String) -> Unit,
    toggles: List<String>,
) {
    when (kind) {
        "clock" -> ClockWidget(form, width, height, userName)
        "battery" -> PowerWidget(form, width, height)
        "calendar" -> CalendarWidget(form, width, height)
        "notes" -> NotesWidget(form, width, height, note, onNoteChange)
        "toggles" -> TogglesWidget(form, toggles, width, height)
        "media" -> MusicWidget(form, width, height)
        "world" -> WorldWidget(form, width, height)
        "countdown" -> CountdownWidget(form, width, height)
        "photo" -> PhotoWidget(form, width, height)
        "todo" -> TodoWidget(form, width, height)
        "aura" -> StreakWidget(form, width, height)
        "weather" -> WeatherWidget(form, width, height)
        else -> Box(Modifier.fillMaxSize())
    }
}

// ---------------------------------------------------------------------- shared pieces

/**
 * Big numbers set with no font padding, so they sit exactly where they are placed, in the font
 * numbers are set in. A number font with a weight of its own (Thin) sets every number in it,
 * unless [keepWeight]: glass letters need the body they ask for.
 */
@Composable
internal fun tight(size: TextUnit, weight: FontWeight = FontWeight.Light, keepWeight: Boolean = false): TextStyle {
    val numbers = LocalNumberFont.current
    return TextStyle(
        fontFamily = numbers.family,
        fontSize = size,
        lineHeight = size,
        fontWeight = if (keepWeight) weight else numbers.weight ?: weight,
        letterSpacing = (-0.02 * size.value).sp,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
        lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.Both),
    )
}

/**
 * The colour of a widget's second line: the date under the time, "On battery", "25 days left".
 * Stronger than the muted grey a page uses, because a widget is read at a glance and over a
 * wallpaper, and stronger still with no pane behind it. On a colour widget's card it is the card's
 * own quiet shade, as strong as that card needs it to be to read.
 */
@Composable
internal fun quiet(): Color =
    LocalWidgetFill.current?.quiet ?: LocalPalette.current.onGlass.copy(alpha = if (LocalBare.current) 0.94f else 0.82f)

/**
 * The shadow of words a widget sets in a style of its own (a time, a toggle's name, a to-do), which
 * takes nothing from the words around it: on glass on the wallpaper, the soft shadow of the names
 * under the icons, which WordsOnWallpaper gives every other word there, and none on a colour card or
 * a page. A "No glass" widget's own styles are left as they were, with none.
 */
@Composable
internal fun wordShadow(): Shadow? = if (LocalBare.current) null else LocalTextStyle.current.shadow

/**
 * A small symbol in a widget: glass in a "No glass" widget; on a pane, the words' ink, or the
 * widget's own colour when it has one.
 */
@Composable
internal fun Symbol(icon: ImageVector, size: Dp, modifier: Modifier = Modifier, tint: Color = Color.Unspecified) {
    val bare = LocalBare.current
    Icon(
        icon,
        contentDescription = null,
        tint = when {
            tint.isSpecified -> tint
            bare -> Color.White
            else -> LocalPalette.current.let { it.decor(it.accent) }
        },
        modifier = modifier
            .size(size)
            .then(if (bare) Modifier.glassInk(bevel = size * 0.09f) else Modifier),
    )
}

@Composable
internal fun rememberNow(): LocalDateTime {
    val now by produceState(initialValue = LocalDateTime.now()) {
        while (true) {
            value = LocalDateTime.now()
            val millisToNextSecond = 1_000L - (System.currentTimeMillis() % 1_000L)
            delay(millisToNextSecond)
        }
    }
    return now
}

/**
 * Today's date, which turns over at midnight. It waits for the turn of the day, and looks again
 * whenever the home screen comes back, since a phone asleep over midnight does not count the hours
 * it sleeps and would wake the wait late; and at least every hour, so a clock or a time zone changed
 * by hand is caught too. It changes once a day, so nothing reading it is drawn again in between.
 */
@Composable
internal fun rememberToday(): LocalDate {
    var today by remember { mutableStateOf(LocalDate.now()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { today = LocalDate.now() }
    LaunchedEffect(Unit) {
        while (true) {
            val now = ZonedDateTime.now()
            val midnight = now.toLocalDate().plusDays(1).atStartOfDay(now.zone)
            // A moment past midnight, so the date has surely moved on by then.
            delay(minOf(Duration.between(now, midnight).toMillis() + 1_000L, 60 * 60_000L))
            today = LocalDate.now()
        }
    }
    return today
}

/**
 * [items] one under another, [gap] apart, as many of them as there is room for and no more than
 * [most]: none is cut off at the foot. When some are left out and there is a [more], it says under
 * the rest how many. Measured rather than counted, so larger text in the phone's settings shows
 * fewer rows instead of clipped ones.
 */
@Composable
internal fun <T> AsManyAsFit(
    items: List<T>,
    modifier: Modifier = Modifier,
    gap: Dp = 0.dp,
    most: Int = Int.MAX_VALUE,
    more: (@Composable (left: Int) -> Unit)? = null,
    row: @Composable (T) -> Unit,
) {
    SubcomposeLayout(modifier) { constraints ->
        // Each row at its own height, however little room there is, so one that would not fit whole
        // is known, and left out.
        val loose = constraints.copy(minWidth = 0, minHeight = 0, maxHeight = Constraints.Infinity)
        val space = gap.roundToPx()
        val rows = items.take(most.coerceAtLeast(0)).mapIndexed { index, item ->
            subcompose(index) { row(item) }.map { it.measure(loose) }
        }
        fun tall(slot: List<Placeable>): Int = slot.maxOfOrNull { it.height } ?: 0
        fun stack(count: Int): Int = (0 until count).sumOf { tall(rows[it]) } + space * (count - 1).coerceAtLeast(0)
        fun under(count: Int, footer: Int): Int = stack(count) + (if (count > 0) space else 0) + footer
        val room = constraints.maxHeight
        var shown = rows.size
        while (shown > 0 && stack(shown) > room) shown--
        var footer: List<Placeable> = emptyList()
        if (more != null && shown < items.size) {
            // The line saying how many more is as tall whatever the number, so it is measured once
            // to make room for it, and then written with the number that is left out.
            val probe = tall(subcompose(MORE_PROBE) { more(items.size) }.map { it.measure(loose) })
            while (shown > 0 && under(shown, probe) > room) shown--
            if (under(shown, probe) <= room) {
                footer = subcompose(MORE_LINE) { more(items.size - shown) }.map { it.measure(loose) }
            }
        }
        val used = if (footer.isEmpty()) stack(shown) else under(shown, tall(footer))
        val width = if (constraints.hasBoundedWidth) {
            constraints.maxWidth
        } else {
            (rows.take(shown).flatten() + footer).maxOfOrNull { it.width } ?: 0
        }
        layout(width.coerceAtLeast(constraints.minWidth), used.coerceIn(constraints.minHeight, constraints.maxHeight)) {
            var y = 0
            for (index in 0 until shown) {
                rows[index].forEach { it.placeRelative(0, y) }
                y += tall(rows[index]) + space
            }
            footer.forEach { it.placeRelative(0, y) }
        }
    }
}

/** The slots [AsManyAsFit] writes its "more" line in: once to measure it, and once as it is shown. */
private const val MORE_PROBE = "moreProbe"
private const val MORE_LINE = "moreLine"

// ---------------------------------------------------------------------- clock

/**
 * How full a gauge is drawn on its way to [fraction]. It springs there: up from empty when it
 * first appears, and from wherever it was whenever the charge changes. Every shape the charge is
 * shown as runs on this, so they all fill the same way. With movement reduced it is simply there.
 */
@Composable
internal fun rememberFilled(fraction: Float): Animatable<Float, AnimationVector1D> {
    val reduced = LocalMotionLevel.current.reduced
    val filled = remember { Animatable(if (reduced) fraction else 0f) }
    LaunchedEffect(fraction, reduced) {
        if (reduced) filled.snapTo(fraction) else filled.animateTo(fraction, spring(dampingRatio = 0.62f, stiffness = 90f))
    }
    return filled
}

/**
 * Where on screen the home-screen text field you are typing in sits, or null when you are not
 * typing. The home screen reads it to end typing when a touch lands anywhere else. There is only
 * ever one home screen, so a single shared instance is enough.
 */
class TypingArea {
    var bounds by mutableStateOf<Rect?>(null)
}

val HomeTypingArea = TypingArea()

/** The largest a named tile gets, however much room the widget has. */
private val MAX_TILE_WIDTH = 168.dp
private val MAX_TILE_HEIGHT = 118.dp

/**
 * How much of a round toggle its symbol fills. Testers found the symbols hard to tell apart at
 * a glance at 46%; 54% is about what Android's own quick settings give theirs.
 */
private const val TOGGLE_SYMBOL = 0.54f

/** The same for a "No glass" toggle, where the symbol is the whole button. */
private const val BARE_TOGGLE_SYMBOL = 0.8f

/** The name under a round toggle: the size of the label under an app, and a little heavier. */
private val TOGGLE_NAME = 12.sp
private val TOGGLE_NAME_LINE = 15.sp
private val TOGGLE_NAME_STYLE = TextStyle(
    fontSize = TOGGLE_NAME,
    lineHeight = TOGGLE_NAME_LINE,
    fontWeight = FontWeight.Medium,
    textAlign = TextAlign.Center,
)

/** Between a round toggle and its name. */
private val TOGGLE_NAME_GAP = 5.dp

/** The smallest a round toggle may get to make room for a second line of name under it. */
private val TOGGLE_NAMED_BUTTON = 38.dp

/** One control the Quick Toggles widget can show. */
@Immutable
private data class ToggleOption(val id: String, val label: String, val detail: String, val icon: ImageVector)

/**
 * Everything the Quick Toggles widget can hold. A launcher is not allowed to flip most of these by
 * itself, so apart from the torch each one opens the phone's own switch for it — the quick panel
 * where Android has one, otherwise the settings page.
 */
private val TOGGLE_OPTIONS = listOf(
    ToggleOption("torch", "Torch", "Turns the flashlight on and off", Icons.Rounded.FlashlightOn),
    ToggleOption("wifi", "Wi-Fi", "Opens the Wi-Fi panel", Icons.Rounded.Wifi),
    ToggleOption("bluetooth", "Bluetooth", "Opens Bluetooth settings", Icons.Rounded.Bluetooth),
    ToggleOption("data", "Mobile data", "Opens the internet panel", Icons.Rounded.SignalCellularAlt),
    ToggleOption("airplane", "Airplane mode", "Opens airplane mode settings", Icons.Rounded.AirplanemodeActive),
    ToggleOption("focus", "Do Not Disturb", "Opens Do Not Disturb", Icons.Rounded.DoNotDisturbOn),
    ToggleOption("location", "Location", "Opens location settings", Icons.Rounded.LocationOn),
    ToggleOption("hotspot", "Hotspot", "Opens hotspot settings", Icons.Rounded.WifiTethering),
    ToggleOption("saver", "Battery saver", "Opens battery saver settings", Icons.Rounded.BatterySaver),
    ToggleOption("volume", "Volume", "Opens the volume panel", Icons.Rounded.GraphicEq),
    ToggleOption("nfc", "NFC", "Opens NFC settings", Icons.Rounded.Nfc),
    ToggleOption("display", "Display", "Opens display settings", Icons.Rounded.BrightnessMedium),
)

@Immutable
private data class Toggle(val option: ToggleOption, val active: Boolean, val onClick: () -> Unit) {
    val label: String get() = option.label
    val icon: ImageVector get() = option.icon
}

@Composable
private fun TogglesWidget(form: WidgetForm, chosen: List<String>, width: Dp, height: Dp) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    LaunchedEffect(Unit) { com.gabriel.zenithui.data.Torch.watch(context) }
    val torchOn by com.gabriel.zenithui.data.Torch.on.collectAsState()

    fun actionFor(id: String): () -> Unit = when (id) {
        "torch" -> { { com.gabriel.zenithui.data.Torch.toggle(context) } }
        "wifi" -> { { context.startActivityIntent(Settings.Panel.ACTION_WIFI, Settings.ACTION_WIFI_SETTINGS) } }
        "bluetooth" -> { { context.startActivityIntent(Settings.ACTION_BLUETOOTH_SETTINGS) } }
        "data" -> { { context.startActivityIntent(Settings.Panel.ACTION_INTERNET_CONNECTIVITY, Settings.ACTION_WIRELESS_SETTINGS) } }
        "airplane" -> { { context.startActivityIntent(Settings.ACTION_AIRPLANE_MODE_SETTINGS) } }
        "focus" -> { { context.startActivityIntent("android.settings.ZEN_MODE_SETTINGS", Settings.ACTION_SOUND_SETTINGS) } }
        "location" -> { { context.startActivityIntent(Settings.ACTION_LOCATION_SOURCE_SETTINGS) } }
        "hotspot" -> { { context.startActivityIntent("android.settings.TETHER_SETTINGS", Settings.ACTION_WIRELESS_SETTINGS) } }
        "saver" -> { { context.startActivityIntent(Settings.ACTION_BATTERY_SAVER_SETTINGS) } }
        "volume" -> { { context.startActivityIntent(Settings.Panel.ACTION_VOLUME, Settings.ACTION_SOUND_SETTINGS) } }
        "nfc" -> { { context.startActivityIntent(Settings.ACTION_NFC_SETTINGS, Settings.ACTION_WIRELESS_SETTINGS) } }
        else -> { { context.startActivityIntent(Settings.ACTION_DISPLAY_SETTINGS) } }
    }

    val options = chosen
        .mapNotNull { id -> TOGGLE_OPTIONS.firstOrNull { it.id == id } }
        .ifEmpty { TOGGLE_OPTIONS.take(1) }

    // Lay the chosen controls out on whichever grid gives each of them the most room at this size,
    // as many as can still be pressed: see ToggleGrid, which the widget showcase reads as well.
    val grid = remember(width, height, options.size) { ToggleGrid.of(width.value, height.value, options.size) }
    val pad = if (form == WidgetForm.Mini) 6.dp else 12.dp
    val toggles = options.take(grid.shown)
        .map { option -> Toggle(option, option.id == "torch" && torchOn, actionFor(option.id)) }
    val columns = grid.columns
    val cellWidth = grid.cellWidth.dp
    val cellHeight = grid.cellHeight.dp
    val tiles = grid.style == ToggleGrid.Style.Tiles
    val list = grid.style == ToggleGrid.Style.List
    val named = grid.style == ToggleGrid.Style.Named

    // A name under a button is set like the label under an app. The longest decides for them all,
    // so every button in the widget stays level: see ToggleNames for the rule.
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val labels = toggles.map { it.label }
    val nameLine = with(density) { TOGGLE_NAME_LINE.toDp() }
    // Measured in the font they are drawn in, or a wider font would not fit where it was measured to.
    val nameFont = LocalZenithFont.current
    val names = remember(labels, cellWidth, cellHeight, density.density, density.fontScale, nameFont) {
        fun widthOf(text: String) = with(density) {
            measurer.measure(text, TOGGLE_NAME_STYLE.copy(fontFamily = nameFont), softWrap = false, maxLines = 1).size.width.toDp().value
        }
        ToggleNames.fit(
            widestName = labels.maxOf { widthOf(it) },
            widestWord = labels.flatMap { it.split(' ') }.maxOf { widthOf(it) },
            cell = (cellWidth - 2.dp).value,
            roomForTwo = (cellHeight - TOGGLE_NAME_GAP - nameLine * 2) * 0.9f >= TOGGLE_NAMED_BUTTON,
        )
    }
    val nameStyle = TOGGLE_NAME_STYLE.copy(
        fontFamily = nameFont,
        fontSize = TOGGLE_NAME * names.scale,
        lineHeight = TOGGLE_NAME_LINE * names.scale,
        color = palette.onGlass,
        shadow = wordShadow(),
    )
    // Buttons never grow past a comfortable size.
    val button = when {
        list -> minOf(cellHeight * 0.72f, 42.dp)
        named -> minOf(cellWidth * 0.6f, (cellHeight - TOGGLE_NAME_GAP - nameLine * names.scale * names.lines) * 0.9f, 46.dp)
        else -> minOf(cellWidth, cellHeight) * 0.74f
    }.coerceAtMost(46.dp)

    Column(Modifier.fillMaxSize().padding(pad)) {
        toggles.chunked(columns).forEach { line ->
            Row(
                Modifier.weight(1f).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                line.forEach { toggle ->
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = if (list) Alignment.CenterStart else Alignment.Center) {
                        when {
                            tiles -> ToggleTile(
                                toggle,
                                Modifier
                                    .sizeIn(maxWidth = MAX_TILE_WIDTH, maxHeight = MAX_TILE_HEIGHT)
                                    .fillMaxSize()
                                    .padding(4.dp),
                                // A short tile has room for the name only: its second line would
                                // be cut through the middle.
                                detail = grid.detail,
                            )
                            list -> Row(verticalAlignment = Alignment.CenterVertically) {
                                ToggleButton(toggle, button)
                                Spacer(Modifier.width(10.dp))
                                Text(toggle.label, color = palette.onGlass, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            named -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                ToggleButton(toggle, button)
                                Spacer(Modifier.height(TOGGLE_NAME_GAP))
                                // Every name takes the same number of lines, so the buttons line up.
                                Text(
                                    toggle.label,
                                    style = nameStyle,
                                    minLines = names.lines,
                                    maxLines = names.lines,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            else -> ToggleButton(toggle, button)
                        }
                    }
                }
                // Keep the last line's controls in their columns rather than spreading them out.
                repeat(columns - line.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

/**
 * Where you choose what the Quick Toggles widget shows. The widget rearranges itself to fit
 * whatever it holds, at whatever size it is, so any mix works.
 */
@Composable
fun TogglesSheet(
    visible: Boolean,
    selected: List<String>,
    onChange: (List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    ZenithSheet(visible = visible, onDismiss = onDismiss, title = "Quick Toggles") {
        Text(
            "Choose what the widget shows. It rearranges itself to fit, whatever its size.",
            color = palette.onGlassMuted,
            fontSize = 12.sp,
        )
        Spacer(Modifier.height(8.dp))
        Column(Modifier.verticalScroll(rememberScrollState())) {
            TOGGLE_OPTIONS.forEach { option ->
                val on = option.id in selected
                ToggleChoice(option, on) {
                    val next = if (on) selected - option.id else selected + option.id
                    // Always at least one control, and always in the catalogue's order.
                    if (next.isNotEmpty()) onChange(TOGGLE_OPTIONS.map { it.id }.filter { it in next })
                    next.isNotEmpty()
                }
            }
        }
    }
}

/**
 * One control the widget could show. Chosen, its disc takes the accent, and gives a small bounce
 * as it does; the switch beside it is the one Settings has, worked by the whole row. [onFlip]
 * answers whether it was flipped: the last one left cannot be taken away, and shakes its head.
 */
@Composable
private fun ToggleChoice(option: ToggleOption, on: Boolean, onFlip: () -> Boolean) {
    val palette = LocalPalette.current
    val tick = rememberTick()
    val reduced = LocalMotionLevel.current.reduced
    val touch = remember { MutableInteractionSource() }
    val pressed by touch.collectIsPressedAsState()
    var refused by remember { mutableIntStateOf(0) }
    val disc by animateColorAsState(
        targetValue = if (on) palette.accent.copy(alpha = 0.9f) else palette.onGlass.copy(alpha = 0.10f),
        animationSpec = tween(260),
        label = "choiceDisc",
    )
    val ink by animateColorAsState(if (on) inkOnTint(palette.accent) else palette.onGlass, tween(260), label = "choiceInk")
    // Not as the list arrives: only when this one is chosen or let go while it is on show.
    val bounce = remember { Animatable(1f) }
    val shownAs = remember { booleanArrayOf(on) }
    LaunchedEffect(on) {
        if (shownAs[0] == on) return@LaunchedEffect
        shownAs[0] = on
        if (reduced) return@LaunchedEffect
        bounce.snapTo(0.8f)
        bounce.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 420f))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .shake(refused)
            .clickable(interactionSource = touch, indication = null) {
                if (onFlip()) tick() else refused++
            }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(38.dp)
                .graphicsLayer {
                    scaleX = bounce.value
                    scaleY = bounce.value
                }
                .clip(RoundedCornerShape(percent = 50))
                .background(disc),
            contentAlignment = Alignment.Center,
        ) {
            Icon(option.icon, contentDescription = null, tint = ink, modifier = Modifier.size(21.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(option.label, color = palette.onGlass, fontSize = 15.sp)
            Text(option.detail, color = palette.onGlassMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.width(12.dp))
        OnOffSwitch(on, pressed)
    }
}

private fun Context.startActivityIntent(action: String, fallback: String? = null) {
    runCatching { startActivity(Intent(action).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        .onFailure {
            if (fallback != null) runCatching { startActivity(Intent(fallback).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
}

@Composable
private fun ToggleButton(toggle: Toggle, size: Dp) {
    val palette = LocalPalette.current
    if (LocalBare.current) {
        // No disc: the symbol alone is the button, clear glass when off and the accent's glass when on.
        val ink by animateColorAsState(
            targetValue = if (toggle.active) palette.accent else Color.White,
            animationSpec = tween(420),
            label = "toggleInk",
        )
        Box(
            Modifier
                .size(size)
                .glassPress(GlassMotion.Lean)
                .clip(RoundedCornerShape(percent = 50))
                .clickable(onClick = toggle.onClick),
            contentAlignment = Alignment.Center,
        ) {
            Symbol(toggle.icon, size * BARE_TOGGLE_SYMBOL, tint = ink)
        }
        return
    }
    val background by animateColorAsState(
        targetValue = if (toggle.active) palette.accent.copy(alpha = 0.9f) else palette.onGlass.copy(alpha = 0.10f),
        animationSpec = tween(420),
        label = "toggleBg",
    )
    val tint by animateColorAsState(
        targetValue = if (toggle.active) inkOnTint(palette.accent) else palette.onGlass,
        animationSpec = tween(420),
        label = "toggleTint",
    )
    // On, it squares up a little; off, it is a circle again. The shape says it as well as the colour.
    val corner by animateIntAsState(
        targetValue = if (toggle.active) 34 else 50,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 260f),
        label = "toggleShape",
    )
    Box(
        Modifier
            .size(size)
            // Leans towards the finger, and keeps squaring up when it is on.
            .glassPress(GlassMotion.Lean)
            .clip(RoundedCornerShape(percent = corner.coerceIn(0, 50)))
            .background(background)
            .clickable(onClick = toggle.onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(toggle.icon, contentDescription = toggle.label, tint = tint, modifier = Modifier.size(size * TOGGLE_SYMBOL))
    }
}

@Composable
private fun ToggleTile(toggle: Toggle, modifier: Modifier, detail: Boolean) {
    val palette = LocalPalette.current
    if (LocalBare.current) {
        val ink by animateColorAsState(
            targetValue = if (toggle.active) palette.accent else Color.White,
            animationSpec = tween(420),
            label = "tileInk",
        )
        Column(
            modifier
                .pressBounce(0.94f)
                .clip(RoundedCornerShape(20.dp))
                .clickable(onClick = toggle.onClick)
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Symbol(toggle.icon, 34.dp, tint = ink)
            TileText(toggle, palette.onGlass, detail)
        }
        return
    }
    val background by animateColorAsState(
        targetValue = if (toggle.active) palette.accent.copy(alpha = 0.9f) else palette.onGlass.copy(alpha = 0.08f),
        animationSpec = tween(420),
        label = "tileBg",
    )
    val ink = if (toggle.active) inkOnTint(palette.accent) else palette.onGlass
    Column(
        modifier
            .pressBounce(0.94f)
            .clip(RoundedCornerShape(20.dp))
            .background(background)
            .clickable(onClick = toggle.onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Icon(toggle.icon, contentDescription = null, tint = ink, modifier = Modifier.size(26.dp))
        TileText(toggle, ink, detail)
    }
}

@Composable
private fun ColumnScope.TileText(toggle: Toggle, ink: Color, detail: Boolean) {
    Text(toggle.label, color = ink, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    if (!detail) return
    Text(
        text = if (toggle.option.id == "torch") (if (toggle.active) "On" else "Off") else "Open",
        color = ink.copy(alpha = 0.8f),
        fontSize = 12.sp,
        maxLines = 1,
    )
}

// ---------------------------------------------------------------------- search

/**
 * A search widget in a layout saved before Search left the page: until the layout is brought up to
 * date ([com.gabriel.zenithui.data.LayoutTidy]) it is drawn as it was, a bar, or a round button
 * where it is too narrow for one.
 */
@Composable
private fun SearchWidget(onSearch: () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Too narrow for a bar: a round search button instead.
        if (maxWidth < 150.dp) SearchButton(onSearch) else SearchBar(onSearch)
    }
}

@Composable
private fun SearchButton(onSearch: () -> Unit) {
    val palette = LocalPalette.current
    Box(
        Modifier
            .size(SEARCH_BAR_HEIGHT)
            .glass(
                RoundedCornerShape(percent = 50),
                strength = 1.1f,
                material = GlassMaterial.Clear,
                interactive = true,
                motion = GlassMotion.Lean,
            )
            .clickable(onClick = onSearch),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = "Search", tint = palette.onGlass, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun SearchBar(onSearch: () -> Unit) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(SEARCH_BAR_HEIGHT)
            .pressBounce(0.97f)
            .glass(ZenithShapes.pill, strength = 1.1f, interactive = true)
            .clickable(onClick = onSearch)
            .padding(start = 18.dp, end = SEARCH_BAR_INSET),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.onGlassMuted, modifier = Modifier.size(18.dp))
        Spacer(Modifier.size(10.dp))
        Text(
            text = "Search apps or the web",
            color = palette.onGlassMuted,
            fontSize = 14.sp,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Box(
            Modifier
                // Sized to the bar so the circle sits flush inside its rounded end.
                .size(SEARCH_BAR_HEIGHT - SEARCH_BAR_INSET * 2)
                .pressBounce(0.88f)
                .clip(RoundedCornerShape(percent = 50))
                .background(palette.accent.copy(alpha = 0.9f))
                .clickable { openWebSearch(context) },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.TravelExplore,
                contentDescription = "Search the web",
                tint = inkOnTint(palette.accent),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

/** The visible height of the search bar; its cell only reacts to touches inside this. */
val SEARCH_BAR_HEIGHT = 50.dp

/** Gap between the bar's edge and the round web-search button. */
private val SEARCH_BAR_INSET = 6.dp

/** Opens the device search app ready for a query, or Google in a browser. */
private fun openWebSearch(context: Context) {
    val search = Intent(Intent.ACTION_WEB_SEARCH).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    val browser = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com"))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(search) }
        .onFailure { runCatching { context.startActivity(browser) } }
}
