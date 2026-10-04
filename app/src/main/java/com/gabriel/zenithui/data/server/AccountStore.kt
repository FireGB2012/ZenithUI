package com.gabriel.zenithui.data.server

import android.content.Context
import com.gabriel.zenithui.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

/**
 * Where Zenith's server is, for this build. See `server.properties`: an empty address means the
 * build has no server, and then nothing of accounts is shown and nothing is ever sent.
 */
object ServerConfig {
    val url: String = BuildConfig.SERVER_URL.trim().trimEnd('/')

    /** The "Web application" OAuth client ID. Empty means there is no Google sign-in. */
    val googleClientId: String = BuildConfig.GOOGLE_CLIENT_ID.trim()

    val enabled: Boolean get() = url.isNotEmpty()
    val google: Boolean get() = enabled && googleClientId.isNotEmpty()
}

/** This phone's sign-in to a Zenith account. */
data class Signed(
    /** The key the server gave this phone. It opens the account, so it is never shown and never backed up. */
    val token: String,
    val account: Account,
    /** The Google address signed in with, to show on the Account page. Kept on this phone only: the server never hears it. */
    val email: String?,
)

/**
 * Keeps the sign-in, in a preferences file of its own (`account.xml`) that Android's backups and
 * phone-to-phone transfers leave out: see `res/xml/backup_rules.xml`. A new phone signs in again.
 */
class AccountStore(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)
    private val _signed = MutableStateFlow(read())

    /** Null when nobody is signed in on this phone. */
    val signed: StateFlow<Signed?> = _signed.asStateFlow()

    fun signIn(token: String, account: Account, email: String?) {
        prefs.edit()
            .putString(KEY_TOKEN, token)
            .putString(KEY_ACCOUNT, account.toJson().toString())
            .apply { if (email == null) remove(KEY_EMAIL) else putString(KEY_EMAIL, email) }
            .apply()
        _signed.value = Signed(token, account, email)
    }

    /** The account as the server last described it. Nothing happens if nobody is signed in. */
    fun update(account: Account) {
        val current = _signed.value ?: return
        prefs.edit().putString(KEY_ACCOUNT, account.toJson().toString()).apply()
        _signed.value = current.copy(account = account)
    }

    fun signOut() {
        prefs.edit().clear().apply()
        _signed.value = null
    }

    private fun read(): Signed? {
        val token = prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotEmpty() } ?: return null
        val account = runCatching { Account.fromJson(JSONObject(prefs.getString(KEY_ACCOUNT, null).orEmpty())) }.getOrNull() ?: return null
        return Signed(token, account, prefs.getString(KEY_EMAIL, null))
    }

    private companion object {
        const val FILE = "account"
        const val KEY_TOKEN = "token"
        const val KEY_ACCOUNT = "account"
        const val KEY_EMAIL = "email"
    }
}
