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
import app.larova.core.domain.usecase.DeleteCard
import app.larova.core.domain.usecase.EnsureRootBoard
import app.larova.core.domain.usecase.ObserveHomeTiles
import app.larova.core.domain.usecase.ObserveTile
import app.larova.core.domain.usecase.ReorderTiles
import app.larova.core.domain.usecase.SaveCard
import app.larova.core.domain.usecase.SearchTiles
import app.larova.core.domain.usecase.ToggleChecklistItem
import app.larova.core.platform.AndroidExternalActions
import app.larova.core.platform.AndroidPlatformPaths
import app.larova.core.platform.ExternalActions
import app.larova.core.platform.PlatformNames
import app.larova.core.platform.PlatformPaths
import app.larova.feature.card.CardViewModel
import app.larova.feature.card.edit.EditCardViewModel
import app.larova.feature.home.ArrangeTilesViewModel
import app.larova.feature.home.HomeViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
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
    single<ExternalActions> { AndroidExternalActions(androidContext()) }

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

    // Use cases are factories rather than singletons: each one is a couple of fields around a
    // repository, and none of them holds state worth sharing.
    factory { EnsureRootBoard(get()) }
    factory { ObserveHomeTiles(get(), get()) }
    factory { ObserveTile(get()) }
    factory { ToggleChecklistItem(get()) }
    factory { SaveCard(get(), get()) }
    factory { DeleteCard(get()) }
    factory { SearchTiles(get()) }
    factory { ReorderTiles(get(), get()) }

    viewModel { AppViewModel(get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { ArrangeTilesViewModel(get(), get()) }
    // The card id comes from the navigation route, so it is passed in rather than injected.
    viewModel { parameters -> CardViewModel(parameters.get(), get(), get()) }
    viewModel { parameters -> EditCardViewModel(parameters.get(), get(), get(), get()) }
}

/** Kept next to the module so a caller cannot get the parameter order wrong. */
fun cardViewModelParameters(cardId: String) = parametersOf(cardId)

/** An empty id is a new tile; the editor treats it as such rather than looking one up. */
fun editCardViewModelParameters(cardId: String) = parametersOf(cardId)
