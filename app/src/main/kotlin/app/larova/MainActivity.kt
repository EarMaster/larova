package app.larova

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.larova.core.platform.ExternalActions
import app.larova.core.ui.theme.LarovaTheme
import app.larova.core.ui.theme.resolve
import app.larova.navigation.LarovaNavHost
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { LarovaApp() }
    }
}

/**
 * The theme sits above the navigation graph, so the appearance setting applies to every screen
 * including the ones not yet written. Night in particular has to reach the guide screen, which is
 * the reason the mode exists.
 */
@Composable
private fun LarovaApp() {
    val viewModel = koinViewModel<AppViewModel>()
    val actions = koinInject<ExternalActions>()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()

    LarovaTheme(mode = appearance.resolve()) {
        LarovaNavHost(
            appearance = appearance,
            onAppearanceChange = viewModel::setAppearance,
            onPrepareCall = actions::prepareCall,
            onOpenUrl = actions::openUrl,
        )
    }
}
