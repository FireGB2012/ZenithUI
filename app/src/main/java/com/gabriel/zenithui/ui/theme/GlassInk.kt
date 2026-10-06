package com.gabriel.zenithui.ui.theme

import android.graphics.BitmapShader
import android.graphics.LinearGradient
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import kotlin.math.hypot
import kotlin.math.max

/**
 * Makes whatever this draws out of glass, the way iOS makes the Lock Screen clock: no pane behind
 * it, the numerals and symbols themselves are the glass. Its own outline becomes a rounded rim, so
 * the wallpaper behind it bends and magnifies towards its edges, a crisp highlight runs along the
 * edges facing the light and moves as the phone tilts, and a soft shade round it keeps it clear of
 * the wallpaper. What is drawn white is clear glass; what is drawn in a colour is glass of that
 * colour, toned by how bright the wallpaper behind it is.
 *
 * [bevel] is how far in from its edges the glass stays curved: about a third of the stroke width
 * looks right, so a big numeral gets a wider rim than a small icon.
 *
 * Android 13+ draws it with a shader over Zenith's copy of the wallpaper, or with frosted glass
 * when there is no copy to show. Older Android draws it as it is, a little translucent.
 */
@Composable
fun Modifier.glassInk(bevel: Dp = 4.dp, halo: Boolean = true): Modifier {
    if (Build.VERSION.SDK_INT < 33) return this.graphicsLayer { alpha = 0.92f }

    val backdrop = LocalBackdrop.current
    val sharp = LocalGlassBackdrop.current ?: backdrop
    val screen = LocalScreenSize.current
    val wallpaperOffset = LocalWallpaperOffset.current
    val tilt = LocalTilt.current
    val strength = LocalGlassStrength.current.coerceIn(0f, 1.4f)
    val lift = LocalGlassBody.current.lift
    val ink = remember(sharp, backdrop) {
        // A GPU that cannot compile it still shows the shape, plainly.
        runCatching { GlassInkShader(sharp, backdrop) }.getOrNull()
    }
    var origin by remember { mutableStateOf(Offset.Zero) }

    return this
        // The rim's highlight and the shade round the shape need a little room past its edges.
        .bleed(INK_BLEED)
        .onGloballyPositioned { origin = it.positionInWindow() }
        .graphicsLayer {
            // Read here, not in composition: moving, paging and tilting only rebuild the effect.
            val lean = tilt.value
            renderEffect = ink?.effect(
                size = size,
                origin = origin,
                screen = screen,
                wallpaperOffset = systemWallpaperOffset(wallpaperOffset.value),
                bevel = bevel.toPx(),
                light = Offset(-0.45f - 1.6f * lean.x, -1f - 1.2f * lean.y),
                strength = strength,
                halo = halo,
                lift = lift,
            )?.asComposeRenderEffect()
        }
        .padding(INK_BLEED)
}

private val INK_BLEED = 8.dp

/** How much more light lifted glass gathers over a dark picture, on top of what it always does. */
private const val INK_GATHER = 0.27f

/**
 * Lets the next modifiers draw [margin] past this element's edges on every side without taking
 * any more room in the layout: measured bigger, reported at the original size, placed back by the
 * margin so the content lands exactly where it would have been.
 */
private fun Modifier.bleed(margin: Dp): Modifier = layout { measurable, constraints ->
    val extra = margin.roundToPx()
    val placeable = measurable.measure(constraints.offset(extra * 2, extra * 2))
    layout((placeable.width - extra * 2).coerceAtLeast(0), (placeable.height - extra * 2).coerceAtLeast(0)) {
        placeable.place(-extra, -extra)
    }
}

/** The shader behind [glassInk]: one per element, rebuilt only when the wallpaper changes. */
@RequiresApi(33)
internal class GlassInkShader(sharp: ImageBitmap?, blurred: ImageBitmap?) {
    private val sharpSize = sharp?.let { Size(it.width.toFloat(), it.height.toFloat()) }
    private val blurSize = blurred?.let { Size(it.width.toFloat(), it.height.toFloat()) }
    private val seeThrough = sharp != null && blurred != null
    private val shader = RuntimeShader(PROGRAM).apply {
        if (sharp != null && blurred != null) {
            setInputShader("wallpaper", bitmapShader(sharp))
            setInputShader("diffused", bitmapShader(blurred))
        } else {
            // Nothing to see through: the program takes its frosted branch and never samples these.
            setInputShader("wallpaper", flat())
            setInputShader("diffused", flat())
        }
    }

