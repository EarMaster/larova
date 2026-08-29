package app.larova.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.usecase.ApplyTemplate
import app.larova.core.domain.usecase.CardDraft
import app.larova.core.domain.usecase.EnsureRootBoard
import app.larova.core.domain.usecase.ObserveHomeTiles
import app.larova.core.domain.usecase.SearchTiles
import app.larova.core.domain.usecase.Tile
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * `isLoading` is not decoration. An empty grid and a grid that has not loaded yet look identical,
 * and "Nothing here yet" shown for a moment to someone whose tiles are about to appear is a small
 * lie the app can avoid telling.
 */
data class HomeUiState(
    val tiles: List<HomeTile> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = true,
) {
    val isSearching: Boolean get() = query.isNotBlank()
}

class HomeViewModel(
    ensureRootBoard: EnsureRootBoard,
    observeHomeTiles: ObserveHomeTiles,
    searchTiles: SearchTiles,
    private val applyTemplate: ApplyTemplate,
) : ViewModel() {

    private val query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HomeUiState> = query
        .flatMapLatest { text ->
            // Searching replaces the grid rather than filtering it in place, so the ordered start
            // screen is never shown in an order the parents did not choose.
            val source = if (text.isBlank()) observeHomeTiles() else searchTiles(text)
            source.map { tiles ->
                HomeUiState(
                    tiles = tiles.map { it.toHomeTile() },
                    query = text,
                    isLoading = false,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HomeUiState(),
        )

    init {
        // Without a start screen there is nowhere for the first tile to go, and the empty state
        // would be describing a board that does not exist.
        viewModelScope.launch { ensureRootBoard() }
    }

    fun onQueryChange(text: String) {
        query.value = text
    }

    fun onClearQuery() {
        query.value = ""
    }

    /**
     * Writes a template onto the start screen.
     *
     * The draft arrives finished, because the words in it are string resources the screen has
     * already resolved: a template is written in the language the app is in at that moment, and
     * belongs to the parents from then on rather than following the setting.
     */
    fun onUseTemplate(draft: CardDraft) {
        viewModelScope.launch { applyTemplate(listOf(draft)) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}

internal fun Tile.toHomeTile() = HomeTile(
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
        is CardPayload.Phone -> TileSubtitle.Numbers(payload.people.size)
        is CardPayload.Phone -> TileSubtitle.Numbers(payload.people.size)
        is CardPayload.Folder -> TileSubtitle.Folder
        else -> TileSubtitle.None
    }
}
