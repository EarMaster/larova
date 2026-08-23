package app.larova.core.domain

import app.larova.core.domain.model.Board
import app.larova.core.domain.model.Card
import app.larova.core.domain.repository.BoardRepository
import app.larova.core.domain.repository.CardRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * In-memory stand-ins for the two repositories the use cases need.
 *
 * They keep their contents in a `MutableStateFlow`, so a test can assert that observing reacts to a
 * write rather than only that the write happened.
 */
@OptIn(ExperimentalUuidApi::class)
class FakeBoardRepository(initial: List<Board> = emptyList()) : BoardRepository {

    val boards = MutableStateFlow(initial)
    var upsertCount = 0
        private set

    override fun observeRootBoard(): Flow<Board?> =
        boards.map { list -> list.firstOrNull { it.parentId == null } }

    override fun observeChildren(parentId: Uuid?): Flow<List<Board>> =
        boards.map { list -> list.filter { it.parentId == parentId } }

    override suspend fun find(id: Uuid): Board? = boards.value.firstOrNull { it.id == id }

    override suspend fun upsert(board: Board) {
        upsertCount++
        boards.value = boards.value.filterNot { it.id == board.id } + board
    }

    override suspend fun delete(id: Uuid) {
        boards.value = boards.value.filterNot { it.id == id }
    }
}

@OptIn(ExperimentalUuidApi::class)
class FakeCardRepository(initial: List<Card> = emptyList()) : CardRepository {

    val cards = MutableStateFlow(initial)

    override fun observeCards(boardId: Uuid): Flow<List<Card>> =
        cards.map { list -> list.filter { it.boardId == boardId }.sortedBy { it.sortIndex } }

    override fun observeAllCards(): Flow<List<Card>> =
        cards.map { list -> list.sortedBy { it.sortIndex } }

    override fun search(query: String): Flow<List<Card>> =
        cards.map { list ->
            list.filter {
                it.title.contains(query, ignoreCase = true) ||
                    it.subtitle?.contains(query, ignoreCase = true) == true
            }
        }

    override suspend fun find(id: Uuid): Card? = cards.value.firstOrNull { it.id == id }

    override suspend fun upsert(card: Card) {
        cards.value = cards.value.filterNot { it.id == card.id } + card
    }

    override suspend fun reorder(boardId: Uuid, orderedIds: List<Uuid>) {
        cards.value = cards.value.map { card ->
            val index = orderedIds.indexOf(card.id)
            if (card.boardId == boardId && index >= 0) card.copy(sortIndex = index) else card
        }
    }

    override suspend fun delete(id: Uuid) {
        cards.value = cards.value.filterNot { it.id == id }
    }
}
