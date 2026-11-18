package com.gabriel.zenithui.live

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import com.gabriel.zenithui.data.ConfigRepository
import com.gabriel.zenithui.data.LiveMotion
import com.gabriel.zenithui.data.WallColors
import com.gabriel.zenithui.data.WallStyle
import com.gabriel.zenithui.data.WallpaperArt
import com.gabriel.zenithui.data.WallpaperFollow
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.theme.Palettes
import com.gabriel.zenithui.ui.theme.wallColors
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Zenith's moving designs as Android's live wallpaper ([LiveWallpaper]), seen from the rest of
 * Zenith: whether it is the wallpaper now, Android's screen for putting it up (a live wallpaper
 * cannot be set without asking), which design and colours it shows, the still the lock screen
 * shows where the live wallpaper cannot be there itself, and the still the glass bends.
 */
object LiveWallpapers {

    private const val PREFS = "live_wallpaper"
    private const val KEY_LOCK_ID = "lockId"
    private const val KEY_LOCK_KEY = "lockKey"
    private const val KEY_TONE = "tone"

    fun component(context: Context): ComponentName = ComponentName(context.packageName, LiveWallpaper::class.java.name)

    /** Whether Zenith's live wallpaper is the home screen's wallpaper now. */
    fun isOurs(context: Context): Boolean = runCatching {
        WallpaperManager.getInstance(context.applicationContext).wallpaperInfo?.component == component(context)
    }.getOrDefault(false)

