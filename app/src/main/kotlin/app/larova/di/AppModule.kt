package app.larova.di

import app.larova.AndroidShortcuts
import app.larova.AppViewModel
import app.larova.BuildConfig
import app.larova.core.billing.BuildUnlockedEntitlementRepository
import app.larova.core.billing.PlayBilling
import app.larova.core.billing.PlayEntitlementRepository
import app.larova.core.billing.PurchaseVerifier
import app.larova.core.billing.SupportPurchases
import app.larova.core.data.db.LarovaDatabase
import app.larova.core.data.db.createLarovaDatabase
import app.larova.core.data.prefs.DataStoreEntitlementCache
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
import app.larova.core.domain.repository.EntitlementCache
import app.larova.core.domain.repository.EntitlementRepository
import app.larova.core.domain.repository.CardRepository
import app.larova.core.domain.repository.LogRepository
import app.larova.core.domain.repository.MediaRepository
import app.larova.core.domain.repository.PinRepository
import app.larova.core.domain.repository.PreferencesRepository
import app.larova.core.domain.session.ViewModeSession
import app.larova.core.domain.export.Digest
import app.larova.core.domain.media.ImageStore
import app.larova.core.domain.media.AudioRecorder
import app.larova.core.domain.media.MediaIntake
import app.larova.core.domain.export.PackageIo
import app.larova.core.domain.export.MediaFiles
import app.larova.core.domain.export.PackageStore
import app.larova.core.domain.usecase.AddImage
import app.larova.core.domain.usecase.ApplyTemplate
import app.larova.core.domain.usecase.AddMediaFile
import app.larova.core.domain.usecase.FindMediaFile
import app.larova.core.domain.usecase.CleanUpMedia
import app.larova.core.domain.app.InstalledApps
import app.larova.core.domain.app.Shortcuts
import app.larova.core.domain.usecase.Apps
import app.larova.core.domain.usecase.CreateFolderBoard
import app.larova.core.domain.usecase.DeleteCard
import app.larova.core.domain.usecase.Folders
import app.larova.core.domain.usecase.LoadImage
import app.larova.core.domain.usecase.Media
import app.larova.core.domain.usecase.ExportPackage
import app.larova.core.domain.usecase.ObserveEntitlement
import app.larova.core.domain.usecase.ObserveLastBackup
import app.larova.core.domain.app.Translators
import app.larova.core.domain.usecase.CanTranslate
import app.larova.core.domain.usecase.ObserveLockedTypes
import app.larova.core.domain.usecase.Translations
import app.larova.core.platform.AndroidTranslators
import app.larova.core.domain.usecase.ObserveSupportCount
import app.larova.core.domain.usecase.RecordLastBackup
import app.larova.core.domain.usecase.ClearLog
import app.larova.core.domain.usecase.ImportPackage
import app.larova.core.domain.usecase.MostOpenedTiles
import app.larova.core.domain.usecase.ObserveLog
import app.larova.core.domain.usecase.PublishShortcuts
import app.larova.core.domain.usecase.PruneLog
import app.larova.core.domain.usecase.RecordEvent
import app.larova.core.domain.usecase.ReadPackagePreview
import app.larova.core.domain.usecase.Recording
import app.larova.core.domain.usecase.HasPin
import app.larova.core.domain.usecase.IsAppInstalled
import app.larova.core.domain.usecase.PickableApps
import app.larova.core.domain.usecase.LockParentView
import app.larova.core.domain.usecase.EnsureRootBoard
import app.larova.core.domain.usecase.ObserveBoardTiles
import app.larova.core.domain.usecase.ObserveHelpContacts
import app.larova.core.domain.usecase.ObserveHomeTiles
import app.larova.core.domain.usecase.ObserveTile
import app.larova.core.domain.usecase.RecordSupport
import app.larova.core.domain.usecase.RefreshEntitlement
import app.larova.core.domain.usecase.UnlockPrice
import app.larova.core.domain.usecase.ReorderTiles
import app.larova.core.domain.usecase.SaveCard
import app.larova.core.domain.usecase.SearchTiles
import app.larova.core.domain.usecase.SetPin
import app.larova.core.domain.usecase.TileEditing
import app.larova.core.domain.usecase.TileSource
import app.larova.core.domain.usecase.ToggleChecklistItem
import app.larova.core.domain.usecase.UnlockWithBiometrics
import app.larova.core.domain.usecase.UnlockWithPin
import app.larova.core.platform.AndroidExternalActions
import app.larova.core.platform.AndroidImageStore
import app.larova.core.platform.AndroidInstalledApps
import app.larova.core.platform.AndroidAudioRecorder
import app.larova.core.platform.AndroidMediaIntake
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
import app.larova.feature.card.edit.EditTarget
import app.larova.feature.help.HelpViewModel
import app.larova.feature.home.ArrangeTilesViewModel
import app.larova.feature.home.HomeViewModel
import app.larova.feature.settings.LogViewModel
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
    single<ImageStore> { AndroidImageStore(androidContext(), get()) }
    single<InstalledApps> { AndroidInstalledApps(androidContext()) }
    single<Translators> { AndroidTranslators(androidContext()) }
    single<Shortcuts> { AndroidShortcuts(androidContext()) }
    single<MediaIntake> { AndroidMediaIntake(androidContext(), get()) }
    // A singleton because a microphone is: two recorders on one device is not an error the framework
    // reports usefully, it simply produces silence.
    single<AudioRecorder> { AndroidAudioRecorder(androidContext(), get()) }
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

    // The paid unlock.
    //
    // Two bindings behind one interface, chosen by a build flag rather than by a flavour: the AAB
    // Play sells checks a purchase, and the APK on the GitHub Release has no store behind it and
    // says so. Nothing above this line can tell the difference, which is the point — the editor
    // asks one question and gets one answer.
    //
    // PlayBilling is a singleton because a BillingClient is: it owns a binding to the Play Store,
    // and a second one is a second connection nobody closes.
    single<EntitlementCache> { DataStoreEntitlementCache(get()) }
    single { PurchaseVerifier(BuildConfig.LICENSING_KEY) }
    single { PlayBilling(androidContext()) }
    // Separate from the entitlement repository: a contribution unlocks nothing, and the two must
    // not be able to influence each other.
    single { SupportPurchases(billing = get(), verifier = get()) }
    // Bound under its own type as well as behind the interface: launching a purchase needs an
    // Activity, so it is deliberately not on EntitlementRepository, and `rememberUnlockPurchase`
    // asks for the concrete one. Lazy, so a build with no paid tier never constructs a
    // BillingClient — nothing injects it there.
    single { PlayEntitlementRepository(cache = get(), verifier = get(), billing = get()) }
    single<EntitlementRepository> {
        if (BuildConfig.PAID_TIER) {
            get<PlayEntitlementRepository>()
        } else {
            BuildUnlockedEntitlementRepository()
        }
    }

    // Use cases are factories rather than singletons: each one is a couple of fields around a
    // repository, and none of them holds state worth sharing.
    factory { EnsureRootBoard(get()) }
    factory { ObserveHomeTiles(get(), get()) }
    factory { ObserveBoardTiles(get()) }
    factory { ObserveTile(get()) }
    factory { ObserveHelpContacts(get()) }
    factory { ToggleChecklistItem(get()) }
    factory { RecordEvent(get()) }
    factory { ObserveLog(get(), get()) }
    factory { ClearLog(get()) }
    factory { PruneLog(get()) }
    factory { MostOpenedTiles(get(), get()) }
    factory { PublishShortcuts(get(), get()) }
    factory { SaveCard(get(), get()) }
    factory { DeleteCard(get(), get()) }
    factory { TileEditing(get(), get(), get()) }
    factory { TileSource(get(), get()) }
    factory { CreateFolderBoard(get()) }
    factory { Folders(get(), get()) }
    factory { PickableApps(get()) }
    factory { IsAppInstalled(get()) }
    factory { Apps(get(), get()) }
    factory { CanTranslate(get()) }
    factory { Translations(get()) }
    factory { AddImage(get(), get()) }
    factory { AddMediaFile(get(), get()) }
    factory { LoadImage(get(), get()) }
    factory { FindMediaFile(get(), get()) }
    factory { CleanUpMedia(get(), get()) }
    factory { Media(get(), get(), get(), get(), get()) }
    factory { Recording(get(), get()) }
    factory { SearchTiles(get()) }
    factory { ApplyTemplate(get()) }
    factory { ReorderTiles(get(), get()) }
    factory { HasPin(get()) }
    factory { SetPin(get()) }
    factory { UnlockWithPin(get(), get()) }
    factory { UnlockWithBiometrics(get(), get()) }
    factory { LockParentView(get()) }

    factory { ObserveEntitlement(get()) }
    factory { ObserveLockedTypes(get()) }
    factory { RefreshEntitlement(get()) }
    factory { UnlockPrice(get()) }
    factory { ObserveSupportCount(get()) }
    factory { RecordSupport(get()) }

    // The version in the manifest is the app's own, read from the build rather than written twice.
    factory { ExportPackage(get(), get(), get(), get(), get(), BuildConfig.VERSION_NAME) }
    factory { ObserveLastBackup(get()) }
    factory { RecordLastBackup(get()) }
    factory { ReadPackagePreview(get()) }
    factory { ImportPackage(get(), get(), get(), get(), get(), get()) }

    viewModel {
        AppViewModel(
            get(), get(), get(), get(), get(), get(),
            get(), get(), get(), get(), get(),
        )
    }
    viewModel { UnlockViewModel(get(), get(), get()) }
    viewModel { PinSetupViewModel(get()) }
    viewModel { HomeViewModel(get(), get(), get(), get()) }
    // The board comes from the route: the start screen when it is empty, a folder otherwise.
    viewModel { parameters -> ArrangeTilesViewModel(parameters.get(), get(), get(), get()) }
    viewModel { HelpViewModel(get(), get()) }
    viewModel { LogViewModel(get(), get(), get()) }
    viewModel { TransferViewModel(get(), get(), get(), get(), get()) }
    // The card id comes from the navigation route, so it is passed in rather than injected.
    viewModel { parameters ->
        CardViewModel(parameters.get(), get(), get(), get(), get(), get(), get())
    }
    viewModel { parameters ->
        EditCardViewModel(parameters.get(), get(), get(), get(), get(), get(), get(), get())
    }
}

/** Kept next to the module so a caller cannot get the parameter order wrong. */
fun cardViewModelParameters(cardId: String) = parametersOf(cardId)

/**
 * An empty card id is a new tile; the editor treats it as such rather than looking one up. An empty
 * board id is the start screen. The two travel as one [EditTarget] so they cannot be swapped.
 */
fun editCardViewModelParameters(cardId: String, boardId: String) =
    parametersOf(EditTarget(cardId = cardId, boardId = boardId))

/** An empty board id is the start screen. */
fun arrangeViewModelParameters(boardId: String) = parametersOf(boardId)
