package app.larova.feature.card

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.parseUuidOrNull
import app.larova.core.domain.usecase.ObserveTile
import app.larova.core.domain.usecase.ToggleChecklistItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * [missing] covers three cases that look the same to the person holding the phone: the tile was
 * deleted, the identifier in the back stack is not a tile at all, and the payload was written by a
 * version that knows a type this one does not. All three end with "this is not here", and none of
 * them should end with a crash on a screen someone opened while looking for help.
 */
data class CardUiState(
    val title: String = "",
    val colorToken: String = "",
    val payload: CardPayload? = null,
    val isLoading: Boolean = true,
    val missing: Boolean = false,
)

class CardViewModel(
    private val cardId: String,
    private val observeTile: ObserveTile,
    private val toggleChecklistItem: ToggleChecklistItem,
) : ViewModel() {

    private val _state = MutableStateFlow(CardUiState())
    val state: StateFlow<CardUiState> = _state.asStateFlow()

    init {
        reload()
    }

    fun onToggleItem(index: Int) {
        val id = parseUuidOrNull(cardId) ?: return
        viewModelScope.launch {
            // Ticking an item is the one write a caregiver can make. Reload rather than mutate the
            // state here: the stored payload is the truth, and a tick that failed must not leave a
            // checkbox looking as though it succeeded.
            toggleChecklistItem(id, index)
            reload()
        }
    }

    private fun reload() {
        viewModelScope.launch {
            val tile = observeTile(cardId)
            _state.value = if (tile == null) {
                CardUiState(isLoading = false, missing = true)
            } else {
                CardUiState(
                    title = tile.card.title,
                    colorToken = tile.card.colorToken,
                    payload = tile.payload,
                    isLoading = false,
                )
            }
        }
    }
}
