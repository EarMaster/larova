package app.larova.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.larova.core.domain.model.AppearanceSetting
import app.larova.feature.card.CardScreen
import app.larova.feature.help.HelpScreen
import app.larova.feature.home.HomeScreen
import app.larova.feature.home.HomeTile
import app.larova.feature.settings.SettingsScreen
import app.larova.feature.transfer.TransferScreen

/**
 * Holds the graph together. Screens take callbacks and know nothing about where they sit, which is
 * what keeps the feature modules independent of each other.
 */
@Composable
fun LarovaNavHost(
    tiles: List<HomeTile>,
    appearance: AppearanceSetting,
    onAppearanceChange: (AppearanceSetting) -> Unit,
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
            HomeScreen(
                tiles = tiles,
                onOpenTile = { id -> navController.navigate(CardRoute(id)) },
                onOpenSettings = { navController.navigate(SettingsRoute) },
                onOpenTransfer = { navController.navigate(TransferRoute) },
                onHelp = openHelp,
            )
        }

        composable<CardRoute> { entry ->
            val route = entry.toRoute<CardRoute>()
            CardScreen(
                title = tiles.firstOrNull { it.id == route.cardId }?.title ?: route.cardId,
                type = null,
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
