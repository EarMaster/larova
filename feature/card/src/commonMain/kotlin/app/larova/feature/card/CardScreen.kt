package app.larova.feature.card

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import app.larova.core.domain.model.CardPayload
import app.larova.core.ui.component.ContentWidth
import app.larova.core.ui.component.LarovaScaffold
import app.larova.core.ui.icon.MoreVertical
import app.larova.core.ui.icon.TileSymbol
import app.larova.core.ui.icon.Translate
import app.larova.core.ui.icon.image
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.arrange_title
import app.larova.core.ui.resources.cd_edit_tile
import app.larova.core.ui.resources.cd_language
import app.larova.core.ui.resources.cd_menu
import app.larova.core.ui.resources.cd_translate
import app.larova.core.ui.resources.cd_translate_language
import app.larova.core.ui.resources.home_add_tile
import app.larova.core.ui.resources.translate_follow_app
import org.jetbrains.compose.resources.stringResource

/**
 * What opening a tile leads to.
 *
 * One renderer per payload type, dispatched in [CardContent]. A type this version does not know is
 * not a bug to be handled at this level — the payload never decodes in the first place, the tile is
 * left out of the grid, and the state arrives here marked missing.
 *
 * Every renderer gets the same frame: title, a way back, and the help bar. Someone who opened the
 * wrong tile while looking for help must not have to work out how to get out of it.
 *
 * The three folder callbacks default to doing nothing, because every other type ignores them: a
 * folder is the only tile with things inside it to open, add to and rearrange.
 */
@Composable
fun CardScreen(
    state: CardUiState,
    isParentView: Boolean,
    onToggleItem: (Int) -> Unit,
    onPrepareCall: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenApp: (String) -> Unit,
    onTranslate: (String) -> Unit,
    /**
     * Chooses which language tiles are shown in, on this phone. Null follows the app's own
     * language. Only offered on a tile that has more than one, which is the only place somebody has
     * a reason to think about it.
     */
    onContentLanguageChange: (String?) -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
    loadPicture: suspend (String) -> ImageBitmap? = { null },
    onOpenTile: (String) -> Unit = {},
    onAddTileHere: () -> Unit = {},
    onArrange: () -> Unit = {},
) {
    val isFolder = state.payload is CardPayload.Folder

    LarovaScaffold(
        title = state.title,
        onHelp = onHelp,
        onBack = onBack,
        modifier = modifier,
        // A folder is a grid and everything else is something to read, which is the whole of what
        // the frame needs to know to lay this out on a tablet.
        contentWidth = if (isFolder) ContentWidth.Grid else ContentWidth.Reading,
        actions = {
            // Outside CardActions rather than inside it, and that is the whole point: CardActions
            // returns early in caregiver view, and the person who cannot read the tile is exactly
            // the person in caregiver view. The slot itself belongs to nobody.
            //
            // In the bar rather than above the content. This was a row of chips under the title
            // once, and the cost was paid on the tile that could least afford it: a guide is one
            // step at a time in a weighted scroller, so a strip in the content takes the height a
            // step needs, and at the 200 % font scale this app promises that is the difference
            // between a step fitting and not.
            //
            // First, so it does not move when parent view adds the edit button beside it, and so a
            // right-to-left layout mirrors it along with everything else.
            LanguageMenu(
                languages = state.languages,
                chosen = state.chosenLanguage,
                handOffText = state.translationText.takeIf { state.canTranslate },
                onSelect = onContentLanguageChange,
                onTranslate = onTranslate,
            )
            CardActions(
                state = state,
                isParentView = isParentView,
                isFolder = isFolder,
                onArrange = onArrange,
                onEdit = onEdit,
            )
        },
        floatingActionButton = {
            // Only a folder has somewhere to put a new tile. The label spells it out, because a
            // plus on a grid of tiles is not self-explanatory to someone who did not choose this
            // phone — the same reason the start screen extends its own button.
            if (isParentView && isFolder) {
                ExtendedFloatingActionButton(
                    onClick = onAddTileHere,
                    icon = { Icon(imageVector = TileSymbol.STAR.image, contentDescription = null) },
                    text = { Text(stringResource(Res.string.home_add_tile)) },
                )
            }
        },
    ) { insets ->
        CardContent(
            state = state,
            onToggleItem = onToggleItem,
            onPrepareCall = onPrepareCall,
            onOpenUrl = onOpenUrl,
            onOpenApp = onOpenApp,
            onOpenTile = onOpenTile,
            onBack = onBack,
            loadPicture = loadPicture,
            modifier = Modifier.padding(insets),
        )
    }
}

/**
 * Which language this tile is read in, and the way out to a translation app.
 *
 * One control for both, because to the person holding the phone they are one question — "I cannot
 * read this" — with two answers, depending on whether the parent already wrote it in a language
 * they can read. Two globes side by side would ask them to know which answer applies first.
 *
 * It always opens rather than sometimes acting: a control that hands the tile to a translator on
 * one tile and opens a list on the next is one nobody can learn.
 *
 * Choosing a language sets it for **every** tile on this phone, not just this one. The setting is
 * about the person holding it, not about this guide; `settings_content_language_hint` says so where
 * there is room to say it.
 */
