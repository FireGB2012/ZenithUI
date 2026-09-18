package com.gabriel.zenithui.ui.onboarding

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.ui.theme.LocalPalette
import kotlinx.coroutines.delay

/**
 * The first thing you see. No splash, no logo spinning: a question, a place to answer it, and the
 * keyboard already up. Leaving it blank is fine — the button turns into a skip.
 */
@Composable
fun NameStep(
    name: String,
    onNameChange: (String) -> Unit,
    onDone: () -> Unit,
    playIntro: Boolean,
) {
    val palette = LocalPalette.current
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var focused by remember { mutableStateOf(false) }

    // Once the words have landed, bring the keyboard up so you can just start typing.
    LaunchedEffect(Unit) {
        delay(if (playIntro) 420 else 180)
        runCatching { focus.requestFocus() }
        keyboard?.show()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp),
    ) {
        Spacer(Modifier.height(36.dp))
        ScreenTitle(
            eyebrow = "Welcome to Zenith",
            headline = "What should we\ncall you?",
            lede = null,
            play = playIntro,
        )
        Spacer(Modifier.height(40.dp))

        val underline by animateColorAsState(
            if (focused) palette.accent else palette.onGlass.copy(alpha = 0.2f),
            tween(260),
            label = "underline",
        )
        Column(Modifier.rise(2, playIntro)) {
            BasicTextField(
                value = name,
                onValueChange = { if (it.length <= 24) onNameChange(it.replace("\n", "")) },
                singleLine = true,
                textStyle = TextStyle(color = palette.onGlass, fontSize = 28.sp, fontWeight = FontWeight.Medium),
                cursorBrush = SolidColor(palette.accent),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next,
                ),
                keyboardActions = KeyboardActions(onNext = { onDone() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .onFocusChanged { focused = it.isFocused },
                decorationBox = { inner ->
                    Box {
                        if (name.isEmpty()) {
                            Text(
                                "Your first name",
                                color = palette.onGlass.copy(alpha = 0.28f),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        inner()
                    }
                },
            )
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(underline)
            )
        }

        Spacer(Modifier.height(14.dp))
        Text(
            text = "Only used to say hello. It stays on this phone.",
            color = palette.onGlassMuted,
            fontSize = 13.sp,
            modifier = Modifier.rise(3, playIntro),
        )
    }
}
