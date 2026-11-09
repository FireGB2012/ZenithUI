package com.gabriel.zenithui.ui.home

import android.content.Intent
import android.util.LruCache
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.gabriel.zenithui.data.GlassWallpaper
import com.gabriel.zenithui.data.WallGroup
import com.gabriel.zenithui.data.WallStyle
import com.gabriel.zenithui.data.WallpaperArt
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.data.findActivity
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.ZenithButton
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.onboarding.PhonePreview
import com.gabriel.zenithui.ui.onboarding.Segmented
import com.gabriel.zenithui.ui.settings.ActionRow
import com.gabriel.zenithui.ui.settings.ControlRow
import com.gabriel.zenithui.ui.settings.GroupDivider
import com.gabriel.zenithui.ui.settings.ROW_INSET
import com.gabriel.zenithui.ui.settings.Section
import com.gabriel.zenithui.ui.settings.SettingsPreview
import com.gabriel.zenithui.ui.settings.ToggleRow
import com.gabriel.zenithui.ui.theme.ButtonKind
import com.gabriel.zenithui.ui.theme.LocalApple
import com.gabriel.zenithui.ui.theme.LocalBackdrop
import com.gabriel.zenithui.ui.theme.LocalGlassLevels
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.Palettes
import com.gabriel.zenithui.ui.theme.ZenithPalette
import com.gabriel.zenithui.ui.theme.inkOnTint
import com.gabriel.zenithui.ui.theme.toned
import com.gabriel.zenithui.ui.theme.wallColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.gabriel.zenithui.ui.theme.AppleColours

/**
 * A gallery of wallpapers Zenith paints itself, in every theme's colours and in light and dark.
 * It opens on the ones made for the theme you are using; the chips along the top sort the designs
 * onto shelves and show them in any other theme. Tapping one shows it large, as it will be by day
 * and by night, with your own home screen on it, and the sheet's own chevron goes back to the
 * gallery. Once set, a design is kept in step with the theme: change the palette, or go from light
 * to dark, and the wallpaper is painted again to match. Your own photos are one row away at the
 * bottom. As on an iPhone's sheet, everything in it stands on solid cards.
 */
