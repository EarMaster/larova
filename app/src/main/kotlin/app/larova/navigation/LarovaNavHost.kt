package app.larova.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.larova.rememberBiometricUnlock
import app.larova.core.domain.model.AppearanceSetting
import androidx.compose.runtime.LaunchedEffect
import app.larova.di.arrangeViewModelParameters
import app.larova.di.cardViewModelParameters
import app.larova.di.editCardViewModelParameters
import app.larova.feature.card.CardScreen
import app.larova.feature.card.CardViewModel
import app.larova.feature.card.edit.EditCardScreen
import app.larova.feature.card.edit.EditCardViewModel
import app.larova.feature.card.edit.callbacks
import app.larova.feature.help.HelpScreen
import app.larova.feature.help.HelpViewModel
import app.larova.feature.home.ArrangeTilesScreen
import app.larova.feature.home.ArrangeTilesViewModel
import app.larova.feature.home.HomeScreen
import app.larova.feature.home.HomeViewModel
import app.larova.feature.settings.LogScreen
import app.larova.feature.settings.LogViewModel
import app.larova.feature.settings.PinSetupScreen
import app.larova.feature.settings.PinSetupViewModel
import app.larova.feature.settings.SettingsScreen
import app.larova.feature.settings.UnlockScreen
import app.larova.feature.settings.UnlockViewModel
import app.larova.feature.transfer.TransferScreen
import app.larova.feature.transfer.TransferViewModel
import app.larova.formatExportDate
import app.larova.formatLogTime
import app.larova.rememberBackupPicker
import app.larova.rememberMicrophoneRequest
import app.larova.rememberPicturePicker
import app.larova.rememberRestorePicker
import app.larova.rememberSoundPicker
import app.larova.rememberVideoPicker
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
    onPrepareCall: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenApp: (String) -> Unit,
    openCardId: String? = null,
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
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
            val pickPicture = rememberPicturePicker(viewModel::onPictureChosen)
            val pickVideo = rememberVideoPicker(viewModel::onMediaChosen)
            val pickSound = rememberSoundPicker(viewModel::onMediaChosen)
            val requestMicrophone = rememberMicrophoneRequest(viewModel::onStartRecording)

            EditCardScreen(
                state = state,
                callbacks = viewModel.callbacks(
                    openPicturePicker = pickPicture,
                    openVideoPicker = pickVideo,
                    openSoundPicker = pickSound,
                    requestMicrophone = requestMicrophone,
                ),
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
            SettingsScreen(
                appearance = appearance,
                onAppearanceChange = onAppearanceChange,
                isParentView = isParentView,
                onUnlock = { navController.navigate(UnlockRoute) },
                onLock = onLockParentView,
                onChangePin = { navController.navigate(PinSetupRoute) },
                onOpenTransfer = { navController.navigate(TransferRoute) },
                onBack = goBack,
            )
        }
    }
}
