package com.gabriel.zenithui.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.edgeIn
import com.gabriel.zenithui.ui.common.edgeOut
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberTick
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.glass

/**
 * A pill of glass rising from the foot of the screen when something has been taken off the home
 * screen: what it was, and Undo, round which a ring runs down for as long as it can still be put
 * back. Tapping Undo puts it back; the pill sinks away when the time is up.
 */
@Composable
fun UndoPill(what: String?, key: Any?, millis: Long, onUndo: () -> Unit, modifier: Modifier = Modifier) {
    val reduced = LocalMotionLevel.current.reduced
    // Kept while the pill goes, so its words do not empty out as it sinks.
    val shown = remember { arrayOf("") }
    if (what != null) shown[0] = what
    AnimatedVisibility(
        visible = what != null,
        enter = edgeIn(reduced, fromFoot = true),
        exit = edgeOut(reduced, toFoot = true),
        modifier = modifier,
    ) {
        val palette = LocalPalette.current
        val tick = rememberTick()
        val left = remember(key) { Animatable(1f) }
        LaunchedEffect(key) {
            if (!reduced) left.animateTo(0f, tween(millis.toInt(), easing = LinearEasing))
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .widthIn(max = 360.dp)
                .glass(RoundedCornerShape(50), solid = 0.4f)
                .padding(start = 18.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        ) {
            Text(
                shown[0],
                color = palette.onGlass,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            Spacer(Modifier.width(10.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .pressBounce(0.9f)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                        tick()
                        onUndo()
                    }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
            ) {
                Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                    // The time still left to put it back, running down.
                    Canvas(Modifier.size(26.dp)) {
                        val stroke = 2.dp.toPx()
                        drawArc(
                            color = palette.accent,
                            startAngle = -90f,
                            sweepAngle = 360f * left.value,
                            useCenter = false,
                            style = Stroke(stroke, cap = StrokeCap.Round),
                            topLeft = androidx.compose.ui.geometry.Offset(stroke / 2f, stroke / 2f),
                            size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                        )
                    }
                    Icon(Icons.AutoMirrored.Rounded.Undo, contentDescription = null, tint = palette.accent, modifier = Modifier.size(15.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("Undo", color = palette.accent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** What [item] is called in the pill: the app's name, the folder's, the widget's. */
fun removedName(item: com.gabriel.zenithui.data.HomeItem, appsByKey: Map<String, com.gabriel.zenithui.data.AppInfo>): String = when (item) {
    is com.gabriel.zenithui.data.AppCell -> appsByKey[item.key]?.label ?: "App"
    is com.gabriel.zenithui.data.FolderCell -> item.title.ifBlank { "Folder" }
    is com.gabriel.zenithui.data.WidgetCell -> com.gabriel.zenithui.data.Widgets.byType(item.type)?.title ?: "Widget"
    is com.gabriel.zenithui.data.ShortcutCell -> item.label
    is com.gabriel.zenithui.data.HostedWidgetCell -> "Widget"
    is com.gabriel.zenithui.data.StackCell -> "Widget stack"
}
