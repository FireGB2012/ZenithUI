package com.gabriel.zenithui.ui.home.widgets

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.WidgetForm
import com.gabriel.zenithui.ui.home.HomeTypingArea
import com.gabriel.zenithui.ui.home.LocalBare
import com.gabriel.zenithui.ui.home.rememberToday
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import java.time.format.DateTimeFormatter

/**
 * Notes: a note you write on the widget itself, its first line set as its title, the way a note is
 * written. Headed in yellow, as notes are.
 *
 * - Mini: a note's symbol and the first words.
 * - Strip: the note on one line, its title first, to jot on.
 * - Square, Wide, Tall and Big: "Notes" and the day over the note, with more room each.
 */
@Composable
internal fun NotesWidget(form: WidgetForm, width: Dp, height: Dp, note: String, onNoteChange: (String) -> Unit) {
    val today = rememberToday()
    val day = remember(today) { today.format(DateTimeFormatter.ofPattern("EEE d MMM")) }
    val yellow = hue(Hue.Yellow)

    @Composable
    fun Heading(date: Boolean) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Glyph(Icons.Rounded.StickyNote2, 15.dp, tint = yellow)
            Spacer(Modifier.width(6.dp))
            Label("Notes", color = wordHue(yellow), modifier = Modifier.weight(1f))
            if (date) Words(day, Type.caps, weight = FontWeight.SemiBold, color = soft())
        }
    }

    Fitted(form, width, height) { w, _ ->
        val root = Modifier.fillMaxSize()
        when (form) {
            WidgetForm.Mini -> Column(root.padding(INSET_MINI)) {
                Glyph(Icons.Rounded.StickyNote2, 14.dp, tint = yellow)
                Spacer(Modifier.height(2.dp))
                NoteField(note, onNoteChange, Type.caps, title = false, single = false, placeholder = "Note", modifier = Modifier.weight(1f))
            }

            WidgetForm.Strip -> Row(root.padding(horizontal = INSET_STRIP), verticalAlignment = Alignment.CenterVertically) {
                Glyph(Icons.Rounded.StickyNote2, 18.dp, tint = yellow)
                Spacer(Modifier.width(10.dp))
                NoteField(note, onNoteChange, Type.body, title = false, single = true, placeholder = "Jot something down", modifier = Modifier.weight(1f))
            }

            WidgetForm.Square -> Column(root.padding(INSET)) {
                Heading(date = false)
                Spacer(Modifier.height(8.dp))
                NoteField(note, onNoteChange, Type.body, title = true, single = false, placeholder = "Tap to write a note", modifier = Modifier.weight(1f))
            }

            else -> Column(root.padding(INSET)) {
                Heading(date = true)
                Spacer(Modifier.height(10.dp))
                NoteField(
                    note, onNoteChange,
                    if (form == WidgetForm.Big && w >= 300.dp) 15.sp else Type.body,
                    title = true, single = false, placeholder = "Tap to write a note",
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * The note, written on in place. With [title] its first line is set larger and bolder, as a note's
 * title is. While it is being typed in, a touch anywhere else on the home screen ends the typing.
 */
@Composable
private fun NoteField(
    note: String,
    onNoteChange: (String) -> Unit,
    size: TextUnit,
    title: Boolean,
    single: Boolean,
    placeholder: String,
    modifier: Modifier,
) {
    val palette = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    DisposableEffect(Unit) { onDispose { if (focused) HomeTypingArea.bounds = null } }
    val bare = LocalBare.current
    val shade = androidx.compose.material3.LocalTextStyle.current.shadow
    val style = TextStyle(fontFamily = LocalZenithFont.current, color = ink(), fontSize = size, lineHeight = size * 1.32f, shadow = shade)
    val rest = soft()
    val look = remember(size, title, single, rest) {
        when {
            single -> oneLine(rest)
            title -> titleLine(size)
            else -> VisualTransformation.None
        }
    }
    BasicTextField(
        value = note,
        onValueChange = onNoteChange,
        singleLine = single,
        textStyle = style,
        cursorBrush = SolidColor(hue(Hue.Yellow)),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
        visualTransformation = look,
        modifier = modifier
            .fillMaxWidth()
            .then(if (single) Modifier.fadeEnd(20.dp).padding(vertical = 4.dp) else Modifier)
            .onGloballyPositioned { coords ->
                bounds = coords.boundsInRoot()
                if (focused) HomeTypingArea.bounds = bounds
            }
            .onFocusChanged { state ->
                focused = state.isFocused
                HomeTypingArea.bounds = if (state.isFocused) bounds else null
            },
        decorationBox = { inner ->
            Box(Modifier.fillMaxWidth()) {
                if (note.isEmpty()) {
                    Words(
                        placeholder,
                        size,
                        weight = FontWeight.Normal,
                        color = palette.onGlass.copy(alpha = if (bare) 0.8f else 0.55f),
                        maxLines = if (single) 1 else 3,
                    )
                }
                inner()
            }
        },
    )
}

/**
 * The end of a line that runs on past its room, faded out rather than cut through a letter. The
 * layer is drawn on its own so the fade takes the words' shadow with them.
 */
private fun Modifier.fadeEnd(length: Dp) = graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen).drawWithContent {
    drawContent()
    val fade = length.toPx()
    drawRect(
        Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = size.width - fade, endX = size.width),
        blendMode = BlendMode.DstIn,
    )
}

/**
 * A note on one line: its lines run on with a space for each break, the first, its title, semibold and
 * the rest in [rest]. Only how it looks: the note keeps its lines for the bigger sizes.
 */
private fun oneLine(rest: Color) = VisualTransformation { text ->
    val end = text.text.indexOf('\n').let { if (it < 0) text.text.length else it }
    val styled = buildAnnotatedString {
        append(text.text.replace('\n', ' '))
        if (end > 0) addStyle(SpanStyle(fontWeight = FontWeight.SemiBold), 0, end)
        if (end < text.text.length) addStyle(SpanStyle(color = rest), end, text.text.length)
    }
    TransformedText(styled, OffsetMapping.Identity)
}

/** A note's first line set as its title: a step larger and semibold. The text itself is unchanged. */
private fun titleLine(size: TextUnit) = VisualTransformation { text ->
    val end = text.text.indexOf('\n').let { if (it < 0) text.text.length else it }
    val styled = buildAnnotatedString {
        append(text)
        if (end > 0) addStyle(SpanStyle(fontWeight = FontWeight.SemiBold, fontSize = size * 1.15f), 0, end)
    }
    TransformedText(styled, OffsetMapping.Identity)
}
