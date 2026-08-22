package app.larova.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import app.larova.core.ui.component.LarovaScaffold
import app.larova.core.ui.component.TileCard
import app.larova.core.ui.icon.MoreVertical
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.cd_menu
import app.larova.core.ui.resources.home_greeting
import app.larova.core.ui.resources.settings_title
import app.larova.core.ui.resources.transfer_title
import app.larova.core.ui.theme.Dimens
import app.larova.core.ui.theme.TileColor
import org.jetbrains.compose.resources.stringResource

/**
 * The start screen.
 *
 * Two columns with a 12dp gutter and a 20dp margin, per docs/design/design-system.md §5. The grid
 * is what the caregiver sees first and often all they see, so nothing is added to it that does not
 * have to be there.
 */
@Composable
fun HomeScreen(
    tiles: List<HomeTile>,
    onOpenTile: (String) -> Unit,
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
            HomeMenu(onOpenSettings = onOpenSettings, onOpenTransfer = onOpenTransfer)
        },
    ) { insets ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(insets),
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
                    symbol = tile.symbol,
                    color = TileColor.fromKey(tile.colorToken),
                    onClick = { onOpenTile(tile.id) },
                )
            }
        }
    }
}

@Composable
private fun HomeMenu(onOpenSettings: () -> Unit, onOpenTransfer: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(imageVector = MoreVertical, contentDescription = stringResource(Res.string.cd_menu))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.settings_title)) },
            onClick = {
                expanded = false
                onOpenSettings()
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
}

/**
 * Shown when there is nothing to show. Not reachable yet — the templates offered on first run mean
 * a real installation never starts empty (docs/concept.md §4.6) — but a screen that can render zero
 * tiles is better than one that assumes it never has to.
 */
@Composable
fun HomeEmptyState(modifier: Modifier = Modifier) {
    Text(
        text = stringResource(Res.string.home_greeting),
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier,
    )
}
