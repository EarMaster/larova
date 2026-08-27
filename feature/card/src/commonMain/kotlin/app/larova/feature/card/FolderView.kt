package app.larova.feature.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.larova.core.ui.component.TileCard
import app.larova.core.ui.theme.Dimens

/**
 * What is inside a folder: the same two-column grid as the start screen.
 *
 * The same grid on purpose. A caregiver who has learned to read the start screen has learned to
 * read this, and a folder that looked like a list would be a second thing to learn for no gain.
 *
 * One level deep and no further (docs/concept.md §4.1) — which is enforced where tiles are made,
 * not here: the editor does not offer the folder type when it was opened from inside one.
 */
@Composable
fun FolderView(
    tiles: List<FolderTile>,
    onOpenTile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tiles.isEmpty()) {
        EmptyPayloadNote(modifier = modifier)
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
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
                subtitle = tile.subtitle,
                colorToken = tile.colorToken,
                symbolKey = tile.symbolKey,
                onClick = { onOpenTile(tile.id) },
            )
        }
    }
}
