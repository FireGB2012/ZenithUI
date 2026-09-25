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
import android.view.WindowManager
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

    /** How long after Zenith sets a wallpaper a change of colours is taken to be that wallpaper's. */
    private const val CLAIM_WINDOW_MS = 20_000L

    /** Sets [bitmap] as the wallpaper and keeps a copy for the glass. Returns false if Android refused. */
    suspend fun set(context: Context, bitmap: Bitmap, which: Int): Boolean = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val dir = File(app.filesDir, DIR).apply { mkdirs() }
        val pending = File(dir, PENDING)
        // Written aside first: if Android refuses the wallpaper, the copy of the old one stays put.
        val saved = runCatching { save(bitmap, pending) }.isSuccess
        val ok = runCatching {
            WallpaperManager.getInstance(app).setBitmap(bitmap, null, true, which)
            true
        }.getOrDefault(false)
        if (!ok || !saved) {
            pending.delete()
            // Set without a copy: whatever copy there was no longer matches the screen.
            if (ok) forget(app)
            return@withContext ok
        }
        pending.renameTo(File(dir, FILE))
        // The colours Android is about to announce belong to this picture. The old signature is kept
        // until then, which also covers a new wallpaper whose colours come out exactly the same.
        prefs(app).edit().putLong(KEY_SET_AT, SystemClock.elapsedRealtime()).apply()
        true
    }

    /**
     * Takes a photo of any shape and makes it the wallpaper: scaled and centre-cropped to exactly
     * this screen, so the copy the glass uses lines up with what Android shows pixel for pixel.
     */
    suspend fun setPhoto(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val bounds = context.getSystemService(WindowManager::class.java).maximumWindowMetrics.bounds
        val width = bounds.width()
        val height = bounds.height()
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
        val setAt = prefs.getLong(KEY_SET_AT, 0L)
        if (setAt == 0L || colours == null) return
        if (SystemClock.elapsedRealtime() - setAt > CLAIM_WINDOW_MS) return
        prefs.edit().putString(KEY_SIGNATURE, signature(colours)).remove(KEY_SET_AT).apply()
    }

    /** The kept copy, if it is still the wallpaper on screen; otherwise null. */
    fun load(context: Context): Bitmap? {
        val app = context.applicationContext
        val file = File(File(app.filesDir, DIR), FILE)
        if (!file.exists()) return null
        val manager = WallpaperManager.getInstance(app)
        // A live wallpaper moves; a still copy of anything behind it would be a lie.
        if (runCatching { manager.wallpaperInfo != null }.getOrDefault(false)) return null
        val prefs = prefs(app)
        val justSet = SystemClock.elapsedRealtime() - prefs.getLong(KEY_SET_AT, 0L) < CLAIM_WINDOW_MS &&
            prefs.getLong(KEY_SET_AT, 0L) != 0L
        val expected = prefs.getString(KEY_SIGNATURE, null)
        // Just set by Zenith, before Android has finished working its colours out: it is ours.
        if (!justSet && (expected == null || currentSignature(app) != expected)) return null
        return runCatching { BitmapFactory.decodeFile(file.path) }.getOrNull()
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
        target.outputStream().use { copy.compress(Bitmap.CompressFormat.WEBP_LOSSY, 92, it) }
        if (copy !== bitmap) copy.recycle()
    }

    private fun forget(context: Context) {
        File(File(context.filesDir, DIR), FILE).delete()
        prefs(context).edit().remove(KEY_SIGNATURE).remove(KEY_SET_AT).apply()
    }

    private fun signature(colours: WallpaperColors): String =
        listOfNotNull(colours.primaryColor, colours.secondaryColor, colours.tertiaryColor)
            .joinToString(",") { it.toArgb().toString() }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
