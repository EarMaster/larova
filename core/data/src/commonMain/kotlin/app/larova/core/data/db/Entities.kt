package app.larova.core.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * The database rows.
 *
 * They are deliberately not the domain models. `:core:domain` has no platform dependencies, and
 * these carry Room annotations, foreign keys and primitive column types. The mapping between the
 * two is a few dozen lines in Mappers.kt and it buys a layer boundary that holds.
 *
 * Two column choices are compatibility decisions rather than preferences:
 *
 * `type` and `colorToken` are stored as the **key strings**, not as enum ordinals. An ordinal
 * shifts the moment a value is inserted into the middle of an enum, silently turning every stored
 * tile into a different one. The key never moves.
 *
 * `type` is a plain string rather than a validated enum so that a tile written by a newer version
 * survives a round trip through this one: it is skipped when rendering, but it is not deleted, and
 * an export from here still carries it.
 */
@Entity(
    tableName = "boards",
    indices = [Index("parentId"), Index("sortIndex")],
)
data class BoardEntity(
    @PrimaryKey val id: String,
    val parentId: String?,
    val title: String,
    val sortIndex: Int,
    val updatedAtEpochMillis: Long,
)

@Entity(
    tableName = "cards",
    foreignKeys = [
        ForeignKey(
            entity = BoardEntity::class,
            parentColumns = ["id"],
            childColumns = ["boardId"],
            // Deleting a folder deletes the tiles on the board behind it. The alternative is rows
            // no screen can reach, which is how a backup ends up bigger than the app.
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("boardId"), Index("boardId", "sortIndex"), Index("title")],
)
data class CardEntity(
    @PrimaryKey val id: String,
    val boardId: String,
    val title: String,
    val subtitle: String?,
    val icon: String,
    val colorToken: String,
    val sortIndex: Int,
    val visibleToCaregiver: Boolean,
    val type: String,
    val payload: String,
    val locale: String?,
    val updatedAtEpochMillis: Long,
)

@Entity(tableName = "media", indices = [Index(value = ["sha256"])])
data class MediaAssetEntity(
    @PrimaryKey val id: String,
    val relativePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,
)

@Entity(tableName = "log", indices = [Index("atEpochMillis"), Index("cardId")])
data class LogEntryEntity(
    @PrimaryKey val id: String,
    val atEpochMillis: Long,
    val kind: String,
    /**
     * Not a foreign key. A log line saying a tile was opened stays true after the tile is deleted,
     * and losing the history would be the opposite of what the log is for.
     */
    val cardId: String?,
    val note: String?,
)
