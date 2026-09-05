package app.larova.feature.card.edit

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.larova.core.domain.media.ImageSize
import app.larova.core.domain.media.isLargeMedia
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.CheckItem
import app.larova.core.domain.model.MAX_TABLE_COLUMNS
import app.larova.core.domain.model.PhoneEntry
import app.larova.core.domain.model.Step
import app.larova.core.domain.model.isOpenableUrl
import app.larova.core.domain.model.parseUuidOrNull
import app.larova.core.domain.model.phoneOf
import app.larova.core.domain.model.tableOf
import app.larova.core.domain.usecase.Apps
import app.larova.core.domain.usecase.CardDraft
import app.larova.core.domain.usecase.Folders
import app.larova.core.domain.usecase.Media
import app.larova.core.domain.usecase.ObserveLockedTypes
import app.larova.core.domain.usecase.Recording
import app.larova.core.domain.usecase.SaveCard
import app.larova.core.domain.usecase.Tile
import app.larova.core.domain.usecase.TileEditing
import app.larova.core.domain.usecase.Translations
import app.larova.core.domain.usecase.UnlockPrice
import app.larova.core.ui.icon.TileSymbol
import app.larova.core.ui.theme.TileColor
import app.larova.feature.card.toImageBitmapOrNull
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * One line of a guide as the editor holds it: the words, and the picture that goes with them.
 *
 * The picture is carried as its identifier rather than as the picture itself. It was written to
 * app-private storage the moment it was picked, and a parent who moves a step around, or types over
 * every word of it, has not asked for the photograph to be picked again.
 */
data class StepDraft(val text: String = "", val mediaId: String? = null)

/**
 * One person on a call tile as the editor holds them.
 *
 * Separate from the domain's `PhoneEntry` because this one may be half-typed: a name with no
 * number yet is a perfectly good state to be in while filling a form, and an unusable state to
 * store. `phoneOf` is what turns these into the other, dropping the ones nobody finished.
 */
data class ContactDraft(
    val name: String = "",
    val number: String = "",
    val relation: String = "",
    val inHelpSheet: Boolean = false,
)

/** Why an attempt to buy the unlock did not end with it being bought. */
enum class OfferMessage {
    /** Cash or a carrier bill: taken, not yet paid. It arrives on a later start-up. */
    PENDING,

