package app.larova.feature.card.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.parseUuidOrNull
import app.larova.core.domain.model.plainTextOf
import app.larova.core.domain.model.textFieldsOf
import app.larova.core.domain.model.withTextFields
import app.larova.core.domain.usecase.DeleteCardText
import app.larova.core.domain.usecase.SaveCardText
import app.larova.core.domain.usecase.TileSource
import app.larova.core.domain.usecase.Translations
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi

/**
 * One tile, in one other language.
 *
 * [fields] is the tile's words in order — the steps of a guide, the items of a checklist, the cells
 * of a table — and nothing else. Not the colour, not the symbol, not the pictures, not the phone
 * numbers: none of those are translated, and offering them would let a variant drift into being a
 * different tile.
 *
 * A new language starts as a **copy of the original**, not as blank fields. Somebody translating
 * needs to see what they are translating, and an empty form gives them nothing to work from; the
 * Translate button hands these same words to a translation app so the answer can be pasted back
 * field by field. Nothing is auto-filled and nothing is parsed — Larova never reads the clipboard.
 */
data class EditTranslationUiState(
    /** The language's own name for itself, for the title bar. */
    val languageName: String = "",
    val title: String = "",
    val subtitle: String = "",
    val fields: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val missing: Boolean = false,
    /** True once there is a stored translation to remove, so a new one offers no delete. */
    val exists: Boolean = false,
    val titleMissing: Boolean = false,
    val saved: Boolean = false,
    /** These words, ready to hand to a translation app. Empty when nothing can take them. */
    val handOffText: String = "",
    val canTranslate: Boolean = false,
)

/** Which tile, and which language. Both come from the route and neither means anything alone. */
data class TranslationTarget(val cardId: String, val lang: String)

@OptIn(ExperimentalUuidApi::class)
class EditTranslationViewModel(
    target: TranslationTarget,
    private val tiles: TileSource,
    private val translations: Translations,
    private val saveCardText: SaveCardText,
    private val deleteCardText: DeleteCardText,
) : ViewModel() {

    private val cardId = target.cardId
    private val lang = target.lang

    private val _state = MutableStateFlow(EditTranslationUiState())
    val state: StateFlow<EditTranslationUiState> = _state.asStateFlow()

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val tile = tiles.observe(cardId)
            if (tile == null) {
                _state.value = EditTranslationUiState(isLoading = false, missing = true)
                return@launch
            }
            val existing = translations.textsFor(tile.card.id).first().firstOrNull { it.lang == lang }
            val payload = existing?.let { CardPayloadCodec.decodeOrNull(it.payload) } ?: tile.payload
            val title = existing?.title ?: tile.card.title
            val subtitle = existing?.subtitle ?: tile.card.subtitle.orEmpty()

            _state.value = EditTranslationUiState(
                languageName = translations.nameOf(lang),
                title = title,
                subtitle = subtitle,
                fields = textFieldsOf(payload),
                isLoading = false,
                exists = existing != null,
                canTranslate = translations.isAvailable(),
                handOffText = plainTextOf(title, subtitle.ifBlank { null }, payload),
            )
        }
    }

    fun onTitleChange(value: String) =
        _state.update { it.copy(title = value, titleMissing = false) }

    fun onSubtitleChange(value: String) = _state.update { it.copy(subtitle = value) }

    fun onFieldChange(index: Int, value: String) = _state.update { current ->
        if (index !in current.fields.indices) {
            current
        } else {
            current.copy(fields = current.fields.toMutableList().also { it[index] = value })
        }
    }

    /**
     * Saves this language.
     *
     * The payload is built by putting the edited words back into **the original's** structure, so
     * the variant is the same kind of tile with the same shape whatever was typed. The stored
     * refusals in `SaveCardText` are the second line of defence rather than the first.
     */
    fun onSave() {
        val current = _state.value
        if (current.title.isBlank()) {
            _state.update { it.copy(titleMissing = true) }
            return
        }
        viewModelScope.launch {
            val id = parseUuidOrNull(cardId) ?: return@launch
            val tile = tiles.observe(cardId) ?: return@launch
            val payload = withTextFields(tile.payload, current.fields)
            val result = saveCardText(
                cardId = id,
                lang = lang,
                title = current.title,
                subtitle = current.subtitle.takeIf { it.isNotBlank() },
                payload = CardPayloadCodec.encode(payload),
            )
            _state.update {
                when (result) {
                    is SaveCardText.Result.Saved -> it.copy(saved = true)
                    SaveCardText.Result.TitleMissing -> it.copy(titleMissing = true)
                    // Nothing a person can act on, and nothing they did: the tile went, or the file
                    // this came from was already inconsistent. Leaving the screen is the answer.
                    else -> it.copy(saved = true)
                }
            }
        }
    }

    fun onDelete() {
        viewModelScope.launch {
            parseUuidOrNull(cardId)?.let { deleteCardText(it, lang) }
            _state.update { it.copy(saved = true) }
        }
    }
}
