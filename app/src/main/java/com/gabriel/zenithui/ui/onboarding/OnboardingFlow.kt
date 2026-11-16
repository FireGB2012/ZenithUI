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
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
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
import com.gabriel.zenithui.data.NewLook
import com.gabriel.zenithui.data.Themes
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.data.findActivity
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.common.rememberAnimationsEnabled
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithTheme
import com.gabriel.zenithui.ui.theme.glass
import com.gabriel.zenithui.ui.theme.palette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Onboarding is a form, not a canvas: past this width the eye has to travel too far between a
 * label and its control, so the column stays phone-sized and centres itself on bigger screens.
 */
private val FORM_MAX_WIDTH = 480.dp

/** How long each thing takes to click into place while the home screen is being put together. */
private const val ASSEMBLE_STEP_MS = 110L

/** How long "All set" at a hundred percent stays up before the finished phone takes its place. */
private const val ALL_SET_HOLD_MS = 450L

/** How long the work runs before setup offers to let it finish in the background. */
private const val SKIP_AFTER_MS = 3_000L

/** The celebration before the hand-off: how long the light has before the phone starts to grow. */
private const val CELEBRATE_MS = 260L

/** Keeps the widget picks across the activity being rebuilt, as rememberSaveable does for the rest. */
private val WIDGET_LIST_SAVER = listSaver<SnapshotStateList<String>, String>(
    save = { it.toList() },
    restore = { it.toMutableStateList() },
)

internal object Steps {
    const val HELLO = 0
    const val NAME = 1
    /** The theme, and light or dark. */
    const val LOOK = 2
    const val HOME = 3
    const val BUILD = 4
    /** "Optimizing Zenith": the work that makes the first home screen and drawer fast. */
    const val OPTIMIZE = 5
    const val PLUS = 6
    const val TIPS = 7
}

/** The three places you can be: the greeting, your name, and the stage with your phone on it. */
private object Phase {
    const val HELLO = 0
    const val NAME = 1
    const val STAGE = 2
}

