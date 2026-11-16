package com.gabriel.zenithui.data

import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import java.util.stream.IntStream
import kotlin.math.atan2
import kotlin.math.cbrt
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Colour as the eye sees it, for the Depth designs: OKLab, where equal steps look equal, and its
 * polar form, lightness, chroma and hue. A gradient run through it from one colour to another goes
 * round the colour wheel by the short way and stays as vivid as its ends, where the same gradient
 * mixed as red, green and blue goes grey in the middle. Light itself is added and scaled in linear
 * RGB, as it is in the world; only what is shown is turned back into the screen's sRGB.
 *
 * Plain arithmetic on numbers, so it is tested without a phone.
 */
internal object Oklab {

    /** An sRGB channel, 0 to 255, as linear light, 0 to 1. */
    private val linear = FloatArray(256) { decode(it / 255f) }

    /** Linear light back to sRGB, 0 to 255 and not yet rounded, finely enough for the darkest shadows. */
    private const val STEPS = 8192
    private val encoded = FloatArray(STEPS + 1) { encode(it / STEPS.toFloat()) * 255f }

    private fun decode(c: Float): Float = if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).pow(2.4f)

    private fun encode(c: Float): Float = if (c <= 0.0031308f) 12.92f * c else 1.055f * c.pow(1f / 2.4f) - 0.055f

    /** One sRGB channel (0 to 255) as linear light. */
    fun toLinear(channel: Int): Float = linear[channel and 0xFF]

    /** A colour as linear light, red, green and blue. */
    fun linearOf(argb: Int): FloatArray =
        floatArrayOf(toLinear(argb shr 16), toLinear(argb shr 8), toLinear(argb))

    /** Linear light as an sRGB channel, 0 to 255, plus [dither] (from -0.5 to 0.5) before it is rounded. */
    fun toChannel(value: Float, dither: Float = 0f): Int {
        val v = if (value <= 0f) 0f else if (value >= 1f) 1f else value
        return (encoded[(v * STEPS + 0.5f).toInt()] + 0.5f + dither).toInt().coerceIn(0, 255)
    }

    /** Linear light, red, green and blue, as OKLab: lightness, then a and b. */
    fun lab(r: Float, g: Float, b: Float): FloatArray {
        val l = cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b)
        val m = cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b)
        val s = cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b)
        return floatArrayOf(
            0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s,
            1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s,
            0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s,
        )
    }

    /** OKLab as linear light, into [out] from [at]; it may fall outside 0 to 1 when the colour is beyond the screen's. */
    fun linear(lightness: Float, a: Float, b: Float, out: FloatArray, at: Int = 0) {
        val l = lightness + 0.3963377774f * a + 0.2158037573f * b
        val m = lightness - 0.1055613458f * a - 0.0638541728f * b
        val s = lightness - 0.0894841775f * a - 1.2914855480f * b
        val l3 = l * l * l
        val m3 = m * m * m
        val s3 = s * s * s
        out[at] = 4.0767416621f * l3 - 3.3077115913f * m3 + 0.2309699292f * s3
        out[at + 1] = -1.2684380046f * l3 + 2.6097574011f * m3 - 0.3413193965f * s3
        out[at + 2] = -0.0041960863f * l3 - 0.7034186147f * m3 + 1.7076147010f * s3
    }

    /**
     * Lightness, chroma and hue as linear light the screen can show: where the colour is more vivid
     * than any the screen has at that lightness and hue, its chroma is drawn in until it fits,
     * rather than any one channel being cut off, which would turn the hue.
     */
    fun fromLch(lightness: Float, chroma: Float, hue: Float, out: FloatArray, at: Int = 0) {
        val l = lightness.coerceIn(0f, 1f)
        val ca = cos(hue)
        val sa = sin(hue)
        linear(l, chroma * ca, chroma * sa, out, at)
        if (fits(out, at)) return
        var low = 0f
        var high = chroma
        repeat(16) {
            val mid = (low + high) / 2f
            linear(l, mid * ca, mid * sa, out, at)
            if (fits(out, at)) low = mid else high = mid
        }
        linear(l, low * ca, low * sa, out, at)
        for (i in at until at + 3) out[i] = out[i].coerceIn(0f, 1f)
    }

    private fun fits(c: FloatArray, at: Int): Boolean {
        val e = 1e-4f
        return c[at] >= -e && c[at] <= 1 + e && c[at + 1] >= -e && c[at + 1] <= 1 + e && c[at + 2] >= -e && c[at + 2] <= 1 + e
    }
}

