package com.gabriel.zenithui

import android.app.Activity
import android.app.role.RoleManager
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gabriel.zenithui.data.RefreshRate
import com.gabriel.zenithui.data.findActivity
import com.gabriel.zenithui.island.IslandService
import com.gabriel.zenithui.ui.common.LocalHapticsOn
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.MotionLevel
import com.gabriel.zenithui.ui.common.NoHapticFeedback
import com.gabriel.zenithui.ui.common.rememberAnimationsEnabled
import com.gabriel.zenithui.ui.common.LocalIconLoader
import com.gabriel.zenithui.ui.home.HomeScreen
import com.gabriel.zenithui.ui.onboarding.OnboardingFlow
import com.gabriel.zenithui.ui.theme.ZenithTheme
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: ZenithViewModel by viewModels()

    /** At most this many automatic prompts; after that the hint above the dock is the only ask. */
    private val maxHomePrompts = 3
    private var askedThisSession = false

    private val homeRoleRequest = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Whatever the answer, re-read the world: as the default home app we can read the wallpaper.
        viewModel.onEnvironmentChanged()
    }

    /** Android's own screen for putting Zenith's live wallpaper up: a design that moves needs its say-so once. */
    private val livePicker = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        viewModel.liveAnswered()
    }

    /** Android's own "allow Zenith to create widgets" question, for another app's widget. */
    private val widgetBind = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.widgetBound(result.resultCode == RESULT_OK)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
            WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
        )
        // What this phone's chip says about how much it can draw, before the first frame: see Performance.
        com.gabriel.zenithui.data.PhonePerformance.start(applicationContext)
        setContent { ZenithApp(viewModel) }

        // Ask as soon as the launcher is actually entered — including right after onboarding.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.settings.map { it.onboarded }.collect { onboarded ->
                    if (onboarded) askToBeDefaultLauncher()
                }
            }
        }

        // A design that moves was set: Android shows it and asks to put it up.
        lifecycleScope.launch {
            viewModel.liveAsks.collect {
                val opened = com.gabriel.zenithui.live.LiveWallpapers.pickers(this@MainActivity).any { intent ->
                    runCatching { livePicker.launch(intent) }.isSuccess
                }
                if (!opened) {
                    android.widget.Toast.makeText(this@MainActivity, "This phone has no screen for live wallpapers", android.widget.Toast.LENGTH_SHORT).show()
                    viewModel.liveAnswered()
                }
            }
        }

        // What Android has to be asked while another app's widget is being added.
        lifecycleScope.launch {
            viewModel.widgetAsks.collect { ask ->
                when (ask) {
                    is ZenithViewModel.WidgetAsk.Bind -> {
                        val intent = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, ask.id)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, ask.info.provider)
                            .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, ask.info.profile)
                        if (runCatching { widgetBind.launch(intent) }.isFailure) viewModel.widgetBound(false)
                    }
                    // Only the host can open a set-up screen that its app has not made public, so it
                    // is opened the old way, and its answer comes back to onActivityResult.
                    is ZenithViewModel.WidgetAsk.Configure -> runCatching {
                        viewModel.widgetHost.startAppWidgetConfigureActivityForResult(this@MainActivity, ask.id, 0, REQUEST_CONFIGURE, null)
                    }.onFailure {
                        // A set-up screen that will not open: the widget goes on as it is.
                        viewModel.widgetConfigured(true)
                    }
                }
            }
        }
    }

    @Deprecated("The widget host can only start a set-up screen for an old-style result.")
    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CONFIGURE) {
            viewModel.widgetConfigured(resultCode == RESULT_OK)
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onStart() {
        super.onStart()
        viewModel.homeShowing(true)
        // No picture of another app is taken while the home screen is on show (see PictureService).
        com.gabriel.zenithui.data.AppPictures.homeShown(true)
        // Other apps' widgets are kept up to date while the home screen is on show.
        runCatching { viewModel.widgetHost.startListening() }
    }

    override fun onStop() {
        viewModel.homeShowing(false)
        com.gabriel.zenithui.data.AppPictures.homeShown(false)
        runCatching { viewModel.widgetHost.stopListening() }
        super.onStop()
    }

    private companion object {
        const val REQUEST_CONFIGURE = 0x7A1
    }

    override fun onResume() {
        super.onResume()
        // Back from Android's live wallpaper screen, should its answer not have come back to us.
        viewModel.liveAnswered()
        viewModel.refreshWallpaper()
        viewModel.refreshApps()
        // Notification dots come from a listener Android can drop while Zenith is in the back.
        com.gabriel.zenithui.data.NotificationDots.reconnect(this)
        // Coming home is the natural moment to re-ask Play: it is how a cancelled subscription
        // stops unlocking Plus without waiting for the launcher process to be killed.
        viewModel.refreshPurchases()
    }

    /** Whether Zenith's window had the screen to itself, up to the moment something new arrived. */
    private var focused = false

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        focused = hasFocus
    }

    // Android's word that the transition bringing Zenith into view is over. Coming home from an
    // app, that is when the phone's own close of the app has finished (see AndroidClose).
    override fun onEnterAnimationComplete() {
        super.onEnterAnimationComplete()
        com.gabriel.zenithui.data.AndroidClose.saysDone()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Going home from an app with gestures, Android may ask where its icon is, so it can spring
        // the app's window into it. It is answered first, while the window is still on its way.
        com.gabriel.zenithui.data.GestureHandshake.from(intent)?.let { ask ->
            com.gabriel.zenithui.data.GestureHandshake.answerer?.invoke(ask)
        }
        // Home pressed: everything folds back. Pressed while Zenith was already in front, rather
        // than on the way back from an app, it is also the gesture of that name. Android's own
        // launcher tells the two apart the same way.
        val alreadyHome = focused && (intent.flags and Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT) == 0
        viewModel.onHomePressed(alreadyHome)
    }

    /** Shows the system "default home app" dialog, falling back to the home settings screen. */
    private fun askToBeDefaultLauncher() {
        if (askedThisSession || isDefaultHome()) return
        if (viewModel.settings.value.homePrompts >= maxHomePrompts) return
        askedThisSession = true
        viewModel.noteHomePrompt()

        val roleManager = getSystemService(RoleManager::class.java)
        val canRequestRole = roleManager != null &&
            roleManager.isRoleAvailable(RoleManager.ROLE_HOME) &&
            !roleManager.isRoleHeld(RoleManager.ROLE_HOME)

        val launched = canRequestRole && runCatching {
            homeRoleRequest.launch(roleManager.createRequestRoleIntent(RoleManager.ROLE_HOME))
        }.isSuccess

        if (!launched) {
            runCatching { homeRoleRequest.launch(Intent(Settings.ACTION_HOME_SETTINGS)) }
        }
    }

    private fun isDefaultHome(): Boolean = runCatching {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        packageManager.resolveActivity(home, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
            ?.activityInfo?.packageName == packageName
    }.getOrDefault(true)
}

