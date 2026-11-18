package com.gabriel.zenithui.data

import android.graphics.Bitmap
import android.os.SystemClock
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * When the app picture helper ([PictureService]) takes a picture, which ones it keeps and for how
 * long. Plain numbers and sums, so they can be tested without a phone.
 */
object PictureRules {
    /**
     * Whether the helper is offered at all: from Android 11, where an accessibility service can take
     * a screenshot, and only in a copy of Zenith that has it ([declared]). The copy that installs
     * by tapping it goes without, since Play Protect refuses a sideloaded app with one, and so does
     * the Play build until Play's Accessibility API declaration is made.
     */
    fun offered(sdk: Int, declared: Boolean): Boolean = sdk >= 30 && declared

    /** How long an app's screen has to have been in front before its picture is taken. */
    const val SETTLE_MS = 700L

    /** Between pictures while the same app stays in front. */
    const val REFRESH_MS = 3_000L

    /**
     * How long the app has to stay in front after a picture for it to count. A swipe home moves the
     * app before Android says anything has changed, and a picture taken then would show it on its
     * way down.
     */
    const val CONFIRM_MS = 2_000L

    /** A picture older than this is not used for opening: the app may have had to start again. */
    const val OPEN_FRESH_MS = 5 * 60_000L

    /**
     * How long after an app has gone it still counts as the one being closed: Android says it has
     * gone as a swipe home starts, and shows the home screen once its own close is done.
     */
    const val LEFT_FRESH_MS = 3_000L

    /** Pictures of at most this many apps are kept. */
    const val KEEP_APPS = 4

    /** A picture is kept at most this many pixels along its longer side. */
    const val LONG_SIDE = 1280

    /** The status and navigation bars, at the top and foot of a picture, as shares of its height. */
    private const val BARS = 0.06f

    /** A picture this much black between the bars is an app that hides itself from screenshots. */
    private const val BLANK_SHARE = 0.97f

    /** Darker than this in every channel is black. */
    private const val BLACK = 8

    /** What came to the front, by what Android says of it. */
    enum class Front {
        /** An app with an icon: its picture can be taken. */
        App,
        /** A home app, or recents, which is a home app's. */
        Home,
        /** Over an app, without it having gone: Zenith's own windows (it says for itself when the home screen is on show), a keyboard. */
        Ignore,
        /** Anything else: System UI, the lock screen, a dialog of the system's. */
        Other,
    }

    fun kind(pkg: String?, own: String, homes: Set<String>, keyboards: Set<String>, launchable: (String) -> Boolean): Front = when {
        pkg.isNullOrEmpty() -> Front.Ignore
        pkg == own -> Front.Ignore
        pkg in keyboards -> Front.Ignore
        pkg in homes -> Front.Home
        launchable(pkg) -> Front.App
        else -> Front.Other
    }

    /** The size a [width] by [height] picture is kept at. */
    fun shrunk(width: Int, height: Int): Pair<Int, Int> {
        val long = maxOf(width, height)
        if (long <= LONG_SIDE) return width to height
        val scale = LONG_SIDE.toFloat() / long
        return (width * scale).roundToInt().coerceAtLeast(1) to (height * scale).roundToInt().coerceAtLeast(1)
    }

    /** Whether a [width] by [height] picture can be drawn on a [screenWidth] by [screenHeight] screen: the same way up and much the same shape. */
    fun fits(width: Int, height: Int, screenWidth: Float, screenHeight: Float): Boolean {
        if (width <= 0 || height <= 0 || screenWidth <= 0f || screenHeight <= 0f) return false
        if ((width > height) != (screenWidth > screenHeight)) return false
        val picture = width.toFloat() / height
        val screen = screenWidth / screenHeight
        return abs(picture / screen - 1f) <= 0.12f
    }

