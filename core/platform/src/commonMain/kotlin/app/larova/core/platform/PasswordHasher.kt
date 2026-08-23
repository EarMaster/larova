package app.larova.core.platform

/**
 * Turns a secret into something safe to store, and checks one against it.
 *
 * The algorithm belongs to the platform: Android has Argon2 through a native binding, iOS will use
 * CryptoKit. What both sides must agree on is the *shape* of the stored string — see
 * [PasswordHash] — because a hash written by one and read by the other is exactly the situation an
 * export between two phones creates.
 *
 * A PIN is only four to twelve digits, so no amount of stretching makes it strong against someone
 * with the stored hash and time. What stretching buys is that the hash on its own is not the PIN,
 * and that trying every possibility is slow rather than instant.
 */
interface PasswordHasher {

    /** Hashes with a fresh random salt and returns the whole tagged string. */
    fun hash(secret: String): String

    /**
     * Checks [secret] against a string produced by [hash]. False for anything unparseable — a
     * corrupted or truncated hash must not be an exception on the unlock screen, and must never
     * accidentally pass.
     */
    fun verify(secret: String, stored: String): Boolean
}

/**
 * The stored format: `algorithm$parameters$salt$hash`, all of it in the string.
 *
 * The algorithm tag is the whole point. It means the parameters can be raised, or the algorithm
 * replaced, without anyone being locked out: an old hash still says how to check against it, and
 * the next successful unlock is the moment to rewrite it in the newer form.
 */
object PasswordHash {
    const val SEPARATOR = "$"
    const val ARGON2ID = "argon2id"
}
