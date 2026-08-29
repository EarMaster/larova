package app.larova.core.ui.component

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.larova.core.ui.icon.TileSymbol
import app.larova.core.ui.icon.image
import app.larova.core.ui.theme.Dimens
import app.larova.core.ui.theme.GuideStepStyle
import app.larova.core.ui.theme.LocalAppMode
import app.larova.core.ui.theme.TileColor
import app.larova.core.ui.theme.resolve

/**
 * One person to call: a name, what they are to the child, and the number.
 *
 * Shared between the help sheet and a call tile deliberately. They are the same act — press a
 * person, the dialler opens with their number in it — and a caregiver who has learned one row has
 * learned the other. Two rows that looked different would be two things to learn at the moment
 * there is least room to learn anything.
 *
 * Deliberately larger than a list row elsewhere in the app. This is pressed by somebody who may be
 * holding a crying child, and 56dp is a floor rather than a target.
 */
@Composable
fun ContactRow(
    displayName: String,
    number: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    relation: String? = null,
    token: TileColor = TileColor.CLAY,
) {
    val colors = token.resolve(LocalAppMode.current)

    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = CONTACT_ROW_HEIGHT),
        shape = RoundedCornerShape(Dimens.TileRadius),
        // A clickable Surface already announces itself as a button, and the texts inside it are
        // read as its label — one control, name and relation together, rather than three separate
        // lines beside a button.
        color = colors.surface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Icon(
                imageVector = TileSymbol.PHONE.image,
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(32.dp),
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = displayName,
                    style = GuideStepStyle,
                    color = colors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (relation != null) {
                    Text(
                        text = relation,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.accent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    // Never mirrored in a right-to-left layout: a phone number reads the same way
                    // in every language, and reversing one produces a different number.
                    text = number,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.accent,
                )
            }
        }
    }
}

private val CONTACT_ROW_HEIGHT = 88.dp
