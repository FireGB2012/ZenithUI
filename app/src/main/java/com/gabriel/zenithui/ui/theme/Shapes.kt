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

/** A true superellipse — the continuous curve behind Zenith's icon tiles. */
class SquircleShape(private val exponent: Float = 2.9f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val rx = size.width / 2f
        val ry = size.height / 2f
        val p = 2f / exponent
        val steps = 96
        for (i in 0..steps) {
            val t = (i.toFloat() / steps) * (2f * Math.PI).toFloat()
            val c = cos(t)
            val s = sin(t)
            val x = rx + rx * (if (c < 0f) -1f else 1f) * abs(c).pow(p)
            val y = ry + ry * (if (s < 0f) -1f else 1f) * abs(s).pow(p)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

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

    private val outlines = HashMap<String, Shape>()

    fun forIconShape(id: String): Shape = when (id) {
        "circle" -> RoundedCornerShape(percent = 50)
        "rounded" -> RoundedCornerShape(22)
        "square" -> RoundedCornerShape(4)
        "pebble", "teardrop", "hexagon", "clover" -> synchronized(outlines) { outlines.getOrPut(id) { IconOutlineShape(id) } }
        else -> icon
    }
}