/**
 * First run: a hello, your name, a theme and light or dark, what goes on the page, and then the
 * page itself being put together in front of you, Zenith readying everything it will need
 * ("Optimizing Zenith", real work with real progress), and a moment of light before it grows to
 * fill the screen and becomes your home screen. Soft light drifts behind all of it and follows you
 * from step to step. The button at the bottom never moves; only what is above it changes, so it
 * feels like one place rather than a stack of screens. Picking a theme turns all of setup into it.
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
    val glassLevels by viewModel.glassLevels.collectAsState()
    val reduced = !animate || LocalMotionLevel.current.reduced
    val appsByKey = remember(apps) { apps.associateBy { it.key } }

    var step by rememberSaveable { mutableIntStateOf(Steps.HELLO) }
    var name by rememberSaveable { mutableStateOf("") }
    var appearance by rememberSaveable { mutableStateOf("dark") }
    var themeId by rememberSaveable { mutableStateOf(Themes.DEFAULT) }
    // Setup starts on the new look's grid, the iPhone's, which it offers first.
    var densityId by rememberSaveable { mutableStateOf(GridDensity.Four.id) }
    val widgets = rememberSaveable(saver = WIDGET_LIST_SAVER) { mutableStateListOf("clock", "toggles") }
    var built by rememberSaveable { mutableStateOf(false) }
    var optimized by rememberSaveable { mutableStateOf(false) }
    // Offered once the work has run a while: it carries on in the background either way.
    var offerSkip by remember { mutableStateOf(false) }
    // Which hello was on screen, so the name screen can answer in the same language.
    var greeting by rememberSaveable { mutableIntStateOf(0) }
    var helloReady by rememberSaveable { mutableStateOf(false) }
    // Entrances play the first time a screen is seen; coming back to it, it is simply there.
    var helloSeen by rememberSaveable { mutableStateOf(false) }
    var nameSeen by rememberSaveable { mutableStateOf(false) }
    var lookSeen by rememberSaveable { mutableStateOf(false) }
    // Not saved: if the app is rebuilt mid hand-off, you land back on the tips and tap again.
    var celebrating by remember { mutableStateOf(false) }
    var finishing by remember { mutableStateOf(false) }
    val button = remember { Spot() }
    // Read once, before anything changes it: the sunrise is for the very first look only.
    val sunrise = remember { !helloSeen && step == Steps.HELLO }

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

    // The home screen setup builds, on the new look as building it saves it, so the preview is
    // that home screen and growing it into the real one changes nothing.
    val preview = NewLook.apply(
        ZenithSettings(
            userName = name,
            appearance = appearance,
            theme = themeId,
            glass = 0.85f,
            columns = columns,
            rows = rows,
            // As building it saves it, though the home screen no longer draws a bar for it.
            searchBar = "search" in widgets,
        )
    ).copy(gridDensity = gridDensity.id)
    // And its icons are drawn in the new look's shape from the first, as that home screen's are.
    LaunchedEffect(Unit) { viewModel.previewNewLook() }
    // Setup shows the theme being picked in it, not one that was being tried in Settings.
    LaunchedEffect(Unit) { Themes.trying.value = null }

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

    // Drawn, never composed: the light behind the phone, the band that sweeps across it when it is
    // finished, the fade of everything but the phone, and the phone growing into your home screen.
    val haloBoost = remember { Animatable(1f) }
    val sweep = remember { Animatable(0f) }
    val chrome = remember { Animatable(1f) }
    val zoom = remember { Animatable(0f) }

    LaunchedEffect(step) {
        if (step != Steps.BUILD || built) return@LaunchedEffect
        val work = launch(Dispatchers.Default) {
            viewModel.buildHome(name, appearance, chosen, gridDensity, columns, rows, theme = themeId)
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
        // Its own scope, so moving on straight away does not cut the flourish off half lit.
        scope.launch {
            haloBoost.animateTo(3f, tween(280))
            haloBoost.animateTo(1.2f, spring(dampingRatio = 1f, stiffness = 60f))
        }
        scope.launch {
            sweep.snapTo(0f)
            sweep.animateTo(1f, tween(850, easing = FastOutSlowInEasing))
        }
        // Straight on to the work that makes it fast, while the light is still crossing it.
        step = Steps.OPTIMIZE
    }

    // "Optimizing Zenith": the view model does the work, in its own time, and says how far it has
    // got; this only starts it and waits for it to be done.
    LaunchedEffect(step) {
        if (step != Steps.OPTIMIZE || optimized) return@LaunchedEffect
        viewModel.startOptimizing()
        viewModel.optimizing.first { it?.finished == true }
        // "All set", at a hundred percent, for long enough to be read.
        delay(ALL_SET_HOLD_MS)
        optimized = true
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
        )
        scope.launch {
            haloBoost.animateTo(2.2f, tween(240))
            haloBoost.animateTo(1.2f, spring(dampingRatio = 1f, stiffness = 60f))
        }
    }
    LaunchedEffect(step) {
        offerSkip = false
        if (step != Steps.OPTIMIZE) return@LaunchedEffect
        delay(SKIP_AFTER_MS)
        offerSkip = true
    }

    // The hand-off: everything but the phone fades, the phone grows until its screen is the
    // screen, and the real home screen takes its place in the same frame it lands. The glass of
    // setup goes with it: the light behind fades and the preview's own background dissolves, so
    // your wallpaper is already there when the home screen takes over.
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
    BackHandler { if (!finishing && !celebrating) back() }

    ZenithTheme(settings = preview, screenSize = screenSize) {
        val palette = LocalPalette.current
        // Whether Auto is dark just now, as ZenithTheme reads it, for the ring that choosing light
        // or dark sends out.
        val systemDark = isSystemInDarkTheme()

        // Setup owns the status bar until it is done: in Light the icons go dark, so they do not
        // vanish white on white.
        LaunchedEffect(palette.dark) {
            val window = context.findActivity()?.window ?: return@LaunchedEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !palette.dark
                isAppearanceLightNavigationBars = !palette.dark
            }
        }

        val clock = rememberBackdropClock(running = animate && !finishing)
        val pulse = rememberPulse()
        val scene = when (step) {
            Steps.HELLO -> BackdropScene.Hello
            Steps.NAME -> BackdropScene.Name
            // The lights gather behind the phone while it is built and readied.
            Steps.BUILD -> BackdropScene.Build
            Steps.OPTIMIZE -> if (optimized) BackdropScene.Stage else BackdropScene.Build
            Steps.PLUS -> BackdropScene.Plus
            else -> BackdropScene.Stage
        }
        val restHalo = if (palette.dark) 0.18f else 0.12f
        val plusOpen = step == Steps.PLUS
        // If Plus is bought while the sheet is up (or turns out to be owned), the pitch just goes.
        LaunchedEffect(offerPlus) {
            if (!offerPlus && step == Steps.PLUS) step = Steps.TIPS
        }
        // With the Plus sheet up, the stage behind it steps back a little.
        val recede = animateFloatAsState(if (plusOpen) 0.94f else 1f, spring(dampingRatio = 0.92f, stiffness = 190f), label = "recede")

        Box(Modifier.fillMaxSize()) {
            OnboardingBackdrop(
                scene = scene,
                clock = clock,
                pulse = pulse,
                sunrise = sunrise,
                modifier = Modifier.graphicsLayer { alpha = ((1f - zoom.value) / 0.4f).coerceIn(0f, 1f) },
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
                            clock = clock,
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
                            onGreeted = { origin -> scope.launch { pulse.fire(origin, palette.accent) } },
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
                                optimized = optimized,
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
                                // Handed down as it is, so only the card reads it: it changes with
                                // every icon, and the rest of setup has no need to hear of each one.
                                optimizing = viewModel.optimizing,
                                glassLevels = glassLevels,
                                appearanceDark = preview.isDark(systemDark),
                                reduced = reduced,
                                onTheme = { theme, origin ->
                                    themeId = theme.id
                                    // In the words' colour of the theme being turned to: the screen
                                    // cross-fades into it, and the ring has to show on the new one.
                                    val ink = theme.palette(Themes.isDark(theme, preview.isDark(systemDark))).onGlass
                                    scope.launch { pulse.fire(origin, ink) }
                                },
                                onAppearance = { mode, origin ->
                                    appearance = mode
                                    // In the words' colour of the look being turned to: the page
                                    // changes at once, and the ring has to show on the new one.
                                    val theme = Themes.byId(themeId)
                                    val ink = theme.palette(Themes.isDark(theme, preview.copy(appearance = mode).isDark(systemDark))).onGlass
                                    scope.launch { pulse.fire(origin, ink) }
                                },
                                onGraphicsWarm = { viewModel.graphicsWarmed() },
                                onToggleWidget = { type -> if (type in widgets) widgets.remove(type) else widgets.add(type) },
                                onDensity = { densityId = it.id },
                                halo = { restHalo * haloBoost.value * chrome.value },
                                sweep = { sweep.value },
                                zoom = { zoom.value },
                                chrome = { chrome.value },
                            )
                        }
                    }
                }

                // The one place the next move always is. It steps aside while the home screen is
                // being put together, when there is nothing to press, and while Zenith readies
                // itself, until that has run long enough to be worth offering to finish unseen.
                val quiet = step == Steps.OPTIMIZE && !optimized
                AnimatedVisibility(
                    visible = when (step) {
                        Steps.BUILD -> false
                        Steps.OPTIMIZE -> optimized || offerSkip
                        else -> true
                    },
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
                        AnimatedContent(
                            targetState = quiet,
                            transitionSpec = {
                                fadeIn(tween(220, delayMillis = 60))
                                    .togetherWith(fadeOut(tween(120)))
                                    .using(SizeTransform(clip = false) { _, _ -> spring(dampingRatio = 1f, stiffness = 300f, visibilityThreshold = IntSize(1, 1)) })
                            },
                            contentAlignment = Alignment.Center,
                            label = "nextMove",
                        ) { skip ->
                            if (skip) {
                                // The work goes on without you watching it: nothing is lost.
                                QuietButton(
                                    text = "Finish in the background",
                                    onClick = { if (step == Steps.OPTIMIZE && !optimized) step = if (offerPlus) Steps.PLUS else Steps.TIPS },
                                    modifier = Modifier.widthIn(max = 400.dp),
                                )
                            } else {
                                PrimaryButton(
                                    text = when (step) {
                                        Steps.HELLO -> "Get started"
                                        Steps.NAME -> if (name.isBlank()) "Skip" else "Continue"
                                        Steps.LOOK -> "Continue"
                                        Steps.HOME -> "Build my home screen"
                                        Steps.BUILD, Steps.OPTIMIZE, Steps.PLUS -> "Continue"
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
                                            Steps.BUILD, Steps.OPTIMIZE -> if (optimized) step = if (offerPlus) Steps.PLUS else Steps.TIPS
                                            // Only reachable if the sheet went away on its own.
                                            Steps.PLUS -> step = Steps.TIPS
                                            else -> if (!celebrating && !finishing) {
                                                celebrating = true
                                                if (!animate) {
                                                    finishing = true
                                                } else {
                                                    // A moment of light before the hand-off: a ring
                                                    // from under your finger, the phone lighting up
                                                    // and a band of light across it, then it grows.
                                                    view.performHapticFeedback(
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.LONG_PRESS
                                                    )
                                                    scope.launch { pulse.fire(button.centre, palette.accent) }
                                                    scope.launch {
                                                        haloBoost.animateTo(3f, tween(220))
                                                        haloBoost.animateTo(1.4f, spring(dampingRatio = 1f, stiffness = 90f))
                                                    }
                                                    scope.launch {
                                                        sweep.snapTo(0f)
                                                        sweep.animateTo(1f, tween(620, easing = FastOutSlowInEasing))
                                                    }
                                                    scope.launch {
                                                        delay(CELEBRATE_MS)
                                                        finishing = true
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    modifier = Modifier
                                        .widthIn(max = 400.dp)
                                        .onGloballyPositioned { button.centre = it.boundsInRoot().center }
                                        // The greeting gets a moment to itself before the button arrives;
                                        // its slot is kept, so nothing shifts when it does.
                                        .reveal(step != Steps.HELLO || helloReady),
                                )
                            }
                        }
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
                        .background(if (palette.dark) Color.Black.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.18f))
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
                        .glass(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), strength = 1.1f, solid = 1.2f)
                        .semantics {
                            paneTitle = "Zenith Plus"
                            isTraversalGroup = true
                        }
                        .navigationBarsPadding(),
                )
            }

            // Nothing takes a touch while the phone is growing into your home screen.
            if (celebrating || finishing) {
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
    val palette = LocalPalette.current
    val tick = rememberTick()
    // Always the same height, shown or not, so nothing below it shifts between steps.
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
            // Called by its full name: inside this Row the Row-scoped overload would be picked.
            androidx.compose.animation.AnimatedVisibility(visible = visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(140))) {
                Box(
                    Modifier
                        .size(48.dp)
                        .pressBounce(0.9f)
                        .clip(CircleShape)
                        .clickable {
                            tick()
                            onBack()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .size(40.dp)
                            .glass(CircleShape, strength = 0.9f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = palette.onGlass.copy(alpha = 0.85f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        androidx.compose.animation.AnimatedVisibility(visible = visible, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            StepMarks(current = progress, total = 3, modifier = Modifier.padding(end = 14.dp))
        }
    }
}
