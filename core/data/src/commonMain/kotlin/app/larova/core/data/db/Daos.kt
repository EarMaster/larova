package app.larova.core.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/**
 * Queries are verified by Room at compile time, which is most of the reason this layer is worth
 * the annotations: a column renamed in an entity fails the build rather than at runtime on
 * someone's phone.
 */
@Dao
interface BoardDao {

    /** The start screen. Exactly one board has no parent. */
    @Query("SELECT * FROM boards WHERE parentId IS NULL ORDER BY sortIndex LIMIT 1")
    fun observeRoot(): Flow<BoardEntity?>

    @Query("SELECT * FROM boards WHERE parentId = :parentId ORDER BY sortIndex")
    fun observeChildren(parentId: String): Flow<List<BoardEntity>>

    @Query("SELECT * FROM boards WHERE parentId IS NULL ORDER BY sortIndex")
    fun observeRootLevel(): Flow<List<BoardEntity>>

    @Query("SELECT * FROM boards WHERE id = :id")
    suspend fun find(id: String): BoardEntity?

    @Query("SELECT * FROM boards")
    suspend fun all(): List<BoardEntity>

    @Upsert
    suspend fun upsert(board: BoardEntity)

    @Query("DELETE FROM boards WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface CardDao {

    @Query("SELECT * FROM cards WHERE boardId = :boardId ORDER BY sortIndex")
    fun observeByBoard(boardId: String): Flow<List<CardEntity>>

    /**
     * Titles and subtitles only. Larova stores and displays; it does not read what is inside a
     * tile, which is both the regulatory line (docs/concept.md §2.2) and the reason search cannot
     * surprise anyone with a match from a note they had forgotten writing.
     */
    @Query(
        """
        SELECT * FROM cards
        WHERE title LIKE '%' || :query || '%' OR subtitle LIKE '%' || :query || '%'
        ORDER BY title
        LIMIT 100
        """,
    )
    fun search(query: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun find(id: String): CardEntity?

    @Query("SELECT * FROM cards")
    suspend fun all(): List<CardEntity>

    @Upsert
    suspend fun upsert(card: CardEntity)

    @Query("UPDATE cards SET sortIndex = :sortIndex WHERE id = :id")
    suspend fun updateSortIndex(id: String, sortIndex: Int)

    @Query("DELETE FROM cards WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface MediaDao {

    @Query("SELECT * FROM media")
    fun observeAll(): Flow<List<MediaAssetEntity>>

    @Query("SELECT * FROM media")
    suspend fun all(): List<MediaAssetEntity>

    @Query("SELECT * FROM media WHERE id = :id")
    suspend fun find(id: String): MediaAssetEntity?

    @Upsert
    suspend fun upsert(asset: MediaAssetEntity)

    @Query("DELETE FROM media WHERE id = :id")
    suspend fun delete(id: String)
}

@Dao
interface LogDao {

    @Query("SELECT * FROM log ORDER BY atEpochMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<LogEntryEntity>>

    @Upsert
    suspend fun upsert(entry: LogEntryEntity)

    @Query("DELETE FROM log WHERE atEpochMillis < :beforeEpochMillis")
    suspend fun deleteOlderThan(beforeEpochMillis: Long)

    @Query("DELETE FROM log")
    suspend fun clear()

    @Delete
    suspend fun delete(entry: LogEntryEntity)
}
