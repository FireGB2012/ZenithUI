package com.gabriel.zenithui.ui.settings

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException

/** What came of asking Android for a Google account to sign in with. */
internal sealed interface GoogleAnswer {
    /**
     * Google vouches for whoever chose the account: [idToken] is its word for it, which Zenith's
     * server checks for itself. [email] is only shown on the Account page; it is not sent on.
     */
    data class Chosen(val idToken: String, val email: String?) : GoogleAnswer

    /** The chooser was closed without choosing. Not a failure: nothing needs saying. */
    data object BackedOut : GoogleAnswer

    data class Failed(val message: String) : GoogleAnswer
}

/**
 * Puts up Android's own account chooser and waits for a choice. [context] has to be the activity:
 * the chooser is shown over it. [clientId] is the "Web application" OAuth client ID that the
 * server checks the answer against; the Android client IDs that go with it in Google Cloud are
 * what let this app, signed with this key, ask at all.
 */
internal suspend fun askGoogle(context: Context, clientId: String): GoogleAnswer {
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
        .build()
    return try {
        val credential = CredentialManager.create(context).getCredential(context, request).credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val google = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleAnswer.Chosen(google.idToken, google.id.takeIf { "@" in it })
        } else {
            GoogleAnswer.Failed("Google answered with something that is not a sign-in.")
        }
    } catch (_: GetCredentialCancellationException) {
        GoogleAnswer.BackedOut
    } catch (_: NoCredentialException) {
        GoogleAnswer.Failed("There is no Google account on this phone to sign in with.")
    } catch (_: GetCredentialException) {
        GoogleAnswer.Failed("Google sign-in is not working on this phone right now.")
    } catch (_: GoogleIdTokenParsingException) {
        GoogleAnswer.Failed("Google answered with something that is not a sign-in.")
    }
}

/** Tells Android that nobody is signed in here any more, so the next sign-in asks which account again. */
internal suspend fun forgetGoogle(context: Context) {
    runCatching { CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest()) }
}
