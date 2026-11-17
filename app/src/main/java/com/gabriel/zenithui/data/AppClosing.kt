package com.gabriel.zenithui.data

import androidx.compose.ui.geometry.Rect
import kotlin.math.roundToInt

/**
 * How an app that has just been closed goes back into its icon. Which one it is depends on what
 * the phone offers and what is on: see `specs/2026-11-16-app-open-close-animations-design.md`.
 */
enum class CloseRoute {
    /** Android springs the app's real window into the icon; Zenith knocks the icons round it once it is there. */
    Handshake,
    /** A card holding a picture of the app shrinks into its icon, or into the middle of the screen. */
    Picture,
    /**
     * A card in the icon's colour, the one the app opened on, shrinks into it, or into the middle of
     * the screen: what every phone gets with no handshake and no picture (the Closing card of
     * Advanced animation settings).
     */
    Card,
    /** Only the icon landing in its place. */
    Land,
    /** Nothing of Zenith's: the home screen comes forward, and that is all. */
    None,
}

/** The rules of closing an app, kept apart from the screen so they can be tested on their own. */
object CloseRules {
    /** Settings → Motion & feel → Closing apps: into its icon, every way the phone allows. */
    const val INTO_ICON = "icon"

    /** Only the landing, and the colour card if it is set to a size: how it was before. */
    const val PLAIN = "plain"

    /** How long after Android's question its answer still decides how the app closes. */
    const val HANDSHAKE_FRESH_MS = 2_000L

    /** How long Android has to say the window has reached the icon before Zenith carries on without it. */
    const val HANDSHAKE_WAIT_MS = 900L

    /** Below this the Closing card is at Auto: [CARD_START] closing Into its icon, no card closing Plain. */
    const val CARD_OFF = 0.005f

    /**
     * How far to full size the closing card sets off from, left at Auto: the whole screen, as
     * Void's does. Zenith waits for Android's own close to be over first (AndroidClose), so the
     * card is the app back as it was left, going into its icon, and not a carrying on from part
     * way. (It was 72% while Zenith's close ran on top of Android's.)
     */
    const val CARD_START = 1f

    /** How solid the app's icon is on a closing card that fills the screen: a ghost of it. */
    const val GHOST_FAINT = 0.06f

    /** The share of the way to full size below which the icon on a closing card is solid. */
    const val GHOST_SOLID = 0.3f

    /** How wide the icon on a closing card is at full size, against the card's own width. */
    const val GHOST_SPAN = 1.12f

    /**
     * How far down the screen the home screen comes forward about, as a share of its height:
     * the middle of its foot, over the dock, where Android's own close leaves the app and Void's
     * home screen comes forward from.
     */
    const val HOME_FOOT = 0.88f

    /**
     * How far a closing card bringing the home screen back has shrunk, as a share of the screen,
     * when the home screen starts forward: once the card is well on its way into its icon, so the
     * home screen is seen coming forward as the card lands, as Void's is, and is not over behind it.
     */
    const val HOME_GOES_AT = 0.7f

    /**
     * How near its shape a landing card's squash has to be to count as back, as a share of its
     * icon: about a pixel. Compose's own, 0.01, is two, a jump the eye catches as the card stops.
     */
    const val SQUASH_REST = 0.005f

    /** How long a closing card takes to give way to the icon under it once it is on it. */
    const val HAND_OVER_MS = 90

    /**
     * How long after resuming Zenith waits to be seen before closing the app anyway. Resuming is
     * part of the same step as starting, and a phone that shows a launcher not built into it only
     * once its own close is done (One UI) resumes it well before then.
     */
    const val SIGHT_WAIT_MS = 450L

    /** Between these shares of the way to full size, a picture of the app takes over from its colour and logo. */
    const val PICTURE_FROM = 0.25f
    const val PICTURE_TO = 0.8f

    /** How much of its way a closing card takes to come in. */
    const val CLOSE_FADE_IN = 0.12f

    /** How much of its way, at the end, a card with no icon to go into takes to fade out. */
    const val VANISH = 0.3f

