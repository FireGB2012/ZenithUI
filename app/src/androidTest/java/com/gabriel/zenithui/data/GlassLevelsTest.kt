package com.gabriel.zenithui.data

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.os.SystemClock
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * The glass shows the wallpaper through copies of it made softer and softer. They are made once,
 * by sums in a loop, and a slip there would not crash anything: it would put a wallpaper that is a
 * shade too dark, or not soft enough, behind every pane. So the copies are measured.
 */
@RunWith(AndroidJUnit4::class)
class GlassLevelsTest {

    private fun flat(colour: Int, width: Int = 240, height: Int = 520): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(colour) }

    /** Left half one colour, right half another: the hardest edge there is. */
    private fun split(left: Int, right: Int, width: Int = 480, height: Int = 320): Bitmap {
        val bitmap = flat(left, width, height)
        Canvas(bitmap).drawRect(width / 2f, 0f, width.toFloat(), height.toFloat(), Paint().apply { color = right })
        return bitmap
    }

    private fun all(levels: GlassLevels): List<ImageBitmap> = listOf(levels.sharp, levels.mist, levels.frost, levels.haze)

    /** The average of each channel, 0 to 255. */
    private fun average(image: ImageBitmap): FloatArray {
        val bitmap = image.asAndroidBitmap()
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val sum = LongArray(3)
        for (p in pixels) {
            sum[0] += Color.red(p).toLong(); sum[1] += Color.green(p).toLong(); sum[2] += Color.blue(p).toLong()
        }
        return FloatArray(3) { sum[it].toFloat() / pixels.size }
    }

    /** How many pixels across the middle row are neither of the two colours of a [split] picture: the width of its edge. */
    private fun edgeWidth(image: ImageBitmap): Float {
        val bitmap = image.asAndroidBitmap()
        val y = bitmap.height / 2
        var between = 0
        for (x in 0 until bitmap.width) {
            val red = Color.red(bitmap.getPixel(x, y))
            if (red in 13..242) between++
        }
        return between.toFloat() / bitmap.width
    }

    @Test
    fun eachCopyIsHalfTheSizeOfTheOneBefore() {
        val levels = GlassLevels.of(flat(Color.GRAY, 480, 1040))
        val sizes = all(levels).map { it.width to it.height }
        assertEquals(listOf(480 to 1040, 240 to 520, 120 to 260, 60 to 130), sizes)
    }

    @Test
    fun aPictureTooSmallToHalveStillMakesEveryCopy() {
        val levels = GlassLevels.of(flat(Color.GRAY, 3, 5))
        for (image in all(levels)) assertTrue(image.width >= 1 && image.height >= 1)
    }

    @Test
    fun aFlatPictureIsTheSameColourInEveryCopy() {
        // Dark, where a shade lost shows most, and a colour whose channels differ.
        for (colour in listOf(0xFF0A0B18.toInt(), 0xFF7C6CFF.toInt(), 0xFFEFF4FF.toInt())) {
            val levels = GlassLevels.of(flat(colour))
            for (image in all(levels)) {
                val seen = image.asAndroidBitmap().getPixel(image.width / 2, image.height / 2)
                assertEquals(Integer.toHexString(colour), Integer.toHexString(seen))
            }
        }
    }

    @Test
    fun softeningNeitherDarkensNorLightensAPicture() {
        // A dark gradient, which eighteen averages cut short rather than rounded would darken by
        // most of a tenth.
        val picture = Bitmap.createBitmap(480, 1040, Bitmap.Config.ARGB_8888)
        Canvas(picture).drawRect(
            0f, 0f, 480f, 1040f,
            Paint().apply {
                shader = LinearGradient(0f, 0f, 480f, 1040f, 0xFF05060C.toInt(), 0xFF3A2E66.toInt(), Shader.TileMode.CLAMP)
            },
        )
        val levels = GlassLevels.of(picture)
        val true0 = average(levels.sharp)
        for (image in all(levels).drop(1)) {
            val seen = average(image)
            for (channel in 0..2) {
                assertTrue(
                    "channel $channel is ${seen[channel]} in a copy ${image.width} wide, ${true0[channel]} in the picture",
                    abs(seen[channel] - true0[channel]) <= 1f,
                )
            }
        }
    }

    @Test
    fun eachCopyIsSofterThanTheOneBefore() {
        val levels = GlassLevels.of(split(Color.BLACK, Color.WHITE))
        val widths = all(levels).map(::edgeWidth)
        // As a share of the picture's width, so the copies' different sizes do not come into it.
        assertTrue("the picture itself has a hard edge: $widths", widths[0] < 0.01f)
        for (i in 1 until widths.size) {
            assertTrue("copy $i is no softer than copy ${i - 1}: $widths", widths[i] > widths[i - 1] * 1.8f)
        }
        // Misted is a light blur and hazed a heavy one: about 3 dp and about 27 on a phone, where
        // the picture is some 430 dp wide.
        assertTrue("misted: ${widths[1]}", widths[1] in 0.02f..0.09f)
        assertTrue("hazed: ${widths[3]}", widths[3] in 0.25f..0.75f)
    }

    @Test
    fun theSourceIsLeftAsItWas() {
        val picture = split(Color.RED, Color.BLUE)
        val before = picture.copy(Bitmap.Config.ARGB_8888, false)
        GlassLevels.of(picture)
        assertTrue(!picture.isRecycled && picture.sameAs(before))
    }

    @Test
    fun aWallpaperIsReadyQuickly() {
        val picture = flat(0xFF7C6CFF.toInt(), 960, 1920)
        val start = SystemClock.elapsedRealtime()
        GlassLevels.of(picture)
        val took = SystemClock.elapsedRealtime() - start
        assertTrue("took $took ms", took < 1200)
    }
}