    /** No store, no connection, or nothing on sale. Worth retrying, not worth explaining twice. */
    UNAVAILABLE,
}

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
    val steps: List<StepDraft> = listOf(StepDraft()),
    val noteText: String = "",
    val items: List<CheckItem> = listOf(CheckItem("")),
    val resetDaily: Boolean = false,
    /**
     * A table starts with two columns and one row, because one column is a list and the editor
     * should not have to be told that first.
     */
    val columns: List<String> = listOf("", ""),
    val rows: List<List<String>> = listOf(listOf("", "")),
    /** A call tile starts with one empty person, so there is somewhere to type immediately. */
    val contacts: List<ContactDraft> = listOf(ContactDraft()),
    val webUrl: String = "",
    val webLabel: String = "",
    /**
     * What the website or the app is for, in the family's own words. One field for both types
     * because only one of them is ever being saved — the same reason `mediaCaption` serves a video
     * and a recording.
     */
    val linkCaption: String = "",
    /** The app a shortcut tile opens, and the words the parents put on it. */
    val appPackage: String = "",
    val appLabel: String = "",
    /**
     * The tile types that need buying before they can be authored, empty once anything has
     * vouched for this installation.
     *
     * State rather than a lookup inside the screen, for the same reason everything else here is:
     * the screenshot tests hand a fixture in and take a picture, and a screen that asked a
     * repository could not be photographed locked and unlocked.
     */
    val lockedTypes: Set<CardType> = emptySet(),
    /**
     * The languages this tile already exists in, besides the one it was written in.
     *
     * Empty on a new tile and on the great majority of saved ones. The section that shows them is
     * drawn regardless once the tile exists, because "no other languages yet" is how somebody finds
     * out they can add one.
     */
    val languages: List<VariantSummary> = emptyList(),
    /**
     * The price, as Google Play writes it for this buyer's country. Null while it is being fetched
     * and null for good on a phone that cannot reach the store — the offer is still shown, with a
     * button that does not name a number.
     */
    val offerPrice: String? = null,
    /** What went wrong, or did not finish, the last time somebody tried to buy. */
    val offerMessage: OfferMessage? = null,
    /** What the picker is showing. Empty until it is opened — a phone has a hundred apps on it. */
    val apps: List<AppChoice> = emptyList(),
    val appQuery: String = "",
    val appPickerOpen: Boolean = false,
    /** The video or recording on this tile, and what the parents wrote above it. */
    val mediaId: String? = null,
    val mediaCaption: String = "",
    /**
     * How big the chosen file is, so the editor can say so before it goes into a backup. Not a
     * limit — the parents decide — but a backup is one file and they are the ones sending it.
     */
    val mediaSizeBytes: Long = 0,
    /** The microphone is running. The only state in this editor that is a device rather than a field. */
    val isRecording: Boolean = false,
    /** The microphone could not be opened, or gave back nothing usable. */
    val recordingFailed: Boolean = false,
    val titleMissing: Boolean = false,
    val urlInvalid: Boolean = false,
    /** A shortcut tile was saved without an app chosen. */
    val appMissing: Boolean = false,
    /** A video or audio tile was saved with nothing to play. */
    val mediaMissing: Boolean = false,
    val isLoading: Boolean = false,
    val saved: Boolean = false,
    val deleted: Boolean = false,
    /**
     * Previews of the step pictures, by identifier, at thumbnail size.
     *
     * Kept beside the steps rather than inside them: one picture can be on two steps as easily as
     * on one, and the editor should decode it once either way.
     */
    val pictures: Map<String, ImageBitmap> = emptyMap(),
    /** The last picked picture could not be read. Said out loud rather than silently ignored. */
    val pictureFailed: Boolean = false,
    /**
     * The board this folder tile opens, once there is one. Null while a folder is still being
     * created: the board is made when the tile is saved, so leaving the editor cannot leave an
     * empty board behind.
     */
    val folderBoardId: String? = null,
    /** How many tiles are inside, so that deleting the folder can say what goes with it. */
    val folderTileCount: Int = 0,
    /**
     * Whether the editor was opened from inside a folder. One level is all the information
     * architecture allows, so a folder cannot be offered here (docs/concept.md §4.1).
     */
    val isNested: Boolean = false,
) {
    /** Worth saying out loud before this goes into a backup somebody has to send somewhere. */
    val mediaIsLarge: Boolean get() = isLargeMedia(mediaSizeBytes)
}

/**
 * The types that can be made today. Media and app shortcuts are still to come — offering a type
 * that cannot be filled in yet would be worse than not offering it.
 *
 * A folder is missing from the list when the editor was opened inside one. Not greyed out, not
 * refused on save: absent, because one level deep is the shape of the product rather than a limit
 * to explain (docs/concept.md §4.1).
 *
 * The paid types are handled the other way round, and the difference is deliberate. They stay in
 * this list and the picker marks them with a lock, because somebody who cannot see what buying
 * would get them has no reason to buy it. A folder is absent because it does not exist here; a
 * paid tile is present because it does exist and is simply not paid for yet. Which ones those are
 * is `EditUiState.lockedTypes`, not this function: the answer changes while the app is running.
 */
fun editableTypes(isNested: Boolean): List<CardType> = listOfNotNull(
    CardType.GUIDE,
    CardType.CHECKLIST,
    CardType.NOTE,
    CardType.TABLE,
    CardType.PHONE,
    CardType.WEB,
    CardType.APP_LINK,
    CardType.VIDEO,
    CardType.AUDIO,
    CardType.FOLDER.takeUnless { isNested },
)

/**
 * One language a tile already has, for the list in the editor.
 *
 * [isStale] means the tile was edited after this translation was written — shown here loudly,
 * because this is the screen where it can actually be fixed. On the tile itself it is one quiet
 * line: the caregiver reading it cannot do anything about it, and the text is still the only text
 * in the room that they can read.
 */
data class VariantSummary(val tag: String, val name: String, val isStale: Boolean)

/**
 * Where the editor was opened, as one parameter.
 *
 * Both halves come from the navigation route and neither means anything without the other: an empty
 * [cardId] is a new tile, and [boardId] is the folder it is being made inside — the start screen
 * when it is empty. Passing them together also keeps them from being swapped at the call site,
 * which two bare strings invite.
 */
