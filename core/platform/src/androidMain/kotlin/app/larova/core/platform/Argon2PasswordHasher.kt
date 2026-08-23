package app.larova.core.platform

import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import java.security.SecureRandom
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Argon2id, as docs/technical-notes.md §8 specifies.
 *
 * Argon2 is memory-hard, which is what makes it worth a native dependency: raising the iteration
 * count of a hash that fits in cache buys much less than forcing 32 MB of memory per attempt. The
 * same primitive is what M3 needs for password-protected exports, so it arrives once and is used
 * twice.
 *
 * The parameters are stored alongside each hash rather than assumed, so they can be raised later
 * without locking anyone out of their own installation.
 */
@OptIn(ExperimentalEncodingApi::class)
class Argon2PasswordHasher(
    private val argon2: Argon2Kt = Argon2Kt(),
    private val random: SecureRandom = SecureRandom(),
) : PasswordHasher {

    override fun hash(secret: String): String {
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val encoded = derive(secret, salt)
        return listOf(
            PasswordHash.ARGON2ID,
            "$ITERATIONS,$MEMORY_KIB,$PARALLELISM",
            Base64.encode(salt),
            encoded,
        ).joinToString(PasswordHash.SEPARATOR)
    }

    override fun verify(secret: String, stored: String): Boolean {
        val parts = stored.split(PasswordHash.SEPARATOR)
        if (parts.size != PART_COUNT) return false
        // Named, because a bare parts[2] is the sort of thing that gets swapped in a refactor
        // and still compiles.
        val algorithm = parts.first()
        val parameters = parts.elementAt(1)
        val encodedSalt = parts.elementAt(2)
        val encodedHash = parts.last()

        val cost = parseParameters(parameters)
        val salt = decodeOrNull(encodedSalt)
        if (algorithm != PasswordHash.ARGON2ID || cost == null || salt == null) return false

        val candidate = derive(
            secret = secret,
            salt = salt,
            iterations = cost.iterations,
            memoryKib = cost.memoryKib,
            parallelism = cost.parallelism,
        )
        // Constant-time: an attacker who can measure the comparison learns nothing about how much
        // of the hash matched. Cheap here, and the habit is worth keeping for the export password.
        return constantTimeEquals(candidate, encodedHash)
    }

    /** The parameters a stored hash was made with, which is what allows them to be raised later. */
    private data class Cost(val iterations: Int, val memoryKib: Int, val parallelism: Int)

    private fun derive(
        secret: String,
        salt: ByteArray,
        iterations: Int = ITERATIONS,
        memoryKib: Int = MEMORY_KIB,
        parallelism: Int = PARALLELISM,
    ): String = argon2.hash(
        mode = Argon2Mode.ARGON2_ID,
        password = secret.encodeToByteArray(),
        salt = salt,
        tCostInIterations = iterations,
        mCostInKibibyte = memoryKib,
        parallelism = parallelism,
        hashLengthInBytes = HASH_BYTES,
    ).rawHashAsHexadecimal()

    private fun parseParameters(raw: String): Cost? {
        val values = raw.split(",").mapNotNull { it.toIntOrNull() }
        if (values.size != PARAMETER_COUNT) return null
        val (iterations, memoryKib, parallelism) = values
        return Cost(iterations = iterations, memoryKib = memoryKib, parallelism = parallelism)
    }

    private fun decodeOrNull(raw: String): ByteArray? = try {
        Base64.decode(raw)
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun constantTimeEquals(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var difference = 0
        for (i in a.indices) difference = difference or (a[i].code xor b[i].code)
        return difference == 0
    }

    private companion object {
        /**
         * Chosen for a phone that may be five years old: a second-ish on the unlock screen is
         * acceptable, a second per frame is not. Raise them together with the tag, never alone.
         */
        const val ITERATIONS = 3
        const val MEMORY_KIB = 32 * 1024
        const val PARALLELISM = 2
        const val HASH_BYTES = 32
        const val SALT_BYTES = 16

        /** algorithm, parameters, salt, hash. */
        const val PART_COUNT = 4

        /** iterations, memory, parallelism. */
        const val PARAMETER_COUNT = 3
    }
}
