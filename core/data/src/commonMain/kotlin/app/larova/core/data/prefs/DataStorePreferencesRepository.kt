package app.larova.core.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.core.okio.OkioStorage
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferencesSerializer
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import app.larova.core.domain.model.AppearanceSetting
import app.larova.core.domain.repository.PreferencesRepository
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

    private companion object {
        /** The stored key, not the enum name. Renaming the enum must not reset anyone's setting. */
        val APPEARANCE = stringPreferencesKey("appearance")
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