data class EditTarget(val cardId: String = "", val boardId: String = "")

/**
 * One handler per field, and six tile types worth of fields.
 *
 * Suppressed here rather than by raising the threshold in `detekt.yml`, which is what happened the
 * last two times this class grew: a counter that moves whenever it fires stops guarding the other
 * forty classes. Collapsing the handlers into one `onFieldChange(name, value)` would trade the
 * compiler's checking for the counter, which is the worse trade — a misspelt field name would then
 * be a silent no-op on a screen a parent only sees once.
 *
 * The parameter count is suppressed for the same kind of reason. Seven of the eight are the use
 * cases this one screen genuinely needs, and the eighth is the paid unlock. Bundling them into a
 * holder object to satisfy the counter would hide which screen depends on what, which is the thing
 * the counter exists to make visible.
 */
@OptIn(ExperimentalUuidApi::class)
@Suppress("TooManyFunctions", "LongParameterList")
class EditCardViewModel(
    target: EditTarget,
    private val tile: TileEditing,
    private val media: Media,
    private val recording: Recording,
    private val folders: Folders,
    private val apps: Apps,
    private val translations: Translations,
    lockedTypes: ObserveLockedTypes,
    private val unlockPrice: UnlockPrice,
) : ViewModel() {

    private val cardId: String? = target.cardId.takeIf { it.isNotEmpty() }

    /** The folder the new tile is being made inside, or null for the start screen. */
    private val boardId: Uuid? = parseUuidOrNull(target.boardId)

    private val _state = MutableStateFlow(
        EditUiState(isNew = cardId.isNullOrEmpty(), isNested = boardId != null),
    )
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    /**
     * Which step the picker was opened for.
     *
     * Held here rather than in the screen so that turning the phone while the gallery is open
     * cannot land the picture on the wrong step, or on none.
     */
    private var pictureForStep: Int? = null

    init {
        if (!cardId.isNullOrEmpty()) load(cardId)
        // Collected rather than read once, so a language added on the screen this one opens is
        // listed the moment it is saved and the parent comes back.
        if (!cardId.isNullOrEmpty()) {
            parseUuidOrNull(cardId)?.let { id ->
                viewModelScope.launch {
                    // The tile's own `updatedAt` is the anchor staleness is measured from, read
                    // once here: it is what a person last edited, and a tick on a checklist
                    // deliberately does not move it.
                    val editedAt = tile.observe(cardId)?.card?.updatedAt
                    translations.textsFor(id).collect { texts ->
                        val summaries = texts.map { text ->
                            VariantSummary(
                                tag = text.lang,
                                name = translations.nameOf(text.lang),
                                isStale = editedAt != null && text.updatedAt < editedAt,
                            )
                        }
                        _state.update { it.copy(languages = summaries) }
                    }
                }
            }
        }
        // Collected rather than read once: a purchase can complete while this screen is open, and
        // the lock has to come off the chips without the parent having to back out and return.
        viewModelScope.launch {
            lockedTypes().collect { locked ->
                _state.update { it.copy(lockedTypes = locked) }
                // Also covers an existing tile of a paid type — an imported backup, most likely —
                // which is opened without anybody having tapped a chip.
                if (_state.value.type in locked) fetchPrice()
            }
        }
    }


    /**
     * The three outcomes worth telling somebody about, as three methods rather than one parameter.
     *
     * `:app` does the translating from the store's own result type, which is what keeps
     * `:core:billing` out of this module. A cancelled purchase is deliberately not among them:
     * somebody who changed their mind has already been told what they decided.
     */
    /**
     * Nothing to dismiss here. The overlay is a function of `lockedTypes`, which the entitlement
     * flow empties on its own the moment the purchase verifies — so the fields simply become
     * usable. Only the message is cleared, so a previous failure does not outlive it.
     */
    fun onPurchased() = _state.update { it.copy(offerMessage = null) }

    fun onPurchasePending() = _state.update { it.copy(offerMessage = OfferMessage.PENDING) }

    fun onPurchaseUnavailable() =
        _state.update { it.copy(offerMessage = OfferMessage.UNAVAILABLE) }

    /**
     * Asked for once per offer rather than held: the price can change between one launch and the
     * next, and a number cached from a fortnight ago is worse than no number at all. Failure is
     * silent by design — see [UnlockPrice].
     */
    private fun fetchPrice() {
        if (_state.value.offerPrice != null) return
        viewModelScope.launch {
            val price = unlockPrice()
            if (price != null) _state.update { it.copy(offerPrice = price) }
        }
    }

    fun onTitleChange(value: String) = _state.update { it.copy(title = value, titleMissing = false) }

    fun onSubtitleChange(value: String) = _state.update { it.copy(subtitle = value) }

    fun onColorChange(token: String) = _state.update { it.copy(colorToken = token) }

    fun onSymbolChange(key: String) = _state.update { it.copy(symbolKey = key) }

    fun onNoteChange(value: String) = _state.update { it.copy(noteText = value) }

    fun onResetDailyChange(value: Boolean) = _state.update { it.copy(resetDaily = value) }

    fun onWebLabelChange(value: String) = _state.update { it.copy(webLabel = value) }

    fun onLinkCaptionChange(value: String) = _state.update { it.copy(linkCaption = value) }

    fun onWebUrlChange(value: String) = _state.update { it.copy(webUrl = value, urlInvalid = false) }

    fun onAppLabelChange(value: String) = _state.update { it.copy(appLabel = value) }

    fun onMediaCaptionChange(value: String) = _state.update { it.copy(mediaCaption = value) }

    /**
     * Starts recording. Called only once the microphone has been granted, which the screen arranges
     * — the permission belongs to the platform and the question is asked there.
     */
    fun onStartRecording() {
        if (_state.value.isRecording) return
        viewModelScope.launch {
            val started = recording.start()
            _state.update { it.copy(isRecording = started, recordingFailed = !started) }
        }
    }

    /**
     * Stops, and puts what was recorded on the tile.
     *
     * A recording that produced nothing leaves the tile as it was. Somebody who taps record and stop
     * in the same second has not replaced the recording that was already there.
     */
    fun onStopRecording() {
        if (!_state.value.isRecording) return
        viewModelScope.launch {
            val asset = recording.stop()
            _state.update { state ->
                if (asset == null) {
                    state.copy(isRecording = false, recordingFailed = true)
                } else {
                    state.copy(
                        isRecording = false,
                        recordingFailed = false,
                        mediaId = asset.id.toString(),
                        mediaSizeBytes = asset.sizeBytes,
                        mediaMissing = false,
                    )
                }
            }
        }
    }

    /**
     * A picked video or recording, copied in and put on the tile.
     *
     * Copied now rather than when the tile is saved, for the same reason a picture is: the read
     * permission the picker granted lasts as long as this screen does.
     */
    fun onMediaChosen(source: String) {
        viewModelScope.launch {
            val asset = media.addFile(source)
            if (asset == null) {
                _state.update { it.copy(mediaMissing = true) }
                return@launch
            }
            _state.update {
                it.copy(
                    mediaId = asset.id.toString(),
                    mediaSizeBytes = asset.sizeBytes,
                    mediaMissing = false,
                )
            }
        }
    }

    /**
     * Opens the picker and loads the list behind it.
     *
     * Loaded on opening rather than when the editor appears: reading a hundred labels and drawing a
     * hundred icons is not work to do for a parent who came to write a bedtime guide.
     */
    fun onChooseApp() {
        _state.update { it.copy(appPickerOpen = true) }
        loadApps()
    }

    fun onAppQueryChange(value: String) {
        _state.update { it.copy(appQuery = value) }
        loadApps()
    }

    fun onDismissAppPicker() = _state.update { it.copy(appPickerOpen = false, appQuery = "") }

    /**
     * The app's own name is filled in as the label, and stays editable.
     *
     * A caregiver reads the tile, not the app store: "Music for the car" is what they recognise,
     * and the app's own name is only the best first guess at it.
     */
    fun onAppPicked(choice: AppChoice) = _state.update { state ->
        state.copy(
            appPackage = choice.packageName,
            appLabel = state.appLabel.ifBlank { choice.label },
            appPickerOpen = false,
            appQuery = "",
            appMissing = false,
        )
    }

    private fun loadApps() {
        viewModelScope.launch {
            val query = _state.value.appQuery
            val choices = apps.pickable(query).map { app ->
                AppChoice(
                    packageName = app.packageName,
                    label = app.label,
                    icon = app.icon?.toImageBitmapOrNull(),
                )
            }
            // Dropped if the picker was closed while this was running, so a list cannot arrive
            // after the dialog it belongs to.
            if (_state.value.appPickerOpen) _state.update { it.copy(apps = choices) }
        }
    }

    /** Offered while creating only: changing the type of a filled-in tile would discard content. */
    /**
     * A type was chosen, paid or not.
     *
     * A locked type is selected like any other and its fields are built as usual. The screen then
     * covers them with the offer, so somebody can see what buying would get them before deciding —
     * which is a better answer than a dialog in front of a form they never got to look at.
     */
    fun onTypeChange(type: CardType) {
        _state.update { it.copy(type = type) }
        if (type in _state.value.lockedTypes) fetchPrice()
    }

    fun onStepChange(index: Int, text: String) = _state.update { state ->
        state.copy(
            steps = state.steps.mapIndexed { i, step ->
                if (i == index) step.copy(text = text) else step
            },
        )
    }

    fun onAddStep() = _state.update { it.copy(steps = it.steps + StepDraft()) }

    fun onRemoveStep(index: Int) = _state.update { state ->
        val remaining = state.steps.filterIndexed { i, _ -> i != index }
        // Never down to nothing: a guide screen with no steps has nothing to show, and an editor
        // with no rows gives no way to start typing again.
        state.copy(steps = remaining.ifEmpty { listOf(StepDraft()) })
    }

    /**
     * Remembers which step the picture is for. Opening the picker itself is the platform's job, so
     * the screen does that immediately afterwards.
     */
    fun onPickPictureFor(index: Int) {
        pictureForStep = index
        _state.update { it.copy(pictureFailed = false) }
    }

    /**
     * A picked picture, copied in and put on the step it was picked for.
     *
     * The copy happens now rather than when the tile is saved: the read permission the picker
     * granted lasts as long as this screen does, and a picture left as a reference into the gallery
     * would go blank the day the person tidied it up.
     */
    fun onPictureChosen(source: String) {
        val index = pictureForStep ?: return
        pictureForStep = null
        viewModelScope.launch {
            val id = media.addImage(source)
            if (id == null) {
                _state.update { it.copy(pictureFailed = true) }
                return@launch
            }
            _state.update { state ->
                state.copy(
                    steps = state.steps.mapIndexed { i, step ->
                        if (i == index) step.copy(mediaId = id.toString()) else step
                    },
                    pictureFailed = false,
                )
            }
            loadPictures()
        }
    }

    /**
     * Takes the picture off the step. The file goes at the next sweep rather than now: the same
     * picture may be on another step, and only the payloads together can answer that.
     */
    fun onRemovePicture(index: Int) = _state.update { state ->
        state.copy(
            steps = state.steps.mapIndexed { i, step ->
                if (i == index) step.copy(mediaId = null) else step
            },
            pictureFailed = false,
        )
    }

    fun onItemChange(index: Int, text: String) = _state.update { state ->
        state.copy(
            items = state.items.mapIndexed { i, item -> if (i == index) item.copy(text = text) else item },
        )
    }

    fun onAddItem() = _state.update { it.copy(items = it.items + CheckItem("")) }

    fun onContactChange(index: Int, contact: ContactDraft) = _state.update { state ->
        state.copy(
            contacts = state.contacts.mapIndexed { i, existing -> if (i == index) contact else existing },
        )
    }

    fun onAddContact() = _state.update { it.copy(contacts = it.contacts + ContactDraft()) }

    fun onRemoveContact(index: Int) = _state.update { state ->
        val remaining = state.contacts.filterIndexed { i, _ -> i != index }
        // Never down to nothing: a call tile with no rows gives no way to start typing again.
        state.copy(contacts = remaining.ifEmpty { listOf(ContactDraft()) })
    }

    fun onColumnChange(index: Int, text: String) = _state.update { state ->
        state.copy(
            columns = state.columns.mapIndexed { i, column -> if (i == index) text else column },
        )
    }

    /** A new column reaches every row, so the table stays square while it is being typed. */
    fun onAddColumn() = _state.update { state ->
        if (state.columns.size >= MAX_TABLE_COLUMNS) {
            state
        } else {
            state.copy(
                columns = state.columns + "",
                rows = state.rows.map { it + "" },
            )
        }
    }

    /**
     * Removing a column takes its cells with it.
     *
     * Leaving them would shift every value in the row one heading to the left the next time the
     * table is opened, which on a tile a caregiver reads under time pressure is worse than losing
     * the column.
     */
    fun onRemoveColumn(index: Int) = _state.update { state ->
        val remaining = state.columns.filterIndexed { i, _ -> i != index }
        if (remaining.isEmpty()) {
            state
        } else {
            state.copy(
                columns = remaining,
                rows = state.rows.map { row -> row.filterIndexed { i, _ -> i != index } },
            )
        }
    }

    fun onCellChange(row: Int, column: Int, text: String) = _state.update { state ->
        state.copy(
            rows = state.rows.mapIndexed { r, cells ->
                if (r != row) cells else cells.mapIndexed { c, cell -> if (c == column) text else cell }
            },
        )
    }

    fun onAddRow() = _state.update { state ->
        // `listOf(...)` around the new row on purpose: adding a bare list to a list of lists
        // resolves to the overload that appends its elements, which would flatten the table.
        state.copy(rows = state.rows + listOf(List(state.columns.size) { "" }))
    }

    fun onRemoveRow(index: Int) = _state.update { state ->
        val remaining = state.rows.filterIndexed { i, _ -> i != index }
        state.copy(rows = remaining.ifEmpty { listOf(List(state.columns.size) { "" }) })
    }

    fun onRemoveItem(index: Int) = _state.update { state ->
        val remaining = state.items.filterIndexed { i, _ -> i != index }
        state.copy(items = remaining.ifEmpty { listOf(CheckItem("")) })
    }

    fun onSave() {
        val current = _state.value
        // Unreachable from the screen: the overlay covers Save along with the rest of the form.
        // Kept as a backstop so no future entry point can write a tile of a type nobody paid for,
        // and silent because there is no way to arrive here with something to explain.
        if (current.type in current.lockedTypes) return
        val refusal = current.refusal()
        if (refusal != null) {
            _state.value = refusal
            return
        }

        viewModelScope.launch {
            // The board a folder opens has to exist before the payload can point at it. Made here
            // rather than when the type was picked, so a parent who changed their mind and left
            // has left nothing behind.
            val folderBoardId = folderBoardIdFor(current) ?: return@launch
            val result = tile.save(
                CardDraft(
                    id = parseUuidOrNull(cardId),
                    boardId = boardId,
                    title = current.title,
                    subtitle = current.subtitle,
                    colorToken = current.colorToken,
                    icon = current.symbolKey,
                    payload = current.copy(folderBoardId = folderBoardId).toPayload(),
                ),
            )
            // Whatever the save changed, a picture may now be on no step at all: taken off one, or
            // picked and then replaced before saving.
            if (result is SaveCard.Result.Saved) media.cleanUp()
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
            tile.delete(id)
            // The pictures and recordings of a deleted tile are what nothing points at now.
            media.cleanUp()
            _state.update { it.copy(deleted = true) }
        }
    }

    private fun load(id: String) {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val existing = tile.observe(id)
            _state.value = if (existing == null) {
                // Deleted from under the editor, or never a tile. Treated as gone rather than as a
                // new tile, so saving cannot resurrect it with half its content.
                EditUiState(isNew = false, isLoading = false, deleted = true)
            } else {
                existing.toEditState()
            }
            loadPictures()
            loadFolderCount()
        }
    }

    /**
     * A recording left running when the screen goes is thrown away.
     *
     * `viewModelScope` is cancelled by the time this runs, which is why cancelling is the one
     * recorder call that does not suspend. A microphone left open is the one thing this editor can
     * leak that a person would actually notice.
     */
    override fun onCleared() {
        if (_state.value.isRecording) recording.cancel()
        super.onCleared()
    }

    /**
     * The board id a folder tile needs, creating it if this is the first save.
     *
     * Returns an empty string for every other type — there is nothing to make, and nothing that
     * could fail. Null means the board could not be created, which is the same situation as having
     * no start screen to write to: the save stops and what the parent typed stays on screen.
     */
    private suspend fun folderBoardIdFor(current: EditUiState): String? {
        if (current.type != CardType.FOLDER) return ""
        current.folderBoardId?.let { return it }

        val created = folders.create(current.title.trim())?.toString() ?: return null
        _state.update { it.copy(folderBoardId = created) }
        return created
    }

    /** What deleting this folder would take with it. Read once, when the editor opens. */
    private fun loadFolderCount() {
        val boardId = parseUuidOrNull(_state.value.folderBoardId) ?: return
        viewModelScope.launch {
            val inside = folders.observeTiles(boardId).first().size
            _state.update { it.copy(folderTileCount = inside) }
        }
    }

    /**
     * Thumbnails for whatever the steps refer to now.
     *
     * Thumbnail-sized on purpose: the stored picture is 2048px, and decoding ten of those to draw
     * ten previews the size of a stamp is how an editor runs a phone out of memory. What is already
     * decoded is kept, so adding an eleventh step does not redo the other ten.
     */
    private fun loadPictures() {
        viewModelScope.launch {
            val wanted = _state.value.steps.mapNotNull { it.mediaId }.toSet()
            val loaded = _state.value.pictures.filterKeys { it in wanted }.toMutableMap()
            for (id in wanted - loaded.keys) {
                val picture = parseUuidOrNull(id)
                    ?.let { media.loadImage(it, ImageSize.THUMBNAIL) }
                    ?.toImageBitmapOrNull()
                if (picture != null) loaded[id] = picture
            }
            _state.update { state -> state.copy(pictures = loaded) }
        }
    }
}