    /**
     * Which way it closes. [asked]: Android asked where the icon is (the gesture handshake);
     * [target]: there is an icon on show to go into; [picture]: there is a picture of the app from
     * this time it was open; [recent]: it was opened lately enough to be the app now closing;
     * [cardFrom]: the Closing card's size to start from, Auto below [CARD_OFF].
     */
    fun route(style: String, asked: Boolean, target: Boolean, picture: Boolean, recent: Boolean, cardFrom: Float): CloseRoute = when {
        style == PLAIN -> asBefore(target, recent, cardFrom)
        // Answered, Android has already moved the window as far as it can: anything more would
        // only play the close a second time.
        asked -> if (target) CloseRoute.Handshake else CloseRoute.None
        picture -> CloseRoute.Picture
        // Neither (a Galaxy, buttons, a copy with no picture helper): the card the app opened on
        // goes back into its icon, or into the middle of the screen with none on show.
        recent -> CloseRoute.Card
        else -> CloseRoute.None
    }

    /** How far to full size the closing card sets off from: [cardFrom] if it is set to a size, [CARD_START] at Auto. */
    fun cardStart(cardFrom: Float): Float = if (cardFrom > CARD_OFF) cardFrom else CARD_START

    /**
     * How solid the app's icon on a closing card is at [grown] of the way to full size: a ghost
     * of it while the card fills the screen, coming in as it shrinks, and solid by [GHOST_SOLID],
     * so the card is its icon again by the time it gets there, as Void's is.
     */
    fun ghostAlpha(grown: Float): Float {
        val t = ((1f - grown) / (1f - GHOST_SOLID)).coerceIn(0f, 1f)
        return GHOST_FAINT + (1f - GHOST_FAINT) * t * t * (3f - 2f * t)
    }

    /**
     * How wide the app's icon is drawn on a closing card [cardWidth] wide at [grown]: the card's
     * own width at the icon, a little more at full size. It is the icon blown up, cut to the card,
     * so a card wider than it is high shows the middle of it, as a flattened landing does.
     */
    fun ghostSide(cardWidth: Float, grown: Float): Float = cardWidth * (1f + (GHOST_SPAN - 1f) * grown.coerceIn(0f, 1f))

    /**
     * How far into its icon a closing card is, as a share of its way, at [fraction] of its time
     * (Shrinking time): slowly at first, the app back on the screen for a beat, and faster all the
     * way in, so it lands with a bump and squashes (see LaunchMotion.frame), as Void's does. The
     * fraction cubed: an eighth of the way half way through, and three times an even pace at the end.
     */
    fun intoIcon(fraction: Float): Float {
        val t = fraction.coerceIn(0f, 1f)
        return t * t * t
    }

    /** Where the home screen comes forward about, back from an app, as shares of its width and height. */
    fun comeBackPivot(): Pair<Float, Float> = 0.5f to HOME_FOOT

    /**
     * Where [rest], a place on a [width] by [height] home screen at rest, is drawn while the home
     * screen is zoomed [zoom] about the middle of its foot ([comeBackPivot]), coming forward with a
     * closing card. The card goes into its icon there, where the icon is on the way, and moves with
     * it as the home screen comes forward, as Void's does, so its name stays right under it.
     */
    fun fromFoot(rest: Rect, zoom: Float, width: Float, height: Float): Rect {
        if (zoom == 1f) return rest
        val (across, down) = comeBackPivot()
        val pivotX = width * across
        val pivotY = height * down
        return Rect(
            left = pivotX + (rest.left - pivotX) * zoom,
            top = pivotY + (rest.top - pivotY) * zoom,
            right = pivotX + (rest.right - pivotX) * zoom,
            bottom = pivotY + (rest.bottom - pivotY) * zoom,
        )
    }