/**
 * The run of colour between a colour set's two accents, round the colour wheel by the short way,
 * and carried a quarter of the way on past each end, so two accents close together still give a
 * spectrum with some range. A grey end takes the other end's hue; two greys stay grey.
 */
internal class Spectrum(accent: Int, accentAlt: Int) {

    private val fromL: Float
    private val toL: Float
    private val fromC: Float
    private val toC: Float
    private val fromH: Float
    private val turn: Float

    init {
        val a = lch(accent)
        val b = lch(accentAlt)
        fromL = a[0]; toL = b[0]
        fromC = a[1]; toC = b[1]
        var ha = a[2]
        var dh = wrap(b[2] - a[2])
        if (a[1] < GREY && b[1] >= GREY) { ha = b[2]; dh = 0f }
        if (b[1] < GREY) dh = 0f
        fromH = ha
        turn = dh
    }

    /** Lightness, chroma and hue at [t], 0 to 1 along the spectrum. */
    fun at(t: Float, out: FloatArray) {
        val u = -REACH + t.coerceIn(0f, 1f) * (1f + 2f * REACH)
        val k = u.coerceIn(0f, 1f)
        out[0] = fromL + (toL - fromL) * k
        out[1] = fromC + (toC - fromC) * k
        out[2] = fromH + turn * u
    }

    /**
     * The spectrum as [SIZE] colours of linear light, at the accents' own lightness times
     * [lightness] (or at the OKLab lightness [atLightness], when given) and their chroma times
     * [chroma].
     */
    fun ramp(lightness: Float = 1f, chroma: Float = 1f, atLightness: Float = Float.NaN): Ramp {
        val values = FloatArray(SIZE * 3)
        val lch = FloatArray(3)
        for (i in 0 until SIZE) {
            at(i / (SIZE - 1f), lch)
            val l = if (atLightness.isNaN()) lch[0] * lightness else atLightness
            Oklab.fromLch(l, lch[1] * chroma, lch[2], values, i * 3)
        }
        return Ramp(values)
    }

    companion object {
        const val SIZE = 256

        /** How far past each accent the spectrum carries on, as a share of the way between them. */
        const val REACH = 0.25f

        /** Under this chroma a colour is a grey, and its hue means nothing. */
        private const val GREY = 0.03f

        fun lch(argb: Int): FloatArray {
            val lab = Oklab.lab(Oklab.toLinear(argb shr 16), Oklab.toLinear(argb shr 8), Oklab.toLinear(argb))
            return floatArrayOf(lab[0], hypot(lab[1], lab[2]), atan2(lab[2], lab[1]))
        }

        /** An angle in radians brought into -π to π. */
        fun wrap(angle: Float): Float {
            val tau = (2.0 * Math.PI).toFloat()
            var a = angle % tau
            if (a > Math.PI) a -= tau
            if (a < -Math.PI) a += tau
            return a
        }
    }
}

/** Colours of linear light along a spectrum, read at any point between 0 and 1. */
internal class Ramp(private val values: FloatArray) {

    private val last = values.size / 3 - 1

