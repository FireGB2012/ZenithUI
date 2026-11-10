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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.onboarding.PhonePreview
import com.gabriel.zenithui.ui.onboarding.PrimaryButton
import com.gabriel.zenithui.ui.onboarding.Segmented
import com.gabriel.zenithui.ui.settings.ActionRow
import com.gabriel.zenithui.ui.settings.Caption
import com.gabriel.zenithui.ui.settings.Group
import com.gabriel.zenithui.ui.settings.SettingsHeader
import com.gabriel.zenithui.ui.settings.SettingsPreview
import com.gabriel.zenithui.ui.settings.ToggleRow
import com.gabriel.zenithui.ui.theme.LocalBackdrop
import com.gabriel.zenithui.ui.theme.LocalGlassLevels
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.NeutralLook
import com.gabriel.zenithui.ui.theme.Palettes
import com.gabriel.zenithui.ui.theme.ZenithPalette
import com.gabriel.zenithui.ui.theme.toned
import com.gabriel.zenithui.ui.theme.wallColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A gallery of wallpapers Zenith paints itself, in a choice of colour sets, each with a light
 * picture and a dark one. It opens on the colours your design is in, or was last set in; the chips
 * along the top sort the designs onto shelves and show them in another set. Tapping one shows it
 * large, as it will be by day and by night, with your own home screen on it. Once set, a design
 * keeps its colours and changes between light and dark with Zenith, painted again to match. Your
 * own photos are one row away at the bottom.
 */
@Composable
fun WallpaperSheet(
    visible: Boolean,
    settings: ZenithSettings,
    /** Page one of the home screen, for a design's preview to show on it. */
    home: SettingsPreview,
    /** Whether the designs made for Zenith Plus may be set. */
    plusActive: Boolean,
    /** Sets a design, painted in the colour set with the id [colours], and on the lock screen too if [lockToo]. */
    onSet: (style: WallStyle, colours: String, lockToo: Boolean) -> Unit,
    /** Called once a photo has become the wallpaper. */
    onPhoto: () -> Unit,
    onFollow: (Boolean) -> Unit,
    onPlus: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    // The colour set the wallpaper is painted in, or would be: an empty one is from settings saved
    // before every design was given a set, and means paletteId's.
    val current = remember(
        settings.wallpaperPalette, settings.paletteId,
        settings.customAccent, settings.customAccentAlt, settings.customBackdrop, settings.customDark,
        settings.wallAccent, settings.wallAccentAlt, settings.wallBackdrop, settings.wallDark,
    ) {
        Palettes.setById(settings.wallpaperPalette.ifEmpty { settings.paletteId }, settings)
    }
    // The wallpaper's colours first, then every other set a design can be painted in: the six, the
    // Aura sets you bought, your own if you made one, and the one taken from your wallpaper.
    val palettes = remember(
        current, settings.unlockedThemes, settings.paletteId,
        settings.customAccent, settings.customAccentAlt, settings.customBackdrop, settings.customDark,
        settings.wallAccent, settings.wallAccentAlt, settings.wallBackdrop, settings.wallDark,
    ) {
        val madeOwn = settings.paletteId == Palettes.CUSTOM || settings.customAccent != ZenithSettings().customAccent
        val others = Palettes.all + Palettes.aura.filter { it.id in settings.unlockedThemes } +
            listOfNotNull(Palettes.custom(settings).takeIf { madeOwn }, Palettes.fromWallpaper(settings))
        listOf(current) + others.filter { it.id != current.id }
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

    ZenithSheet(visible = visible, onDismiss = onDismiss, maxHeightFraction = 0.9f) {
        SettingsHeader(
            title = if (picked == null) "Wallpapers" else picked!!.first.label,
            canGoBack = picked != null,
            onBack = { picked = null },
        )
        Spacer(Modifier.height(10.dp))
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
                    following = settings.wallpaperFollow,
                    locked = selection.first.plus && !plusActive,
                    startOnLock = settings.wallpaperLock,
                    onSet = { colours, lockToo ->
                        // No toast: the wallpaper fading in behind the sheet as it closes says it.
                        onSet(selection.first, colours, lockToo)
                        onDismiss()
                    },
                    onPlus = onPlus,
                )
            }
        }
    }
}

