package app.larova

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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

    /**
     * The tile a launcher shortcut asked for, if this activity was started by one.
     *
     * State rather than a field read once, because with `singleTop` a second shortcut arrives at
     * `onNewIntent` in an activity that is already composing.
     */
    private var openCardId by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        openCardId = intent?.cardId()
        setContent { LarovaApp(openCardId = openCardId) }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        openCardId = intent.cardId()
    }

    private fun Intent.cardId(): String? = getStringExtra(EXTRA_CARD_ID)?.takeIf { it.isNotEmpty() }

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
private fun LarovaApp(openCardId: String? = null) {
    val viewModel = koinViewModel<AppViewModel>()
    val actions = koinInject<ExternalActions>()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val viewMode by viewModel.viewMode.collectAsStateWithLifecycle()

    val entitlement by viewModel.entitlement.collectAsStateWithLifecycle()
    val unlockCheck by viewModel.unlockCheck.collectAsStateWithLifecycle()
    val contentLanguage by viewModel.contentLanguage.collectAsStateWithLifecycle()
    val supportCount by viewModel.supportCount.collectAsStateWithLifecycle()
    val supportMessage by viewModel.supportMessage.collectAsStateWithLifecycle()

    LarovaTheme(mode = appearance.resolve()) {
        LarovaNavHost(
            appearance = appearance,
            onAppearanceChange = viewModel::setAppearance,
            isParentView = viewMode.isParent,
            onLockParentView = viewModel::leaveParentView,
            entitlement = entitlement,
            onCheckPurchases = viewModel::checkPurchasesAgain,
            unlockCheck = unlockCheck,
            onDismissUnlockCheck = viewModel::dismissUnlockCheck,
            onUnlockPurchased = viewModel::onUnlockPurchased,
            onUnlockPending = viewModel::onUnlockPending,
            onUnlockUnavailable = viewModel::onUnlockUnavailable,
            supportCount = supportCount,
            supportMessage = supportMessage,
            onSupported = viewModel::onSupported,
            onSupportUnavailable = viewModel::onSupportUnavailable,
            onPrepareCall = actions::prepareCall,
            // Null below Android 13, where there is no screen to open — which is what makes the
            // settings row absent rather than present and inert.
            onOpenLanguageSettings = actions::openAppLanguageSettings
                .takeIf { actions.canOpenAppLanguageSettings },
            contentLanguage = contentLanguage,
            onContentLanguageChange = viewModel::onContentLanguageChange,
            onOpenUrl = actions::openUrl,
            onTranslate = actions::translate,
            onOpenApp = actions::openApp,
            openCardId = openCardId,
        )
    }
}
