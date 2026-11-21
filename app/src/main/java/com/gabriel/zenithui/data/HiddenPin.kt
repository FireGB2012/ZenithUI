package com.gabriel.zenithui.data

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * The PIN on the hidden apps, kept only as a salted hash (PBKDF2): never as it was typed, so it
 * is not in the settings, nor in a backup file, for anybody to read. People use the PIN that
 * opens their phone, or their bank, more often than not.
 *
 * Kept as `pbkdf2:<rounds>:<salt>:<hash>`. A PIN an older Zenith kept as typed (digits only) still
 * opens the list, and is sealed as soon as Zenith starts ([isPlain]).
 */
object HiddenPin {
    private const val PREFIX = "pbkdf2"
    private const val ROUNDS = 100_000
    private const val MOST_ROUNDS = 1_000_000
    private const val SALT_BYTES = 16
    private const val HASH_BITS = 256

    /** The digits of a PIN as it is set: six at most. */
    fun digits(typed: String): String = typed.filter(Char::isDigit).take(6)

    /** [typed] as it is kept; nothing for no PIN. */
    fun seal(typed: String): String {
        val pin = digits(typed)
        if (pin.isEmpty()) return ""
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        val encoder = Base64.getEncoder().withoutPadding()
        return "$PREFIX:$ROUNDS:${encoder.encodeToString(salt)}:${encoder.encodeToString(hash(pin, salt, ROUNDS))}"
    }

    /** Kept as typed, by a Zenith from before the hash. */
    fun isPlain(kept: String): Boolean = kept.isNotEmpty() && kept.all(Char::isDigit)

    /** Whether [typed] is the PIN [kept] keeps. */
    fun matches(typed: String, kept: String): Boolean {
        val pin = digits(typed)
        if (kept.isEmpty()) return pin.isEmpty()
        if (isPlain(kept)) return MessageDigest.isEqual(pin.toByteArray(), kept.toByteArray())
        return runCatching {
            val parts = kept.split(':')
            if (parts.size != 4 || parts[0] != PREFIX || pin.isEmpty()) return false
            val rounds = parts[1].toInt()
            if (rounds !in 1..MOST_ROUNDS) return false
            val decoder = Base64.getDecoder()
            val salt = decoder.decode(parts[2])
            val expected = decoder.decode(parts[3])
            if (salt.isEmpty() || expected.isEmpty()) return false
            MessageDigest.isEqual(hash(pin, salt, rounds), expected)
        }.getOrDefault(false)
    }

    private fun hash(pin: String, salt: ByteArray, rounds: Int): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, rounds, HASH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
