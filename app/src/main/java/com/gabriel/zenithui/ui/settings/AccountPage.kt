package com.gabriel.zenithui.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gabriel.zenithui.data.Aura
import com.gabriel.zenithui.data.findActivity
import com.gabriel.zenithui.data.server.Signed
import com.gabriel.zenithui.ui.common.LocalMotionLevel
import com.gabriel.zenithui.ui.common.Motion
import com.gabriel.zenithui.ui.common.popIn
import com.gabriel.zenithui.ui.common.popOut
import com.gabriel.zenithui.ui.common.pressBounce
import com.gabriel.zenithui.ui.common.rememberHaptics
import com.gabriel.zenithui.ui.common.shake
import com.gabriel.zenithui.ui.common.unfoldIn
import com.gabriel.zenithui.ui.common.unfoldOut
import com.gabriel.zenithui.ui.onboarding.PrimaryButton
import com.gabriel.zenithui.ui.onboarding.Segmented
import com.gabriel.zenithui.ui.theme.LocalPalette
import com.gabriel.zenithui.ui.theme.ZenithShapes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

/**
 * Everything Settings can ask of a Zenith account. Each one that can be refused answers with why,
 * in words for the person, or with null when it worked. A build with no server has none of this:
 * see [SettingsActions.account].
 */
class AccountActions(
    /** Who is signed in on this phone, or null. */
    val signed: Signed?,
    /** The Google "Web application" client ID, or empty when Google sign-in is not offered. */
    val googleClientId: String,
    val create: suspend (username: String, password: String) -> String?,
    val signIn: suspend (username: String, password: String) -> String?,
    val signInWithGoogle: suspend (idToken: String, email: String?) -> String?,
    val signOut: suspend () -> Unit,
    /** The password again, for an account that has one; null for one made with Google. */
    val delete: suspend (password: String?) -> String?,
    val backUp: suspend () -> String?,
    val restore: suspend () -> String?,
    /** Asks the server how the account stands now. */
    val refresh: () -> Unit,
)

/**
 * The Account page. Signed out, it is two boxes and a button, or one tap with Google; signed in,
 * it says who you are, what the account holds, and how to leave or delete it.
 */
