package app.larova.core.domain.model

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * A screen of tiles. A null [parentId] is the start screen, and one level of folders below it is
 * all the information architecture allows: someone searching under pressure should not have to
 * navigate (docs/concept.md §4.1).
 *
 * Not `@Serializable`: the file format has its own row types in `export/ExportRows.kt`, for the
 * reason [Card] gives.
 */
@OptIn(ExperimentalUuidApi::class)
data class Board(
    val id: Uuid,
    val parentId: Uuid? = null,
    val title: String,
    val sortIndex: Int,
    val updatedAt: Instant,
)
