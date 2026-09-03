package app.larova.core.billing

import android.app.Activity
import android.content.Context
import app.larova.core.domain.model.Receipt
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The one-time product, as it is spelled in Play Console.
 *
 * Frozen the same way the application id is: a product id cannot be reused or renamed once anybody
 * has bought it, so this string outlives every other decision in this module.
 */
const val UNLOCK_PRODUCT_ID: String = "app.larova.unlock"

/**
 * The repeatable contribution. Unlocks nothing, and is *consumed* after each purchase so it can be
 * bought again — Play refuses a second purchase of something the app still owns.
 *
 * Frozen for the same reason as the unlock: a product id cannot be renamed or reused once anybody
 * has paid for it.
 */
const val SUPPORT_PRODUCT_ID: String = "app.larova.support"

/** What came back from asking somebody to buy something. */
sealed interface PurchaseOutcome {

    /** Bought and acknowledged. The receipt has already been checked before this is returned. */
    data class Purchased(val receipt: Receipt) : PurchaseOutcome

    /** They changed their mind. Not an error, and nothing should be shown about it. */
    data object Cancelled : PurchaseOutcome

    /**
     * Cash or a carrier bill: Play has taken the order but nobody has paid yet. The unlock arrives
     * later, through the start-up query, and the screen says so rather than pretending it worked.
     */
    data object Pending : PurchaseOutcome

    /** Already owned on this account. The caller treats this as success, not as a failure. */
    data object AlreadyOwned : PurchaseOutcome

    /** The store could not be reached, or the product is not on sale. [code] is Play's own. */
    data class Unavailable(val code: Int) : PurchaseOutcome
}

/** What the paywall may say about the price: Play's own formatting, in Play's currency. */
data class Offer(val formattedPrice: String)

/**
 * A thin, deliberate wrapper over the Play Billing client.
 *
 * Hand-rolled suspend functions rather than `billing-ktx`, because the listener signatures changed
 * in 9.x and a wrapper this small is easier to keep true than a dependency that lags behind it.
 *
 * Every call connects first, and every call answers "no" rather than throwing. This runs in an app
 * with no internet permission, on phones that are offline for weeks at a time, so "could not ask"
 * is the ordinary case here and not the exceptional one.
 *
 * Nothing in this class decides whether the app is unlocked. It reports what the store said; the
 * deciding happens in [PlayEntitlementRepository], which checks the signature first.
 */
class PlayBilling(context: Context) {

    /**
     * The listener is a single slot rather than a stream, because only one purchase is ever being
     * attempted: it is started by somebody tapping a button and finished by them leaving Play's
     * sheet. The mutex is what makes that "only one" true rather than merely likely.
     */
    private var awaiting: ((BillingResult, List<Purchase>?) -> Unit)? = null
    private val purchasing = Mutex()

