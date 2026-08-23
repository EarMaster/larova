package app.larova.feature.card.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.CheckItem
import app.larova.core.domain.model.Step
import app.larova.core.domain.model.isOpenableUrl
import app.larova.core.domain.model.parseUuidOrNull
import app.larova.core.domain.usecase.CardDraft
import app.larova.core.domain.usecase.DeleteCard
import app.larova.core.domain.usecase.ObserveTile
import app.larova.core.domain.usecase.SaveCard
import app.larova.core.domain.usecase.Tile
import app.larova.core.ui.icon.TileSymbol
import app.larova.core.ui.theme.TileColor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What the editor holds while a parent is typing.
 *
 * The fields of all five tile types sit side by side rather than in a sealed hierarchy per type.
 * That is deliberate: someone who starts a note, changes their mind and picks "guide" should not
 * lose the title and colour they already chose, and switching back should still find the note.
 * Only the fields belonging to the chosen type are read when saving.
 */
data class EditUiState(
    val isNew: Boolean = true,
    val type: CardType = CardType.GUIDE,
    val title: String = "",
    val subtitle: String = "",
    val colorToken: String = TileColor.DEFAULT.key,
    val symbolKey: String = TileSymbol.DEFAULT.key,
    val steps: List<String> = listOf(""),
    val noteText: String = "",
    val items: List<CheckItem> = listOf(CheckItem("")),
    val resetDaily: Boolean = false,
    val callName: String = "",
    val callNumber: String = "",
    val callRelation: String = "",
    val callInHelpSheet: Boolean = false,
    val webUrl: String = "",
    val webLabel: String = "",
    val titleMissing: Boolean = false,
    val urlInvalid: Boolean = false,
    val isLoading: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
)

/**
 * The five types M1 can create. Tables, media, app shortcuts and folders arrive in M2 — offering a
 * type that cannot be filled in yet would be worse than not offering it.
 */
val EDITABLE_TYPES = listOf(
    CardType.GUIDE,
    CardType.CHECKLIST,
    CardType.NOTE,
    CardType.PHONE,
    CardType.WEB,
)

class EditCardViewModel(
    private val cardId: String?,
    private val observeTile: ObserveTile,
    private val saveCard: SaveCard,
    private val deleteCard: DeleteCard,
) : ViewModel() {

    private val _state = MutableStateFlow(EditUiState(isNew = cardId.isNullOrEmpty()))
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    init {
        if (!cardId.isNullOrEmpty()) load(cardId)
    }

    fun onTitleChange(value: String) = _state.update { it.copy(title = value, titleMissing = false) }

    fun onSubtitleChange(value: String) = _state.update { it.copy(subtitle = value) }

    fun onColorChange(token: String) = _state.update { it.copy(colorToken = token) }

    fun onSymbolChange(key: String) = _state.update { it.copy(symbolKey = key) }

    fun onNoteChange(value: String) = _state.update { it.copy(noteText = value) }

    fun onResetDailyChange(value: Boolean) = _state.update { it.copy(resetDaily = value) }

    fun onCallNameChange(value: String) = _state.update { it.copy(callName = value) }

    fun onCallNumberChange(value: String) = _state.update { it.copy(callNumber = value) }

    fun onCallRelationChange(value: String) = _state.update { it.copy(callRelation = value) }

    fun onCallInHelpSheetChange(value: Boolean) = _state.update { it.copy(callInHelpSheet = value) }

    fun onWebLabelChange(value: String) = _state.update { it.copy(webLabel = value) }

    fun onWebUrlChange(value: String) = _state.update { it.copy(webUrl = value, urlInvalid = false) }

    /** Offered while creating only: changing the type of a filled-in tile would discard content. */
    fun onTypeChange(type: CardType) = _state.update { it.copy(type = type) }

    fun onStepChange(index: Int, text: String) = _state.update { state ->
        state.copy(steps = state.steps.mapIndexed { i, existing -> if (i == index) text else existing })
    }

    fun onAddStep() = _state.update { it.copy(steps = it.steps + "") }

    fun onRemoveStep(index: Int) = _state.update { state ->
        val remaining = state.steps.filterIndexed { i, _ -> i != index }
        // Never down to nothing: a guide screen with no steps has nothing to show, and an editor
        // with no rows gives no way to start typing again.
        state.copy(steps = remaining.ifEmpty { listOf("") })
    }

    fun onItemChange(index: Int, text: String) = _state.update { state ->
        state.copy(
            items = state.items.mapIndexed { i, item -> if (i == index) item.copy(text = text) else item },
        )
    }

    fun onAddItem() = _state.update { it.copy(items = it.items + CheckItem("")) }

    fun onRemoveItem(index: Int) = _state.update { state ->
        val remaining = state.items.filterIndexed { i, _ -> i != index }
        state.copy(items = remaining.ifEmpty { listOf(CheckItem("")) })
    }

    fun onSave() {
        val current = _state.value
        if (current.title.isBlank()) {
            _state.update { it.copy(titleMissing = true) }
            return
        }
        // Checked here as well as when opening, so a tile cannot be saved in a state where tapping
        // it would do nothing at all.
        if (current.type == CardType.WEB && !isOpenableUrl(current.webUrl)) {
            _state.update { it.copy(urlInvalid = true) }
            return
        }

        viewModelScope.launch {
            val result = saveCard(
                CardDraft(
                    id = parseUuidOrNull(cardId),
                    title = current.title,
                    subtitle = current.subtitle,
                    colorToken = current.colorToken,
                    icon = current.symbolKey,
                    payload = current.toPayload(),
                ),
            )
            _state.update { state ->
                when (result) {
                    is SaveCard.Result.Saved -> state.copy(saved = true)
                    SaveCard.Result.TitleMissing -> state.copy(titleMissing = true)
                    // No start screen to write to. Nothing the parent could do about that, and
                    // staying on the editor at least keeps what they typed.
                    SaveCard.Result.NoBoard -> state
                }
            }
        }
    }

    fun onDelete() {
        val id = parseUuidOrNull(cardId) ?: return
        viewModelScope.launch {
            deleteCard(id)
            _state.update { it.copy(deleted = true) }
        }
    }

    private fun load(id: String) {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val tile = observeTile(id)
            _state.value = if (tile == null) {
                // Deleted from under the editor, or never a tile. Treated as gone rather than as a
                // new tile, so saving cannot resurrect it with half its content.
                EditUiState(isNew = false, isLoading = false, deleted = true)
            } else {
                tile.toEditState()
            }
        }
    }
}

