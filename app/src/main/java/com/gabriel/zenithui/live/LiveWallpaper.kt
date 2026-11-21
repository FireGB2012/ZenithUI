package com.gabriel.zenithui.live

import android.app.KeyguardManager
import android.app.WallpaperColors
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.res.Configuration
import android.database.ContentObserver
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.PowerManager
import android.os.Process
import android.os.SystemClock
import android.provider.Settings
import android.service.wallpaper.WallpaperService
import android.view.Choreographer
import android.view.Display
import android.view.SurfaceHolder
import androidx.core.content.ContextCompat
import com.gabriel.zenithui.data.ConfigRepository
import com.gabriel.zenithui.data.LauncherMotion
import com.gabriel.zenithui.data.LiveCurves
import com.gabriel.zenithui.data.LiveLock
import com.gabriel.zenithui.data.LiveMotion
import com.gabriel.zenithui.data.LivePace
import com.gabriel.zenithui.data.LiveScene
import com.gabriel.zenithui.data.LiveUnlock
import com.gabriel.zenithui.data.LockWord
import com.gabriel.zenithui.data.Performance
import com.gabriel.zenithui.data.PhonePerformance
import com.gabriel.zenithui.data.WallColors
import com.gabriel.zenithui.data.WallStyle
import com.gabriel.zenithui.ui.common.MotionLevel
import com.gabriel.zenithui.ui.theme.SYSTEM_WALLPAPER_ZOOM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

/**
 * Zenith's moving designs (the Live shelf), as Android's live wallpaper: Android draws it behind
 * the home screen, and on the lock screen where the phone lets it.
 *
 * It runs in Zenith's own process and draws on a thread of its own, only while it can be seen, and
 * no more often than the design needs (see [LivePace]): an idle design at 30 frames a second in
 * Lite and 60 in Full, holding still with movement reduced or Battery saver on; an unlock design
 * only for the moment it opens. Each frame only poses layers painted once ([LiveScene]). Which
 * design, in which colours, light or dark, comes from Zenith's settings, as for the still designs
 * ([LiveWallpapers]); Android is told its colours, so the phone's own colours can follow it.
 *
 * A still design named there it shows still, as the picture Zenith puts up for it: Android keeps
 * the live wallpaper on the lock screen when a still one goes on the home screen alone, and for
 * the moment before that picture is up it is still the home screen's ([LiveDesign]).
 */
class LiveWallpaper : WallpaperService() {

    /** The engines up now: the wallpaper's own and, while one is open, a preview's. Main thread only. */
    private val engines = mutableSetOf<LiveEngine>()

