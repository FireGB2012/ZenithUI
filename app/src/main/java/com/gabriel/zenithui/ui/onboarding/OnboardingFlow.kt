package com.gabriel.zenithui.ui.onboarding

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.RoundedCorner
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.gabriel.zenithui.ZenithViewModel
import com.gabriel.zenithui.data.GridDensity
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.LayoutBuilder
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.data.findActivity
import com.gabriel.zenithui.ui.common.rememberAnimationsEnabled
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material3.Text
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.draw.shadow
import androidx.compose.runtime.CompositionLocalProvider
import com.gabriel.zenithui.ui.theme.LocalApple
import com.gabriel.zenithui.ui.theme.elevated
import com.gabriel.zenithui.ui.theme.CardKind

/**
 * Onboarding is a form, not a canvas: past this width the eye has to travel too far between a
 * label and its control, so the column stays phone-sized and centres itself on bigger screens.
 */
private val FORM_MAX_WIDTH = 480.dp

/** How long each thing takes to click into place while the home screen is being put together. */
private const val ASSEMBLE_STEP_MS = 110L

/** Keeps the widget picks across the activity being rebuilt, as rememberSaveable does for the rest. */
private val WIDGET_LIST_SAVER = listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { it.toMutableStateList() },
)

internal object Steps {
    const val HELLO = 0
    const val NAME = 1
    const val LOOK = 2
    const val HOME = 3
    const val BUILD = 4
    const val PLUS = 5
    const val TIPS = 6
}

/** The three places you can be: the greeting, your name, and the stage with your phone on it. */
private object Phase {
    const val HELLO = 0
    const val NAME = 1
    const val STAGE = 2
}

/**
 * First run: a hello, your name, a colour, what goes on the page, and then the page itself being
 * put together in front of you — before it grows to fill the screen and becomes your home screen.
 * It all happens on a plain page, as an iPhone's own setup does: white in the light, black in the
 * dark. The button at the bottom never moves; only what is above it changes, so it feels like one
 * place rather than a stack of screens.
 */
