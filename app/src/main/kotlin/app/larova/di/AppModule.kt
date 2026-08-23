package app.larova.di

import app.larova.AppViewModel
import app.larova.BuildConfig
import app.larova.core.data.db.LarovaDatabase
import app.larova.core.data.db.createLarovaDatabase
import app.larova.core.data.prefs.DataStorePinRepository
import app.larova.core.data.prefs.DataStorePreferencesRepository
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import app.larova.core.data.prefs.createPreferencesDataStore
import app.larova.core.data.repository.RoomBoardRepository
import app.larova.core.data.repository.RoomCardRepository
import app.larova.core.data.repository.RoomLogRepository
import app.larova.core.data.repository.RoomMediaRepository
import app.larova.core.domain.repository.BoardRepository
import app.larova.core.domain.repository.CardRepository
import app.larova.core.domain.repository.LogRepository
import app.larova.core.domain.repository.MediaRepository
import app.larova.core.domain.repository.PinRepository
import app.larova.core.domain.repository.PreferencesRepository
import app.larova.core.domain.session.ViewModeSession
import app.larova.core.domain.export.Digest
import app.larova.core.domain.export.PackageIo
import app.larova.core.domain.export.MediaFiles
import app.larova.core.domain.export.PackageStore
import app.larova.core.domain.usecase.DeleteCard
import app.larova.core.domain.usecase.ExportPackage
import app.larova.core.domain.usecase.ImportPackage
import app.larova.core.domain.usecase.ReadPackagePreview
import app.larova.core.domain.usecase.HasPin
import app.larova.core.domain.usecase.LockParentView
import app.larova.core.domain.usecase.EnsureRootBoard
import app.larova.core.domain.usecase.ObserveHelpContacts
import app.larova.core.domain.usecase.ObserveHomeTiles
import app.larova.core.domain.usecase.ObserveTile
import app.larova.core.domain.usecase.ReorderTiles
import app.larova.core.domain.usecase.SaveCard
import app.larova.core.domain.usecase.SearchTiles
import app.larova.core.domain.usecase.SetPin
import app.larova.core.domain.usecase.ToggleChecklistItem
import app.larova.core.domain.usecase.UnlockWithBiometrics
import app.larova.core.domain.usecase.UnlockWithPin
import app.larova.core.platform.AndroidExternalActions
import app.larova.core.platform.AndroidPlatformPaths
import app.larova.core.platform.AndroidDigest
import app.larova.core.platform.AndroidMediaFiles
import app.larova.core.platform.AndroidPackageStore
import app.larova.core.platform.Argon2PasswordHasher
import app.larova.core.platform.ExternalActions
import app.larova.core.platform.PasswordHasher
import app.larova.core.platform.PlatformNames
import app.larova.core.platform.PlatformPaths
import app.larova.feature.card.CardViewModel
import app.larova.feature.card.edit.EditCardViewModel
import app.larova.feature.help.HelpViewModel
import app.larova.feature.home.ArrangeTilesViewModel
import app.larova.feature.home.HomeViewModel
import app.larova.feature.settings.PinSetupViewModel
import app.larova.feature.transfer.TransferViewModel
import app.larova.feature.settings.UnlockViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
    single<PasswordHasher> { Argon2PasswordHasher() }
    single<PackageStore> { AndroidPackageStore(androidContext()) }
    single<Digest> { AndroidDigest() }
    single<MediaFiles> { AndroidMediaFiles(get()) }
    single { PackageIo(store = get(), digest = get(), mediaFiles = get()) }

    // One session for the whole process, with a scope that outlives every screen: the five-minute
    // timer has to keep running while the user is on a screen that knows nothing about it.
    single { ViewModeSession(CoroutineScope(SupervisorJob() + Dispatchers.Default)) }

    single<LarovaDatabase> { createLarovaDatabase(androidContext()) }
    single { get<LarovaDatabase>().boardDao }
    single { get<LarovaDatabase>().cardDao }
    single { get<LarovaDatabase>().mediaDao }
    single { get<LarovaDatabase>().logDao }

    single<BoardRepository> { RoomBoardRepository(get()) }
    single<CardRepository> { RoomCardRepository(get()) }
    single<MediaRepository> { RoomMediaRepository(get(), get()) }
    single<LogRepository> { RoomLogRepository(get()) }

    // One DataStore for the file, shared by everything that reads it. Two instances over the
    // same path is not a tidiness question: DataStore enforces single ownership, and a second one
    // makes writes throw as soon as both are used.
    single<DataStore<Preferences>> {
        createPreferencesDataStore(get<PlatformPaths>().preferencesFile(PlatformNames.PREFERENCES))
    }

    single<PinRepository> { DataStorePinRepository(get(), get()) }

    single<PreferencesRepository> { DataStorePreferencesRepository(get()) }

    // Use cases are factories rather than singletons: each one is a couple of fields around a
    // repository, and none of them holds state worth sharing.
    factory { EnsureRootBoard(get()) }
    factory { ObserveHomeTiles(get(), get()) }
    factory { ObserveTile(get()) }
    factory { ObserveHelpContacts(get()) }
    factory { ToggleChecklistItem(get()) }
    factory { SaveCard(get(), get()) }
    factory { DeleteCard(get()) }
    factory { SearchTiles(get()) }
    factory { ReorderTiles(get(), get()) }
    factory { HasPin(get()) }
    factory { SetPin(get()) }
    factory { UnlockWithPin(get(), get()) }
    factory { UnlockWithBiometrics(get(), get()) }
    factory { LockParentView(get()) }

    // The version in the manifest is the app's own, read from the build rather than written twice.
    factory { ExportPackage(get(), get(), get(), get(), BuildConfig.VERSION_NAME) }
    factory { ReadPackagePreview(get()) }
    factory { ImportPackage(get(), get(), get(), get(), get()) }

    viewModel { AppViewModel(get(), get(), get()) }
    viewModel { UnlockViewModel(get(), get(), get()) }
    viewModel { PinSetupViewModel(get()) }
    viewModel { HomeViewModel(get(), get(), get()) }
    viewModel { ArrangeTilesViewModel(get(), get()) }
    viewModel { HelpViewModel(get()) }
    viewModel { TransferViewModel(get(), get(), get()) }
    // The card id comes from the navigation route, so it is passed in rather than injected.
    viewModel { parameters -> CardViewModel(parameters.get(), get(), get()) }
    viewModel { parameters -> EditCardViewModel(parameters.get(), get(), get(), get()) }
}

/** Kept next to the module so a caller cannot get the parameter order wrong. */
fun cardViewModelParameters(cardId: String) = parametersOf(cardId)

/** An empty id is a new tile; the editor treats it as such rather than looking one up. */
fun editCardViewModelParameters(cardId: String) = parametersOf(cardId)