/**
 * What the fields add up to, one type at a time.
 *
 * Split into a function per type rather than one long `when` with the logic inside it: every branch
 * has its own rule about what counts as empty, and reading them together is how a rule ends up
 * applied to the wrong type.
 */
private fun EditUiState.toPayload(): CardPayload = when (type) {
    CardType.GUIDE -> guidePayload()
    CardType.CHECKLIST -> checklistPayload()
    CardType.NOTE -> CardPayload.Note(text = noteText.trim())
    // Squared off in the domain rather than here, so a table that arrives from an import is held to
    // the same shape as one somebody just typed.
    CardType.TABLE -> tableOf(columns = columns, rows = rows)
    CardType.VIDEO -> videoPayload()
    CardType.AUDIO -> audioPayload()
    CardType.PHONE -> phonePayload()
    CardType.WEB -> webPayload()
    CardType.APP_LINK -> appLinkPayload()
    CardType.FOLDER -> folderPayload()
}

/**
 * Blank steps are dropped. A parent who taps "add step" twice and fills one of them meant one step,
 * and an empty step in a guide is a screen that says nothing.
 *
 * A step with a picture and no words is not blank: some things are easier shown than described.
 */
private fun EditUiState.guidePayload() = CardPayload.Guide(
    steps = steps
        .map { it.copy(text = it.text.trim()) }
        .filter { it.text.isNotEmpty() || it.mediaId != null }
        .map { Step(text = it.text, mediaId = parseUuidOrNull(it.mediaId)) },
)