    /**
     * Whether [pixels] ([width] by [height], ARGB) are what Android gives for an app that hides
     * itself from screenshots, such as a bank or a private tab: black, apart from the status and
     * navigation bars drawn over it.
     */
    fun blank(pixels: IntArray, width: Int, height: Int): Boolean {
        if (width <= 0 || height <= 0 || pixels.size < width * height) return false
        val top = (height * BARS).toInt()
        val bottom = height - top
        val stepX = (width / 64).coerceAtLeast(1)
        val stepY = (height / 128).coerceAtLeast(1)
        var all = 0
        var dark = 0
        var y = top
        while (y < bottom) {
            var x = 0
            while (x < width) {
                val pixel = pixels[y * width + x]
                all++
                if ((pixel shr 16 and 0xFF) <= BLACK && (pixel shr 8 and 0xFF) <= BLACK && (pixel and 0xFF) <= BLACK) dark++
                x += stepX
            }
            y += stepY
        }
        return all > 0 && dark >= all * BLANK_SHARE
    }
}

/**
 * Pictures of the apps lately in front, newest first for each, as [PictureRules] says. What a
 * picture is does not matter here: on the phone it is a bitmap, in a test a name. [drop] is told of
 * each one let go, so a bitmap can be freed at once.
 *
 * Times are all on one clock, [SystemClock.uptimeMillis] on the phone.
 */
class PictureShelf<T>(private val drop: (T) -> Unit = {}) {

    class Picture<T>(val pkg: String, val image: T, val takenAt: Long, val width: Int, val height: Int)

    private val shelf = LinkedHashMap<String, ArrayDeque<Picture<T>>>()

    /** The app in front, as far as is known; null when what is in front is not an app. */
    var front: String? = null
        private set

    private var frontSince = 0L
    private var left: String? = null
    private var leftAt = 0L
    private var leftSince = 0L

    /** Something came to the front: the app [pkg], or, with null, something that is not an app. */
    @Synchronized
    fun cameToFront(pkg: String?, at: Long) {
        val was = front
        if (was == pkg) return
        if (was != null) leave(was, at)
        front = pkg
        frontSince = at
        prune(at)
    }

    /**
     * When the app [pkg] came to the front this time: while it is in front, and once it has gone
     * for as long as it is the last app to have gone. Null for any other app.
     */
    @Synchronized
    fun visitStart(pkg: String): Long? = when (pkg) {
        front -> frontSince
        left -> leftSince
        else -> null
    }

    /**
     * Something of the system's came over the app in front (the shade, the volume panel, a
     * dialog) without the app going. The pictures of its last moments may show it coming.
     */
    @Synchronized
    fun covered(at: Long) {
        val app = front ?: return
        val pictures = shelf[app] ?: return
        pictures.dropWhere { at - it.takenAt < PictureRules.CONFIRM_MS }
        if (pictures.isEmpty()) shelf.remove(app)
    }

    /** Keeps [picture], if it is of the app in front. False, and let go of, when it is not. */
    @Synchronized
    fun put(picture: Picture<T>): Boolean {
        if (picture.pkg != front) {
            drop(picture.image)
            return false
        }
        val pictures = shelf.getOrPut(picture.pkg) { ArrayDeque() }
        pictures.addFirst(picture)
        // The newest, which is not yet sure, and the one before it, which is.
        while (pictures.size > 2) drop(pictures.removeLast().image)
        prune(picture.takenAt)
        return true
    }

    /** The newest picture of [pkg] that counts and was taken since [since]: for closing it, this time. */
    @Synchronized
    fun forClosing(pkg: String, since: Long, now: Long): Picture<T>? =
        shelf[pkg]?.firstOrNull { it.takenAt >= since && counts(it, now) }

    /** The newest picture of [pkg] that counts and is fresh enough to open it with. */
    @Synchronized
    fun forOpening(pkg: String, now: Long): Picture<T>? =
        shelf[pkg]?.firstOrNull { now - it.takenAt <= PictureRules.OPEN_FRESH_MS && counts(it, now) }

    /** The app being closed now, as far as is known: the one in front, or one that has only just gone. */
    @Synchronized
    fun closingApp(now: Long): String? = front ?: left?.takeIf { now - leftAt <= PictureRules.LEFT_FRESH_MS }

    /** Lets go of every picture, and of which app is in front: the helper has stopped. */
    @Synchronized
    fun clear() {
        dropPictures()
        front = null
        left = null
    }