    /** The colour at [t] into [out] from [at], blended from the two nearest. */
    fun get(t: Float, out: FloatArray, at: Int = 0) {
        val p = (if (t <= 0f) 0f else if (t >= 1f) 1f else t) * last
        val i = p.toInt().coerceAtMost(last - 1)
        val f = p - i
        val j = i * 3
        out[at] = values[j] + (values[j + 3] - values[j]) * f
        out[at + 1] = values[j + 1] + (values[j + 4] - values[j + 1]) * f
        out[at + 2] = values[j + 2] + (values[j + 5] - values[j + 2]) * f
    }

    fun red(t: Float): Float = channel(t, 0)
    fun green(t: Float): Float = channel(t, 1)
    fun blue(t: Float): Float = channel(t, 2)

    private fun channel(t: Float, c: Int): Float {
        val p = (if (t <= 0f) 0f else if (t >= 1f) 1f else t) * last
        val i = p.toInt().coerceAtMost(last - 1)
        val f = p - i
        val j = i * 3 + c
        return values[j] + (values[j + 3] - values[j]) * f
    }
}

/** Small sums the Depth designs share. */
internal object Shade {

    fun smoothstep(edge0: Float, edge1: Float, x: Float): Float {
        val t = ((x - edge0) / (edge1 - edge0)).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    fun clamp01(x: Float): Float = if (x <= 0f) 0f else if (x >= 1f) 1f else x

    /** x to the power 64, by squaring: a tight highlight, cheaply. */
    fun pow64(x: Float): Float {
        var y = clamp01(x)
        repeat(6) { y *= y }
        return y
    }

    /** x to the power 16. */
    fun pow16(x: Float): Float {
        var y = clamp01(x)
        repeat(4) { y *= y }
        return y
    }

    /** x to the power 8. */
    fun pow8(x: Float): Float {
        var y = clamp01(x)
        repeat(3) { y *= y }
        return y
    }

    /** A unit vector, from three numbers. */
    fun unit(x: Float, y: Float, z: Float): FloatArray {
        val l = sqrt(x * x + y * y + z * z)
        return floatArrayOf(x / l, y / l, z / l)
    }

    /** The halfway vector between a light and the eye, which looks straight into the screen. */
    fun half(light: FloatArray): FloatArray = unit(light[0], light[1], light[2] + 1f)

    /** A 4×4 ordered dither, -0.5 to 0.5, so smooth gradients do not come out in steps. */
    private val bayer = floatArrayOf(0f, 8f, 2f, 10f, 12f, 4f, 14f, 6f, 3f, 11f, 1f, 9f, 15f, 7f, 13f, 5f)
        .map { (it + 0.5f) / 16f - 0.5f }.toFloatArray()

    fun dither(x: Int, y: Int): Float = bayer[(y and 3) * 4 + (x and 3)]

    fun mixInto(out: FloatArray, r: Float, g: Float, b: Float, t: Float) {
        out[0] += (r - out[0]) * t
        out[1] += (g - out[1]) * t
        out[2] += (b - out[2]) * t
    }
}

/**
 * Paints the canvas a pixel at a time: [shade] is asked for the linear light at the middle of every
 * pixel (x and y in the canvas's own pixels) and writes red, green and blue into the first three
 * places of the array it is given; the rest of the array, [SCRATCH] places in all, is its own to
 * work in. With a [step] of 2 or more it is asked once for each block of that many pixels square,
 * and the picture is stretched smoothly over the canvas: for designs with no hard edges.
 *
 * The rows are shared between the phone's cores, each with its own array. Every pixel is worked
 * out from where it is and nothing else, so the picture is the same however the rows fall.
 */
internal inline fun Easel.pixels(step: Int = 1, crossinline shade: (x: Float, y: Float, px: FloatArray) -> Unit) {
    val across = ceil(w / step).toInt().coerceAtLeast(1)
    val down = ceil(h / step).toInt().coerceAtLeast(1)
    val out = IntArray(across * down)
    val band = 16
    val bands = (down + band - 1) / band
    IntStream.range(0, bands).parallel().forEach { b ->
        val rgb = FloatArray(SCRATCH)
        val top = b * band
        val bottom = minOf(down, top + band)
        for (j in top until bottom) {
            val y = (j + 0.5f) * step
            val row = j * across
            for (i in 0 until across) {
                rgb[0] = 0f; rgb[1] = 0f; rgb[2] = 0f
                shade((i + 0.5f) * step, y, rgb)
                val d = Shade.dither(i, j)
                out[row + i] = (0xFF shl 24) or
                    (Oklab.toChannel(rgb[0], d) shl 16) or
                    (Oklab.toChannel(rgb[1], d) shl 8) or
                    Oklab.toChannel(rgb[2], d)
            }
        }
    }
    val bitmap = Bitmap.createBitmap(out, across, down, Bitmap.Config.ARGB_8888)
    if (step == 1) {
        canvas.drawBitmap(bitmap, 0f, 0f, null)
    } else {
        canvas.drawBitmap(bitmap, Rect(0, 0, across, down), RectF(0f, 0f, across * step.toFloat(), down * step.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
    }
    bitmap.recycle()
}

/** How many numbers [pixels] gives each pixel to work in, the colour it writes among them. */
internal const val SCRATCH = 48

/** The colours a Depth design paints with, worked out once per picture. */
internal class DepthKit(val c: WallColors) {

    val dark: Boolean = c.dark

    val spectrum = Spectrum(c.accent, c.accentAlt)

    private val deep = Oklab.linearOf(c.deep)
    private val mid = Oklab.linearOf(c.mid)
    private val base = Oklab.linearOf(c.base)

    /** The backdrop's top colour and its foot, as linear light. */
    val deepLight: FloatArray get() = deep.copyOf()
    val baseLight: FloatArray get() = base.copyOf()

    /**
     * The plain sky behind a design at one height, [v] 0 at the top to 1 at the foot, into [out]
     * from [at]: the backdrop's top colour running into its foot. Its middle colour is left out,
     * as by day it is the deepest of the three and would put a grey band across the middle.
     */
    fun sky(v: Float, out: FloatArray, at: Int = 0) {
        val t = Shade.clamp01(v)
        out[at] = deep[0] + (base[0] - deep[0]) * t
        out[at + 1] = deep[1] + (base[1] - deep[1]) * t
        out[at + 2] = deep[2] + (base[2] - deep[2]) * t
    }

    /** The backdrop's top colour running into its middle one, which at night is the richest of the three. */
    fun dusk(v: Float, out: FloatArray, at: Int = 0) {
        val t = Shade.clamp01(v)
        out[at] = deep[0] + (mid[0] - deep[0]) * t
        out[at + 1] = deep[1] + (mid[1] - deep[1]) * t
        out[at + 2] = deep[2] + (mid[2] - deep[2]) * t
    }

    /** Settles the colour in [rgb]'s first three places towards the backdrop's foot by [t], for the quiet under the icons and the dock. */
    fun towardsBase(rgb: FloatArray, t: Float) = Shade.mixInto(rgb, base[0], base[1], base[2], t)

    /** Towards the backdrop's top colour by [t], for the quiet under the clock. */
    fun towardsDeep(rgb: FloatArray, t: Float) = Shade.mixInto(rgb, deep[0], deep[1], deep[2], t)

    fun ramp(lightness: Float = 1f, chroma: Float = 1f): Ramp = spectrum.ramp(lightness, chroma)

    fun rampAt(lightness: Float, chroma: Float = 1f): Ramp = spectrum.ramp(chroma = chroma, atLightness = lightness)

    /** One colour of the spectrum, as linear light. */
    fun colour(t: Float, lightness: Float = 1f, chroma: Float = 1f, atLightness: Float = Float.NaN): FloatArray {
        val lch = FloatArray(3)
        spectrum.at(t, lch)
        val out = FloatArray(3)
        Oklab.fromLch(if (atLightness.isNaN()) lch[0] * lightness else atLightness, lch[1] * chroma, lch[2], out)
        return out
    }
}
