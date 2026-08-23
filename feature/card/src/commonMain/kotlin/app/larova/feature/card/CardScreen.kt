package app.larova.feature.card

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.larova.core.domain.model.CardPayload
import app.larova.core.ui.component.LarovaScaffold
import app.larova.core.ui.icon.TileSymbol
import app.larova.core.ui.icon.image
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.cd_edit_tile
import org.jetbrains.compose.resources.stringResource

/**
 * What opening a tile leads to.
 *
 * One renderer per payload type, dispatched here. A type this version does not know is not a bug
 * to be handled at this level — the payload never decodes in the first place, the tile is left out
 * of the grid, and the state arrives here marked missing.
 *
 * Every renderer gets the same frame: title, a way back, and the help bar. Someone who opened the
 * wrong tile while looking for help must not have to work out how to get out of it.
 */
@Composable
fun CardScreen(
    state: CardUiState,
    isParentView: Boolean,
    onToggleItem: (Int) -> Unit,
    onPrepareCall: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
    onEdit: () -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LarovaScaffold(
        title = state.title,
        onHelp = onHelp,
        onBack = onBack,
        modifier = modifier,
        actions = {
            if (isParentView && !state.missing) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = TileSymbol.NOTE.image,
                        contentDescription = stringResource(Res.string.cd_edit_tile, state.title),
                    )
                }
            }
        },
    ) { insets ->
        val content = Modifier.padding(insets)

        when {
            state.isLoading -> Unit

            state.payload == null -> EmptyPayloadNote(modifier = content)

            else -> when (val payload = state.payload) {
                is CardPayload.Guide -> GuideView(
                    guide = payload,
                    modifier = content,
                    onFinish = onBack,
                )

                is CardPayload.Note -> NoteView(note = payload, modifier = content)

                is CardPayload.Checklist -> ChecklistView(
                    checklist = payload,
                    onToggle = onToggleItem,
                    modifier = content,
                )

                is CardPayload.Phone -> CallView(
                    phone = payload,
                    onPrepareCall = onPrepareCall,
                    modifier = content,
                )

                is CardPayload.Web -> WebsiteView(
                    web = payload,
                    onOpenUrl = onOpenUrl,
                    modifier = content,
                )

                // Tables, media, app shortcuts and folders are M2. They cannot be reached from the
                // grid before then, because nothing can create one — but the branch is here rather
                // than a wildcard, so adding a type is a compile error until it has a renderer.
                is CardPayload.Table,
                is CardPayload.Video,
                is CardPayload.Audio,
                is CardPayload.AppLink,
                is CardPayload.Folder,
                -> EmptyPayloadNote(modifier = content)
            }
        }
    }
}
