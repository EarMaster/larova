package app.larova.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.larova.core.domain.model.Receipt
import app.larova.core.domain.repository.EntitlementCache
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The evidence for a paid unlock, kept next to the other settings and out of the database.
 *
 * Same reasoning as the PIN hash one file over: it is not content. A backup handed to a grandparent
 * carries a family's tiles and must not carry the parents' purchase, and keeping this in the
 * preferences file rather than in a table is what makes that true by construction rather than by
 * remembering to exclude it from `ExportContent`.
 *
 * Two string keys rather than one encoded pair. A half-written record is then simply "nothing
 * stored" — which is the safe thing for this to say when it is unsure — instead of a payload whose
 * signature no longer matches it, which would look like tampering rather than like an interrupted
 * write.
 *
 * What is stored is the receipt as the store handed it over, never a boolean. `true` in a
 * preferences file is a claim anybody with a rooted phone can write; a signed payload is one they
 * would have to forge. It is checked on the way out every time, by whoever knows the public key.
 */
class DataStoreEntitlementCache(
    private val dataStore: DataStore<Preferences>,
) : EntitlementCache {

    override fun observe(): Flow<Receipt?> = dataStore.data.map { prefs ->
        val payload = prefs[RECEIPT_PAYLOAD]?.takeIf { it.isNotEmpty() }
        val signature = prefs[RECEIPT_SIGNATURE]?.takeIf { it.isNotEmpty() }
        if (payload == null || signature == null) null else Receipt(payload, signature)
    }

    override suspend fun write(receipt: Receipt) {
        dataStore.edit {
            it[RECEIPT_PAYLOAD] = receipt.payload
            it[RECEIPT_SIGNATURE] = receipt.signature
        }
    }

    override suspend fun clear() {
        dataStore.edit {
            it.remove(RECEIPT_PAYLOAD)
            it.remove(RECEIPT_SIGNATURE)
        }
    }

    private companion object {
        /** Stored keys, not property names. Renaming anything here must not un-buy an unlock. */
        val RECEIPT_PAYLOAD = stringPreferencesKey("unlockReceipt")
        val RECEIPT_SIGNATURE = stringPreferencesKey("unlockSignature")
    }
}
