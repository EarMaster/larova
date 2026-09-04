package app.larova.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.larova.core.ui.theme.Dimens
import app.larova.core.ui.theme.LocalAppMode
import app.larova.core.ui.theme.TileColor
import app.larova.core.ui.theme.resolve

/**
 * A whole paragraph that is also a button — the "big action" from
 * `docs/design/prototypes/screens.html`.
 *
 * The reason it is a card rather than a labelled button with text above it is that the label alone
 * is never enough here. "Back up" and "Restore" both need a sentence before somebody will press
 * them, and a sentence that sits *outside* the target is a sentence a screen reader reads as
 * unrelated text and a thumb misses by 40dp. Everything that explains the action is inside the
 * thing that performs it.
 *
 * The icon chip carries a tile colour rather than a theme colour, which is what keeps these
 * recognisable as Larova and not as Material — `sand` by default, the same desaturated stand-in
 * that new tiles get.
 *
 * **This is the shape a block on the settings screen takes.** Backup, the full version and the
 * contribution are all one of these, and anything added there should be too: an icon chip, a
 * title, and the sentence that explains it, inside the target that performs it. A section built
 * out of loose `Text`s and a `TextButton` looked like a different app on the same screen, and it
 * is also the arrangement that fails every rule in the paragraph above — the sentence outside the
 * target, the button a thumb has to find. Reach for this before inventing a layout.
 */
@Composable
fun ActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    token: TileColor = TileColor.SAND,
    /**
     * The state of the thing, above the sentence that explains it: "Not unlocked", "Unlocked".
     *
     * A separate line rather than the first words of [description], because it is the one thing
     * on the card somebody scans for and a paragraph is where it would go to hide. Most cards
     * have no state to report and leave this null.
     */
    status: String? = null,
) {
    val colors = token.resolve(LocalAppMode.current)

    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget),
        shape = RoundedCornerShape(Dimens.TileRadius),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                modifier = Modifier.size(44.dp),
                shape = RoundedCornerShape(Dimens.ChipRadius),
                color = colors.surface,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    // No content description: the title beside it says the same thing, and a
                    // screen reader announcing both reads the button's name twice.
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = colors.accent,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                status?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