    override fun onCreateEngine(): Engine = LiveEngine()

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // Light turning to dark, or back: each engine works out its colours again.
        engines.forEach { it.replan() }
    }

    private inner class LiveEngine : Engine() {

        private val main = Handler(Looper.getMainLooper())
        private val thread = HandlerThread("ZenithLiveWallpaper", Process.THREAD_PRIORITY_DISPLAY).apply { start() }
        private val render = Handler(thread.looper)
        private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        private val keyguard = getSystemService(KeyguardManager::class.java)
        private val power = getSystemService(PowerManager::class.java)

        @Volatile private var alive = true
        @Volatile private var colours: WallpaperColors? = null
        private var listening = false

        // Everything below is the drawing thread's alone.

        private var choreographer: Choreographer? = null
        private var surface: SurfaceHolder? = null
        private var width = 0
        private var height = 0
        private var visible = false

        /** Whether the surface gives a hardware canvas. Without one a frame is too slow to move, and the design holds still. */
        private var hardware = true
        private var plan: Plan? = null
        private var shown: Shown? = null

        /** The scene being painted off this thread, by its key, while the one before is still shown. */
        private var wantedKey: String? = null

        /** A new design fades in over the one before it, or over its own backdrop when there was none: since when, or 0. */
        private var fadeFrom = 0L
        private var leaving: Shown? = null
        private var leavingSeconds = 0.0
        private var leavingOpen = 1f

        private var unlock = LiveUnlock(0L)

        /** How far into its motion an idle design is: it only moves on while it is seen. */
        private var seconds = 0.0
        private var clockFrom = 0L
        private var lastFrame = 0L
        private var looping = false
        private var vsyncNanos = 16_666_667L

        private val grain = Paint()
        private val backdrop = Paint()
        private var backdropFor: Pair<WallColors, Int>? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            setTouchEventsEnabled(false)
            // It stays where it is whatever page the launcher is on, as a still wallpaper does from Android 14.
            setOffsetNotificationsEnabled(false)
            engines += this
            render.post { choreographer = Choreographer.getInstance() }
            val app = applicationContext
            PhonePerformance.start(app)
            scope.launch {
                combine(ConfigRepository.get(app).settings, PhonePerformance.verdict) { _, _ -> }.collect { replan() }
            }
            listen()
        }

        override fun onDestroy() {
            alive = false
            engines -= this
            scope.cancel()
            if (listening) {
                runCatching { unregisterReceiver(events) }
                runCatching { contentResolver.unregisterContentObserver(animations) }
                LiveWallpapers.unwatch(this@LiveWallpaper, toneChanged)
                listening = false
            }
            render.post {
                choreographer?.removeFrameCallback(frame)
                render.removeCallbacksAndMessages(null)
                shown?.scene?.release()
                leaving?.scene?.release()
                shown = null
                leaving = null
                thread.quitSafely()
            }
            super.onDestroy()
        }

        override fun onComputeColors(): WallpaperColors? = colours

        override fun onCommand(action: String?, x: Int, y: Int, z: Int, extras: Bundle?, resultRequested: Boolean): Bundle? {
            // Android's own word to the wallpaper as the lock screen starts to go, or the screen to sleep.
            if (!isPreview) {
                when (LiveLock.ofCommand(action)) {
                    LockWord.Unlocked -> render.post { unlocked() }
                    LockWord.ScreenOff -> render.post { screenOff() }
                    LockWord.Nothing -> Unit
                }
            }
            return super.onCommand(action, x, y, z, extras, resultRequested)
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            val locked = keyguard?.isKeyguardLocked == true
            render.post {
                this.visible = visible
                val now = now()
                if (visible && !isPreview) {
                    if (stillOnly()) {
                        if (unlock.closed && !locked) unlock.settle()
                    } else {
                        unlock.shown(now, locked)
                    }
                }
                if (!visible) endFade()
                if (isPreview) previewLoop(visible)
                pace()
                if (visible && !looping) draw(now)
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            onRender(SURFACE_WAIT_MS) {
                surface = holder
                this.width = width
                this.height = height
                // A new surface may give the hardware canvas the last one would not.
                hardware = true
                build()
                draw(now())
            }
        }

        override fun onSurfaceRedrawNeeded(holder: SurfaceHolder) {
            super.onSurfaceRedrawNeeded(holder)
            // Android shows the surface once this returns, so what is there now is drawn first.
            onRender(SURFACE_WAIT_MS) {
                surface = holder
                draw(now())
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            // Nothing may be drawn into it once this returns.
            onRender(DESTROY_WAIT_MS) {
                choreographer?.removeFrameCallback(frame)
                looping = false
                surface = null
            }
            super.onSurfaceDestroyed(holder)
        }

        // ---- What to draw ----

        /** Works out what to draw from Zenith's settings and the phone's state, and hands it to the drawing thread. */
        fun replan() {
            if (!alive) return
            val app = applicationContext
            val settings = ConfigRepository.get(app).settings.value
            val night = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
            val animations = runCatching {
                Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
            }.getOrDefault(true)
            val next = Plan(
                style = LiveWallpapers.design(app, settings),
                colors = LiveWallpapers.colors(app, settings, night),
                lite = Performance.isLite(settings.performance, PhonePerformance.verdict.value),
                reduced = MotionLevel.of(settings.motion, animations).reduced,
                powerSave = power?.isPowerSaveMode == true,
            )
            render.post { apply(next) }
        }

        private fun apply(next: Plan) {
            plan = next
            if (next.reduced && unlock.opening(now())) unlock.settle()
            build()
            pace()
        }

        /** Paints the scene the plan wants, off this thread, unless it is already shown or on its way. */
        private fun build() {
            val next = plan ?: return
            if (width <= 0 || height <= 0) return
            val key = "${next.style.name}|${next.colors}|${width}x$height"
            if (key == shown?.key || key == wantedKey) return
            wantedKey = key
            val w = width
            val h = height
            scope.launch(Dispatchers.Default) {
                val made = runCatching { LiveScene.forWallpaper(next.style, next.colors, w.toFloat(), h.toFloat()).also { it.prepare() } }.getOrNull()
                if (made == null) {
                    render.post { if (wantedKey == key) wantedKey = null }
                    return@launch
                }
                val found = runCatching { coloursOf(made, w, h) }.getOrNull()
                val posted = render.post {
                    if (!alive || wantedKey != key) {
                        made.release()
                        return@post
                    }
                    wantedKey = null
                    swapIn(Shown(made, next.style, key))
                    colours = found
                    main.post { if (alive) runCatching { notifyColorsChanged() } }
                }
                if (!posted) made.release()
            }
        }

        private fun swapIn(next: Shown) {
            val old = shown
            // Where the one going was, to fade out from there.
            val oldOpen = if (old?.style?.live == LiveMotion.Unlock) unlock.pose(now()) else 1f
            val oldSeconds = seconds
            shown = next
            if (old == null || old.style != next.style) {
                // Another design: its own opening, and its own motion from the start. One waiting
                // at the lock pose for the phone to be unlocked carries on waiting.
                val waiting = unlock.closed
                unlock = LiveUnlock(next.scene.openMs)
                if (waiting) unlock.screenOff()
                seconds = 0.0
                clockFrom = 0L
            }
            endFade()
            if (visible) {
                leaving = old
                leavingSeconds = oldSeconds
                leavingOpen = oldOpen
                fadeFrom = now()
            } else {
                old?.scene?.release()
            }
            if (isPreview) previewLoop(visible)
            pace()
            draw(now())
        }

        private fun endFade() {
            leaving?.scene?.release()
            leaving = null
            fadeFrom = 0L
            fadeDone = false
        }

        // ---- When to draw ----

        /** Milliseconds between frames now, or 0 when what is on screen can stay. */
        private fun frameMs(now: Long): Int {
            val current = shown ?: return 0
            val p = plan ?: return 0
            if (!visible) return 0
            if (fadeFrom != 0L) return LivePace.FULL_MS
            if (!hardware) return 0
            val motion = current.style.live ?: return 0
            return LivePace.frameMs(motion, p.lite, p.reduced, p.powerSave, unlock.opening(now))
        }

        /** Whether an idle design's own motion is wanted, rather than frames for some other reason. */
        private fun drifting(): Boolean {
            val p = plan ?: return false
            return hardware && shown?.style?.live == LiveMotion.Idle &&
                LivePace.frameMs(LiveMotion.Idle, p.lite, p.reduced, p.powerSave, opening = false) > 0
        }

        /**
         * Whether an idle design holds still on this frame for Zenith, which is moving ([LivePace.holds]):
         * never while one design fades into another, nor in Android's preview of it.
         */
        private fun makesWay(frameTimeNanos: Long, now: Long): Boolean {
            val p = plan ?: return false
            val motion = shown?.style?.live ?: return false
            if (isPreview || fadeFrom != 0L || unlock.opening(now)) return false
            return LivePace.holds(motion, p.lite, LauncherMotion.moving(frameTimeNanos))
        }

        /** Without movement, or without a canvas fast enough for it, an unlock design goes straight to its home pose. */
        private fun stillOnly(): Boolean = plan?.reduced == true || !hardware

        private fun pace() {
            watch()
            val wanted = frameMs(now()) > 0
            val c = choreographer ?: return
            if (wanted && !looping) {
                looping = true
                lastFrame = 0L
                clockFrom = 0L
                vsyncNanos = vsync()
                c.postFrameCallback(frame)
            } else if (!wanted && looping) {
                c.removeFrameCallback(frame)
                looping = false
                clockFrom = 0L
            }
        }

        private val frame = object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                looping = false
                val now = frameTimeNanos / 1_000_000L
                val ms = frameMs(now)
                if (ms == 0) {
                    // The frame that ends it: the home pose an opening lands on, the end of a fade.
                    clockFrom = 0L
                    if (visible) draw(now)
                    return
                }
                if (LivePace.due(frameTimeNanos, lastFrame, ms, vsyncNanos)) {
                    if (makesWay(frameTimeNanos, now)) {
                        // Zenith is moving: the frame is left to it, and the design's clock waits, so
                        // it carries on from where it stopped rather than jumping ahead.
                        clockFrom = 0L
                    } else {
                        if (drifting()) {
                            if (clockFrom != 0L) seconds += (frameTimeNanos - clockFrom).coerceIn(0L, MAX_STEP_NANOS) / 1e9
                            clockFrom = frameTimeNanos
                        }
                        draw(now)
                    }
                    lastFrame = frameTimeNanos
                }
                val next = frameMs(now)
                if (next > 0) {
                    looping = true
                    // Woken for the vsync its next frame is due on, not for every one between.
                    val target = lastFrame + next * 1_000_000L - vsyncNanos / 2
                    val wait = ((target - System.nanoTime()) / 1_000_000L).coerceAtLeast(0L)
                    choreographer?.postFrameCallbackDelayed(this, wait)
                } else {
                    clockFrom = 0L
                    if (visible) draw(now)
                }
            }
        }

        // ---- Drawing ----

        private fun draw(now: Long) {
            val holder = surface ?: return
            if (!holder.surface.isValid) return
            val canvas = lock(holder) ?: return
            try {
                paint(canvas, now)
            } catch (_: Exception) {
                // A frame that could not be drawn leaves the one before it.
            } finally {
                runCatching { holder.unlockCanvasAndPost(canvas) }
            }
            // Let go of only once the frame that last drew it is on its way.
            if (fadeDone) endFade()
        }

        /** The fade drew its last frame: what it faded out is let go of after that frame. */
        private var fadeDone = false

        private fun lock(holder: SurfaceHolder): Canvas? {
            if (hardware) {
                val canvas = runCatching { holder.lockHardwareCanvas() }
                canvas.getOrNull()?.let { return it }
                // A valid surface that will not give a hardware canvas never will.
                if (canvas.isFailure) hardware = false
            }
            return runCatching { holder.lockCanvas() }.getOrNull()
        }

        private fun paint(canvas: Canvas, now: Long) {
            val current = shown
            if (current == null) {
                drawBackdrop(canvas, 1f)
                return
            }
            // As much bigger than the screen as Android makes a still wallpaper, so the glass,
            // which bends a still of it, lines up with it, and the still on the lock screen opens
            // into it without a jump.
            val zoom = SYSTEM_WALLPAPER_ZOOM
            canvas.save()
            canvas.scale(zoom, zoom, width / 2f, height / 2f)
            val open = if (current.style.live == LiveMotion.Unlock) unlock.pose(now) else 1f
            current.scene.draw(canvas, seconds, open)
            canvas.restore()
            if (fadeFrom != 0L) {
                val t = ((now - fadeFrom) / FADE_MS.toFloat()).coerceIn(0f, 1f)
                val left = 1f - LiveCurves.easeInOut(t)
                val previous = leaving
                if (left > 0f) {
                    if (previous == null) {
                        drawBackdrop(canvas, left)
                    } else {
                        canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(), (left * 255f).roundToInt())
                        canvas.scale(zoom, zoom, width / 2f, height / 2f)
                        previous.scene.draw(canvas, leavingSeconds, leavingOpen)
                        canvas.restore()
                    }
                }
                if (t >= 1f) fadeDone = true
            }
            LiveScene.grain(canvas, width.toFloat(), height.toFloat(), grain)
        }

        /** The design's backdrop alone: what is shown in the moment before its layers are ready. */
        private fun drawBackdrop(canvas: Canvas, alpha: Float) {
            val c = plan?.colors
            if (c == null) {
                canvas.drawColor(Color.BLACK)
                return
            }
            if (backdropFor != c to height) {
                backdrop.shader = LinearGradient(0f, 0f, 0f, height.toFloat(), intArrayOf(c.deep, c.mid, c.base), null, Shader.TileMode.CLAMP)
                backdropFor = c to height
            }
            backdrop.alpha = (alpha * 255f).roundToInt()
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backdrop)
        }

        /** The colours Android is told, from a small picture of the design at rest. */
        private fun coloursOf(scene: LiveScene, w: Int, h: Int): WallpaperColors? {
            val across = COLOURS_WIDTH
            val down = (COLOURS_WIDTH * h / w.toFloat()).roundToInt().coerceAtLeast(1)
            val small = Bitmap.createBitmap(across, down, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(small)
            canvas.scale(across / w.toFloat(), down / h.toFloat())
            scene.draw(canvas, 0.0, 1f)
            return WallpaperColors.fromBitmap(small).also { small.recycle() }
        }

        // ---- The phone being locked and unlocked ----

        private val events = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == PowerManager.ACTION_POWER_SAVE_MODE_CHANGED) {
                    replan()
                    return
                }
                when (LiveLock.ofBroadcast(intent.action, keyguard?.isKeyguardLocked == true)) {
                    LockWord.Unlocked -> render.post { unlocked() }
                    LockWord.ScreenOff -> render.post { screenOff() }
                    LockWord.Nothing -> Unit
                }
            }
        }

        private val animations = object : ContentObserver(main) {
            override fun onChange(selfChange: Boolean) = replan()
        }

        private val toneChanged = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> replan() }

        private fun listen() {
            val filter = IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            if (!isPreview) {
                filter.addAction(Intent.ACTION_SCREEN_OFF)
                filter.addAction(Intent.ACTION_SCREEN_ON)
                filter.addAction(Intent.ACTION_USER_PRESENT)
            }
            // Exported, or the word that the phone was unlocked never comes: System UI sends it, and
            // System UI is not Android's own process, which is all a receiver that is not exported
            // hears. These are protected broadcasts, which only the system can send, so exporting
            // it lets nothing else in.
            runCatching { ContextCompat.registerReceiver(this@LiveWallpaper, events, filter, ContextCompat.RECEIVER_EXPORTED) }
            runCatching {
                contentResolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, animations)
            }
            LiveWallpapers.watch(this@LiveWallpaper, toneChanged)
            listening = true
        }

        private val lockCheck = Runnable { lookAtLock() }

        /**
         * While an unlock design waits on show at its lock pose, looks at the lock screen itself
         * every so often: the word that the phone was unlocked can come late, or not at all, and a
         * design that came into view while the lock screen was still going would wait for it.
         */
        private fun watch() {
            render.removeCallbacks(lockCheck)
            if (!isPreview && visible && shown?.style?.live == LiveMotion.Unlock && unlock.waiting()) {
                render.postDelayed(lockCheck, LiveLock.WATCH_MS)
            }
        }

        private fun lookAtLock() {
            if (isPreview || !visible || !unlock.waiting()) return
            // The screen on and no lock screen up. While the screen is going off the lock screen
            // may not be up yet, and that is not the phone being unlocked.
            val gone = power?.isInteractive == true && keyguard?.isKeyguardLocked != true
            if (gone) unlocked() else render.postDelayed(lockCheck, LiveLock.WATCH_MS)
        }

        /**
         * The screen went off: back to the lock pose, drawn now, so that it is what the screen shows
         * when it comes on. An idle design starts its motion again from where it starts, which is
         * also the still on a lock screen it is not on itself, so unlocking shows no jump.
         */
        private fun screenOff() {
            unlock.screenOff()
            if (shown?.style?.live == LiveMotion.Idle) {
                seconds = 0.0
                clockFrom = 0L
            }
            endFade()
            pace()
            draw(now())
        }

        private fun unlocked() {
            val now = now()
            if (stillOnly()) unlock.settle() else unlock.unlocked(now, visible)
            pace()
            if (!unlock.opening(now)) draw(now)
        }

        // ---- The preview Android shows before it is set ----

        private val previewStep = Runnable { stepPreview() }

        /** In a preview an unlock design opens over and over, from its lock pose, so you can see what it does. */
        private fun previewLoop(on: Boolean) {
            render.removeCallbacks(previewStep)
            if (!on || shown?.style?.live != LiveMotion.Unlock || stillOnly()) return
            unlock.screenOff()
            render.postDelayed(previewStep, PREVIEW_LOCK_MS)
        }

        private fun stepPreview() {
            val current = shown ?: return
            if (!visible || current.style.live != LiveMotion.Unlock) return
            val now = now()
            if (unlock.closed && !unlock.opening(now)) {
                unlock.replay(now)
                pace()
                render.postDelayed(previewStep, current.scene.openMs + PREVIEW_HOME_MS)
            } else {
                unlock.screenOff()
                draw(now)
                render.postDelayed(previewStep, PREVIEW_LOCK_MS)
            }
        }

        // ---- Helpers ----

        private fun now(): Long = SystemClock.uptimeMillis()

        /** The time between two refreshes of the screen the wallpaper is on. */
        private fun vsync(): Long {
            val rate = runCatching {
                val display = if (Build.VERSION.SDK_INT >= 30) displayContext?.display else null
                (display ?: getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY))?.refreshRate
            }.getOrNull()?.takeIf { it in 20f..240f } ?: 60f
            return (1e9 / rate).toLong()
        }

        /** Runs [block] on the drawing thread and waits up to [waitMs] for it, as Android's surface calls need. */
        private fun onRender(waitMs: Long, block: () -> Unit) {
            val done = CountDownLatch(1)
            if (!render.post { try { block() } finally { done.countDown() } }) return
            runCatching { done.await(waitMs, TimeUnit.MILLISECONDS) }
        }
    }

    private companion object {
        /** A change of design or colours fades in over this long. */
        const val FADE_MS = 600

        /** The longest step an idle design's clock takes between two frames, so a stall is not seen as a jump. */
        const val MAX_STEP_NANOS = 100_000_000L

        /** In a preview: how long it holds its lock pose before opening, and its home pose after. */
        const val PREVIEW_LOCK_MS = 900L
        const val PREVIEW_HOME_MS = 2_200L

        const val SURFACE_WAIT_MS = 500L
        const val DESTROY_WAIT_MS = 1_000L

        /** How wide the picture Android's colours are worked out from is. */
        const val COLOURS_WIDTH = 112
    }
}

/** What the wallpaper should show: the design, its colours, and how freely it may move. */
private data class Plan(val style: WallStyle, val colors: WallColors, val lite: Boolean, val reduced: Boolean, val powerSave: Boolean)

/** A scene that is ready, the design it is of, and the key it was painted for. */
private class Shown(val scene: LiveScene, val style: WallStyle, val key: String)
