package com.gabriel.zenithui.data

import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * How an app's logo is lifted off its own tile, as an iPhone's are in iOS 26: the logo stands a
 * little off the tile on a soft shadow falling straight down, its own face is lit from above, a
 * little lighter at its top and a little shaded at its foot, as a thing of its own and not part of
 * the tile, and a hairline of light lies on its edges facing the light, up and to the left as
 * Zenith's glass is lit, with a fainter one on the edges opposite. The logo is drawn as its app drew
 * it: the shadow goes under it, and the light and shade over it only.
 *
 * Only a logo that stands on its own can be lifted: an adaptive icon's foreground when it leaves
 * most of the tile to the background, or the artwork Zenith puts on a tile of its own for an old
 * icon. A foreground that covers the tile is the whole icon, with nothing under it to stand off.
 *
 * Rules only, on plain arrays of how much of each pixel the logo covers, from 0 to 1, row by row,
 * so they can be checked without a phone.
 */
object LogoLift {

    /** The lift, in pixels, for an icon of one size. */
    data class Look(
        /** How soft the shadow is, as the spread of its blur, and how far down it falls. */
        val blurPx: Float,
        val dropPx: Float,
        /** How dark the shadow is at its darkest, as black. */
        val shadow: Float,
        /** How wide the hairline of light on the logo's edge is. */
        val rimPx: Float,
        /** How bright it is, as white, on the edges facing the light and on those opposite. */
        val rim: Float,
        val backRim: Float,
        /** The light on the top of the logo's face, as white, and the shade at its foot, as black. */
        val faceLight: Float,
        val faceShade: Float,
    )

    /** The lift for an icon [sizePx] across: the same proportions at every size. */
    fun look(sizePx: Int): Look {
        val side = sizePx.coerceAtLeast(1).toFloat()
        return Look(
            blurPx = side * BLUR_SHARE,
            dropPx = side * DROP_SHARE,
            shadow = SHADOW,
            rimPx = (side * RIM_SHARE).coerceAtLeast(1f),
            rim = RIM,
            backRim = BACK_RIM,
            faceLight = FACE_LIGHT,
            faceShade = FACE_SHADE,
        )
    }

    /** Whether a logo covering [coverage] of its tile ([LogoLift.coverage]) stands on its own, to be lifted. */
    fun lifts(coverage: Float): Boolean = coverage >= MIN_COVERAGE && coverage <= MAX_COVERAGE

    /** How much of the tile a logo covers, from 0 to 1. */
    fun coverage(alpha: FloatArray): Float {
        if (alpha.isEmpty()) return 0f
        var seen = 0
        for (a in alpha) if (a > SEEN) seen++
        return seen / alpha.size.toFloat()
    }

    /**
     * How much of a square [side] pixels across, from [left], [top] in a picture [width] wide, the
     * logo covers: the logo in the middle of a super icon, measured as it is on an ordinary one.
     */
    fun coverage(alpha: FloatArray, width: Int, left: Int, top: Int, side: Int): Float {
        if (side <= 0 || width <= 0) return 0f
        val height = alpha.size / width
        var seen = 0
        for (y in top.coerceAtLeast(0) until (top + side).coerceAtMost(height)) {
            for (x in left.coerceAtLeast(0) until (left + side).coerceAtMost(width)) {
                if (alpha[y * width + x] > SEEN) seen++
            }
        }
        return seen / (side.toFloat() * side)
    }

    /**
     * The shadow a logo covering [alpha] of a picture [width] by [height] casts on what is under it,
     * as black, from 0 to [Look.shadow]: the logo blurred by [Look.blurPx] and moved [Look.dropPx]
     * straight down. Nothing past the picture's edge casts any.
     */
    fun shadow(alpha: FloatArray, width: Int, height: Int, look: Look): FloatArray {
        val radius = boxRadius(look.blurPx)
        val soft = alpha.copyOf()
        val scratch = FloatArray(alpha.size)
        // Three box blurs one after another come out as near a true blur as the eye can tell.
        repeat(3) {
            boxRows(soft, scratch, width, height, radius)
            boxColumns(scratch, soft, width, height, radius)
        }
        val drop = look.dropPx.roundToInt()
        val shadow = FloatArray(alpha.size)
        for (y in 0 until height) {
            val from = y - drop
            if (from < 0 || from >= height) continue
            for (x in 0 until width) {
                shadow[y * width + x] = (soft[from * width + x] * look.shadow).coerceIn(0f, look.shadow)
            }
        }
        return shadow
    }

    /**
     * The light on the edge of a logo covering [alpha] of a picture [width] by [height], as white,
     * from 0 to 1: on the edges facing the light, [Look.rimPx] wide, and fainter on those opposite.
     * It is only ever on the logo, and only where the logo is solid: a faint wash in a foreground,
     * such as a shadow its app drew into it, catches none.
     */
    fun light(alpha: FloatArray, width: Int, height: Int, look: Look): FloatArray {
        val light = FloatArray(alpha.size)
        val dx = TOWARDS_LIGHT_X * look.rimPx
        val dy = TOWARDS_LIGHT_Y * look.rimPx
        for (y in 0 until height) {
            for (x in 0 until width) {
                val i = y * width + x
                val solid = smooth(SOLID_FROM, SOLID_TO, alpha[i])
                if (solid <= 0f) continue
                // An edge faces the light where the logo stops a hairline towards it.
                val towards = sample(alpha, width, height, x + dx, y + dy)
                val away = sample(alpha, width, height, x - dx, y - dy)
                val lit = look.rim * (1f - towards) + look.backRim * (1f - away)
                light[i] = (solid * lit).coerceIn(0f, 1f)
            }
        }
        return light
    }

