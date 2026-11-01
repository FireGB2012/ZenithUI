package com.gabriel.zenithui.data.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

/**
 * Zenith's server, as the app sees it: accounts, one backup for each, and Aura codes. See
 * `server/zenith_server.py` for the other end and `specs/2026-10-04-zenith-server-design.md` for
 * what the two say to each other.
 *
 * Nothing here is called until somebody makes an account or signs in: a phone without an account
 * never sends anything. Every call answers with a [Reply] and never throws.
 */
class ZenithServer(
    address: String,
    /** Only a test build may talk to a server that is not HTTPS, and only to one on the test PC. */
    private val allowPlainHttp: Boolean = false,
) {
    private val base = address.trim().trimEnd('/')

    /** False when this build was made with no server address: there is nothing to talk to. */
    val isSetUp: Boolean get() = base.isNotEmpty()

    /** Whether the server is there, and whether it can check a Google sign-in. */
    suspend fun health(): Reply<Boolean> = call("GET", "/v1/health").json { it.optBoolean("google", false) }

    /** Makes an account and signs in to it. [code] is the Aura code this phone already shows; it is kept if nobody else has it. */
    suspend fun createAccount(username: String, password: String, code: String): Reply<SignedIn> =
        call("POST", "/v1/account", body = JSONObject().put("username", username).put("password", password).put("code", code))
            .json(::signedIn)

    suspend fun signIn(username: String, password: String): Reply<SignedIn> =
        call("POST", "/v1/session", body = JSONObject().put("username", username).put("password", password)).json(::signedIn)

    /** Signs in with what Google handed the phone, making the account the first time. */
    suspend fun signInWithGoogle(idToken: String, code: String): Reply<SignedIn> =
        call("POST", "/v1/session/google", body = JSONObject().put("idToken", idToken).put("code", code)).json(::signedIn)

    suspend fun signOut(token: String): Reply<Unit> = call("DELETE", "/v1/session", token).unit()

    suspend fun me(token: String): Reply<Account> = call("GET", "/v1/me", token).json { Account.fromJson(it.getJSONObject("account")) }

    /** Deletes the account and all that is kept with it. A password account has to give its password again. */
    suspend fun deleteAccount(token: String, password: String?): Reply<Unit> =
        call("DELETE", "/v1/account", token, body = if (password == null) null else JSONObject().put("password", password)).unit()

    /** Keeps [backup] in the account, in place of the last one. Answers with when it was saved. */
    suspend fun saveBackup(token: String, backup: String): Reply<Long> =
        call("PUT", "/v1/backup", token, text = backup).json { it.getLong("savedAt") }

    suspend fun loadBackup(token: String): Reply<CloudBackup> = when (val answer = call("GET", "/v1/backup", token)) {
        is Answer.Got -> when {
            answer.status == 200 -> Reply.Ok(CloudBackup(answer.text, answer.savedAt ?: 0L))
            else -> answer.refusal()
        }
        is Answer.Lost -> Reply.Unreachable(answer.message)
    }

    /** Uses a friend's code. Answers with the account as it stands afterwards. */
    suspend fun redeem(token: String, code: String): Reply<Account> =
        call("POST", "/v1/aura/redeem", token, body = JSONObject().put("code", code)).json { Account.fromJson(it.getJSONObject("account")) }

    /** Shares [look], a Look as JSON with nothing personal in it, from the account. Answers with its code. */
    suspend fun shareLook(token: String, look: String): Reply<String> =
        call("POST", "/v1/looks", token, text = look).json { it.getString("code") }

    /** A Look somebody shared, by its code, as JSON. Needs no account. */
    suspend fun openLook(code: String): Reply<String> =
        when (val answer = call("GET", "/v1/look?code=" + java.net.URLEncoder.encode(code, "UTF-8"))) {
            is Answer.Got -> if (answer.status == 200) Reply.Ok(answer.text) else answer.refusal()
            is Answer.Lost -> Reply.Unreachable(answer.message)
        }

    /** Takes back a Look this account shared: its code opens nothing any more. */
    suspend fun deleteLook(token: String, code: String): Reply<Unit> =
        call("DELETE", "/v1/look?code=" + java.net.URLEncoder.encode(code, "UTF-8"), token).unit()

    /** The Looks the server's keeper chose to show everybody. */
    suspend fun featuredLooks(): Reply<List<SharedLook>> = call("GET", "/v1/looks/featured").json { o ->
        val list = o.getJSONArray("looks")
        (0 until list.length()).map { i -> list.getJSONObject(i).let { SharedLook(it.getString("code"), it.optString("name", "A Look")) } }
    }

    // ------------------------------------------------------------------------- plumbing

    private fun signedIn(o: JSONObject) = SignedIn(o.getString("token"), Account.fromJson(o.getJSONObject("account")), o.optBoolean("created", false))

    private sealed interface Answer {
        class Got(val status: Int, val text: String, val savedAt: Long?) : Answer
        class Lost(val message: String) : Answer
    }

    private fun Answer.Got.refusal(): Reply<Nothing> {
        val said = runCatching { JSONObject(text) }.getOrNull()
        val error = said?.optString("error").orEmpty()
        // No word for why: this did not come from Zenith's server but from something in front of
        // it, or in its place. That is the server being out of reach, not the server saying no.
        if (error.isEmpty()) return Reply.Unreachable()
        return Reply.Refused(status, error, said?.optString("message").orEmpty())
    }

    private fun <T> Answer.json(read: (JSONObject) -> T): Reply<T> = when (this) {
        is Answer.Lost -> Reply.Unreachable(message)
        is Answer.Got -> if (status in 200..299) {
            runCatching { Reply.Ok(read(JSONObject(text))) }.getOrElse { Reply.Unreachable() }
        } else {
            refusal()
        }
    }

    private fun Answer.unit(): Reply<Unit> = when (this) {
        is Answer.Lost -> Reply.Unreachable(message)
        is Answer.Got -> if (status in 200..299) Reply.Ok(Unit) else refusal()
    }

    private suspend fun call(method: String, path: String, token: String? = null, body: JSONObject? = null, text: String? = null): Answer =
        withContext(Dispatchers.IO) {
            if (!isSetUp) return@withContext Answer.Lost(Reply.NO_SERVER)
            if (!base.startsWith("https://") && !(allowPlainHttp && base.startsWith("http://"))) {
                return@withContext Answer.Lost(Reply.NOT_SECURE)
            }
            val sending = (body?.toString() ?: text)?.toByteArray(Charsets.UTF_8)
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(base + path).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = CONNECT_MS
                    readTimeout = READ_MS
                    instanceFollowRedirects = false
                    useCaches = false
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("User-Agent", "ZenithUI")
                    if (token != null) setRequestProperty("Authorization", "Bearer $token")
                    if (sending != null) {
                        doOutput = true
                        setRequestProperty("Content-Type", "application/json; charset=utf-8")
                        // Sent in one piece, not streamed: a streamed request that is answered
                        // "not signed in" is thrown away by Java with the answer still unread.
                        outputStream.use { it.write(sending) }
                    }
                }
                val status = connection.responseCode
                val stream = if (status >= 400) connection.errorStream else connection.inputStream
                val bytes = stream?.use { it.readBounded(MAX_ANSWER) } ?: ByteArray(0)
                Answer.Got(status, bytes.toString(Charsets.UTF_8), connection.getHeaderField("X-Zenith-Saved-At")?.toLongOrNull())
            } catch (_: IOException) {
                Answer.Lost(Reply.NO_ANSWER)
            } catch (_: SecurityException) {
                // Android refusing the connection outright: no permission, or plain HTTP to somewhere it will not allow.
                Answer.Lost(Reply.NO_ANSWER)
            } catch (_: IllegalArgumentException) {
                // An address that is not an address.
                Answer.Lost(Reply.NO_ANSWER)
            } finally {
                connection?.disconnect()
            }
        }

    private companion object {
        const val CONNECT_MS = 10_000
        const val READ_MS = 20_000

        /** A backup is at most half a megabyte; nothing the server says is longer than that. */
        const val MAX_ANSWER = 1024 * 1024

        /** Reads to the end, or throws if whatever is on the other end sends more than it ever should. */
        fun java.io.InputStream.readBounded(limit: Int): ByteArray {
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val read = read(buffer)
                if (read < 0) break
                out.write(buffer, 0, read)
                if (out.size() > limit) throw IOException("answer too long")
            }
            return out.toByteArray()
        }
    }
}
