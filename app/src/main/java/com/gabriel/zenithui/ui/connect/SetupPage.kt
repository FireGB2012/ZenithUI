package com.gabriel.zenithui.ui.connect

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.IconStyles
import com.gabriel.zenithui.data.WidgetLooks
import com.gabriel.zenithui.data.connect.ConnectApply
import com.gabriel.zenithui.data.connect.ConnectCodes
import com.gabriel.zenithui.data.connect.ConnectLayers
import com.gabriel.zenithui.data.connect.ConnectSetup
import com.gabriel.zenithui.data.connect.Layer
import com.gabriel.zenithui.data.connect.SetupSource
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.theme.LocalOnWallpaper
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.Palettes
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * A setup's own page, come in over the tabs: your home screen wearing it as a phone you can turn
 * with a finger, what it is, what of it goes on (each part a chip to leave out), and Apply, which
 * puts it on Zenith for real. Its code, for one from the server, to copy or send; Share and Delete
 * for a Look of your own.
 */
@Composable
internal fun SetupPage(state: ConnectState, bridge: ConnectBridge) {
    val reduced = LocalMotionLevel.current.reduced
    // Kept while the page goes, so it goes with what was on it.
    var shown by remember { mutableStateOf<ConnectSetup?>(null) }
    val progress = remember { Animatable(0f) }
    val target = state.detail
    LaunchedEffect(target) {
        if (target != null) {
            shown = target
            progress.animateTo(1f, if (reduced) tween(150) else spring(dampingRatio = 0.92f, stiffness = 360f))
        } else {
            progress.animateTo(0f, if (reduced) tween(150) else tween(230, easing = FastOutSlowInEasing))
            shown = null
        }
    }
    val setup = shown ?: return
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = constraints.maxWidth
        Box(
            Modifier
                .fillMaxSize()
                // Moved while it is placed, not on a layer, so its glass stays lined up with the wallpaper.
                .offset { IntOffset(if (reduced) 0 else ((1f - progress.value) * width * 0.3f).roundToInt(), 0) }
                .graphicsLayer { alpha = progress.value.coerceIn(0f, 1f) },
        ) {
            PageBody(setup, state, bridge)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PageBody(setup: ConnectSetup, state: ConnectState, bridge: ConnectBridge) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val scroll = remember(setup.id) { ScrollState(0) }
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val available = remember(setup) { ConnectLayers.available(setup) }
    val on = state.layers intersect available
    val worn = rememberWorn(setup, on, bridge)
    val aspect = phoneAspect(bridge)
    val scope = rememberCoroutineScope()
    val spin = remember(setup.id) { Spin(scope) }
    val look = if (setup.source == SetupSource.MINE) bridge.looks.firstOrNull { it.id == setup.lookId } else null
    // Where the big name is in the page, and how tall: read while drawing, to hand it to the bar.
    val title = remember(setup.id) { arrayOf(Float.NaN to 1f) }
    val underBar = {
        val (y, height) = title[0]
        if (y.isNaN()) 0f else ((scroll.value - y) / height.coerceAtLeast(1f)).coerceIn(0f, 1f)
    }

    // Anything scrolled under the bar at the top darkens it at once, not only the big name.
    val scrolledBy = with(LocalDensity.current) { 48.dp.toPx() }
    val shade = { maxOf((underBar() * 1.5f).coerceAtMost(1f), 0.75f * (scroll.value / scrolledBy).coerceIn(0f, 1f)) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val pageHeight = maxHeight
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(top = top + 66.dp, bottom = bottom + 124.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                // The page's picture, but never so tall that, at rest, its name and what it is are
                // pushed under Apply: on a tall, narrow phone (a Galaxy A55) it gives up a little width.
                val byHeight = (pageHeight - top - bottom - 340.dp) * aspect
                val phoneWidth = minOf((maxWidth * 0.6f).coerceAtMost(290.dp), byHeight).coerceAtLeast(150.dp)
                Box(Modifier.width(phoneWidth).aspectRatio(aspect).turned(spin, reduced)) {
                    SetupPhone(worn, bridge, Modifier.fillMaxSize(), artWidth = 540)
                }
            }
            Spacer(Modifier.height(14.dp))
            Note(
                if (reduced) "Your home screen, wearing it" else "Your home screen, wearing it · drag to turn",
                modifier = Modifier.padding(horizontal = 24.dp),
            )
            Spacer(Modifier.height(20.dp))
            Text(
                setup.name,
                color = palette.onGlass,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = (-0.6).sp,
                lineHeight = 36.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .onPlaced { title[0] = it.positionInParent().y to it.size.height.toFloat() }
                    // Going under the bar at the top, it is gone before the small one there comes
                    // up, so the two are never seen on top of each other.
                    .graphicsLayer { alpha = 1f - (underBar() * 1.8f).coerceAtMost(1f) },
            )
            Spacer(Modifier.height(4.dp))
            Text(whoBy(setup), color = palette.onGlassMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(
                setup.about,
                color = palette.onGlass.copy(alpha = 0.9f),
                fontSize = 15.sp,
                lineHeight = 21.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 28.dp).widthIn(max = 520.dp),
            )

            Spacer(Modifier.height(28.dp))
            SectionHead("What goes on", note = "tap to leave a part out")
            Spacer(Modifier.height(12.dp))
            FlowRow(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Layer.entries.filter { it in available }.forEach { layer ->
                    Chip(layer.label, on = layer in state.layers, onClick = { state.toggle(layer) }, icon = layer.icon())
                }
            }
            PageNotes(setup, on, available, bridge)

            if (setup.code.isNotEmpty()) CodeRow(setup, state, stale = look?.sharedIsOld == true)
            if (look != null) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ConnectButton(
                        when {
                            look.shared.isEmpty() -> "Share"
                            look.sharedIsOld -> "Share again"
                            else -> "Its code"
                        },
                        onClick = { state.shareSaved(bridge, look) },
                        icon = Icons.Rounded.IosShare,
                        modifier = Modifier.weight(1f),
                    )
                    ConnectButton(
                        if (state.deleting == look.id) "Tap again" else "Delete",
                        onClick = { state.delete(bridge, look) },
                        icon = Icons.Rounded.DeleteOutline,
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            Spacer(Modifier.height(28.dp))
            InThisSetup(setup)
        }

        TopBar(setup, underBar, shade, onBack = { state.closeDetail() })
        ApplyBar(setup, on, state, bridge)
    }
}

/** Who a setup is by, under its name. */
private fun whoBy(setup: ConnectSetup): String = when (setup.source) {
    SetupSource.PICK -> "By Zenith · free"
    SetupSource.FEATURED -> "Featured on Zenith's server"
    SetupSource.CODE -> "Shared with you"
    SetupSource.MINE -> "Saved on this phone"
}

private fun Layer.icon(): ImageVector = when (this) {
    Layer.WALLPAPER -> Icons.Rounded.Wallpaper
    Layer.THEME -> Icons.Rounded.Palette
    Layer.ICONS -> Icons.Rounded.Apps
    Layer.WIDGETS -> Icons.Rounded.Widgets
    Layer.LAYOUT -> Icons.Rounded.GridView
    Layer.MOTION -> Icons.Rounded.Animation
}

/** What to know before putting it on, as it stands with what is on: a photo that would go, pages that come with it. */
@Composable
private fun PageNotes(setup: ConnectSetup, on: Set<Layer>, available: Set<Layer>, bridge: ConnectBridge) {
    val ownPhoto = bridge.settings.wallpaperStyle.isEmpty()
    val design = remember(setup) { runCatching { JSONObject(setup.settings).optString("wallpaperStyle") }.getOrDefault("") }
    val theirs = setup.source == SetupSource.FEATURED || setup.source == SetupSource.CODE
    val notes = buildList {
        if (on.isEmpty()) add(false to "Turn on a part of it to apply it.")
        if (Layer.WALLPAPER in available && Layer.WALLPAPER !in on && ownPhoto) {
            add(false to "Wallpaper is off because yours is a photo. Turned on, this design takes its place, and Undo can't bring a photo back.")
        }
        if (ConnectApply.replacesPhoto(setup, on, ownPhoto)) {
            add(true to "This design will take your photo's place, and Undo can't bring the photo back. Keep a copy if you want it.")
        }
        if (Layer.WALLPAPER in on && design.isNotEmpty() && !bridge.owns(design)) {
            add(false to "Its wallpaper is a Zenith Plus design, so yours stays.")
        }
        if (theirs && Layer.LAYOUT in available && Layer.LAYOUT !in on) {
            add(false to "Home screen is off, so your apps stay where they are. Turn it on to take their pages too.")
        }
        if (theirs && Layer.LAYOUT in on) {
            add(false to "Their pages come with it, fitted to your screen. Apps you don't have are left out.")
        }
        if (setup.source == SetupSource.MINE && !bridge.plus) {
            add(false to "Switching to a Look of your own is part of Zenith Plus.")
        }
    }
    if (notes.isEmpty()) return
    val palette = LocalPalette.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        notes.forEach { (warning, text) ->
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    if (warning) Icons.Rounded.WarningAmber else Icons.Rounded.Info,
                    contentDescription = null,
                    tint = if (warning) Color(0xFFFFC53D) else palette.onGlassMuted,
                    modifier = Modifier.padding(top = 1.dp).size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
                Note(text, colour = if (warning) palette.onGlass else palette.onGlassMuted)
            }
        }
    }
}

