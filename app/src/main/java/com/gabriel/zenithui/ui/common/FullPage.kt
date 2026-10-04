package com.gabriel.zenithui.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.glass

/**
 * A page the full height of the screen, laid over the home screen: glass over the wallpaper, a
 * bar with a way back and a way out, and whatever the page holds underneath it. Settings is one;
 * the widget gallery is another. It rises from the foot of the screen and goes back there.
 *
 * It sits under the sheets, not among them. While a sheet is open over it ([covered]) the page
 * empties to plain glass: where the glass is only frosted a sheet is see-through, and two layers
 * of words on top of each other can be read by nobody.
 */
@Composable
fun FullPage(
    visible: Boolean,
    covered: Boolean,
    title: String,
    canGoBack: Boolean,
    /** How much of the bar's own small title shows, 0 to 1: it comes in as the large one scrolls away. */
    titleShown: () -> Float,
    onBack: () -> Unit,
    onClose: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val focus = LocalFocusManager.current
    val reduced = LocalMotionLevel.current.reduced
    val uncovered = animateFloatAsState(if (covered) 0f else 1f, tween(200), label = "pageUncovered")

    Box(Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(if (reduced) Motion.REDUCED_MS else 300)),
            exit = fadeOut(tween(if (reduced) Motion.REDUCED_MS else 260)),
        ) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)))
        }
        AnimatedVisibility(
            visible = visible,
            // With movement reduced the page does not travel: it is there, and then it is not.
            enter = if (reduced) fadeIn(tween(Motion.REDUCED_MS)) else slideInVertically(Motion.sheet()) { it } + fadeIn(tween(220)),
            exit = if (reduced) fadeOut(tween(Motion.REDUCED_MS)) else slideOutVertically(tween(320)) { it } + fadeOut(tween(260)),
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .glass(RectangleShape, strength = 1.2f, solid = 0.9f, border = false, shadow = false)
                    // Nothing underneath takes a touch; a tap on empty space puts the keyboard away.
                    .pointerInput(Unit) { detectTapGestures { focus.clearFocus() } }
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                Column(Modifier.fillMaxSize().graphicsLayer { alpha = uncovered.value }) {
                    PageBar(title, canGoBack, titleShown, onBack, onClose)
                    Box(Modifier.weight(1f).fillMaxWidth(), content = content)
                }
            }
        }
    }
}

@Composable
private fun PageBar(title: String, canGoBack: Boolean, titleShown: () -> Float, onBack: () -> Unit, onClose: () -> Unit) {
    val palette = LocalPalette.current
    val line = palette.onGlass
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            // A hairline under the bar once something has scrolled beneath it.
            .drawBehind {
                val shown = titleShown()
                if (shown > 0f) {
                    drawLine(
                        color = line,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx(),
                        alpha = 0.08f * shown,
                    )
                }
            }
            // Pulling the bar down closes the page, the way pulling a sheet down closes a sheet.
            .pointerInput(Unit) { detectVerticalDragGestures { _, delta -> if (delta > 30f) onClose() } }
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp)) {
            androidx.compose.animation.AnimatedVisibility(
                visible = canGoBack,
                enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.7f),
                exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.7f),
            ) {
                BackButton(onBack)
            }
        }
        Text(
            title,
            color = palette.onGlass,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .graphicsLayer {
                    val shown = titleShown()
                    alpha = shown
                    // It comes up into the bar as it appears, as if the large title had shrunk into it.
                    translationY = (1f - shown) * 8.dp.toPx()
                },
        )
        Box(
            Modifier
                .size(36.dp)
                .pressBounce(0.88f)
                .clip(CircleShape)
                .background(palette.onGlass.copy(alpha = 0.08f))
                .clickable(onClick = onClose),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = palette.onGlass, modifier = Modifier.size(19.dp))
        }
    }
}

/** The round back arrow of a page's bar, and of a sheet that has pages. */
@Composable
fun BackButton(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Box(
        modifier
            .size(36.dp)
            .pressBounce(0.88f)
            .clip(CircleShape)
            .background(palette.onGlass.copy(alpha = 0.08f))
            .clickable(onClick = onBack),
        contentAlignment = Alignment.Center,
    ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = palette.onGlass, modifier = Modifier.size(19.dp))
    }
}

/**
 * How long a page takes to go once it is closed, and a little over. What a page was showing is
 * put away after this, out of sight, so that the next time it opens it starts at the top without
 * first arriving on whatever it was closed on.
 */
const val PAGE_GONE_MS = 420L

/** How far a page has to scroll before its large title has gone and the bar's small one is fully in. */
const val TITLE_GONE_PX = 110f

/**
 * The large title at the head of a page. It is part of what scrolls, and as it goes it shrinks
 * towards the bar, fades, and hangs back a little, so it seems to slide under the bar and turn
 * into the small title there. [scrolled] is how far the page has scrolled, read while drawing.
 */
@Composable
fun LargeTitle(text: String, scrolled: () -> Int, modifier: Modifier = Modifier) {
    Text(
        text,
        color = LocalPalette.current.onGlass,
        fontSize = 32.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp,
        modifier = modifier
            .padding(start = 4.dp, top = 2.dp, bottom = 4.dp)
            .graphicsLayer {
                val by = scrolled().coerceAtLeast(0)
                val gone = (by / TITLE_GONE_PX).coerceIn(0f, 1f)
                transformOrigin = TransformOrigin(0f, 0.5f)
                val size = 1f - 0.22f * gone
                scaleX = size
                scaleY = size
                alpha = 1f - gone
                translationY = by.coerceAtMost(TITLE_GONE_PX.toInt()) * 0.35f
            },
    )
}
