package app.larova.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.larova.core.domain.usecase.CardDraft
import app.larova.core.domain.usecase.TemplateId
import app.larova.core.ui.component.ContentWidth
import app.larova.core.ui.component.LarovaScaffold
import app.larova.core.ui.component.TileCard
import app.larova.core.ui.icon.History
import app.larova.core.ui.icon.MoreVertical
import app.larova.core.ui.icon.Reorder
import app.larova.core.ui.icon.Sliders
import app.larova.core.ui.icon.TileSymbol
import app.larova.core.ui.icon.image
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.home_add_tile
import app.larova.core.ui.resources.arrange_title
import app.larova.core.ui.resources.cd_clear_search
import app.larova.core.ui.resources.home_empty_hint
import app.larova.core.ui.resources.home_empty_title
import app.larova.core.ui.resources.home_greeting
import app.larova.core.ui.resources.home_search
import app.larova.core.ui.resources.home_search_empty
import app.larova.core.ui.resources.home_start_with
import app.larova.core.ui.resources.settings_log
import app.larova.core.ui.resources.settings_title
import app.larova.core.ui.resources.tile_folder
import app.larova.core.ui.resources.tile_item_count
import app.larova.core.ui.resources.tile_number_count
import app.larova.core.ui.resources.tile_number_count
import app.larova.core.ui.resources.tile_step_count
import app.larova.core.ui.theme.Dimens
import app.larova.core.ui.theme.tileColumns
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The start screen.
 *
 * Two columns with a 12dp gutter and a 20dp margin, per docs/design/design-system.md §5. This is
 * what the caregiver sees first and often all they see, so nothing is on it that does not have to
 * be there.
 *
 * Three columns on a small tablet and four on a large one — the tiles keep their size and the
 * screen gets more of them, rather than two tiles growing to the width of a table. The search
 * field and the empty state stop at reading width in the middle of it, because a text field
 * stretched across a tablet is a target nobody aims at and a centred paragraph 1120dp wide is not
 * a paragraph.
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
    onOpenLog: () -> Unit,
    onUseTemplate: (CardDraft) -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LarovaScaffold(
        title = stringResource(Res.string.home_greeting),
        onHelp = onHelp,
        modifier = modifier,
        // The one screen that is a grid rather than a column of reading, and the only one that
        // earns the wider cap.
        contentWidth = ContentWidth.Grid,
        actions = {
            // Two buttons rather than an overflow menu. Both are things somebody comes to this
            // screen intending to do, and a menu asks them to guess that they are behind it —
            // which is exactly how backup ended up somewhere nobody found it. The log is in both
            // views: whoever is with the child is the person who knows that lunch did not happen.
            IconButton(onClick = onOpenLog) {
                Icon(
                    imageVector = History,
                    contentDescription = stringResource(Res.string.settings_log),
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Sliders,
                    contentDescription = stringResource(Res.string.settings_title),
                )
            }
        },
        floatingActionButton = {
            // Absent in caregiver view rather than disabled. A greyed-out button is a question
            // ("why not?") asked of someone who is here to read a bedtime routine, and the answer
            // does not concern them.
            if (isParentView) {
                ParentActions(onArrange = onArrange, onAddTile = onAddTile)
            }
        },
    ) { insets ->
        Column(
            modifier = Modifier.fillMaxSize().padding(insets),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
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

                state.tiles.isNotEmpty() -> TileGrid(
                    tiles = state.tiles,
                    onOpenTile = onOpenTile,
                    isParentView = isParentView,
                )

                state.isSearching -> Message(text = stringResource(Res.string.home_search_empty))

                else -> HomeEmptyState(onUseTemplate = onUseTemplate)
            }
        }
    }
}

/**
 * What parent view puts on the grid: rearranging above adding.
 *
 * Two sizes, and the difference is the whole point. Adding a tile is what somebody unlocked parent
 * view to do, so it is extended and says so in words — "+" on a grid of tiles is not
 * self-explanatory to someone who did not choose this phone. Rearranging is the same kind of act
 * on the same content, so it belongs beside it rather than in a menu, but it is the rarer one and
 * takes the smaller button.
 */
@Composable
private fun ParentActions(
    onArrange: () -> Unit,
    onAddTile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SmallFloatingActionButton(
            onClick = onArrange,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) {
            Icon(
                imageVector = Reorder,
                contentDescription = stringResource(Res.string.arrange_title),
            )
        }
        ExtendedFloatingActionButton(
            onClick = onAddTile,
            icon = { Icon(imageVector = TileSymbol.STAR.image, contentDescription = null) },
            text = { Text(stringResource(Res.string.home_add_tile)) },
        )
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
            .widthIn(max = Dimens.ReadingWidth)
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenMargin, vertical = 4.dp),
    )
}

@Composable
private fun Message(text: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .widthIn(max = Dimens.ReadingWidth)
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
    isParentView: Boolean,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(tileColumns()),
        modifier = modifier.fillMaxSize(),
        // Deliberately no key on the grid itself: the search results and the ordered board are two
        // different lists, and animating between them as though tiles had moved would suggest the
        // start screen had been rearranged.
        contentPadding = PaddingValues(
            start = Dimens.ScreenMargin,
            end = Dimens.ScreenMargin,
            top = Dimens.GridGutter,
            // Room to scroll the last tile out from under the two buttons. A floating button that
            // covers a tile is a tile somebody cannot open, and the bottom right is exactly where
            // the newest one lands.
            bottom = if (isParentView) FAB_CLEARANCE else Dimens.ScreenMargin,
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
    is TileSubtitle.Numbers -> pluralStringResource(Res.plurals.tile_number_count, count, count)
    is TileSubtitle.Numbers -> pluralStringResource(Res.plurals.tile_number_count, count, count)
    TileSubtitle.Folder -> stringResource(Res.string.tile_folder)
}

/**
 * What a fresh installation shows.
 *
 * It says what a tile can hold, because "add a tile" means nothing to someone who has never seen
 * one. The templates offered on first run (M2) will make this rare, but a screen that can render
 * zero tiles is better than one that assumes it never has to.
 */
@Composable
@OptIn(ExperimentalLayoutApi::class)
private fun HomeEmptyState(
    onUseTemplate: (CardDraft) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Resolved here, in composition, because every word of a template is a string resource and a
    // click handler is no place to read one. Six string lookups, and only while the grid is empty.
    val drafts = TemplateId.entries.map { it to templateDraft(it) }

    Column(
        modifier = modifier
            .widthIn(max = Dimens.ReadingWidth)
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

        // The six starting points from concept.md §4.6, on the one screen where an empty grid would
        // otherwise be asking "and now what?". They answer it twice over: something real appears,
        // and the tile that appears shows what a guide, a list, a table or a number looks like
        // filled in. Offered in both views, because a fresh installation has no PIN and nothing yet
        // to protect.
        Text(
            text = stringResource(Res.string.home_start_with),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for ((template, draft) in drafts) {
                OutlinedButton(
                    onClick = { onUseTemplate(draft) },
                    modifier = Modifier.heightIn(min = Dimens.MinTouchTarget),
                ) {
                    Text(template.label())
                }
            }
        }
    }
}

/** The two floating buttons plus the gap under them, in the one place the number is used. */
private val FAB_CLEARANCE = 148.dp
