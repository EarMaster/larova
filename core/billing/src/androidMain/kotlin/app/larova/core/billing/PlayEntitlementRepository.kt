package app.larova.core.billing

import android.app.Activity
import app.larova.core.domain.model.Entitlement
import app.larova.core.domain.repository.EntitlementCache
import app.larova.core.domain.repository.EntitlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The paid unlock, as far as the rest of the app is concerned.
 *
 * The stored receipt is re-checked on every read rather than trusted once. That costs an RSA verify
 * per emission, which is nothing next to the alternative: a boolean in a preferences file that
 * anybody with a rooted phone could write by hand.
 *
 * **Nothing here ever takes the unlock away.** [refresh] can only raise the answer. That is a
 * decision rather than an oversight, and the reasoning is worth keeping:
 *
 * - A failed query is the normal case. Larova has no internet permission, so the store is reached
 *   through Play, and on a child's phone that may be offline for a fortnight there is nothing to
 *   reach. "Could not ask" must never render as "did not pay".
 * - A *successful but empty* answer is not treated as a revocation either. It is what a refund
 *   looks like, but it is also what a phone signed into a different Google account looks like —
 *   and this app is installed on one person's phone and set up by another. Losing a paid unlock
 *   because a parent signed out is a support problem and a bad review; a refunded family keeping
 *   the unlock is a rounding error. The licence lets anyone rebuild without the check at all, so
 *   there is no version of this worth being strict about.
 *
 * [EntitlementCache.clear] therefore has no caller here. It exists for the tests and for a manual
 * "forget the purchase" action, which is how a tester gets back to the locked state.
 */
class PlayEntitlementRepository(
    private val cache: EntitlementCache,
    private val verifier: PurchaseVerifier,
    private val billing: PlayBilling,
) : EntitlementRepository {

    override fun observe(): Flow<Entitlement> = cache.observe().map { receipt ->
        if (receipt != null && verifier.verify(receipt)) Entitlement.PLAY else Entitlement.NONE
    }

    override suspend fun refresh() {
        val receipt = billing.ownedUnlock() ?: return
        if (verifier.verify(receipt)) cache.write(receipt)
    }

    override suspend fun formattedPrice(): String? = billing.offer()?.formattedPrice

    /**
     * Not on [EntitlementRepository], because the domain has no business knowing what an Activity
     * is. The caller in `:app` holds this concrete type; the sheet that offers the unlock lives in
     * `:feature:card`, takes state and callbacks, and never sees it.
     *
     * A receipt is only written after it verifies. A purchase whose signature does not check out is
     * reported as unavailable, which is the truthful thing to say about it.
     */
    suspend fun purchase(activity: Activity): PurchaseOutcome {
        val outcome = billing.purchase(activity)
        if (outcome is PurchaseOutcome.Purchased) {
            if (!verifier.verify(outcome.receipt)) {
                return PurchaseOutcome.Unavailable(SIGNATURE_REJECTED)
            }
            cache.write(outcome.receipt)
        }
        // ITEM_ALREADY_OWNED arrives when a reinstall paid before. The receipt comes from the query
        // rather than from the sheet in that case, so ask.
        if (outcome is PurchaseOutcome.AlreadyOwned) refresh()
        return outcome
    }

    /** Play has no code for "we did not sign that", so this one is the app's own. */
    private companion object {
        const val SIGNATURE_REJECTED = -1
    }
}