    /**
     * Whether the home screen comes back with the card: shown whole behind it as it goes up, sunk
     * back, and coming forward about [HOME_FOOT] as it shrinks, with no arrival of its own, as
     * Void's does. That is once Android's own close has been waited out ([waited]), when a card or
     * a picture goes back into the icon. Otherwise the home screen arrives as it always has.
     */
    fun bringsHome(waited: Boolean, route: CloseRoute?): Boolean =
        waited && (route == CloseRoute.Card || route == CloseRoute.Picture)

    /**
     * Whether a card bringing the home screen back goes up the moment Zenith is back, full size,
     * and waits there for Android's close to be waited out ([plan]), rather than going up then:
     * on a phone that shows a still picture of Zenith before its own frames (One UI with gestures,
     * see AndroidClose), so that picture is the app back on the screen, as Void's is.
     */
    fun cardFirst(plan: AndroidClose.Plan?, route: CloseRoute?): Boolean =
        plan?.cardFirst == true && bringsHome(waited = true, route = route)

    private fun asBefore(target: Boolean, recent: Boolean, cardFrom: Float): CloseRoute = when {
        !target || !recent -> CloseRoute.None
        cardFrom > CARD_OFF -> CloseRoute.Card
        else -> CloseRoute.Land
    }

    /**
     * How much of the app's picture shows on a card [grown] of the way to the whole screen: none
     * near the icon, all of it near full size, easing between. The same on the way out and back.
     */
    fun pictureShare(grown: Float): Float {
        val t = ((grown - PICTURE_FROM) / (PICTURE_TO - PICTURE_FROM)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /**
     * How solid a closing card that set off at [from] is at [grown]: one smaller than the screen
     * comes in over its first stretch, so it never pops up over the home screen; one that fills it
     * is solid from its first frame, the app back as it was left, as Void's is.
     */
    fun closingAlpha(grown: Float, from: Float): Float =
        if (from >= 1f) 1f else ((from - grown) / CLOSE_FADE_IN).coerceIn(0f, 1f)

    /** How solid a card with no icon to go into is at [grown]: it fades out over its last stretch. */
    fun vanishAlpha(grown: Float): Float = (grown / VANISH).coerceIn(0f, 1f)

    /** The part of a picture that is drawn. */
    data class Crop(val left: Int, val top: Int, val width: Int, val height: Int)

    /**
     * The part of a [width] by [height] picture that fills a card [cardWidth] by [cardHeight]
     * without stretching it: all of one side and the middle of the other, as iOS fills an icon
     * with the app it is closing.
     */
    fun coverCrop(width: Int, height: Int, cardWidth: Float, cardHeight: Float): Crop {
        if (width <= 0 || height <= 0) return Crop(0, 0, 0, 0)
        if (cardWidth <= 0f || cardHeight <= 0f) return Crop(0, 0, width, height)
        val card = cardWidth / cardHeight
        val picture = width.toFloat() / height
        return if (card > picture) {
            val shown = (width / card).roundToInt().coerceIn(1, height)
            Crop(0, (height - shown) / 2, width, shown)
        } else {
            val shown = (height * card).roundToInt().coerceIn(1, width)
            Crop((width - shown) / 2, 0, shown, height)
        }
    }

    /** Where an app goes with no icon on show to go into: a [side] square in the middle of the screen, as on iOS. */
    fun middle(width: Float, height: Float, side: Float): Rect =
        Rect(width / 2f - side / 2f, height / 2f - side / 2f, width / 2f + side / 2f, height / 2f + side / 2f)

    /** The app a key opens: an app's, a work app's or a shortcut's. Empty for a key that is none of them. */
    fun packageOf(key: String): String = when {
        ShortcutKeys.isShortcut(key) -> ShortcutKeys.parse(key)?.first.orEmpty()
        else -> WorkKeys.appKey(key).substringBefore('/')
    }

    fun isWork(key: String): Boolean = WorkKeys.isWork(key)

    /** The key of the app [packageName]/[className], in the work profile or not. */
    fun keyOf(packageName: String, className: String, work: Boolean): String {
        val key = AppInfo.keyOf(packageName, className)
        return if (work) WorkKeys.of(key) else key
    }
}
