package com.gabriel.zenithui.data

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Holding something on the home screen: the times and distances, in one place, so that how a hold
 * feels can be changed without hunting for numbers.
 */
object HoldTuning {
    /** How long a finger rests on something before its menu comes. */
    const val HOLD_MS = 450L

    /** How far the finger may wander, in dp, before the held thing is picked up instead. */
    const val PICK_UP_DP = 10f

    /** Still held this long after the menu came: the thing is in your hand, ready to be moved. */
    const val EDIT_AFTER_MS = 2000L

    /** The menu: how wide it is, how tall a row is at ordinary text size, and the room round them. */
    const val MENU_WIDTH_DP = 250f
    const val ROW_DP = 44f
    const val SIZE_ROW_DP = 48f
    const val GAP_DP = 8f
    const val PAD_DP = 8f
    const val CORNER_DP = 28f

    /** How far the menu stands from the thing it belongs to, and from the screen's edges. */
    const val STAND_OFF_DP = 10f
    const val MARGIN_DP = 10f

    /** How much a held thing grows: an icon, and anything bigger than one. */
    const val LIFT_ICON = 1.15f
    const val LIFT_WIDGET = 1.04f
}

/** What a row of the menu does. Which shortcut, or which widget of a stack, is the row's index. */
enum class HoldDo {
    UNLOCK, SHORTCUT, EDIT_HOME, EDIT_NAME, RESIZE, ADD_TO_HOME, APP_INFO, REMOVE,
    RENAME, COLOUR, SET_UP, STACK, GLASS, UNSTACK,
}

/** One size a held widget can be given from its menu. */
data class SizeChoice(val name: String, val spanX: Int, val spanY: Int, val current: Boolean, val fits: Boolean)

/** One line of the menu, in reading order for a menu that stands under the thing held. */
sealed interface HoldEntry {
    data class Row(
        val what: HoldDo,
        val label: String,
        /** The red row: it takes something away. */
        val danger: Boolean = false,
        /** A shortcut's row carries a "+" at its end, to put it on the home screen. */
        val plus: Boolean = false,
        val index: Int = 0,
    ) : HoldEntry

    /** The sizes of one of Zenith's widgets, side by side at the head of its menu. */
    data class Sizes(val choices: List<SizeChoice>) : HoldEntry
}

/** What is being held, as far as its menu depends on it. */
sealed interface HoldSubject {
    /** An app, on a page or in the dock. [shortcuts] are its own shortcuts' names, in order. */
    data class App(val shortcuts: List<String> = emptyList(), val inDock: Boolean = false) : HoldSubject

    /** A shortcut pinned as an icon of its own. */
    data object Pinned : HoldSubject

    data object Folder : HoldSubject

    /**
     * One of Zenith's own widgets. [kind] is its base type; [sizes] the named sizes it can take here;
     * [mediaAllowed] whether Zenith may see what is playing, which only Now playing asks about.
     */
    data class ZenithWidget(
        val kind: String,
        val sizes: List<SizeChoice> = emptyList(),
        val mediaAllowed: Boolean = true,
    ) : HoldSubject

    /** Another app's widget, with a pane of glass behind it or not. */
    data class HostedWidget(val glass: Boolean) : HoldSubject

    /** A stack: its widgets' names, the top one first, and whether it can take another. */
    data class Stack(val members: List<String>, val full: Boolean) : HoldSubject
}

/** Which rows a held thing's menu has. Nothing here draws: the rows are words and what they do. */
object HoldMenuRules {

