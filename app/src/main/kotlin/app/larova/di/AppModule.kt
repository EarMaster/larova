package app.larova.di

import app.larova.AppViewModel
import app.larova.core.data.db.LarovaDatabase
import app.larova.core.data.db.createLarovaDatabase
import app.larova.core.data.prefs.DataStorePreferencesRepository
import app.larova.core.data.prefs.createPreferencesDataStore
import app.larova.core.data.repository.RoomBoardRepository
import app.larova.core.data.repository.RoomCardRepository
import app.larova.core.data.repository.RoomLogRepository
import app.larova.core.data.repository.RoomMediaRepository
import app.larova.core.domain.repository.BoardRepository
import app.larova.core.domain.repository.CardRepository
import app.larova.core.domain.repository.LogRepository
import app.larova.core.domain.repository.MediaRepository
import app.larova.core.domain.repository.PreferencesRepository
import app.larova.core.platform.AndroidPlatformPaths
import app.larova.core.platform.PlatformNames
import app.larova.core.platform.PlatformPaths
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * The composition root.
 *
 * Wiring lives here rather than in `:core:data` on purpose: the data layer should not have to know
 * that a `Context` exists, and iOS will bring a second root beside this one rather than a
 * dependency-injection framework threaded through the shared modules.
 *
 * Everything is a singleton because everything here is either a file handle or stateless. The
 * database in particular must not be opened twice.
 */
val appModule = module {

    single<PlatformPaths> { AndroidPlatformPaths(androidContext()) }

    single<LarovaDatabase> { createLarovaDatabase(androidContext()) }
    single { get<LarovaDatabase>().boardDao }
    single { get<LarovaDatabase>().cardDao }
    single { get<LarovaDatabase>().mediaDao }
    single { get<LarovaDatabase>().logDao }

    single<BoardRepository> { RoomBoardRepository(get()) }
    single<CardRepository> { RoomCardRepository(get()) }
    single<MediaRepository> { RoomMediaRepository(get(), get()) }
    single<LogRepository> { RoomLogRepository(get()) }

    single<PreferencesRepository> {
        DataStorePreferencesRepository(
            createPreferencesDataStore(get<PlatformPaths>().preferencesFile(PlatformNames.PREFERENCES)),
        )
    }

    viewModel { AppViewModel(get()) }
}
