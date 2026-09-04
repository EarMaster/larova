package app.larova.core.domain.export

import app.larova.core.domain.model.Board
import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.LogEntry
import app.larova.core.domain.model.LogKind
import app.larova.core.domain.model.MediaAsset
import app.larova.core.domain.serialization.InstantSerializer
import app.larova.core.domain.serialization.UuidSerializer
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * The rows as they are written to `content.json`, separate from the models the app works with.
 *
 * The container used to serialize the domain models directly, and that is precisely how it came to
 * write `"type": "GUIDE"` — the Kotlin constant name — while the database column and the payload
 * discriminator both said `guide`. A format that borrows its vocabulary from whatever the enum
 * constants happen to be called is pinned to identifier spelling that nothing pins back: renaming
 * `APP_LINK` would have changed the format of every existing file with nothing to catch it.
 *
 * So the file gets its own row types, and [type] and [kind] are plain `String`s. Two things follow,
 * and both were the point:
 *
 * 1. A value this build does not recognise can no longer fail the decode. It becomes one row that
 *    [toDomainOrNull] declines, which the caller counts and the screen can explain — rather than an
 *    entire backup reported as "not a Larova backup".
 * 2. Adding a field to [Card] no longer silently changes what a backup contains. Restating the
 *    field names here is duplication, and it is the duplication you want at a format boundary.
 *
 * This mirrors `core/data/.../db/Mappers.kt`, which has always solved the same problem one layer
 * down: a row whose `kind` column is unrecognised is declined by `LogEntryEntity.toDomainOrNull()`
 * and skipped. Same function name, same guard clauses, same failure semantics — the file boundary
 * now reads like the database boundary, so there is nothing new to explain.
 */
@Serializable
@OptIn(ExperimentalUuidApi::class)
data class ExportBoard(
    @Serializable(with = UuidSerializer::class) val id: Uuid,
    @Serializable(with = UuidSerializer::class) val parentId: Uuid? = null,
    val title: String,
    val sortIndex: Int,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

@Serializable
@OptIn(ExperimentalUuidApi::class)
data class ExportCard(
    @Serializable(with = UuidSerializer::class) val id: Uuid,
    @Serializable(with = UuidSerializer::class) val boardId: Uuid,
    val title: String,
    val subtitle: String? = null,
    val icon: String,
    val colorToken: String,
    val sortIndex: Int,
    val visibleToCaregiver: Boolean = true,
    /** The frozen key, e.g. `guide`. A `String`, so an unfamiliar one costs one tile, not the file. */
    val type: String,
    val payload: String,
    val locale: String? = null,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)

@Serializable
@OptIn(ExperimentalUuidApi::class)
data class ExportMediaAsset(
    @Serializable(with = UuidSerializer::class) val id: Uuid,
    val relativePath: String,
    val mimeType: String,
    val sizeBytes: Long,
    val sha256: String,
)

@Serializable
@OptIn(ExperimentalUuidApi::class)
data class ExportLogEntry(
    @Serializable(with = UuidSerializer::class) val id: Uuid,
    @Serializable(with = InstantSerializer::class) val at: Instant,
    /** The frozen key, e.g. `cardOpened`. Same reasoning as [ExportCard.type]. */
    val kind: String,
    @Serializable(with = UuidSerializer::class) val cardId: Uuid? = null,
    val note: String? = null,
)

// --- file to app -------------------------------------------------------------------------------

@OptIn(ExperimentalUuidApi::class)
internal fun ExportBoard.toDomain() = Board(
    id = id,
    parentId = parentId,
    title = title,
    sortIndex = sortIndex,
    updatedAt = updatedAt,
)

/** Null for a tile type this build has never heard of. The caller counts those and says so. */
@OptIn(ExperimentalUuidApi::class)
internal fun ExportCard.toDomainOrNull(): Card? {
    val resolved = cardTypeOrNull(type) ?: return null
    return Card(
        id = id,
        boardId = boardId,
        title = title,
        subtitle = subtitle,
        icon = icon,
        colorToken = colorToken,
        sortIndex = sortIndex,
        visibleToCaregiver = visibleToCaregiver,
        type = resolved,
        payload = payload,
        locale = locale,
        updatedAt = updatedAt,
    )
}

@OptIn(ExperimentalUuidApi::class)
internal fun ExportMediaAsset.toDomain() = MediaAsset(
    id = id,
    relativePath = relativePath,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    sha256 = sha256,
)

/** Null for a log kind this build has never heard of, same as the tile above. */
@OptIn(ExperimentalUuidApi::class)
internal fun ExportLogEntry.toDomainOrNull(): LogEntry? {
    val resolved = logKindOrNull(kind) ?: return null
    return LogEntry(id = id, at = at, kind = resolved, cardId = cardId, note = note)
}

// --- app to file -------------------------------------------------------------------------------

@OptIn(ExperimentalUuidApi::class)
internal fun Board.toExport() = ExportBoard(
    id = id,
    parentId = parentId,
    title = title,
    sortIndex = sortIndex,
    updatedAt = updatedAt,
)

@OptIn(ExperimentalUuidApi::class)
internal fun Card.toExport() = ExportCard(
    id = id,
    boardId = boardId,
    title = title,
    subtitle = subtitle,
    icon = icon,
    colorToken = colorToken,
    sortIndex = sortIndex,
    visibleToCaregiver = visibleToCaregiver,
    type = type.key,
    payload = payload,
    locale = locale,
    updatedAt = updatedAt,
)

@OptIn(ExperimentalUuidApi::class)
internal fun MediaAsset.toExport() = ExportMediaAsset(
    id = id,
    relativePath = relativePath,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    sha256 = sha256,
)

@OptIn(ExperimentalUuidApi::class)
internal fun LogEntry.toExport() =
    ExportLogEntry(id = id, at = at, kind = kind.key, cardId = cardId, note = note)

// --- reading both spellings --------------------------------------------------------------------

/**
 * The frozen key first, then the Kotlin constant name.
 *
 * Files written by `0.1.0` through `0.4.2` spell tile types as constant names (`GUIDE`, `APP_LINK`),
 * because the container serialized the enum directly. Those files are out there and may be a
 * family's only copy, so the reader accepts both spellings and always will. Both are therefore
 * frozen, and `ModelKeysTest` pins the constant names as well as the keys so a rename is a red test
 * rather than a silently broken reader.
 *
 * Deliberately here rather than on `CardType.Companion`: the database column has never held `GUIDE`
 * and `Mappers.kt` must keep refusing it. This leniency belongs to the file format alone.
 *
 * Not gated on `schemaVersion` either. A version-conditional parser is the thing that rots, and
 * accepting a constant name in a v2 file costs nothing.
 */
private fun cardTypeOrNull(wire: String): CardType? =
    CardType.fromKey(wire) ?: CardType.entries.firstOrNull { it.name == wire }

private fun logKindOrNull(wire: String): LogKind? =
    LogKind.fromKey(wire) ?: LogKind.entries.firstOrNull { it.name == wire }
