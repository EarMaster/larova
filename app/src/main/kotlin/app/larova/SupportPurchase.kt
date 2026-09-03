package app.larova

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import app.larova.core.billing.PurchaseOutcome
import app.larova.core.billing.SupportPurchases
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * The route to Play's sheet for the repeatable contribution.
 *
 * Here rather than in `:feature:settings` for the same reason as `rememberUnlockPurchase`: it needs
 * an Activity, and an Activity is not something a singleton can hold without leaking it. The screen
 * is handed a nullable lambda and learns only whether contributing is possible at all.
 *
 * Null when this build has no paid tier. The card is then not drawn, rather than drawn and inert.
 */
@Composable
fun rememberSupportPurchase(onOutcome: (PurchaseOutcome) -> Unit): (() -> Unit)? {
    if (!BuildConfig.PAID_TIER) return null

    val context = LocalContext.current
    val activity = context.findFragmentActivity() ?: return null
    val scope = rememberCoroutineScope()
    val support = koinInject<SupportPurchases>()

    // Swept here rather than at launch, because here is where it matters: the moment before
    // somebody could tap. A consume that never landed leaves the product owned, and Play then
    // refuses every later purchase with ITEM_ALREADY_OWNED — somebody willing to give twice would
    // find they could not, with nothing on screen to explain it. Cheap and silent either way.
    LaunchedEffect(Unit) { support.clearLeftovers() }

    return {
        scope.launch { onOutcome(support.contribute(activity)) }
        Unit
    }
}
