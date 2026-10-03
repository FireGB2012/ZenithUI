package com.gabriel.zenithui.data

import android.app.WallpaperColors
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.net.Uri
import android.os.SystemClock
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Android does not let a launcher read the wallpaper back (since Android 13 that needs a system
 * permission), so glass has nothing to bend. What Zenith can do is keep its own copy of any
 * wallpaper it sets — its own designs, or a photo you pick in Zenith — and use that.
 *
 * The copy is only trusted while it is still the wallpaper. Anyone may read the wallpaper's
 * colours, so when Android announces the colours of a wallpaper Zenith has just set, Zenith notes
 * them; if they later differ, the wallpaper was changed somewhere else and the copy is ignored.
 */
object GlassWallpaper {
    private const val DIR = "glass"
    private const val FILE = "wallpaper.webp"
    private const val PENDING = "wallpaper.pending"
    private const val PREFS = "glass_wallpaper"
    private const val KEY_SIGNATURE = "signature"
    private const val KEY_SET_AT = "setAt"
    private const val KEY_SET_CLOCK = "setClock"
    private const val KEY_APPLIED = "applied"

    /** How long after Zenith sets a wallpaper a change of colours is taken to be that wallpaper's. */
    private const val CLAIM_WINDOW_MS = 20_000L

    /**
     * Sets [bitmap] as the wallpaper and keeps a copy for the glass. Returns false if Android
     * refused. [key] names the picture when it is one of Zenith's designs (see [WallpaperFollow]),
     * and is what [appliedKey] gives back; a photo has none.
     */
    suspend fun set(context: Context, bitmap: Bitmap, which: Int, key: String? = null): Boolean = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val dir = File(app.filesDir, DIR).apply { mkdirs() }
        val pending = File(dir, PENDING)
        // Written aside first: if Android refuses the wallpaper, the copy of the old one stays put.
        val saved = runCatching { save(bitmap, pending) }.isSuccess
        val prefs = prefs(app)
        val claimedAt = prefs.getLong(KEY_SET_AT, 0L)
        val claimedClock = prefs.getLong(KEY_SET_CLOCK, 0L)
        // Claimed before Android is asked, not after: it can announce the new picture's colours
        // before setBitmap has returned, and they must be taken as this picture's. The old
        // signature is kept until then, which also covers a new wallpaper whose colours come out
        // exactly the same.
        prefs.edit()
            .putLong(KEY_SET_AT, SystemClock.elapsedRealtime())
            .putLong(KEY_SET_CLOCK, System.currentTimeMillis())
            .commit()
        val ok = runCatching {
            WallpaperManager.getInstance(app).setBitmap(bitmap, null, true, which)
            true
        }.getOrDefault(false)
        if (!ok) {
            pending.delete()
            // Nothing changed on screen, so nothing was claimed either.
            prefs.edit().putLong(KEY_SET_AT, claimedAt).putLong(KEY_SET_CLOCK, claimedClock).apply()
            return@withContext false
        }
        if (!saved) {
            pending.delete()
            // Set without a copy: whatever copy there was no longer matches the screen.
            forget(app)
            return@withContext true
        }
        pending.renameTo(File(dir, FILE))
        prefs.edit().apply { if (key == null) remove(KEY_APPLIED) else putString(KEY_APPLIED, key) }.apply()
        true
    }

    /** The key of the design Zenith last put up, or null if the last thing it set was a photo, or nothing. */
    fun appliedKey(context: Context): String? = prefs(context.applicationContext).getString(KEY_APPLIED, null)

    /**
     * Takes a photo of any shape and makes it the wallpaper: scaled and centre-cropped to exactly
     * this screen, so the copy the glass uses lines up with what Android shows pixel for pixel.
     */
    suspend fun setPhoto(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val (width, height) = WallpaperArt.screenSize(context)
        val decoded = runCatching {
            val size = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, size) }
            // Decode no bigger than needed: a 50 MP photo would not fit in memory at full size.
            var sample = 1
            while (size.outWidth / (sample * 2) >= width && size.outHeight / (sample * 2) >= height) sample *= 2
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
            }
        }.getOrNull() ?: return@withContext false
        val fitted = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val scale = max(width / decoded.width.toFloat(), height / decoded.height.toFloat())
        val drawnWidth = (decoded.width * scale).roundToInt()
        val drawnHeight = (decoded.height * scale).roundToInt()
        val left = (width - drawnWidth) / 2
        val top = (height - drawnHeight) / 2
        Canvas(fitted).drawBitmap(
            decoded,
            null,
            Rect(left, top, left + drawnWidth, top + drawnHeight),
            Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG),
        )
        decoded.recycle()
        val ok = set(context, fitted, WallpaperManager.FLAG_SYSTEM)
        fitted.recycle()
        ok
    }

    /** Android announced new wallpaper colours. If Zenith set that wallpaper just now, they are its signature. */
    fun onColoursChanged(context: Context, colours: WallpaperColors?) {
        val prefs = prefs(context.applicationContext)
        if (colours == null || !justSet(prefs)) return
        // The claim is left standing for its full time: two wallpapers set one after the other
        // have their colours announced one after the other too, and the second must still count.
        prefs.edit().putString(KEY_SIGNATURE, signature(colours)).apply()
    }

    /**
     * Whether the kept copy is still the wallpaper on screen: the only time Zenith may show it in
     * the glass, and the only time it may paint over the wallpaper without being asked.
     */
    fun isCurrent(context: Context): Boolean {
        val app = context.applicationContext
        if (!File(File(app.filesDir, DIR), FILE).exists()) return false
        val manager = WallpaperManager.getInstance(app)
        // A live wallpaper moves; a still copy of anything behind it would be a lie.
        if (runCatching { manager.wallpaperInfo != null }.getOrDefault(false)) return false
        val prefs = prefs(app)
        // Just set by Zenith, before Android has finished working its colours out: it is ours.
        if (justSet(prefs)) return true
        val expected = prefs.getString(KEY_SIGNATURE, null) ?: return false
        return currentSignature(app) == expected
    }

    /** The kept copy, if it is still the wallpaper on screen; otherwise null. */
    fun load(context: Context): Bitmap? {
        if (!isCurrent(context)) return null
        val file = File(File(context.applicationContext.filesDir, DIR), FILE)
        return runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull()
    }

    /**
     * Whether Zenith set the wallpaper within the last few seconds. Counted on two clocks: the one
     * that never jumps, and the one that survives a restart of the phone, after which the first
     * starts again from nothing and would otherwise come round to the same reading.
     */
    private fun justSet(prefs: android.content.SharedPreferences): Boolean {
        val setAt = prefs.getLong(KEY_SET_AT, 0L)
        if (setAt == 0L) return false
        val running = SystemClock.elapsedRealtime() - setAt
        val clock = System.currentTimeMillis() - prefs.getLong(KEY_SET_CLOCK, 0L)
        return running in 0 until CLAIM_WINDOW_MS && clock in -CLAIM_WINDOW_MS until CLAIM_WINDOW_MS * 2
    }

    /** Changes whenever the kept copy is replaced, even by a picture with the same colours. */
    fun stamp(context: Context): Long = File(File(context.applicationContext.filesDir, DIR), FILE).lastModified()

    /** What the wallpaper on screen is right now, as far as anyone may know: its colours. */
    fun currentSignature(context: Context): String? = runCatching {
        WallpaperManager.getInstance(context.applicationContext).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
    }.getOrNull()?.let(::signature)

    private fun save(bitmap: Bitmap, target: File) {
        // A little over half the screen's resolution is plenty; glass softens most of what it shows.
        val scale = (1080f / max(bitmap.width, 1)).coerceAtMost(1f)
        val copy = if (scale < 1f) {
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).roundToInt(), (bitmap.height * scale).roundToInt(), true)
        } else {
            bitmap
        }
        // WEBP_LOSSY arrived in Android 11; before that WEBP meant the same thing.
        @Suppress("DEPRECATION")
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else Bitmap.CompressFormat.WEBP
        target.outputStream().use { copy.compress(format, 92, it) }
        if (copy !== bitmap) copy.recycle()
    }

    private fun forget(context: Context) {
        File(File(context.filesDir, DIR), FILE).delete()
        prefs(context).edit().remove(KEY_SIGNATURE).remove(KEY_SET_AT).remove(KEY_SET_CLOCK).remove(KEY_APPLIED).apply()
    }

    private fun signature(colours: WallpaperColors): String =
        listOfNotNull(colours.primaryColor, colours.secondaryColor, colours.tertiaryColor)
            .joinToString(",") { it.toArgb().toString() }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
