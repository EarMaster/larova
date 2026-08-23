package app.larova.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.usecase.EnsureRootBoard
import app.larova.core.domain.usecase.ObserveHomeTiles
import app.larova.core.domain.usecase.Tile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * `isLoading` is not decoration. An empty grid and a grid that has not loaded yet look identical,
 * and "Nothing here yet" shown for a moment to someone whose tiles are about to appear is a small
 * lie the app can avoid telling.
 */
data class HomeUiState(
    val tiles: List<HomeTile> = emptyList(),
    val isLoading: Boolean = true,
)

class HomeViewModel(
    private val ensureRootBoard: EnsureRootBoard,
    observeHomeTiles: ObserveHomeTiles,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            // Before collecting: without a start screen there is nowhere for the first tile to go,
            // and the empty state would be describing a board that does not exist.
            ensureRootBoard()
        }
        viewModelScope.launch {
            observeHomeTiles().collect { tiles ->
                _state.value = HomeUiState(tiles = tiles.map { it.toHomeTile() }, isLoading = false)
            }
        }
    }
}

private fun Tile.toHomeTile() = HomeTile(
    id = card.id.toString(),
    title = card.title,
    colorToken = card.colorToken,
    symbolKey = card.icon,
    subtitle = subtitle(),
)

/**
 * What the parents wrote wins. Only when they wrote nothing does the tile describe itself, and only
 * for the two types where a count says something useful — "3 steps" helps, "a note" does not.
 */
private fun Tile.subtitle(): TileSubtitle {
    val custom = card.subtitle?.takeIf { it.isNotBlank() }
    if (custom != null) return TileSubtitle.Custom(custom)

    return when (val payload = payload) {
        is CardPayload.Guide -> TileSubtitle.Steps(payload.steps.size)
        is CardPayload.Checklist -> TileSubtitle.Items(payload.items.size)
        else -> TileSubtitle.None
    }
}
