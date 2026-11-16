package com.gabriel.zenithui.data

import android.accessibilityservice.AccessibilityService
import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * Zenith's app picture helper: an accessibility service for one thing only, taking a picture of
 * the app in front now and then, so that a closed app can be seen going back into its icon and an
 * app opened again can be seen coming out of it. It is how the competitor does it, and the only
 * way Android gives a launcher a picture of another app without asking each time.
 *
 * It asks for one kind of event, "another screen came to the front", which apps send whoever is
 * listening, and it cannot read what is on the screen. Pictures stay in memory ([AppPictures]),
 * shrunk, for the last few apps only, and are let go of when the screen goes off. Nothing is
 * written down or sent anywhere. It is off until switched on in Android's settings, and Zenith opens
 * and closes apps without it as it always has. When it is taken, and which are kept: [PictureRules].
 */
class PictureService : AccessibilityService() {

    private val main = Handler(Looper.getMainLooper())

    /** Pictures are shrunk and checked off the main thread, one at a time. */
    private val worker: Executor = Executors.newSingleThreadExecutor { job ->
        Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
            job.run()
        }, "zenith-pictures").apply { isDaemon = true }
    }

    /** The app whose picture is due, or null while nothing is to be taken. */
    private var due: String? = null
    private val shoot = Runnable { shootNow() }

    /** How many times the picture has waited for Zenith to go, this time. */
    private var waits = 0

    private var homes: Set<String> = emptySet()
    private var keyboards: Set<String> = emptySet()
    private val launchable = HashMap<String, Boolean>()

    private val screen = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    stop()
                    AppPictures.dropPictures()
                    // Cheap to find out again, and an app may have come or gone meanwhile.
                    launchable.clear()
                }
                // On again: the app that was in front very likely still is, and says nothing.
                // Unlocked is when its picture can be taken; with no lock, that is now.
                Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> {
                    val pkg = AppPictures.front ?: return
                    due = pkg
                    waits = 0
                    schedule(PictureRules.SETTLE_MS)
                }
            }
        }
    }

    override fun onServiceConnected() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            // No screenshots for a service before Android 11: nothing to do.
            disableSelf()
            return
        }
        AppPictures.running = true
        _on.value = true
        homes = homePackages()
        keyboards = keyboardPackages()
        runCatching {
            val filter = IntentFilter(Intent.ACTION_SCREEN_OFF).apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            ContextCompat.registerReceiver(this, screen, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        val pkg = event.packageName?.toString()
        when (PictureRules.kind(pkg, packageName, homes, keyboards, ::isLaunchable)) {
            PictureRules.Front.App -> {
                AppPictures.cameToFront(pkg)
                due = pkg
                waits = 0
                // A new screen of the app: its picture once it has settled, then now and then.
                schedule(PictureRules.SETTLE_MS)
            }
            PictureRules.Front.Home -> {
                stop()
                AppPictures.cameToFront(null)
            }
            PictureRules.Front.Other -> {
                // Over the app, which is still there: nothing more until it shows a screen again.
                stop()
                AppPictures.covered()
            }
            PictureRules.Front.Ignore -> Unit
        }
    }

    override fun onInterrupt() = Unit

    @Suppress("DEPRECATION")
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Only when Android is really short: a launcher is told the gentler levels every time an
        // app opens over it, which is exactly when its picture is wanted.
        if (level == TRIM_MEMORY_RUNNING_CRITICAL || level >= TRIM_MEMORY_COMPLETE) AppPictures.dropPictures()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        AppPictures.dropPictures()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        shutDown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        shutDown()
        super.onDestroy()
    }

    private fun shutDown() {
        stop()
        runCatching { unregisterReceiver(screen) }
        AppPictures.running = false
        _on.value = false
        AppPictures.clear()
    }

    private fun stop() {
        due = null
        main.removeCallbacks(shoot)
    }

    private fun schedule(delay: Long) {
        main.removeCallbacks(shoot)
        main.postDelayed(shoot, delay)
    }

    private fun shootNow() {
        val pkg = due ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        // Never the lock screen, never with the screen off.
        if (AppPictures.front != pkg || !awake()) return
        // Never Zenith. An app just opened from it is often still on its way in, over Zenith, so
        // it is given a moment more, a few times; an app in a window over Zenith never is.
        if (AppPictures.homeShowing) {
            if (waits++ < HOME_WAITS) schedule(PictureRules.SETTLE_MS)
            return
        }
        val asked = SystemClock.uptimeMillis()
        runCatching { take(pkg, asked) }
        schedule(PictureRules.REFRESH_MS)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun take(pkg: String, asked: Long) {
        takeScreenshot(
            Display.DEFAULT_DISPLAY,
            worker,
            object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) = keep(pkg, result, asked)
                override fun onFailure(errorCode: Int) = Unit
            },
        )
    }

    /** Shrinks the picture, checks it is not one Android blacked out, and keeps it. On [worker]. */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun keep(pkg: String, result: AccessibilityService.ScreenshotResult, takenAt: Long) {
        val buffer = result.hardwareBuffer
        try {
            val hardware = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace) ?: return
            val (width, height) = PictureRules.shrunk(hardware.width, hardware.height)
            // Read back once, in full, then shrunk: a picture on the graphics chip cannot be
            // looked at, and it is drawn from memory a moment later anyway.
            val full = hardware.copy(Bitmap.Config.ARGB_8888, false)
            hardware.recycle()
            if (full == null) return
            val small = if (full.width == width && full.height == height) full else Bitmap.createScaledBitmap(full, width, height, true)
            if (small !== full) full.recycle()
            if (blank(small)) {
                small.recycle()
                return
            }
            small.prepareToDraw()
            if (!AppPictures.put(pkg, small, takenAt)) small.recycle()
        } catch (_: Throwable) {
            // A picture is a nicety: never a reason for anything to go wrong.
        } finally {
            buffer.close()
        }
    }

    private fun blank(picture: Bitmap): Boolean {
        val probe = Bitmap.createScaledBitmap(picture, PROBE_WIDTH, PROBE_HEIGHT, false)
        val pixels = IntArray(PROBE_WIDTH * PROBE_HEIGHT)
        probe.getPixels(pixels, 0, PROBE_WIDTH, 0, 0, PROBE_WIDTH, PROBE_HEIGHT)
        if (probe !== picture) probe.recycle()
        return PictureRules.blank(pixels, PROBE_WIDTH, PROBE_HEIGHT)
    }

    private fun awake(): Boolean {
        val power = getSystemService(PowerManager::class.java)
        val keyguard = getSystemService(KeyguardManager::class.java)
        return power?.isInteractive != false && keyguard?.isKeyguardLocked != true
    }

    private fun isLaunchable(pkg: String): Boolean =
        launchable.getOrPut(pkg) { runCatching { packageManager.getLaunchIntentForPackage(pkg) != null }.getOrDefault(false) }

    /** Every home app on the phone: recents is one of theirs, and nothing to take a picture of. */
    private fun homePackages(): Set<String> = runCatching {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        packageManager.queryIntentActivities(home, 0).mapNotNull { it.activityInfo?.packageName }.toSet()
    }.getOrDefault(emptySet()) + packageName

    /** The keyboards switched on: one comes and goes over an app without the app going anywhere. */
    private fun keyboardPackages(): Set<String> {
        val found = HashSet<String>()
        runCatching {
            getSystemService(InputMethodManager::class.java)?.enabledInputMethodList?.forEach { found += it.packageName }
        }
        runCatching {
            listOf(Settings.Secure.ENABLED_INPUT_METHODS, Settings.Secure.DEFAULT_INPUT_METHOD).forEach { setting ->
                Settings.Secure.getString(contentResolver, setting).orEmpty().split(':')
                    .mapNotNull { ComponentName.unflattenFromString(it)?.packageName ?: it.substringBefore('/').takeIf(String::isNotBlank) }
                    .forEach { found += it }
            }
        }
        return found
    }

    companion object {
        private const val HOME_WAITS = 3
        private const val PROBE_WIDTH = 64
        private const val PROBE_HEIGHT = 128

        private val _on = MutableStateFlow(false)

        /** Whether the helper is running right now. */
        val on: StateFlow<Boolean> = _on.asStateFlow()

        /** Whether this phone can have it: Android 11 or later. */
        val supported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

        /** Whether it has been switched on in Android's settings, running or not. */
        fun enabled(context: Context): Boolean {
            val mine = ComponentName(context, PictureService::class.java)
            val list = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
            return list.split(':').any { ComponentName.unflattenFromString(it) == mine }
        }

        /** Android's list of accessibility services, where it is switched on and off. */
        fun openSettings(context: Context) {
            runCatching {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}
