package app.larova

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.larova.core.domain.session.ViewModeSession
import app.larova.core.platform.ExternalActions
import app.larova.core.ui.theme.LarovaTheme
import app.larova.core.ui.theme.resolve
import app.larova.navigation.LarovaNavHost
import org.koin.android.ext.android.inject
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * A FragmentActivity rather than a ComponentActivity, because BiometricPrompt needs one.
 */
class MainActivity : FragmentActivity() {

    private val session: ViewModeSession by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent { LarovaApp() }
    }

    /**
     * Every tap, scroll and key press extends parent view. Reported by the framework for real user
     * input only, which is exactly the definition wanted here — a screen that redraws itself must
     * not count as somebody being present.
     */
    override fun onUserInteraction() {
        super.onUserInteraction()
        session.touch()
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
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()

    LarovaTheme(mode = appearance.resolve()) {
        LarovaNavHost(
            appearance = appearance,
            onAppearanceChange = viewModel::setAppearance,
            isParentView = viewMode.isParent,
            onLockParentView = viewModel::leaveParentView,
            onPrepareCall = actions::prepareCall,
            onOpenUrl = actions::openUrl,
            onOpenApp = actions::openApp,
        )
    }
}
