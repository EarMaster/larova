package app.larova.feature.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
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
import app.larova.core.domain.usecase.HelpContact
import app.larova.core.ui.component.LarovaScaffold
import app.larova.core.ui.icon.TileSymbol
import app.larova.core.ui.icon.image
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.help_call_note
import app.larova.core.ui.resources.help_no_contacts
import app.larova.core.ui.resources.help_sheet_title
import app.larova.core.ui.theme.Dimens
import app.larova.core.ui.theme.GuideStepStyle
import app.larova.core.ui.theme.LocalAppMode
import app.larova.core.ui.theme.TileColor
import app.larova.core.ui.theme.resolve
import org.jetbrains.compose.resources.stringResource

/**
 * What the help bar opens.
 *
 * Read under time pressure, possibly by someone who has never seen this app: a short list of
 * people, each one large enough to hit without aiming, each tap opening the dialler with the number
 * already in it. Nothing else is on this screen.
 *
 * Larova prepares the call and never places it. That keeps the app out of emergency-services
 * regulation, needs no permission, and makes a mistaken tap harmless — and it is said in as many
 * words at the bottom, because the person reading it is entitled to know what the button will do
 * before they press it.
 */
@Composable
fun HelpScreen(
    contacts: List<HelpContact>,
    onPrepareCall: (String) -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LarovaScaffold(
        title = stringResource(Res.string.help_sheet_title),
        onHelp = onHelp,
        onBack = onBack,
        modifier = modifier,
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = Dimens.ScreenMargin, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (contacts.isEmpty()) {
                Text(
                    // Not an error. It means the parents have not marked a number yet, and the
                    // person reading this needs to know that rather than to keep looking.
                    text = stringResource(Res.string.help_no_contacts),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                for (contact in contacts) {
                    ContactRow(contact = contact, onClick = { onPrepareCall(contact.number) })
                }
            }

            Text(
                text = stringResource(Res.string.help_call_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/**
 * Deliberately larger than a list row elsewhere in the app. This is the one screen where the person
 * using it may be holding a crying child, and 56dp is a floor rather than a target.
 */
@Composable
private fun ContactRow(
    contact: HelpContact,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TileColor.CLAY.resolve(LocalAppMode.current)

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
                    text = contact.displayName,
                    style = GuideStepStyle,
                    color = colors.accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                val relation = contact.relation
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
                    text = contact.number,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.accent,
                )
            }
        }
    }
}

private val CONTACT_ROW_HEIGHT = 88.dp
