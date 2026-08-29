package app.larova.feature.help

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.larova.core.domain.usecase.HelpContact
import app.larova.core.ui.component.ContactRow
import app.larova.core.ui.component.LarovaScaffold
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.help_call_note
import app.larova.core.ui.resources.help_no_contacts
import app.larova.core.ui.resources.help_sheet_title
import app.larova.core.ui.theme.Dimens
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
                    ContactRow(
                        displayName = contact.displayName,
                        number = contact.number,
                        relation = contact.relation,
                        onClick = { onPrepareCall(contact.number) },
                    )
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
