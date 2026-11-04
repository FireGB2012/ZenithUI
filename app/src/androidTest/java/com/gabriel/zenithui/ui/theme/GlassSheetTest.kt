package com.gabriel.zenithui.ui.theme

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Picture
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.Typeface
import androidx.annotation.RequiresApi
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import com.gabriel.zenithui.data.GlassBody
import com.gabriel.zenithui.data.GlassLevels
import com.gabriel.zenithui.data.WallStyle
import com.gabriel.zenithui.data.WallpaperArt
import com.gabriel.zenithui.data.WallpaperSource
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.max

/**
 * Not a check: a home screen's worth of glass painted over a few wallpapers, to be pulled off the
 * device and looked at, which is the only way to judge a material. The panes are drawn by the
 * launcher's own [drawLiquidGlass], from its own recipe, over a wallpaper lined up the way Android
 * shows it.
 *
 * Run it with `adb shell am instrument -w -e class com.gabriel.zenithui.ui.theme.GlassSheetTest`
 * and the test runner's name, then pull `glass-*.png` from the app's external cache. It takes:
 * - `-e bodies clear,frosted,solid` for a sheet in each of those (see [GlassBody]); Clear alone otherwise;
 * - `-e path soft` for the glass a phone without the shader draws, and `-e path none` for the
 *   glass with no copy of the wallpaper to show;
 * - `-e lean 0.6,-0.3` for the phone tipped that far (see [LocalTilt]).
 */
