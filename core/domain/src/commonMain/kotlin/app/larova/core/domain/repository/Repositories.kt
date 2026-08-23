package app.larova.core.domain.repository

import app.larova.core.domain.model.AppearanceSetting
import app.larova.core.domain.model.Board
import app.larova.core.domain.model.Card
import app.larova.core.domain.model.LogEntry
import app.larova.core.domain.model.MediaAsset
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow

/**
 * The contracts the data layer implements and the use cases depend on.
 *
 * They live in `:core:domain` so that the direction of the arrow is fixed: UI reads state from a
 * ViewModel, the ViewModel calls use cases, use cases call these. Nothing above ever reaches into
 * the data layer, and nothing here knows what a database or a file system is.
 */
@OptIn(ExperimentalUuidApi::class)
interface BoardRepository {
    /** The start screen, which is the board with no parent. Created on first run if absent. */
    fun observeRootBoard(): Flow<Board?>

    fun observeChildren(parentId: Uuid?): Flow<List<Board>>

    suspend fun find(id: Uuid): Board?

    suspend fun upsert(board: Board)

    suspend fun delete(id: Uuid)
}

@OptIn(ExperimentalUuidApi::class)
interface CardRepository {
    fun observeCards(boardId: Uuid): Flow<List<Card>>

    /** Search runs over titles and subtitles only. Payload content is never interpreted. */
    fun search(query: String): Flow<List<Card>>

    suspend fun find(id: Uuid): Card?

    suspend fun upsert(card: Card)

    /** Persists a whole board's order in one write, so a reorder cannot end up half applied. */
    suspend fun reorder(boardId: Uuid, orderedIds: List<Uuid>)

    suspend fun delete(id: Uuid)
}

@OptIn(ExperimentalUuidApi::class)
interface MediaRepository {
    fun observeAll(): Flow<List<MediaAsset>>

    suspend fun find(id: Uuid): MediaAsset?

    suspend fun register(asset: MediaAsset)

    suspend fun delete(id: Uuid)

    /** Removes files no card refers to any more. Media outlives the tile that introduced it. */
    suspend fun deleteOrphans(): Int
}

interface LogRepository {
    fun observeRecent(limit: Int): Flow<List<LogEntry>>

    suspend fun append(entry: LogEntry)

    /** Retention is 30 days by default, adjustable, and applied on the way in as well as out. */
    suspend fun pruneOlderThanDays(days: Int)

    suspend fun clear()
}

/**
 * Settings that are not content: appearance, retention, the parent-view PIN. Deliberately separate
 * from the database, since none of it belongs in an export of a family's tiles.
 */
interface PreferencesRepository {
    fun observeAppearance(): Flow<AppearanceSetting>

    suspend fun setAppearance(setting: AppearanceSetting)
}
