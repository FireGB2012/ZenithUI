package com.gabriel.zenithui.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.gabriel.zenithui.ZenithViewModel
import com.gabriel.zenithui.data.GridDensity
import com.gabriel.zenithui.data.HomeItem
import com.gabriel.zenithui.data.LayoutBuilder
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Onboarding is a form, not a canvas: past this width the eye has to travel too far between a
 * label and its control, so the column stays phone-sized and centres itself on bigger screens.
 */
private val FORM_MAX_WIDTH = 480.dp

/** How long each thing takes to click into place while the home screen is being put together. */
private const val ASSEMBLE_STEP_MS = 110L

private object Steps {
    const val NAME = 0
    const val LOOK = 1
    const val HOME = 2
    const val BUILD = 3
    const val PLUS = 4
    const val TIPS = 5
}

/**
 * First run: your name, a colour, what goes on the page, and then the page itself being put
 * together in front of you. The top bar and the button at the bottom never move; only what is
 * between them changes, so going through it feels like one screen rather than a stack of them.
 */
@Composable
fun OnboardingFlow(
    viewModel: ZenithViewModel,
    backdrop: ImageBitmap?,
    screenSize: IntSize,
    onFinished: () -> Unit,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val plus by viewModel.plus.collectAsState()
    val apps by viewModel.apps.collectAsState()
    val appsByKey = remember(apps) { apps.associateBy { it.key } }

    var step by rememberSaveable { mutableIntStateOf(Steps.NAME) }
    var name by rememberSaveable { mutableStateOf("") }
    var paletteId by rememberSaveable { mutableStateOf("midnight") }
    var appearance by rememberSaveable { mutableStateOf("dark") }
    var densityId by rememberSaveable { mutableStateOf(GridDensity.Balanced.id) }
    val widgets = remember { androidx.compose.runtime.mutableStateListOf("clock", "toggles") }
    var built by rememberSaveable { mutableStateOf(false) }
    // Entrances play the first time a screen is seen; coming back to it, it is simply there.
    var nameSeen by rememberSaveable { mutableStateOf(false) }
    var lookSeen by rememberSaveable { mutableStateOf(false) }

    // The grid you pick is a cell size; how many of them fit is the screen's business.
    val gridDensity = GridDensity.byId(densityId)
    val pixelsPerDp = LocalDensity.current.density
    val screenWidth = if (screenSize == IntSize.Zero) 411.dp else (screenSize.width / pixelsPerDp).dp
    val screenHeight = if (screenSize == IntSize.Zero) 891.dp else (screenSize.height / pixelsPerDp).dp
    val (columns, rows) = remember(gridDensity, screenWidth, screenHeight) {
        gridDensity.gridFor(screenWidth.value, screenHeight.value)
    }

    val preview = ZenithSettings(
        userName = name,
        paletteId = paletteId,
        appearance = appearance,
        glass = 0.85f,
        columns = columns,
        rows = rows,
    )

    // The page the preview shows is laid out by the same code that builds the real one.
    val dock by produceState(emptyList<String>(), apps) {
        value = withContext(Dispatchers.Default) { viewModel.repository.suggestedDock(4) }
    }
    val chosen = widgets.toList()
    val items by produceState(emptyList<HomeItem>(), apps, chosen, columns, rows, dock) {
        value = withContext(Dispatchers.Default) {
            LayoutBuilder.build(viewModel.repository, apps, chosen, columns, rows, dock)
        }
    }

    // How many things are on the preview page. All of them, except while it is being put together.
    var shown by remember { mutableIntStateOf(Int.MAX_VALUE) }

    LaunchedEffect(step) {
        if (step != Steps.BUILD || built) return@LaunchedEffect
        val work = launch(Dispatchers.Default) {
            viewModel.buildHome(name, paletteId, appearance, chosen, gridDensity, columns, rows)
        }
        // Clear the page while the controls fold away and the phone grows into their space…
        shown = 0
        delay(520)
        // …then put it back together, one piece at a time.
        val total = items.size + dock.size
        for (count in 1..total) {
            shown = count
            delay(ASSEMBLE_STEP_MS)
        }
        delay(380)
        work.join()
        shown = Int.MAX_VALUE
        built = true
    }

    fun back() {
        when (step) {
            Steps.LOOK -> step = Steps.NAME
            Steps.HOME -> step = Steps.LOOK
        }
    }
    BackHandler(enabled = step == Steps.LOOK || step == Steps.HOME) { back() }

    fun finish() {
        viewModel.markOnboarded()
        onFinished()
    }

    ZenithTheme(settings = preview, backdrop = null, screenSize = screenSize) {
        val palette = LocalPalette.current
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(palette.backdrop)),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                Modifier
                    .widthIn(max = FORM_MAX_WIDTH)
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                TopBar(
                    canGoBack = step == Steps.LOOK || step == Steps.HOME,
                    progress = step.takeIf { it <= Steps.HOME },
                    onBack = { back() },
                )

                // Name, the studio, Plus and the tips are the four places you can be. The three
                // studio steps share one screen so the phone in it never leaves.
                val phase = when (step) {
                    Steps.NAME -> 0
                    Steps.PLUS -> 2
                    Steps.TIPS -> 3
                    else -> 1
                }
                AnimatedContent(
                    targetState = phase,
                    transitionSpec = {
                        val forward = targetState > initialState
                        val slide = spring(dampingRatio = 1f, stiffness = 300f, visibilityThreshold = IntOffset(1, 1))
                        (fadeIn(tween(280, delayMillis = 60)) +
                            slideInHorizontally(slide) { (it * 0.14f * if (forward) 1 else -1).toInt() })
                            .togetherWith(
                                fadeOut(tween(160)) +
                                    slideOutHorizontally(slide) { (it * -0.1f * if (forward) 1 else -1).toInt() }
                            )
                    },
                    label = "phase",
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                ) { current ->
                    when (current) {
                        0 -> NameStep(
                            name = name,
                            onNameChange = { name = it },
                            onDone = {
                                focusManager.clearFocus()
                                nameSeen = true
                                step = Steps.LOOK
                            },
                            playIntro = !nameSeen,
                        )

                        1 -> StudioStep(
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
                            onPalette = { paletteId = it },
                            onAppearance = { appearance = it },
                            onToggleWidget = { type -> if (type in widgets) widgets.remove(type) else widgets.add(type) },
                            onDensity = { densityId = it.id },
                        )

                        2 -> PlusStep(status = plus)
                        else -> TipsStep()
                    }
                }

                // The one place the next move always is. It steps aside only while the home
                // screen is being put together, when there is nothing to press.
                AnimatedVisibility(
                    visible = step != Steps.BUILD || built,
                    enter = fadeIn(tween(260)) + expandVertically(spring(dampingRatio = 1f, stiffness = 300f)),
                    exit = fadeOut(tween(140)) + shrinkVertically(spring(dampingRatio = 1f, stiffness = 300f)),
                ) {
                    Column(Modifier.padding(horizontal = 24.dp).padding(top = 8.dp, bottom = 16.dp)) {
                        PrimaryButton(
                            text = when (step) {
                                Steps.NAME -> if (name.isBlank()) "Skip" else "Continue"
                                Steps.LOOK -> "Continue"
                                Steps.HOME -> "Set up my home screen"
                                Steps.BUILD -> "Continue"
                                Steps.PLUS -> "Try it free"
                                else -> "Start using Zenith"
                            },
                            onClick = {
                                when (step) {
                                    Steps.NAME -> {
                                        focusManager.clearFocus()
                                        nameSeen = true
                                        step = Steps.LOOK
                                    }

                                    Steps.LOOK -> {
                                        lookSeen = true
                                        step = Steps.HOME
                                    }

                                    Steps.HOME -> step = Steps.BUILD
                                    Steps.BUILD -> step = Steps.PLUS
                                    Steps.PLUS -> {
                                        // Play shows its own sheet once the trial subscription
                                        // exists; until then the week is counted on the device.
                                        viewModel.startTrialViaPlayOrLocally(context as? android.app.Activity)
                                        step = Steps.TIPS
                                    }

                                    else -> finish()
                                }
                            },
                        )
                        AnimatedVisibility(
                            visible = step == Steps.PLUS,
                            enter = fadeIn(tween(220)) + expandVertically(),
                            exit = fadeOut(tween(120)) + shrinkVertically(),
                        ) {
                            QuietButton(text = "Not now", onClick = { step = Steps.TIPS })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopBar(canGoBack: Boolean, progress: Int?, onBack: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
            // Called by its full name: inside this Row the Row-scoped overload would be picked.
            androidx.compose.animation.AnimatedVisibility(visible = canGoBack, enter = fadeIn(tween(200)), exit = fadeOut(tween(140))) {
                Box(
                    Modifier
                        .size(44.dp)
                        .pressBounce(0.9f)
                        .clip(CircleShape)
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = palette.onGlass.copy(alpha = 0.85f),
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
        Spacer(Modifier.weight(1f))
        AnimatedVisibility(visible = progress != null, enter = fadeIn(tween(200)), exit = fadeOut(tween(200))) {
            StepMarks(current = progress ?: 0, total = 3, modifier = Modifier.padding(end = 14.dp))
        }
    }
}
