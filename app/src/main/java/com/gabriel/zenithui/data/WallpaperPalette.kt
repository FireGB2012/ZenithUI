package com.gabriel.zenithui.data

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * A theme taken from a wallpaper, for a photo nobody designed for Zenith: the liveliest colour
 * there is a fair amount of, for the glow; a second, a different hue, for gradients; the colour
 * there is most of, for the backdrop; and whether the picture is dark or light.
 *
 * Plain arithmetic on ARGB numbers, so it is tested without a phone.
 */
object WallpaperPalette {

    /** What a wallpaper gives a palette. Colours are ARGB. */
    data class Picked(val accent: Int, val accentAlt: Int, val backdrop: Int, val dark: Boolean)

    /** A hue with no colour to speak of: greys, near black and near white. */
    private const val GREY_SATURATION = 0.16f

    /** Under this [glowScore] a family of hue is too dull, too dark or pale, or too little to glow. */
    private const val LIVELY = 0.02f

    /** Under this share of the picture, a colour is a speck, however bright. */
    private const val SPECK = 0.004f

    /** The second colour is at least this far round the colour wheel from the first. */
    private const val APART = 40f

    /**
     * The palette in [pixels] (ARGB, any order, any count). Pixels less than half opaque are
     * not counted. Null when there is nothing to count.
     */
    fun fromPixels(pixels: IntArray): Picked? {
        // Into 4096 boxes, four bits a channel, each keeping how many fell in and their sum.
        val count = IntArray(4096)
        val red = LongArray(4096)
        val green = LongArray(4096)
        val blue = LongArray(4096)
        var total = 0
        for (pixel in pixels) {
            if ((pixel ushr 24) < 128) continue
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val box = ((r shr 4) shl 8) or ((g shr 4) shl 4) or (b shr 4)
            count[box]++
            red[box] += r.toLong()
            green[box] += g.toLong()
            blue[box] += b.toLong()
            total++
        }
        if (total == 0) return null
        val weighted = (0 until 4096).filter { count[it] > 0 }.map { box ->
            val n = count[box]
            argb((red[box] / n).toInt(), (green[box] / n).toInt(), (blue[box] / n).toInt()) to n.toFloat() / total
        }
        return pick(weighted)
    }

    /**
     * The palette in [weighted]: colours, each with how much of the picture it is. The weights
     * need not add up to one. Android's own reading of a wallpaper — its main colour and the two
     * after it — goes in here as three colours with falling weights.
     */
    fun pick(weighted: List<Pair<Int, Float>>): Picked? {
        val sum = weighted.sumOf { it.second.toDouble() }.toFloat()
        if (sum <= 0f) return null
        val colours = weighted.filter { it.second > 0f }.map { (colour, weight) -> Hsl.of(colour) to weight / sum }

        // Light or dark: the picture's lightness, each colour counted by its share.
        val lightness = colours.sumOf { (hsl, weight) -> (hsl.l * weight).toDouble() }.toFloat()
        val dark = lightness < 0.5f

        // Twelve families of hue, thirty degrees each, and the greys apart.
        val families = colours.filter { (hsl, _) -> hsl.s >= GREY_SATURATION && hsl.l in 0.08f..0.94f }
            .groupBy { (hsl, _) -> ((hsl.h / 30f).toInt()).coerceIn(0, 11) }
            .map { (_, members) -> Family.of(members) }

        val ranked = families.filter { it.weight >= SPECK }.sortedByDescending(::glowScore)
        val accent = ranked.firstOrNull { glowScore(it) >= LIVELY && it.colour.s >= 0.22f }?.colour
        val alt = accent?.let { first -> ranked.map { it.colour }.firstOrNull { hueApart(it.h, first.h) >= APART } }

        // The backdrop is the colour there is most of, greys and all, kept deep or pale.
        val most = colours.maxByOrNull { it.second }?.first ?: return null
        val backdrop = if (dark) {
            Hsl(most.h, most.s.coerceAtMost(0.45f), most.l.coerceIn(0.05f, 0.12f))
        } else {
            Hsl(most.h, most.s.coerceAtMost(0.35f), most.l.coerceIn(0.9f, 0.95f))
        }

        // With nothing lively in the picture, a cool violet that sits with any grey.
        val glow = accent?.let(::glowing) ?: Hsl(247f, 0.9f, 0.65f)
        val second = alt?.let(::glowing) ?: Hsl((glow.h + 48f) % 360f, glow.s, glow.l)
        return Picked(glow.argb(), second.argb(), backdrop.argb(), dark)
    }