@Composable
private fun LanguageMenu(
    languages: List<TileLanguage>,
    chosen: String?,
    /** The words on this tile, or null when nothing on this phone will take them. */
    handOffText: String?,
    onSelect: (String?) -> Unit,
    onTranslate: (String) -> Unit,
) {
    val canHandOff = !handOffText.isNullOrBlank()
    // Nothing to choose between and nowhere to send it: no control at all, rather than one that
    // opens an empty list.
    if (languages.isEmpty() && !canHandOff) return

    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(imageVector = Translate, contentDescription = stringResource(Res.string.cd_language))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        // Only where there is a choice to make. On an untranslated tile this menu is the hand-off
        // and nothing else: a row reading "follow the app language" above it would be a setting
        // offered on the one screen with no use for it.
        if (languages.isNotEmpty()) {
            // First and always present: it is the default, and the one entry somebody who has
            // changed this and wants it back is looking for.
            LanguageItem(
                label = stringResource(Res.string.translate_follow_app),
                selected = chosen == null,
                onClick = {
                    expanded = false
                    onSelect(null)
                },
            )
            for (language in languages) {
                LanguageItem(
                    label = language.name,
                    selected = chosen == language.tag,
                    onClick = {
                        expanded = false
                        onSelect(language.tag)
                    },
                )
            }
        }
        if (canHandOff) {
            if (languages.isNotEmpty()) HorizontalDivider()
            // Below the line because it is not a fourth language: it takes these words off the
            // phone, to an app the person chose, and brings nothing back.
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.cd_translate)) },
                onClick = {
                    expanded = false
                    onTranslate(handOffText.orEmpty())
                },
            )
        }
    }
}

/**
 * One language to read the tile in.
 *
 * A radio rather than a tick: these are one choice among several rather than several switches, and
 * the role is what tells a screen reader which of the two this is. The label is the language's own
 * name for itself, so `cd_translate_language` carries the rest — a row announcing only "Türkçe"
 * leaves somebody who cannot see the globe above it to infer what choosing it would do.
 */
@Composable
private fun LanguageItem(label: String, selected: Boolean, onClick: () -> Unit) {
    val description = stringResource(Res.string.cd_translate_language, label)
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { RadioButton(selected = selected, onClick = null) },
        onClick = onClick,
        modifier = Modifier.semantics {
            this.selected = selected
            contentDescription = description
        },
    )
}

/**
 * What parent view adds to the bar: rearranging the inside of a folder, and editing the tile.
 *
 * Absent rather than disabled in caregiver view — a greyed-out control is a question asked of
 * someone who came here to read something.
 */
@Composable
private fun CardActions(
    state: CardUiState,
    isParentView: Boolean,
    isFolder: Boolean,
    onArrange: () -> Unit,
    onEdit: () -> Unit,
) {
    if (!isParentView) return

    // Behind the same control, with the same icon, in the same corner as rearranging the start
    // screen. One item in it today, and it is where somebody who has rearranged a start screen
    // will look. Only worth offering once there are two tiles to swap.
    if (isFolder && state.folderTiles.size > 1) {
        FolderMenu(onArrange = onArrange)
    }
    if (!state.missing) {
        IconButton(onClick = onEdit) {
            Icon(
                imageVector = TileSymbol.NOTE.image,
                contentDescription = stringResource(Res.string.cd_edit_tile, state.title),
            )
        }
    }
}

/**
 * One renderer per payload type.
 *
 * Separate from the frame so that adding a type touches the dispatch and nothing else, and so the
 * bar, the button and the ten renderers are not one function that only reads top to bottom.
 */
@Composable
private fun CardContent(
    state: CardUiState,
    onToggleItem: (Int) -> Unit,
    onPrepareCall: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenApp: (String) -> Unit,
    onOpenTile: (String) -> Unit,
    onBack: () -> Unit,
    loadPicture: suspend (String) -> ImageBitmap?,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) return

    when (val payload = state.payload) {
        null -> EmptyPayloadNote(modifier = modifier)

        is CardPayload.Guide -> GuideView(
            guide = payload,
            modifier = modifier,
            loadPicture = loadPicture,
            onFinish = onBack,
        )

        is CardPayload.Note -> NoteView(note = payload, modifier = modifier)

        is CardPayload.Table -> TableView(table = payload, modifier = modifier)

        is CardPayload.Folder -> FolderView(
            tiles = state.folderTiles,
            onOpenTile = onOpenTile,
            modifier = modifier,
        )

        is CardPayload.Checklist -> ChecklistView(
            checklist = payload,
            onToggle = onToggleItem,
            modifier = modifier,
        )

        is CardPayload.Phone -> CallView(
            phone = payload,
            onPrepareCall = onPrepareCall,
            modifier = modifier,
        )

        is CardPayload.Web -> WebsiteView(
            web = payload,
            onOpenUrl = onOpenUrl,
            modifier = modifier,
        )

        is CardPayload.AppLink -> AppLinkView(
            appLink = payload,
            isInstalled = state.appInstalled,
            onOpenApp = onOpenApp,
            modifier = modifier,
        )

        is CardPayload.Video -> MediaView(
            caption = payload.caption,
            absolutePath = state.mediaPath,
            showVideo = true,
            modifier = modifier,
        )

        is CardPayload.Audio -> MediaView(
            caption = payload.caption,
            absolutePath = state.mediaPath,
            showVideo = false,
            modifier = modifier,
        )

        // Every type in concept.md §4.1 now has a renderer. The branches stay exhaustive rather
        // than ending in a wildcard, so a type added later is a compile error until it has one.
    }
}

/** The one thing parent view can do to a folder that is not editing the tile itself. */
@Composable
private fun FolderMenu(onArrange: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(imageVector = MoreVertical, contentDescription = stringResource(Res.string.cd_menu))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.arrange_title)) },
            onClick = {
                expanded = false
                onArrange()
            },
        )
    }
}
