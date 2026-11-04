package com.gabriel.zenithui.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.unit.dp

/** The corners of Apple's solid things: a widget's tile, a card of rows, a sheet's top. */
enum class CardKind(val shape: Shape) {
    Widget(RoundedCornerShape(22.dp)),
    Grouped(RoundedCornerShape(12.dp)),
    Sheet(RoundedCornerShape(topStart = 38.dp, topEnd = 38.dp)),
}

/**
 * Something solid, as an iPhone draws it: [kind]'s corners in the card colour of the light or dark
 * in use, or in [colour]. No rim, no glow, nothing of the wallpaper: where glass belongs, it is
 * glass; everywhere else it is this.
 */
@Composable
fun Modifier.solidCard(kind: CardKind = CardKind.Grouped, colour: Color = Color.Unspecified): Modifier {
    val fill = if (colour.isSpecified) colour else LocalApple.current.card
    return this.clip(kind.shape).background(fill)
}
