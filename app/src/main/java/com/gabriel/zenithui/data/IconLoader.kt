package com.gabriel.zenithui.data

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.gabriel.zenithui.ui.theme.SUPERELLIPSE_STEPS
import com.gabriel.zenithui.ui.theme.SquircleShape
import com.gabriel.zenithui.ui.theme.superellipseRadius
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** Builds masked, cached icon bitmaps, honouring the selected icon pack and icon shape. */
class IconLoader(context: Context, private val repo: AppRepository) {

    private val appContext = context.applicationContext
    private val density = appContext.resources.displayMetrics.densityDpi
    /** Sized by memory rather than count, so a big cache never makes Zenith the first thing Android closes. */
    private val cache = object : LruCache<String, ImageBitmap>((Runtime.getRuntime().maxMemory() / 8).toInt()) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }
    private val lock = Mutex()

    /**
     * Rendered icons are also saved here, so a fresh start draws the home screen complete at once:
     * a folder for each way Zenith has drawn them ([RENDER_LOOK]), so none drawn the old way is ever
     * shown again.
     */
    private val diskDir = java.io.File(appContext.cacheDir, "icons-$RENDER_LOOK").apply { mkdirs() }

    /** Icons rendered by this run, as opposed to copies read back from an earlier one. */
    private val fresh = java.util.Collections.synchronizedSet(HashSet<String>())

    /** False until the icon pack has actually been loaded; icons are not rendered before then. */
    @Volatile private var configured = false

    private var pack: IconPack? = null
    private var packName: String = ""
    private var shape: String = "squircle"

    /** The pictures you chose for single apps instead of their own, by app key: see [IconSource]. */
    @Volatile private var chosen: Map<String, String> = emptyMap()

    /** Photos chosen as icons. Kept with Zenith's files, not its cache, so Android never clears them. */
    val photoDir = java.io.File(appContext.filesDir, "icons").apply { mkdirs() }

    private val packResources = HashMap<String, android.content.res.Resources?>()

    private val _version = MutableStateFlow(0)

    /** Bumped whenever the rendered look of every icon changes. */
    val version: StateFlow<Int> = _version.asStateFlow()

    /**
     * Sets which look saved icons belong to, before anything is drawn, so they can be found on the
     * very first frame. The pack itself is loaded later by [configure].
     */
    fun prime(iconPack: String, iconShape: String) {
        if (configured) return
        packName = iconPack
        shape = iconShape
    }

    suspend fun configure(iconPack: String, iconShape: String) {
        if (configured && iconPack == packName && iconShape == shape) return
        withContext(Dispatchers.IO) {
            lock.withLock {
                val lookChanged = configured && (iconPack != packName || iconShape != shape)
                val loadPack = !configured || iconPack != packName
                shape = iconShape
                packName = iconPack
                if (loadPack) {
                    pack = if (iconPack.isEmpty()) null else IconPack(appContext, iconPack).takeIf { it.load() }
                }
                if (lookChanged) {
                    cache.evictAll()
                    // Kept icons are of the old look too; whoever kept them asks again (see warm).
                    kept.clear()
                    fresh.clear()
                    diskDir.listFiles()?.forEach { it.delete() }
                }
                if (!configured) forgetOldLooks()
                configured = true
            }
        }
        _version.value = _version.value + 1
    }

    /**
     * Which apps have a picture you chose for them. Every icon asks again when this changes; only
     * the ones that did change are drawn again, since what was chosen is part of what an icon is
     * saved under.
     */
    fun setChosen(icons: Map<String, String>) {
        if (icons == chosen) return
        chosen = icons
        _version.value = _version.value + 1
    }

    /** True when [key] has a picture you chose, which is shown as chosen whatever the icon style. */
    fun hasChosen(key: String): Boolean = chosen.containsKey(key)

    /**
     * The outline every icon's picture is cut to, one of [IconShapes.ids]: what the light on an icon
     * follows, whatever shape its caller draws a plate in. A change bumps [version].
     */
    val iconShape: String get() = shape

    /**
     * Keeps the photo at [uri] as an icon: the middle of it, square, at the size icons are drawn at
     * their largest. Returns what it is kept as, for [IconSource.photo], or null if it cannot be read.
     */
    suspend fun keepPhoto(uri: android.net.Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val source = android.graphics.ImageDecoder.createSource(appContext.contentResolver, uri)
            val decoded = android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                // No need to hold a whole camera picture to keep 288 pixels of it.
                val shortest = minOf(info.size.width, info.size.height)
                if (shortest > PHOTO_SIDE * 2) decoder.setTargetSampleSize(shortest / (PHOTO_SIDE * 2))
            }
            val side = minOf(decoded.width, decoded.height)
            val square = Bitmap.createBitmap(decoded, (decoded.width - side) / 2, (decoded.height - side) / 2, side, side)
            val kept = Bitmap.createScaledBitmap(square, PHOTO_SIDE, PHOTO_SIDE, true)
            val name = "photo-${System.currentTimeMillis()}.png"
            java.io.File(photoDir, name).outputStream().use { kept.compress(Bitmap.CompressFormat.PNG, 100, it) }
            name
        }.getOrNull()
    }

    /** Whether [source] is a photo this phone has. */
    fun hasPhoto(source: String): Boolean {
        val photo = IconSource.parse(source) as? IconSource.Photo ?: return false
        return java.io.File(photoDir, photo.file).exists()
    }

    /** Lets a photo go once no app uses it any more. */
    fun forgetPhoto(source: String) {
        val photo = IconSource.parse(source) as? IconSource.Photo ?: return
        runCatching { java.io.File(photoDir, photo.file).delete() }
    }

    /**
     * Lets go of every photo not in [inUse]: those of a Look deleted, or of a home screen another
     * Look was put on over. One kept in the last minute is left alone: it is on its way to an app.
     */
    fun tidyPhotos(inUse: Set<String>) {
        val recent = System.currentTimeMillis() - 60_000
        photoDir.listFiles()?.forEach { file ->
            if (IconSource.photo(file.name) !in inUse && file.lastModified() < recent) runCatching { file.delete() }
        }
    }

    /**
     * The icon at once if there is one to hand — in memory, or saved by an earlier run — so the home
     * screen never draws a blank and then fills in icon by icon.
     */
    fun peek(key: String, sizePx: Int): ImageBitmap? = peekSaved(cacheKey(key, sizePx))

    /** The app's one-colour symbol, white, if it has been drawn before: see [loadGlyph]. */
    fun peekGlyph(key: String, sizePx: Int): ImageBitmap? = peekSaved(glyphKey(key, sizePx))

    private fun peekSaved(cacheKey: String): ImageBitmap? {
        inMemory(cacheKey)?.let { return it }
        val file = fileFor(cacheKey)
        if (!file.exists()) return null
        val saved = runCatching { android.graphics.BitmapFactory.decodeFile(file.path)?.asImageBitmap() }.getOrNull()
            ?: return null
        cache.put(cacheKey, saved)
        return saved
    }

    suspend fun load(key: String, sizePx: Int): ImageBitmap? = loadAs(key, cacheKey(key, sizePx)) { render(it, sizePx) }

    /**
     * What Tinted and Glass icons are made of: the app's symbol alone, in white, where its icon
     * would put it. The adaptive icon's monochrome layer when it has one (Android 13's themed
     * icons); otherwise its foreground, when that is a logo on its own; otherwise whatever stands
     * out of the icon from the colour of its own tile.
     */
    suspend fun loadGlyph(key: String, sizePx: Int): ImageBitmap? = loadAs(key, glyphKey(key, sizePx)) { renderGlyph(it, sizePx) }

    private suspend fun loadAs(key: String, cacheKey: String, draw: (Drawable) -> Bitmap): ImageBitmap? {
        // A saved copy is shown straight away, but rendered once more each run in case the app has
        // changed its icon since.
        if (cacheKey in fresh) inMemory(cacheKey)?.let { return it }
        if (!configured) return peekSaved(cacheKey)
        return withContext(Dispatchers.IO) { drawNow(key, cacheKey, draw) }
    }

    /** Draws [key]'s picture with [draw] on this thread, keeps it in memory and saves it: see [loadAs]. */
    private fun drawNow(key: String, cacheKey: String, draw: (Drawable) -> Bitmap): ImageBitmap? {
        val drawable = drawableFor(key) ?: return peekSaved(cacheKey)
        val bitmap = runCatching { draw(drawable) }.getOrNull() ?: return peekSaved(cacheKey)
        val rendered = bitmap.asImageBitmap()
        cache.put(cacheKey, rendered)
        // One the drawer keeps to hand is swapped for the new drawing in place.
        if (kept.containsKey(cacheKey)) kept[cacheKey] = rendered
        fresh += cacheKey
        // A pack's pictures shown only to be chosen from are not worth a file each.
        if (!IconSource.isSource(key)) runCatching {
            fileFor(cacheKey).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        return rendered
    }

    // ------------------------------------------------------------ kept to hand

    /**
     * Icons held in memory for as long as they are wanted, out of the cache's reach: every app at
     * the size the drawer draws it, by what [warm] was asked to keep them for. The cache may let any
     * other icon go to make room; these stay, so the drawer never waits for one, however long it is
     * since it was last opened.
     */
    private val kept = java.util.concurrent.ConcurrentHashMap<String, ImageBitmap>()

    /** Which of [kept] each caller of [warm] asked for, so one asking again lets go of only its own. */
    private val keptFor = HashMap<String, Set<String>>()

    /** The icon only if it is already in memory: it never reads a file, so it is safe in a frame. */
    fun peekMemory(key: String, sizePx: Int): ImageBitmap? = inMemory(cacheKey(key, sizePx))

    /** The app's symbol only if it is already in memory: see [peekMemory] and [loadGlyph]. */
    fun peekGlyphMemory(key: String, sizePx: Int): ImageBitmap? = inMemory(glyphKey(key, sizePx))

    private fun inMemory(cacheKey: String): ImageBitmap? = kept[cacheKey] ?: cache.get(cacheKey)

    /**
     * Draws every one of [keys] at [sizePx] now, off the main thread and behind everything else, and
     * keeps them in memory until [purpose] asks again with others: the drawer's icons, worked out
     * once when the launcher starts rather than one by one as its rows come into view. One already
     * drawn this run costs nothing, so asking again after an app is installed draws only that app.
     * With [glyphs] (Tinted and Glass icons) an app is drawn as its symbol instead, as those styles
     * show it, unless it has a picture you chose, which is shown as it is. [onEach] hears how far it
     * has got, as it goes: setup's "Optimizing Zenith" shows it.
     */
    suspend fun warm(
        purpose: String,
        keys: List<String>,
        sizePx: Int,
        glyphs: Boolean,
        onEach: (done: Int, total: Int) -> Unit = { _, _ -> },
    ): Int = withContext(warmer) {
        if (!configured || sizePx <= 0) return@withContext 0
        val wanted = LinkedHashMap<String, Pair<String, Boolean>>()
        for (key in keys) {
            val symbol = glyphs && !IconSource.isSource(key) && !hasChosen(key)
            wanted[if (symbol) glyphKey(key, sizePx) else cacheKey(key, sizePx)] = key to symbol
        }
        // What this purpose kept before and does not want now goes back to the cache's care.
        synchronized(keptFor) {
            keptFor[purpose].orEmpty().forEach { old ->
                if (old !in wanted && keptFor.none { (other, keys) -> other != purpose && old in keys }) kept.remove(old)
            }
            keptFor[purpose] = wanted.keys.toSet()
        }
        var done = 0
        for ((cacheKey, which) in wanted) {
            ensureActive()
            val (key, glyph) = which
            val ready = if (cacheKey in fresh) inMemory(cacheKey) else null
            val icon = ready ?: drawNow(key, cacheKey) { if (glyph) renderGlyph(it, sizePx) else render(it, sizePx) }
            if (icon != null) kept[cacheKey] = icon
            done++
            onEach(done, wanted.size)
        }
        done
    }

    /**
     * Forgets that the icons of [packages] were drawn this run, after Android said those apps were
     * installed again, updated or changed: the next [load] or [warm] draws them afresh, while what
     * is in memory is shown until it does.
     */
    fun forget(packages: Set<String>) {
        if (packages.isEmpty()) return
        synchronized(fresh) { fresh.removeAll { IconKeys.packageOf(it) in packages } }
    }

    private fun cacheKey(key: String, sizePx: Int) = "$key|$sizePx|$shape|$packName|${chosen[key].orEmpty()}"

    private fun glyphKey(key: String, sizePx: Int) = "glyph|" + cacheKey(key, sizePx)

    private fun fileFor(cacheKey: String) =
        java.io.File(diskDir, Integer.toHexString(cacheKey.hashCode()) + "-" + cacheKey.length + ".png")

    /** Lets go of icons saved the way an earlier Zenith drew them, which are never read again. */
    private fun forgetOldLooks() {
        appContext.cacheDir.listFiles()?.forEach { dir ->
            if (dir.isDirectory && dir.name.startsWith("icons") && dir.name != diskDir.name) runCatching { dir.deleteRecursively() }
        }
    }

    /**
     * Icons are asked for the moment the home screen composes, which can be before the app list
     * has finished loading. Bumping the version makes every icon ask again.
     */
    fun invalidate() {
        _version.value = _version.value + 1
    }

    private fun drawableFor(key: String): Drawable? {
        // A picture asked for by itself: one of a pack's, shown in the picker before it is chosen.
        IconSource.parse(key)?.let { return sourceDrawable(it) }
        if (ShortcutKeys.isShortcut(key)) return repo.shortcutIcon(key, density)
        if (WorkKeys.isWork(key)) return repo.workIcon(key, density)
        // One you chose for this app, unless it has gone (a pack uninstalled, a photo cleared).
        chosen[key]?.let(IconSource::parse)?.let(::sourceDrawable)?.let { return it }
        val parts = key.split('/', limit = 2)
        if (parts.size == 2) {
            pack?.drawableFor(ComponentName(parts[0], parts[1]))?.let { return it }
        }
        return repo.originalIcon(key, density) ?: repo.systemIcon(key, density)
    }

    private fun sourceDrawable(source: IconSource): Drawable? = runCatching {
        when (source) {
            is IconSource.Pack -> {
                val res = synchronized(packResources) {
                    packResources.getOrPut(source.packageName) {
                        runCatching { appContext.packageManager.getResourcesForApplication(source.packageName) }.getOrNull()
                    }
                } ?: return@runCatching null
                val id = res.getIdentifier(source.drawable, "drawable", source.packageName)
                if (id == 0) null else androidx.core.content.res.ResourcesCompat.getDrawable(res, id, null)
            }
            is IconSource.Photo -> {
                val file = java.io.File(photoDir, source.file)
                if (!file.exists()) null
                else android.graphics.BitmapFactory.decodeFile(file.path)
                    ?.let { android.graphics.drawable.BitmapDrawable(appContext.resources, it) }
            }
        }
    }.getOrNull()

    // ------------------------------------------------------------ colours

    private val colours = HashMap<String, IconColour>()

    /**
     * The main colour of each app's icon, for the drawer's By colour: what its coloured pixels
     * come to together, the more coloured the more they count, so a white logo on a red tile is red.
     */
    suspend fun colours(keys: List<String>): Map<String, IconColour> = withContext(Dispatchers.IO) {
        val found = HashMap<String, IconColour>()
        for (key in keys) {
            synchronized(colours) { colours[key] }?.let { found[key] = it; continue }
            val picture = load(key, 64) ?: continue
            val colour = mainColour(picture) ?: continue
            synchronized(colours) { colours[key] = colour }
            found[key] = colour
        }
        found
    }

    private fun mainColour(picture: ImageBitmap): IconColour? {
        val bitmap = runCatching { picture.asAndroidBitmap() }.getOrNull() ?: return null
        val w = bitmap.width
        val h = bitmap.height
        val pixels = IntArray(w * h)
        bitmap.getPixels(pixels, 0, w, 0, 0, w, h)
        var r = 0.0
        var g = 0.0
        var b = 0.0
        var weight = 0.0
        for (p in pixels) {
            if ((p ushr 24) < 160) continue
            val pr = (p shr 16) and 0xFF
            val pg = (p shr 8) and 0xFF
            val pb = p and 0xFF
            val chroma = (maxOf(pr, pg, pb) - minOf(pr, pg, pb)) / 255.0
            val w8 = 0.05 + chroma * chroma
            r += pr * w8
            g += pg * w8
            b += pb * w8
            weight += w8
        }
        if (weight <= 0.0) return null
        return IconColour.of((r / weight).toInt(), (g / weight).toInt(), (b / weight).toInt())
    }

    // ------------------------------------------------------------ symbols

    private fun renderGlyph(drawable: Drawable, size: Int): Bitmap {
        val adaptive = drawable as? AdaptiveIconDrawable
        // Drawn over the same room as the layers of an ordinary icon, so the symbol is the size
        // the logo would be.
        val inset = (size * 0.25f).toInt()
        val bounds = Rect(-inset, -inset, size + inset, size + inset)
        if (adaptive != null) {
            if (android.os.Build.VERSION.SDK_INT >= 33) {
                adaptive.monochrome?.let { return silhouette(layer(it, bounds, size), shade = false) }
            }
            adaptive.foreground?.let { foreground ->
                val drawn = layer(foreground, bounds, size)
                // A logo on its own, the background left to the other layer: its outline is the symbol.
                if (coverage(drawn) < 0.5f) return silhouette(drawn, shade = true)
                drawn.recycle()
            }
        }
        // As its app drew it, with no lift: the logo's shadow would stand out of the tile too.
        val whole = render(drawable, size, lift = false)
        return standOut(whole).also { whole.recycle() }
    }

    private fun layer(drawable: Drawable, bounds: Rect, size: Int): Bitmap {
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawable.bounds = bounds
        drawable.draw(Canvas(bitmap))
        return bitmap
    }

    /** How much of the tile a picture covers, from 0 to 1. */
    private fun coverage(bitmap: Bitmap): Float {
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        return pixels.count { (it ushr 24) > 24 } / pixels.size.toFloat()
    }

    /**
     * [source] in white, shaped by where it is drawn. With [shade] the darker parts of it come out
     * a little fainter, so a logo of many colours keeps something of its inside rather than
     * becoming one blot.
     */
    private fun silhouette(source: Bitmap, shade: Boolean): Bitmap {
        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)
        source.recycle()
        var brightest = 0f
        if (shade) {
            for (p in pixels) if ((p ushr 24) > 128) brightest = maxOf(brightest, luminance(p))
        }
        for (i in pixels.indices) {
            val p = pixels[i]
            var alpha = (p ushr 24) / 255f
            if (shade && brightest > 0.01f) alpha *= 0.45f + 0.55f * (luminance(p) / brightest).coerceIn(0f, 1f)
            pixels[i] = ((alpha * 255f).toInt().coerceIn(0, 255) shl 24) or 0xFFFFFF
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    /**
     * The symbol of an icon that has none of its own: whatever stands out from the colour of the
     * icon's own tile, lighter or darker, comes out white. The tile's colour is read a little in
     * from its outline, and the outline itself is left out, so the rim never becomes part of it.
     */
    private fun standOut(icon: Bitmap): Bitmap {
        val size = icon.width
        val pixels = IntArray(size * size)
        icon.getPixels(pixels, 0, size, 0, 0, size, size)
        val mid = size / 2f
        var r = 0L
        var g = 0L
        var b = 0L
        var count = 0
        for (k in 0 until 48) {
            val angle = k * (2.0 * Math.PI / 48)
            val x = (mid + cos(angle) * size * 0.40).toInt().coerceIn(0, size - 1)
            val y = (mid + sin(angle) * size * 0.40).toInt().coerceIn(0, size - 1)
            val p = pixels[y * size + x]
            if ((p ushr 24) < 200) continue
            r += (p shr 16) and 0xFF
            g += (p shr 8) and 0xFF
            b += p and 0xFF
            count++
        }
        val tileR = if (count > 0) (r / count).toInt() else 128
        val tileG = if (count > 0) (g / count).toInt() else 128
        val tileB = if (count > 0) (b / count).toInt() else 128
        for (y in 0 until size) {
            for (x in 0 until size) {
                val i = y * size + x
                val p = pixels[i]
                val a = (p ushr 24) / 255f
                if (a < 0.03f) {
                    pixels[i] = 0
                    continue
                }
                val dr = ((p shr 16) and 0xFF) - tileR
                val dg = ((p shr 8) and 0xFF) - tileG
                val db = (p and 0xFF) - tileB
                val apart = kotlin.math.sqrt((dr * dr + dg * dg + db * db).toFloat()) / 441.7f
                val fromMiddle = kotlin.math.hypot(x - mid, y - mid) / mid
                val rim = 1f - smooth(0.82f, 0.94f, fromMiddle)
                val strength = smooth(0.10f, 0.30f, apart) * a * rim
                pixels[i] = ((strength * 255f).toInt().coerceIn(0, 255) shl 24) or 0xFFFFFF
            }
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    private fun luminance(p: Int): Float =
        (0.2126f * ((p shr 16) and 0xFF) + 0.7152f * ((p shr 8) and 0xFF) + 0.0722f * (p and 0xFF)) / 255f

    private fun smooth(from: Float, to: Float, x: Float): Float {
        val t = ((x - from) / (to - from)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    // ------------------------------------------------------------ super icons

    private val wideCache = LruCache<String, ImageBitmap>(48)

    private fun wideKey(key: String, width: Int, height: Int) = "wide|$key|$width|$height|$shape|$packName"

    fun peekWide(key: String, widthPx: Int, heightPx: Int): ImageBitmap? = wideCache.get(wideKey(key, widthPx, heightPx))

    /**
     * An app's icon stretched into a super icon, the way HyperOS draws them: the icon's own
     * background fills the whole shape — wide or tall — and its logo sits in the middle at the
     * usual size.
     */
    suspend fun loadWide(key: String, widthPx: Int, heightPx: Int): ImageBitmap? {
        if (widthPx <= 0 || heightPx <= 0) return null
        val cacheKey = wideKey(key, widthPx, heightPx)
        wideCache.get(cacheKey)?.let { return it }
        if (!configured) return null
        return withContext(Dispatchers.IO) {
            val drawable = drawableFor(key) ?: return@withContext null
            val rendered = runCatching { renderWide(drawable, widthPx, heightPx).asImageBitmap() }.getOrNull()
                ?: return@withContext null
            wideCache.put(cacheKey, rendered)
            rendered
        }
    }

    private fun renderWide(drawable: Drawable, width: Int, height: Int): Bitmap {
        val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        // The short side is what an ordinary icon would be; the long side is the stretch.
        val side = minOf(width, height)
        val radius = side * 0.26f
        canvas.clipPath(Path().apply {
            addRoundRect(RectF(0f, 0f, width.toFloat(), height.toFloat()), radius, radius, Path.Direction.CW)
        })
        val left = (width - side) / 2
        val top = (height - side) / 2

        val adaptive = drawable as? AdaptiveIconDrawable
        if (adaptive != null) {
            // The background covers the whole shape without being squashed: it is drawn square, as
            // long as the icon plus its usual overscan, and centred.
            val cover = (maxOf(width, height) * 1.5f).toInt()
            adaptive.background?.let {
                it.bounds = Rect((width - cover) / 2, (height - cover) / 2, (width + cover) / 2, (height + cover) / 2)
                it.draw(canvas)
            }
            // The logo at exactly the size it has on an ordinary icon, in the middle, lifted off the
            // background as it is there.
            val inset = (side * 0.25f).toInt()
            adaptive.foreground?.let { foreground ->
                val logo = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                foreground.bounds = Rect(left - inset, top - inset, left + side + inset, top + side + inset)
                foreground.draw(Canvas(logo))
                drawLifted(canvas, logo, side, left, top)
                logo.recycle()
            }
            return output
        }

        // An icon with no separate background: its tile colour fills the shape and the icon sits
        // in the middle.
        val square = render(drawable, side)
        val edge = square.getPixel((side * 0.14f).toInt(), side / 2)
        canvas.drawColor(if (Color.alpha(edge) > 200) edge else blend(edge, Color.WHITE, 0.5f))
        canvas.drawBitmap(square, left.toFloat(), top.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        square.recycle()
        return output
    }

    /**
     * Every icon comes out the same size and silhouette. Adaptive icons are masked to the chosen
     * shape; legacy icons are measured, scaled to the same footprint and given a matching tile so
     * they cannot look bigger or smaller than their neighbours. With [lift] a logo that stands on
     * its own is lifted off its tile, as an iPhone's are ([LogoLift]).
     */
    private fun render(drawable: Drawable, size: Int, lift: Boolean = true): Bitmap {
        val shapePath = IconShapes.path(shape, size.toFloat())

        val adaptive = drawable as? AdaptiveIconDrawable
        if (adaptive != null) {
            val layer = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val layerCanvas = Canvas(layer)
            // Adaptive icons reserve the outer 25% for parallax; expand so our mask fills the tile.
            val inset = (size * 0.25f).toInt()
            val bounds = Rect(-inset, -inset, size + inset, size + inset)
            adaptive.background?.let { it.bounds = bounds; it.draw(layerCanvas) }
            adaptive.foreground?.let { foreground ->
                if (lift) {
                    val logo = layer(foreground, bounds, size)
                    drawLifted(layerCanvas, logo, size)
                    logo.recycle()
                } else {
                    foreground.bounds = bounds
                    foreground.draw(layerCanvas)
                }
            }
            val output = maskWith(layer, shapePath, size)
            layer.recycle()
            return output
        }

        val raw = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        drawable.setBounds(0, 0, size, size)
        drawable.draw(Canvas(raw))

        val content = visibleBounds(raw, size)
        if (content == null) {
            raw.recycle()
            return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        }

        // Artwork that already fills its canvas is treated like an adaptive icon.
        val fullBleed = content.width() >= size * 0.86f && content.height() >= size * 0.86f
        if (fullBleed) {
            val output = maskWith(raw, shapePath, size)
            raw.recycle()
            return output
        }

        // Otherwise: a tile in the icon's own colour, with the artwork centred at a fixed scale.
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val tint = averageColour(raw, content)
        val tile = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, size.toFloat(),
                blend(tint, Color.WHITE, 0.62f),
                blend(tint, Color.WHITE, 0.34f),
                Shader.TileMode.CLAMP,
            )
        }
        canvas.drawPath(shapePath, tile)

        val longest = maxOf(content.width(), content.height()).toFloat()
        val target = size * LEGACY_CONTENT_FRACTION
        val scale = target / longest
        val destWidth = content.width() * scale
        val destHeight = content.height() * scale
        val dest = RectF(
            (size - destWidth) / 2f,
            (size - destHeight) / 2f,
            (size + destWidth) / 2f,
            (size + destHeight) / 2f,
        )
        val artwork = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        if (lift) {
            // The artwork is the logo here, on a tile of Zenith's own, so it is lifted off it too.
            val logo = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            Canvas(logo).drawBitmap(raw, content, dest, artwork)
            drawLifted(canvas, logo, size)
            logo.recycle()
        } else {
            canvas.drawBitmap(raw, content, dest, artwork)
        }
        raw.recycle()
        return output
    }

    /**
     * Draws [logo], a picture as big as [canvas], lifted off what is drawn there already, as an
     * iPhone's logos are ([LogoLift]): on a soft shadow falling straight down, its face lit from
     * above, with a hairline of light on its edges. The logo is measured over the square [side]
     * across from [left], [top], where an ordinary icon's would be; one that covers the whole of it,
     * or shows nothing, is drawn as it is.
     */
    private fun drawLifted(canvas: Canvas, logo: Bitmap, side: Int, left: Int = 0, top: Int = 0) {
        val width = logo.width
        val height = logo.height
        val pixels = IntArray(width * height)
        logo.getPixels(pixels, 0, width, 0, 0, width, height)
        val alpha = FloatArray(pixels.size) { (pixels[it] ushr 24) / 255f }
        if (!LogoLift.lifts(LogoLift.coverage(alpha, width, left, top, side))) {
            canvas.drawBitmap(logo, 0f, 0f, null)
            return
        }
        val look = LogoLift.look(side)
        val face = LogoLift.face(alpha, width, height, look)
        val edge = LogoLift.light(alpha, width, height, look)
        val shadow = wash(LogoLift.shadow(alpha, width, height, look), width, height, 0x000000)
        // Over the logo: the shade at the foot of its face, black, then the light at its top and on
        // its edge, white.
        val shade = wash(FloatArray(face.size) { (-face[it]).coerceAtLeast(0f) }, width, height, 0x000000)
        val light = wash(FloatArray(face.size) { (edge[it] + face[it].coerceAtLeast(0f)).coerceAtMost(1f) }, width, height, 0xFFFFFF)
        canvas.drawBitmap(shadow, 0f, 0f, null)
        canvas.drawBitmap(logo, 0f, 0f, null)
        canvas.drawBitmap(shade, 0f, 0f, null)
        canvas.drawBitmap(light, 0f, 0f, null)
        shadow.recycle()
        shade.recycle()
        light.recycle()
    }

    /** One colour, [rgb], laid on as thickly at each pixel as [amount] says, from 0 to 1. */
    private fun wash(amount: FloatArray, width: Int, height: Int, rgb: Int): Bitmap {
        val pixels = IntArray(amount.size) { ((amount[it] * 255f).roundToInt().coerceIn(0, 255) shl 24) or rgb }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun maskWith(source: Bitmap, shapePath: Path, size: Int): Bitmap {
        val output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawPath(shapePath, paint)
        paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(source, 0f, 0f, paint)
        return output
    }

    /** Tight box around everything that is not transparent. */
    private fun visibleBounds(bitmap: Bitmap, size: Int): Rect? {
        val pixels = IntArray(size * size)
        bitmap.getPixels(pixels, 0, size, 0, 0, size, size)
        var left = size
        var top = size
        var right = -1
        var bottom = -1
        for (y in 0 until size) {
            val row = y * size
            for (x in 0 until size) {
                if ((pixels[row + x] ushr 24) > 24) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }
        if (right < left || bottom < top) return null
        return Rect(left, top, right + 1, bottom + 1)
    }

    private fun averageColour(bitmap: Bitmap, bounds: Rect): Int {
        var count = 0
        var r = 0L
        var g = 0L
        var b = 0L
        val step = maxOf(1, bounds.width() / 24)
        var y = bounds.top
        while (y < bounds.bottom) {
            var x = bounds.left
            while (x < bounds.right) {
                val pixel = bitmap.getPixel(x, y)
                if ((pixel ushr 24) > 128) {
                    r += (pixel shr 16) and 0xFF
                    g += (pixel shr 8) and 0xFF
                    b += pixel and 0xFF
                    count++
                }
                x += step
            }
            y += step
        }
        if (count == 0) return Color.rgb(190, 195, 205)
        return Color.rgb((r / count).toInt(), (g / count).toInt(), (b / count).toInt())
    }

    private fun blend(color: Int, towards: Int, amount: Float): Int {
        fun mix(a: Int, b: Int) = (a + (b - a) * amount).toInt().coerceIn(0, 255)
        return Color.rgb(
            mix(Color.red(color), Color.red(towards)),
            mix(Color.green(color), Color.green(towards)),
            mix(Color.blue(color), Color.blue(towards)),
        )
    }

    private companion object {
        /** How much of the tile a legacy icon's artwork fills. */
        const val LEGACY_CONTENT_FRACTION = 0.62f

        /**
         * Which way icons are drawn, for the folder they are saved in: 2 lifts each logo off its
         * tile ([LogoLift]). A new way of drawing them is a new number.
         */
        const val RENDER_LOOK = 2

        /** The side a photo chosen as an icon is kept at: the most any icon is drawn at. */
        const val PHOTO_SIDE = 288

        /**
         * Drawing ahead of time happens on a thread of its own, at background priority: whatever
         * the screen is doing at the time comes first. One for the whole app, however many times
         * Zenith's screen is made again.
         */
        val warmer by lazy {
            java.util.concurrent.Executors.newSingleThreadExecutor { job ->
                Thread({
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
                    job.run()
                }, "zenith-icons").apply { isDaemon = true }
            }.asCoroutineDispatcher()
        }
    }
}

/**
 * Icon masks, shared by the renderer and the Compose UI, so a tile and its picture always agree.
 * Saved icon pictures are kept under a shape's id, so an id's outline never changes: a new outline
 * is a new id, added at the end.
 */
object IconShapes {

    val ids = listOf("squircle", "circle", "rounded", "square", "pebble", "teardrop", "hexagon", "clover", "iphone")

    fun label(id: String): String = when (id) {
        "circle" -> "Circle"
        "rounded" -> "Rounded"
        "square" -> "Square"
        "pebble" -> "Pebble"
        "teardrop" -> "Teardrop"
        "hexagon" -> "Hexagon"
        "clover" -> "Clover"
        "iphone" -> "iPhone"
        else -> "Squircle"
    }

    fun path(shape: String, size: Float): Path {
        val path = Path()
        val r = size / 2f
        when (shape) {
            "circle" -> path.addCircle(r, r, r, Path.Direction.CW)
            "square" -> path.addRect(0f, 0f, size, size, Path.Direction.CW)
            "rounded" -> path.addRoundRect(RectF(0f, 0f, size, size), size * 0.22f, size * 0.22f, Path.Direction.CW)
            "pebble" -> pebble(path, size)
            "teardrop" -> {
                // Round everywhere but the corner a drop hangs from.
                val tip = size * 0.15f
                path.addRoundRect(
                    RectF(0f, 0f, size, size),
                    floatArrayOf(r, r, r, r, tip, tip, r, r),
                    Path.Direction.CW,
                )
            }
            "hexagon" -> roundedPolygon(
                path, size,
                listOf(0.5f to 0f, 1f to 0.25f, 1f to 0.75f, 0.5f to 1f, 0f to 0.75f, 0f to 0.25f),
                corner = 0.2f,
            )
            "clover" -> clover(path, size)
            // An iPhone's rounded square: flatter sides than the squircle's and fuller, continuous corners.
            "iphone" -> superellipse(path, size, 5f)
            else -> superellipse(path, size, 2.9f)
        }
        return path
    }

    /** A worn stone: full on one side, a little flatter on the other, never quite symmetrical. */
    private fun pebble(path: Path, size: Float) {
        val u = size / 100f
        path.moveTo(55f * u, 0f)
        path.cubicTo(25f * u, 0f, 0f, 25f * u, 0f, 50f * u)
        path.cubicTo(0f, 78f * u, 28f * u, 100f * u, 55f * u, 100f * u)
        path.cubicTo(85f * u, 100f * u, 100f * u, 85f * u, 100f * u, 58f * u)
        path.cubicTo(100f * u, 30f * u, 86f * u, 0f, 55f * u, 0f)
        path.close()
    }

    /** A polygon with its corners turned, given as points in a unit square. */
    private fun roundedPolygon(path: Path, size: Float, points: List<Pair<Float, Float>>, corner: Float) {
        val n = points.size
        fun at(i: Int) = points[(i + n) % n].let { (x, y) -> x * size to y * size }
        for (i in 0 until n) {
            val (px, py) = at(i - 1)
            val (cx, cy) = at(i)
            val (nx, ny) = at(i + 1)
            val inX = cx + (px - cx) * corner
            val inY = cy + (py - cy) * corner
            val outX = cx + (nx - cx) * corner
            val outY = cy + (ny - cy) * corner
            if (i == 0) path.moveTo(inX, inY) else path.lineTo(inX, inY)
            path.quadTo(cx, cy, outX, outY)
        }
        path.close()
    }

    /** Four leaves round a middle, touching every edge of the tile. */
    private fun clover(path: Path, size: Float) {
        val leaf = size * 0.27f
        val reach = size * 0.23f
        val mid = size / 2f
        path.addCircle(mid, mid - reach, leaf, Path.Direction.CW)
        listOf(mid + reach to mid, mid to mid + reach, mid - reach to mid).forEach { (x, y) ->
            path.op(Path().apply { addCircle(x, y, leaf, Path.Direction.CW) }, Path.Op.UNION)
        }
        path.op(Path().apply { addCircle(mid, mid, size * 0.3f, Path.Direction.CW) }, Path.Op.UNION)
    }

    /** Traced point for point as [SquircleShape] traces it, so a tile and the icon cut for it are one shape. */
    private fun superellipse(path: Path, size: Float, exponent: Float) {
        val r = size / 2f
        for (i in 0..SUPERELLIPSE_STEPS) {
            val angle = (i.toFloat() / SUPERELLIPSE_STEPS) * (2f * Math.PI).toFloat()
            val reach = superellipseRadius(angle, exponent)
            val x = r + r * reach * cos(angle)
            val y = r + r * reach * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
    }
}