private fun EditUiState.checklistPayload() = CardPayload.Checklist(
    items = items.map { it.copy(text = it.text.trim()) }.filter { it.text.isNotEmpty() },
    resetDaily = resetDaily,
)

/**
 * The identifier is written before this runs, so one that will not parse means a tile whose file was
 * never stored. A note keeps the caption rather than writing a tile that plays nothing.
 */
private fun EditUiState.videoPayload(): CardPayload = parseUuidOrNull(mediaId)
    ?.let { CardPayload.Video(mediaId = it, caption = mediaCaption.trim().ifEmpty { null }) }
    ?: CardPayload.Note(text = mediaCaption.trim())

private fun EditUiState.audioPayload(): CardPayload = parseUuidOrNull(mediaId)
    ?.let { CardPayload.Audio(mediaId = it, caption = mediaCaption.trim().ifEmpty { null }) }
    ?: CardPayload.Note(text = mediaCaption.trim())

/**
 * The tile's people as the editor holds them, and never an empty list: a form with no rows gives
 * nowhere to start typing. Reads `people` rather than either half of the payload, so a tile written
 * before the list existed opens as the one person it holds.
 */
private fun CardPayload.Phone.contactDrafts(): List<ContactDraft> = people
    .map { entry ->
        ContactDraft(
            name = entry.displayName,
            number = entry.number,
            relation = entry.relation.orEmpty(),
            inHelpSheet = entry.inHelpSheet,
        )
    }
    .ifEmpty { listOf(ContactDraft()) }

