package com.gabriel.zenithui.ui.home

import android.content.Intent
import android.util.LruCache
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.lifecycle.lifecycleScope
import com.gabriel.zenithui.data.findActivity
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
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.Photo
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.GlassWallpaper
import com.gabriel.zenithui.data.WallColors
import com.gabriel.zenithui.data.WallStyle
import com.gabriel.zenithui.data.WallpaperArt
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.onboarding.PrimaryButton
import com.gabriel.zenithui.ui.onboarding.Segmented
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.Palettes
import com.gabriel.zenithui.ui.theme.ZenithPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * A gallery of wallpapers Zenith paints itself, in every theme's colours. It opens on the ones
 * made for the theme you are using; the chips along the top show the same designs in any other.
 * Tapping one shows it large, and setting a wallpaper from another theme can switch Zenith's
 * colours to match. Your own photos are one row away at the bottom.
 */
@Composable
fun WallpaperSheet(
    visible: Boolean,
    settings: ZenithSettings,
    /** Called once a wallpaper is set; [paletteId] is the theme to switch to, if you asked to match. */
    onApplied: (paletteId: String?) -> Unit,
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
    var filter by remember { mutableStateOf(current.id) }
    var picked by remember { mutableStateOf<Pair<WallStyle, ZenithPalette>?>(null) }
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
    val latestApplied by androidx.compose.runtime.rememberUpdatedState(onApplied)
    val latestDismiss by androidx.compose.runtime.rememberUpdatedState(onDismiss)
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        (activityScope ?: sheetScope).launch {
            val ok = GlassWallpaper.setPhoto(context, uri)
            Toast.makeText(context, if (ok) "Wallpaper set" else "That picture could not be used", Toast.LENGTH_SHORT).show()
            if (ok) {
                latestApplied(null)
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
                (fadeIn(tween(240, delayMillis = 40)) + scaleIn(spring(dampingRatio = 1f, stiffness = 400f), initialScale = 0.96f))
                    .togetherWith(fadeOut(tween(120)))
                    .using(SizeTransform(clip = false) { _, _ -> spring(dampingRatio = 1f, stiffness = 380f, visibilityThreshold = IntSize(1, 1)) })
            },
            label = "wallpaperPage",
        ) { selection ->
            if (selection == null) {
                Browse(
                    palettes = palettes,
                    currentId = current.id,
                    filter = filter,
                    onFilter = { filter = it },
                    onPick = { style, palette -> picked = style to palette },
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
                    isCurrentTheme = selection.second.id == current.id,
                    onApplied = onApplied,
                    onDismiss = onDismiss,
                )
            }
        }
    }
}

