package app.larova.core.domain.usecase

import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * One person to reach, as the help sheet needs them.
 *
 * Flattened out of the tile deliberately: the sheet is read under stress and shows a name, a
 * relation and a number. Nothing else about the tile matters at that moment.
 */
data class HelpContact(
    val cardId: String,
    val displayName: String,
    val number: String,
    val relation: String?,
)

/**
 * The numbers behind the help bar.
 *
 * Taken from the people the parents marked on their call tiles, in the order the tiles are in and
 * then the order within each tile, and capped. A tile can hold eight people and a board can hold
 * many tiles, so the cap is doing real work: a sheet with nine numbers on it is not a sheet anyone
 * reads in an emergency, and two or three is what docs/concept.md §4.3 describes.
 *
 * Tiles from any board, not just the start screen: a number worth reaching in a hurry might sit
 * inside a folder, and being one level deep must not put it out of reach.
 */
class ObserveHelpContacts(private val cards: CardRepository) {

    operator fun invoke(): Flow<List<HelpContact>> = cards.observeAllCards().map { list ->
        list.asSequence()
            .flatMap { card ->
                val phone = CardPayloadCodec.decodeOrNull(card.payload) as? CardPayload.Phone
                phone?.people.orEmpty().asSequence()
                    .filter { it.inHelpSheet && it.number.isNotBlank() }
                    .map { entry ->
                        HelpContact(
                            cardId = card.id.toString(),
                            // The tile's own title if the person has no name of their own: the
                            // parents called the tile something, and that is what the caregiver
                            // has seen. On a tile holding several people this is rarer than it
                            // was, but a single-person tile is still the common case.
                            displayName = entry.displayName.ifBlank { card.title },
                            number = entry.number,
                            relation = entry.relation?.takeIf { it.isNotBlank() },
                        )
                    }
            }
            .take(MAX_CONTACTS)
            .toList()
    }

    companion object {
        const val MAX_CONTACTS = 4
    }
}
