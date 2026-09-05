package app.larova.feature.card

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.larova.core.domain.media.ImageSize
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.CardText
import app.larova.core.domain.model.parseUuidOrNull
import app.larova.core.domain.model.plainTextOf
import app.larova.core.domain.model.resolveCardText
import app.larova.core.domain.usecase.Apps
import app.larova.core.domain.usecase.Media
import app.larova.core.domain.usecase.RecordEvent
import app.larova.core.domain.usecase.Tile
import app.larova.core.domain.usecase.TileSource
import app.larova.core.domain.usecase.ToggleChecklistItem
import app.larova.core.domain.usecase.Translations
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
    /** The tiles inside this one, when it is a folder. Empty for every other type. */
    val folderTiles: List<FolderTile> = emptyList(),
    /**
     * The board those tiles live on, for the two things parent view can do here: put another tile
     * inside the folder, and rearrange what is already in it.
     */
    val folderBoardId: String? = null,
    /**
     * Whether the app a shortcut tile points at is still on the phone. Asked when the tile is drawn:
     * an app that has been uninstalled since is an ordinary thing to find.
     */
    val appInstalled: Boolean = false,
    /**
     * Whether anything on this phone will take text to translate. Asked while the tile is drawn,
     * the same way [appInstalled] is: a translation app uninstalled since last time is ordinary.
     *
     * False by default, so a screen built from a fixture draws no control and every golden taken
     * before this existed still matches.
     */
    val canTranslate: Boolean = false,
    /**
     * This tile, flattened to the words on it, ready to hand over. Empty when there is nothing to
     * hand over — a tile with nothing but a title still has its title.
     */
    val translationText: String = "",
    /**
     * Every language this tile exists in: the tile's own text first, then its variants.
     *
     * One entry means no control — most tiles in most installations have exactly one, and a chip
     * row offering a single choice is furniture.
     */
    val languages: List<TileLanguage> = emptyList(),
    /** Which of them is on screen. Null is the tile's own text. */
    val shownLanguage: String? = null,
    /** The tile was edited after this translation was written. Shown regardless, and said. */
    val isStaleTranslation: Boolean = false,
    /**
     * Where the video or recording on this tile actually is. Null when the row is there and the file
     * is not, which the screen says in words rather than handing a player a path to nowhere.
     */
    val mediaPath: String? = null,
)

/**
 * One language a tile can be read in.
 *
 * [name] is the language's own name for itself — "Türkçe", not "Turkish" — because a caregiver
 * looking for their language is the one person who cannot be assumed to read the app's. It comes
 * from the platform's locale data rather than from `strings.xml`; see `AppLanguage.nameOf`.
 */
data class TileLanguage(val tag: String?, val name: String)

/**
 * Suppressed rather than bundled, and rather than raising the threshold — which is what
 * `EditCardViewModel` argues for one file over, for the same reason: a counter that moves whenever
 * it fires stops guarding the other forty classes.
 *
 * Six of the seven are the use cases this one screen genuinely needs, and the seventh is the tile's
 * id, which comes from the route rather than from Koin and cannot be bundled with anything.
 * `Translations` is already a holder over a single use case, put there precisely so this reads as
 * one dependency instead of two; collapsing further would mean a "CardDependencies" object whose
 * only job is to satisfy a number, and which would hide which screen depends on what — the thing
 * the counter exists to make visible.
 */
