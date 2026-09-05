package app.larova.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.larova.BuildConfig
import app.larova.core.billing.PurchaseOutcome
import app.larova.core.domain.app.AppLanguage
import app.larova.core.domain.model.AppearanceSetting
import app.larova.core.domain.model.Entitlement
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.edit_cancel
import app.larova.core.ui.resources.edit_language_pick
import app.larova.core.ui.theme.Dimens
import app.larova.di.arrangeViewModelParameters
import app.larova.di.cardViewModelParameters
import app.larova.di.editCardViewModelParameters
import app.larova.di.editTranslationViewModelParameters
import app.larova.feature.card.CardScreen
import app.larova.feature.card.CardViewModel
import app.larova.feature.card.edit.EditCardScreen
import app.larova.feature.card.edit.EditCardViewModel
import app.larova.feature.card.edit.EditTranslationScreen
import app.larova.feature.card.edit.EditTranslationViewModel
import app.larova.feature.card.edit.SymbolPickerScreen
import app.larova.feature.card.edit.callbacks
import app.larova.feature.help.HelpScreen
import app.larova.feature.help.HelpViewModel
import app.larova.feature.home.ArrangeTilesScreen
import app.larova.feature.home.ArrangeTilesViewModel
import app.larova.feature.home.HomeScreen
import app.larova.feature.home.HomeViewModel
import app.larova.feature.settings.ContentLanguageSetting
import app.larova.feature.settings.LogScreen
import app.larova.feature.settings.LogViewModel
import app.larova.feature.settings.PinSetupScreen
import app.larova.feature.settings.PinSetupViewModel
import app.larova.feature.settings.SUPPORT_URL
import app.larova.feature.settings.SettingsScreen
import app.larova.feature.settings.SupportMessage
import app.larova.feature.settings.UnlockCheck
import app.larova.feature.settings.UnlockScreen
import app.larova.feature.settings.UnlockViewModel
import app.larova.feature.transfer.TransferScreen
import app.larova.feature.transfer.TransferViewModel
import app.larova.formatExportDate
import app.larova.formatLogTime
import app.larova.rememberBackupPicker
import app.larova.rememberBiometricUnlock
import app.larova.rememberMicrophoneRequest
import app.larova.rememberPicturePicker
import app.larova.rememberRestorePicker
import app.larova.rememberSoundPicker
import app.larova.rememberSupportPurchase
import app.larova.rememberUnlockPurchase
import app.larova.rememberVideoPicker
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Holds the graph together. Screens take callbacks and know nothing about where they sit, which is
 * what keeps the feature modules independent of each other.
 */
