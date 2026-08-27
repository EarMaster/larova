package app.larova.feature.card

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.larova.core.domain.media.ImageSize
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.parseUuidOrNull
import app.larova.core.domain.usecase.Apps
import app.larova.core.domain.usecase.Media
import app.larova.core.domain.usecase.RecordEvent
import app.larova.core.domain.usecase.Tile
import app.larova.core.domain.usecase.TileSource
import app.larova.core.domain.usecase.ToggleChecklistItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
     * Where the video or recording on this tile actually is. Null when the row is there and the file
     * is not, which the screen says in words rather than handing a player a path to nowhere.
     */
    val mediaPath: String? = null,
)

class CardViewModel(
    private val cardId: String,
    private val tiles: TileSource,
    private val toggleChecklistItem: ToggleChecklistItem,
    private val media: Media,
    private val apps: Apps,
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
                CardUiState(
                    title = tile.card.title,
                    colorToken = tile.card.colorToken,
                    payload = tile.payload,
                    isLoading = false,
                    folderBoardId = (tile.payload as? CardPayload.Folder)?.boardId?.toString(),
                )
            }
            watchFolder(tile?.payload as? CardPayload.Folder)
            checkApp(tile?.payload as? CardPayload.AppLink)
            findMedia(tile?.payload)
        }
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
