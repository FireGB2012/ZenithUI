package com.gabriel.zenithui.ui.connect

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.connect.ConnectSetup
import com.gabriel.zenithui.data.connect.ConnectWall
import com.gabriel.zenithui.data.connect.ConnectWalls
import com.gabriel.zenithui.data.connect.SetupKind
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// How a setup is shown without its real files: its wallpaper drawn from its glows, and a little
// home screen on it in its own colours, as the prototype's previews were.

/** A setup's wallpaper behind whatever this is on, drawn from its glows once for each size. */
internal fun Modifier.wall(id: String): Modifier = wall(ConnectWalls.byId(id))

internal fun Modifier.wall(wall: ConnectWall?): Modifier = drawWithCache {
    val painters = wallPainters(wall, size)
    onDrawBehind { painters.forEach { it(this) } }
}

/** What draws [wall] at [size], worked out once: the colour or the two under it, then each glow, the first on top. */
private fun wallPainters(wall: ConnectWall?, size: Size): List<DrawScope.() -> Unit> {
    if (wall == null) return listOf({ drawRect(Color(0xFF15151C)) })
    val base: DrawScope.() -> Unit = if (wall.base.size == 1) {
        val colour = Color(wall.base[0])
        ({ drawRect(colour) })
    } else {
        val brush = Brush.verticalGradient(wall.base.map { Color(it) })
        ({ drawRect(brush) })
    }
    val glows = wall.glows.asReversed().map { glow ->
        val colour = Color(glow.colour)
        val cx = glow.x * size.width
        val cy = glow.y * size.height
        val rx = (glow.rx * size.width).coerceAtLeast(1f)
        val ry = (glow.ry * size.height).coerceAtLeast(1f)
        val brush = Brush.radialGradient(0f to colour, glow.reach to colour.copy(alpha = 0f), center = Offset(cx, cy), radius = ry)
        // A circle as tall as the ellipse, stretched sideways about its middle to the ellipse's width.
        val k = ry / rx
        val painter: DrawScope.() -> Unit = {
            withTransform({ scale(scaleX = rx / ry, scaleY = 1f, pivot = Offset(cx, cy)) }) {
                drawRect(brush, topLeft = Offset(cx - cx * k - 1f, 0f), size = Size(size.width * k + 2f, size.height))
            }
        }
        painter
    }
    return listOf(base) + glows
}

/** The colours a little home screen is drawn in on a dark wallpaper, or on a pale one. */
@Immutable
internal class Tone(
    val ink: Color,
    val sub: Color,
    val glass: Color,
    val line: Color,
    val dot: Color,
    val iconBg: Color,
    val iconInk: Color,
) {
    companion object {
        val Dark = Tone(
            ink = Color(0xFFF6F6F9),
            sub = Color(0xBFF6F6F9),
            glass = Color.White.copy(alpha = 0.17f),
            line = Color.White.copy(alpha = 0.24f),
            dot = Color.White.copy(alpha = 0.6f),
            iconBg = Color.White.copy(alpha = 0.14f),
            iconInk = Color(0xFFF6F6F9),
        )
        val Light = Tone(
            ink = Color(0xFF16161E),
            sub = Color(0x9916161E),
            glass = Color(0x1416161E),
            line = Color(0x1F16161E),
            dot = Color(0x6616161E),
            iconBg = Color(0xFF1B1B22),
            iconInk = Color(0xFFEDEBE6),
        )

        fun of(dark: Boolean) = if (dark) Dark else Light
    }
}

/** Today as the previews' clocks show it under the time: "Thu 19 Nov". */
internal fun shortDate(): String = LocalDate.now().format(DateTimeFormatter.ofPattern("EEE d MMM", Locale.getDefault()))

/** The icons an icon pack's preview shows, line drawn. */
private val GLYPHS = listOf(
    Icons.Outlined.Phone, Icons.Outlined.ChatBubbleOutline, Icons.Outlined.CameraAlt, Icons.Outlined.MusicNote,
    Icons.Outlined.Place, Icons.Outlined.MailOutline, Icons.Outlined.Schedule, Icons.Outlined.PlayArrow,
    Icons.Outlined.Cloud, Icons.Outlined.FavoriteBorder, Icons.Outlined.Explore, Icons.Outlined.Tune,
)

/**
 * A setup as a little home screen on its wallpaper: a clock, a widget, its icons and a dock for a
 * full setup; its widgets for a widget preset; a page of its icons for an icon pack. [hero] is the
 * tighter one inside the setup of the week's phone.
 */
@Composable
internal fun SetupPreview(setup: ConnectSetup, modifier: Modifier = Modifier, hero: Boolean = false) {
    val tone = Tone.of(setup.dark)
    Box(modifier.wall(setup.wall)) {
        when (setup.kind) {
            SetupKind.SETUP -> HomePreview(setup, tone, hero)
            SetupKind.WIDGETS -> WidgetsPreview(setup, tone)
            SetupKind.ICONS -> IconsPreview(setup, tone)
        }
    }
}