    fun effect(
        size: Size,
        /** Top-left of the element's layer on screen, in pixels. */
        origin: Offset,
        screen: IntSize,
        wallpaperOffset: Offset,
        bevel: Float,
        light: Offset,
        strength: Float,
        halo: Boolean,
        /** How far the glass is lifted off a picture of its own tone: see GlassBody. */
        lift: Float = 0f,
    ): RenderEffect? {
        if (size.width <= 0f || size.height <= 0f) return null
        val visible = seeThrough && sharpSize != null && blurSize != null && screen.width > 0 && screen.height > 0
        if (visible) {
            // The wallpaper as Android shows it, exactly as the panes line it up.
            val scale = max(screen.width / sharpSize!!.width, screen.height / sharpSize.height) * SYSTEM_WALLPAPER_ZOOM
            val cropX = (sharpSize.width * scale - screen.width) * wallpaperOffset.x.coerceIn(0f, 1f)
            val cropY = (sharpSize.height * scale - screen.height) * wallpaperOffset.y.coerceIn(0f, 1f)
            shader.setFloatUniform("origin", origin.x + cropX, origin.y + cropY)
            shader.setFloatUniform("imageScale", 1f / scale)
            shader.setFloatUniform("blurRatio", blurSize!!.width / sharpSize.width, blurSize.height / sharpSize.height)
        } else {
            shader.setFloatUniform("origin", 0f, 0f)
            shader.setFloatUniform("imageScale", 1f)
            shader.setFloatUniform("blurRatio", 1f, 1f)
        }
        shader.setFloatUniform("seeThrough", if (visible) 1f else 0f)
        shader.setFloatUniform("bevel", bevel.coerceAtLeast(1f))
        shader.setFloatUniform("refraction", 0.55f + 0.45f * strength.coerceAtMost(1.2f))
        val length = hypot(light.x, light.y).coerceAtLeast(0.001f)
        shader.setFloatUniform("light", light.x / length, light.y / length)
        val lifted = lift.coerceIn(0f, 1f)
        shader.setFloatUniform("halo", if (halo) 0.26f + 0.10f * lifted else 0f)
        shader.setFloatUniform("gather", INK_GATHER * lifted)
        // The uniforms are copied into the effect as it is made, so every change needs a new one.
        return RenderEffect.createRuntimeShaderEffect(shader, "content")
    }

