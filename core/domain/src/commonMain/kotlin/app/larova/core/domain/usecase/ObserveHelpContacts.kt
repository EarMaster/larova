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
 * Taken from the call tiles the parents marked, in the order the tiles are in, and capped. A sheet
 * with nine numbers on it is not a sheet anyone reads in an emergency; two or three is what
 * docs/concept.md §4.3 describes, and the cap makes the promise true even if somebody marks
 * everything.
 *
 * Tiles from any board, not just the start screen: a number worth reaching in a hurry might sit
 * inside a folder, and being one level deep must not put it out of reach.
 */
class ObserveHelpContacts(private val cards: CardRepository) {

    operator fun invoke(): Flow<List<HelpContact>> = cards.observeAllCards().map { list ->
        list.asSequence()
            .mapNotNull { card ->
                val phone = CardPayloadCodec.decodeOrNull(card.payload) as? CardPayload.Phone
                phone?.takeIf { it.inHelpSheet && it.number.isNotBlank() }?.let { payload ->
                    HelpContact(
                        cardId = card.id.toString(),
                        // The tile's own title if the payload has no name of its own: the parents
                        // called the tile something, and that is what the caregiver has seen.
                        displayName = payload.displayName.ifBlank { card.title },
                        number = payload.number,
                        relation = payload.relation?.takeIf { it.isNotBlank() },
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
