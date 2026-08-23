package app.larova.core.domain.usecase

import app.larova.core.domain.model.Board
import app.larova.core.domain.repository.BoardRepository
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first

/**
 * Makes sure there is a start screen to put tiles on, and returns it.
 *
 * Called on every launch rather than only on first run: the alternative is a flag somewhere that
 * says the board was created, and a flag that disagrees with the database leaves an installation
 * with nowhere to write. Checking is one query.
 *
 * The title is empty because the start screen has no name in the interface — the screen header is
 * a greeting, not a board title. Naming it here would mean inventing a user-facing string in the
 * domain layer, which is where hardcoded English starts.
 */
class EnsureRootBoard(private val boards: BoardRepository) {

    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(): Board {
        boards.observeRootBoard().first()?.let { return it }

        val board = Board(
            id = Uuid.random(),
            parentId = null,
            title = "",
            sortIndex = 0,
            updatedAt = Clock.System.now(),
        )
        boards.upsert(board)
        return board
    }
}
