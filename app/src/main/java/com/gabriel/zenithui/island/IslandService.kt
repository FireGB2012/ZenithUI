package com.gabriel.zenithui.island

import android.app.ActivityOptions
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.media.AudioManager
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.WindowManager
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.gabriel.zenithui.data.IslandKind
import com.gabriel.zenithui.data.IslandMoment
import com.gabriel.zenithui.data.IslandRules
import com.gabriel.zenithui.data.NotificationDots
import com.gabriel.zenithui.data.NowPlaying
import com.gabriel.zenithui.data.Playing
import com.gabriel.zenithui.data.SettingsStore
import com.gabriel.zenithui.data.WallpaperPalette
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.MotionLevel
import com.gabriel.zenithui.ui.theme.ZenithTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The island over other apps (Zenith Plus): a small window round the camera, drawn over whatever
 * app is open, and a second one for a card while one is open. Zenith starts it while the island is
 * on, Plus is active and Android lets Zenith show over other apps ([sync]), and stops it otherwise.
 *
 * It stays in sight as long as it runs — at rest as a black disc the size of the camera, on the
 * camera, so not to be seen — because Android keeps an app with a window over others running, and
 * would otherwise stop this a minute after Zenith left the screen.
 */
class IslandService : Service(), LifecycleOwner, SavedStateRegistryOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    private val scope = MainScope()
    private var window: WindowManager? = null
    private var pill: ComposeView? = null
    private var card: ComposeView? = null

    private val moments = MutableStateFlow<List<IslandMoment>>(emptyList())
    private val level = MutableStateFlow(0)
    private val fullScreen = MutableStateFlow(false)
    private val sideways = MutableStateFlow(false)
    private val geometry = MutableStateFlow(IslandGeometry.typical())
    private val expanded = MutableStateFlow(false)

    /** What both windows draw. */
    private val shown = MutableStateFlow(IslandState(null))
    private val visible = MutableStateFlow(true)

    /** What was showing when the card opened: a message being read stays, whatever comes after it. */
    private var held: Pair<IslandKind, IslandMoment?>? = null

    /** What tapping each notification still showing opens, by when it came in. */
    private val opens = HashMap<Long, PendingIntent?>()
    private var plugged = false

    private val pillParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
        PixelFormat.TRANSLUCENT,
    ).apply {
        gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
        title = "Zenith island"
        layoutInDisplayCutoutMode = cutoutMode()
    }

    private val battery = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100).coerceAtLeast(1)
            val percent = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) * 100 / scale
            val now = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0
            level.value = percent
            // The charger going in is a moment; finding it already in, as this starts, is not.
            if (now && !plugged && !isInitialStickyBroadcast) add(IslandMoment(IslandKind.PLUGGED, System.currentTimeMillis(), level = percent))
            plugged = now
        }
    }

    private val ringer = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (isInitialStickyBroadcast) return
            val mode = when (intent.getIntExtra(AudioManager.EXTRA_RINGER_MODE, AudioManager.RINGER_MODE_NORMAL)) {
                AudioManager.RINGER_MODE_VIBRATE -> "vibrate"
                AudioManager.RINGER_MODE_SILENT -> "silent"
                else -> "ring"
            }
            add(IslandMoment(IslandKind.RINGER, System.currentTimeMillis(), text = mode))
        }
    }

    private fun add(moment: IslandMoment) = moments.update { IslandRules.prune(it + moment, moment.at) }

    override fun onCreate() {
        super.onCreate()
        savedState.performAttach()
        savedState.performRestore(null)
        lifecycleRegistry.currentState = Lifecycle.State.CREATED
        val manager = getSystemService(WindowManager::class.java)
        if (!Settings.canDrawOverlays(this) || manager == null) {
            stopSelf()
            return
        }
        window = manager
        sideways.value = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        measure()
        val view = compose { Compact() }
        // An app gone full screen hides the status bar for every window: so the island can tell.
        ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
            fullScreen.value = !insets.isVisible(WindowInsetsCompat.Type.statusBars())
            insets
        }
        if (runCatching { manager.addView(view, pillParams) }.isFailure) {
            stopSelf()
            return
        }
        pill = view
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED

        registerReceiver(battery, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        registerReceiver(ringer, IntentFilter(AudioManager.RINGER_MODE_CHANGED_ACTION))
        NowPlaying.watch(this)
        NotificationDots.reconnect(this)
        scope.launch {
            NotificationDots.posted.collect { posted ->
                // What the music app says about its own song is the music, not news.
                if (posted.packageName == NowPlaying.now.value?.packageName) return@collect
                val at = System.currentTimeMillis()
                opens[at] = posted.open
                add(IslandMoment(IslandKind.NOTIFICATION, at, posted.packageName, posted.title.ifBlank { posted.text }, if (posted.title.isBlank()) "" else posted.text))
            }
        }
        scope.launch { expanded.collect { open -> if (open) openCard() } }
        scope.launch { follow() }
    }

    private fun compose(content: @Composable () -> Unit) = ComposeView(this).apply {
        setViewTreeLifecycleOwner(this@IslandService)
        setViewTreeSavedStateRegistryOwner(this@IslandService)
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
        setContent {
            val store = remember { SettingsStore.get(this@IslandService) }
            val settings by store.settings.collectAsState()
            // Theirs, but no tilt sensor: the island has no glass to light.
            ZenithTheme(settings.copy(glassShine = false)) {
                CompositionLocalProvider(LocalMotionLevel provides MotionLevel.of(settings.motion, systemAnimations())) {
                    content()
                }
            }
        }
    }

    /** Works out what the island shows, again whenever anything it shows changes, and as clocks tick. */
    private suspend fun follow() {
        val flags = combine(fullScreen, sideways, expanded, level) { full, turned, open, percent -> Flags(full, turned, open, percent) }
        combine(SettingsStore.get(this).settings, NowPlaying.now, moments, NotificationDots.live, flags) { settings, music, list, live, f ->
            Inputs(settings, music, list, live, f)
        }.collectLatest { inputs ->
            while (true) {
                val now = System.currentTimeMillis()
                val next = render(inputs, now) ?: break
                delay((next - now).coerceAtLeast(16L))
            }
        }
    }

    private class Flags(val full: Boolean, val turned: Boolean, val open: Boolean, val level: Int)
    private class Inputs(val settings: ZenithSettings, val music: Playing?, val moments: List<IslandMoment>, val live: Map<String, NotificationDots.Live>, val flags: Flags)

    /** Shows what is going on at [now]; answers with when it next needs to look again, or null. */
    private fun render(inputs: Inputs, now: Long): Long? {
        val island = inputs.settings.island
        val hidden = (inputs.flags.full && island.hideFullScreen) || (inputs.flags.turned && island.hideSideways)
        // A call before a timer; of two timers, the newer.
        val clock = inputs.live.values.sortedWith(compareByDescending<NotificationDots.Live> { it.call }.thenByDescending { it.postedAt }).firstOrNull()
        val playing = inputs.music?.playing == true
        val picked = if (hidden) null else IslandRules.pick(island, playing, clock != null, inputs.moments, now)
        val open = inputs.flags.open && !hidden
        val showing = if (open) held ?: picked else picked
        if (inputs.flags.open && (hidden || showing?.first?.opens != true)) expanded.value = false
        val kind = showing?.first
        val moment = showing?.second
        val secondary = IslandRules.secondary(island, playing, clock != null, kind)
        val from = when (kind) {
            IslandKind.NOTIFICATION -> moment?.packageName
            IslandKind.LIVE -> clock?.packageName
            else -> null
        }
        shown.value = IslandState(
            kind = kind,
            moment = moment,
            music = inputs.music,
            musicColour = colourOf(inputs.music?.art),
            live = clock,
            liveText = clock?.let { it.shown ?: IslandRules.clock(it.base, it.countDown, now) }.orEmpty(),
            level = inputs.flags.level,
            appIcon = iconOf(from),
            appName = nameOf(from),
            secondary = secondary,
            secondaryIcon = if (secondary == IslandKind.LIVE) iconOf(clock?.packageName) else null,
        )
        visible.value = !hidden
        // Touchable whenever it is in sight: Android lets a window over other apps that takes no
        // touches be only 80% solid, which shows the app through the black. It sits in the status
        // bar, which keeps the touches there, so this takes none from the app under it but a hair.
        setTouchable(!hidden)
        opens.keys.retainAll(inputs.moments.map { it.at }.toSet())
        // The next moment to end, and the next second of a running clock that is on show.
        val ending = IslandRules.nextChange(inputs.moments, now)
        val ticking = if (clock != null && clock.shown == null && (kind == IslandKind.LIVE || secondary == IslandKind.LIVE)) {
            now + 1000L - Math.floorMod(now - clock.base, 1000L)
        } else {
            null
        }
        return listOfNotNull(ending, ticking).minOrNull()
    }

    // ------------------------------------------------------------------ the windows

    @Composable
    private fun Compact() {
        val state by shown.collectAsState()
        val shape by geometry.collectAsState()
        val open by expanded.collectAsState()
        val seen by visible.collectAsState()
        IslandCompact(
            state = state,
            geometry = shape,
            expanded = open,
            onTap = ::expand,
            onHold = { openWhatIsShown(state) },
            skirt = if (state.kind?.opens == true && !open) SKIRT else 0.dp,
            visible = seen,
        )
    }

    @Composable
    private fun Card() {
        val state by shown.collectAsState()
        val shape by geometry.collectAsState()
        val open by expanded.collectAsState()
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopStart) {
            IslandCard(
                state = state,
                geometry = shape,
                open = open,
                from = shape.root,
                actions = IslandActions(
                    close = { expanded.value = false },
                    openNotification = { openWhatIsShown(state) },
                    openLive = { openWhatIsShown(state) },
                    openMusic = { openWhatIsShown(state) },
                    playPause = { NowPlaying.playPause() },
                    next = { NowPlaying.next() },
                    previous = { NowPlaying.previous() },
                ),
                onClosed = ::closeCard,
            )
        }
    }

    /** Opens the island into a card, for what it is showing, if that is one that opens. */
    private fun expand() {
        val state = shown.value
        val kind = state.kind ?: return
        if (!kind.opens) return
        held = kind to state.moment
        pill?.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
        expanded.value = true
    }

    private fun openCard() {
        if (card != null) return
        val manager = window ?: return
        val density = resources.displayMetrics.density
        val shape = geometry.value
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            ((shape.cardTop.value + CARD_ROOM) * density).toInt(),
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            title = "Zenith island card"
            layoutInDisplayCutoutMode = cutoutMode()
        }
        val view = compose { Card() }
        view.setOnTouchListener { _, event ->
            // A touch anywhere outside the card's window closes it, and goes on to what is under it.
            if (event.action == MotionEvent.ACTION_OUTSIDE) expanded.value = false
            false
        }
        if (runCatching { manager.addView(view, params) }.isSuccess) card = view
    }

    private fun closeCard() {
        if (expanded.value) return
        held = null
        val view = card ?: return
        card = null
        runCatching { window?.removeViewImmediate(view) }
    }

    /** Opens the app behind what is shown: the notification's own place, the call, the music app. */
    private fun openWhatIsShown(state: IslandState) {
        when (state.kind) {
            IslandKind.NOTIFICATION -> {
                val intent = state.moment?.at?.let(opens::get)
                if (intent != null) send(intent) else state.moment?.packageName?.let(::launch)
                moments.value = emptyList()
            }
            IslandKind.LIVE -> state.live?.let { live -> live.open?.let(::send) ?: launch(live.packageName) }
            IslandKind.MUSIC -> state.music?.packageName?.takeIf { it.isNotEmpty() }?.let(::launch)
            else -> Unit
        }
        expanded.value = false
    }

    // ------------------------------------------------------------------ where the camera is

    /** This phone's camera and status bar: the island goes round the one, under the other's icons. */
    private fun measure() {
        val manager = window ?: return
        val density = resources.displayMetrics.density
        val cutout = if (Build.VERSION.SDK_INT >= 30) {
            manager.currentWindowMetrics.windowInsets.displayCutout
        } else {
            @Suppress("DEPRECATION")
            manager.defaultDisplay.cutout
        }
        val width = if (Build.VERSION.SDK_INT >= 30) manager.currentWindowMetrics.bounds.width() else resources.displayMetrics.widthPixels
        val bar = if (Build.VERSION.SDK_INT >= 30) {
            manager.currentWindowMetrics.windowInsets.getInsets(android.view.WindowInsets.Type.statusBars()).top
        } else {
            @Suppress("DiscouragedApi", "InternalInsetsDefinition")
            resources.getIdentifier("status_bar_height", "dimen", "android").takeIf { it != 0 }?.let(resources::getDimensionPixelSize) ?: 0
        }
        val hole = cutout?.boundingRectTop?.takeIf { !it.isEmpty }
        val shape = IslandGeometry(
            hole = ((hole?.let { minOf(it.width(), it.height()) } ?: 0) / density).dp,
            holeX = ((hole?.exactCenterX() ?: (width / 2f)) / density).dp,
            holeY = ((hole?.exactCenterY() ?: 0f) / density).dp,
            statusBar = (bar / density).dp,
            width = (width / density).dp,
        )
        geometry.value = shape
        pillParams.x = ((shape.centreX.value - shape.width.value / 2f) * density).toInt()
        pillParams.y = (shape.top.value * density).toInt().coerceAtLeast(0)
        pill?.let { runCatching { manager.updateViewLayout(it, pillParams) } }
    }

    private fun setTouchable(on: Boolean) {
        val touchable = pillParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE == 0
        if (on == touchable) return
        pillParams.flags = if (on) {
            pillParams.flags and WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE.inv()
        } else {
            pillParams.flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        }
        val manager = window ?: return
        pill?.let { runCatching { manager.updateViewLayout(it, pillParams) } }
    }

    // ------------------------------------------------------------------ what it shows

    private val icons = LinkedHashMap<String, ImageBitmap?>()
    private val names = LinkedHashMap<String, String>()

    private fun iconOf(packageName: String?): ImageBitmap? {
        if (packageName.isNullOrEmpty()) return null
        return icons.getOrPut(packageName) {
            if (icons.size > 24) icons.remove(icons.keys.first())
            runCatching { packageManager.getApplicationIcon(packageName).toBitmap(120, 120).asImageBitmap() }.getOrNull()
        }
    }

    private fun nameOf(packageName: String?): String {
        if (packageName.isNullOrEmpty()) return ""
        return names.getOrPut(packageName) {
            runCatching { packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString() }.getOrDefault("")
        }
    }

    private var lastArt: ImageBitmap? = null
    private var lastColour: Color? = null

    /** The cover's liveliest colour, for the bars and the progress line: the music's own. */
    private fun colourOf(art: ImageBitmap?): Color? {
        if (art == null) return null
        if (art === lastArt) return lastColour
        lastArt = art
        lastColour = runCatching {
            val pixels = IntArray(art.width * art.height)
            art.readPixels(pixels)
            WallpaperPalette.fromPixels(pixels)?.let { Color(it.accent) }
        }.getOrNull()
        return lastColour
    }

    private fun send(intent: PendingIntent) {
        // Started from over another app, with Zenith's own leave to do so.
        val options = if (Build.VERSION.SDK_INT >= 34) {
            val mode = if (Build.VERSION.SDK_INT >= 36) {
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOW_ALWAYS
            } else {
                @Suppress("DEPRECATION")
                ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
            }
            ActivityOptions.makeBasic().setPendingIntentBackgroundActivityStartMode(mode).toBundle()
        } else {
            null
        }
        runCatching { intent.send(this, 0, null, null, null, null, options) }
    }

    private fun launch(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) ?: return
        runCatching { startActivity(intent) }
    }

    private fun systemAnimations(): Boolean =
        Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        sideways.value = newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE
        expanded.value = false
        // Turned, the camera is somewhere else.
        measure()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onBind(intent: Intent?) = null

    override fun onDestroy() {
        lifecycleRegistry.currentState = Lifecycle.State.DESTROYED
        runCatching { unregisterReceiver(battery) }
        runCatching { unregisterReceiver(ringer) }
        listOfNotNull(pill, card).forEach { v -> runCatching { window?.removeViewImmediate(v) } }
        pill = null
        card = null
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /**
         * Room under the island that still takes a tap: the status bar sits over app windows and
         * keeps the touches in it, so a finger on the island itself may never reach it (so on the
         * Galaxy A55: a tap on the island went to the status bar, one just under it opened it).
         */
        private val SKIRT: Dp = 14.dp

        /** How much room the card's window has under the card's top. */
        private const val CARD_ROOM = 190f

        private fun cutoutMode() = if (Build.VERSION.SDK_INT >= 30) {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        } else {
            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        /** Starts the island when [wanted] and Android allows it over other apps, and stops it otherwise. */
        fun sync(context: Context, wanted: Boolean) {
            val intent = Intent(context, IslandService::class.java)
            if (wanted && Settings.canDrawOverlays(context)) {
                runCatching { context.startService(intent) }
            } else {
                runCatching { context.stopService(intent) }
            }
        }

        /** Whether Android lets Zenith show over other apps. */
        fun allowed(context: Context): Boolean = Settings.canDrawOverlays(context)

        /** Opens Android's page for letting Zenith show over other apps. */
        fun ask(context: Context) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, android.net.Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (runCatching { context.startActivity(intent) }.isFailure) {
                runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
            }
        }
    }
}
