package app.larova.core.domain.model

import app.larova.core.domain.serialization.InstantSerializer
import app.larova.core.domain.serialization.UuidSerializer
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * A screen of tiles. A null [parentId] is the start screen, and one level of folders below it is
 * all the information architecture allows: someone searching under pressure should not have to
 * navigate (docs/concept.md §4.1).
 */
@OptIn(ExperimentalUuidApi::class)
@Serializable
data class Board(
    @Serializable(with = UuidSerializer::class) val id: Uuid,
    @Serializable(with = UuidSerializer::class) val parentId: Uuid? = null,
    val title: String,
    val sortIndex: Int,
    @Serializable(with = InstantSerializer::class) val updatedAt: Instant,
)
