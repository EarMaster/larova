package app.larova.feature.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.larova.core.domain.export.ExportManifest
import app.larova.core.domain.export.ImportMode
import app.larova.core.ui.component.LarovaScaffold
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.transfer_backup
import app.larova.core.ui.resources.transfer_backup_failed
import app.larova.core.ui.resources.transfer_backup_hint
import app.larova.core.ui.resources.transfer_damaged
import app.larova.core.ui.resources.transfer_merge
import app.larova.core.ui.resources.transfer_preview
import app.larova.core.ui.resources.transfer_replace
import app.larova.core.ui.resources.transfer_restore
import app.larova.core.ui.resources.transfer_restore_hint
import app.larova.core.ui.resources.transfer_restored
import app.larova.core.ui.resources.transfer_saved
import app.larova.core.ui.resources.transfer_title
import app.larova.core.ui.resources.transfer_unreadable
import app.larova.core.ui.resources.transfer_version_too_new
import app.larova.core.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/**
 * Backup and restore.
 *
 * The destination and the source both come from the system file dialog, which lists every cloud
 * provider installed on the phone. That is the whole of Larova's cloud support: no SDK, no OAuth,
 * no account, and nothing that can stop working when a provider changes its API.
 */
@Composable
fun TransferScreen(
    state: TransferUiState,
    formatDate: (ExportManifest) -> String,
    onBackup: () -> Unit,
    onRestore: () -> Unit,
    onConfirmImport: (ImportMode) -> Unit,
    onCancelImport: () -> Unit,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenMargin, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (state.isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Section(
                title = stringResource(Res.string.transfer_backup),
                hint = stringResource(Res.string.transfer_backup_hint),
                action = stringResource(Res.string.transfer_backup),
                enabled = !state.isBusy,
                onClick = onBackup,
            )

            Section(
                title = stringResource(Res.string.transfer_restore),
                hint = stringResource(Res.string.transfer_restore_hint),
                action = stringResource(Res.string.transfer_restore),
                enabled = !state.isBusy,
                onClick = onRestore,
            )

            val outcome = state.outcome
            if (outcome != null) {
                Text(
                    text = outcome.message(),
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (outcome.isProblem) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onBackground
                    },
                )
            }
        }
    }

    val preview = state.preview
    if (preview != null) {
        ImportChoiceDialog(
            summary = stringResource(
                Res.string.transfer_preview,
                preview.counts.cards,
                preview.counts.media,
                formatDate(preview),
            ),
            label = preview.label,
            onReplace = { onConfirmImport(ImportMode.REPLACE) },
            onMerge = { onConfirmImport(ImportMode.MERGE) },
            onCancel = onCancelImport,
        )
    }
}

/**
 * The one dialog in the app that offers something irreversible, so it says what is in the file
 * first and names both choices in the user's own terms — "replace everything" and "add to what is
 * here" — rather than asking them to understand the word merge.
 */
@Composable
private fun ImportChoiceDialog(
    summary: String,
    label: String?,
    onReplace: () -> Unit,
    onMerge: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(label ?: stringResource(Res.string.transfer_restore)) },
        text = { Text(summary) },
        confirmButton = {
            TextButton(onClick = onMerge) { Text(stringResource(Res.string.transfer_merge)) }
        },
        dismissButton = {
            TextButton(onClick = onReplace) {
                Text(
                    text = stringResource(Res.string.transfer_replace),
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
    )
}

@Composable
private fun Section(
    title: String,
    hint: String,
    action: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.MinTouchTarget),
        ) {
            Text(action)
        }
    }
}

private val TransferOutcome.isProblem: Boolean
    get() = this !is TransferOutcome.BackedUp && this !is TransferOutcome.Restored

@Composable
private fun TransferOutcome.message(): String = when (this) {
    is TransferOutcome.BackedUp -> stringResource(Res.string.transfer_saved, cards, media)
    is TransferOutcome.Restored -> stringResource(Res.string.transfer_restored, cards, media)
    TransferOutcome.BackupFailed -> stringResource(Res.string.transfer_backup_failed)
    is TransferOutcome.FileTooNew -> stringResource(Res.string.transfer_version_too_new)
    TransferOutcome.FileDamaged -> stringResource(Res.string.transfer_damaged)
    TransferOutcome.FileUnreadable -> stringResource(Res.string.transfer_unreadable)
}