    /**
     * The menu for [subject], top to bottom as it reads when it stands under the thing. With the
     * layout [locked] it leads with the way to unlock, and offers nothing that moves, resizes or
     * removes. The red row, when there is one, is always the last.
     */
    fun rows(subject: HoldSubject, locked: Boolean): List<HoldEntry> = buildList {
        if (locked) add(HoldEntry.Row(HoldDo.UNLOCK, "Unlock Layout"))
        when (subject) {
            is HoldSubject.App -> {
                subject.shortcuts.forEachIndexed { index, name ->
                    add(HoldEntry.Row(HoldDo.SHORTCUT, name, plus = true, index = index))
                }
                if (!locked) add(HoldEntry.Row(HoldDo.EDIT_HOME, "Edit Home Screen"))
                add(HoldEntry.Row(HoldDo.EDIT_NAME, "Edit Name and Icon"))
                if (!locked) {
                    // In the dock an icon is always an icon; on a page it can be pulled to any size.
                    if (subject.inDock) add(HoldEntry.Row(HoldDo.ADD_TO_HOME, "Add to Home Screen"))
                    else add(HoldEntry.Row(HoldDo.RESIZE, "Resize"))
                }
                add(HoldEntry.Row(HoldDo.APP_INFO, "App Info"))
                if (!locked) add(HoldEntry.Row(HoldDo.REMOVE, "Remove App", danger = true))
            }

            HoldSubject.Pinned -> if (!locked) {
                add(HoldEntry.Row(HoldDo.EDIT_HOME, "Edit Home Screen"))
                add(HoldEntry.Row(HoldDo.REMOVE, "Remove Shortcut", danger = true))
            }

            HoldSubject.Folder -> {
                add(HoldEntry.Row(HoldDo.RENAME, "Rename"))
                add(HoldEntry.Row(HoldDo.COLOUR, "Colour"))
                if (!locked) {
                    add(HoldEntry.Row(HoldDo.EDIT_HOME, "Edit Home Screen"))
                    add(HoldEntry.Row(HoldDo.REMOVE, "Remove Folder", danger = true))
                }
            }

            is HoldSubject.ZenithWidget -> {
                if (!locked && subject.sizes.size > 1) add(HoldEntry.Sizes(subject.sizes))
                setUpLabel(subject.kind, subject.mediaAllowed)?.let { add(HoldEntry.Row(HoldDo.SET_UP, it)) }
                add(HoldEntry.Row(HoldDo.COLOUR, "Colour"))
                if (!locked) {
                    add(HoldEntry.Row(HoldDo.STACK, "Stack Another Widget"))
                    add(HoldEntry.Row(HoldDo.EDIT_HOME, "Edit Home Screen"))
                    add(HoldEntry.Row(HoldDo.REMOVE, "Remove Widget", danger = true))
                }
            }

            is HoldSubject.HostedWidget -> {
                add(HoldEntry.Row(HoldDo.GLASS, if (subject.glass) "Take the Glass Away" else "Put Glass Behind It"))
                if (!locked) {
                    add(HoldEntry.Row(HoldDo.STACK, "Stack Another Widget"))
                    add(HoldEntry.Row(HoldDo.EDIT_HOME, "Edit Home Screen"))
                    add(HoldEntry.Row(HoldDo.REMOVE, "Remove Widget", danger = true))
                }
            }

            is HoldSubject.Stack -> {
                if (!locked && !subject.full) add(HoldEntry.Row(HoldDo.STACK, "Stack Another Widget"))
                subject.members.forEachIndexed { index, name ->
                    add(HoldEntry.Row(HoldDo.UNSTACK, "Take $name Out", index = index))
                }
                if (!locked) {
                    add(HoldEntry.Row(HoldDo.EDIT_HOME, "Edit Home Screen"))
                    add(HoldEntry.Row(HoldDo.REMOVE, "Remove Stack", danger = true))
                }
            }
        }
    }

    /**
     * What a widget's set-up row says. Most open a set-up of their own, and say "Edit Widget"; the
     * few that simply do something say what. Null for a widget with nothing to set.
     */
    private fun setUpLabel(kind: String, mediaAllowed: Boolean): String? = when (kind) {
        "toggles", "world", "countdown", "weather" -> "Edit Widget"
        "photo" -> "Choose Photos"
        "todo" -> "Clear What Is Done"
        "media" -> if (mediaAllowed) null else "Let Zenith See What Is Playing"
        else -> null
    }

    /** How tall the menu is: its rows, the size row with the gap under it, and the room above and below. */
    fun height(entries: List<HoldEntry>, row: Float, sizeRow: Float, gap: Float, pad: Float): Float =
        pad * 2f + entries.sumOf {
            when (it) {
                is HoldEntry.Row -> row.toDouble()
                is HoldEntry.Sizes -> (sizeRow + gap).toDouble()
            }
        }.toFloat()
}

/** Where a menu stands: its top left corner, and whether it is above the thing rather than under it. */
data class MenuPlace(val left: Float, val top: Float, val above: Boolean)

object HoldMenuPlace {