@Composable
fun ZenithApp(viewModel: ZenithViewModel) {
    val settings by viewModel.settings.collectAsState()
    val glassLevels by viewModel.glassLevels.collectAsState()
    val plus by viewModel.plus.collectAsState()
    var screenSize by remember { mutableStateOf(IntSize.Zero) }

    // The grid is a function of the screen, not a stored number: re-derive it whenever the window
    // resizes, so a fold opening or a tablet gets more cells rather than bigger ones.
    val pixelsPerDp = LocalDensity.current.density
    LaunchedEffect(screenSize, settings.gridDensity, settings.gridColumns, settings.gridRows, settings.onboarded) {
        if (!settings.onboarded || screenSize == IntSize.Zero) return@LaunchedEffect
        // Only a size that has settled counts. While the window is still being laid out it can
        // report a smaller size for a frame or two, and a grid worked out from that would tidy
        // the home screen into it — a change that is not undone when the real size arrives.
        kotlinx.coroutines.delay(350)
        if (screenSize.width / pixelsPerDp < 280f || screenSize.height / pixelsPerDp < 400f) return@LaunchedEffect
        viewModel.resolveGrid(
            widthDp = screenSize.width / pixelsPerDp,
            heightDp = screenSize.height / pixelsPerDp,
        )
    }

    // Everybody draws at the full speed of the screen. This is not something Plus unlocks: a
    // launcher that stutters reads as broken, and the free tier is the advert.
    val activity = LocalContext.current.findActivity()
    LaunchedEffect(activity) {
        val host = activity ?: return@LaunchedEffect
        RefreshRate.applyFullSpeed(host)
    }
    // For a phone whose chip says nothing, Auto watches how its frames keep up (see PhonePerformance).
    androidx.compose.runtime.DisposableEffect(activity) {
        val stop = activity?.let { com.gabriel.zenithui.data.PhonePerformance.watch(it) }
        onDispose { stop?.invoke() }
    }

    val view = LocalView.current
    // Light or dark exactly as ZenithTheme works it out, so the bar icons never disagree with the page.
    val lightBars = !settings.isDark(isSystemInDarkTheme())
    SideEffect {
        // Until setup is done it owns the bar colours, since it previews a Light or Dark that is not saved yet.
        if (!settings.onboarded) return@SideEffect
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightBars
            isAppearanceLightNavigationBars = lightBars
            // Hidden, it still comes in for a moment when swiped down from the very top.
            systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (settings.hideStatusBar) hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
            else show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { screenSize = it }
    ) {
        val systemHaptics = LocalHapticFeedback.current
        CompositionLocalProvider(
            LocalIconLoader provides viewModel.icons,
            LocalHapticsOn provides settings.haptics,
            // Older code asks Compose for haptics directly; with the switch off it gets silence.
            LocalHapticFeedback provides if (settings.haptics) systemHaptics else NoHapticFeedback,
            LocalMotionLevel provides MotionLevel.of(settings.motion, rememberAnimationsEnabled()),
        ) {
            AnimatedContent(
                targetState = settings.onboarded,
                transitionSpec = {
                    if (targetState) {
                        // Setup has just grown its preview to fill the screen: the real home
                        // screen fades in on top of it, and only then does setup let go.
                        ContentTransform(
                            targetContentEnter = fadeIn(tween(200)),
                            initialContentExit = fadeOut(tween(100, delayMillis = 200)),
                            targetContentZIndex = 1f,
                        )
                    } else {
                        fadeIn(tween(200)).togetherWith(fadeOut(tween(200)))
                    }
                },
                label = "handoff",
                modifier = Modifier.fillMaxSize(),
            ) { onboarded ->
                if (onboarded) {
                    // The island runs while it is on, Plus is active and Android lets it over other
                    // apps; asked again as Zenith comes back from Android's settings.
                    val context = LocalContext.current
                    var overApps by remember { mutableStateOf(IslandService.allowed(context)) }
                    LifecycleResumeEffect(Unit) {
                        overApps = IslandService.allowed(context)
                        onPauseOrDispose { }
                    }
                    LaunchedEffect(plus.active, settings.island.on, overApps) {
                        IslandService.sync(context, plus.active && settings.island.on && overApps)
                    }
                    // The Glass studio is Plus: with Plus ended the glass is Zenith's own again, the tuning kept.
                    val tuning = if (plus.active) settings.glassTuning else com.gabriel.zenithui.data.GlassTuning()
                    ZenithTheme(settings = settings, glass = glassLevels, screenSize = screenSize, glassTuning = tuning) {
                        HomeScreen(viewModel)
                    }
                } else {
                    OnboardingFlow(
                        viewModel = viewModel,
                        backdrop = glassLevels?.frost,
                        screenSize = screenSize,
                        onFinished = { },
                    )
                }
            }
        }
    }
}
