package app.larova.feature.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.larova.core.ui.component.LarovaScaffold
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.transfer_backup
import app.larova.core.ui.resources.transfer_backup_hint
import app.larova.core.ui.resources.transfer_restore
import app.larova.core.ui.resources.transfer_restore_hint
import app.larova.core.ui.resources.transfer_title
import app.larova.core.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/**
 * Backup and restore.
 *
 * Both are described here before either works, because the wording is the feature: the destination
 * is chosen through the system dialog, which lists every cloud provider installed on the phone.
 * There is no Drive SDK, no OAuth and no cloud integration on our side — that is what makes an
 * offline app able to back up at all.
 */
@Composable
fun TransferScreen(
    onBack: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LarovaScaffold(
        title = stringResource(Res.string.transfer_title),
        onHelp = onHelp,
        onBack = onBack,
        modifier = modifier,
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = Dimens.ScreenMargin, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Section(
                title = stringResource(Res.string.transfer_backup),
                hint = stringResource(Res.string.transfer_backup_hint),
            )
            Section(
                title = stringResource(Res.string.transfer_restore),
                hint = stringResource(Res.string.transfer_restore_hint),
            )
        }
    }
}

@Composable
private fun Section(title: String, hint: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp),
        )
    }
}