@Composable
private fun Browse(
    palettes: List<ZenithPalette>,
    currentId: String,
    filter: String,
    onFilter: (String) -> Unit,
    onPick: (WallStyle, ZenithPalette) -> Unit,
    onPhotos: () -> Unit,
    onOtherApp: () -> Unit,
) {
    val palette = LocalPalette.current
    val shown = palettes.firstOrNull { it.id == filter } ?: palettes.first()
    Column(Modifier.verticalScroll(rememberScrollState())) {
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            palettes.forEach { option ->
                ThemeChip(option, selected = option.id == shown.id, yours = option.id == currentId) { onFilter(option.id) }
            }
        }
        Spacer(Modifier.height(14.dp))
        AnimatedContent(
            targetState = shown,
            transitionSpec = { fadeIn(tween(220, delayMillis = 60)).togetherWith(fadeOut(tween(120))) },
            label = "wallpaperTheme",
        ) { theme ->
            Column {
                Caption(if (theme.id == currentId) "Made to match your theme" else "In ${theme.name}'s colours")
                Spacer(Modifier.height(10.dp))
                WallStyle.entries.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { style ->
                            Thumb(style, theme, Modifier.weight(1f)) { onPick(style, theme) }
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
    isCurrentTheme: Boolean,
    onApplied: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf("home") }
    var match by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            WallpaperImage(
                style = style,
                palette = palette,
                widthPx = 540,
                modifier = Modifier
                    .height(360.dp)
                    .aspectRatio(0.46f)
                    .clip(RoundedCornerShape(28.dp))
                    .border(1.dp, LocalPalette.current.onGlass.copy(alpha = 0.16f), RoundedCornerShape(28.dp)),
            )
        }
        Spacer(Modifier.height(14.dp))
        Segmented(
            options = listOf("home" to "Home screen", "both" to "Home and lock"),
            selected = target,
            onSelect = { target = it },
        )
        if (!isCurrentTheme) {
            Spacer(Modifier.height(10.dp))
            Group {
                ToggleRow(
                    title = "Use ${palette.name} in Zenith too",
                    subtitle = "Glass, buttons and accents to match",
                    checked = match,
                    onChange = { match = it },
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            text = if (busy) "Setting…" else "Set wallpaper",
            onClick = {
                if (busy) return@PrimaryButton
                busy = true
                scope.launch {
                    val ok = WallpaperArt.apply(
                        context = context,
                        style = style,
                        colors = palette.wallColors(),
                        seed = seedFor(style, palette),
                        lockToo = target == "both",
                    )
                    busy = false
                    if (ok) {
                        onApplied(if (!isCurrentTheme && match) palette.id else null)
                        Toast.makeText(context, "Wallpaper set", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    } else {
                        Toast.makeText(context, "Android would not set that wallpaper", Toast.LENGTH_SHORT).show()
                    }
                }
            },
        )
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun ThemeChip(option: ZenithPalette, selected: Boolean, yours: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val fill by animateColorAsState(
        if (selected) palette.onGlass.copy(alpha = 0.16f) else Color.Transparent,
        tween(200),
        label = "themeChip",
    )
    Row(
        Modifier
            .height(38.dp)
            .pressBounce(0.94f)
            .clip(RoundedCornerShape(50))
            .background(fill)
            .border(1.dp, palette.onGlass.copy(alpha = if (selected) 0f else 0.16f), RoundedCornerShape(50))
            .clickable(onClick = onClick)
            .padding(start = 8.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(option.accent, option.accentAlt)))
        )
        Spacer(Modifier.width(8.dp))
        Text(
            if (yours) "${option.name} (yours)" else option.name,
            color = palette.onGlass,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

@Composable
private fun Thumb(style: WallStyle, palette: ZenithPalette, modifier: Modifier, onClick: () -> Unit) {
    val ui = LocalPalette.current
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        WallpaperImage(
            style = style,
            palette = palette,
            widthPx = 240,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.46f)
                .pressBounce(0.94f)
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, ui.onGlass.copy(alpha = 0.12f), RoundedCornerShape(16.dp))
                .clickable(onClick = onClick),
        )
        Spacer(Modifier.height(5.dp))
        Text(style.label, color = ui.onGlassMuted, fontSize = 12.sp)
    }
}

/**
 * One painted wallpaper, drawn off the main thread and kept, so scrolling back past it is
 * instant. It fades in once ready rather than popping.
 */
@Composable
private fun WallpaperImage(style: WallStyle, palette: ZenithPalette, widthPx: Int, modifier: Modifier) {
    val ui = LocalPalette.current
    val heightPx = (widthPx / 0.46f).toInt()
    val colors = palette.wallColors()
    val key = "${style.name}|${palette.id}|$colors|$widthPx"
    val image by produceState(initialValue = thumbCache.get(key), key) {
        if (value == null) {
            value = withContext(Dispatchers.Default) {
                WallpaperArt.render(style, colors, widthPx, heightPx, seedFor(style, palette)).asImageBitmap()
            }.also { thumbCache.put(key, it) }
        }
    }
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

private val thumbCache = LruCache<String, ImageBitmap>(120)

/** The same design in the same theme is always the same picture, thumbnail or full size. */
private fun seedFor(style: WallStyle, palette: ZenithPalette): Int = style.ordinal * 7919 + palette.id.hashCode()

private fun ZenithPalette.wallColors(): WallColors = WallColors(
    accent = accent.toArgb(),
    accentAlt = accentAlt.toArgb(),
    deep = backdrop.first().toArgb(),
    mid = backdrop.getOrElse(1) { backdrop.first() }.toArgb(),
    base = backdrop.last().toArgb(),
)