    private fun bitmapShader(image: ImageBitmap) =
        BitmapShader(image.asAndroidBitmap(), Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply {
            filterMode = BitmapShader.FILTER_MODE_LINEAR
        }

    private fun flat() = LinearGradient(0f, 0f, 1f, 1f, 0xFF808080.toInt(), 0xFF808080.toInt(), Shader.TileMode.CLAMP)

    companion object {
        /**
         * The element's own drawing arrives as `content`; only its coverage shapes the glass (plus
         * its colour, as a tint). Coverage averaged over a small disc is a height map: 1 deep in a
         * stroke, a half on its edge, 0 outside. The same taps give the slope, so one pass yields
         * both how curved the glass is here and which way it faces.
         */
        internal const val PROGRAM = """
            uniform shader content;
            uniform shader wallpaper;
            uniform shader diffused;
            uniform float2 origin;
            uniform float imageScale;
            uniform float2 blurRatio;
            uniform float seeThrough;
            uniform float bevel;
            uniform float refraction;
            uniform float2 light;
            uniform float halo;
            uniform float gather;

            float3 behind(float2 point, float blur) {
                if (seeThrough < 0.5) {
                    // No picture of the wallpaper: frosted glass, which still takes the light.
                    return float3(0.60, 0.62, 0.68);
                }
                float2 uv = (point + origin) * imageScale;
                float3 sharp = wallpaper.eval(uv).rgb;
                float3 soft = diffused.eval(uv * blurRatio).rgb;
                return mix(sharp, soft, blur);
            }

            half4 main(float2 p) {
                half4 src = content.eval(p);
                float a = src.a;

                float h = a;
                float2 slope = float2(0.0);
                for (int ring = 1; ring <= 3; ring++) {
                    float r = bevel * float(ring) / 3.0;
                    for (int k = 0; k < 12; k++) {
                        // Each ring turned a little against the last, so the taps do not line up.
                        float angle = (float(k) + 0.33 * float(ring)) * 0.52359878;
                        float2 dir = float2(cos(angle), sin(angle));
                        float c = content.eval(p + dir * r).a;
                        h += c;
                        slope += c * dir;
                    }
                }
                h /= 37.0;

                // Right on the edge, a line of even width whatever the bevel: how much of a tiny
                // ring round this point falls outside the shape.
                float edgeRing = 0.0;
                float edgeWidth = min(1.4 * bevel, 4.0);
                for (int k = 0; k < 8; k++) {
                    float angle = float(k) * 0.78539816;
                    edgeRing += content.eval(p + float2(cos(angle), sin(angle)) * edgeWidth).a;
                }
                float hairline = clamp((a - edgeRing / 8.0) * 2.2, 0.0, 1.0);

                // Round the shape, a soft shade that lifts it off the wallpaper.
                float shade = halo * smoothstep(0.0, 0.7, h);
                if (a <= 0.002) {
                    return half4(0.0, 0.0, 0.0, half(shade));
                }

                // How curved the glass is here: 1 on the edge, 0 where it has gone flat.
                float t = clamp((1.0 - h) * 2.0, 0.0, 1.0);
                float curve = 1.0 - sqrt(max(1.0 - t * t, 0.0));
                float steep = length(slope);
                float2 inward = steep > 0.0001 ? slope / steep : float2(0.0);
                float2 normal = -inward;

                // Through the glass: what is behind is pulled in from further inside, magnified
                // as through the rounded edge of a thick lens, with a whisper of colour split.
                float2 bend = inward * curve * bevel * 1.7 * refraction;
                float blur = max(0.18 * (1.0 - 0.5 * t), 0.4 * curve);
                float3 color = float3(
                    behind(p + bend * 0.94, blur).r,
                    behind(p + bend, blur).g,
                    behind(p + bend * 1.06, blur).b
                );
                float luma = dot(color, float3(0.2126, 0.7152, 0.0722));
                color = mix(float3(luma), color, 1.25);

                // Coloured ink makes coloured glass: its tone follows the brightness behind it,
                // as tinted glass does. White ink stays clear.
                float3 ink = clamp(float3(src.rgb) / max(a, 0.001), 0.0, 1.0);
                float chroma = max(ink.r, max(ink.g, ink.b)) - min(ink.r, min(ink.g, ink.b));
                color = mix(color, ink * (0.35 + 0.9 * luma), clamp(chroma * 1.4, 0.0, 0.85));
                // Glass gathers light: it reads brighter than the picture it sits on, and most
                // over a dark one, where clear glass would be lost. Over a pale one it is the other
                // way about, and the glass holds a little shade of its own.
                float lowlight = 1.0 - smoothstep(0.08, 0.62, luma);
                color = mix(color, float3(1.0), 0.12 + gather * lowlight);
                color *= 1.0 - 1.5 * gather * smoothstep(0.62, 0.95, luma);

                // Light: a crisp line along the edges facing it, a weaker one where it leaves,
                // a soft bloom on the lit curve, and the rim brighter at its grazing angle.
                float facing = dot(normal, light);
                float lit = max(facing, 0.0);
                float away = max(-facing, 0.0);
                float rim = hairline * (0.12 + 0.3 * gather + 0.58 * pow(lit, 1.4) + 0.28 * pow(away, 1.8));
                float bloom = t * t * t * (0.18 * pow(lit, 2.0) + 0.06 * pow(away, 2.4));
                color += rim + bloom + pow(t, 4.0) * 0.04;
                // The curve turned from the light sits in the glass's own shadow.
                color *= 1.0 - 0.12 * t * t * away;

                float alpha = a + shade * (1.0 - a);
                return half4(half3(clamp(color, 0.0, 1.0) * a), half(alpha));
            }
        """
    }
}