    /**
     * Where the menu for a held thing stands, everything in pixels of one space.
     *
     * Under the thing, [gap] away, when there is room down to [roomBottom]; otherwise above it, and
     * then its rows are read from the bottom up, so the ones nearest the finger stay nearest. With
     * room on neither side it takes the roomier one and is pushed back onto the screen. A thing in
     * the left half lines the menu's left edge up with its own, one in the right half the right
     * edges; either way the menu stays [margin] inside the screen.
     */
    fun place(
        itemLeft: Float,
        itemTop: Float,
        itemRight: Float,
        itemBottom: Float,
        menuWidth: Float,
        menuHeight: Float,
        screenWidth: Float,
        roomTop: Float,
        roomBottom: Float,
        gap: Float,
        margin: Float,
    ): MenuPlace {
        var above = itemBottom + gap + menuHeight > roomBottom
        if (above && itemTop - gap - menuHeight < roomTop) above = (itemTop - roomTop) > (roomBottom - itemBottom)
        val top = (if (above) itemTop - gap - menuHeight else itemBottom + gap)
            .coerceIn(roomTop, max(roomTop, roomBottom - menuHeight))
        // A menu wider than the screen has room for no margin at all.
        val least = min(margin, max(0f, (screenWidth - menuWidth) / 2f))
        val left = (if ((itemLeft + itemRight) / 2f < screenWidth / 2f) itemLeft else itemRight - menuWidth)
            .coerceIn(least, max(least, screenWidth - least - menuWidth))
        return MenuPlace(left, top, above)
    }
}

/** What a finger that is still down after the hold means by moving. */
object HoldGesture {

    /** Heading for the menu: down towards one under the thing, up towards one above, within a cone. */
    fun towardsMenu(dx: Float, dy: Float, above: Boolean): Boolean =
        if (above) dy < 0f && abs(dx) < -dy * 1.2f else dy > 0f && abs(dx) < dy * 1.2f

    /**
     * The held thing is picked up: the finger has left its place by more than [slop], and not on its
     * way to the menu. Never once the finger has reached the menu ([onMenu]), and never with the
     * layout [locked]. [dx] and [dy] are from where the hold began.
     */
    fun picksUp(dx: Float, dy: Float, slop: Float, above: Boolean, onMenu: Boolean, locked: Boolean): Boolean =
        !locked && !onMenu && hypot(dx, dy) > slop && !towardsMenu(dx, dy, above)
}

/** The spring the liquid things move on, worked out rather than stepped, so a frame can be asked for. */
object LiquidSpring {

    /**
     * A damped spring's answer to being let go, from 0 towards 1: [t] seconds in, [w] its stiffness
     * and [z] its damping. Under 1 it passes its mark once or more before it settles.
     */
    fun step(t: Float, w: Float, z: Float): Float {
        if (t <= 0f) return 0f
        if (z >= 1f) return 1f - exp(-w * t) * (1f + w * t)
        val wd = w * sqrt(1f - z * z)
        return 1f - exp(-z * w * t) * (cos(wd * t) + (z * w / wd) * sin(wd * t))
    }
}

/** What the pour is between: the held icon and the menu it becomes, in pixels of one space. */
data class PourGeometry(
    val above: Boolean,
    val iconCentreX: Float,
    val iconCentreY: Float,
    val iconHeight: Float,
    /** How much the held thing has grown. */
    val lift: Float,
    val menuLeft: Float,
    val menuTop: Float,
    val menuWidth: Float,
    val menuHeight: Float,
    /** Pixels to a dp: the neck and the drop are sized in dp. */
    val unit: Float,
)

/** The neck of glass between the icon and the drop: from one point to another, pinched in the middle. */
data class PourNeck(val x0: Float, val y0: Float, val x1: Float, val y1: Float, val w0: Float, val wm: Float, val w1: Float)

/** One frame of the menu as a drop of glass: where it is, how round, how solid, and its neck if it has one. */
data class PourFrame(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
    val radius: Float,
    val alpha: Float = 1f,
    val neck: PourNeck? = null,
)

/**
 * The menu poured out of the icon: a drop of glass on a thin neck that swells into the menu, the
 * neck thinning until it snaps, the menu settling a touch wide and then a touch tall. And drawn back
 * in again. The numbers are the mockup's (its `pourMenuIn` and `pourMenuOut`).
 */
object Pour {
    /** How long pouring out takes, and when in it the neck snaps. */
    const val MS = 300
    const val SNAP_S = 0.15f

    /** Drawn back in; sooner when the icon is picked up from under it. */
    const val OUT_MS = 230
    const val OUT_PICKED_MS = 150

    private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

    /** Down for a menu under the icon, up for one above it. */
    private fun dir(g: PourGeometry) = if (g.above) -1f else 1f