/**
 * Blank lines are dropped on the way in. A parent who taps "add step" twice and fills one of them
 * meant one step, and an empty step in a guide is a screen that says nothing.
 */
private fun EditUiState.toPayload(): CardPayload = when (type) {
    CardType.GUIDE -> CardPayload.Guide(
        steps = steps.map { it.trim() }.filter { it.isNotEmpty() }.map { Step(text = it) },
    )

    CardType.CHECKLIST -> CardPayload.Checklist(
        items = items.map { it.copy(text = it.text.trim()) }.filter { it.text.isNotEmpty() },
        resetDaily = resetDaily,
    )

    CardType.NOTE -> CardPayload.Note(text = noteText.trim())

    CardType.PHONE -> CardPayload.Phone(
        displayName = callName.trim().ifEmpty { title.trim() },
        number = callNumber.trim(),
        relation = callRelation.trim().takeIf { it.isNotEmpty() },
        inHelpSheet = callInHelpSheet,
    )

    CardType.WEB -> CardPayload.Web(
        url = webUrl.trim(),
        label = webLabel.trim().takeIf { it.isNotEmpty() },
    )

    // Not offered by the picker, so not reachable — but a `when` without a branch for them would
    // stop compiling the day one is added, which is exactly when this needs revisiting.
    CardType.TABLE, CardType.VIDEO, CardType.AUDIO, CardType.APP_LINK, CardType.FOLDER ->
        CardPayload.Note(text = noteText.trim())
}

/** Existing content into editor fields. Ticked items keep their state; editing text is not undoing. */
private fun Tile.toEditState(): EditUiState {
    val base = EditUiState(
        isNew = false,
        type = card.type,
        title = card.title,
        subtitle = card.subtitle.orEmpty(),
        colorToken = card.colorToken,
        symbolKey = card.icon,
        isLoading = false,
    )
    return when (val payload = payload) {
        is CardPayload.Guide -> base.copy(
            steps = payload.steps.map { it.text }.ifEmpty { listOf("") },
        )

        is CardPayload.Checklist -> base.copy(
            items = payload.items.ifEmpty { listOf(CheckItem("")) },
            resetDaily = payload.resetDaily,
        )

        is CardPayload.Note -> base.copy(noteText = payload.text)

        is CardPayload.Phone -> base.copy(
            callName = payload.displayName,
            callNumber = payload.number,
            callRelation = payload.relation.orEmpty(),
            callInHelpSheet = payload.inHelpSheet,
        )

        is CardPayload.Web -> base.copy(
            webUrl = payload.url,
            webLabel = payload.label.orEmpty(),
        )

        else -> base
    }
}
