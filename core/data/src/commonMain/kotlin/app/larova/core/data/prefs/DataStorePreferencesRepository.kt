package app.larova.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import app.larova.core.domain.model.AppearanceSetting
import app.larova.core.domain.model.LastBackup
import app.larova.core.domain.repository.PreferencesRepository
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okio.FileSystem
import okio.Path.Companion.toPath

/**
 * Settings, kept out of the database on purpose: appearance, retention and the parent-view PIN are
 * not content, and none of them belong in an export of a family's tiles.
 *
 * A stored value that cannot be understood falls back to the default rather than throwing. The
 * appearance setting is read before the first frame, and an app that refuses to start because a
 * preferences file is odd is worse than an app that starts in light mode.
 */
class DataStorePreferencesRepository(
    private val dataStore: DataStore<Preferences>,
) : PreferencesRepository {

    override fun observeAppearance(): Flow<AppearanceSetting> =
        dataStore.data.map { AppearanceSetting.fromKey(it[APPEARANCE]) }

    override suspend fun setAppearance(setting: AppearanceSetting) {
        dataStore.edit { it[APPEARANCE] = setting.key }
    }

    /**
     * Three keys rather than one encoded string. The timestamp is the part that has to survive an
     * app that gains a fourth field later, and a half-written record with no time in it is simply
     * "never backed up" — which is the safe thing for this to say when it is unsure.
     */
    override fun observeLastBackup(): Flow<LastBackup?> = dataStore.data.map { prefs ->
        prefs[LAST_BACKUP_AT]?.let { millis ->
            LastBackup(
                at = Instant.fromEpochMilliseconds(millis),
                cards = prefs[LAST_BACKUP_CARDS] ?: 0,
                media = prefs[LAST_BACKUP_MEDIA] ?: 0,
            )
        }
    }

    override suspend fun setLastBackup(backup: LastBackup) {
        dataStore.edit {
            it[LAST_BACKUP_AT] = backup.at.toEpochMilliseconds()
            it[LAST_BACKUP_CARDS] = backup.cards
            it[LAST_BACKUP_MEDIA] = backup.media
        }
    }

    override fun observeSupportCount(): Flow<Int> =
        dataStore.data.map { it[SUPPORT_COUNT] ?: 0 }

    /**
     * Read-then-write inside one `edit`, which DataStore serialises — two taps in quick succession
     * therefore count as two rather than racing to the same number.
     */
    override suspend fun addSupport() {
        dataStore.edit { it[SUPPORT_COUNT] = (it[SUPPORT_COUNT] ?: 0) + 1 }
    }

    private companion object {
        /** The stored key, not the enum name. Renaming the enum must not reset anyone's setting. */
        val APPEARANCE = stringPreferencesKey("appearance")
        val LAST_BACKUP_AT = longPreferencesKey("lastBackupAt")
        val LAST_BACKUP_CARDS = intPreferencesKey("lastBackupCards")
        val LAST_BACKUP_MEDIA = intPreferencesKey("lastBackupMedia")
        val SUPPORT_COUNT = intPreferencesKey("supportCount")
    }
}

/**
 * Built from a path rather than a platform handle, so the same call works on both platforms — the
 * path itself comes from `:core:platform`.
 */
fun createPreferencesDataStore(path: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        storage = OkioStorage(
            fileSystem = FileSystem.SYSTEM,
            serializer = PreferencesSerializer,
            producePath = { path.toPath() },
        ),
    )