/** A setup's code, to copy or send to somebody. */
@Composable
private fun CodeRow(setup: ConnectSetup, state: ConnectState, stale: Boolean) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 22.dp)
            .pane(ROW)
            .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Its code", color = palette.onGlassMuted, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Text(setup.shownCode, color = palette.onGlass, fontSize = 21.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.4.sp)
            }
            RoundButton(Icons.Rounded.ContentCopy, "Copy the code", onClick = {
                copyText(context, "Zenith Look", setup.shownCode)
                state.say("Copied ${setup.shownCode}")
            })
            Spacer(Modifier.width(8.dp))
            RoundButton(Icons.AutoMirrored.Rounded.Send, "Send the code", onClick = {
                sendText(context, ConnectCodes.message(setup.name, setup.code), "Send ${setup.name}")
            })
        }
        if (stale) {
            Spacer(Modifier.height(6.dp))
            Note("Saved over since it was shared: the code still gives it as it was. Share again to update it.")
        }
    }
}

/** What the setup is made of, one line each. */
@Composable
private fun InThisSetup(setup: ConnectSetup) {
    val palette = LocalPalette.current
    val rows = remember(setup) { madeOf(setup) }
    SectionHead("In this setup")
    Spacer(Modifier.height(12.dp))
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .pane(CARD)
            .padding(vertical = 6.dp),
    ) {
        rows.forEachIndexed { i, (label, value) ->
            if (i > 0) {
                Box(
                    Modifier
                        .padding(start = 18.dp)
                        .fillMaxWidth()
                        .height(0.6.dp)
                        .background(palette.onGlass.copy(alpha = 0.12f)),
                )
            }
            Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = palette.onGlass, fontSize = 15.sp, modifier = Modifier.weight(1f))
                Spacer(Modifier.width(12.dp))
                Text(value, color = palette.onGlassMuted, fontSize = 15.sp, textAlign = TextAlign.End, maxLines = 2, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** The lines of "In this setup", from what it holds. */
internal fun madeOf(setup: ConnectSetup): List<Pair<String, String>> {
    val o = runCatching { JSONObject(setup.settings) }.getOrNull() ?: JSONObject()
    return buildList {
        o.optString("theme").takeIf { it.isNotEmpty() }?.let { theme ->
            val mode = when (o.optString("appearance")) {
                "light" -> " · Light"
                "dark" -> " · Dark"
                else -> ""
            }
            add("Theme" to ConnectApply.themeName(theme) + mode)
        }
        o.optString("wallpaperStyle").takeIf { it.isNotEmpty() }?.let { style ->
            val colours = o.optString("wallpaperPalette").ifEmpty { o.optString("paletteId") }
            val name = Palettes.all.firstOrNull { it.id == colours }?.name
            add("Wallpaper" to if (name != null) "$style · $name colours" else style)
        }
        o.optString("iconStyle").takeIf { it.isNotEmpty() }?.let { add("Icons" to IconStyles.label(it)) }
        o.optString("widgetLook").takeIf { it.isNotEmpty() }?.let { add("Widgets" to WidgetLooks.label(it)) }
        add("Home screen" to pagesOf(setup))
    }
}

private fun pagesOf(setup: ConnectSetup): String {
    val pages = ConnectApply.pageCount(setup.layout)
    if (!ConnectLayers.hasLayout(setup) || pages == 0) return "Yours, as it is"
    val things = ConnectApply.items(setup.layout).size
    return (if (pages == 1) "1 page" else "$pages pages") + " · " + (if (things == 1) "1 app or widget" else "$things apps and widgets")
}

/**
 * Back, at the top, over a shade so the clock stays clear. As the page scrolls under it the shade
 * deepens ([shade]), and once the big name has gone under it the name comes up small beside Back
 * ([underBar], 0 to 1). Both are read while drawing.
 */
@Composable
private fun BoxScope.TopBar(setup: ConnectSetup, underBar: () -> Float, shade: () -> Float, onBack: () -> Unit) {
    val palette = LocalPalette.current
    val ink = edgeColour(top = true)
    val strong = !LocalOnWallpaper.current
    Box(
        Modifier
            .align(Alignment.TopCenter)
            .fillMaxWidth()
            .drawBehind {
                val deep = shade()
                drawRect(
                    Brush.verticalGradient(
                        0f to ink.copy(alpha = if (strong) 0.9f + 0.08f * deep else 0.5f + 0.47f * deep),
                        0.7f to ink.copy(alpha = if (strong) 0.55f + 0.4f * deep else 0.2f + 0.7f * deep),
                        1f to Color.Transparent,
                    ),
                )
            }
            .statusBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 14.dp),
    ) {
        RoundButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back", onClick = onBack, size = 44.dp)
        Text(
            setup.name,
            color = palette.onGlass,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 60.dp)
                .graphicsLayer { alpha = ((underBar() - 0.55f) / 0.45f).coerceIn(0f, 1f) },
        )
    }
}

