package app.larova.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.larova.core.ui.component.LarovaScaffold
import app.larova.core.ui.component.TileCard
import app.larova.core.ui.icon.MoreVertical
import app.larova.core.ui.icon.TileSymbol
import app.larova.core.ui.icon.image
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.cd_menu
import app.larova.core.ui.resources.home_add_tile
import app.larova.core.ui.resources.arrange_title
import app.larova.core.ui.resources.cd_clear_search
import app.larova.core.ui.resources.home_empty_hint
import app.larova.core.ui.resources.home_empty_title
import app.larova.core.ui.resources.home_greeting
import app.larova.core.ui.resources.home_search
import app.larova.core.ui.resources.home_search_empty
import app.larova.core.ui.resources.settings_title
import app.larova.core.ui.resources.tile_item_count
import app.larova.core.ui.resources.tile_step_count
import app.larova.core.ui.resources.transfer_title
import app.larova.core.ui.theme.Dimens
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The start screen.
 *
 * Two columns with a 12dp gutter and a 20dp margin, per docs/design/design-system.md §5. This is
 * what the caregiver sees first and often all they see, so nothing is on it that does not have to
 * be there.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    isParentView: Boolean,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onOpenTile: (String) -> Unit,
    onAddTile: () -> Unit,
    onArrange: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTransfer: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LarovaScaffold(
        title = stringResource(Res.string.home_greeting),
        onHelp = onHelp,
        modifier = modifier,
        actions = {
            // Everything a caregiver does not need is behind this one control: settings, backup,
            // and later the switch to parent view. The grid stays the whole screen.
            HomeMenu(
                isParentView = isParentView,
                onArrange = onArrange,
                onOpenSettings = onOpenSettings,
                onOpenTransfer = onOpenTransfer,
            )
        },
        floatingActionButton = {
            // Absent in caregiver view rather than disabled. A greyed-out button is a question
            // ("why not?") asked of someone who is here to read a bedtime routine, and the answer
            // does not concern them.
            //
            // Extended rather than an icon alone: "+" on a grid of tiles is not self-explanatory to
            // someone who did not choose this phone.
            if (isParentView) {
                ExtendedFloatingActionButton(
                    onClick = onAddTile,
                    icon = { Icon(imageVector = TileSymbol.STAR.image, contentDescription = null) },
                    text = { Text(stringResource(Res.string.home_add_tile)) },
                )
            }
        },
    ) { insets ->
        Column(modifier = Modifier.fillMaxSize().padding(insets)) {
            // Always visible, per docs/concept.md 4.1. Someone looking for one thing under time
            // pressure should not have to find the search first.
            SearchField(
                query = state.query,
                onQueryChange = onQueryChange,
                onClear = onClearQuery,
            )

            when {
                // Nothing is drawn while loading. A grid that is about to have tiles and a grid
                // that has none look the same, and flashing "Nothing here yet" at someone whose
                // content is one frame away is a lie the app can avoid telling.
                state.isLoading -> Unit

                state.tiles.isNotEmpty() -> TileGrid(tiles = state.tiles, onOpenTile = onOpenTile)

                state.isSearching -> Message(text = stringResource(Res.string.home_search_empty))

                else -> HomeEmptyState()
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text(stringResource(Res.string.home_search)) },
        singleLine = true,
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = onClear) {
                    Icon(
                        imageVector = MoreVertical,
                        contentDescription = stringResource(Res.string.cd_clear_search),
                    )
                }
            }
        } else {
            null
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenMargin, vertical = 4.dp),
    )
}

@Composable
private fun Message(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenMargin),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun TileGrid(
    tiles: List<HomeTile>,
    onOpenTile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        // Deliberately no key on the grid itself: the search results and the ordered board are two
        // different lists, and animating between them as though tiles had moved would suggest the
        // start screen had been rearranged.
        contentPadding = PaddingValues(
            start = Dimens.ScreenMargin,
            end = Dimens.ScreenMargin,
            top = Dimens.GridGutter,
            bottom = Dimens.ScreenMargin,
        ),
        horizontalArrangement = Arrangement.spacedBy(Dimens.GridGutter),
        verticalArrangement = Arrangement.spacedBy(Dimens.GridGutter),
    ) {
        items(tiles, key = { it.id }) { tile ->
            TileCard(
                title = tile.title,
                subtitle = tile.subtitle.text(),
                colorToken = tile.colorToken,
                symbolKey = tile.symbolKey,
                onClick = { onOpenTile(tile.id) },
            )
        }
    }
}

/** Plurals are resolved here rather than in the ViewModel, because they need the language. */
@Composable
private fun TileSubtitle.text(): String? = when (this) {
    TileSubtitle.None -> null
    is TileSubtitle.Custom -> text
    is TileSubtitle.Steps -> pluralStringResource(Res.plurals.tile_step_count, count, count)
    is TileSubtitle.Items -> pluralStringResource(Res.plurals.tile_item_count, count, count)
}

@Composable
private fun HomeMenu(
    isParentView: Boolean,
    onArrange: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTransfer: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(imageVector = MoreVertical, contentDescription = stringResource(Res.string.cd_menu))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        // Rearranging and backup are parent-view work. Settings stays, because it is the way in.
        if (isParentView) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.arrange_title)) },
                onClick = {
                    expanded = false
                    onArrange()
                },
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.transfer_title)) },
                onClick = {
                    expanded = false
                    onOpenTransfer()
                },
            )
        }
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.settings_title)) },
            onClick = {
                expanded = false
                onOpenSettings()
            },
        )
    }
}

/**
 * What a fresh installation shows.
 *
 * It says what a tile can hold, because "add a tile" means nothing to someone who has never seen
 * one. The templates offered on first run (M2) will make this rare, but a screen that can render
 * zero tiles is better than one that assumes it never has to.
 */
@Composable
private fun HomeEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenMargin),
        verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = stringResource(Res.string.home_empty_title),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(Res.string.home_empty_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
