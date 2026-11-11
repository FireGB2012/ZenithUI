package com.gabriel.zenithui.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.AppSearch
import com.gabriel.zenithui.data.AppShortcut
import com.gabriel.zenithui.data.GlassLevels
import com.gabriel.zenithui.data.GlassTuning
import com.gabriel.zenithui.data.SettingsLinks
import com.gabriel.zenithui.ui.common.LocalLabelStyle
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.labelShadow
import com.gabriel.zenithui.ui.common.popIn
import com.gabriel.zenithui.ui.common.popOut
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.symbolShadowColour
import com.gabriel.zenithui.ui.theme.GlassLook
import com.gabriel.zenithui.ui.theme.GlassMaterial
import com.gabriel.zenithui.ui.theme.LiquidGlassShader
import com.gabriel.zenithui.ui.theme.LocalBackdrop
import com.gabriel.zenithui.ui.theme.LocalGlassBody
import com.gabriel.zenithui.ui.theme.LocalGlassLevels
import com.gabriel.zenithui.ui.theme.LocalGlassStrength
import com.gabriel.zenithui.ui.theme.LocalGlassTuning
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalScreenSize
import com.gabriel.zenithui.ui.theme.LocalTilt
import com.gabriel.zenithui.ui.theme.LocalWallColour
import com.gabriel.zenithui.ui.theme.LocalWallpaperOffset
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.WALLPAPER_VEIL
import com.gabriel.zenithui.ui.theme.WALL_TINT
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.drawLiquidGlass
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.glassLook
import com.gabriel.zenithui.ui.theme.glowColour
import com.gabriel.zenithui.ui.theme.glowWith
import com.gabriel.zenithui.ui.theme.shadowPaints
import com.gabriel.zenithui.ui.theme.sharedLens
import com.gabriel.zenithui.ui.theme.tuned
import com.gabriel.zenithui.ui.theme.wallUnder
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Search out of the pill, the home screen's third twist. Tapping the Search pill stretches its glass
 * into a wide field that rides up on the keyboard, the home screen dimming behind it (and softening:
 * the home screen blurs itself, on Android 12 and up). What is typed finds apps by their names
 * ([AppSearch]), then the answers the drawer gives (a sum, a conversion, a setting, a shortcut), then
 * a search of the web. Back, a tap outside it, or the keyboard put away shrinks it back into the
 * pill with a small wobble. With movement reduced, or with no pill on the screen to come out of, it
 * fades in and out where it sits instead.
 *
 * [open] says which way it is going, and [from] is where the pill is, in the window's pixels. Its
 * glass is painted with the lens at [pillToField] of the way between the two, on a spring that
 * carries it a little past the field as it opens and a little into the pill as it closes, the way
 * the card an app opens in is painted: a pill stretched by a layer would have a thicker rim, bent
 * corners and a magnified wallpaper. At either end it is the pill's own glass to the pixel, so the
 * pill is not drawn while this is ([onShrunk] says when it may be again).
 *
 * The field is the home screen's typing area while it has the keyboard ([HomeTypingArea]), as a
 * note or to-do widget's is: a note being typed in gives the keyboard up to it, and the home screen
 * puts the keyboard away only for a touch outside the field and its results. The field is only ever
 * drawn on the home screen, over the wallpaper, so its words are white with the names' soft shadow
 * in Light and in Dark, and its glass is the wallpaper's, the same in both.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchField(
    open: Boolean,
    from: Rect,
    apps: List<AppInfo>,
    shortcuts: List<AppShortcut>,
    onOpenApp: (String) -> Unit,
    onClose: () -> Unit,
    /** One of Android's settings, or Zenith's own, found by its name. */
    onSetting: (SettingsLinks.Link) -> Unit = {},
    /** A sum's answer or a conversion, tapped to copy it. */
    onCopy: (String) -> Unit = {},
    /** The glass is back in the pill and nothing of the field is drawn: the pill can show again. */
    onShrunk: () -> Unit = {},
    /** The shape the home screen's icons are cut to, for the apps it finds. */
    iconShape: Shape = ZenithShapes.icon,
) {
    val reduced = LocalMotionLevel.current.reduced
    // Out of the pill and back into it; without a pill to come out of, or with movement reduced, a fade.
    val stretch = !reduced && !from.isEmpty
    // Settling finely, so the glass's last step into the pill is too small to see as the pill takes over.
    val progress = remember { Animatable(0f, visibilityThreshold = 0.001f) }
    var query by remember { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val closing by rememberUpdatedState(onClose)
    val shrunk by rememberUpdatedState(onShrunk)

    // Where the field is, in the window, for the glass to stretch to; and where this is drawn.
    var fieldAt by remember { mutableStateOf(Rect.Zero) }
    var origin by remember { mutableStateOf(Offset.Zero) }
    // The field and its results, where the home screen leaves the keyboard up for a touch.
    val panel = remember { arrayOf(Rect.Zero) }
    var typing by remember { mutableStateOf(false) }
    // A field taken off the screen while it has the keyboard must not leave its area behind.
    DisposableEffect(Unit) { onDispose { if (typing) HomeTypingArea.bounds = null } }

    LaunchedEffect(open) {
        if (open) {
            // A frame first, so the text field is in place to take the keyboard; it comes up as the
            // glass stretches.
            withFrameNanos { }
            runCatching { focus.requestFocus() }
            progress.animateTo(1f, if (stretch) STRETCH else tween(Motion.REDUCED_MS))
        } else {
            runCatching { focusManager.clearFocus(force = true) }
            progress.animateTo(0f, if (stretch) SHRINK else tween(Motion.REDUCED_MS))
            shrunk()
        }
    }

    // The keyboard put away while the field is open, by Back (which the keyboard takes first) or by
    // its own button, puts the search away too, as it starts to go rather than once it has gone. It
    // has to have come up for the field first: a keyboard still going down from a note as this
    // opens, or going down and up again as the note hands it over, is not it.
    val keyboardTarget = WindowInsets.imeAnimationTarget
    LaunchedEffect(open) {
        if (!open) return@LaunchedEffect
        var cameUp = false
        snapshotFlow { typing to (keyboardTarget.getBottom(density) > 0) }.collect { (ours, up) ->
            if (ours && up) cameUp = true else if (!up && cameUp) closing()
        }
    }
    BackHandler(enabled = open) { closing() }

    // Once the field has faded on its way back into the pill, what is in it is taken away, so that
    // nothing of it takes the touches meant for the home screen while the glass finishes shrinking.
    val faded by remember(stretch) { derivedStateOf { progress.value < if (stretch) CONTENT_GONE else 0.02f } }
    val live = open || !faded

    val levels = LocalGlassLevels.current
    val backdrop = LocalBackdrop.current
    val screen = LocalScreenSize.current
    val wallpaperOffset = LocalWallpaperOffset.current
    val tilt = LocalTilt.current
    val tuning = LocalGlassTuning.current
    val savedWall = LocalWallColour.current
    val strength = LocalGlassStrength.current.coerceIn(0f, 1.4f)
    val body = LocalGlassBody.current
    // The lens every pane on the home screen shares: the pill's own glass, stretched.
    val lens = sharedLens(levels)
    val paint = remember { FieldPaint() }
    val shadow = labelShadow(onWallpaper = true, legacyShadow = LocalLabelStyle.current.shadow, density = density.density)
    // The pill's own magnifier and word, set as the pill sets them, painted in the middle of the glass
    // as it leaves the pill and fading as it stretches, and coming back as it shrinks: without them
    // the words would blink out the moment the pill was tapped, and in again as it reappeared.
    val measurer = rememberTextMeasurer()
    val pillWordsStyle = LocalTextStyle.current
        .merge(TextStyle(shadow = shadow))
        .merge(TextStyle(color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold))
    val pillWords = remember(measurer, pillWordsStyle) { measurer.measure("Search", pillWordsStyle, maxLines = 1) }
    val magnifier = rememberVectorPainter(Icons.Rounded.Search)

    // Its words stand on the wallpaper, as the pill's do: white, with the names' shadow, in Light and in Dark.
    OnWallpaper {
        CompositionLocalProvider(
            LocalPalette provides LocalPalette.current.copy(dark = true),
            LocalTextStyle provides LocalTextStyle.current.merge(TextStyle(shadow = shadow)),
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    // While it is open nothing under it answers a finger, and a tap on none of it is a tap
                    // outside it.
                    .then(if (open) Modifier.pointerInput(Unit) { detectTapGestures { closing() } } else Modifier),
            ) {
                Canvas(Modifier.fillMaxSize().onGloballyPositioned { origin = it.positionInWindow() }) {
                    // Read here, while drawing: the spring, where the field is and the light all move
                    // the glass without anything being composed again.
                    val p = progress.value
                    val shown = p.coerceIn(0f, 1f)
                    if (shown > 0f) drawRect(Color.Black.copy(alpha = SEARCH_DIM * shown))
                    // Until the field has been placed the glass is still the pill.
                    val field = fieldAt.takeUnless { it.isEmpty } ?: from
                    val at = if (stretch) pillToField(p.coerceIn(-OVERSHOOT, 1f + OVERSHOOT), from, field) else field
                    val look = glassLook(
                        material = GlassMaterial.Clear,
                        strength = strength,
                        // A little body as it opens, under the words typed into it.
                        solid = FIELD_BODY * shown,
                        opacity = if (stretch) 1f else shown,
                        dark = true,
                        tint = WALLPAPER_VEIL,
                        depth = body,
                        onWallpaper = true,
                    ).tuned(tuning).copy(wallTint = WALL_TINT)
                    drawFieldGlass(
                        at = at,
                        origin = origin,
                        look = look,
                        paint = paint,
                        lens = lens,
                        softened = backdrop,
                        levels = levels,
                        screen = screen,
                        wallpaperOffset = wallpaperOffset.value,
                        savedWall = savedWall,
                        lean = tilt.value,
                        tuning = tuning,
                    )
                    if (stretch) {
                        val words = (1f - p / PILL_WORDS_GONE).coerceIn(0f, 1f)
                        if (words > 0.004f) drawPillWords(at.center - origin, words, pillWords, magnifier, shadow)
                    }
                }

                Column(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .onGloballyPositioned { coords ->
                            panel[0] = coords.boundsInRoot()
                            if (typing) HomeTypingArea.bounds = panel[0]
                        },
                ) {
                    if (live) {
                        SearchResults(
                            query = query,
                            apps = apps,
                            shortcuts = shortcuts,
                            enabled = open,
                            iconShape = iconShape,
                            onOpenApp = onOpenApp,
                            onSetting = onSetting,
                            onCopy = onCopy,
                            onWeb = {
                                webSearch(context, query.trim())
                                closing()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .graphicsLayer { alpha = resultsShown(progress.value, stretch) },
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                    Box(
                        Modifier
                            .padding(horizontal = FIELD_SIDE)
                            .fillMaxWidth()
                            .height(FIELD_HEIGHT)
                            .onGloballyPositioned { fieldAt = it.boundsInWindow() }
                            // A tap anywhere on the field is a tap in it, not outside it: the keyboard
                            // comes back up.
                            .then(if (live) Modifier.pointerInput(Unit) { detectTapGestures { runCatching { focus.requestFocus() } } } else Modifier)
                            .graphicsLayer { alpha = wordsShown(progress.value, stretch) },
                    ) {
                        if (live) Row(
                            Modifier
                                .fillMaxSize()
                                .padding(start = 16.dp, end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ShadowedSymbol(Icons.Rounded.Search, 18.dp, shadow)
                            Spacer(Modifier.width(10.dp))
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it.replace("\n", "") },
                                enabled = open,
                                singleLine = true,
                                maxLines = 1,
                                textStyle = TextStyle(
                                    fontFamily = LocalZenithFont.current,
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    shadow = shadow,
                                ),
                                cursorBrush = SolidColor(LocalPalette.current.accent),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                                keyboardActions = KeyboardActions(
                                    onGo = {
                                        // The app it found first, as the drawer's Go opens; with no app
                                        // found, the web. An empty field opens nothing.
                                        val first = AppSearch.go(query, apps)
                                        when {
                                            first != null -> onOpenApp(first.key)
                                            query.isNotBlank() -> {
                                                webSearch(context, query.trim())
                                                closing()
                                            }
                                        }
                                    },
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focus)
                                    .onFocusChanged { state ->
                                        val was = typing
                                        typing = state.isFocused
                                        // Told it is not focused as it first appears, it leaves alone the
                                        // area of a note still being typed in: only its own is cleared.
                                        if (state.isFocused) HomeTypingArea.bounds = panel[0] else if (was) HomeTypingArea.bounds = null
                                    },
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        // The pill's own word, where it was going to be typed.
                                        if (query.isEmpty()) {
                                            Text(
                                                "Search",
                                                color = LocalPalette.current.onGlassMuted,
                                                fontSize = 17.sp,
                                                maxLines = 1,
                                            )
                                        }
                                        inner()
                                    }
                                },
                            )
                            // Something typed: a way to clear it opens out beside it.
                            AnimatedVisibility(
                                visible = query.isNotEmpty(),
                                enter = popIn(reduced) + expandHorizontally(),
                                exit = popOut(reduced) + shrinkHorizontally(),
                            ) {
                                Box(
                                    Modifier
                                        .size(28.dp)
                                        .pressBounce(0.85f)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.18f))
                                        .clickable(enabled = open, role = Role.Button) { query = "" },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.White, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                    // Under the field: the keyboard as it comes up and goes, or with it down, room to put
                    // the field's middle where the pill's is.
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .keyboardRoom(
                                keyboard = WindowInsets.ime,
                                bars = WindowInsets.navigationBars,
                                pillMiddle = if (from.isEmpty || screen.height <= 0) 0 else (screen.height - from.center.y).roundToInt(),
                            ),
                    )
                }
            }
        }
    }
}

/**
 * What the search finds for [query], from the top: the apps whose names have it in them, four to a
 * row as on the home screen; then the answers the drawer gives; then a way to look for it on the web.
 * Nothing while the field is empty, which leaves the home screen softened behind it.
 */
@Composable
private fun SearchResults(
    query: String,
    apps: List<AppInfo>,
    shortcuts: List<AppShortcut>,
    /** False while the field shrinks back into the pill: nothing in it is chosen on its way out. */
    enabled: Boolean,
    iconShape: Shape,
    onOpenApp: (String) -> Unit,
    onSetting: (SettingsLinks.Link) -> Unit,
    onCopy: (String) -> Unit,
    onWeb: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val words = query.trim()
    val found = remember(words, apps) { AppSearch.match(words, apps).chunked(RESULT_COLUMNS) }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = FIELD_SIDE, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        userScrollEnabled = enabled,
    ) {
        if (words.isEmpty()) return@LazyColumn
        items(found, key = { row -> row.first().key }) { row ->
            Row(Modifier.fillMaxWidth()) {
                for (app in row) {
                    Box(
                        Modifier
                            .weight(1f)
                            .clickable(interactionSource = null, indication = null, enabled = enabled, role = Role.Button) { onOpenApp(app.key) },
                        contentAlignment = Alignment.Center,
                    ) {
                        AppTile(
                            key = app.key,
                            label = app.label,
                            iconSize = RESULT_ICON,
                            showLabel = true,
                            shape = iconShape,
                            badge = true,
                        )
                    }
                }
                // A row that is not full keeps its icons under the ones above.
                repeat(RESULT_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
        item(key = "answers") {
            SearchAnswers(
                query = query,
                shortcuts = shortcuts,
                onCopy = onCopy,
                onSetting = onSetting,
                onShortcut = { onOpenApp(it.key) },
                // The apps are above them here, not below as in the drawer.
                appsBelow = false,
            )
        }
        item(key = "web") {
            Row(
                Modifier
                    .fillMaxWidth()
                    .pressBounce(0.97f)
                    .glass(ZenithShapes.pill, material = GlassMaterial.Clear)
                    .clickable(enabled = enabled, onClick = onWeb)
                    .padding(horizontal = 18.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.TravelExplore, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Search the web for “$words”",
                    color = LocalPalette.current.onGlass,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * A white symbol with the names' [shadow] under it, as the pill's magnifier has: a copy of the symbol
 * in the shadow's colour, as far down as the shadow falls, since a symbol cannot carry a text shadow.
 */
@Composable
private fun ShadowedSymbol(symbol: ImageVector, size: Dp, shadow: Shadow?) {
    Box {
        if (shadow != null) {
            Icon(
                symbol,
                contentDescription = null,
                tint = symbolShadowColour(shadow),
                modifier = Modifier
                    .size(size)
                    .graphicsLayer {
                        translationX = shadow.offset.x
                        translationY = shadow.offset.y
                    },
            )
        }
        Icon(symbol, contentDescription = null, tint = Color.White, modifier = Modifier.size(size))
    }
}

/**
 * Under the field: the keyboard as it comes up and goes down, with a little air above it; with the
 * keyboard down, room enough to put the field's middle where the pill's is, [pillMiddle] pixels above
 * the foot of the screen, so the glass grows out of the pill to either side rather than first dipping
 * towards the dock; and never less than the gesture bar needs. The insets are read as this is
 * measured, so the keyboard moving it measures it again and composes nothing.
 */
private fun Modifier.keyboardRoom(keyboard: WindowInsets, bars: WindowInsets, pillMiddle: Int): Modifier =
    layout { measurable, constraints ->
        val air = FIELD_GAP.roundToPx()
        val underPill = pillMiddle - FIELD_HEIGHT.roundToPx() / 2
        val height = maxOf(keyboard.getBottom(this) + air, bars.getBottom(this) + air, underPill)
            .coerceIn(0, constraints.maxHeight)
        val placeable = measurable.measure(Constraints.fixed(constraints.maxWidth, height))
        layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }

/**
 * What the field's glass is drawn with, made once and used again on every frame: its outline for the
 * glow and the shadows, its clip, and the shadows themselves, made again only when the glass changes
 * size or fades.
 */
private class FieldPaint {
    val outline = android.graphics.Path()
    val clip = Path()
    val glow = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
    private var shadowsFor = Size.Zero
    private var shadowsAt = -1f
    private var shadows: List<android.graphics.Paint> = emptyList()

    /** The two shadows a pane of [size] casts ([shadowPaints]), as the pill's own glass casts them. */
    fun shadows(look: GlassLook, size: Size, density: Float): List<android.graphics.Paint> {
        if (size != shadowsFor || look.opacity != shadowsAt) {
            shadows = shadowPaints(look, GlassMaterial.Clear, dark = true, density = density, pane = size)
            shadowsFor = size
            shadowsAt = look.opacity
        }
        return shadows
    }
}

/**
 * The field's glass at [at], in the window, from a canvas whose top-left is at [origin]: the glow and
 * the shadows outside it, then the pane, exactly as `glass()` draws the pill, so that at either end of
 * the stretch it is the pill.
 */
private fun DrawScope.drawFieldGlass(
    at: Rect,
    origin: Offset,
    look: GlassLook,
    paint: FieldPaint,
    lens: LiquidGlassShader?,
    softened: ImageBitmap?,
    levels: GlassLevels?,
    screen: IntSize,
    wallpaperOffset: Offset,
    savedWall: Color,
    lean: Offset,
    tuning: GlassTuning,
) {
    val pane = at.size
    if (pane.width < 1f || pane.height < 1f || look.opacity <= 0f) return
    val left = at.left - origin.x
    val top = at.top - origin.y
    // A capsule, as the pill is: its ends half as round as it is tall.
    val radius = min(pane.width, pane.height) / 2f
    // The wallpaper's colour under it, for its glow, and for its veil where there is no lens.
    val wall = wallUnder(levels, at.topLeft, pane, screen, wallpaperOffset, savedWall)
    paint.outline.rewind()
    paint.outline.addRoundRect(left, top, left + pane.width, top + pane.height, radius, radius, android.graphics.Path.Direction.CW)
    drawIntoCanvas {
        val canvas = it.nativeCanvas
        canvas.save()
        // Only the glow and the shadows are wanted here: the pane itself is drawn next.
        canvas.clipOutPath(paint.outline)
        if (wall.isSpecified) {
            paint.glow.glowWith(glowColour(wall, dark = true), look.opacity, density)
            canvas.drawPath(paint.outline, paint.glow)
        }
        for (shade in paint.shadows(look, pane, density)) canvas.drawPath(paint.outline, shade)
        canvas.restore()
    }
    val shape = RoundRect(0f, 0f, pane.width, pane.height, CornerRadius(radius, radius))
    paint.clip.reset()
    paint.clip.addRoundRect(shape)
    translate(left, top) {
        clipPath(paint.clip) {
            drawLiquidGlass(
                paneSize = pane,
                outline = Outline.Rounded(shape),
                lens = lens,
                softened = softened,
                origin = at.topLeft,
                screen = screen,
                wallpaperOffset = wallpaperOffset,
                look = look,
                lean = lean,
                dark = true,
                tuning = tuning,
                wall = wall,
            )
        }
    }
}

/**
 * The pill's magnifier and word round [centre], [alpha] of them, as the pill lays them out: the
 * magnifier, a gap, the word, the three as one line in the middle. A symbol cannot carry a text
 * shadow, so the magnifier's is a copy of it underneath, as the pill's is; the word's shadow fades
 * with it, which the shadow its style carries would not.
 */
private fun DrawScope.drawPillWords(centre: Offset, alpha: Float, words: TextLayoutResult, magnifier: VectorPainter, shadow: Shadow?) {
    val icon = PILL_ICON.toPx()
    val gap = PILL_GAP.toPx()
    val left = centre.x - (icon + gap + words.size.width) / 2f
    val iconTop = centre.y - icon / 2f
    if (shadow != null) {
        translate(left + shadow.offset.x, iconTop + shadow.offset.y) {
            with(magnifier) { draw(Size(icon, icon), alpha = alpha, colorFilter = ColorFilter.tint(symbolShadowColour(shadow))) }
        }
    }
    translate(left, iconTop) {
        with(magnifier) { draw(Size(icon, icon), alpha = alpha, colorFilter = ColorFilter.tint(Color.White)) }
    }
    drawText(
        textLayoutResult = words,
        topLeft = Offset(left + icon + gap, centre.y - words.size.height / 2f),
        alpha = alpha,
        shadow = shadow?.let { it.copy(color = it.color.copy(alpha = it.color.alpha * alpha)) },
    )
}

/** How much of the field's own words and cursor show, [p] of the way open: they come as the glass arrives. */
private fun wordsShown(p: Float, stretch: Boolean): Float =
    if (stretch) ((p - 0.55f) / 0.45f).coerceIn(0f, 1f) else p.coerceIn(0f, 1f)

/** How much of what the search found shows, [p] of the way open. */
private fun resultsShown(p: Float, stretch: Boolean): Float =
    if (stretch) ((p - CONTENT_GONE) / (1f - CONTENT_GONE)).coerceIn(0f, 1f) else p.coerceIn(0f, 1f)

/**
 * The glass on its way out of the pill: quick, and a little past the field before it settles. On its
 * way back the spring is a touch looser, so it wobbles once into the pill.
 */
private val STRETCH = spring<Float>(dampingRatio = 0.74f, stiffness = 340f)
private val SHRINK = spring<Float>(dampingRatio = 0.7f, stiffness = 400f)

/** How far past either end the spring may carry the glass: no further than looks like a wobble. */
private const val OVERSHOOT = 0.15f

/** How far open the field is when its results have faded and are taken away, on its way back. */
private const val CONTENT_GONE = 0.3f

/** How far open the glass is when the pill's words in it have faded, and how they are set, as in the pill. */
private const val PILL_WORDS_GONE = 0.3f
private val PILL_ICON = 16.dp
private val PILL_GAP = 6.dp

/** How dark the home screen goes behind the field, at most. */
private const val SEARCH_DIM = 0.45f

/** The body the field's glass gains as it opens, for the words typed into it. */
private const val FIELD_BODY = 0.12f

/** How tall the field is, how far in from the sides of the screen, and the air between it and the keyboard. */
private val FIELD_HEIGHT = 48.dp
private val FIELD_SIDE = 12.dp
private val FIELD_GAP = 8.dp

/** The apps found, four to a row as on the home screen, at the drawer's size. */
private const val RESULT_COLUMNS = 4
private val RESULT_ICON = STANDARD_ICON_SIZE
