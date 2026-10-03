package com.gabriel.zenithui

import android.app.Activity
import android.app.role.RoleManager
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.gabriel.zenithui.data.GridDensity
import com.gabriel.zenithui.data.RefreshRate
import com.gabriel.zenithui.data.findActivity
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

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
            WindowManager.LayoutParams.FLAG_SHOW_WALLPAPER,
        )
        setContent { ZenithApp(viewModel) }

        // Ask as soon as the launcher is actually entered — including right after onboarding.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                viewModel.settings.map { it.onboarded }.collect { onboarded ->
                    if (onboarded) askToBeDefaultLauncher()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.homeShowing(true)
    }

    override fun onStop() {
        viewModel.homeShowing(false)
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshWallpaper()
        viewModel.refreshApps()
        // Coming home is the natural moment to re-ask Play: it is how a cancelled subscription
        // stops unlocking Plus without waiting for the launcher process to be killed.
        viewModel.refreshPurchases()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Pressing home while already home: fold everything back to page one.
        viewModel.onHomePressed()
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
    val backdrop by viewModel.backdrop.collectAsState()
    val sharpBackdrop by viewModel.sharpBackdrop.collectAsState()
    var screenSize by remember { mutableStateOf(IntSize.Zero) }

    // The grid is a function of the screen, not a stored number: re-derive it whenever the window
    // resizes, so a fold opening or a tablet gets more cells rather than bigger ones.
    val pixelsPerDp = LocalDensity.current.density
    LaunchedEffect(screenSize, settings.gridDensity, settings.onboarded) {
        if (!settings.onboarded || screenSize == IntSize.Zero) return@LaunchedEffect
        // Only a size that has settled counts. While the window is still being laid out it can
        // report a smaller size for a frame or two, and a grid worked out from that would tidy
        // the home screen into it — a change that is not undone when the real size arrives.
        kotlinx.coroutines.delay(350)
        if (screenSize.width / pixelsPerDp < 280f || screenSize.height / pixelsPerDp < 400f) return@LaunchedEffect
        viewModel.resolveGrid(
            density = GridDensity.byId(settings.gridDensity),
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

    val view = LocalView.current
    // Light or dark exactly as ZenithTheme works it out, so the bar icons never disagree with the page.
    val lightBars = !settings.isDark(isSystemInDarkTheme())
    SideEffect {
        // Until setup is done it owns the bar colours, since it previews palettes that are not saved yet.
        if (!settings.onboarded) return@SideEffect
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = lightBars
            isAppearanceLightNavigationBars = lightBars
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .onSizeChanged { screenSize = it }
    ) {
        CompositionLocalProvider(LocalIconLoader provides viewModel.icons) {
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
                    ZenithTheme(settings = settings, backdrop = backdrop, screenSize = screenSize, sharpBackdrop = sharpBackdrop) {
                        HomeScreen(viewModel)
                    }
                } else {
                    OnboardingFlow(
                        viewModel = viewModel,
                        backdrop = backdrop,
                        screenSize = screenSize,
                        onFinished = { },
                    )
                }
            }
        }
    }
}