    /**
     * How well a family of hue would glow: lively, neither dark nor pale, and enough of it to be
     * the picture's own. Past half the picture, more does not count for more.
     */
    private fun glowScore(family: Family): Float =
        sqrt(family.weight.coerceAtMost(0.5f)) * family.colour.s * (1f - abs(family.colour.l - 0.55f))

    /** A colour as an accent: strong enough to glow as a fill, never so dark or pale it cannot be read. */
    private fun glowing(colour: Hsl): Hsl = Hsl(colour.h, colour.s.coerceAtLeast(0.55f), colour.l.coerceIn(0.52f, 0.68f))

    private fun hueApart(a: Float, b: Float): Float {
        val d = abs(a - b) % 360f
        return if (d > 180f) 360f - d else d
    }

    private fun argb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    /** One family of hue: its share of the picture, and its colour, each member counted by its share. */
    private class Family(val colour: Hsl, val weight: Float) {
        companion object {
            fun of(members: List<Pair<Hsl, Float>>): Family {
                val weight = members.sumOf { it.second.toDouble() }.toFloat()
                // Hue is an angle: averaged as a direction, so 350 and 10 come out at 0, not 180.
                var x = 0.0
                var y = 0.0
                var s = 0.0
                var l = 0.0
                for ((hsl, w) in members) {
                    val angle = Math.toRadians(hsl.h.toDouble())
                    x += cos(angle) * w
                    y += sin(angle) * w
                    s += hsl.s * w
                    l += hsl.l * w
                }
                val hue = ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
                return Family(Hsl(hue, (s / weight).toFloat(), (l / weight).toFloat()), weight)
            }
        }
    }

    /** Hue in degrees, saturation and lightness from 0 to 1. */
    internal data class Hsl(val h: Float, val s: Float, val l: Float) {
        fun argb(): Int {
            val c = (1f - abs(2f * l - 1f)) * s
            val hp = (h % 360f + 360f) % 360f / 60f
            val x = c * (1f - abs(hp % 2f - 1f))
            val (r1, g1, b1) = when {
                hp < 1f -> Triple(c, x, 0f)
                hp < 2f -> Triple(x, c, 0f)
                hp < 3f -> Triple(0f, c, x)
                hp < 4f -> Triple(0f, x, c)
                hp < 5f -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }
            val m = l - c / 2f
            fun channel(v: Float) = ((v + m) * 255f).roundToInt().coerceIn(0, 255)
            return argb(channel(r1), channel(g1), channel(b1))
        }

        companion object {
            fun of(argb: Int): Hsl {
                val r = ((argb shr 16) and 0xFF) / 255f
                val g = ((argb shr 8) and 0xFF) / 255f
                val b = (argb and 0xFF) / 255f
                val max = maxOf(r, g, b)
                val min = minOf(r, g, b)
                val l = (max + min) / 2f
                if (max == min) return Hsl(0f, 0f, l)
                val d = max - min
                val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
                val h = when (max) {
                    r -> ((g - b) / d + (if (g < b) 6f else 0f))
                    g -> ((b - r) / d + 2f)
                    else -> ((r - g) / d + 4f)
                } * 60f
                return Hsl(h, s, l)
            }
        }
    }
}
