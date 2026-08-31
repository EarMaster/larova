package app.larova.core.domain.usecase

import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.Entitlement
import app.larova.core.domain.repository.EntitlementRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * The tile types that are sold.
 *
 * One list, in the domain, because the editor, the settings screen and the store listing all have
 * to agree about it and three copies would not. Changing this set changes what is for sale, so it
 * changes the store listing and `docs/pages/privacy.md` with it.
 *
 * Guides, notes, checklists and tables are not here and should not be: they are what the app is
 * for. Neither is `PHONE` — a tile that dials a grandparent is the one thing a caregiver may need
 * in a hurry, and putting that behind a payment would be indefensible. `FOLDER` is absent because
 * it is already limited by shape rather than by price.
 */
val PAID_TILE_TYPES: Set<CardType> = setOf(
    CardType.APP_LINK,
    CardType.VIDEO,
    CardType.AUDIO,
)

/**
 * What vouches for this installation, if anything.
 *
 * Read by the settings screen, which has to say something true about it, and by anything that needs
 * the reason rather than just the answer.
 */
class ObserveEntitlement(private val entitlements: EntitlementRepository) {
    operator fun invoke(): Flow<Entitlement> = entitlements.observe()
}

/**
 * The types the editor must present as locked. Empty the moment anything has vouched for the
 * installation, so an unlocked build never carries a list of things to grey out.
 *
 * Phrased as "locked" rather than "available" on purpose: the editor shows these and disables them,
 * because a person who cannot see what buying would get them has no reason to buy it. That is the
 * opposite of how a nested folder is handled, and the difference is deliberate — see the comment on
 * `editableTypes`.
 */
class ObserveLockedTypes(private val entitlements: EntitlementRepository) {
    operator fun invoke(): Flow<Set<CardType>> =
        entitlements.observe().map { if (it.unlocked) emptySet() else PAID_TILE_TYPES }
}

/**
 * Asks the store again.
 *
 * Called on start and after a purchase returns. Failure is not an outcome worth reporting to
 * anyone: on a phone that has been offline for a fortnight it is the normal answer, and it leaves
 * whatever was already known exactly as it was.
 */
class RefreshEntitlement(private val entitlements: EntitlementRepository) {
    suspend operator fun invoke() = entitlements.refresh()
}

/**
 * What the unlock costs, for the sheet that offers it.
 *
 * Null is a normal answer and the screen has to cope with it: a phone that cannot reach the store
 * still shows the offer, just without a number on the button. Refusing to show the offer at all
 * would leave somebody who tapped a locked tile with no explanation.
 */
class UnlockPrice(private val entitlements: EntitlementRepository) {
    suspend operator fun invoke(): String? = entitlements.formattedPrice()
}