    private val client: BillingClient = BillingClient.newBuilder(context)
        .setListener { result, purchases -> awaiting?.invoke(result, purchases) }
        // Required for one-time products since 6.x. Cash and carrier billing are common enough in
        // the fourteen listed locales that refusing them would be a decision, not a default.
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build(),
        )
        // Play drops the binding whenever it updates itself. Without this, every reconnect becomes
        // the app's problem, and the app without it is the one that stops working after an update.
        .enableAutoServiceReconnection()
        .build()

    /** Play's own price string, or null if it could not be asked. Never a number written here. */
    suspend fun offer(productId: String): Offer? {
        val details = productDetails(productId) ?: return null
        val price = bestOffer(details)?.formattedPrice
            ?: details.oneTimePurchaseOfferDetails?.formattedPrice
            ?: return null
        return Offer(price)
    }

    /**
     * What this Google account already owns.
     *
     * Null means "could not tell" — offline, no Play, a sideloaded build. It does **not** mean "not
     * bought", and the caller must not read it that way. A successful but empty answer comes back
     * as null for the same reason: a phone signed into a different account than the one that paid
     * would otherwise silently lose the unlock, and that is a worse failure than a refund kept.
     */
    suspend fun ownedPurchase(productId: String): Receipt? {
        if (!connect()) return null
        val purchase = suspendCancellableCoroutine<Purchase?> { continuation ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            client.queryPurchasesAsync(params) { result, purchases ->
                val owned = if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases.firstOrNull { isPurchased(it, productId) }
                } else {
                    null
                }
                if (continuation.isActive) continuation.resume(owned)
            }
        } ?: return null
        acknowledge(purchase)
        return purchase.receipt()
    }

    /**
     * Opens Play's sheet and waits for it to close.
     *
     * Takes the Activity as a parameter and keeps no reference to one, for the same reason
     * `BiometricUnlock` lives in `:app`: the Activity belongs to the caller.
     */
    suspend fun purchase(
        activity: Activity,
        productId: String,
        /**
         * True for anything meant to be bought again. Play will not sell a product the app still
         * owns, so a repeatable contribution has to be consumed; a permanent unlock must not be,
         * or it would come back for sale.
         */
        repeatable: Boolean,
    ): PurchaseOutcome {
        if (!connect()) {
            return PurchaseOutcome.Unavailable(
                BillingClient.BillingResponseCode.SERVICE_DISCONNECTED,
            )
        }
        val details = productDetails(productId) ?: return PurchaseOutcome.Unavailable(
            BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
        )
        return purchasing.withLock { launchAndAwait(activity, details, productId, repeatable) }
    }

    private suspend fun launchAndAwait(
        activity: Activity,
        details: ProductDetails,
        productId: String,
        repeatable: Boolean,
    ): PurchaseOutcome = suspendCancellableCoroutine { continuation ->
        awaiting = { result, purchases ->
            awaiting = null
            if (continuation.isActive) {
                continuation.resume(outcomeOf(result, purchases, productId, repeatable))
            }
        }
        continuation.invokeOnCancellation { awaiting = null }

        val launch = client.launchBillingFlow(activity, flowParams(details))
        if (launch.responseCode != BillingClient.BillingResponseCode.OK) {
            awaiting = null
            if (continuation.isActive) {
                continuation.resume(PurchaseOutcome.Unavailable(launch.responseCode))
            }
        }
    }

    private fun flowParams(details: ProductDetails): BillingFlowParams {
        val product = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .apply {
                // One-time products gained offer tokens in 8.x. Details in the older shape have
                // none, and setting a null token is an error rather than a no-op, so it is only
                // set when there is one.
                // The same offer the price came from. Picking separately is how an app ends up
                // advertising one amount and charging another.
                bestOffer(details)?.offerToken?.let(::setOfferToken)
            }
            .build()
        return BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(product))
            .build()
    }

    private fun outcomeOf(
        result: BillingResult,
        purchases: List<Purchase>?,
        productId: String,
        repeatable: Boolean,
    ): PurchaseOutcome =
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                val bought = purchases.orEmpty().firstOrNull { productId in it.products }
                when {
                    bought == null -> PurchaseOutcome.Cancelled
                    bought.purchaseState == Purchase.PurchaseState.PENDING -> PurchaseOutcome.Pending
                    isPurchased(bought, productId) -> {
                        // Settled inside the listener rather than after it: a purchase that is
                        // never consumed cannot be bought again, and a consumable that is never
                        // acknowledged is refunded after three days. `consumeAsync` does both.
                        settle(bought, repeatable)
                        PurchaseOutcome.Purchased(bought.receipt())
                    }
                    else -> PurchaseOutcome.Unavailable(result.responseCode)
                }
            }

            BillingClient.BillingResponseCode.USER_CANCELED -> PurchaseOutcome.Cancelled
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> PurchaseOutcome.AlreadyOwned
            else -> PurchaseOutcome.Unavailable(result.responseCode)
        }

    /**
     * The cheapest offer on a product, which is the one the buyer should get.
     *
     * A product carries one offer per purchase option plus one for every discount running on it,
     * so as soon as anything is on sale the list has more than one entry and `first()` is a
     * coin toss — it could show the sale price and charge full, or the reverse. Lowest price is
     * both self-consistent and the answer nobody has to apologise for.
     *
     * Falls back to the singular `oneTimePurchaseOfferDetails` at the call sites, for details in
     * the pre-8.x shape that carry no offer list at all.
     */
    private fun bestOffer(details: ProductDetails): ProductDetails.OneTimePurchaseOfferDetails? =
        details.oneTimePurchaseOfferDetailsList
            .orEmpty()
            .minByOrNull { it.priceAmountMicros }

    private suspend fun productDetails(productId: String): ProductDetails? {
        if (!connect()) return null
        return suspendCancellableCoroutine { continuation ->
            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(
                    listOf(
                        QueryProductDetailsParams.Product.newBuilder()
                            .setProductId(productId)
                            .setProductType(BillingClient.ProductType.INAPP)
                            .build(),
                    ),
                )
                .build()
            client.queryProductDetailsAsync(params) { result, queried ->
                val details = if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queried.productDetailsList.firstOrNull { it.productId == productId }
                } else {
                    null
                }
                if (continuation.isActive) continuation.resume(details)
            }
        }
    }

    /**
     * Consume it or acknowledge it, and never neither.
     *
     * Both settle a purchase; the difference is what happens next. Consuming gives the product back
     * to the shelf so it can be bought again, which is the whole point of the contribution.
     * Acknowledging leaves it owned, which is what a permanent unlock needs. `consumeAsync`
     * acknowledges as a side effect, so a consumed purchase is never at risk of the three-day
     * automatic refund.
     *
     * Fire and forget, because the listener that calls this is not a coroutine. A consume that
     * fails is picked up by [clearLeftovers] on the next start.
     */
    private fun settle(purchase: Purchase, repeatable: Boolean) {
        if (repeatable) {
            val params = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client.consumeAsync(params) { _, _ -> }
        } else if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client.acknowledgePurchase(params) { }
        }
    }

    /**
     * Consumes anything of [productId] still owned from an earlier run.
     *
     * Without this, one consume that did not land — the process died, the phone went offline
     * between paying and consuming — would leave the product owned for good, and Play would refuse
     * every later purchase of it with ITEM_ALREADY_OWNED. Somebody who wanted to contribute twice
     * would simply be unable to, with nothing on screen to explain why.
     */
    suspend fun clearLeftovers(productId: String) {
        if (!connect()) return
        val owned = suspendCancellableCoroutine<List<Purchase>> { continuation ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build()
            client.queryPurchasesAsync(params) { result, purchases ->
                val hits = if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    purchases.filter { isPurchased(it, productId) }
                } else {
                    emptyList()
                }
                if (continuation.isActive) continuation.resume(hits)
            }
        }
        owned.forEach { settle(it, repeatable = true) }
    }

    /**
     * An unacknowledged one-time purchase is refunded automatically after three days, so this is
     * not optional, and it is done from both paths: the purchase itself, and the start-up query —
     * the app can be closed between paying and being told about it.
     */
    private suspend fun acknowledge(purchase: Purchase) {
        if (purchase.isAcknowledged) return
        suspendCancellableCoroutine { continuation ->
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            client.acknowledgePurchase(params) {
                // The result is deliberately not acted on. A failed acknowledgement is retried by
                // the next start-up query, which is a better answer than a dialog nobody can act on.
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
    }

    private fun isPurchased(purchase: Purchase, productId: String): Boolean =
        productId in purchase.products &&
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED

    private fun Purchase.receipt() = Receipt(payload = originalJson, signature = signature)

    private suspend fun connect(): Boolean {
        if (client.isReady) return true
        return suspendCancellableCoroutine { continuation ->
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (continuation.isActive) {
                        val ok = result.responseCode == BillingClient.BillingResponseCode.OK
                        continuation.resume(ok)
                    }
                }

                override fun onBillingServiceDisconnected() {
                    // Also arrives long after a successful setup, which is why this is guarded
                    // rather than assumed to be the first answer.
                    if (continuation.isActive) continuation.resume(false)
                }
            })
        }
    }
}