@Composable
internal fun AccountOptions(actions: SettingsActions) {
    val account = actions.account ?: return
    // Both belong to the page, not to either half of it: what is asked of the server in one half
    // may be answered when the other is on show.
    val scope = rememberCoroutineScope()
    var said by remember { mutableStateOf<String?>(null) }
    val reduced = LocalMotionLevel.current.reduced
    LaunchedEffect(Unit) { account.refresh() }
    Spot("account") {
        AnimatedContent(
            targetState = account.signed,
            // Signed in or not is what changes the page; the account itself only changes what it says.
            contentKey = { it != null },
            transitionSpec = {
                if (reduced) {
                    fadeIn(tween(Motion.REDUCED_MS)).togetherWith(fadeOut(tween(Motion.REDUCED_MS)))
                } else {
                    fadeIn(tween(220, delayMillis = 60)).togetherWith(fadeOut(tween(120)))
                }
            },
            label = "accountSigned",
        ) { signed ->
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (signed != null) {
                    SignedIn(signed, account, scope, said, onSaid = { said = it })
                } else {
                    SignedOut(account, scope, said, onSaid = { said = it })
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------- signed out

@Composable
private fun SignedOut(account: AccountActions, scope: CoroutineScope, said: String?, onSaid: (String?) -> Unit) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    // The keyboard is put away, and the box keeps the focus: taking the focus away as well would
    // hand it, on a phone with real keys, to the first thing on the page that can be pressed.
    val keyboard = LocalSoftwareKeyboardController.current
    val haptics = rememberHaptics()
    val reduced = LocalMotionLevel.current.reduced
    var making by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var refusals by remember { mutableIntStateOf(0) }

    /** One attempt at getting in at a time; when it did not work, the page says why. */
    fun attempt(work: suspend () -> String?) {
        if (busy) return
        busy = true
        onSaid(null)
        scope.launch {
            val refused = try {
                work()
            } finally {
                busy = false
            }
            onSaid(refused)
            if (refused == null) haptics.confirm() else refusals++
        }
    }

    fun submit() {
        val name = username.trim()
        when {
            name.isEmpty() || password.isEmpty() -> {
                onSaid("Type a username and a password.")
                refusals++
            }
            else -> {
                keyboard?.hide()
                if (making) attempt { account.create(name, password) } else attempt { account.signIn(name, password) }
            }
        }
    }

    Note(
        "An account makes your Aura code count for real and, with Zenith Plus, keeps a backup of your home screen. " +
            "Without one, nothing leaves this phone.",
        Modifier.riseIn(0),
    )
    Box(Modifier.riseIn(1)) {
        Segmented(
            options = listOf("in" to "Sign in", "new" to "Create account"),
            selected = if (making) "new" else "in",
            onSelect = {
                making = it == "new"
                onSaid(null)
            },
        )
    }
    Group(padding = 14.dp, rise = 2) {
        AccountField(
            value = username,
            onChange = {
                // A username is small letters, numbers and _: what cannot be in one is not typed.
                username = it.lowercase().filter { ch -> ch in 'a'..'z' || ch in '0'..'9' || ch == '_' }.take(20)
                onSaid(null)
            },
            hint = "Username",
        )
        Spacer(Modifier.height(10.dp))
        AccountField(
            value = password,
            onChange = {
                password = it.take(200)
                onSaid(null)
            },
            hint = "Password",
            secret = true,
            action = ImeAction.Done,
            onAction = { submit() },
        )
        AnimatedVisibility(visible = making, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
            Note(
                "3 to 20 small letters, numbers or _ for the username, and at least 8 characters for the password. " +
                    "Zenith keeps no email address, so a forgotten password cannot be reset: write it down.",
                Modifier.padding(top = 12.dp),
            )
        }
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            text = when {
                busy -> "One moment…"
                making -> "Create account"
                else -> "Sign in"
            },
            onClick = { submit() },
            modifier = Modifier.shake(refusals),
        )
        AnimatedVisibility(visible = said != null, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
            Text(said.orEmpty(), color = palette.onGlass, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp, start = 4.dp, end = 4.dp))
        }
    }

    if (account.googleClientId.isNotEmpty()) {
        Row(
            Modifier
                .riseIn(3)
                .fillMaxWidth()
                .height(50.dp)
                .pressBounce(0.97f)
                .clip(ZenithShapes.pill)
                .background(palette.onGlass.copy(alpha = 0.09f))
                .border(1.dp, palette.onGlass.copy(alpha = 0.12f), ZenithShapes.pill)
                .clickable {
                    if (busy) return@clickable
                    keyboard?.hide()
                    scope.launch {
                        // Android's own chooser is shown over the activity, so it is the activity that asks.
                        when (val answer = askGoogle(context.findActivity() ?: context, account.googleClientId)) {
                            is GoogleAnswer.Chosen -> attempt { account.signInWithGoogle(answer.idToken, answer.email) }
                            is GoogleAnswer.Failed -> {
                                onSaid(answer.message)
                                refusals++
                            }
                            GoogleAnswer.BackedOut -> Unit
                        }
                    }
                },
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            GoogleMark()
            Spacer(Modifier.width(10.dp))
            Text("Continue with Google", color = palette.onGlass, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Google's G as a letter in its own blue: enough to say whose button it is, without a picture file. */
@Composable
private fun GoogleMark() {
    Box(Modifier.size(22.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
        Text("G", color = Color(0xFF4285F4), fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

/**
 * A box to type a username or a password in. In use it is ringed in the accent, like the search
 * box; a password is dots until the eye at its end is tapped.
 */
@Composable
private fun AccountField(
    value: String,
    onChange: (String) -> Unit,
    hint: String,
    secret: Boolean = false,
    action: ImeAction = ImeAction.Next,
    onAction: () -> Unit = {},
) {
    val palette = LocalPalette.current
    var focused by remember { mutableStateOf(false) }
    var shown by remember { mutableStateOf(false) }
    val ring by animateColorAsState(if (focused) palette.accent.copy(alpha = 0.75f) else Color.Transparent, tween(220), label = "fieldRing")
    Row(
        Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(palette.onGlass.copy(alpha = 0.08f))
            .border(1.5.dp, ring, RoundedCornerShape(16.dp))
            .padding(start = 16.dp, end = if (secret) 6.dp else 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(hint, color = palette.onGlassMuted, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = palette.onGlass, fontSize = 15.sp),
                cursorBrush = SolidColor(palette.accent),
                visualTransformation = if (secret && !shown) PasswordVisualTransformation() else VisualTransformation.None,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    keyboardType = if (secret) KeyboardType.Password else KeyboardType.Ascii,
                    imeAction = action,
                ),
                // Only "done" is taken over: "next" keeps Android's own meaning, on to the next box.
                keyboardActions = KeyboardActions(onDone = { onAction() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.isFocused }
                    // The hint is drawn behind the box, not in it, so a screen reader is told what the box is for.
                    .semantics { contentDescription = hint },
            )
        }
        if (secret) {
            val reduced = LocalMotionLevel.current.reduced
            Box(
                Modifier
                    .size(38.dp)
                    .pressBounce(0.86f)
                    .clip(CircleShape)
                    .clickable { shown = !shown },
                contentAlignment = Alignment.Center,
            ) {
                // The eye shuts and opens: one gives way to the other where it stands.
                AnimatedContent(
                    targetState = shown,
                    transitionSpec = { popIn(reduced, from = 0.5f).togetherWith(popOut(reduced, to = 0.5f)) },
                    label = "passwordEye",
                ) { open ->
                    Icon(
                        if (open) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (open) "Hide password" else "Show password",
                        tint = palette.onGlassMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------- signed in

@Composable
private fun SignedIn(signed: Signed, account: AccountActions, scope: CoroutineScope, said: String?, onSaid: (String?) -> Unit) {
    val palette = LocalPalette.current
    val context = LocalContext.current
    // The keyboard is put away, and the box keeps the focus: taking the focus away as well would
    // hand it, on a phone with real keys, to the first thing on the page that can be pressed.
    val keyboard = LocalSoftwareKeyboardController.current
    val reduced = LocalMotionLevel.current.reduced
    val name = signed.account.username?.let { "@$it" } ?: signed.email ?: "Google account"
    // The letter on the accent: whichever of dark and white reads.
    val onAccent = if (palette.accent.luminance() > 0.3f) Color(0xFF0B0C10) else Color.White

    Group(padding = 16.dp, rise = 0) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(palette.accent, palette.accentAlt))),
                contentAlignment = Alignment.Center,
            ) {
                val letter = name.firstOrNull { it.isLetterOrDigit() }?.uppercase()
                if (letter != null) {
                    Text(letter, color = onAccent, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Rounded.Person, contentDescription = null, tint = onAccent, modifier = Modifier.size(24.dp))
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(name, color = palette.onGlass, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(if (signed.account.google) "Signed in with Google" else "Zenith account", color = palette.onGlassMuted, fontSize = 13.sp)
            }
        }
    }

    Group(padding = 16.dp, rise = 1) {
        Held("Aura code", signed.account.code)
        Spacer(Modifier.height(12.dp))
        Held(
            "Friends who used it",
            if (signed.account.referralCap > 0) "${signed.account.referrals} of ${signed.account.referralCap}" else "${signed.account.referrals}",
        )
        Spacer(Modifier.height(12.dp))
        Held("Backup", signed.account.backupAt?.let { "Saved ${moment(it)}" } ?: "None yet")
        Spacer(Modifier.height(12.dp))
        Note(
            "Each friend who uses your code gets ${Aura.REFERRAL_REWARD} aura, and so do you. " +
                "A backup is saved from Backup & reset, with Zenith Plus.",
        )
    }

    var leaving by remember { mutableStateOf(false) }
    Group(rise = 2) {
        ActionRow(
            icon = Icons.AutoMirrored.Rounded.Logout,
            title = if (leaving) "Signing out…" else "Sign out",
            subtitle = "Your home screen stays as it is",
        ) {
            if (leaving) return@ActionRow
            leaving = true
            onSaid(null)
            scope.launch {
                try {
                    if (signed.account.google) forgetGoogle(context)
                    account.signOut()
                } finally {
                    leaving = false
                }
            }
        }
    }

    // Deleting cannot be undone, so it is two steps: the row opens what will happen, and a
    // password account has to give its password again before the second tap does it.
    var deleting by remember { mutableStateOf(false) }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var refusals by remember { mutableIntStateOf(0) }
    Spot("accountDelete") {
        Group(rise = 3) {
            ActionRow(
                icon = Icons.Rounded.DeleteForever,
                title = when {
                    busy -> "Deleting…"
                    deleting -> "Delete it for good"
                    else -> "Delete account"
                },
                subtitle = if (deleting) "Tap again, or go back to keep it" else "Removes it from Zenith's server",
                danger = true,
                modifier = Modifier.shake(refusals),
            ) {
                when {
                    busy -> Unit
                    !deleting -> deleting = true
                    else -> {
                        busy = true
                        onSaid(null)
                        keyboard?.hide()
                        scope.launch {
                            val google = signed.account.google
                            val refused = try {
                                account.delete(if (google) null else password)
                            } finally {
                                busy = false
                            }
                            // Said to the page, not to this half of it: if the answer was that this
                            // phone is signed out, it is the other half that is there to say it.
                            onSaid(refused)
                            if (refused != null) refusals++ else if (google) forgetGoogle(context)
                        }
                    }
                }
            }
            AnimatedVisibility(visible = deleting, enter = unfoldIn(reduced), exit = unfoldOut(reduced)) {
                Column(Modifier.padding(start = 14.dp, end = 14.dp, bottom = 14.dp)) {
                    Note("The account, its Aura code and its backup are deleted from Zenith's server, at once and for good. Nothing on this phone changes.")
                    if (!signed.account.google) {
                        Spacer(Modifier.height(10.dp))
                        AccountField(
                            value = password,
                            onChange = {
                                password = it.take(200)
                                onSaid(null)
                            },
                            hint = "Your password, to be sure it is you",
                            secret = true,
                            action = ImeAction.Done,
                            onAction = { keyboard?.hide() },
                        )
                    }
                    if (said != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(said, color = palette.onGlass, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
            }
        }
    }
}

/** One thing the account holds: what it is, and its value at the end of the line. */
@Composable
private fun Held(what: String, value: String) {
    val palette = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(what, color = palette.onGlassMuted, fontSize = 14.sp, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        Text(value, color = palette.onGlass, fontSize = 14.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A moment in time, the way the phone writes dates. */
internal fun moment(millis: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(millis))
