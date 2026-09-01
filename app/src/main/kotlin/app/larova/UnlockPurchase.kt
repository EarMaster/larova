package app.larova

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import app.larova.core.billing.PlayEntitlementRepository
import app.larova.core.billing.PurchaseOutcome
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

/**
 * The route to Google Play's purchase sheet.
 *
 * Here rather than in `:feature:card` for the same reason `rememberBiometricUnlock` is here: it
 * needs an Activity, and an Activity is not something a singleton can hold without leaking it. The
 * screen is handed a nullable lambda and learns only whether buying is possible at all — which is
 * also what keeps `:core:billing` out of every feature module, and what lets the iOS entry point
 * supply a StoreKit equivalent later without touching the editor.
 *
 * Null when this build has no paid tier. Nothing is locked in that case, so the sheet that would
 * call this should be unreachable; returning null rather than a no-op lambda is what makes that a
 * disabled button instead of a button that silently does nothing.
 */
@Composable
fun rememberUnlockPurchase(onOutcome: (PurchaseOutcome) -> Unit): (() -> Unit)? {
    if (!BuildConfig.PAID_TIER) return null

    val context = LocalContext.current
    val activity = context.findFragmentActivity() ?: return null
    val scope = rememberCoroutineScope()

    // The concrete type, not the interface: launching a purchase needs an Activity, so it is
    // deliberately absent from EntitlementRepository. This is the one place that difference shows.
    val entitlements = koinInject<PlayEntitlementRepository>()

    return {
        scope.launch { onOutcome(entitlements.purchase(activity)) }
        Unit
    }
}