    /** Where on the icon's edge the drop hangs: under its middle, but never off the menu's shoulder. */
    private fun anchorX(g: PourGeometry) = g.iconCentreX.coerceIn(
        g.menuLeft + min(36f * g.unit, g.menuWidth / 2f),
        g.menuLeft + g.menuWidth - min(36f * g.unit, g.menuWidth / 2f),
    )

    /** The lifted icon's edge that faces the menu, and the menu's edge that faces the icon. */
    private fun anchorY(g: PourGeometry) = g.iconCentreY + dir(g) * g.iconHeight * g.lift / 2f
    private fun nearEdge(g: PourGeometry) = if (g.above) g.menuTop + g.menuHeight else g.menuTop

    /** The frame [seconds] after the pour began. */
    fun frameIn(seconds: Float, g: PourGeometry): PourFrame {
        val u = g.unit
        val d = dir(g)
        val ax = anchorX(g)
        val ay = anchorY(g)
        val ny = nearEdge(g)
        val drop = 22f * u
        // The width leads and the height follows: a touch wide, then a touch tall.
        val fx = LiquidSpring.step(seconds - 0.01f, 27f, 0.72f)
        val fy = LiquidSpring.step(seconds - 0.02f, 25f, 0.7f)
        // The drop pulls away from the icon, stretching the neck.
        val near = lerp(ay - d * 3f * u, ny, LiquidSpring.step(seconds - 0.015f, 36f, 0.86f))
        val far = lerp(ay + d * (drop - 3f * u), ny + d * g.menuHeight, fy)
        val width = max(8f * u, lerp(drop, g.menuWidth, fx))
        val height = max(8f * u, abs(far - near))
        val left = lerp(ax, g.menuLeft + g.menuWidth / 2f, LiquidSpring.step(seconds, 24f, 0.82f)) - width / 2f
        val top = min(near, far)
        val radius = min(HoldTuning.CORNER_DP * u, min(width, height) / 2f)
        val neck = if (seconds < SNAP_S) {
            // Fat at the icon, pinching thinner until it snaps.
            val through = seconds / SNAP_S
            val pinch = (1f - through).pow(0.9f)
            val lo = left + radius * 0.8f
            val hi = left + width - radius * 0.8f
            PourNeck(
                x0 = ax, y0 = ay - d * 6f * u,
                x1 = if (lo <= hi) ax.coerceIn(lo, hi) else left + width / 2f, y1 = near + d * 5f * u,
                w0 = (26f - 8f * through) * u, wm = (2.5f + 13f * pinch) * u, w1 = min(width * 0.75f, 40f * u),
            )
        } else {
            // The stub left on the icon draws back in.
            val k = ((seconds - SNAP_S) / 0.07f).coerceIn(0f, 1f)
            if (k < 1f) {
                PourNeck(ax, ay - d * 6f * u, ax, ay + d * 7f * u * (1f - k), (20f - 8f * k) * u, (8f * (1f - k) + 0.5f) * u, 2f * u)
            } else {
                null
            }
        }
        return PourFrame(left, top, width, height, radius, 1f, neck)
    }

    /** The frame [fraction] of the way through being drawn back in: gathering speed, with no overshoot. */
    fun frameOut(fraction: Float, g: PourGeometry): PourFrame {
        val u = g.unit
        val d = dir(g)
        val ax = anchorX(g)
        val ay = anchorY(g)
        val ny = nearEdge(g)
        val p = fraction.coerceIn(0f, 1f).pow(1.6f)
        val near = lerp(ny, ay - d * 3f * u, p)
        val far = lerp(ny + d * g.menuHeight, ay + d * 16f * u, p)
        val width = lerp(g.menuWidth, 18f * u, p)
        val height = max(6f * u, abs(far - near))
        val left = lerp(g.menuLeft + g.menuWidth / 2f, ax, p) - width / 2f
        val top = min(near, far)
        val radius = min(HoldTuning.CORNER_DP * u, min(width, height) / 2f)
        val neck = if (p > 0.4f) {
            val lo = left + radius
            val hi = left + width - radius
            PourNeck(
                x0 = ax, y0 = ay - d * 6f * u,
                x1 = if (lo <= hi) ax.coerceIn(lo, hi) else left + width / 2f, y1 = near + d * 3f * u,
                w0 = 22f * u, wm = (3f + 11f * (p - 0.4f) / 0.6f) * u, w1 = min(width * 0.7f, 26f * u),
            )
        } else {
            null
        }
        return PourFrame(left, top, width, height, radius, if (p > 0.85f) (1f - p) / 0.15f else 1f, neck)
    }
}