@Composable
fun OnboardingFlow(
    viewModel: ZenithViewModel,
    backdrop: ImageBitmap?,
    screenSize: IntSize,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val animate = rememberAnimationsEnabled()
    val plus by viewModel.plus.collectAsState()
    val apps by viewModel.apps.collectAsState()
    val appsByKey = remember(apps) { apps.associateBy { it.key } }

    var step by rememberSaveable { mutableIntStateOf(Steps.HELLO) }
    var name by rememberSaveable { mutableStateOf("") }
    // Setup starts on the palette a new install has: Frost, Apple's blue.
    var paletteId by rememberSaveable { mutableStateOf(ZenithSettings().paletteId) }
    var appearance by rememberSaveable { mutableStateOf("dark") }
    var densityId by rememberSaveable { mutableStateOf(GridDensity.Balanced.id) }
    val widgets = rememberSaveable(saver = WIDGET_LIST_SAVER) { mutableStateListOf("clock", "toggles") }
    var built by rememberSaveable { mutableStateOf(false) }
    // Which hello was on screen, so the name screen can answer in the same language.
    var greeting by rememberSaveable { mutableIntStateOf(0) }
    var helloReady by rememberSaveable { mutableStateOf(false) }
    // Entrances play the first time a screen is seen; coming back to it, it is simply there.
    var helloSeen by rememberSaveable { mutableStateOf(false) }
    var nameSeen by rememberSaveable { mutableStateOf(false) }
    var lookSeen by rememberSaveable { mutableStateOf(false) }
    // Not saved: if the app is rebuilt mid hand-off, you land back on the tips and tap again.
    var finishing by remember { mutableStateOf(false) }

    // The grid you pick is a cell size; how many of them fit is the screen's business.
    val gridDensity = GridDensity.byId(densityId)
    val pixelsPerDp = LocalDensity.current.density
    val screenWidth = if (screenSize == IntSize.Zero) 411.dp else (screenSize.width / pixelsPerDp).dp
    val screenHeight = if (screenSize == IntSize.Zero) 891.dp else (screenSize.height / pixelsPerDp).dp
    val (columns, rows) = remember(gridDensity, screenWidth, screenHeight) {
        gridDensity.gridFor(screenWidth.value, screenHeight.value)
    }

    // The curve of this phone's own screen corners, so the preview is shaped like the real thing.
    val deviceCorner = remember(view, screenSize) {
        // Android 12 reports the screen's own corners; before that, a typical phone's.
        val radius = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.rootWindowInsets?.getRoundedCorner(RoundedCorner.POSITION_TOP_LEFT)?.radius ?: 0
        } else {
            0
        }
        if (radius > 0) (radius / pixelsPerDp).dp else 46.dp
    }

    val preview = ZenithSettings(
        userName = name,
        paletteId = paletteId,
        appearance = appearance,
        glass = 0.85f,
        columns = columns,
        rows = rows,
        // Choosing the search bar puts the bar above the dock, as it will on the real home screen.
        searchBar = "search" in widgets,
    )

    // The page the preview shows is laid out by the same code that builds the real one.
    val dock by produceState(emptyList<String>(), apps, columns) {
        value = withContext(Dispatchers.Default) { viewModel.repository.suggestedDock(LayoutBuilder.setupDockSize(columns)) }
    }
    val chosen = widgets.toList()
    val items by produceState(emptyList<HomeItem>(), apps, chosen, columns, rows, dock) {
        value = withContext(Dispatchers.Default) {
            LayoutBuilder.build(viewModel.repository, apps, chosen, columns, rows, dock)
        }
    }

    // The free week is only offered while it is true: not to someone who owns Plus, and not to
    // someone who has already had their week.
    val offerPlus = !plus.purchased && plus.trialAvailable

    // How many things are on the preview page. All of them, except while it is being put together.
    var shown by remember { mutableIntStateOf(Int.MAX_VALUE) }

    // Drawn, never composed: the band of light that sweeps across the phone when it is finished,
    // the fade of everything but the phone, and the phone growing into your home screen.
    val sweep = remember { Animatable(0f) }
    val chrome = remember { Animatable(1f) }
    val zoom = remember { Animatable(0f) }

    LaunchedEffect(step) {
        if (step != Steps.BUILD || built) return@LaunchedEffect
        val work = launch(Dispatchers.Default) {
            viewModel.buildHome(name, paletteId, appearance, chosen, gridDensity, columns, rows)
        }
        // Clear the page while the controls fold away and the phone grows into their space…
        shown = 0
        delay(520)
        // …then put it back together, one piece at a time, with a tick you can feel for each.
        val total = items.size + dock.size
        for (count in 1..total) {
            shown = count
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            delay(ASSEMBLE_STEP_MS)
        }
        delay(380)
        work.join()
        shown = Int.MAX_VALUE
        built = true
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
        )
        // Its own scope, so tapping on straight away does not cut the flourish off half way.
        scope.launch {
            sweep.snapTo(0f)
            sweep.animateTo(1f, tween(850, easing = FastOutSlowInEasing))
        }
    }

    // The hand-off: everything but the phone fades, the phone grows until its screen is the
    // screen, and the real home screen takes its place in the same frame it lands.
    LaunchedEffect(finishing) {
        if (!finishing) return@LaunchedEffect
        var committed = false
        fun commit() {
            if (committed) return
            committed = true
            viewModel.markOnboarded(entrance = false)
            onFinished()
        }
        if (!animate) {
            commit()
            return@LaunchedEffect
        }
        launch { chrome.animateTo(0f, tween(180)) }
        delay(60)
        zoom.animateTo(1f, spring(dampingRatio = 1f, stiffness = 170f)) {
            if (value >= 0.985f) commit()
        }
        commit()
    }

    fun leaveName() {
        keyboard?.hide()
        focusManager.clearFocus()
        nameSeen = true
        step = Steps.LOOK
    }

    fun back() {
        when (step) {
            Steps.NAME -> {
                keyboard?.hide()
                focusManager.clearFocus()
                step = Steps.HELLO
            }
            Steps.LOOK -> step = Steps.NAME
            Steps.HOME -> step = Steps.LOOK
            Steps.PLUS -> step = Steps.TIPS
            // Everywhere else back has nowhere to go: the greeting is the start, the build cannot
            // be un-built, and the hand-off is already under way.
        }
    }
    // Always taken. This is the home screen's activity, so an unhandled back would close it and
    // Android would start it again from scratch — setup included, with everything you picked lost.
    BackHandler { if (!finishing) back() }

    ZenithTheme(settings = preview, screenSize = screenSize) {
        val palette = LocalPalette.current

        // Setup owns the status bar until it is done: on a light palette the icons go dark, so
        // they do not vanish white on white.
        LaunchedEffect(palette.dark) {
            val window = context.findActivity()?.window ?: return@LaunchedEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !palette.dark
                isAppearanceLightNavigationBars = !palette.dark
            }
        }

        val apple = LocalApple.current
        val plusOpen = step == Steps.PLUS
        // If Plus is bought while the sheet is up (or turns out to be owned), the pitch just goes.
        LaunchedEffect(offerPlus) {
            if (!offerPlus && step == Steps.PLUS) step = Steps.TIPS
        }
        // With the Plus sheet up, the stage behind it steps back a little.
        val recede = animateFloatAsState(if (plusOpen) 0.94f else 1f, spring(dampingRatio = 0.92f, stiffness = 190f), label = "recede")

        Box(Modifier.fillMaxSize()) {
            // A plain page, as an iPhone's setup is. Over the last stretch of the hand-off it fades,
            // so the real wallpaper is already there when the phone lands as your home screen.
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = ((1f - zoom.value) / 0.4f).coerceIn(0f, 1f) }
                    .background(apple.plain)
            )

            Column(
                Modifier
                    .align(Alignment.TopCenter)
                    .widthIn(max = FORM_MAX_WIDTH)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
                    // With the Plus sheet up, TalkBack reads the sheet, not the stage hidden behind it.
                    .then(if (plusOpen) Modifier.clearAndSetSemantics { } else Modifier),
            ) {
                TopBar(
                    visible = step in Steps.NAME..Steps.HOME && !finishing,
                    progress = (step - Steps.NAME).coerceIn(0, 2),
                    onBack = { back() },
                )

                val phase = when (step) {
                    Steps.HELLO -> Phase.HELLO
                    Steps.NAME -> Phase.NAME
                    else -> Phase.STAGE
                }
                AnimatedContent(
                    targetState = phase,
                    transitionSpec = {
                        val forward = targetState > initialState
                        val arrive = spring<Float>(dampingRatio = 1f, stiffness = 260f)
                        when {
                            // Leaving the greeting, you move through it: it grows and fades past you.
                            initialState == Phase.HELLO && forward ->
                                (fadeIn(tween(320, delayMillis = 90)) + scaleIn(arrive, initialScale = 1.03f))
                                    .togetherWith(fadeOut(tween(280)) + scaleOut(tween(320, easing = FastOutLinearInEasing), targetScale = 1.06f))

                            forward ->
                                (fadeIn(tween(320, delayMillis = 90)) + scaleIn(arrive, initialScale = 1.03f))
                                    .togetherWith(fadeOut(tween(160)) + scaleOut(tween(200), targetScale = 0.98f))

                            else ->
                                (fadeIn(tween(320, delayMillis = 90)) + scaleIn(arrive, initialScale = 0.97f))
                                    .togetherWith(fadeOut(tween(160)) + scaleOut(tween(200), targetScale = 1.02f))
                        }.using(SizeTransform(clip = false))
                    },
                    label = "phase",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) { current ->
                    when (current) {
                        Phase.HELLO -> HelloStep(
                            startIndex = greeting,
                            firstTime = !helloSeen,
                            animate = animate,
                            // Frozen once you move on, so the name screen answers in the
                            // language you left on, not the one that came up as it faded.
                            onWord = { if (step == Steps.HELLO) greeting = it },
                            onReady = { helloReady = true },
                        )

                        Phase.NAME -> NameStep(
                            name = name,
                            onNameChange = { name = it },
                            onDone = { leaveName() },
                            greeting = greeting,
                            playIntro = !nameSeen,
                            canFocus = transition.currentState == transition.targetState,
                        )

                        else -> Box(
                            Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = recede.value
                                    scaleY = recede.value
                                },
                        ) {
                            StudioStep(
                                step = step,
                                built = built,
                                name = name,
                                settings = preview,
                                items = items,
                                dock = dock,
                                appsByKey = appsByKey,
                                wallpaper = backdrop,
                                screenWidth = screenWidth,
                                screenHeight = screenHeight,
                                shown = shown,
                                selectedWidgets = chosen,
                                density = gridDensity,
                                playIntro = !lookSeen,
                                deviceCorner = deviceCorner,
                                onPalette = { id -> paletteId = id },
                                onAppearance = { mode -> appearance = mode },
                                onToggleWidget = { type -> if (type in widgets) widgets.remove(type) else widgets.add(type) },
                                onDensity = { densityId = it.id },
                                sweep = { sweep.value },
                                zoom = { zoom.value },
                                chrome = { chrome.value },
                            )
                        }
                    }
                }

                // The one place the next move always is. It steps aside only while the home
                // screen is being put together, when there is nothing to press.
                AnimatedVisibility(
                    visible = step != Steps.BUILD || built,
                    enter = fadeIn(tween(260)) + expandVertically(spring(dampingRatio = 1f, stiffness = 300f)),
                    exit = fadeOut(tween(140)) + shrinkVertically(spring(dampingRatio = 1f, stiffness = 300f)),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .padding(top = 8.dp, bottom = 16.dp)
                            // Alpha only for the hand-off: collapsing would move the phone mid-zoom.
                            .graphicsLayer { alpha = chrome.value },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        PrimaryButton(
                            text = when (step) {
                                Steps.HELLO -> "Let's go"
                                Steps.NAME -> if (name.isBlank()) "Skip" else "Continue"
                                Steps.LOOK -> "Continue"
                                Steps.HOME -> "Build my home screen"
                                Steps.BUILD, Steps.PLUS -> "Continue"
                                else -> "Take me home"
                            },
                            onClick = {
                                when (step) {
                                    Steps.HELLO -> if (helloReady) {
                                        helloSeen = true
                                        step = Steps.NAME
                                    } else {
                                        helloReady = true
                                    }

                                    Steps.NAME -> leaveName()
                                    Steps.LOOK -> {
                                        lookSeen = true
                                        step = Steps.HOME
                                    }

                                    Steps.HOME -> step = Steps.BUILD
                                    Steps.BUILD -> if (built) step = if (offerPlus) Steps.PLUS else Steps.TIPS
                                    // Only reachable if the sheet went away on its own.
                                    Steps.PLUS -> step = Steps.TIPS
                                    else -> if (!finishing) finishing = true
                                }
                            },
                            modifier = Modifier
                                .widthIn(max = 400.dp)
                                // The greeting gets a moment to itself before the button arrives;
                                // its slot is kept, so nothing shifts when it does.
                                .reveal(step != Steps.HELLO || helloReady),
                        )
                    }
                }
            }

            // Zenith Plus, as a sheet over the finished phone rather than a page of its own.
            AnimatedVisibility(
                visible = plusOpen,
                enter = fadeIn(tween(240)),
                exit = fadeOut(tween(240)),
            ) {
                Box(
                    Modifier
                        .fillMaxSize()
                        // The page dims as it does behind every sheet.
                        .background(Color.Black.copy(alpha = 0.35f))
                        // Takes the touches, so the button underneath cannot be pressed through it.
                        .pointerInput(Unit) { detectTapGestures { } },
                )
            }
            AnimatedVisibility(
                visible = plusOpen,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(spring(dampingRatio = 1f, stiffness = 300f, visibilityThreshold = IntOffset(1, 1))) { it } +
                    fadeIn(tween(160)),
                exit = slideOutVertically(tween(240, easing = FastOutLinearInEasing)) { it } + fadeOut(tween(240)),
            ) {
                // On the sheet, Apple's colours a step up: the page under it is the plain one.
                CompositionLocalProvider(LocalApple provides apple.elevated()) {
                    PlusSheet(
                        status = plus,
                        onTry = {
                            // The sheet is still under your finger as it slides away: answer once.
                            if (step == Steps.PLUS) {
                                step = Steps.TIPS
                                // Play shows its own sheet once the trial subscription exists; until
                                // then the week is counted on the device.
                                viewModel.startTrialViaPlayOrLocally(context.findActivity())
                            }
                        },
                        onNotNow = { if (step == Steps.PLUS) step = Steps.TIPS },
                        modifier = Modifier
                            .widthIn(max = FORM_MAX_WIDTH)
                            .fillMaxWidth()
                            .heightIn(max = screenHeight * 0.88f)
                            // Solid, as a sheet over a page of its own is: glass would be a hole through
                            // the page to the wallpaper.
                            .shadow(16.dp, CardKind.Sheet.shape, clip = false)
                            .clip(CardKind.Sheet.shape)
                            // Apple's grouped grey a step up, so the cards on it stand off it.
                            .background(apple.elevated().page)
                            .semantics {
                                paneTitle = "Zenith Plus"
                                isTraversalGroup = true
                            }
                            .navigationBarsPadding(),
                    )
                }
            }

            // Nothing takes a touch while the phone is growing into your home screen.
            if (finishing) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            awaitPointerEventScope {
                                while (true) awaitPointerEvent().changes.forEach { it.consume() }
                            }
                        }
                )
            }
        }
    }
}

@Composable
private fun TopBar(visible: Boolean, progress: Int, onBack: () -> Unit) {
    val accent = LocalPalette.current.accent
    // Always the same height, shown or not, so nothing below it shifts between steps.
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.heightIn(min = 48.dp), contentAlignment = Alignment.CenterStart) {
            // Called by its full name: inside this Row the Row-scoped overload would be picked.
            androidx.compose.animation.AnimatedVisibility(visible = visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(140))) {
                // Apple's Back: a chevron and the word in the accent, dimming while it is held.
                val touch = remember { MutableInteractionSource() }
                val pressed by touch.collectIsPressedAsState()
                Row(
                    Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(interactionSource = touch, indication = null, role = Role.Button, onClick = onBack)
                        .graphicsLayer { alpha = if (pressed) 0.35f else 1f }
                        .padding(start = 2.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = null, tint = accent, modifier = Modifier.size(20.dp))
                    Text("Back", color = accent, fontSize = 17.sp)
                }
            }
        }
        Spacer(Modifier.weight(1f))
        androidx.compose.animation.AnimatedVisibility(visible = visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            StepMarks(current = progress, total = 3, modifier = Modifier.padding(end = 14.dp))
        }
    }
}
