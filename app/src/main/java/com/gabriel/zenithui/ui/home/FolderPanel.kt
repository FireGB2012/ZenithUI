package com.gabriel.zenithui.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.FolderCell
import com.gabriel.zenithui.data.ZenithSettings
import com.gabriel.zenithui.ui.common.launchOrigin
import com.gabriel.zenithui.ui.common.rollOver
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import kotlinx.coroutines.delay

/**
 * A folder, open. It grows out of its own icon into a pane of glass in the middle of the screen
 * while the home screen dims behind it, and goes back into the icon the same way — so where it
 * came from, and where it has gone, is never in doubt.
 *
 * [progress] is how far open it is, 0 in the icon to 1 open, read only while drawing. [origin] is
 * the icon it came from, in the home screen's own pixels; without one (opened from a menu to be
 * renamed), or when it does not [grow], it fades in where it stands.
 */
@Composable
internal fun FolderPanel(
    folder: FolderCell,
    origin: Rect?,
    progress: () -> Float,
    grows: Boolean,
    appsByKey: Map<String, AppInfo>,
    settings: ZenithSettings,
    onOpen: (String) -> Unit,
    onRemoveApp: (String) -> Unit,
    onRename: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    var title by remember(folder.id) { mutableStateOf(folder.title) }
    // Saving on every keystroke rewrites the whole layout as you type; wait for a pause.
    LaunchedEffect(title) {
        delay(400)
        if (title.isNotBlank() && title != folder.title) onRename(title)
    }
    // Where the pane stands once open. Measured on a box that never moves, around the one that does.
    var home by remember { mutableStateOf(Rect.Zero) }

    Box(Modifier.fillMaxSize().imePadding()) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = progress().coerceIn(0f, 1f) }
                .background(Color.Black.copy(alpha = 0.38f))
                // Anywhere outside the pane puts the folder away.
                .pointerInput(Unit) { detectTapGestures { onDismiss() } }
        )
        Box(
            Modifier
                .align(Alignment.Center)
                .padding(horizontal = 22.dp)
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .onGloballyPositioned { home = it.boundsInRoot() },
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        val open = progress()
                        val from = origin
                        if (grows && from != null && !home.isEmpty) {
                            // The icon's own rectangle at 0, the pane's at 1, and everything
                            // between: one shape changing size, not one thing replacing another.
                            scaleX = from.width / home.width + (1f - from.width / home.width) * open
                            scaleY = from.height / home.height + (1f - from.height / home.height) * open
                            translationX = (from.center.x - home.center.x) * (1f - open)
                            translationY = (from.center.y - home.center.y) * (1f - open)
                            alpha = (open * 4f).coerceIn(0f, 1f)
                        } else {
                            alpha = open.coerceIn(0f, 1f)
                        }
                    }
                    .glass(RoundedCornerShape(34.dp), strength = 1.2f, solid = 0.65f)
                    // A touch on the pane itself is not a touch outside it.
                    .pointerInput(Unit) { detectTapGestures { } }
                    .padding(horizontal = 18.dp, vertical = 16.dp),
            ) {
                // What is in it comes into view once the pane is most of the way open, so the
                // icons are never seen squeezed into the shape of the icon it grew from.
                Column(Modifier.graphicsLayer { alpha = ((progress() - 0.4f) / 0.6f).coerceIn(0f, 1f) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it.replace("\n", "") },
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = LocalZenithFont.current, color = palette.onGlass, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
                            cursorBrush = SolidColor(palette.accent),
                            modifier = Modifier.weight(1f),
                        )
                        // One taken out, and the count rolls down to what is left.
                        AnimatedContent(
                            targetState = folder.keys.size,
                            transitionSpec = { rollOver(reduced = !grows, down = targetState < initialState) },
                            label = "folderCount",
                        ) { count ->
                            Text("$count apps", color = palette.onGlassMuted, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(settings.columns.coerceIn(3, 4)),
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.heightIn(max = 340.dp),
                    ) {
                        itemsIndexed(folder.keys, key = { _, app -> app }) { index, key ->
                            Box(
                                Modifier
                                    // One taken out fades where it stood, and the rest close up.
                                    .animateItem()
                                    // They come out of the middle one after another as the pane
                                    // opens, and go back in, last out first, as it closes.
                                    .graphicsLayer {
                                        if (grows) {
                                            val out = Unpack.shown(progress(), index)
                                            alpha = out
                                            val size = 0.6f + 0.4f * out
                                            scaleX = size
                                            scaleY = size
                                        }
                                    }
                                    // So an app opened from here grows out of its icon here.
                                    .launchOrigin(key)
                                    .pointerInput(key) {
                                        detectTapGestures(
                                            onTap = { onOpen(key) },
                                            onLongPress = { onRemoveApp(key) },
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                AppTile(
                                    key = key,
                                    label = appsByKey[key]?.label ?: "",
                                    iconSize = STANDARD_ICON_SIZE,
                                    showLabel = true,
                                    shape = ZenithShapes.forIconShape(settings.iconShape),
                                    badge = true,
                                    onGlass = true,
                                )
                            }
                        }
                    }
                    Text(
                        "Long press an app to move it back onto the home screen.",
                        color = palette.onGlassMuted,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

/**
 * A folder's apps coming out of it. They follow the pane as it opens, each a little behind the one
 * before, so they are seen spilling out of the middle rather than all turning up at once. It is
 * one sum of how far open the folder is, so they cost no animation of their own, and closing the
 * folder runs it backwards for nothing.
 */
internal object Unpack {

    /** How far open the pane is before the first app starts out. */
    private const val FROM = 0.35f

    /** How much later each starts than the one before, and how many get a turn of their own. */
    private const val STEP = 0.035f
    private const val TURNS = 8

    /** How much of the pane's opening one app takes over coming out. */
    private const val SPAN = 0.35f

    /**
     * How far out app [index] is with the folder [open] by this much (0 in its icon, 1 open): 0
     * while it has not started, 1 once it is in its place.
     */
    fun shown(open: Float, index: Int): Float =
        ((open - FROM - STEP * index.coerceIn(0, TURNS)) / SPAN).coerceIn(0f, 1f)
}
