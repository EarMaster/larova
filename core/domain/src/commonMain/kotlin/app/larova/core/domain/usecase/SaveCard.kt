package app.larova.core.domain.usecase

import app.larova.core.domain.model.Board
import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.cardType
import app.larova.core.domain.repository.BoardRepository
import app.larova.core.domain.repository.CardRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first

/**
 * What the editor has in hand: a tile that may or may not exist yet.
 *
 * A null [id] means a new tile. Everything else is what the parent typed, including the two keys —
 * the editor picks a colour token and a symbol key, never a colour or a drawing.
 */
@OptIn(ExperimentalUuidApi::class)
data class CardDraft(
    val id: Uuid? = null,
    /**
     * Which board a **new** tile goes on. Null is the start screen, which is what the editor passes
     * unless it was opened from inside a folder. An existing tile keeps the board it is already on:
     * saving a tile is not a way to move it.
     */
    val boardId: Uuid? = null,
    val title: String,
    val subtitle: String? = null,
    val colorToken: String,
    val icon: String,
    val payload: CardPayload,
    val visibleToCaregiver: Boolean = true,
)

/**
 * Reading a tile, writing it, and removing it — the three things an editor does to one.
 *
 * Grouped like [app.larova.core.domain.usecase.Pictures] and the rest: a constructor that lists
 * every use case one by one is a constructor nobody reads, and these three are never useful apart.
 */
class TileEditing(
    val observe: ObserveTile,
    val save: SaveCard,
    val delete: DeleteCard,
)

/**
 * Creates or updates a tile.
 *
 * The type is derived from the payload rather than passed alongside it, so the column and the JSON
 * discriminator cannot disagree — a row claiming to be a checklist with a guide inside it would
 * survive every test and fail on the screen of whoever opened it.
 *
 * A new tile goes to the end of the board. Anything else would move the tiles a caregiver has
 * already learned the position of.
 */
class SaveCard(
    private val cards: CardRepository,
    private val boards: BoardRepository,
) {

    sealed interface Result {
        @OptIn(ExperimentalUuidApi::class)
        data class Saved(val id: Uuid) : Result

        /** A tile with no title is unreadable in the grid, which is the only place it appears. */
        data object TitleMissing : Result

        /** No start screen to put it on. Should not happen; refusing beats writing an orphan row. */
        data object NoBoard : Result
    }

    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(draft: CardDraft): Result {
        val title = draft.title.trim()
        if (title.isEmpty()) return Result.TitleMissing

        val existing = draft.id?.let { cards.find(it) }
        val boardId = existing?.boardId
            ?: draft.boardId
            ?: boards.observeRootBoard().first()?.id
            ?: return Result.NoBoard

        val card = Card(
            id = existing?.id ?: Uuid.random(),
            boardId = boardId,
            title = title,
            subtitle = draft.subtitle?.trim()?.takeIf { it.isNotEmpty() },
            icon = draft.icon,
            colorToken = draft.colorToken,
            sortIndex = existing?.sortIndex ?: nextSortIndex(boardId),
            visibleToCaregiver = draft.visibleToCaregiver,
            type = draft.payload.cardType,
            payload = CardPayloadCodec.encode(draft.payload),
            locale = existing?.locale,
            updatedAt = Clock.System.now(),
        )
        cards.upsert(card)
        return Result.Saved(card.id)
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun nextSortIndex(boardId: Uuid): Int =
        cards.observeCards(boardId).first().maxOfOrNull { it.sortIndex }?.plus(1) ?: 0
}

/**
 * Removes a tile, and — if it is a folder — everything inside it.
 *
 * A folder is the one tile that owns something other than itself. Deleting the tile and leaving its
 * board would leave the tiles on it in the database with no way to reach them: not on the start
 * screen, not in a folder, invisible everywhere but in a backup, and still counted in its tile
 * count. The screen says out loud how many go with it before this runs.
 *
 * The media the tiles referred to is not deleted here. Cleaning that up is a separate pass over
 * every remaining tile, because two tiles can point at the same picture and deleting one of them
 * must not blank the other.
 */
class DeleteCard(
    private val cards: CardRepository,
    private val boards: BoardRepository,
) {

    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(id: Uuid): Boolean {
        val card = cards.find(id) ?: return false

        val folder = CardPayloadCodec.decodeOrNull(card.payload) as? CardPayload.Folder
        if (folder != null) {
            for (inside in cards.observeCards(folder.boardId).first()) {
                cards.delete(inside.id)
            }
            boards.delete(folder.boardId)
        }

        cards.delete(id)
        return true
    }
}

/**
 * Makes the board a new folder tile will open.
 *
 * The board has to exist before the payload can point at it, so this runs when the tile is saved
 * rather than when the type is picked — a parent who opens the editor, taps "folder" and then leaves
 * should not have left an empty board behind.
 *
 * One level deep: the new board hangs off the start screen, which is why this takes no parent. A
 * folder inside a folder is not something the information architecture allows (docs/concept.md
 * §4.1), and the editor does not offer the type when it was opened from inside one.
 *
 * The board carries the tile's title so a backup read by hand is legible, but nothing keeps the two
 * in step afterwards: the title a caregiver sees is the tile's, and renaming a folder has no reason
 * to write to a second row.
 */
class CreateFolderBoard(private val boards: BoardRepository) {

    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(title: String): Uuid? {
        val root = boards.observeRootBoard().first() ?: return null
        val siblings = boards.observeChildren(root.id).first()

        val board = Board(
            id = Uuid.random(),
            parentId = root.id,
            title = title,
            sortIndex = siblings.maxOfOrNull { it.sortIndex }?.plus(1) ?: 0,
            updatedAt = Clock.System.now(),
        )
        boards.upsert(board)
        return board.id
    }
}