    /**
     * Lets go of every picture, keeping which app is in front: the screen has gone off, and when
     * it comes back on that app is very likely still there, with nothing to say so.
     */
    @Synchronized
    fun dropPictures() {
        shelf.values.forEach { pictures -> pictures.forEach { drop(it.image) } }
        shelf.clear()
    }

    /** Once an app has gone, whatever is left of it has been sure since it was taken. */
    private fun counts(picture: Picture<T>, now: Long): Boolean =
        picture.pkg != front || now - picture.takenAt >= PictureRules.CONFIRM_MS

    private fun leave(pkg: String, at: Long) {
        left = pkg
        leftAt = at
        leftSince = frontSince
        val pictures = shelf[pkg] ?: return
        // Taken in the last moments it was there: it may already have been on its way out.
        pictures.dropWhere { at - it.takenAt < PictureRules.CONFIRM_MS }
        // The newest that is left is the one it is opened with next time.
        while (pictures.size > 1) drop(pictures.removeLast().image)
        if (pictures.isEmpty()) shelf.remove(pkg)
    }

    private fun prune(now: Long) {
        val iterator = shelf.entries.iterator()
        while (iterator.hasNext()) {
            val (pkg, pictures) = iterator.next()
            if (pkg == front) continue
            while (pictures.isNotEmpty() && now - pictures.last().takenAt > PictureRules.OPEN_FRESH_MS) {
                drop(pictures.removeLast().image)
            }
            if (pictures.isEmpty()) iterator.remove()
        }
        while (shelf.size > PictureRules.KEEP_APPS) {
            val oldest = shelf.entries.filter { it.key != front }.minByOrNull { it.value.first().takenAt } ?: break
            oldest.value.forEach { drop(it.image) }
            shelf.remove(oldest.key)
        }
    }

    private fun ArrayDeque<Picture<T>>.dropWhere(predicate: (Picture<T>) -> Boolean) {
        val iterator = iterator()
        while (iterator.hasNext()) {
            val picture = iterator.next()
            if (predicate(picture)) {
                iterator.remove()
                drop(picture.image)
            }
        }
    }
}

/**
 * The pictures Zenith's app picture helper has taken, for opening and closing apps with. Kept in
 * memory only, in Zenith's own process (the helper runs in it too), and let go of when the screen
 * goes off, when the helper is switched off and when Android is short of memory.
 */
object AppPictures {
    // Not recycled when let go of: a card may still be drawing it, and the few kept are freed
    // with the rest of what is no longer used.
    private val shelf = PictureShelf<Bitmap>()

    /** Whether the helper is running now. Read by Settings and the home screen. */
    @Volatile
    var running = false
        internal set

    /** Whether Zenith's home screen is on show, as Zenith itself says. While it is, no picture is taken. */
    @Volatile
    var homeShowing = false
        private set

    val front: String? get() = shelf.front

    fun cameToFront(pkg: String?, at: Long = SystemClock.uptimeMillis()) = shelf.cameToFront(pkg, at)

    fun covered(at: Long = SystemClock.uptimeMillis()) = shelf.covered(at)

    /** Zenith's own home screen coming on show, or going. Shown, it is in front of every app. */
    fun homeShown(shown: Boolean, at: Long = SystemClock.uptimeMillis()) {
        homeShowing = shown
        if (shown) shelf.cameToFront(null, at)
    }

    fun put(pkg: String, bitmap: Bitmap, takenAt: Long) =
        shelf.put(PictureShelf.Picture(pkg, bitmap, takenAt, bitmap.width, bitmap.height))

    fun forClosing(pkg: String, since: Long, now: Long = SystemClock.uptimeMillis()): Bitmap? =
        shelf.forClosing(pkg, since, now)?.image

    fun forOpening(pkg: String, now: Long = SystemClock.uptimeMillis()): Bitmap? =
        shelf.forOpening(pkg, now)?.image

    fun closingApp(now: Long = SystemClock.uptimeMillis()): String? = shelf.closingApp(now)

    /** When [pkg] came to the front this time, so that closing it only uses a picture from this time. */
    fun visitStart(pkg: String): Long? = shelf.visitStart(pkg)

    fun clear() = shelf.clear()

    fun dropPictures() = shelf.dropPictures()
}
