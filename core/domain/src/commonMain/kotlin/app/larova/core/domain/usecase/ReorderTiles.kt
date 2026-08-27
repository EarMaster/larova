package app.larova.core.domain.usecase

import app.larova.core.domain.repository.BoardRepository
import app.larova.core.domain.repository.CardRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first

/**
 * Writes a new order for a board.
 *
 * The whole order goes in one call rather than one swap at a time. A caregiver learns where things
 * are by position, so a half-applied rearrangement — two tiles claiming the same place, or an
 * order that depends on which write landed first — is worse than one that did not happen.
 *
 * A null board is the start screen. Naming it that way rather than looking it up at every call site
 * keeps the arrange screen from having to know how the start screen is found.
 */
class ReorderTiles(
    private val boards: BoardRepository,
    private val cards: CardRepository,
) {

    @OptIn(ExperimentalUuidApi::class)
    suspend operator fun invoke(orderedIds: List<Uuid>, boardId: Uuid? = null): Boolean {
        if (orderedIds.isEmpty()) return false
        val target = boardId ?: boards.observeRootBoard().first()?.id ?: return false
        cards.reorder(target, orderedIds)
        return true
    }
}

/**
 * The same element, one place further up or down.
 *
 * Pure, and separate from the writing, because this is the part with an off-by-one in it. Moving
 * the first tile up or the last one down returns the list unchanged rather than throwing: the
 * buttons that do it are disabled at the ends, and a stale screen must not be able to scramble
 * somebody's start screen.
 */
fun <T> List<T>.moved(from: Int, to: Int): List<T> {
    if (from !in indices || to !in indices || from == to) return this
    val mutable = toMutableList()
    mutable.add(to, mutable.removeAt(from))
    return mutable
}
