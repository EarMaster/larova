package app.larova.core.data.repository

import app.larova.core.data.db.BoardDao
import app.larova.core.data.db.CardDao
import app.larova.core.data.db.CardTextDao
import app.larova.core.data.db.LogDao
import app.larova.core.data.db.MediaDao
import app.larova.core.data.db.toDomainOrNull
import app.larova.core.data.db.toEntity
import app.larova.core.domain.model.Board
import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.CardText
import app.larova.core.domain.model.LogEntry
import app.larova.core.domain.model.MediaAsset
import app.larova.core.domain.model.referencedMediaIds
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
import kotlinx.coroutines.flow.map

/**
 * The repositories.
 *
 * Every one of them drops rows it cannot read (`mapNotNull` over `toDomainOrNull`) instead of
 * failing the flow. A single unreadable row — a tile type from a newer version, an identifier that
 * is not a UUID — costs that tile, not the screen it is on.
 */
@OptIn(ExperimentalUuidApi::class)
class RoomBoardRepository(private val dao: BoardDao) : BoardRepository {

    override fun observeRootBoard(): Flow<Board?> =
        dao.observeRoot().map { it?.toDomainOrNull() }

    override fun observeChildren(parentId: Uuid?): Flow<List<Board>> {
        val flow = if (parentId == null) dao.observeRootLevel() else dao.observeChildren(parentId.toString())
        return flow.map { rows -> rows.mapNotNull { it.toDomainOrNull() } }
    }

    override suspend fun find(id: Uuid): Board? = dao.find(id.toString())?.toDomainOrNull()

    override suspend fun all(): List<Board> = dao.all().mapNotNull { it.toDomainOrNull() }

    override suspend fun upsert(board: Board) = dao.upsert(board.toEntity())

    override suspend fun delete(id: Uuid) = dao.delete(id.toString())
}

@OptIn(ExperimentalUuidApi::class)
class RoomCardRepository(private val dao: CardDao) : CardRepository {

    override fun observeCards(boardId: Uuid): Flow<List<Card>> =
        dao.observeByBoard(boardId.toString()).map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override fun observeAllCards(): Flow<List<Card>> =
        dao.observeAll().map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override fun search(query: String): Flow<List<Card>> =
        dao.search(query).map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override suspend fun find(id: Uuid): Card? = dao.find(id.toString())?.toDomainOrNull()

    override suspend fun upsert(card: Card) = dao.upsert(card.toEntity())

    /**
     * The whole board's order in one pass, so a drag that is interrupted cannot leave two tiles
     * claiming the same position. Cards not named in [orderedIds] keep their index.
     */
    override suspend fun reorder(boardId: Uuid, orderedIds: List<Uuid>) {
        orderedIds.forEachIndexed { index, id ->
            dao.updateSortIndex(id.toString(), index)
        }
    }

    override suspend fun delete(id: Uuid) = dao.delete(id.toString())
}

@OptIn(ExperimentalUuidApi::class)
class RoomCardTextRepository(private val dao: CardTextDao) : CardTextRepository {

    override fun observeAll(): Flow<List<CardText>> =
        dao.observeAll().map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override fun observeForCard(cardId: Uuid): Flow<List<CardText>> =
        dao.observeForCard(cardId.toString()).map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override suspend fun all(): List<CardText> = dao.all().mapNotNull { it.toDomainOrNull() }

    override suspend fun upsert(text: CardText) = dao.upsert(text.toEntity())

    override suspend fun delete(cardId: Uuid, lang: String) = dao.delete(cardId.toString(), lang)

    override suspend fun deleteForCard(cardId: Uuid) = dao.deleteForCard(cardId.toString())
}

class RoomMediaRepository(
    private val mediaDao: MediaDao,
    private val cardDao: CardDao,
    private val cardTextDao: CardTextDao,
) : MediaRepository {

    override fun observeAll(): Flow<List<MediaAsset>> =
        mediaDao.observeAll().map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override suspend fun find(id: Uuid): MediaAsset? = mediaDao.find(id.toString())?.toDomainOrNull()

    override suspend fun register(asset: MediaAsset) = mediaDao.upsert(asset.toEntity())

    override suspend fun delete(id: Uuid) = mediaDao.delete(id.toString())

    /**
     * References live inside payload JSON, so the only way to find an orphan is to decode every
     * payload and see what is still pointed at. A payload this version cannot decode is treated as
     * referencing everything it might: unknown types are skipped for rendering, but their files are
     * not deleted underneath them.
     *
     * Deleting the row only. Removing the file on disk is the media store's job, and it runs from
     * the same list.
     */
    override suspend fun deleteOrphans(): Int {
        // Translations too, and this is not a detail: a translated guide may point at a different
        // picture — a sign photographed in the caregiver's own language — and a sweep that read
        // only the original's payload would find that file unreferenced and delete it. Silently,
        // permanently, and on the first sweep after somebody adds their first translation. A
        // picture can be taken again; the voice on it cannot.
        val payloads = cardDao.all().map { it.payload } + cardTextDao.all().map { it.payload }
        val undecodable = payloads.any { CardPayloadCodec.decodeOrNull(it) == null }
        if (undecodable) return 0

        val referenced: Set<Uuid> = payloads
            .mapNotNull { CardPayloadCodec.decodeOrNull(it) }
            .flatMapTo(mutableSetOf()) { it.referencedMediaIds }

        val orphans = mediaDao.all().filter { row ->
            row.toDomainOrNull()?.id?.let { it !in referenced } ?: true
        }
        orphans.forEach { mediaDao.delete(it.id) }
        return orphans.size
    }
}

class RoomLogRepository(private val dao: LogDao) : LogRepository {

    override fun observeRecent(limit: Int): Flow<List<LogEntry>> =
        dao.observeRecent(limit).map { rows -> rows.mapNotNull { it.toDomainOrNull() } }

    override suspend fun append(entry: LogEntry) = dao.upsert(entry.toEntity())

    override suspend fun pruneOlderThanDays(days: Int) {
        val cutoff = Clock.System.now() - days.days
        dao.deleteOlderThan(cutoff.toEpochMilliseconds())
    }

    override suspend fun clear() = dao.clear()
}
