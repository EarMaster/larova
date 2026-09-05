package app.larova.core.domain

import app.larova.core.domain.model.Board
import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardText
import app.larova.core.domain.model.LogEntry
import app.larova.core.domain.model.MediaAsset
import app.larova.core.domain.repository.BoardRepository
import app.larova.core.domain.repository.CardRepository
import app.larova.core.domain.repository.CardTextRepository
import app.larova.core.domain.repository.LogRepository
import app.larova.core.domain.repository.MediaRepository
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
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

    override suspend fun all(): List<Board> = boards.value

    override suspend fun upsert(board: Board) {
        upsertCount++
        boards.value = boards.value.filterNot { it.id == board.id } + board
    }

    override suspend fun delete(id: Uuid) {
        boards.value = boards.value.filterNot { it.id == id }
    }
}

/**
 * Variants, keyed the way the table is.
 *
 * The cascade is modelled rather than assumed: SQLite removes a tile's variants when the tile goes,
 * and a fake that did not would let a test pass on a wipe that left translations behind.
 */
@OptIn(ExperimentalUuidApi::class)
class FakeCardTextRepository(initial: List<CardText> = emptyList()) : CardTextRepository {

    val texts = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<CardText>> = texts

    override fun observeForCard(cardId: Uuid): Flow<List<CardText>> =
        texts.map { list -> list.filter { it.cardId == cardId } }

    override suspend fun all(): List<CardText> = texts.value

    override suspend fun upsert(text: CardText) {
        texts.value = texts.value.filterNot { it.cardId == text.cardId && it.lang == text.lang } +
            text
    }

    override suspend fun delete(cardId: Uuid, lang: String) {
        texts.value = texts.value.filterNot { it.cardId == cardId && it.lang == lang }
    }

    override suspend fun deleteForCard(cardId: Uuid) {
        texts.value = texts.value.filterNot { it.cardId == cardId }
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

@OptIn(ExperimentalUuidApi::class)
class FakeMediaRepository(initial: List<MediaAsset> = emptyList()) : MediaRepository {

    val assets = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<MediaAsset>> = assets

    override suspend fun find(id: Uuid): MediaAsset? = assets.value.firstOrNull { it.id == id }

    override suspend fun register(asset: MediaAsset) {
        assets.value = assets.value.filterNot { it.id == asset.id } + asset
    }

    override suspend fun delete(id: Uuid) {
        assets.value = assets.value.filterNot { it.id == id }
    }

    /**
     * Which rows no tile refers to any more.
     *
     * The real one answers that by decoding every payload. A test says it outright instead: what is
     * being checked here is what happens to the files afterwards, not the walk that finds them.
     */
    var orphans: Set<Uuid> = emptySet()

    override suspend fun deleteOrphans(): Int {
        val removed = assets.value.filter { it.id in orphans }
        assets.value = assets.value - removed.toSet()
        return removed.size
    }
}

/**
 * The log, in memory and newest first.
 *
 * `observeRecent` sorts rather than trusting insertion order, because the real one is an ORDER BY
 * and an import writes entries in whatever order the file had them.
 */
class FakeLogRepository(initial: List<LogEntry> = emptyList()) : LogRepository {

    val entries = MutableStateFlow(initial)

    override fun observeRecent(limit: Int): Flow<List<LogEntry>> =
        entries.map { list -> list.sortedByDescending { it.at }.take(limit) }

    override suspend fun append(entry: LogEntry) {
        entries.value = entries.value.filterNot { it.id == entry.id } + entry
    }

    override suspend fun pruneOlderThanDays(days: Int) {
        val cutoff = Clock.System.now() - days.days
        entries.value = entries.value.filter { it.at >= cutoff }
    }

    override suspend fun clear() {
        entries.value = emptyList()
    }
}
