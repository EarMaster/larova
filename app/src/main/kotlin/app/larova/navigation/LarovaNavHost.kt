package app.larova.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
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
import app.larova.feature.home.HomeScreen
import app.larova.feature.home.HomeViewModel
import app.larova.feature.settings.SettingsScreen
import app.larova.feature.transfer.TransferScreen
import org.koin.compose.viewmodel.koinViewModel

/**
 * Holds the graph together. Screens take callbacks and know nothing about where they sit, which is
 * what keeps the feature modules independent of each other.
 */
@Composable
fun LarovaNavHost(
    appearance: AppearanceSetting,
    onAppearanceChange: (AppearanceSetting) -> Unit,
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
                onOpenTile = { id -> navController.navigate(CardRoute(id)) },
                onAddTile = { navController.navigate(CardEditRoute()) },
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

        composable<HelpRoute> {
            HelpScreen(onBack = goBack, onHelp = openHelp)
        }

        composable<TransferRoute> {
            TransferScreen(onBack = goBack, onHelp = openHelp)
        }

        composable<SettingsRoute> {
            SettingsScreen(
                appearance = appearance,
                onAppearanceChange = onAppearanceChange,
                onBack = goBack,
                onHelp = openHelp,
            )
        }
    }
}
