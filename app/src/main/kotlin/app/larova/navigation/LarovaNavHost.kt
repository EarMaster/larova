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
import app.larova.feature.settings.PinSetupScreen
import app.larova.feature.settings.PinSetupViewModel
import app.larova.feature.settings.SettingsScreen
import app.larova.feature.settings.UnlockScreen
import app.larova.feature.settings.UnlockViewModel
import app.larova.feature.transfer.TransferScreen
import app.larova.feature.transfer.TransferViewModel
import app.larova.formatExportDate
import app.larova.rememberBackupPicker
import app.larova.rememberRestorePicker
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
    modifier: Modifier = Modifier,
) {
    val navController = rememberNavController()
    val openHelp: () -> Unit = { navController.navigate(HelpRoute) }
    val goBack: () -> Unit = { navController.popBackStack() }

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
                onArrange = { navController.navigate(ArrangeRoute) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenTransfer = { navController.navigate(TransferRoute) },
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
            CardScreen(
                state = state,
                isParentView = isParentView,
                onToggleItem = viewModel::onToggleItem,
                onPrepareCall = onPrepareCall,
                onOpenUrl = onOpenUrl,
                onEdit = { navController.navigate(CardEditRoute(route.cardId)) },
                onBack = goBack,
                onHelp = openHelp,
            )
        }

        composable<CardEditRoute> { entry ->
            val route = entry.toRoute<CardEditRoute>()
            val viewModel = koinViewModel<EditCardViewModel>(
                key = "edit-" + route.cardId,
                parameters = { editCardViewModelParameters(route.cardId) },
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

            EditCardScreen(
                state = state,
                callbacks = viewModel.callbacks(),
                onBack = goBack,
                onHelp = openHelp,
            )
        }

        composable<ArrangeRoute> {
            val viewModel = koinViewModel<ArrangeTilesViewModel>()
            val tiles by viewModel.tiles.collectAsStateWithLifecycle()
            ArrangeTilesScreen(
                tiles = tiles,
                onMoveUp = viewModel::onMoveUp,
                onMoveDown = viewModel::onMoveDown,
                onBack = goBack,
                onHelp = openHelp,
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
                onHelp = openHelp,
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
                onHelp = openHelp,
            )
        }

        composable<HelpRoute> {
            val viewModel = koinViewModel<HelpViewModel>()
            val contacts by viewModel.contacts.collectAsStateWithLifecycle()
            HelpScreen(
                contacts = contacts,
                onPrepareCall = onPrepareCall,
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
                onHelp = openHelp,
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
                onBack = goBack,
                onHelp = openHelp,
            )
        }
    }
}
