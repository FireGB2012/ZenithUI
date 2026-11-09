package com.gabriel.zenithui.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.theme.LocalApple
import com.gabriel.zenithui.ui.theme.LocalPalette

/**
 * A page the full height of the screen, laid over the home screen, as an iPhone draws a screen of
 * its own: solid, in Apple's page colour, with a bar along the top that holds the way back and the
 * way out, and whatever the page holds underneath it. Settings is one; the widget gallery is
 * another. It rises from the foot of the screen and goes back there.
 *
 * It sits under the sheets, not among them. While a sheet is open over it ([covered]) the page
 * empties: a sheet can be see-through, and two layers of words on top of each other can be read
 * by nobody.
 */
@Composable
fun FullPage(
    visible: Boolean,
    covered: Boolean,
    title: String,
    canGoBack: Boolean,
    /** What Back says while there is somewhere to go back to: the name of where it goes. */
    backTo: String,
    /** How much of the bar's own small title shows, 0 to 1: it comes in as the large one scrolls away. */
    titleShown: () -> Float,
    onBack: () -> Unit,
    onClose: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val focus = LocalFocusManager.current
    val reduced = LocalMotionLevel.current.reduced
    val page = LocalApple.current.page
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
                    .background(page)
                    // Nothing underneath takes a touch; a tap on empty space puts the keyboard away.
                    .pointerInput(Unit) { detectTapGestures { focus.clearFocus() } }
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding(),
            ) {
                Column(Modifier.fillMaxSize().graphicsLayer { alpha = uncovered.value }) {
                    PageBar(title, canGoBack, backTo, titleShown, onBack, onClose)
                    Box(Modifier.weight(1f).fillMaxWidth(), content = content)
                }
            }
        }
    }
}

@Composable
private fun PageBar(
    title: String,
    canGoBack: Boolean,
    backTo: String,
    titleShown: () -> Float,
    onBack: () -> Unit,
    onClose: () -> Unit,
) {
    val apple = LocalApple.current
    val reduced = LocalMotionLevel.current.reduced
    val line = apple.separator
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            // A hairline under the bar once something has scrolled beneath it, as an iPhone's has.
            .drawBehind {
                val shown = titleShown()
                if (shown > 0f) {
                    drawLine(
                        color = line,
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1f,
                        alpha = shown,
                    )
                }
            }
            // Pulling the bar down closes the page, the way pulling a sheet down closes a sheet.
            .pointerInput(Unit) { detectVerticalDragGestures { _, delta -> if (delta > 30f) onClose() } }
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            // Named in full: inside the bar's row the row's own version would be picked.
            androidx.compose.animation.AnimatedVisibility(
                visible = canGoBack,
                enter = if (reduced) fadeIn(tween(Motion.REDUCED_MS)) else fadeIn(tween(180)) + slideInHorizontally(tween(240)) { it / 3 },
                exit = fadeOut(tween(120)),
            ) {
                PageBack(backTo, onBack)
            }
        }
        Text(
            title,
            color = apple.words,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1.5f)
                .graphicsLayer {
                    val shown = titleShown()
                    alpha = shown
                    // It comes up into the bar as it appears, as if the large title had shrunk into it.
                    translationY = (1f - shown) * 8.dp.toPx()
                },
        )
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            CloseButton(onClose)
        }
    }
}

/** Apple's Back: a chevron and the name of where it goes, in the accent. It dims under a finger. */
@Composable
private fun PageBack(label: String, onBack: () -> Unit) {
    val accent = LocalPalette.current.accent
    val touch = remember { MutableInteractionSource() }
    val pressed by touch.collectIsPressedAsState()
    Row(
        Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(interactionSource = touch, indication = null, role = Role.Button, onClickLabel = "Back to $label", onClick = onBack)
            .graphicsLayer { alpha = if (pressed) 0.35f else 1f }
            .padding(start = 4.dp, end = 8.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = null, tint = accent, modifier = Modifier.size(19.dp))
        Text(label, color = accent, fontSize = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/**
 * The way out of a page or a sheet: a small round grey button with a cross in it, as an iPhone's
 * sheets have. Its room to be touched is wider than the circle.
 */
@Composable
internal fun CloseButton(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val apple = LocalApple.current
    val touch = remember { MutableInteractionSource() }
    val pressed by touch.collectIsPressedAsState()
    Box(
        modifier
            .size(44.dp)
            .clip(CircleShape)
            .clickable(interactionSource = touch, indication = null, role = Role.Button, onClick = onClose),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .graphicsLayer { alpha = if (pressed) 0.5f else 1f }
                .clip(CircleShape)
                .background(apple.field),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = apple.secondary, modifier = Modifier.size(17.dp))
        }
    }
}

/** The round back arrow of a sheet that has pages. */
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
 * The large title at the head of a page, bold, in Apple's words. It is part of what scrolls, and as
 * it goes it shrinks towards the bar, fades, and hangs back a little, so it seems to slide under
 * the bar and turn into the small title there. [scrolled] is how far the page has scrolled, read
 * while drawing.
 */
@Composable
fun LargeTitle(text: String, scrolled: () -> Int, modifier: Modifier = Modifier) {
    Text(
        text,
        color = LocalApple.current.words,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
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
