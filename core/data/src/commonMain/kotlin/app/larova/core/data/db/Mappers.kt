package app.larova.core.data.db

import app.larova.core.domain.model.Board
import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.LogEntry
import app.larova.core.domain.model.LogKind
import app.larova.core.domain.model.MediaAsset
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Rows to models and back.
 *
 * The domain side uses `Uuid` and `Instant`; the database stores text and epoch milliseconds.
 * Doing the conversion here rather than with Room type converters keeps the boundary visible: this
 * is the file to read when asking what the database actually holds.
 *
 * Anything a stored row cannot be turned into returns null rather than throwing. A single
 * unreadable row — a tile type from a newer version, an identifier that is not a UUID — costs that
 * one tile, not the whole screen.
 */
@OptIn(ExperimentalUuidApi::class)
internal fun BoardEntity.toDomainOrNull(): Board? {
    val id = uuidOrNull(id) ?: return null
    return Board(
        id = id,
        parentId = parentId?.let { uuidOrNull(it) },
        title = title,
        sortIndex = sortIndex,
        updatedAt = Instant.fromEpochMilliseconds(updatedAtEpochMillis),
    )
}

@OptIn(ExperimentalUuidApi::class)
internal fun Board.toEntity() = BoardEntity(
    id = id.toString(),
    parentId = parentId?.toString(),
    title = title,
    sortIndex = sortIndex,
    updatedAtEpochMillis = updatedAt.toEpochMilliseconds(),
)

@OptIn(ExperimentalUuidApi::class)
internal fun CardEntity.toDomainOrNull(): Card? {
    val id = uuidOrNull(id) ?: return null
    val boardId = uuidOrNull(boardId) ?: return null
    // An unknown type is skipped for display but stays in the table, so an export from here still
    // carries the tile a newer version wrote.
    val type = CardType.fromKey(type) ?: return null
    return Card(
        id = id,
        boardId = boardId,
        title = title,
        subtitle = subtitle,
        icon = icon,
        colorToken = colorToken,
        sortIndex = sortIndex,
        visibleToCaregiver = visibleToCaregiver,
        type = type,
        payload = payload,
        locale = locale,
        updatedAt = Instant.fromEpochMilliseconds(updatedAtEpochMillis),
    )
}

@OptIn(ExperimentalUuidApi::class)
internal fun Card.toEntity() = CardEntity(
    id = id.toString(),
    boardId = boardId.toString(),
    title = title,
    subtitle = subtitle,
    icon = icon,
    colorToken = colorToken,
    sortIndex = sortIndex,
    visibleToCaregiver = visibleToCaregiver,
    type = type.key,
    payload = payload,
    locale = locale,
    updatedAtEpochMillis = updatedAt.toEpochMilliseconds(),
)

@OptIn(ExperimentalUuidApi::class)
internal fun MediaAssetEntity.toDomainOrNull(): MediaAsset? {
    val id = uuidOrNull(id) ?: return null
    return MediaAsset(
        id = id,
        relativePath = relativePath,
        mimeType = mimeType,
        sizeBytes = sizeBytes,
        sha256 = sha256,
    )
}

@OptIn(ExperimentalUuidApi::class)
internal fun MediaAsset.toEntity() = MediaAssetEntity(
    id = id.toString(),
    relativePath = relativePath,
    mimeType = mimeType,
    sizeBytes = sizeBytes,
    sha256 = sha256,
)

@OptIn(ExperimentalUuidApi::class)
internal fun LogEntryEntity.toDomainOrNull(): LogEntry? {
    val id = uuidOrNull(id) ?: return null
    val kind = LogKind.fromKey(kind) ?: return null
    return LogEntry(
        id = id,
        at = Instant.fromEpochMilliseconds(atEpochMillis),
        kind = kind,
        cardId = cardId?.let { uuidOrNull(it) },
        note = note,
    )
}

@OptIn(ExperimentalUuidApi::class)
internal fun LogEntry.toEntity() = LogEntryEntity(
    id = id.toString(),
    atEpochMillis = at.toEpochMilliseconds(),
    kind = kind.key,
    cardId = cardId?.toString(),
    note = note,
)

@OptIn(ExperimentalUuidApi::class)
private fun uuidOrNull(raw: String): Uuid? = try {
    Uuid.parse(raw)
} catch (_: IllegalArgumentException) {
    null
}
