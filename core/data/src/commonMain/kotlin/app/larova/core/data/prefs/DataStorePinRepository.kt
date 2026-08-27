package app.larova.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.larova.core.domain.repository.PinRepository
import app.larova.core.platform.PasswordHasher
import kotlinx.coroutines.flow.first

/**
 * The PIN hash, kept next to the other settings and out of the database.
 *
 * It is not content: it does not belong in an export, and a package sent to a grandparent must not
 * carry the parents' PIN with it. Keeping it in the preferences file rather than in a table is what
 * makes that true by construction rather than by remembering to exclude it.
 *
 * What is stored is the tagged hash and nothing else. Verifying happens here so that no caller ever
 * holds both the PIN and the hash, and there is deliberately no way to read the PIN back.
 *
 * Not yet wrapped in a Keystore key, which docs/technical-notes.md §8 asks for. App-private
 * storage is encrypted at rest from Android 10, so the gap this leaves is an attacker with a
 * rooted device — worth closing in the hardening pass, not worth blocking parent view on.
 */
class DataStorePinRepository(
    private val dataStore: DataStore<Preferences>,
    private val hasher: PasswordHasher,
) : PinRepository {

    override suspend fun hasPin(): Boolean = stored() != null

    override suspend fun setPin(pin: String) {
        val hash = hasher.hash(pin)
        dataStore.edit { it[PIN_HASH] = hash }
    }

    override suspend fun verify(pin: String): Boolean {
        val stored = stored() ?: return false
        return hasher.verify(pin, stored)
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(PIN_HASH) }
    }

    private suspend fun stored(): String? =
        dataStore.data.first()[PIN_HASH]?.takeIf { it.isNotEmpty() }

    private companion object {
        val PIN_HASH = stringPreferencesKey("parent_pin_hash")
    }
}
