package com.gabriel.zenithui.data.server

import com.gabriel.zenithui.data.Aura
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.AfterClass
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.BeforeClass
import org.junit.Test
import java.io.File
import java.net.ServerSocket
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * The app's side against the real server: `server/zenith_server.py` is started on a spare port
 * with an empty database, and the app's own client talks to it. If the two ever stop
 * understanding each other, this is where it shows.
 *
 * Needs Python on this machine; where there is none, the tests that need the server are skipped.
 */
class ZenithServerTest {

    private fun <T> Reply<T>.yes(): T = (this as? Reply.Ok)?.value ?: error("expected a yes, got $this")
    private fun Reply<*>.no(): Reply.Refused = this as? Reply.Refused ?: error("expected a no, got $this")

    @Test
    fun `an account is read as the server writes it`() {
        val account = Account.fromJson(
            JSONObject("""{"username":"gabriel","google":false,"code":"WTR4RT","referrals":3,"referralCap":10,"redeemed":true,"backupAt":1791132740824}""")
        )
        assertEquals(Account("gabriel", false, "WTR4RT", 3, 10, true, 1791132740824), account)
        // And survives being kept on the phone and read back.
        assertEquals(account, Account.fromJson(JSONObject(account.toJson().toString())))
    }

    @Test
    fun `a Google account has no username and a new account no backup`() {
        val account = Account.fromJson(JSONObject("""{"username":null,"google":true,"code":"BCDFGH","referrals":0,"referralCap":10,"redeemed":false,"backupAt":null}"""))
        assertNull(account.username)
        assertTrue(account.google)
        assertNull(account.backupAt)
        assertEquals(account, Account.fromJson(JSONObject(account.toJson().toString())))
    }

    @Test
    fun `what the server leaves out reads as the least it could mean`() {
        assertEquals(Account(null, false, "", 0, 0, false, null), Account.fromJson(JSONObject("{}")))
        assertEquals(0, Account.fromJson(JSONObject("""{"referrals":-4}""")).referrals)
    }

    @Test
    fun `a code pays its owner once for each friend who has not been paid for`() {
        assertEquals(0, Aura.referralPayout(referrals = 0, paid = 0))
        assertEquals(Aura.REFERRAL_REWARD, Aura.referralPayout(referrals = 1, paid = 0))
        assertEquals(2 * Aura.REFERRAL_REWARD, Aura.referralPayout(referrals = 5, paid = 3))
        assertEquals(0, Aura.referralPayout(referrals = 5, paid = 5))
        // The count can only have gone down if a backup from a later day was restored: nothing is taken back.
        assertEquals(0, Aura.referralPayout(referrals = 2, paid = 5))
    }

    @Test
    fun `a build with no server sends nothing and says so`() = runBlocking {
        val nowhere = ZenithServer("")
        assertFalse(nowhere.isSetUp)
        assertEquals(Reply.Unreachable(Reply.NO_SERVER), nowhere.health())
        assertEquals(Reply.NO_SERVER, nowhere.signIn("gabriel", "password-1").problem())
    }

    @Test
    fun `only a test build may talk to a server that is not secure`() = runBlocking {
        assertEquals(Reply.Unreachable(Reply.NOT_SECURE), ZenithServer("http://127.0.0.1:9").signIn("gabriel", "password-1"))
        assertEquals(Reply.Unreachable(Reply.NOT_SECURE), ZenithServer("ftp://example.com", allowPlainHttp = true).health())
    }

    @Test
    fun `a server that is not there is out of reach, not an error`() = runBlocking {
        // Nothing listens on the discard port.
        val gone = ZenithServer("http://127.0.0.1:9", allowPlainHttp = true)
        assertEquals(Reply.Unreachable(Reply.NO_ANSWER), gone.health())
        assertEquals(Reply.NO_ANSWER, gone.me("token").problem())
        assertEquals(Reply.Unreachable(Reply.NO_ANSWER), ZenithServer("http://not a host", allowPlainHttp = true).health())
    }

