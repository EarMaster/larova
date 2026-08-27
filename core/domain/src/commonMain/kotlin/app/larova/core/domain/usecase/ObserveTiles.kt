package app.larova.core.domain.usecase

import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.parseUuidOrNull
import app.larova.core.domain.repository.BoardRepository
import app.larova.core.domain.repository.CardRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * A card together with its decoded payload.
 *
 * Pairing them is the point: a tile whose payload cannot be read here has no business being
 * offered for opening, and the only way to know is to decode it.
 */
data class Tile(val card: Card, val payload: CardPayload)

/**
 * The tiles of the start screen, in their stored order.
 *
 * Tiles whose payload this version cannot decode are left out rather than shown and then failing
 * on the way in. They stay in the database untouched, so an export from here still carries them
 * and a later version can render them.
 */
class ObserveHomeTiles(
    private val boards: BoardRepository,
    private val cards: CardRepository,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<Tile>> =
        boards.observeRootBoard().flatMapLatest { board ->
            if (board == null) {
                flowOf(emptyList())
            } else {
                cards.observeCards(board.id).map { list -> list.mapNotNull { it.toTileOrNull() } }
            }
        }
}

/**
 * The tiles inside one folder.
 *
 * Separate from [ObserveHomeTiles] rather than one use case with a nullable board: the start screen
 * is found by having no parent and is created if it is missing, while a folder's board is named by
 * the tile that opens it and simply exists or does not. Collapsing the two would mean a folder whose
 * board went missing quietly showing the start screen instead.
 */
class ObserveBoardTiles(private val cards: CardRepository) {

    @OptIn(ExperimentalUuidApi::class)
    operator fun invoke(boardId: Uuid): Flow<List<Tile>> =
        cards.observeCards(boardId).map { list -> list.mapNotNull { it.toTileOrNull() } }
}

/**
 * Where a card screen gets its content: the tile itself, and — when that tile is a folder — the
 * tiles inside it.
 *
 * The two travel together because the screen cannot know which it needs until the payload is
 * decoded, and it decodes the payload by asking for the tile. Grouped like the export ports are, so
 * a ViewModel constructor stays short enough to read.
 */
class TileSource(
    val observe: ObserveTile,
    val onBoard: ObserveBoardTiles,
)

/** One tile by id, or null if it is gone or unreadable. */
class ObserveTile(private val cards: CardRepository) {

    suspend operator fun invoke(id: String): Tile? {
        val uuid = parseUuidOrNull(id) ?: return null
        return cards.find(uuid)?.toTileOrNull()
    }
}

internal fun Card.toTileOrNull(): Tile? =
    CardPayloadCodec.decodeOrNull(payload)?.let { Tile(this, it) }
