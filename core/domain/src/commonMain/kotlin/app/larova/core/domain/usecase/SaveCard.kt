package app.larova.core.domain.usecase

import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.cardType
import app.larova.core.domain.repository.BoardRepository
import app.larova.core.domain.repository.CardRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first

/**
 * What the editor has in hand: a tile that may or may not exist yet.
 *
 * A null [id] means a new tile. Everything else is what the parent typed, including the two keys —
 * the editor picks a colour token and a symbol key, never a colour or a drawing.
 */
@OptIn(ExperimentalUuidApi::class)
data class CardDraft(
    val id: Uuid? = null,
    val title: String,
    val subtitle: String? = null,
    val colorToken: String,
    val icon: String,
    val payload: CardPayload,
    val visibleToCaregiver: Boolean = true,
)

/**
 * Creates or updates a tile.
 *
 * The type is derived from the payload rather than passed alongside it, so the column and the JSON
 * discriminator cannot disagree — a row claiming to be a checklist with a guide inside it would
 * survive every test and fail on the screen of whoever opened it.
 *
 * A new tile goes to the end of the board. Anything else would move the tiles a caregiver has
 * already learned the position of.
 */
class SaveCard(
    private val cards: CardRepository,
    private val boards: BoardRepository,
) {

    sealed interface Result {
        @OptIn(ExperimentalUuidApi::class)
        data class Saved(val id: Uuid) : Result

        /** A tile with no title is unreadable in the grid, which is the only place it appears. */
        data object TitleMissing : Result

        /** No start screen to put it on. Should not happen; refusing beats writing an orphan row. */
        data object NoBoard : Result
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(draft: CardDraft): Result {
        val title = draft.title.trim()
        if (title.isEmpty()) return Result.TitleMissing

        val existing = draft.id?.let { cards.find(it) }
        val boardId = existing?.boardId
            ?: boards.observeRootBoard().first()?.id
            ?: return Result.NoBoard

        val card = Card(
            id = existing?.id ?: Uuid.random(),
            boardId = boardId,
            title = title,
            subtitle = draft.subtitle?.trim()?.takeIf { it.isNotEmpty() },
            icon = draft.icon,
            colorToken = draft.colorToken,
            sortIndex = existing?.sortIndex ?: nextSortIndex(boardId),
            visibleToCaregiver = draft.visibleToCaregiver,
            type = draft.payload.cardType,
            payload = CardPayloadCodec.encode(draft.payload),
            locale = existing?.locale,
            updatedAt = Clock.System.now(),
        )
        cards.upsert(card)
        return Result.Saved(card.id)
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun nextSortIndex(boardId: Uuid): Int =
        cards.observeCards(boardId).first().maxOfOrNull { it.sortIndex }?.plus(1) ?: 0
}

/**
 * Removes a tile.
 *
 * The media it referred to is not deleted here. Cleaning that up is a separate pass that looks at
 * every remaining tile, because two tiles can point at the same picture and deleting one of them
 * must not blank the other.
 */
class DeleteCard(private val cards: CardRepository) {

    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(id: Uuid): Boolean {
        if (cards.find(id) == null) return false
        cards.delete(id)
        return true
    }
}
