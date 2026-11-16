package com.gabriel.zenithui.ui.home.widgets

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.TodoItem
import com.gabriel.zenithui.data.Todos
import com.gabriel.zenithui.data.WidgetForm
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.home.AsManyAsFit
import com.gabriel.zenithui.ui.home.HomeTypingArea
import com.gabriel.zenithui.ui.home.LocalBare
import com.gabriel.zenithui.ui.home.LocalWidgetWorld
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.glassInk
import com.gabriel.zenithui.ui.theme.inkOnTint

/**
 * To-do: a list you tick off on the widget, headed with how many are left, as a reminders list is,
 * its ticks blue (its words too on a card). What is done drops below what is not, struck through, and
 * can be ticked back.
 *
 * - Mini: how many are left.
 * - Strip: the next thing, with its tick, and how many more.
 * - Square: the heading and the first few.
 * - Wide: the heading and the list in two columns.
 * - Tall: the heading, the list, and a line to add one at the foot.
 * - Big: the same, with a way to clear what is done.
 */
@Composable
internal fun TodoWidget(form: WidgetForm, width: Dp, height: Dp) {
    val world = LocalWidgetWorld.current
    val todos = world?.settings?.todos.orEmpty()
    val left = Todos.left(todos)
    val blue = hue(Hue.Blue)
    fun change(next: List<TodoItem>) = world?.change?.invoke { it.copy(todos = next) }
    val indexed = remember(todos) { todos.mapIndexed { i, item -> i to item } }
    val spoken = if (left == 0) "To do, all done" else "To do, $left left"

    @Composable
    fun Heading() {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Words("To do", Type.headline, weight = FontWeight.SemiBold, color = wordHue(blue), modifier = Modifier.weight(1f))
            Hero("$left", 22.sp, weight = FontWeight.Medium, color = wordHue(blue), rolling = true)
        }
    }

    @Composable
    fun Nothing(modifier: Modifier) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Glyph(Icons.Rounded.CheckCircle, 18.dp, tint = blue)
            Spacer(Modifier.width(8.dp))
            Words(if (todos.isEmpty()) "Nothing to do yet" else "All done", Type.body, weight = FontWeight.Medium, color = soft())
        }
    }

    Fitted(form, width, height) { w, _ ->
        val root = Modifier.fillMaxSize()
        when (form) {
            WidgetForm.Mini -> Column(root.padding(INSET_MINI).says(spoken), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Hero("$left", 24.sp, weight = FontWeight.Medium, color = wordHue(blue), rolling = true)
                Label("to do")
            }

            WidgetForm.Strip -> Row(root.padding(horizontal = INSET_STRIP), verticalAlignment = Alignment.CenterVertically) {
                val next = indexed.firstOrNull { !it.second.done }
                if (next == null) {
                    Nothing(Modifier.weight(1f))
                } else {
                    Item(next.second, Modifier.weight(1f)) { change(Todos.toggle(todos, next.first)) }
                    if (left > 1) Words("+${left - 1}", Type.meta, weight = FontWeight.SemiBold, color = soft())
                }
            }

            WidgetForm.Square -> Column(root.padding(INSET)) {
                Heading()
                Spacer(Modifier.height(8.dp))
                if (todos.isEmpty() || left == 0) {
                    Nothing(Modifier.weight(1f))
                } else {
                    List(indexed.filter { !it.second.done }, Modifier.weight(1f)) { i -> change(Todos.toggle(todos, i)) }
                }
            }

            WidgetForm.Wide -> Column(root.padding(INSET)) {
                Heading()
                Spacer(Modifier.height(8.dp))
                if (todos.isEmpty()) {
                    Nothing(Modifier.weight(1f))
                } else {
                    val half = (indexed.size + 1) / 2
                    Row(Modifier.weight(1f)) {
                        List(indexed.take(half.coerceAtMost(3)), Modifier.weight(1f).fillMaxHeight()) { i -> change(Todos.toggle(todos, i)) }
                        Spacer(Modifier.width(12.dp))
                        List(indexed.drop(half.coerceAtMost(3)).take(3), Modifier.weight(1f).fillMaxHeight()) { i -> change(Todos.toggle(todos, i)) }
                    }
                }
            }

            else -> Column(root.padding(INSET)) {
                Heading()
                Spacer(Modifier.height(8.dp))
                if (todos.isEmpty()) {
                    Nothing(Modifier.padding(vertical = 6.dp))
                    Spacer(Modifier.weight(1f))
                } else {
                    List(indexed, Modifier.weight(1f)) { i -> change(Todos.toggle(todos, i)) }
                }
                AddLine(blue) { text -> change(Todos.add(todos, text)) }
                if (form == WidgetForm.Big && todos.any { it.done } && w >= 200.dp) {
                    Spacer(Modifier.height(4.dp))
                    Words(
                        "Clear what is done",
                        Type.meta,
                        weight = FontWeight.SemiBold,
                        color = wordHue(blue),
                        modifier = Modifier
                            .pressBounce(0.94f)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { change(Todos.clearDone(todos)) }
                            .padding(vertical = 4.dp),
                    )
                }
            }
        }
    }
}