    /**
     * The light and shade on the face of a logo covering [alpha] of a picture [width] by [height],
     * from the top of the logo to its foot, whatever it stands on: white at its top, as much as
     * [Look.faceLight], gone before its middle, and black coming in after it, as much as
     * [Look.faceShade] at its foot, given here below nothing. Only ever on the logo.
     */
    fun face(alpha: FloatArray, width: Int, height: Int, look: Look): FloatArray {
        val face = FloatArray(alpha.size)
        var top = -1
        var bottom = -1
        for (y in 0 until height) {
            for (x in 0 until width) {
                if (alpha[y * width + x] > SEEN) {
                    if (top < 0) top = y
                    bottom = y
                    break
                }
            }
        }
        if (top < 0 || bottom <= top) return face
        val span = (bottom - top).toFloat()
        for (y in top..bottom) {
            val t = (y - top) / span
            val tone = when {
                t < FACE_PLAIN_FROM -> look.faceLight * (1f - t / FACE_PLAIN_FROM)
                t > FACE_PLAIN_TO -> -look.faceShade * ((t - FACE_PLAIN_TO) / (1f - FACE_PLAIN_TO))
                else -> 0f
            }
            if (tone == 0f) continue
            for (x in 0 until width) {
                val i = y * width + x
                face[i] = tone * alpha[i]
            }
        }
        return face
    }

    /** The box that, three times over, blurs as far as a true blur of spread [spread]. */
    private fun boxRadius(spread: Float): Int =
        ((sqrt(1f + 4f * spread * spread) - 1f) / 2f).roundToInt().coerceAtLeast(1)

    private fun boxRows(from: FloatArray, to: FloatArray, width: Int, height: Int, radius: Int) {
        val span = 2f * radius + 1f
        for (y in 0 until height) {
            val row = y * width
            var sum = 0f
            for (x in 0..minOf(radius, width - 1)) sum += from[row + x]
            for (x in 0 until width) {
                to[row + x] = (sum / span).coerceAtLeast(0f)
                val leaving = x - radius
                val coming = x + radius + 1
                if (leaving >= 0) sum -= from[row + leaving]
                if (coming < width) sum += from[row + coming]
            }
        }
    }

    private fun boxColumns(from: FloatArray, to: FloatArray, width: Int, height: Int, radius: Int) {
        val span = 2f * radius + 1f
        for (x in 0 until width) {
            var sum = 0f
            for (y in 0..minOf(radius, height - 1)) sum += from[y * width + x]
            for (y in 0 until height) {
                to[y * width + x] = (sum / span).coerceAtLeast(0f)
                val leaving = y - radius
                val coming = y + radius + 1
                if (leaving >= 0) sum -= from[leaving * width + x]
                if (coming < height) sum += from[coming * width + x]
            }
        }
    }

    /** How much of the point [x], [y] the logo covers, between pixels; nothing past the edge. */
    private fun sample(alpha: FloatArray, width: Int, height: Int, x: Float, y: Float): Float {
        val left = floor(x).toInt()
        val top = floor(y).toInt()
        val across = x - left
        val down = y - top
        fun at(px: Int, py: Int) = if (px < 0 || py < 0 || px >= width || py >= height) 0f else alpha[py * width + px]
        val upper = at(left, top) + (at(left + 1, top) - at(left, top)) * across
        val lower = at(left, top + 1) + (at(left + 1, top + 1) - at(left, top + 1)) * across
        return upper + (lower - upper) * down
    }

    private fun smooth(from: Float, to: Float, x: Float): Float {
        val t = ((x - from) / (to - from)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** How soft the shadow is and how far it falls, as shares of the icon's side. */
    private const val BLUR_SHARE = 0.032f
    private const val DROP_SHARE = 0.028f

    /** How dark the shadow is at its darkest: enough to stand the logo off a pale tile, never a black blot. */
    private const val SHADOW = 0.34f

    /** The hairline of light: about a dp on a home screen's icon. */
    private const val RIM_SHARE = 0.014f

    /** How bright it is facing the light, and opposite, as the rim of Zenith's glass is. */
    private const val RIM = 0.5f
    private const val BACK_RIM = 0.18f

    /**
     * The light on the top of the logo's face and the shade at its foot: a white logo goes a pale
     * grey at its foot, as an iPhone's do, and the edge light opposite the lamp shows on it. The
     * light is the fainter, so a coloured logo's top keeps its colour rather than going milky.
     */
    private const val FACE_LIGHT = 0.07f
    private const val FACE_SHADE = 0.12f

    /** Between these shares of the logo's height, from its top, its face is as its app drew it. */
    private const val FACE_PLAIN_FROM = 0.45f
    private const val FACE_PLAIN_TO = 0.55f

    /** A logo covers at least this much of its tile, and no more than this: past it, it is the tile. */
    private const val MIN_COVERAGE = 0.004f
    private const val MAX_COVERAGE = 0.62f

    /** What counts as covered, as [IconLoader] counts it. */
    private const val SEEN = 24f / 255f

    /** From how solid to how solid a pixel of the logo goes from catching no light to catching it all. */
    private const val SOLID_FROM = 0.35f
    private const val SOLID_TO = 0.85f

    /** One step towards the light, which comes from up and to the left as Zenith's glass is lit. */
    private val TOWARDS_LIGHT_X = cos(Math.toRadians(GlassTuning.ZENITH_LIGHT.toDouble())).toFloat()
    private val TOWARDS_LIGHT_Y = sin(Math.toRadians(GlassTuning.ZENITH_LIGHT.toDouble())).toFloat()
}
