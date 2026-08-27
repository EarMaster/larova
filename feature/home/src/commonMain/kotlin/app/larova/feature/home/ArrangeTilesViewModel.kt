package app.larova.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.larova.core.domain.model.parseUuidOrNull
import app.larova.core.domain.usecase.ObserveBoardTiles
import app.larova.core.domain.usecase.ObserveHomeTiles
import app.larova.core.domain.usecase.ReorderTiles
import app.larova.core.domain.usecase.moved
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The arrange screen reads the same flow the grid does, so a move is confirmed by the stored order
 * coming back rather than by the screen assuming it worked. If a write fails, the row visibly does
 * not move — which is the correct thing for it to do.
 */
class ArrangeTilesViewModel(
    boardId: String,
    observeHomeTiles: ObserveHomeTiles,
    observeBoardTiles: ObserveBoardTiles,
    private val reorderTiles: ReorderTiles,
) : ViewModel() {

    /** Null is the start screen, which is found rather than named. */
    private val board = parseUuidOrNull(boardId)

    val tiles: StateFlow<List<HomeTile>> = (board?.let(observeBoardTiles::invoke) ?: observeHomeTiles())
        .map { list -> list.map { it.toHomeTile() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList(),
        )

    fun onMoveUp(index: Int) = move(from = index, to = index - 1)

    fun onMoveDown(index: Int) = move(from = index, to = index + 1)

    private fun move(from: Int, to: Int) {
        val current = tiles.value
        val reordered = current.moved(from, to)
        // Unchanged means the move was off the end of the list, which the disabled buttons should
        // already have prevented. Not worth a write, and not worth an error either.
        if (reordered == current) return

        val ids = reordered.mapNotNull { parseUuidOrNull(it.id) }
        if (ids.size != reordered.size) return

        viewModelScope.launch { reorderTiles(ids, board) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
