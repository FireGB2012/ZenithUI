package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.LocalZenithFont
import com.gabriel.zenithui.ui.theme.ZenithShapes
import com.gabriel.zenithui.ui.theme.glass
import kotlinx.coroutines.delay

/**
 * Hello again, in whichever language was on screen, and a place to answer it. The greeting picks
 * up your name as you type it. Everything hangs from the top, so the keyboard coming up moves only
 * the button, never the words. Leaving it blank is fine — the button turns into a skip.
 */
@Composable
fun NameStep(
    name: String,
    onNameChange: (String) -> Unit,
    onDone: () -> Unit,
    greeting: Int,
    playIntro: Boolean,
    /** True once this screen has finished arriving; asking for the keyboard mid-transition stutters both. */
    canFocus: Boolean,
) {
    val palette = LocalPalette.current
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }

    LaunchedEffect(canFocus) {
        if (!canFocus) return@LaunchedEffect
        delay(120)
        runCatching { focus.requestFocus() }
        keyboard?.show()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(40.dp))
        GreetingLine(greetingFor(greeting, name), Modifier.rise(0, playIntro))
        Spacer(Modifier.height(8.dp))
        Headline("What should we\ncall you?", Modifier.rise(1, playIntro))
        Spacer(Modifier.height(32.dp))

        val ring by animateColorAsState(
            if (focused) palette.accent else Color.Transparent,
            tween(240),
            label = "fieldRing",
        )
        BasicTextField(
            value = name,
            onValueChange = { if (it.length <= 24) onNameChange(it.replace("\n", "")) },
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = LocalZenithFont.current,
                color = palette.onGlass,
                fontSize = 26.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = (-0.4).sp,
                textAlign = TextAlign.Center,
            ),
            cursorBrush = SolidColor(palette.accent),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(onNext = { onDone() }),
            modifier = Modifier
                .fillMaxWidth()
                .rise(2, playIntro)
                .focusRequester(focus)
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { inner ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .glass(ZenithShapes.pill, strength = 0.9f)
                        .border(1.5.dp, ring, ZenithShapes.pill)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (name.isEmpty()) {
                        Text(
                            "Your first name",
                            color = palette.onGlass.copy(alpha = 0.3f),
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (-0.4).sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                    inner()
                }
            },
        )

        Spacer(Modifier.height(12.dp))
        Text(
            text = "Only used to say hello. It stays on this phone.",
            color = palette.onGlassMuted,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.rise(3, playIntro),
        )
    }
}
