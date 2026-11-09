package com.gabriel.zenithui.ui.onboarding

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import kotlinx.coroutines.delay
import com.gabriel.zenithui.ui.theme.LocalApple
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background

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
    val apple = LocalApple.current
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

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

        BasicTextField(
            value = name,
            onValueChange = { if (it.length <= 24) onNameChange(it.replace("\n", "")) },
            singleLine = true,
            textStyle = TextStyle(
                fontFamily = LocalZenithFont.current,
                color = apple.words,
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
                .focusRequester(focus),
            decorationBox = { inner ->
                // Apple's grey field, the whole of it the place to type; the cursor says it is in use.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(apple.field)
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (name.isEmpty()) {
                        Text(
                            "Your first name",
                            color = apple.secondary,
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
            color = apple.secondary,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.rise(3, playIntro),
        )
    }
}
