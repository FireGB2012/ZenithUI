package com.gabriel.zenithui

import android.app.Application
import android.app.WallpaperManager
import android.content.ComponentCallbacks
import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.gabriel.zenithui.data.GlassWallpaper
import com.gabriel.zenithui.data.WallColors
import com.gabriel.zenithui.data.WallStyle
import com.gabriel.zenithui.data.WallpaperArt
import com.gabriel.zenithui.data.WallpaperFollow
import com.gabriel.zenithui.data.WallpaperSource
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.theme.Palettes
import com.gabriel.zenithui.ui.theme.wallColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * A wallpaper on its way in. The home screen draws [image] over the old wallpaper, fading it in,
 * and keeps it there until Android is showing the same picture for itself; then it is [leaving].
 * [since] is when it first appeared, so a screen that is rebuilt half way through carries on from
 * where the fade had got to instead of starting it again.
 */
data class Incoming(val image: ImageBitmap, val leaving: Boolean, val since: Long)

/**
 * Keeps one of Zenith's own wallpapers in step with light and dark, in its own colour set. When
 * light turns to dark, or the design is set again in other colours, it is painted again and faded in.
 *
 * It only ever repaints a wallpaper Zenith set and that is still on screen: a photo, or anything
 * chosen in another app, is left alone, and choosing one ends the following ([forget]).
 */
class WallpaperDirector(
    private val app: Application,
    private val scope: CoroutineScope,
    private val settings: StateFlow<ZenithSettings>,
    private val source: WallpaperSource,
    /** Called when something else has become the wallpaper: Zenith's design is no longer in use. */
    private val forget: () -> Unit,
) {
    /** Everything in the settings that decides the picture, and nothing that does not. */
    private data class Wanted(
        val style: String,
        val follow: Boolean,
        val appearance: String,
        val light: WallColors,
        val dark: WallColors,
    )

    private val systemDark = MutableStateFlow(isNight(app.resources.configuration))

    /** Counts the times a wallpaper was asked for by name; one not yet [answered] is painted whatever the rule says. */
    private val requests = MutableStateFlow(0)
    private var answered = 0

    /** Counts Android's announcements of a changed wallpaper. */
    private val announced = MutableStateFlow(0)

    private val _incoming = MutableStateFlow<Incoming?>(null)
    val incoming: StateFlow<Incoming?> = _incoming.asStateFlow()

    private val _refused = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    /** Android would not take a wallpaper. */
    val refused: SharedFlow<Unit> = _refused.asSharedFlow()

    /** Whether the home screen is on show. When it is not there is nothing to fade, so the wallpaper is simply set. */
    @Volatile
    var homeShowing: Boolean = false

    // The application hears of night and day even while no screen of Zenith's is up.
    private val callbacks = object : ComponentCallbacks {
        override fun onConfigurationChanged(newConfig: Configuration) {
            systemDark.value = isNight(newConfig)
        }

        @Deprecated("Deprecated in Java")
        override fun onLowMemory() = Unit
    }

    fun start() {
        app.registerComponentCallbacks(callbacks)
        scope.launch {
            combine(settings.map(::wanted).distinctUntilChanged(), systemDark, requests) { _, _, request -> request }
                .collectLatest { request ->
                    // Tapping through choices: each tap cancels the one before, here or anywhere
                    // below, so only where the tapping stops is painted and set.
                    delay(SETTLE_MS)
                    try {
                        follow(request)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        dismiss()
                    }
                }
        }
    }

    fun stop() {
        app.unregisterComponentCallbacks(callbacks)
    }

    /** Paint and set the design in the settings now, whether or not it would have been repainted anyway. */
    fun request() {
        requests.value += 1
    }

    /** Android announced that the wallpaper changed, from here or from anywhere else. */
    fun wallpaperChanged() {
        announced.value += 1
        if (settings.value.wallpaperStyle.isEmpty()) return
        scope.launch {
            val ours = withContext(Dispatchers.IO) { GlassWallpaper.isCurrent(app) }
            if (!ours) forget()
        }
    }

    private fun wanted(now: ZenithSettings) = Wanted(
        style = now.wallpaperStyle,
        follow = now.wallpaperFollow,
        appearance = now.appearance,
        light = Palettes.forWallpaper(now, dark = false).wallColors(),
        dark = Palettes.forWallpaper(now, dark = true).wallColors(),
    )

    private suspend fun follow(request: Int) {
        val explicit = request != answered
        val now = settings.value
        val style = WallStyle.byName(now.wallpaperStyle)
        if (style == null) {
            answered = request
            dismiss()
            return
        }
        val colors = Palettes.forWallpaper(now, now.isDark(systemDark.value)).wallColors()
        val (width, height) = WallpaperArt.screenSize(app)
        val key = WallpaperFollow.key(style.name, colors, width, height)
        if (!explicit) {
            val ours = withContext(Dispatchers.IO) { GlassWallpaper.isCurrent(app) }
            if (!WallpaperFollow.shouldRepaint(now, key, GlassWallpaper.appliedKey(app), ours)) {
                if (!ours) forget()
                dismiss()
                return
            }
        }
        val bitmap = withContext(Dispatchers.Default) { WallpaperArt.render(style, colors, width, height) }
        val which = if (now.wallpaperLock) {
            WallpaperManager.FLAG_SYSTEM or WallpaperManager.FLAG_LOCK
        } else {
            WallpaperManager.FLAG_SYSTEM
        }
        if (!homeShowing) {
            val ok = GlassWallpaper.set(app, bitmap, which, key)
            answered = request
            if (!ok) _refused.tryEmit(Unit)
            dismiss()
            if (ok) source.refresh()
            return
        }
        // The new picture fades in over the old wallpaper; half way, the glass changes over to
        // it; once it is fully in, Android is given it, and nothing is seen to change.
        _incoming.value = Incoming(bitmap.asImageBitmap(), leaving = false, since = SystemClock.uptimeMillis())
        delay(GLASS_AT_MS)
        source.present(bitmap)
        delay(FADE_IN_MS - GLASS_AT_MS)
        val before = announced.value
        val ok = GlassWallpaper.set(app, bitmap, which, key)
        answered = request
        if (!ok) {
            _refused.tryEmit(Unit)
            dismiss()
            return
        }
        // Android announces a new wallpaper's colours about when it starts to show it. The layer
        // stays until then, and a moment longer, so the old picture is never seen through it.
        withTimeoutOrNull(SHOWN_WAIT_MS) { announced.first { it != before } }
        delay(SHOWN_GRACE_MS)
        dismiss()
    }

    /** Takes the layer away, if it is up, and hands the glass back to whatever the wallpaper really is. */
    private suspend fun dismiss() {
        if (source.release()) source.refresh()
        val showing = _incoming.value ?: return
        _incoming.value = showing.copy(leaving = true)
        delay(FADE_OUT_MS + 40L)
        _incoming.value = null
    }

    private fun isNight(configuration: Configuration): Boolean =
        configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    companion object {
        /** How long the settings have to hold still before their wallpaper is painted. */
        private const val SETTLE_MS = 200L

        /** The new picture's fade in, and the old layer's fade out. */
        const val FADE_IN_MS = 700
        const val FADE_OUT_MS = 300

        /** When, during the fade in, the glass starts bending the new picture instead of the old. */
        private const val GLASS_AT_MS = 350L

        private const val SHOWN_WAIT_MS = 1500L
        private const val SHOWN_GRACE_MS = 250L
    }
}
