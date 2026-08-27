package app.larova.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.larova.core.ui.component.LarovaScaffold
import app.larova.core.ui.icon.BackArrow
import app.larova.core.ui.icon.TileSymbol
import app.larova.core.ui.icon.image
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.arrange_hint
import app.larova.core.ui.resources.arrange_title
import app.larova.core.ui.resources.cd_move_down
import app.larova.core.ui.resources.cd_move_up
import app.larova.core.ui.theme.Dimens
import app.larova.core.ui.theme.LocalAppMode
import app.larova.core.ui.theme.TileColor
import app.larova.core.ui.theme.resolve
import org.jetbrains.compose.resources.stringResource

/**
 * Rearranging the start screen.
 *
 * Buttons rather than drag and drop, deliberately. A long-press-and-drag on a grid is invisible to
 * anyone who does not already know it exists, it fights TalkBack, and it is exactly the gesture
 * that goes wrong on a phone held in one hand. Two 56dp buttons per row say what they do.
 *
 * Every move is written immediately. There is no save button to forget, and the list redraws from
 * what was stored rather than from what the screen hoped it stored.
 */
@Composable
fun ArrangeTilesScreen(
    tiles: List<HomeTile>,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LarovaScaffold(
        title = stringResource(Res.string.arrange_title),
        onHelp = onHelp,
        onBack = onBack,
        modifier = modifier,
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = Dimens.ScreenMargin),
        ) {
            Text(
                text = stringResource(Res.string.arrange_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                itemsIndexed(tiles, key = { _, tile -> tile.id }) { index, tile ->
                    ArrangeRow(
                        tile = tile,
                        canMoveUp = index > 0,
                        canMoveDown = index < tiles.lastIndex,
                        onMoveUp = { onMoveUp(index) },
                        onMoveDown = { onMoveDown(index) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ArrangeRow(
    tile: HomeTile,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TileColor.fromKey(tile.colorToken).resolve(LocalAppMode.current)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = TileSymbol.fromKey(tile.symbolKey).image,
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = tile.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(
                // The back arrow rotated a quarter turn: one drawing, and it mirrors correctly in
                // right-to-left layouts for the same reason the original does.
                imageVector = BackArrow,
                contentDescription = stringResource(Res.string.cd_move_up, tile.title),
                modifier = Modifier.graphicsLayer { rotationZ = QUARTER_TURN },
            )
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(
                imageVector = BackArrow,
                contentDescription = stringResource(Res.string.cd_move_down, tile.title),
                modifier = Modifier.graphicsLayer { rotationZ = -QUARTER_TURN },
            )
        }
    }
}

private const val QUARTER_TURN = 90f
