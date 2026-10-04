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
 * The copy is only trusted while it is still the wallpaper. Android numbers every wallpaper it
 * is given: Zenith notes the number of each one it sets, and when the wallpaper on screen has
 * another number it was changed somewhere else and the copy is ignored (see [WallpaperTrust]).
 * The wallpaper's colours, noted when Android announces them, do the same for a copy made before
 * numbers were kept; they cannot be the only test, because not every phone announces them.
 */
object GlassWallpaper {
    private const val DIR = "glass"
    private const val FILE = "wallpaper.webp"
    private const val PENDING = "wallpaper.pending"
    private const val PREFS = "glass_wallpaper"
    private const val KEY_ID = "id"
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
        val manager = WallpaperManager.getInstance(app)
        val claimedAt = prefs.getLong(KEY_SET_AT, 0L)
        val claimedClock = prefs.getLong(KEY_SET_CLOCK, 0L)
        // Claimed before Android is asked, not after: the wallpaper has its new number, and may
        // have its colours announced, before setBitmap has returned, and anyone asking in those
        // moments must be told it is this picture's.
        prefs.edit()
            .putLong(KEY_SET_AT, SystemClock.elapsedRealtime())
            .putLong(KEY_SET_CLOCK, System.currentTimeMillis())
            .commit()

        /** Nothing changed on screen, so nothing was claimed either. */
        fun unclaim() {
            pending.delete()
            prefs.edit().putLong(KEY_SET_AT, claimedAt).putLong(KEY_SET_CLOCK, claimedClock).apply()
        }

        val before = systemId(manager)
        val returned = runCatching { manager.setBitmap(bitmap, null, true, which) }.getOrNull()
        if (returned == null) {
            unclaim()
            return@withContext false
        }
        val after = systemId(manager)
        val id = WallpaperTrust.idAfterSet(before, returned, after)
        if (id == 0 && after > 0) {
            // Android took the call and the wallpaper is the one it was. Either it may not be
            // changed on this phone, or it was handed the very picture it already shows. The
            // kept copy says which: only the same picture again makes the same copy.
            val same = saved && runCatching { pending.readBytes().contentEquals(File(dir, FILE).readBytes()) }.getOrDefault(false)
            unclaim()
            return@withContext same
        }
        if (!saved) {
            pending.delete()
            // Set without a copy: whatever copy there was no longer matches the screen.
            forget(app)
            return@withContext true
        }
        pending.renameTo(File(dir, FILE))
        prefs.edit().apply {
            if (key == null) remove(KEY_APPLIED) else putString(KEY_APPLIED, key)
            // The number is what vouches for the copy from now on. A phone that gives none
            // (id is 0 here only when it will not say any number at all) is left to the colours.
            if (id > 0) putInt(KEY_ID, id) else remove(KEY_ID)
        }.apply()
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

    /**
     * Android announced new wallpaper colours. If Zenith set that wallpaper just now, they are its
     * signature: what a phone that gives wallpapers no number is left to know the copy by.
     */
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
        val storedId = prefs.getInt(KEY_ID, 0)
        val currentId = systemId(manager)
        val current = WallpaperTrust.isCurrent(
            // Just set by Zenith, while Android is still taking the picture in: it is ours.
            justSet = justSet(prefs),
            storedId = storedId,
            currentId = currentId,
            storedColours = prefs.getString(KEY_SIGNATURE, null),
            currentColours = { currentSignature(app) },
        )
        // A copy vouched for by its colours alone, from before numbers were noted, is given the
        // number of the wallpaper it has just been matched to, and is known by that from now on.
        if (current && storedId <= 0 && currentId > 0 && !justSet(prefs)) {
            prefs.edit().putInt(KEY_ID, currentId).apply()
        }
        return current
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

    /** The colours of the wallpaper on screen right now, which anyone may read. */
    fun currentSignature(context: Context): String? = runCatching {
        WallpaperManager.getInstance(context.applicationContext).getWallpaperColors(WallpaperManager.FLAG_SYSTEM)
    }.getOrNull()?.let(::signature)

    /**
     * What the wallpaper on screen is right now, as far as anyone may know, in a word that
     * changes when the wallpaper does: its number, or its colours on a phone that gives none.
     */
    fun screenKey(context: Context): String {
        val id = systemId(WallpaperManager.getInstance(context.applicationContext))
        return if (id > 0) "id:$id" else "colours:${currentSignature(context)}"
    }

    /** The number Android gave the wallpaper now on the home screen, or -1 if it will not say. */
    private fun systemId(manager: WallpaperManager): Int =
        runCatching { manager.getWallpaperId(WallpaperManager.FLAG_SYSTEM) }.getOrDefault(-1)

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
        prefs(context).edit().remove(KEY_ID).remove(KEY_SIGNATURE).remove(KEY_SET_AT).remove(KEY_SET_CLOCK).remove(KEY_APPLIED).apply()
    }

    private fun signature(colours: WallpaperColors): String =
        listOfNotNull(colours.primaryColor, colours.secondaryColor, colours.tertiaryColor)
            .joinToString(",") { it.toArgb().toString() }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
