package app.larova.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.larova.core.ui.theme.Dimens
import app.larova.core.ui.theme.LocalAppMode
import app.larova.core.ui.theme.TileColor
import app.larova.core.ui.theme.resolve

/**
 * One tile on the start screen.
 *
 * Takes a [TileColor] — a token — and resolves it against the active mode here. Callers holding a
 * stored `Card.colorToken` string go through `TileColor.fromKey(...)`, which falls back to the
 * default instead of failing, so a tile written by a newer version still draws.
 *
 * Colour is never the only differentiator: every tile also carries a symbol and a label, which is
 * what keeps `moss` and `clay` apart under red-green colour blindness.
 */
@Composable
fun TileCard(
    title: String,
    symbol: String,
    color: TileColor,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val colors = color.resolve(LocalAppMode.current)

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = Dimens.TileMinHeight),
        shape = RoundedCornerShape(Dimens.TileRadius),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SymbolChip(symbol = symbol, modifier = Modifier.size(44.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.accent,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.accent,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * The symbol sits on a translucent overlay rather than a second opaque colour, so one chip style
 * works on all eight tile surfaces in all three modes.
 *
 * The symbol arrives as text for now. The registry that maps a stored symbol key to a drawing
 * comes with the tile editor; what matters at this point is that the key is what gets stored.
 */
@Composable
private fun SymbolChip(symbol: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.ChipRadius),
        color = app.larova.core.ui.theme.LocalSurfaces.current.chipOverlay,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = symbol, style = MaterialTheme.typography.titleMedium)
        }
    }
}