@Composable
private fun HomePreview(setup: ConnectSetup, tone: Tone, hero: Boolean) {
    val date = remember { shortDate() }
    Column(
        Modifier
            .fillMaxSize()
            .padding(start = if (hero) 10.dp else 12.dp, end = if (hero) 10.dp else 12.dp, top = if (hero) 18.dp else 40.dp, bottom = if (hero) 8.dp else 10.dp),
        verticalArrangement = Arrangement.spacedBy(if (hero) 7.dp else 8.dp),
    ) {
        Column {
            Text(setup.clock, color = tone.ink, fontSize = if (hero) 20.sp else 24.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.4).sp, lineHeight = if (hero) 20.sp else 24.sp)
            Text(date, color = tone.sub, fontSize = if (hero) 8.sp else 9.sp, lineHeight = 10.sp)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(if (hero) 34.dp else 38.dp)
                .clip(RoundedCornerShape(if (hero) 10.dp else 12.dp))
                .background(tone.glass)
                .border(1.dp, tone.line, RoundedCornerShape(if (hero) 10.dp else 12.dp)),
        )
        IconGrid(columns = 4, count = 8, round = setup.iconRound, gap = if (hero) 7.dp else 8.dp) { _, shape ->
            Box(Modifier.fillMaxSize().clip(shape).background(tone.glass))
        }
        Spacer(Modifier.weight(1f))
        Row(
            Modifier
                .fillMaxWidth()
                .height(if (hero) 24.dp else 28.dp)
                .clip(RoundedCornerShape(50))
                .background(tone.glass)
                .border(1.dp, tone.line, RoundedCornerShape(50))
                .padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val dot = if (hero) 12.dp else 14.dp
            repeat(4) { Box(Modifier.size(dot).clip(RoundedCornerShape(percent = (setup.iconRound * 100).toInt())).background(tone.dot)) }
        }
    }
}

@Composable
private fun WidgetsPreview(setup: ConnectSetup, tone: Tone) {
    val date = remember { shortDate() }
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.fillMaxSize().padding(start = 10.dp, end = 10.dp, top = 38.dp, bottom = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(shape)
                .background(tone.glass)
                .border(1.dp, tone.line, shape)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(setup.clock, color = tone.ink, fontSize = 20.sp, fontWeight = FontWeight.Medium, letterSpacing = (-0.4).sp, lineHeight = 20.sp)
            Text(date, color = tone.sub, fontSize = 9.sp, lineHeight = 10.sp)
        }
        Row(Modifier.fillMaxWidth().height(42.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(tone.glass)
                    .border(1.dp, tone.line, shape)
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Box(Modifier.size(11.dp).clip(CircleShape).background(tone.ink.copy(alpha = 0.85f)))
                Text("14°", color = tone.ink, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 12.sp)
            }
            Row(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(shape)
                    .background(tone.glass)
                    .border(1.dp, tone.line, shape)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                listOf(10, 18, 13, 22, 8).forEach { h ->
                    Box(Modifier.width(4.dp).height(h.dp).clip(RoundedCornerShape(2.dp)).background(tone.ink.copy(alpha = 0.8f)))
                }
            }
        }
    }
}

@Composable
private fun IconsPreview(setup: ConnectSetup, tone: Tone) {
    Box(Modifier.fillMaxSize().padding(start = 12.dp, end = 12.dp, top = 40.dp, bottom = 12.dp)) {
        IconGrid(columns = 4, count = GLYPHS.size, round = setup.iconRound, gap = 9.dp) { index, shape ->
            Box(Modifier.fillMaxSize().clip(shape).background(tone.iconBg), contentAlignment = Alignment.Center) {
                Icon(GLYPHS[index], contentDescription = null, tint = tone.iconInk, modifier = Modifier.fillMaxSize(0.5f))
            }
        }
    }
}

/** [count] square icons, [columns] across with [gap] between, rounded by [round] of their side. */
@Composable
private fun IconGrid(
    columns: Int,
    count: Int,
    round: Float,
    gap: Dp,
    icon: @Composable androidx.compose.foundation.layout.BoxScope.(index: Int, shape: androidx.compose.ui.graphics.Shape) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val side = (maxWidth - gap * (columns - 1)) / columns
        val shape = RoundedCornerShape(side * round.coerceIn(0f, 0.5f))
        Column(verticalArrangement = Arrangement.spacedBy(gap)) {
            (0 until count).chunked(columns).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                    row.forEach { index ->
                        Box(Modifier.size(side).aspectRatio(1f)) { icon(index, shape) }
                    }
                }
            }
        }
    }
}

/** A phone in its black frame with [setup]'s home screen on it. */
@Composable
internal fun MiniPhone(setup: ConnectSetup, modifier: Modifier = Modifier, hero: Boolean = true, corner: Dp = 24.dp) {
    Box(
        modifier
            .clip(RoundedCornerShape(corner))
            .background(Color(0xFF07070D))
            .border(1.5.dp, Color.White.copy(alpha = 0.16f), RoundedCornerShape(corner))
            .padding(4.dp),
    ) {
        SetupPreview(setup, Modifier.fillMaxSize().clip(RoundedCornerShape(corner - 4.dp)), hero = hero)
    }
}

/** The capsule a card shows what kind of setup it is in, over its preview. */
@Composable
internal fun KindChip(kind: SetupKind, modifier: Modifier = Modifier) {
    Box(
        modifier
            .height(22.dp)
            .clip(RoundedCornerShape(50))
            .background(Color(0x8C0A0B10))
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(kind.label.uppercase(), color = Color(0xFFF6F6F9), fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.8.sp, maxLines = 1)
    }
}

/** A row of a setup's five colours. */
@Composable
internal fun Swatches(palette: List<Long>, modifier: Modifier = Modifier, size: Dp = 28.dp, shown: (Int) -> Float = { 1f }) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        palette.forEachIndexed { index, colour ->
            Canvas(Modifier.size(size)) {
                val s = shown(index).coerceIn(0f, 1f)
                if (s <= 0f) return@Canvas
                drawCircle(Color(colour), radius = this.size.minDimension / 2f * (0.6f + 0.4f * s), alpha = s)
                drawCircle(Color.White.copy(alpha = 0.22f * s), radius = this.size.minDimension / 2f * (0.6f + 0.4f * s), style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()))
            }
        }
    }
}