@RunWith(AndroidJUnit4::class)
@SdkSuppress(minSdkVersion = 33)
@RequiresApi(33)
class GlassSheetTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val arguments = InstrumentationRegistry.getArguments()
    private val density = 2.625f
    private val screen = IntSize(1080, 2424)

    private class Pane(
        val name: String,
        val left: Float,
        val top: Float,
        val width: Float,
        val height: Float,
        val shape: Shape,
        val material: GlassMaterial,
        val strength: Float,
        val solid: Float = 0f,
        val icons: Int = 0,
        val pressed: Boolean = false,
    )

    /** The panes of a home screen, in dp: widgets, search, a folder, small controls, a sheet and the dock. */
    private val panes = listOf(
        Pane("Widget", 20f, 120f, 176f, 176f, ZenithShapes.card, GlassMaterial.Regular, 1f),
        Pane("Widget", 214f, 120f, 176f, 176f, ZenithShapes.card, GlassMaterial.Regular, 1f, pressed = true),
        Pane("Search apps or the web", 20f, 318f, 371f, 52f, ZenithShapes.pill, GlassMaterial.Regular, 1.2f),
        Pane("", 28f, 396f, 64f, 64f, ZenithShapes.icon, GlassMaterial.Clear, 1.2f),
        Pane("", 116f, 400f, 56f, 56f, CircleShape, GlassMaterial.Clear, 1.1f),
        Pane("Clear pill", 196f, 404f, 194f, 46f, ZenithShapes.pill, GlassMaterial.Clear, 0.9f),
        Pane("Sheet", 12f, 492f, 387f, 250f, RoundedCornerShape(34.dp), GlassMaterial.Regular, 1.2f, solid = 0.65f),
        Pane("", 12f, 800f, 387f, 86f, ZenithShapes.panel, GlassMaterial.Clear, 1.15f, icons = 4),
    )

    @Test
    fun writesGlassSheets() {
        val scenes = listOf(
            Triple("waves-dark", WallpaperArt.render(WallStyle.Waves, Palettes.Midnight.toned(true).wallColors(), 1080, 2424), true),
            Triple("bubbles-light", WallpaperArt.render(WallStyle.Bubbles, Palettes.Frost.toned(false).wallColors(), 1080, 2424), false),
            Triple("city-dark", WallpaperArt.render(WallStyle.City, Palettes.Sunset.toned(true).wallColors(), 1080, 2424), true),
            Triple("photo-dark", photo(), true),
            Triple("photo-light", photo(), false),
        )
        val bodies = arguments.getString("bodies")?.split(',')?.map { GlassBody.byId(it.trim()) } ?: listOf(GlassBody.Clear)
        for ((name, wallpaper, dark) in scenes) for (body in bodies) {
            val sheet = paint(wallpaper, dark, body)
            val suffix = if (bodies.size > 1) "-${body.id}" else ""
            File(context.externalCacheDir, "glass-$name$suffix.png").outputStream().use {
                sheet.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            sheet.recycle()
        }
    }

    private fun paint(wallpaper: Bitmap, dark: Boolean, body: GlassBody): Bitmap {
        val source = WallpaperSource(context)
        runBlocking { source.present(wallpaper) }
        val levels = source.levels.value!!
        val path = arguments.getString("path") ?: "lens"
        // What the glass is given to show: everything, only the frosted copy, or nothing at all.
        val shown: GlassLevels? = levels.takeIf { path != "none" }
        val lens = shown?.takeIf { path == "lens" }?.let(::LiquidGlassShader)
        val lean = arguments.getString("lean")?.split(',')?.map { it.trim().toFloat() }?.let { Offset(it[0], it[1]) } ?: Offset.Zero
        val palette = (if (dark) Palettes.Midnight else Palettes.Frost).toned(dark)

        val picture = Picture()
        val canvas = picture.beginRecording(screen.width, screen.height)
        drawWallpaper(canvas, levels)
        val words = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.onGlass.toArgb()
            textSize = 15f * density
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val scope = CanvasDrawScope()
        for (pane in panes) {
            val size = Size(pane.width * density, pane.height * density)
            val origin = Offset(pane.left * density, pane.top * density)
            val outline = pane.shape.createOutline(size, LayoutDirection.Ltr, Density(density))
            val edge = android.graphics.Path().apply { addOutline(outline) }
            val look = glassLook(
                material = pane.material,
                strength = 0.8f * pane.strength,
                solid = pane.solid,
                opacity = 1f,
                dark = dark,
                tint = palette.glassTint,
                depth = body,
            )
            canvas.save()
            canvas.translate(origin.x, origin.y)
            canvas.save()
            canvas.clipOutPath(edge)
            for (shadow in shadowPaints(look, pane.material, dark, density, size)) canvas.drawPath(edge, shadow)
            canvas.restore()
            canvas.clipPath(edge)
            val touch = if (pane.pressed) GlassTouch(Offset(size.width * 0.3f, size.height * 0.6f), size.maxDimension * 0.42f, 1f) else GlassTouch.None
            scope.draw(Density(density), LayoutDirection.Ltr, androidx.compose.ui.graphics.Canvas(canvas), size) {
                drawLiquidGlass(
                    paneSize = size,
                    outline = outline,
                    lens = lens,
                    softened = shown?.frost,
                    origin = origin,
                    screen = screen,
                    wallpaperOffset = Offset(0.5f, 0.5f),
                    look = look,
                    lean = lean,
                    dark = dark,
                    touch = touch,
                )
            }
            if (pane.name.isNotEmpty()) canvas.drawText(pane.name, 18f * density, 30f * density, words)
            if (pane.name == "Sheet") {
                words.alpha = 158
                canvas.drawText("Words on a sheet have to stay easy to read,", 18f * density, 62f * density, words)
                canvas.drawText("whatever the wallpaper is doing behind them.", 18f * density, 84f * density, words)
                words.alpha = 255
            }
            if (pane.icons > 0) drawIcons(canvas, size, pane.icons)
            canvas.restore()
        }
        picture.endRecording()
        // A picture is played back on the GPU, which is the only place the shader runs.
        val hardware = Bitmap.createBitmap(picture)
        return hardware.copy(Bitmap.Config.ARGB_8888, false).also { hardware.recycle() }
    }

    /** The wallpaper as Android shows it, by the same sums the glass lines itself up with. */
    private fun drawWallpaper(canvas: Canvas, levels: GlassLevels) {
        val bitmap = levels.sharp.asAndroidBitmap()
        val scale = max(screen.width / bitmap.width.toFloat(), screen.height / bitmap.height.toFloat()) * SYSTEM_WALLPAPER_ZOOM
        val shown = systemWallpaperOffset(Offset(0.5f, 0.5f))
        canvas.save()
        canvas.translate(-(bitmap.width * scale - screen.width) * shown.x, -(bitmap.height * scale - screen.height) * shown.y)
        canvas.scale(scale, scale)
        canvas.drawBitmap(bitmap, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))
        canvas.restore()
    }

    private fun drawIcons(canvas: Canvas, size: Size, count: Int) {
        val side = 60f * density
        val gap = (size.width - side * count) / (count + 1)
        val colours = intArrayOf(0xFFFFFFFF.toInt(), 0xFF34C759.toInt(), 0xFF0A84FF.toInt(), 0xFFFF9F0A.toInt())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (i in 0 until count) {
            val left = gap + i * (side + gap)
            val top = (size.height - side) / 2f
            paint.color = colours[i % colours.size]
            canvas.drawRoundRect(left, top, left + side, top + side, side * 0.27f, side * 0.27f, paint)
        }
    }

    /**
     * A stand-in for a photograph: big fields of strong colour, hard edges, fine lines and print,
     * which between them show everything glass does to a picture - how far it bends it, how much
     * it softens it, and what of it the rim throws back.
     */
    private fun photo(): Bitmap {
        val w = 1080
        val h = 2424
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.shader = LinearGradient(
            0f, 0f, w.toFloat(), h.toFloat(),
            intArrayOf(0xFF1B2A6B.toInt(), 0xFFB23A8E.toInt(), 0xFFFF8A3D.toInt(), 0xFFFFD86B.toInt()),
            floatArrayOf(0f, 0.4f, 0.75f, 1f),
            Shader.TileMode.CLAMP,
        )
        canvas.drawRect(0f, 0f, w.toFloat(), h.toFloat(), paint)
        // A sun and a few lights.
        val lights = listOf(
            floatArrayOf(820f, 520f, 300f), floatArrayOf(180f, 1250f, 240f),
            floatArrayOf(900f, 1700f, 280f), floatArrayOf(260f, 2150f, 200f),
        )
        val tones = intArrayOf(0xFFFFF2B0.toInt(), 0xFF39E5C8.toInt(), 0xFF6FA8FF.toInt(), 0xFFFF5C8A.toInt())
        lights.forEachIndexed { i, (x, y, r) ->
            paint.shader = RadialGradient(x, y, r, intArrayOf(tones[i], tones[i] and 0x00FFFFFF), floatArrayOf(0.55f, 1f), Shader.TileMode.CLAMP)
            canvas.drawCircle(x, y, r, paint)
        }
        paint.shader = null
        // Hard-edged bars, the kind a skyline or a window frame puts behind a pane.
        paint.color = 0xFF10131A.toInt()
        for (i in 0 until 9) canvas.drawRect(60f + i * 120f, 2000f - (i * 53 % 190), 130f + i * 120f, 2424f, paint)
        paint.color = 0xFFF6F7FB.toInt()
        for (i in 0 until 6) canvas.drawRect(0f, 980f + i * 46f, w.toFloat(), 992f + i * 46f, paint)
        // Fine diagonal lines, to see the bend.
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 5f
        paint.color = 0xCCFFFFFF.toInt()
        for (i in -12 until 24) canvas.drawLine(i * 90f, 0f, i * 90f + 900f, h.toFloat(), paint)
        paint.style = Paint.Style.FILL
        // Print.
        paint.color = 0xFFFFFFFF.toInt()
        paint.textSize = 54f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        for (i in 0 until 26) canvas.drawText("Zenith liquid glass 0123456789 the quick brown fox", -40f - (i % 3) * 60f, 90f + i * 92f, paint)
        return bitmap
    }
}