@Composable
private fun Browse(
    settings: ZenithSettings,
    palettes: List<ZenithPalette>,
    dark: Boolean,
    plusActive: Boolean,
    filter: String,
    onFilter: (String) -> Unit,
    shelf: WallGroup?,
    onShelf: (WallGroup?) -> Unit,
    /** A design was tapped: which, in which colour set, and where on screen the middle of its thumbnail is. */
    onPick: (WallStyle, ZenithPalette, Offset) -> Unit,
    onFollow: (Boolean) -> Unit,
    onPhotos: () -> Unit,
    onOtherApp: () -> Unit,
) {
    val shown = palettes.firstOrNull { it.id == filter } ?: palettes.first()
    val inUse = WallStyle.byName(settings.wallpaperStyle)
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Group {
            ToggleRow(
                title = "Follows light and dark",
                subtitle = when {
                    inUse == null -> "Pick one and it will change with light and dark."
                    settings.wallpaperFollow -> "${inUse.label} repaints when Zenith goes light or dark"
                    else -> "${inUse.label} stays as it is"
                },
                checked = settings.wallpaperFollow,
                onChange = onFollow,
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Chip("All", selected = shelf == null) { onShelf(null) }
            WallGroup.entries.forEach { group ->
                Chip(group.label, selected = shelf == group) { onShelf(group) }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            palettes.forEach { option ->
                Chip(
                    label = option.name,
                    selected = option.id == shown.id,
                    dot = option,
                ) { onFilter(option.id) }
            }
        }
        Spacer(Modifier.height(14.dp))
        AnimatedContent(
            targetState = shown to shelf,
            transitionSpec = { fadeIn(tween(220, delayMillis = 60)).togetherWith(fadeOut(tween(120))) },
            label = "wallpaperTheme",
        ) { (theme, group) ->
            // Shown as they would be set: in this colour set, light or dark as Zenith is now.
            val toned = remember(theme, dark) { theme.toned(dark) }
            val styles = remember(group) { WallStyle.entries.filter { group == null || it.group == group } }
            Column {
                Caption("${styles.size} designs in ${coloursOf(theme)}")
                Spacer(Modifier.height(10.dp))
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
                    Spacer(Modifier.height(10.dp))
                }
            }
        }
        Group {
            ActionRow(
                icon = Icons.Rounded.Photo,
                title = "Your photos",
                subtitle = "Pick a picture; the glass will bend it too",
                onClick = onPhotos,
            )
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

@Composable
private fun Preview(
    style: WallStyle,
    palette: ZenithPalette,
    settings: ZenithSettings,
    home: SettingsPreview,
    dark: Boolean,
    following: Boolean,
    locked: Boolean,
    startOnLock: Boolean,
    onSet: (colours: String, lockToo: Boolean) -> Unit,
    onPlus: () -> Unit,
) {
    val ui = LocalPalette.current
    var target by remember { mutableStateOf(if (startOnLock) "both" else "home") }
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
        // real thing will bend this one, which is not behind it yet.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
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
                            Box(Modifier.matchParentSize().clip(RoundedCornerShape(corner)).background(ui.onGlass.copy(alpha = 0.06f)))
                        } else {
                            CompositionLocalProvider(
                                LocalPalette provides NeutralLook.of(night),
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
                        color = if (now) ui.onGlass else ui.onGlassMuted,
                        fontSize = 12.sp,
                        fontWeight = if (now) FontWeight.SemiBold else FontWeight.Medium,
                    )
                }
                if (!night) Spacer(Modifier.width(12.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        Segmented(
            options = listOf("home" to "Home screen", "both" to "Home and lock"),
            selected = target,
            onSelect = { target = it },
        )
        Spacer(Modifier.height(10.dp))
        Caption(
            if (!following) {
                "Following is off, so it stays as it is until you set it again."
            } else {
                "In ${coloursOf(palette)}. It changes between light and dark as Zenith does."
            }
        )
        Spacer(Modifier.height(12.dp))
        PrimaryButton(
            text = if (locked) "Unlock with Plus" else "Set wallpaper",
            onClick = {
                if (locked) {
                    onPlus()
                } else {
                    onSet(palette.id, target == "both")
                }
            },
        )
        Spacer(Modifier.height(4.dp))
    }
}

/**
 * A colour set's colours, as a sentence names them. Your own and the wallpaper's are named for
 * where they came from, since "Yours's colours" would not read.
 */
private fun coloursOf(set: ZenithPalette): String = when (set.id) {
    Palettes.CUSTOM -> "your own colours"
    Palettes.WALLPAPER -> "the colours taken from your wallpaper"
    else -> "${set.name}'s colours"
}

/** A pill to choose with: a shelf of designs, or, with a [dot] of its two accents, a colour set. */
@Composable
private fun Chip(label: String, selected: Boolean, dot: ZenithPalette? = null, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val fill by animateColorAsState(
        if (selected) palette.onGlass.copy(alpha = 0.16f) else Color.Transparent,
        tween(200),
        label = "chip",
    )
    Row(
        Modifier
            .height(38.dp)
            .pressBounce(0.94f)
            .clip(RoundedCornerShape(50))
            .background(fill)
            .border(1.dp, palette.onGlass.copy(alpha = if (selected) 0f else 0.16f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(start = if (dot != null) 8.dp else 14.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (dot != null) {
            Box(
                Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(dot.accent, dot.accentAlt)))
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            label,
            color = palette.onGlass,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

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
    val ui = LocalPalette.current
    val centre = remember { arrayOf(Offset.Zero) }
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.46f)
                .onGloballyPositioned { centre[0] = it.boundsInRoot().center }
                .pressBounce(0.94f)
                .clip(RoundedCornerShape(16.dp))
                .border(
                    if (inUse) 2.dp else 1.dp,
                    if (inUse) ui.accent else ui.onGlass.copy(alpha = 0.12f),
                    RoundedCornerShape(16.dp),
                )
                .clickable { onClick(centre[0]) },
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
            color = if (inUse) ui.onGlass else ui.onGlassMuted,
            fontSize = 12.sp,
            fontWeight = if (inUse) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

/** Between a preview and the ring round the one that is as Zenith is now. */
private val RING_GAP = 4.dp

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

/** A painted wallpaper as a picture. It fades in once ready rather than popping. */
@Composable
private fun WallpaperImage(style: WallStyle, palette: ZenithPalette, widthPx: Int, modifier: Modifier) {
    val ui = LocalPalette.current
    val image = rememberWallpaperArt(style, palette, widthPx)
    val shown by animateFloatAsState(if (image != null) 1f else 0f, tween(260), label = "wallpaperFade")
    Box(modifier.background(ui.onGlass.copy(alpha = 0.06f))) {
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
 * Painted thumbnails, kept by how much memory they take and not by how many there are:
 * thirty-three designs in every colour set, light and dark, would otherwise be a great many pictures.
 */
private val thumbCache = object : LruCache<String, ImageBitmap>(24 * 1024 * 1024) {
    override fun sizeOf(key: String, value: ImageBitmap): Int = value.width * value.height * 4
}
