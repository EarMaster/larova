package app.larova.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.larova.core.ui.icon.TileSymbol
import app.larova.core.ui.icon.image
import app.larova.core.ui.theme.Dimens
import app.larova.core.ui.theme.LocalAppMode
import app.larova.core.ui.theme.LocalSurfaces
import app.larova.core.ui.theme.TileColor
import app.larova.core.ui.theme.resolve

/**
 * One tile on the start screen.
 *
 * Takes the stored **keys** — a colour token and a symbol — and resolves both here, against the
 * active appearance mode. Unknown keys fall back rather than failing, which is what lets a tile
 * written by a newer version still draw. Neither fallback touches what is stored.
 *
 * Colour is never the only differentiator: every tile also carries a symbol and a label, which is
 * what keeps `moss` and `clay` apart under red-green colour blindness.
 *
 * Every tile is [tileHeight] tall regardless of what it says, so a row of them lines up — see
 * [Dimens.TileChrome]. The symbol sits at the top and the words at the bottom rather than the two
 * being stacked from the top, which is what makes a tile with a one-line title look composed
 * instead of unfinished.
 */
@Composable
fun TileCard(
    title: String,
    colorToken: String,
    symbolKey: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
) {
    val colors = TileColor.fromKey(colorToken).resolve(LocalAppMode.current)

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(tileHeight()),
        shape = RoundedCornerShape(Dimens.TileRadius),
        colors = CardDefaults.cardColors(containerColor = colors.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            SymbolChip(
                symbolKey = symbolKey,
                tint = colors.accent,
                modifier = Modifier.size(44.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
}

/**
 * How tall every tile is.
 *
 * Only the text half is multiplied by the font scale. Scaling the whole tile would double the
 * padding and the symbol chip along with the words at 200 %, which is how a grid of four tiles
 * becomes a grid of one and a half.
 */
@Composable
fun tileHeight(): Dp = Dimens.TileChrome + Dimens.TileText * LocalDensity.current.fontScale

/**
 * The symbol sits on a translucent overlay rather than a second opaque colour, so one chip style
 * works on all eight tile surfaces in all three modes.
 *
 * No content description: the symbol repeats what the title says, and a screen reader announcing
 * "star, Bedtime" is worse than one announcing "Bedtime".
 */
@Composable
private fun SymbolChip(symbolKey: String, tint: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.ChipRadius),
        color = LocalSurfaces.current.chipOverlay,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = TileSymbol.fromKey(symbolKey).image,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