/**
 * The tile's own title stands in for a name nobody typed, and only on the first person: a lone
 * number with no name is a riddle, but the fourth row on "Important numbers" borrowing the tile's
 * title would put the same name on four different people.
 */
private fun EditUiState.phonePayload() = phoneOf(
    contacts.mapIndexed { index, draft ->
        PhoneEntry(
            displayName = draft.name.trim().ifEmpty { if (index == 0) title.trim() else "" },
            number = draft.number.trim(),
            relation = draft.relation.trim().takeIf { it.isNotEmpty() },
            inHelpSheet = draft.inHelpSheet,
        )
    },
)

private fun EditUiState.webPayload() = CardPayload.Web(
    url = webUrl.trim(),
    label = webLabel.trim().takeIf { it.isNotEmpty() },
    caption = linkCaption.trim().takeIf { it.isNotEmpty() },
)

private fun EditUiState.appLinkPayload() = CardPayload.AppLink(
    packageName = appPackage,
    label = appLabel.trim().ifEmpty { title.trim() },
    caption = linkCaption.trim().takeIf { it.isNotEmpty() },
)

/**
 * The board is created before this runs, so an identifier with nothing behind it means a folder that
 * was never saved. A note keeps the title and colour rather than writing a folder that opens
 * nothing.
 */
