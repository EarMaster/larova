package app.larova.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.CardText
import app.larova.core.domain.model.resolveCardText
import app.larova.core.domain.usecase.ApplyTemplate
import app.larova.core.domain.usecase.CardDraft
import app.larova.core.domain.usecase.EnsureRootBoard
import app.larova.core.domain.usecase.ObserveHomeTiles
import app.larova.core.domain.usecase.SearchTiles
import app.larova.core.domain.usecase.Tile
import app.larova.core.domain.usecase.Translations
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
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
    private val translations: Translations,
    private val applyTemplate: ApplyTemplate,
) : ViewModel() {

    private val query = MutableStateFlow("")

    @OptIn(ExperimentalCoroutinesApi::class)
    val state: StateFlow<HomeUiState> = query
        .flatMapLatest { text ->
            // Searching replaces the grid rather than filtering it in place, so the ordered start
            // screen is never shown in an order the parents did not choose.
            val source = if (text.isBlank()) observeHomeTiles() else searchTiles(text)
            // Resolved here as well as on the tile screen, and from the same two flows, so the
            // grid and the tile a tap opens can never disagree about which language they are in.
            // Both watch the preference, so choosing a language on one tile redraws the grid
            // behind it without either being told about the other.
            combine(source, translations.allTexts(), translations.language()) { tiles, texts, lang ->
                HomeUiState(
                    tiles = tiles.map { it.toHomeTile(texts, lang) },
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

/**
 * A grid tile, in the language this phone is reading tiles in.
 *
 * The colour and the symbol are the tile's own whichever language it is read in — they are keys,
 * not words, and a caregiver learns the grid by shape and colour before they read any of it. Only
 * the words change.
 *
 * A tile with no translation for that language keeps what the parents wrote. **Nothing marks it**:
 * most tiles in most installations are untranslated, and a badge on each would be a grid of badges
 * saying "incomplete" about tiles that are nothing of the kind.
 */
internal fun Tile.toHomeTile(): HomeTile =
    HomeTile(
        id = card.id.toString(),
        title = card.title,
        colorToken = card.colorToken,
        symbolKey = card.icon,
        subtitle = subtitle(card.subtitle, payload),
    )

/**
 * The same tile, in the language this phone is reading tiles in.
 *
 * Two functions rather than a default argument, because the two callers want genuinely different
 * things and a default would let one of them get the other's answer by omission. The start screen
 * resolves; the arrange screen does not — rearranging is parent-view work on the tiles as the
 * parent wrote them, and the editor and `SaveCard` work on the original everywhere else too.
 */
internal fun Tile.toHomeTile(texts: List<CardText>, language: String): HomeTile {
    val shown = resolveCardText(card, texts, language)
    return HomeTile(
        id = card.id.toString(),
        title = shown.title,
        colorToken = card.colorToken,
        symbolKey = card.icon,
        // From the resolved payload, so a variant with fewer steps counts its own.
        subtitle = subtitle(shown.subtitle, CardPayloadCodec.decodeOrNull(shown.payload) ?: payload),
    )
}

/**
 * What the parents wrote wins. Only when they wrote nothing does the tile describe itself, and only
 * for the two types where a count says something useful — "3 steps" helps, "a note" does not.
 */
private fun subtitle(custom: String?, payload: CardPayload): TileSubtitle {
    val written = custom?.takeIf { it.isNotBlank() }
    if (written != null) return TileSubtitle.Custom(written)

    return when (payload) {
        is CardPayload.Guide -> TileSubtitle.Steps(payload.steps.size)
        is CardPayload.Checklist -> TileSubtitle.Items(payload.items.size)
        is CardPayload.Phone -> TileSubtitle.Numbers(payload.people.size)
        is CardPayload.Folder -> TileSubtitle.Folder
        else -> TileSubtitle.None
    }
}
