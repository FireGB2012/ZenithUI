package com.gabriel.zenithui.data.server

import org.json.JSONObject

/** A Zenith account, as the server describes it. */
data class Account(
    /** Null for an account made with Google. */
    val username: String?,
    val google: Boolean,
    /** The Aura code friends type in. The server's word is final: it is what they are checked against. */
    val code: String,
    /** How many friends have used the code, never more than [referralCap]. */
    val referrals: Int,
    val referralCap: Int,
    /** Whether this account has used somebody else's code. It may do that once. */
    val redeemed: Boolean,
    /** When the backup in the account was saved, in milliseconds; null when there is none. */
    val backupAt: Long?,
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("username", username ?: JSONObject.NULL)
        put("google", google)
        put("code", code)
        put("referrals", referrals)
        put("referralCap", referralCap)
        put("redeemed", redeemed)
        put("backupAt", backupAt ?: JSONObject.NULL)
    }

    companion object {
        /** Reads what the server sent. Anything it left out reads as the least it could mean. */
        fun fromJson(o: JSONObject): Account = Account(
            username = if (o.isNull("username")) null else o.optString("username").ifEmpty { null },
            google = o.optBoolean("google", false),
            code = o.optString("code"),
            referrals = o.optInt("referrals", 0).coerceAtLeast(0),
            referralCap = o.optInt("referralCap", 0).coerceAtLeast(0),
            redeemed = o.optBoolean("redeemed", false),
            backupAt = if (o.isNull("backupAt")) null else o.optLong("backupAt").takeIf { it > 0 },
        )
    }
}

/** A sign-in: the key the server gave this phone, and the account it opens. */
data class SignedIn(val token: String, val account: Account, /** True when signing in with Google made the account. */ val created: Boolean = false)

/** The backup kept in an account, exactly as the app sent it, and when. */
data class CloudBackup(val text: String, val savedAt: Long)

/** A Look on the server, as the Featured list names it: its code and its name. */
data class SharedLook(val code: String, val name: String) {
    /** How the code is written for people: LOOK- and the six characters. */
    val shown: String get() = "LOOK-$code"
}

/** What came of asking the server for something. */
sealed interface Reply<out T> {
    data class Ok<T>(val value: T) : Reply<T>

    /**
     * The server answered, and the answer is no. [error] is its one word for why, which the app
     * can act on; [message] is a sentence a person can read.
     */
    data class Refused(val status: Int, val error: String, val message: String) : Reply<Nothing> {
        /** The sign-in this phone holds is no good any more: the account was deleted, or it was left unused too long. */
        val signedOut: Boolean get() = status == 401 && error == "signed_out"
    }

    /** There was no answer to speak of: no connection, nothing at the other end, or something that is not Zenith's server. */
    data class Unreachable(val message: String = NO_ANSWER) : Reply<Nothing>

    companion object {
        const val NO_ANSWER = "Could not reach Zenith's server. Check your connection and try again."
        const val NOT_SECURE = "This build's server address is not secure, so nothing was sent."
        const val NO_SERVER = "This build of Zenith has no server."
    }
}

/** What to tell a person when a reply is not a yes; null when it is. */
fun Reply<*>.problem(): String? = when (this) {
    is Reply.Ok -> null
    is Reply.Refused -> message.ifBlank { "The server said no." }
    is Reply.Unreachable -> message
}