private fun EditUiState.folderPayload(): CardPayload = parseUuidOrNull(folderBoardId)
    ?.let { CardPayload.Folder(boardId = it) }
    ?: CardPayload.Note(text = noteText.trim())

/**
 * The first thing wrong with what was typed, as the state that says so, or null if it can be saved.
 *
 * Every reason a save is refused in one place: a tile that cannot be opened, tapped or played is a
 * tile whose parent will not be there when a caregiver finds out. Checked here as well as when the
 * field was filled in, because a screen can be left half-finished and come back.
 */
private fun EditUiState.refusal(): EditUiState? = when {
    title.isBlank() -> copy(titleMissing = true)
    type == CardType.WEB && !isOpenableUrl(webUrl) -> copy(urlInvalid = true)
    type == CardType.APP_LINK && appPackage.isBlank() -> copy(appMissing = true)
    type in MEDIA_TYPES && mediaId == null -> copy(mediaMissing = true)
    else -> null
}

/**
 * The two types whose content is a file. Named once so the save check and the fields agree about
 * which they are.
 */
private val MEDIA_TYPES = setOf(CardType.VIDEO, CardType.AUDIO)

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
            steps = payload.steps
                .map { StepDraft(text = it.text, mediaId = it.mediaId?.toString()) }
                .ifEmpty { listOf(StepDraft()) },
        )

        is CardPayload.Checklist -> base.copy(
            items = payload.items.ifEmpty { listOf(CheckItem("")) },
            resetDaily = payload.resetDaily,
        )

        is CardPayload.Note -> base.copy(noteText = payload.text)

        is CardPayload.Folder -> base.copy(folderBoardId = payload.boardId.toString())

        is CardPayload.Table -> base.copy(
            // A stored table is square, but a payload written by hand or by a future version may
            // not be, and the editor has one field per column either way.
            columns = payload.columns.ifEmpty { listOf("", "") },
            rows = payload.rows
                .map { row -> List(payload.columns.size) { i -> row.getOrNull(i).orEmpty() } }
                .ifEmpty { listOf(List(payload.columns.size.coerceAtLeast(2)) { "" }) },
        )

        is CardPayload.Phone -> base.copy(
            // `people` rather than either half of the payload, so a tile written before 0.3.0
            // opens in the editor as the one person it holds rather than as an empty form.
            contacts = payload.contactDrafts(),
        )

        is CardPayload.Video -> base.copy(
            mediaId = payload.mediaId.toString(),
            mediaCaption = payload.caption.orEmpty(),
        )

        is CardPayload.Audio -> base.copy(
            mediaId = payload.mediaId.toString(),
            mediaCaption = payload.caption.orEmpty(),
        )

        is CardPayload.AppLink -> base.copy(
            linkCaption = payload.caption.orEmpty(),
            appPackage = payload.packageName,
            appLabel = payload.label,
        )

        is CardPayload.Web -> base.copy(
            linkCaption = payload.caption.orEmpty(),
            webUrl = payload.url,
            webLabel = payload.label.orEmpty(),
        )
    }
}