    /**
     * Android's screens for putting Zenith's live wallpaper up, in the order to try them: its own
     * preview of it with a button to set it, or failing that its list of live wallpapers.
     */
    fun pickers(context: Context): List<Intent> = listOf(
        Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            .putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, component(context)),
        Intent(WallpaperManager.ACTION_LIVE_WALLPAPER_CHOOSER),
    )

    /** The design the live wallpaper shows: the one in the settings, or Hover where that one does not move. */
    fun design(settings: ZenithSettings): WallStyle =
        WallStyle.byName(settings.wallpaperStyle)?.takeIf { it.live != null } ?: WallStyle.Hover

    /**
     * Whether it is in its dark colours: as Zenith is now ([night] being whether the phone is dark),
     * or, with following off, as it was when it was set, the way a still design stays as it was.
     */
    fun dark(context: Context, settings: ZenithSettings, night: Boolean): Boolean {
        if (settings.wallpaperFollow) return settings.isDark(night)
        return when (prefs(context).getString(KEY_TONE, null)) {
            "dark" -> true
            "light" -> false
            else -> settings.isDark(night)
        }
    }

    fun colors(context: Context, settings: ZenithSettings, night: Boolean): WallColors =
        Palettes.forWallpaper(settings, dark(context, settings, night)).wallColors()

    /** Notes whether it is dark now, for it to stay that way once following is off. */
    fun keepTone(context: Context, dark: Boolean) {
        val tone = if (dark) "dark" else "light"
        val prefs = prefs(context)
        if (prefs.getString(KEY_TONE, null) != tone) prefs.edit().putString(KEY_TONE, tone).apply()
    }

    /** Tells [listener] whenever the tone kept for it changes. */
    fun watch(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs(context).registerOnSharedPreferenceChangeListener(listener)

    fun unwatch(context: Context, listener: SharedPreferences.OnSharedPreferenceChangeListener) =
        prefs(context).unregisterOnSharedPreferenceChangeListener(listener)

    /** Whether the phone is dark now, as the application sees it. */
    fun night(context: Context): Boolean =
        context.applicationContext.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

    /**
     * Whether the lock screen shows the live wallpaper itself, so needs no still of it. Android 14
     * says which wallpaper the lock screen has; on any version a lock screen with no wallpaper of its
     * own shows the home screen's.
     */
    fun lockShowsLive(context: Context): Boolean = runCatching {
        val manager = WallpaperManager.getInstance(context.applicationContext)
        val ours = component(context)
        val lockOwn = Build.VERSION.SDK_INT >= 34 && manager.getWallpaperInfo(WallpaperManager.FLAG_LOCK)?.component == ours
        lockOwn || (manager.getWallpaperId(WallpaperManager.FLAG_LOCK) < 0 && isOurs(context))
    }.getOrDefault(false)

    /**
     * Puts a still of [style] on the lock screen, where the live wallpaper is not there itself: an
     * unlock design in its lock pose, so that unlocking carries straight on into its opening, and an
     * idle one as it starts out, which is where it starts from each time the phone is unlocked.
     * Asked for ([force]), it goes up whatever the lock screen has; otherwise only over a still
     * Zenith put there that is still showing, and only when the picture would change. Slow: it
     * paints the whole screen, so it is called off the main thread.
     */
    fun setLockStill(context: Context, style: WallStyle, colors: WallColors, force: Boolean): Boolean {
        val app = context.applicationContext
        if (lockShowsLive(app)) return true
        val manager = WallpaperManager.getInstance(app)
        val (width, height) = WallpaperArt.screenSize(app)
        val key = WallpaperFollow.key(style.name, colors, width, height)
        val prefs = prefs(app)
        if (!force) {
            val stored = prefs.getInt(KEY_LOCK_ID, 0)
            val current = runCatching { manager.getWallpaperId(WallpaperManager.FLAG_LOCK) }.getOrDefault(-1)
            // A lock screen chosen anywhere else is never painted over.
            if (stored <= 0 || current != stored) return false
            if (prefs.getString(KEY_LOCK_KEY, null) == key) return true
        }
        val bitmap = WallpaperArt.render(style, colors, width, height, open = 0f)
        val id = runCatching { manager.setBitmap(bitmap, null, true, WallpaperManager.FLAG_LOCK) }.getOrDefault(0)
        bitmap.recycle()
        if (id <= 0) return false
        prefs.edit().putInt(KEY_LOCK_ID, id).putString(KEY_LOCK_KEY, key).apply()
        return true
    }

    /** A still of the live wallpaper for the glass to bend: which design, in which colours, how big, and its key. */
    class GlassStill internal constructor(val key: String, val style: WallStyle, val colors: WallColors, val width: Int, val height: Int)

    /** What the glass should be bending while Zenith's live wallpaper is up, as the settings are now. */
    fun glassStill(context: Context): GlassStill {
        val app = context.applicationContext
        val settings = ConfigRepository.get(app).settings.value
        val style = design(settings)
        val colors = colors(app, settings, night(app))
        val (screenWidth, screenHeight) = WallpaperArt.screenSize(app)
        // The glass softens most of what it shows. A design that drifts is softened further below,
        // so it is painted smaller still.
        val width = min(screenWidth, if (style.live == LiveMotion.Idle) GLASS_IDLE_WIDTH else GLASS_WIDTH).coerceAtLeast(1)
        val height = (screenHeight * width / screenWidth.toFloat()).roundToInt().coerceAtLeast(1)
        return GlassStill("live:" + WallpaperFollow.key(style.name, colors, width, height), style, colors, width, height)
    }

    /**
     * The picture for [still]: the design at its home pose. One that drifts is never quite where a
     * still of it would be, so the glass is given only its colours, softly, and nothing it could be
     * seen to be out of step with.
     */
    fun paint(still: GlassStill): Bitmap {
        val bitmap = WallpaperArt.render(still.style, still.colors, still.width, still.height, open = 1f)
        return if (still.style.live == LiveMotion.Idle) soften(bitmap) else bitmap
    }

    /** [source] halved three times and stretched back: a blur, cheaply. [source] is used up. */
    private fun soften(source: Bitmap): Bitmap {
        var small = source
        repeat(3) {
            val next = Bitmap.createScaledBitmap(small, (small.width / 2).coerceAtLeast(1), (small.height / 2).coerceAtLeast(1), true)
            if (small !== source) small.recycle()
            small = next
        }
        val out = Bitmap.createScaledBitmap(small, source.width, source.height, true)
        small.recycle()
        source.recycle()
        return out
    }

    private fun prefs(context: Context) = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private const val GLASS_WIDTH = 720
    private const val GLASS_IDLE_WIDTH = 480
}
