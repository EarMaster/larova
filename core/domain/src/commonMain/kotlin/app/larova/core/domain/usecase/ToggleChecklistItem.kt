package app.larova.core.domain.usecase

import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.repository.CardRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Ticks or unticks one item of a checklist.
 *
 * A caregiver does this, which is why it is one of the few writes available outside parent view:
 * ticking off "teeth brushed" is reading the tile, not editing it. Nothing else about the tile can
 * be changed from here.
 *
 * Returns whether anything was written, so a caller cannot mistake "no such item" for success. An
 * index that no longer exists is a stale screen rather than a defect — the tile may have been
 * edited on the other side of the app — so it is answered with false rather than an exception.
 */
class ToggleChecklistItem(private val cards: CardRepository) {

    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(cardId: Uuid, itemIndex: Int): Boolean {
        val card = cards.find(cardId) ?: return false
        val payload = CardPayloadCodec.decodeOrNull(card.payload) as? CardPayload.Checklist
            ?: return false
        if (itemIndex !in payload.items.indices) return false

        val items = payload.items.toMutableList()
        val item = items[itemIndex]
        items[itemIndex] = item.copy(done = !item.done)

        cards.upsert(
            card.copy(
                payload = CardPayloadCodec.encode(payload.copy(items = items)),
                updatedAt = Clock.System.now(),
            ),
        )
        return true
    }
}