/** The list's items, as many as there is room for, with how many more under them when some are left out. */
@Composable
private fun List(items: List<Pair<Int, TodoItem>>, modifier: Modifier, onTick: (Int) -> Unit) {
    AsManyAsFit(
        items,
        modifier,
        gap = 2.dp,
        more = { more -> Words("$more more", Type.meta, weight = FontWeight.Medium, color = soft(), modifier = Modifier.padding(start = 30.dp)) },
    ) { (index, item) -> Item(item, Modifier.fillMaxWidth()) { onTick(index) } }
}

/** One thing to do: its tick and its words, the whole line a place to tick it. */
@Composable
private fun Item(item: TodoItem, modifier: Modifier, onTick: () -> Unit) {
    val reduced = LocalMotionLevel.current.reduced
    val words by animateColorAsState(if (item.done) soft().copy(alpha = 0.6f) else ink(), tween(if (reduced) Motion.REDUCED_MS else 260), label = "todoInk")
    Row(
        modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onTick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Tick(item.done)
        Spacer(Modifier.width(10.dp))
        Words(item.text, Type.body, weight = FontWeight.Normal, color = words, strike = item.done, modifier = Modifier.weight(1f, fill = false))
    }
}

/** A round tick: an empty ring, or a blue disc with its check drawing itself in as it is ticked. */
@Composable
private fun Tick(done: Boolean) {
    val reduced = LocalMotionLevel.current.reduced
    val fill by animateFloatAsState(if (done) 1f else 0f, if (reduced) tween(Motion.REDUCED_MS) else Motion.smooth(), label = "tick")
    val blue = hue(Hue.Blue)
    val ring = faint(0.55f)
    val bare = LocalBare.current
    val check = if (bare) Color.White else inkOnTint(blue)
    Box(Modifier.size(20.dp).pressBounce(0.8f).then(if (bare) Modifier.glassInk(bevel = 2.dp) else Modifier)) {
        Canvas(Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            drawCircle(if (bare) Color.White else ring.copy(alpha = ring.alpha * (1f - fill)), r - 1.dp.toPx(), style = Stroke(1.6.dp.toPx()))
            if (fill > 0f) drawCircle(if (bare) Color.White else blue, r * fill.coerceIn(0f, 1f))
            if (fill > 0.05f) {
                val draw = fill.coerceIn(0f, 1f)
                val a = Offset(size.width * 0.29f, size.height * 0.52f)
                val b = Offset(size.width * 0.44f, size.height * 0.67f)
                val c = Offset(size.width * 0.72f, size.height * 0.37f)
                val first = (draw * 2f).coerceAtMost(1f)
                val ink = if (bare) Color.Black.copy(alpha = 0.6f) else check
                drawLine(ink, a, a + (b - a) * first, 2.dp.toPx(), StrokeCap.Round)
                if (draw > 0.5f) drawLine(ink, b, b + (c - b) * ((draw - 0.5f) * 2f), 2.dp.toPx(), StrokeCap.Round)
            }
        }
    }
}

/** The line at the foot of the list for a new one, added with the keyboard's Done. */
@Composable
private fun AddLine(blue: Color, onAdd: (String) -> Unit) {
    var text by remember { mutableStateOf("") }
    var focused by remember { mutableStateOf(false) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    DisposableEffect(Unit) { onDispose { if (focused) HomeTypingArea.bounds = null } }
    val shade = androidx.compose.material3.LocalTextStyle.current.shadow
    Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Glyph(Icons.Rounded.Add, 20.dp, tint = blue)
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f)) {
            if (text.isEmpty()) Words("Add something", Type.body, weight = FontWeight.Normal, color = soft())
            BasicTextField(
                value = text,
                onValueChange = { text = it.take(80) },
                singleLine = true,
                textStyle = TextStyle(fontFamily = LocalZenithFont.current, color = ink(), fontSize = Type.body, shadow = shade),
                cursorBrush = SolidColor(blue),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    onAdd(text)
                    text = ""
                }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coords ->
                        bounds = coords.boundsInRoot()
                        if (focused) HomeTypingArea.bounds = bounds
                    }
                    .onFocusChanged { state ->
                        focused = state.isFocused
                        HomeTypingArea.bounds = if (state.isFocused) bounds else null
                    },
            )
        }
    }
}