@Composable
fun WallpaperSheet(
    visible: Boolean,
    settings: ZenithSettings,
    /** Page one of the home screen, for a design's preview to show on it. */
    home: SettingsPreview,
    /** Whether the designs made for Zenith Plus may be set. */
    plusActive: Boolean,
    /**
     * Sets a design. [pinnedPalette] is the palette to keep it in whatever the theme does, or empty
     * to follow the theme; [switchThemeTo] is the theme to change Zenith to as well, if asked.
     */
    onSet: (style: WallStyle, pinnedPalette: String, lockToo: Boolean, switchThemeTo: String?) -> Unit,
    /** Called once a photo has become the wallpaper. */
    onPhoto: () -> Unit,
    onFollow: (Boolean) -> Unit,
    onPlus: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val current = remember(settings.paletteId, settings.customAccent, settings.customAccentAlt, settings.customBackdrop) {
        Palettes.resolve(settings)
    }
    // The theme you are using first, then every other one you can use.
    val palettes = remember(current, settings.unlockedThemes) {
        buildList {
            add(current)
            addAll((Palettes.all + Palettes.aura.filter { it.id in settings.unlockedThemes }).filter { it.id != current.id })
        }
    }
    val dark = settings.isDark(isSystemInDarkTheme())
    var filter by remember { mutableStateOf(current.id) }
    var shelf by remember { mutableStateOf<WallGroup?>(null) }
    var picked by remember { mutableStateOf<Pair<WallStyle, ZenithPalette>?>(null) }
    // Where the thumbnail that was tapped sits within the sheet, for its preview to grow out of.
    var pagesAt by remember { mutableStateOf(Rect.Zero) }
    var growFrom by remember { mutableStateOf(TransformOrigin.Center) }
    val reduced = LocalMotionLevel.current.reduced
    LaunchedEffect(visible) {
        if (visible) {
            filter = current.id
            picked = null
        }
    }
    BackHandler(enabled = visible && picked != null) { picked = null }

    // Your own photo, through Android's photo picker (no permission needed). Zenith sets it itself
    // so it can keep a copy for the glass, which Android would not otherwise let it see.
    // Run on the activity, not this sheet: closing the sheet must not cut a wallpaper off half set.
    val activityScope = (context.findActivity() as? androidx.activity.ComponentActivity)?.lifecycleScope
    val sheetScope = rememberCoroutineScope()
    val latestPhoto by rememberUpdatedState(onPhoto)
    val latestDismiss by rememberUpdatedState(onDismiss)
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        (activityScope ?: sheetScope).launch {
            val ok = GlassWallpaper.setPhoto(context, uri)
            Toast.makeText(context, if (ok) "Wallpaper set" else "That picture could not be used", Toast.LENGTH_SHORT).show()
            if (ok) {
                latestPhoto()
                latestDismiss()
            }
        }
    }

    // The sheet's own header names the page: the gallery, or the design shown large, with the
    // chevron back to the gallery while one is.
    ZenithSheet(
        visible = visible,
        onDismiss = onDismiss,
        title = picked?.first?.label ?: "Wallpapers",
        maxHeightFraction = 0.9f,
        onBack = if (picked != null) ({ picked = null }) else null,
        backTo = "Wallpapers",
    ) {
        AnimatedContent(
            targetState = picked,
            transitionSpec = {
                val opening = targetState != null
                when {
                    reduced -> fadeIn(tween(Motion.REDUCED_MS)).togetherWith(fadeOut(tween(Motion.REDUCED_MS)))
                    // The preview grows out of the thumbnail that was tapped...
                    opening -> (fadeIn(tween(200)) + scaleIn(Motion.open(), initialScale = 0.3f, transformOrigin = growFrom))
                        .togetherWith(fadeOut(tween(160)))
                    // ...and goes back into it.
                    else -> fadeIn(tween(240, delayMillis = 80))
                        .togetherWith(fadeOut(tween(200)) + scaleOut(Motion.close(), targetScale = 0.3f, transformOrigin = growFrom))
                }.using(SizeTransform(clip = false) { _, _ -> spring(dampingRatio = 1f, stiffness = 380f, visibilityThreshold = IntSize(1, 1)) })
            },
            modifier = Modifier.onGloballyPositioned { pagesAt = it.boundsInRoot() },
            label = "wallpaperPage",
        ) { selection ->
            if (selection == null) {
                Browse(
                    settings = settings,
                    palettes = palettes,
                    currentId = current.id,
                    dark = dark,
                    plusActive = plusActive,
                    filter = filter,
                    onFilter = { filter = it },
                    shelf = shelf,
                    onShelf = { shelf = it },
                    onPick = { style, palette, centre ->
                        if (!pagesAt.isEmpty) {
                            growFrom = TransformOrigin(
                                ((centre.x - pagesAt.left) / pagesAt.width).coerceIn(0f, 1f),
                                ((centre.y - pagesAt.top) / pagesAt.height).coerceIn(0f, 1f),
                            )
                        }
                        picked = style to palette
                    },
                    onFollow = onFollow,
                    onPhotos = {
                        // The sheet stays open underneath the picker, so its answer has somewhere to land.
                        pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onOtherApp = {
                        runCatching {
                            context.startActivity(
                                Intent.createChooser(Intent(Intent.ACTION_SET_WALLPAPER), "Wallpaper")
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        }
                        onDismiss()
                    },
                )
            } else {
                Preview(
                    style = selection.first,
                    palette = selection.second,
                    settings = settings,
                    home = home,
                    dark = dark,
                    isCurrentTheme = selection.second.id == current.id,
                    following = settings.wallpaperFollow,
                    locked = selection.first.plus && !plusActive,
                    startOnLock = settings.wallpaperLock,
                    onSet = { pinned, lockToo, theme ->
                        // No toast: the wallpaper fading in behind the sheet as it closes says it.
                        onSet(selection.first, pinned, lockToo, theme)
                        onDismiss()
                    },
                    onPlus = onPlus,
                )
            }
        }
    }
}

/**
 * The gallery, card by card: whether the wallpaper follows the theme; the chips that choose a shelf
 * and a theme, with how many designs that makes in grey under them; the designs themselves, three
 * to a row; and the ways to a photo of your own or to another app's wallpapers.
 */
@Composable
private fun Browse(
    settings: ZenithSettings,
    palettes: List<ZenithPalette>,
    currentId: String,
    dark: Boolean,
    plusActive: Boolean,
    filter: String,
    onFilter: (String) -> Unit,
    shelf: WallGroup?,
    onShelf: (WallGroup?) -> Unit,
    /** A design was tapped: which, in which theme, and where on screen the middle of its thumbnail is. */
    onPick: (WallStyle, ZenithPalette, Offset) -> Unit,
    onFollow: (Boolean) -> Unit,
    onPhotos: () -> Unit,
    onOtherApp: () -> Unit,
) {
    val shown = palettes.firstOrNull { it.id == filter } ?: palettes.first()
    val inUse = WallStyle.byName(settings.wallpaperStyle)
    // How many designs the shelf chosen holds, for the words under the chips.
    val count = remember(shelf) { WallStyle.entries.count { shelf == null || it.group == shelf } }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Section {
            ToggleRow(
                title = "Follows your theme",
                subtitle = when {
                    inUse == null -> "Pick one and it will follow your theme."
                    settings.wallpaperFollow -> "${inUse.label} repaints when you change palette, or go light or dark"
                    else -> "${inUse.label} stays as it is"
                },
                checked = settings.wallpaperFollow,
                onChange = onFollow,
            )
        }
        // Which shelf, and in whose colours: a row of chips for each, scrolling sideways, with a
        // hairline between them. Under the card, how many designs that is and whose colours they
        // are painted in.
        Section(
            footer = if (shown.id == currentId) {
                "$count designs, painted in your theme's colours"
            } else {
                "$count designs, painted in ${shown.name}'s colours"
            },
        ) {
            ControlRow(edgeToEdge = true) {
                ChipRow {
                    Chip("All", selected = shelf == null) { onShelf(null) }
                    WallGroup.entries.forEach { group ->
                        Chip(group.label, selected = shelf == group) { onShelf(group) }
                    }
                }
            }
            GroupDivider(inset = ROW_INSET)
            ControlRow(edgeToEdge = true) {
                ChipRow {
                    palettes.forEach { option ->
                        Chip(
                            label = if (option.id == currentId) "${option.name} (yours)" else option.name,
                            selected = option.id == shown.id,
                            dot = option,
                        ) { onFilter(option.id) }
                    }
                }
            }
        }
        // The designs, on a card of their own. A new shelf or theme fades in over the last, and
        // the card takes its new height smoothly.
        Section {
            ControlRow {
                AnimatedContent(
                    targetState = shown to shelf,
                    transitionSpec = { fadeIn(tween(220, delayMillis = 60)).togetherWith(fadeOut(tween(120))) },
                    label = "wallpaperTheme",
                ) { (theme, group) ->
                    // Shown as they would be set: in this theme's colours, light or dark as Zenith is now.
                    val toned = remember(theme, dark) { theme.toned(dark) }
                    val styles = remember(group) { WallStyle.entries.filter { group == null || it.group == group } }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        styles.chunked(3).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                row.forEach { style ->
                                    Thumb(
                                        style = style,
                                        palette = toned,
                                        locked = style.plus && !plusActive,
                                        inUse = style == inUse,
                                        modifier = Modifier.weight(1f),
                                    ) { centre -> onPick(style, theme, centre) }
                                }
                                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }
        }
        Section {
            ActionRow(
                icon = Icons.Rounded.Photo,
                title = "Your photos",
                subtitle = "Pick a picture; the glass will bend it too",
                onClick = onPhotos,
            )
            GroupDivider()
            ActionRow(
                icon = Icons.Rounded.Wallpaper,
                title = "Another app",
                subtitle = "Glass stays frosted, as Android keeps those private",
                onClick = onOtherApp,
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

/**
 * One design shown large before it is set. On a card, the two pictures it is, by day and by night,
 * the one as Zenith is now ringed in the accent; on the next, where it goes and, for a design in
 * another theme's colours, whether Zenith changes to that theme too, with what that means for the
 * wallpaper in grey under the card; and under them, the button that sets it.
 */
@Composable
private fun Preview(
    style: WallStyle,
    palette: ZenithPalette,
    settings: ZenithSettings,
    home: SettingsPreview,
    dark: Boolean,
    isCurrentTheme: Boolean,
    following: Boolean,
    locked: Boolean,
    startOnLock: Boolean,
    onSet: (pinnedPalette: String, lockToo: Boolean, switchThemeTo: String?) -> Unit,
    onPlus: () -> Unit,
) {
    val ui = LocalPalette.current
    val apple = LocalApple.current
    var target by remember { mutableStateOf(if (startOnLock) "both" else "home") }
    var match by remember { mutableStateOf(true) }
    // In another theme's colours and Zenith not changing to it: the wallpaper keeps those colours.
    val pinned = !isCurrentTheme && !match
    // The colours the home screen on it will be in: the design's, if Zenith is changing to them.
    val homeIn = if (isCurrentTheme || match) palette else ui
    val configuration = LocalConfiguration.current
    val screenWidth = home.screenWidth.takeIf { it > 0.dp } ?: configuration.screenWidthDp.dp
    val screenHeight = home.screenHeight.takeIf { it > 0.dp } ?: configuration.screenHeightDp.dp
    val tall = 292.dp
    // The phone's own corner, at the size it is drawn.
    val corner = 46.dp * (tall / screenHeight)

    Column(Modifier.verticalScroll(rememberScrollState())) {
        // By day and by night, side by side: one design is two pictures. On each is your own home
        // screen, its words in the ink they take in that light, so what living with it is like is
        // seen before it is set. Its glass is frosted, as over any wallpaper Zenith cannot see: the
        // real thing will bend this one, which is not behind it yet. The pair stands on a card, as
        // an iPhone shows a wallpaper in its Settings.
        Section {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                listOf(false, true).forEach { night ->
                    val now = night == dark
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val art = rememberWallpaperArt(style, remember(palette, night) { palette.toned(night) }, widthPx = 360)
                        val appear by animateFloatAsState(if (art != null) 1f else 0f, tween(260), label = "previewFade")
                        val ring by animateColorAsState(if (now) ui.accent else Color.Transparent, tween(220), label = "previewRing")
                        Box(
                            Modifier
                                .border(2.dp, ring, RoundedCornerShape(corner + RING_GAP))
                                .padding(RING_GAP)
                                .size(tall * (screenWidth / screenHeight), tall),
                        ) {
                            if (art == null) {
                                // While it is painted, the grey of a card on a card holds its place.
                                Box(Modifier.matchParentSize().clip(RoundedCornerShape(corner)).background(apple.raised))
                            } else {
                                CompositionLocalProvider(
                                    LocalPalette provides remember(homeIn, night) { homeIn.toned(night) },
                                    // Each phone in its own tone, widget tiles too, not the tone in use now.
                                    LocalApple provides AppleColours.of(night),
                                    LocalGlassLevels provides null,
                                    LocalBackdrop provides null,
                                ) {
                                    PhonePreview(
                                        items = home.items,
                                        dock = settings.dock,
                                        appsByKey = home.appsByKey,
                                        settings = settings,
                                        wallpaper = art,
                                        screenWidth = screenWidth,
                                        screenHeight = screenHeight,
                                        shown = Int.MAX_VALUE,
                                        modifier = Modifier
                                            .matchParentSize()
                                            .graphicsLayer { alpha = appear },
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(6.dp))
                        Text(
                            (if (night) "Dark" else "Light") + if (now) ", as now" else "",
                            color = if (now) apple.words else apple.secondary,
                            fontSize = 12.sp,
                            fontWeight = if (now) FontWeight.SemiBold else FontWeight.Medium,
                        )
                    }
                    if (!night) Spacer(Modifier.width(12.dp))
                }
            }
        }
        Section(
            footer = when {
                !following -> "Following is off, so it stays as it is until you set it again."
                pinned -> "Stays in ${palette.name}'s colours, and still changes between light and dark."
                else -> "Changes with your theme: its palette, and light or dark."
            },
        ) {
            ControlRow {
                Segmented(
                    options = listOf("home" to "Home screen", "both" to "Home and lock"),
                    selected = target,
                    onSelect = { target = it },
                )
            }
            if (!isCurrentTheme) {
                GroupDivider(inset = ROW_INSET)
                ToggleRow(
                    title = "Use ${palette.name} in Zenith too",
                    // A theme now colours the switches, buttons and highlights, and no longer the glass.
                    subtitle = "Switches, buttons and accents to match",
                    checked = match,
                    onChange = { match = it },
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        ZenithButton(
            text = if (locked) "Unlock with Plus" else "Set wallpaper",
            onClick = {
                if (locked) {
                    onPlus()
                } else {
                    onSet(
                        if (pinned) palette.id else "",
                        target == "both",
                        if (!isCurrentTheme && match) palette.id else null,
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            kind = ButtonKind.Main,
        )
        Spacer(Modifier.height(4.dp))
    }
}

/**
 * A row of chips running from edge to edge of its card. It scrolls sideways, and at either end its
 * chips start and stop where the card's rows do.
 */
@Composable
private fun ChipRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

/**
 * A chip to choose with, on a card: a shelf of designs, or, with a [dot] of its two accents, a
 * theme. The one chosen is filled with the accent, its words in the ink the accent needs, and a
 * thin ring keeps its dot apart from that fill; the others are the grey of a card on a card. It
 * ticks under the finger when it becomes the one chosen.
 */
@Composable
private fun Chip(label: String, selected: Boolean, dot: ZenithPalette? = null, onClick: () -> Unit) {
    val apple = LocalApple.current
    val accent = LocalPalette.current.accent
    val ink = inkOnTint(accent)
    val tick = rememberTick()
    val fill by animateColorAsState(if (selected) accent else apple.raised, tween(200), label = "chip")
    val words by animateColorAsState(if (selected) ink else apple.words, tween(200), label = "chipWords")
    val rim by animateColorAsState(ink.copy(alpha = if (selected) 0.5f else 0f), tween(200), label = "chipDotRim")
    Row(
        Modifier
            .height(38.dp)
            .pressBounce(0.94f)
            .clip(RoundedCornerShape(50))
            .background(fill)
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.RadioButton,
            ) {
                if (!selected) tick()
                onClick()
            }
            .padding(start = if (dot != null) 8.dp else 14.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(dot.accent, dot.accentAlt)))
                    .border(1.5.dp, rim, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            label,
            color = words,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

/**
 * One design to pick, on a card: its picture, and its name under it. The one you have now is
 * ringed in the accent, as Apple marks what is chosen; one made for Zenith Plus wears a lock.
 */
@Composable
private fun Thumb(
    style: WallStyle,
    palette: ZenithPalette,
    locked: Boolean,
    inUse: Boolean,
    modifier: Modifier,
    /** Tapped; with where the middle of the thumbnail is on screen. */
    onClick: (Offset) -> Unit,
) {
    val apple = LocalApple.current
    val accent = LocalPalette.current.accent
    val centre = remember { arrayOf(Offset.Zero) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.46f)
                .onGloballyPositioned { centre[0] = it.boundsInRoot().center }
                .pressBounce(0.94f)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onClick(centre[0]) }
                // Every thumbnail keeps room for the ring, so the one that has it is no smaller.
                .border(2.dp, if (inUse) accent else Color.Transparent, RoundedCornerShape(THUMB_CORNER + RING_GAP))
                .padding(RING_GAP)
                .clip(RoundedCornerShape(THUMB_CORNER)),
        ) {
            WallpaperImage(style = style, palette = palette, widthPx = 240, modifier = Modifier.fillMaxSize())
            if (locked) {
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp)
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Rounded.Lock, contentDescription = "Needs Zenith Plus", tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            style.label,
            color = if (inUse) apple.words else apple.secondary,
            fontSize = 12.sp,
            fontWeight = if (inUse) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/**
 * Between a picture and the ring in the accent round the one that is chosen: the preview that is
 * as Zenith is now, and the thumbnail of the design you have.
 */
private val RING_GAP = 4.dp

/** The corners of a design's thumbnail, inside the room kept for its ring. */
private val THUMB_CORNER = 16.dp

/**
 * One painted wallpaper, drawn off the main thread and kept, so scrolling back past it is
 * instant: null until it is ready.
 */
@Composable
private fun rememberWallpaperArt(style: WallStyle, palette: ZenithPalette, widthPx: Int): ImageBitmap? {
    val heightPx = (widthPx / 0.46f).toInt()
    val colors = palette.wallColors()
    val key = "${style.name}|$colors|$widthPx"
    val image by produceState(initialValue = thumbCache.get(key), key) {
        value = thumbCache.get(key) ?: withContext(Dispatchers.Default) {
            WallpaperArt.render(style, colors, widthPx, heightPx).asImageBitmap()
        }.also { thumbCache.put(key, it) }
    }
    return image
}

/**
 * A painted wallpaper as a picture. Until it is ready its place is the grey of a card on a card,
 * and it fades in over that rather than popping.
 */
@Composable
private fun WallpaperImage(style: WallStyle, palette: ZenithPalette, widthPx: Int, modifier: Modifier) {
    val image = rememberWallpaperArt(style, palette, widthPx)
    val shown by animateFloatAsState(if (image != null) 1f else 0f, tween(260), label = "wallpaperFade")
    Box(modifier.background(LocalApple.current.raised)) {
        image?.let {
            Image(
                bitmap = it,
                contentDescription = "${style.label} wallpaper",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = shown },
            )
        }
    }
}

/**
 * Painted thumbnails, kept by how much memory they take and not by how many there are: twenty-two
 * designs in every theme, light and dark, would otherwise be a great many pictures.
 */
private val thumbCache = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
}
