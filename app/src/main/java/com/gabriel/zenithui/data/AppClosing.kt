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
    /** A card in the icon's colour shrinks into it: the Closing card of Advanced animation settings. */
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

    /** Only the landing, and the colour card if it is turned up: how it was before. */
    const val PLAIN = "plain"

    /** How long after Android's question its answer still decides how the app closes. */
    const val HANDSHAKE_FRESH_MS = 2_000L

    /** How long Android has to say the window has reached the icon before Zenith carries on without it. */
    const val HANDSHAKE_WAIT_MS = 900L

    /** Below this the Closing card is off. */
    const val CARD_OFF = 0.005f

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
     * [cardFrom]: the Closing card's size to start from, nothing below [CARD_OFF].
     */
    fun route(style: String, asked: Boolean, target: Boolean, picture: Boolean, recent: Boolean, cardFrom: Float): CloseRoute = when {
        style == PLAIN -> asBefore(target, recent, cardFrom)
        // Answered, Android has already moved the window as far as it can: anything more would
        // only play the close a second time.
        asked -> if (target) CloseRoute.Handshake else CloseRoute.None
        picture -> CloseRoute.Picture
        else -> asBefore(target, recent, cardFrom)
    }

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

    /** How solid a closing card that set off at [from] is at [grown]: it comes in over its first stretch. */
    fun closingAlpha(grown: Float, from: Float): Float = ((from - grown) / CLOSE_FADE_IN).coerceIn(0f, 1f)

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
