package app.larova.feature.settings

/**
 * What the full-version card is doing, and what the last look at the store found.
 *
 * The card asks Google Play, and asking takes a moment on a phone that has to wake the Play app
 * up first. Without a state to show, a tap did nothing visible and then — in the common case,
 * where there is nothing to find — went on doing nothing visible, which is indistinguishable from
 * a card that is not a button at all. So the wait is shown, and the empty answer is shown too.
 *
 * [Idle] covers both "not asked yet" and "asked, and it found the purchase": the entitlement
 * itself is what says the second one, on the card, and a second announcement of the same fact
 * would be one too many.
 */
sealed interface UnlockCheck {

    /** Nothing to say. Either nobody has asked, or the answer was the unlock and the card shows it. */
    data object Idle : UnlockCheck

    /** The store is being asked. Ends in [Idle] or in [NotFound]; it cannot hang about. */
    data object Checking : UnlockCheck

    /**
     * The store was asked and had nothing for this account — or could not be asked at all, which
     * from here looks the same and is worth the same offer.
     *
     * @param price as Google Play writes it for this buyer's country, or null when nobody could be
     *   asked. The offer is still made without a number on the button — see `UnlockPrice`.
     * @param message what happened the last time somebody tried to buy from this dialog, if
     *   anything did.
     */
    data class NotFound(
        val price: String? = null,
        val message: UnlockMessage? = null,
    ) : UnlockCheck
}

/**
 * The two purchase outcomes the dialog has to say something about.
 *
 * A cancelled purchase is not among them, the same way it is not on the locked tile's offer:
 * somebody who backed out has already seen their own decision. A completed one is not either —
 * the dialog closes and the card behind it reads "Unlocked".
 */
enum class UnlockMessage {
    /** Play took the order but the money has not arrived. Nothing is unlocked yet, and it may be. */
    PENDING,

    /** Play could not be reached, or refused. */
    UNAVAILABLE,
}
