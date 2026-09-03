package app.larova.core.billing

import android.app.Activity

/**
 * The repeatable contribution: buying it unlocks nothing and can be done again.
 *
 * Separate from [PlayEntitlementRepository] on purpose. That class answers one question — may this
 * installation author the paid tile types — and a contribution has no bearing on it. Folding the
 * two together would put "how many times somebody said thank you" next to the thing that decides
 * what the editor offers, and those must not be able to influence each other.
 *
 * How many times it has been bought is **not** kept here. Play forgets a consumed purchase, so the
 * count cannot come from the store; it is a local tally the caller keeps. That means it does not
 * survive a reinstall, which is the honest cost of the product being repeatable at all.
 */
class SupportPurchases(
    private val billing: PlayBilling,
    private val verifier: PurchaseVerifier,
) {

    /** Play's own price, or null if the store could not be asked. Never a number written here. */
    suspend fun price(): String? = billing.offer(SUPPORT_PRODUCT_ID)?.formattedPrice

    /**
     * Opens Play's sheet, and consumes what comes back so it can be bought again.
     *
     * The receipt is verified even though nothing is unlocked by it. The tally is only a thank-you,
     * but counting a purchase that Play did not sign would make the number a lie, and a number
     * shown to somebody who paid ought to be true.
     */
    suspend fun contribute(activity: Activity): PurchaseOutcome {
        val outcome = billing.purchase(activity, SUPPORT_PRODUCT_ID, repeatable = true)
        if (outcome is PurchaseOutcome.Purchased && !verifier.verify(outcome.receipt)) {
            return PurchaseOutcome.Unavailable(SIGNATURE_REJECTED)
        }
        return outcome
    }

    /**
     * Consumes a contribution left owned by an earlier run.
     *
     * Called at start-up. A consume that never landed would otherwise make every later purchase
     * fail with ITEM_ALREADY_OWNED, and somebody willing to give twice would find they could not.
     */
    suspend fun clearLeftovers() = billing.clearLeftovers(SUPPORT_PRODUCT_ID)

    /** Play has no code for "we did not sign that", so this one is the app's own. */
    private companion object {
        const val SIGNATURE_REJECTED = -1
    }
}