    @Test
    fun `the app and the server understand each other`() = runBlocking {
        assumeTrue("Python is needed to run the server", address != null)
        val server = ZenithServer(address!!, allowPlainHttp = true)
        // No Google client ID was given to this server.
        assertEquals(false, server.health().yes())

        // An account, with the code this phone already shows.
        val mine = server.createAccount("gabriel", "password-1", "WTR4RT").yes()
        assertEquals(Account("gabriel", false, "WTR4RT", 0, 10, false, null), mine.account)
        assertEquals(mine.account, server.me(mine.token).yes())

        // Things the server says no to, in words the app can act on.
        assertEquals("taken", server.createAccount("gabriel", "password-2", "BCDFGH").no().error)
        assertEquals("bad_username", server.createAccount("No Good", "password-1", "BCDFGH").no().error)
        assertEquals("wrong", server.signIn("gabriel", "not-the-password").no().error)
        assertTrue(server.me("made-up").no().signedOut)
        assertEquals("no_google", server.signInWithGoogle("not-a-token", "BCDFGH").no().error)

        // A second phone signs in to the same account.
        val second = server.signIn("gabriel", "password-1").yes()
        assertEquals(mine.account, second.account)

        // A backup, with letters that are not ASCII, goes up and comes back as it went.
        val backup = """{"format":"zenith-ui-backup","version":1,"settings":{"userName":"Gabriel ÆØÅ ✨","note":"line\nbreak"},"layout":[]}"""
        assertEquals("no_backup", server.loadBackup(mine.token).no().error)
        val savedAt = server.saveBackup(mine.token, backup).yes()
        assertTrue(savedAt > 1_700_000_000_000)
        assertEquals(CloudBackup(backup, savedAt), server.loadBackup(second.token).yes())
        assertEquals(savedAt, server.me(mine.token).yes().backupAt)
        assertEquals("not_backup", server.saveBackup(mine.token, """{"format":"something else"}""").no().error)

        // A friend uses the code: once, and not their own.
        val friend = server.createAccount("a_friend", "password-3", "WTR4RT").yes()
        assertTrue("a code already taken is replaced", friend.account.code != "WTR4RT")
        assertEquals("own_code", server.redeem(friend.token, friend.account.code).no().error)
        assertEquals("unknown_code", server.redeem(friend.token, "ZZZZZZ").no().error)
        assertTrue(server.redeem(friend.token, "wtr4rt").yes().redeemed)
        assertEquals("already", server.redeem(friend.token, "WTR4RT").no().error)
        val paid = server.me(mine.token).yes()
        assertEquals(1, paid.referrals)
        assertEquals(Aura.REFERRAL_REWARD, Aura.referralPayout(paid.referrals, paid = 0))

        // Signing out ends that phone's sign-in and no other.
        server.signOut(second.token).yes()
        assertTrue(server.me(second.token).no().signedOut)
        assertEquals("gabriel", server.me(mine.token).yes().username)

        // Deleting takes the password, and then everything.
        assertEquals("wrong", server.deleteAccount(mine.token, "not-the-password").no().error)
        assertEquals("wrong", server.deleteAccount(mine.token, null).no().error)
        server.deleteAccount(mine.token, "password-1").yes()
        assertTrue(server.me(mine.token).no().signedOut)
        assertEquals("wrong", server.signIn("gabriel", "password-1").no().error)
        // The friend keeps their account, and stays someone who has used a code.
        assertTrue(server.me(friend.token).yes().redeemed)
    }

    @Test
    fun `something that is not Zenith's server is out of reach, not a refusal`() = runBlocking {
        assumeTrue("Python is needed to run the server", address != null)
        // The front page is HTML: the kind of thing a web server answers with when the program behind it is down.
        val notTheApi = ZenithServer("$address/delete-account#", allowPlainHttp = true)
        assertEquals(Reply.Unreachable(), notTheApi.health())
    }

    companion object {
        private var process: Process? = null
        private var database: File? = null

        /** Where the server is listening, or null when there is no Python to run it with. */
        private var address: String? = null

        @JvmStatic
        @BeforeClass
        fun startServer() {
            // Unit tests run in the app module's folder; the server is beside it.
            val program = listOf(File("../server/zenith_server.py"), File("server/zenith_server.py")).firstOrNull { it.exists() } ?: return
            val python = listOf("python", "python3", "py").firstOrNull { name ->
                runCatching { ProcessBuilder(name, "--version").redirectErrorStream(true).start().waitFor(10, TimeUnit.SECONDS) }.getOrDefault(false)
            } ?: return
            val port = ServerSocket(0).use { it.localPort }
            val file = File.createTempFile("zenith-contract-", ".db").also { it.delete() }
            val log = File(file.path + ".log")
            val started = ProcessBuilder(python, program.absolutePath)
                .redirectErrorStream(true)
                .redirectOutput(log)
                .apply {
                    environment()["ZENITH_PORT"] = port.toString()
                    environment()["ZENITH_HOST"] = "127.0.0.1"
                    environment()["ZENITH_DB"] = file.absolutePath
                    environment().remove("ZENITH_GOOGLE_CLIENT_ID")
                }
                .start()
            process = started
            database = file
            val base = "http://127.0.0.1:$port"
            repeat(100) {
                val up = runCatching { URL("$base/v1/health").openStream().use { it.readBytes() }.isNotEmpty() }.getOrDefault(false)
                if (up) {
                    address = base
                    return
                }
                if (!started.isAlive) return
                Thread.sleep(100)
            }
        }

        @JvmStatic
        @AfterClass
        fun stopServer() {
            process?.destroy()
            process?.waitFor(5, TimeUnit.SECONDS)
            process?.destroyForcibly()
            database?.let { file -> listOf("", "-wal", "-shm", ".log").forEach { File(file.path + it).delete() } }
        }
    }
}