@Composable
fun LarovaNavHost(
    appearance: AppearanceSetting,
    onAppearanceChange: (AppearanceSetting) -> Unit,
    isParentView: Boolean,
    onLockParentView: () -> Unit,
    entitlement: Entitlement,
    onCheckPurchases: () -> Unit,
    unlockCheck: UnlockCheck,
    onDismissUnlockCheck: () -> Unit,
    onUnlockPurchased: () -> Unit,
    onUnlockPending: () -> Unit,
    onUnlockUnavailable: () -> Unit,
    supportCount: Int,
    supportMessage: SupportMessage?,
    onSupported: () -> Unit,
    onSupportUnavailable: () -> Unit,
    onPrepareCall: (String) -> Unit,
    /** Null on a phone with no per-app language screen; the settings row is then absent. */
    onOpenLanguageSettings: (() -> Unit)?,
    contentLanguage: ContentLanguageSetting?,
    onContentLanguageChange: (String?) -> Unit,
    onOpenUrl: (String) -> Unit,
    onTranslate: (String) -> Unit,
    onOpenApp: (String) -> Unit,
    openCardId: String? = null,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    // The endonym for a tag, resolved once for the graph. From the platform rather than from
    // strings.xml: a language names itself the same way whatever the app is set to.
    val appLanguage = koinInject<AppLanguage>()
    val languageNameOf: (String) -> String = { appLanguage.nameOf(it) }
    val openHelp: () -> Unit = { navController.navigate(HelpRoute) }
    val goBack: () -> Unit = { navController.popBackStack() }

    // A launcher shortcut opens the tile on top of the start screen rather than instead of it, so
    // "back" goes where somebody would expect it to: the grid, not out of the app.
    LaunchedEffect(openCardId) {
        if (!openCardId.isNullOrEmpty()) navController.navigate(CardRoute(openCardId))
    }

    NavHost(
        navController = navController,
        startDestination = HomeRoute,
        modifier = modifier,
    ) {
        composable<HomeRoute> {
            val viewModel = koinViewModel<HomeViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()
            HomeScreen(
                state = state,
                isParentView = isParentView,
                onQueryChange = viewModel::onQueryChange,
                onClearQuery = viewModel::onClearQuery,
                onOpenTile = { id -> navController.navigate(CardRoute(id)) },
                onAddTile = { navController.navigate(CardEditRoute()) },
                onArrange = { navController.navigate(ArrangeRoute()) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenLog = { navController.navigate(LogRoute) },
                onUseTemplate = viewModel::onUseTemplate,
                onHelp = openHelp,
            )
        }

        composable<CardRoute> { entry ->
            val route = entry.toRoute<CardRoute>()
            // Keyed on the id: two card screens on the back stack are two different tiles, and a
            // shared ViewModel would show the second one the first one's content.
            val viewModel = koinViewModel<CardViewModel>(
                key = route.cardId,
                parameters = { cardViewModelParameters(route.cardId) },
            )
            val state by viewModel.state.collectAsStateWithLifecycle()

            // Recorded when the screen arrives, not when the tile was tapped: what the log is for is
            // what was actually read.
            LaunchedEffect(route.cardId) { viewModel.onOpened() }

            CardScreen(
                state = state,
                isParentView = isParentView,
                onToggleItem = viewModel::onToggleItem,
                // Two calls rather than one: the ViewModel writes the log line, the platform opens
                // the dialler. Neither belongs inside the other.
                onPrepareCall = { number ->
                    viewModel.onCallPrepared()
                    onPrepareCall(number)
                },
                onOpenUrl = onOpenUrl,
                onOpenApp = onOpenApp,
                // Straight through, unlike onPrepareCall: there is nothing for the ViewModel to
                // record here. Handing words to another app is not something that happened to the
                // tile, and a log line about it would be a line about the caregiver instead.
                onTranslate = onTranslate,
                onContentLanguageChange = viewModel::onContentLanguageChange,
                onEdit = { navController.navigate(CardEditRoute(route.cardId)) },
                onBack = goBack,
                onHelp = openHelp,
                loadPicture = viewModel::pictureFor,
                // Only a folder uses these three. A tile inside one opens the same card screen as
                // a tile on the start screen, which is what keeps the graph two levels deep.
                onOpenTile = { id -> navController.navigate(CardRoute(id)) },
                onAddTileHere = {
                    state.folderBoardId?.let { navController.navigate(CardEditRoute(boardId = it)) }
                },
                onArrange = {
                    state.folderBoardId?.let { navController.navigate(ArrangeRoute(boardId = it)) }
                },
            )
        }

        composable<CardEditRoute> { entry ->
            val route = entry.toRoute<CardEditRoute>()
            val viewModel = koinViewModel<EditCardViewModel>(
                // Keyed on both: "a new tile on the start screen" and "a new tile in this folder"
                // are two different editors, and sharing one would put the tile on the wrong board.
                key = "edit-" + route.cardId + "-" + route.boardId,
                parameters = { editCardViewModelParameters(route.cardId, route.boardId) },
            )
            val state by viewModel.state.collectAsStateWithLifecycle()

            // Leaving is the navigator's job, not the ViewModel's: it is the only thing here that
            // knows a deleted tile also has to take its detail screen off the back stack.
            LaunchedEffect(state.saved, state.deleted) {
                if (state.saved) {
                    navController.popBackStack()
                } else if (state.deleted) {
                    navController.popBackStack(route = HomeRoute, inclusive = false)
                }
            }

            // The picker belongs to the platform, so it is opened from here; the ViewModel has
            // already been told which step the picture is for.
            // The picker pops back with its answer here rather than the editor being recreated
            // around a new argument: everything else on this screen is half-typed and unsaved.
            val chosenSymbol = entry.savedStateHandle.remove<String>(SYMBOL_RESULT)
            LaunchedEffect(chosenSymbol) {
                chosenSymbol?.let(viewModel::onSymbolChange)
            }

            val pickPicture = rememberPicturePicker(viewModel::onPictureChosen)
            val pickVideo = rememberVideoPicker(viewModel::onMediaChosen)
            val pickSound = rememberSoundPicker(viewModel::onMediaChosen)
            val requestMicrophone = rememberMicrophoneRequest(viewModel::onStartRecording)
            // The store's own result type is translated here rather than in :feature:card, which is
            // what keeps :core:billing out of every feature module. A cancelled purchase is left
            // alone on purpose: somebody who backed out has already seen their own decision.
            val buyUnlock = rememberUnlockPurchase(viewModel::applyUnlockOutcome)
            // Which languages there are to add. The fourteen the app itself speaks, minus the ones
            // this tile already has — offering a language twice would produce a second row under
            // the same key and quietly replace the first.
            var choosingLanguage by remember { mutableStateOf(false) }

            EditCardScreen(
                state = state,
                callbacks = viewModel.callbacks(
                    openSymbolPicker = {
                        navController.navigate(
                            SymbolPickerRoute(
                                selectedKey = state.symbolKey,
                                colorToken = state.colorToken,
                            ),
                        )
                    },
                    openPicturePicker = pickPicture,
                    openVideoPicker = pickVideo,
                    openSoundPicker = pickSound,
                    requestMicrophone = requestMicrophone,
                    buyUnlock = buyUnlock,
                    addLanguage = { choosingLanguage = true },
                    editLanguage = { lang ->
                        navController.navigate(CardTranslationRoute(route.cardId, lang))
                    },
                ),
                onBack = goBack,
            )

            if (choosingLanguage) {
                LanguagePickerDialog(
                    taken = state.languages.map { it.tag }.toSet(),
                    nameOf = languageNameOf,
                    onPick = { lang ->
                        choosingLanguage = false
                        navController.navigate(CardTranslationRoute(route.cardId, lang))
                    },
                    onDismiss = { choosingLanguage = false },
                )
            }
        }

        composable<CardTranslationRoute> { entry ->
            val route = entry.toRoute<CardTranslationRoute>()
            val viewModel = koinViewModel<EditTranslationViewModel>(
                key = "translation-" + route.cardId + "-" + route.lang,
                parameters = { editTranslationViewModelParameters(route.cardId, route.lang) },
            )
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(state.saved) {
                if (state.saved) navController.popBackStack()
            }

            EditTranslationScreen(
                state = state,
                onTitleChange = viewModel::onTitleChange,
                onSubtitleChange = viewModel::onSubtitleChange,
                onFieldChange = viewModel::onFieldChange,
                onTranslate = onTranslate,
                onSave = viewModel::onSave,
                onDelete = viewModel::onDelete,
                onBack = goBack,
            )
        }

        composable<ArrangeRoute> { entry ->
            val route = entry.toRoute<ArrangeRoute>()
            val viewModel = koinViewModel<ArrangeTilesViewModel>(
                key = "arrange-" + route.boardId,
                parameters = { arrangeViewModelParameters(route.boardId) },
            )
            val tiles by viewModel.tiles.collectAsStateWithLifecycle()
            ArrangeTilesScreen(
                tiles = tiles,
                onMoveUp = viewModel::onMoveUp,
                onMoveDown = viewModel::onMoveDown,
                onBack = goBack,
            )
        }

        composable<UnlockRoute> {
            val viewModel = koinViewModel<UnlockViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            // A fresh installation has no PIN to check, so the first unlock is a set-up instead.
            // Handled here rather than inside the screen: it is a change of destination.
            LaunchedEffect(state.needsPinSetup) {
                if (state.needsPinSetup) {
                    navController.navigate(PinSetupRoute) {
                        popUpTo(UnlockRoute) { inclusive = true }
                    }
                }
            }
            LaunchedEffect(state.unlocked) {
                if (state.unlocked) navController.popBackStack()
            }

            UnlockScreen(
                pin = state.pin,
                onPinChange = viewModel::onPinChange,
                onUnlock = viewModel::onUnlock,
                onUseBiometrics = rememberBiometricUnlock(onAccepted = viewModel::onBiometricsAccepted),
                wrongPin = state.wrongPin,
                onBack = goBack,
            )
        }

        composable<SymbolPickerRoute> { entry ->
            val route = entry.toRoute<SymbolPickerRoute>()
            SymbolPickerScreen(
                selectedKey = route.selectedKey,
                colorToken = route.colorToken,
                onPick = { key ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle?.set(SYMBOL_RESULT, key)
                    navController.popBackStack()
                },
                onBack = goBack,
            )
        }

        composable<PinSetupRoute> {
            val viewModel = koinViewModel<PinSetupViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(state.saved) {
                if (state.saved) navController.popBackStack()
            }

            PinSetupScreen(
                pin = state.pin,
                repeated = state.repeated,
                onPinChange = viewModel::onPinChange,
                onRepeatChange = viewModel::onRepeatChange,
                onSave = viewModel::onSave,
                error = state.error,
                onBack = goBack,
            )
        }

        composable<HelpRoute> {
            val viewModel = koinViewModel<HelpViewModel>()
            val contacts by viewModel.contacts.collectAsStateWithLifecycle()
            HelpScreen(
                contacts = contacts,
                onPrepareCall = { number ->
                    contacts.firstOrNull { it.number == number }
                        ?.let { viewModel.onCallPrepared(it.cardId) }
                    onPrepareCall(number)
                },
                onBack = goBack,
                // Already here: tapping the bar on this screen does nothing rather than stacking a
                // second copy of it on the back stack.
                onHelp = {},
            )
        }

        composable<TransferRoute> {
            val viewModel = koinViewModel<TransferViewModel>()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val pickDestination = rememberBackupPicker { destination ->
                viewModel.onDestinationChosen(destination, label = null)
            }
            val pickSource = rememberRestorePicker(viewModel::onSourceChosen)

            TransferScreen(
                state = state,
                formatDate = ::formatExportDate,
                onBackup = pickDestination,
                onRestore = pickSource,
                onConfirmImport = viewModel::onConfirmImport,
                onCancelImport = viewModel::onCancelImport,
                onBack = goBack,
            )
        }

        composable<LogRoute> {
            val viewModel = koinViewModel<LogViewModel>()
            val lines by viewModel.lines.collectAsStateWithLifecycle()
            val note by viewModel.note.collectAsStateWithLifecycle()

            LogScreen(
                lines = lines,
                note = note,
                isParentView = isParentView,
                formatTime = ::formatLogTime,
                onNoteChange = viewModel::onNoteChange,
                onAddNote = viewModel::onAddNote,
                onClear = viewModel::onClear,
                onBack = goBack,
            )
        }

        composable<SettingsRoute> {
            // Cancelled is deliberately silent: somebody who backed out of paying has already
            // seen their own decision, and a card that said so would be nagging.
            val support = rememberSupportPurchase { outcome ->
                outcome.applyTo(onSupported = onSupported, onUnavailable = onSupportUnavailable)
            }
            // The same sheet the locked tile opens, from the offer the full-version card puts up
            // when it finds nothing. One product, so a purchase started here unlocks the tiles.
            val buyUnlock = rememberUnlockPurchase { outcome ->
                outcome.applyUnlock(
                    onPurchased = onUnlockPurchased,
                    onPending = onUnlockPending,
                    onUnavailable = onUnlockUnavailable,
                )
            }
            SettingsScreen(
                appearance = appearance,
                onAppearanceChange = onAppearanceChange,
                isParentView = isParentView,
                onUnlock = { navController.navigate(UnlockRoute) },
                onLock = onLockParentView,
                onChangePin = { navController.navigate(PinSetupRoute) },
                onOpenTransfer = { navController.navigate(TransferRoute) },
                onBack = goBack,
                entitlement = entitlement,
                // Null in a build with no store behind it: there is nothing to ask, and the status
                // line says as much instead of offering a button that cannot help.
                onCheckPurchases = onCheckPurchases.takeIf { BuildConfig.PAID_TIER },
                unlockCheck = unlockCheck,
                onDismissUnlockCheck = onDismissUnlockCheck,
                onBuyUnlock = buyUnlock,
                // The browser, through the same external action a Website tile uses. No internet
                // permission is involved: the URL is handed to whatever app owns http, and this
                // one never resolves it.
                onOpenSupportPage = { onOpenUrl(SUPPORT_URL) },
                onOpenLanguageSettings = onOpenLanguageSettings,
                contentLanguage = contentLanguage,
                onContentLanguageChange = onContentLanguageChange,
                supportCount = supportCount,
                onSupport = support,
                supportMessage = supportMessage,
                appVersion = BuildConfig.VERSION_NAME,
            )
        }
    }
}

/**
 * The store's own result type, translated for the editor.
 *
 * Extracted from the graph rather than written inline: two `when` blocks over a sealed interface
 * pushed `LarovaNavHost` past the complexity the project allows, and a navigation graph is the
 * wrong place to read purchase semantics anyway. This is also the one place that decides Play's
 * vocabulary maps onto the app's, which is easier to check when it is one function.
 *
 * `AlreadyOwned` counts as success: a reinstall that already paid is unlocked, not refused.
 * `Cancelled` is silent — somebody who backed out has already seen their own decision.
 */
private fun EditCardViewModel.applyUnlockOutcome(outcome: PurchaseOutcome) {
    outcome.applyUnlock(
        onPurchased = ::onPurchased,
        onPending = ::onPurchasePending,
        onUnavailable = ::onPurchaseUnavailable,
    )
}

/**
 * The unlock's outcome, read once for both places that sell it — the locked tile's offer and the
 * settings card's. Two copies of this `when` is how the two screens would come to disagree about
 * what `AlreadyOwned` means.
 */
private fun PurchaseOutcome.applyUnlock(
    onPurchased: () -> Unit,
    onPending: () -> Unit,
    onUnavailable: () -> Unit,
) {
    when (this) {
        is PurchaseOutcome.Purchased, PurchaseOutcome.AlreadyOwned -> onPurchased()
        PurchaseOutcome.Pending -> onPending()
        is PurchaseOutcome.Unavailable -> onUnavailable()
        PurchaseOutcome.Cancelled -> Unit
    }
}

/**
 * The same translation for the repeatable contribution, which reads differently.
 *
 * `Pending` is silent here rather than reported: an unpaid cash order is not a contribution yet,
 * and the tally must only ever count money that arrived. `AlreadyOwned` is a failure rather than a
 * success — it means a previous purchase was never consumed, so the sweep in `rememberSupportPurchase`
 * has not caught up and nothing new was bought.
 */
private fun PurchaseOutcome.applyTo(onSupported: () -> Unit, onUnavailable: () -> Unit) {
    when (this) {
        is PurchaseOutcome.Purchased -> onSupported()
        PurchaseOutcome.Cancelled, PurchaseOutcome.Pending -> Unit
        PurchaseOutcome.AlreadyOwned, is PurchaseOutcome.Unavailable -> onUnavailable()
    }
}

/**
 * Which language to write a tile in.
 *
 * The fourteen the app itself speaks, minus the ones this tile already has. Offering one twice
 * would produce a second row under the same `(cardId, lang)` key and quietly replace the first —
 * and a parent who tapped "Turkish" expecting a blank form would find their own earlier work.
 *
 * The list is the app's own locales rather than every language the phone knows: these are the ones
 * a caregiver is likely to need, and a list of two hundred is a list nobody reads. Each is named in
 * its own language, from the platform, so a Turkish reader sees "Türkçe" whatever the app is set to.
 */
@Composable
private fun LanguagePickerDialog(
    taken: Set<String>,
    nameOf: (String) -> String,
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val offered = APP_LANGUAGES.filterNot { it in taken }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.edit_language_pick)) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                for (tag in offered) {
                    TextButton(
                        onClick = { onPick(tag) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Dimens.MinTouchTarget),
                    ) {
                        Text(text = nameOf(tag), modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(Res.string.edit_cancel))
            }
        },
    )
}

/**
 * The languages Larova itself speaks, as BCP-47 tags.
 *
 * The same fourteen as `locales_config.xml` and the `values-*` folders, and deliberately a separate
 * list rather than one derived from them: Compose resources cannot be enumerated at runtime, and a
 * list derived from the phone's installed locales would offer two hundred entries. If a fifteenth
 * language is ever added, `docs/localization.md` §6 is where the other places to touch are listed —
 * add it here too.
 */
private val APP_LANGUAGES = listOf(
    "en", "de", "fr", "it", "es", "pt-PT", "uk", "pl", "ru", "tr", "ar", "hi", "zh", "ja",
)
