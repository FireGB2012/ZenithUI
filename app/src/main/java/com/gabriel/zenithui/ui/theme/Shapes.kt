package com.gabriel.zenithui.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import com.gabriel.zenithui.data.IconShapes
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * How far from the middle the edge of a unit superellipse is, [angle] radians round from straight
 * out to the right: 1 at the middle of every side, and further towards each corner the higher the
 * [exponent] is. An exponent of 2 is a circle, 1 all the way round; a square's corner would be the
 * square root of 2 out.
 */
internal fun superellipseRadius(angle: Float, exponent: Float): Float {
    val across = abs(cos(angle)).pow(exponent)
    val up = abs(sin(angle)).pow(exponent)
    return (across + up).pow(-1f / exponent)
}

/**
 * Points round a superellipse's outline, for this shape and for the icon renderer's
 * ([IconShapes.path]) alike, so the two agree. Enough that even the iPhone's fuller corners on the
 * largest icon are within a tenth of a pixel of the true curve.
 */
internal const val SUPERELLIPSE_STEPS = 192

/** Zenith's own squircle, the shape icons had before there was a choice. */
internal const val SQUIRCLE_EXPONENT = 2.9f

/**
 * A true superellipse: the continuous curve behind Zenith's icon tiles. The higher its [exponent],
 * the flatter its sides and the fuller its corners: Zenith's squircle is 2.9, the iPhone's 5.
 *
 * A data class, because the same exponent is the same outline: a shape made afresh is equal to the
 * one before it, and never counts as a change.
 */
data class SquircleShape(val exponent: Float = SQUIRCLE_EXPONENT) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val rx = size.width / 2f
        val ry = size.height / 2f
        for (i in 0..SUPERELLIPSE_STEPS) {
            val angle = (i.toFloat() / SUPERELLIPSE_STEPS) * (2f * Math.PI).toFloat()
            val reach = superellipseRadius(angle, exponent)
            val x = rx + rx * reach * cos(angle)
            val y = ry + ry * reach * sin(angle)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * The exponent the lens draws a free-form outline with: the shape's own when it is a
 * [SquircleShape], and Zenith's squircle for any other, which is how the lens has always drawn them.
 */
internal fun squircleExponent(shape: Shape): Float = (shape as? SquircleShape)?.exponent ?: SQUIRCLE_EXPONENT

/**
 * One of [IconShapes]' outlines as a Compose shape: the very path the icon renderer cuts pictures
 * with, so a tile drawn here and an icon cut there are the same shape to the pixel.
 */
class IconOutlineShape(private val id: String) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val side = minOf(size.width, size.height).coerceAtLeast(1f)
        val path = IconShapes.path(id, side)
        if (size.width != side || size.height != side) {
            path.transform(android.graphics.Matrix().apply { setScale(size.width / side, size.height / side) })
        }
        return Outline.Generic(path.asComposePath())
    }
}

object ZenithShapes {
    val panel = RoundedCornerShape(30.dp)
    val card = RoundedCornerShape(24.dp)
    val tile = RoundedCornerShape(20.dp)
    val pill = RoundedCornerShape(percent = 50)
    val icon = SquircleShape()

    /** An iPhone's rounded square, its corners continuous: the same curve [IconShapes] cuts "iphone" icons to. */
    private val iphone = SquircleShape(5f)

    private val outlines = HashMap<String, Shape>()

    fun forIconShape(id: String): Shape = when (id) {
        "circle" -> RoundedCornerShape(percent = 50)
        "rounded" -> RoundedCornerShape(22)
        "square" -> RoundedCornerShape(4)
        "iphone" -> iphone
        "pebble", "teardrop", "hexagon", "clover" -> synchronized(outlines) { outlines.getOrPut(id) { IconOutlineShape(id) } }
        else -> icon
    }
}
