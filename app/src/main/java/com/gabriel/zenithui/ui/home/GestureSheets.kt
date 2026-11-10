package com.gabriel.zenithui.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.AppInfo
import com.gabriel.zenithui.data.AppShortcut
import com.gabriel.zenithui.data.GestureActions
import com.gabriel.zenithui.data.Gestures
import com.gabriel.zenithui.ui.common.AppIcon
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.SectionLabel
import com.gabriel.zenithui.ui.common.appearIn
import com.gabriel.zenithui.ui.common.popIn
import com.gabriel.zenithui.ui.common.popOut
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.staggerDelay
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont

/**
 * What one gesture could do: Zenith's own actions first, each saying what it does, then every app
 * — and under an app, its shortcuts, opened with the arrow beside it. The one chosen now carries a
 * tick, which pops onto whichever is tapped as the sheet goes.
 */
@Composable
fun GestureActionSheet(
    visible: Boolean,
    gesture: String,
    current: String,
    apps: List<AppInfo>,
    /** Whether the lock helper is on, so Lock the phone can say it is still to be set up. */
    helperOn: Boolean,
    shortcutsOf: suspend (String) -> List<AppShortcut>,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    var query by remember { mutableStateOf("") }
    val words = query.trim().lowercase()
    val shown = remember(apps, words) { if (words.isEmpty()) apps else apps.filter { words in it.label.lowercase() } }
    var opened by remember { mutableStateOf<String?>(null) }

    ZenithSheet(visible = visible, onDismiss = onDismiss, title = Gestures.label(gesture), maxHeightFraction = 0.86f) {
        LazyColumn(Modifier.weight(1f, fill = false)) {
            items(GestureActions.fixed, key = { it }) { action ->
                val index = GestureActions.fixed.indexOf(action)
                ChoiceLine(
                    title = GestureActions.label(action) { null },
                    detail = GestureActions.detail(action),
                    note = if (GestureActions.needsHelper(action) && !helperOn) "Set up" else null,
                    selected = current == action,
                    modifier = Modifier.appearIn(delayMillis = staggerDelay(index), riseFrom = 18f),
                ) { onPick(action) }
            }
            item(key = "apps") {
                SectionLabel("Or open an app", Modifier.padding(start = 10.dp, top = 18.dp, bottom = 8.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp)
                        .height(42.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(palette.onGlass.copy(alpha = 0.08f))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.onGlassMuted, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) Text("Find an app", color = palette.onGlassMuted, fontSize = 14.sp)
                        BasicTextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            textStyle = TextStyle(fontFamily = LocalZenithFont.current, color = palette.onGlass, fontSize = 14.sp),
                            cursorBrush = SolidColor(palette.accent),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
            items(shown, key = { "app:" + it.key }) { app ->
                AppChoice(
                    app = app,
                    current = current,
                    open = opened == app.key,
                    onToggle = { opened = if (opened == app.key) null else app.key },
                    shortcutsOf = shortcutsOf,
                    onPick = onPick,
                )
            }
        }
    }
}

/** One thing to choose: what it is, what it does, and a tick when it is the one. */
@Composable
private fun ChoiceLine(
    title: String,
    detail: String?,
    note: String? = null,
    selected: Boolean,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val fill by animateColorAsState(if (selected) palette.accent.copy(alpha = 0.14f) else Color.Transparent, tween(220), label = "choiceFill")
    Row(
        modifier
            .fillMaxWidth()
            .pressBounce(0.98f)
            .clip(RoundedCornerShape(16.dp))
            .background(fill)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.width(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (detail != null) Text(detail, color = palette.onGlassMuted, fontSize = 12.sp)
        }
        if (note != null) {
            Text(note, color = palette.onGlassMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.width(8.dp))
        }
        AnimatedVisibility(visible = selected, enter = popIn(reduced), exit = popOut(reduced)) {
            Icon(Icons.Rounded.Check, contentDescription = "Chosen", tint = palette.accent, modifier = Modifier.size(20.dp))
        }
        trailing?.invoke()
    }
}

/** An app to open, and — under the arrow beside it — the shortcuts it offers. */
@Composable
private fun AppChoice(
    app: AppInfo,
    current: String,
    open: Boolean,
    onToggle: () -> Unit,
    shortcutsOf: suspend (String) -> List<AppShortcut>,
    onPick: (String) -> Unit,
) {
    val palette = LocalPalette.current
    val reduced = LocalMotionLevel.current.reduced
    val turn by animateFloatAsState(if (open) 180f else 0f, if (reduced) tween(Motion.REDUCED_MS) else Motion.snappy(), label = "expand")
    Column {
        ChoiceLine(
            title = app.label,
            detail = null,
            selected = current == GestureActions.app(app.key),
            leading = { AppIcon(key = app.key, label = app.label, size = 32.dp) },
            trailing = {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable(onClick = onToggle),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.ExpandMore,
                        contentDescription = "Its shortcuts",
                        tint = palette.onGlassMuted,
                        modifier = Modifier.size(20.dp).graphicsLayer { rotationZ = turn },
                    )
                }
            },
        ) { onPick(GestureActions.app(app.key)) }
        AnimatedVisibility(visible = open, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
            val shortcuts by produceState<List<AppShortcut>?>(initialValue = null, app.key) {
                value = shortcutsOf(app.packageName)
            }
            Column(Modifier.padding(start = 44.dp)) {
                when {
                    shortcuts == null -> Spacer(Modifier.height(8.dp))
                    shortcuts!!.isEmpty() -> Text(
                        "${app.label} offers no shortcuts",
                        color = palette.onGlassMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                    else -> shortcuts!!.forEachIndexed { index, shortcut ->
                        ChoiceLine(
                            title = shortcut.label,
                            detail = null,
                            selected = current == shortcut.key,
                            leading = { AppIcon(key = shortcut.key, label = shortcut.label, size = 26.dp) },
                            modifier = Modifier.appearIn(delayMillis = staggerDelay(index), riseFrom = 14f),
                        ) { onPick(shortcut.key) }
                    }
                }
            }
        }
    }
}
