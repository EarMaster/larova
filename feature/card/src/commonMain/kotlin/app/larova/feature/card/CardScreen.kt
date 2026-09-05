package app.larova.feature.card

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
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
import app.larova.core.ui.resources.cd_menu
import app.larova.core.ui.resources.cd_translate
import app.larova.core.ui.resources.cd_translate_language
import app.larova.core.ui.resources.home_add_tile
import app.larova.core.ui.resources.translate_original
import app.larova.core.ui.resources.translate_stale
import app.larova.core.ui.theme.Dimens
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
     * Chooses which language tiles are shown in, on this phone. Only reachable from a tile that has
     * more than one, which is the only place somebody has a reason to think about it.
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
            // In the bar rather than above the content, because a guide is one step at a time in a
            // weighted scroller — a strip in the content would cost the most height on the tile
            // that has the least, and at the 200 % font scale this app promises that is the
            // difference between a step fitting and not.
            //
            // First, so it does not move when parent view adds the edit button beside it, and so a
            // right-to-left layout mirrors it along with everything else.
            if (state.canTranslate && state.translationText.isNotBlank()) {
                IconButton(onClick = { onTranslate(state.translationText) }) {
                    Icon(
                        imageVector = Translate,
                        contentDescription = stringResource(Res.string.cd_translate),
                    )
                }
            }
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
        Column(modifier = Modifier.padding(insets)) {
            // Only when there is a choice. Most tiles in most installations have one language, and
            // a row offering one option is furniture on a screen that is read in a hurry.
            if (state.languages.size > 1) {
                LanguageBar(
                    languages = state.languages,
                    shown = state.shownLanguage,
                    isStale = state.isStaleTranslation,
                    onSelect = onContentLanguageChange,
                )
            }
            CardContent(
                state = state,
                onToggleItem = onToggleItem,
                onPrepareCall = onPrepareCall,
                onOpenUrl = onOpenUrl,
                onOpenApp = onOpenApp,
                onOpenTile = onOpenTile,
                onBack = onBack,
                loadPicture = loadPicture,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Which language this tile is being read in.
 *
 * Above the content rather than in the bar, because unlike the translate hand-off this is about
 * what is on the screen already — and because it is the one control here a caregiver is meant to
 * find without being told. Chips rather than a menu for the same reason: the languages a tile has
 * are worth seeing at a glance, and there are never many.
 *
 * Choosing one sets the language for **every** tile on this phone, not just this one. The setting
 * is about the person holding it, not about this guide; `settings_content_language_hint` says so
 * where there is room to say it.
 */
@Composable
private fun LanguageBar(
    languages: List<TileLanguage>,
    shown: String?,
    isStale: Boolean,
    onSelect: (String?) -> Unit,
) {
    val asWritten = stringResource(Res.string.translate_original)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenMargin, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (language in languages) {
                val label = language.name.ifBlank { asWritten }
                // Resolved out here: `semantics` is not a composable scope, and a chip that
                // announces only "Türkçe" leaves a screen-reader user to infer what it does.
                val description = stringResource(Res.string.cd_translate_language, label)
                FilterChip(
                    selected = language.tag == shown,
                    onClick = { onSelect(language.tag) },
                    label = { Text(label) },
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .semantics { contentDescription = description },
                )
            }
        }
        if (isStale) {
            // Neither amber nor alarm red: invariant 4 reserves the first for the active step and
            // the second for the help bar, and this is a note rather than a warning. The text is
            // shown and so is the translation — German that nobody in the room can read is not the
            // safer option, it is just the quieter one.
            Text(
                text = stringResource(Res.string.translate_stale),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
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