/** Apply, at the foot of the page whatever is scrolled, with Undo beside it while something Connect put on can be taken off. */
@Composable
private fun BoxScope.ApplyBar(setup: ConnectSetup, on: Set<Layer>, state: ConnectState, bridge: ConnectBridge) {
    val canApply = ConnectApply.canApply(setup, on)
    val label = if (setup.source == SetupSource.MINE && !bridge.plus) "Apply · Plus" else "Apply to Zenith"
    Box(
        Modifier
            .align(Alignment.BottomCenter)
            .fillMaxWidth()
            .background(footFade())
            .navigationBarsPadding()
            .padding(start = 16.dp, end = 16.dp, top = 26.dp, bottom = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(Modifier.widthIn(max = 560.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (bridge.worn != null) {
                ConnectButton("Undo", onClick = { state.undo(bridge) }, icon = Icons.AutoMirrored.Rounded.Undo, height = 56.dp)
            }
            ConnectButton(
                label,
                onClick = { state.apply(bridge) },
                icon = Icons.Rounded.Check,
                tinted = true,
                enabled = canApply,
                height = 56.dp,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** A Look's name as a word in a sentence. */
internal fun quoted(name: String): String = name.trim().ifEmpty { "it" }.let { if (it.length > 24) it.take(23) + "…" else it }