@Suppress("LongParameterList")
class CardViewModel(
    private val cardId: String,
    private val tiles: TileSource,
    private val toggleChecklistItem: ToggleChecklistItem,
    private val media: Media,
    private val apps: Apps,
    private val translations: Translations,
    private val recordEvent: RecordEvent,
) : ViewModel() {

    private val _state = MutableStateFlow(CardUiState())
    val state: StateFlow<CardUiState> = _state.asStateFlow()

    /**
     * The subscription to a folder's contents.
     *
     * Cancelled and restarted with each reload rather than left running: a reload can find the tile
     * pointing at a different board, or gone, and two collectors writing the same field would leave
     * whichever finished last on screen.
     */
    private var folderTiles: Job? = null

    init {
        reload()
    }

    /**
     * Recorded here rather than by the screen, so that a tile opened from the start screen, from a
     * folder and from a search result all read the same in the log.
     */
    fun onOpened() {
        val id = parseUuidOrNull(cardId) ?: return
        viewModelScope.launch { recordEvent.cardOpened(id) }
    }

    /** The call is placed by the phone app; what Larova can honestly log is that it handed it over. */
    fun onCallPrepared() {
        val id = parseUuidOrNull(cardId) ?: return
        viewModelScope.launch { recordEvent.callPrepared(id) }
    }

    fun onToggleItem(index: Int) {
        val id = parseUuidOrNull(cardId) ?: return
        viewModelScope.launch {
            // Ticking an item is the one write a caregiver can make. Reload rather than mutate the
            // state here: the stored payload is the truth, and a tick that failed must not leave a
            // checkbox looking as though it succeeded.
            toggleChecklistItem(id, index)
            recordEvent.checkToggled(id)
            reload()
        }
    }

    /**
     * One step's picture, at the size a phone screen can actually show.
     *
     * Suspending rather than part of the state: the guide asks for the step it is on, when it gets
     * there. A tile with ten pictures on it would otherwise decode all ten to open, on the screen
     * that most needs to open quickly.
     */
    suspend fun pictureFor(mediaId: String): ImageBitmap? {
        val id = parseUuidOrNull(mediaId) ?: return null
        return media.loadImage(id, ImageSize.ON_SCREEN)?.toImageBitmapOrNull()
    }

    private fun reload() {
        viewModelScope.launch {
            val tile = tiles.observe(cardId)
            _state.value = if (tile == null) {
                CardUiState(isLoading = false, missing = true)
            } else {
                val variants = translations.textsFor(tile.card.id).first()
                val shown = resolveCardText(tile.card, variants, translations.language().first())
                // The resolved text takes the state's existing field names, so not one of the ten
                // renderers below has to know that translation exists. A variant is the same three
                // things a tile is — title, second line, payload — which is the whole reason for
                // storing it that way.
                CardUiState(
                    title = shown.title,
                    colorToken = tile.card.colorToken,
                    payload = CardPayloadCodec.decodeOrNull(shown.payload) ?: tile.payload,
                    isLoading = false,
                    folderBoardId = (tile.payload as? CardPayload.Folder)?.boardId?.toString(),
                    languages = languagesOf(tile.card.locale, variants),
                    shownLanguage = shown.lang,
                    isStaleTranslation = shown.possiblyOutOfDate,
                )
            }
            watchFolder(tile?.payload as? CardPayload.Folder)
            checkApp(tile?.payload as? CardPayload.AppLink)
            findMedia(tile?.payload)
            checkTranslate(tile)
        }
    }

    /**
     * Whether to offer the hand-off, and what it would hand over.
     *
     * Both at once, because neither is useful alone: a control with nothing behind it and text with
     * nowhere to send it are the same bug seen from two ends. The flattening happens here rather
     * than in the state class because the whole `Card` is in reach here and only here — the screen
     * never sees the tile's second line, and does not need to.
     */
    /**
     * The tile's own text first, then one entry per variant, in a stable order.
     *
     * The original's entry is labelled with the language it was written in when somebody has said
     * what that is, and with a string when nobody has. Nothing guesses: a tile whose language was
     * never recorded says "as written" rather than naming a language it might not be in.
     */
    private fun languagesOf(sourceLocale: String?, variants: List<CardText>): List<TileLanguage> {
        if (variants.isEmpty()) return emptyList()
        val original = TileLanguage(
            tag = null,
            name = sourceLocale?.let { translations.nameOf(it) } ?: "",
        )
        return listOf(original) + variants.map { TileLanguage(it.lang, translations.nameOf(it.lang)) }
    }

    /** Chooses which language every tile is shown in, on this phone. Not just this one. */
    fun onContentLanguageChange(tag: String?) {
        viewModelScope.launch {
            translations.choose(tag)
            reload()
        }
    }

    private suspend fun checkTranslate(tile: Tile?) {
        if (tile == null) return
        // The *resolved* text, not the original: the hand-off translates what you are looking at,
        // which is the only rule with no surprises in it.
        val current = _state.value
        val text = plainTextOf(
            title = current.title.ifBlank { tile.card.title },
            subtitle = tile.card.subtitle,
            payload = current.payload ?: tile.payload,
        )
        val available = translations.isAvailable()
        _state.update { it.copy(canTranslate = available, translationText = text) }
    }

    /**
     * The file behind a video or audio tile, looked up once when the tile opens.
     *
     * Both types keep their media the same way, so one branch does both rather than the screen
     * asking twice for what is the same question.
     */
    private suspend fun findMedia(payload: CardPayload?) {
        val id = when (payload) {
            is CardPayload.Video -> payload.mediaId
            is CardPayload.Audio -> payload.mediaId
            else -> return
        }
        val file = media.findFile(id)
        _state.update { it.copy(mediaPath = file?.absolutePath) }
    }

    private suspend fun checkApp(appLink: CardPayload.AppLink?) {
        if (appLink == null) return
        val installed = apps.isInstalled(appLink.packageName)
        _state.update { it.copy(appInstalled = installed) }
    }

    /**
     * A folder is the one tile whose contents can change while it is open — something added to it
     * from this very screen, or a tile inside it deleted — so it is observed rather than read once.
     */
    private fun watchFolder(folder: CardPayload.Folder?) {
        folderTiles?.cancel()
        if (folder == null) return

        folderTiles = viewModelScope.launch {
            tiles.onBoard(folder.boardId).collect { inside ->
                _state.update { it.copy(folderTiles = inside.map { tile -> tile.toFolderTile() }) }
            }
        }
    }
}

/**
 * What the parents wrote is the second line. Nothing is derived here — a count of steps helps on
 * the start screen, where a tile is all there is to go on, but inside a folder the tile has just
 * been chosen from a short list and another number on it only adds noise.
 */
private fun Tile.toFolderTile() = FolderTile(
    id = card.id.toString(),
    title = card.title,
    colorToken = card.colorToken,
    symbolKey = card.icon,
    subtitle = card.subtitle?.takeIf { it.isNotBlank() },
)
