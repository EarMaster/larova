package app.larova.feature.transfer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.larova.core.domain.export.ImportMode
import app.larova.core.domain.model.LastBackup
import app.larova.core.ui.component.ActionCard
import app.larova.core.ui.component.LarovaScaffold
import app.larova.core.ui.icon.OpenFile
import app.larova.core.ui.icon.SaveFile
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.transfer_backup
import app.larova.core.ui.resources.transfer_backup_failed
import app.larova.core.ui.resources.transfer_backup_hint
import app.larova.core.ui.resources.transfer_damaged
import app.larova.core.ui.resources.transfer_last_backup
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
import kotlin.time.Instant
import org.jetbrains.compose.resources.stringResource

/**
 * Backup and restore.
 *
 * The destination and the source both come from the system file dialog, which lists every cloud
 * provider installed on the phone. That is the whole of Larova's cloud support: no SDK, no OAuth,
 * no account, and nothing that can stop working when a provider changes its API.
 *
 * Two actions, each of them a card that contains its own explanation — see the prototype at
 * `docs/design/prototypes/screens.html`. The mockup puts a row of destination chips under
 * "Back up" — this phone, a cloud drive, a USB stick — and they are deliberately not here: Larova
 * has no idea which of those a given phone actually offers, so naming them is a promise about
 * somebody else's dialog. "You choose where it goes" is the true version, and it is already in
 * the card. A heading, a paragraph and a button underneath is three things to read in order
 * before pressing the third; a card is one thing to read and the same thing to press. It also
 * fixes the accessibility half of the same problem, because the sentence is then part of the
 * button's label instead of unrelated text above it.
 */
@Composable
fun TransferScreen(
    state: TransferUiState,
    formatDate: (Instant) -> String,
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
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (state.isBusy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            ActionCard(
                icon = SaveFile,
                title = stringResource(Res.string.transfer_backup),
                description = stringResource(Res.string.transfer_backup_hint),
                onClick = onBackup,
                enabled = !state.isBusy,
            )

            ActionCard(
                icon = OpenFile,
                title = stringResource(Res.string.transfer_restore),
                description = stringResource(Res.string.transfer_restore_hint),
                onClick = onRestore,
                enabled = !state.isBusy,
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
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            LastBackupNote(backup = state.lastBackup, formatDate = formatDate)
        }
    }

    val preview = state.preview
    if (preview != null) {
        ImportChoiceDialog(
            summary = stringResource(
                Res.string.transfer_preview,
                preview.counts.cards,
                preview.counts.media,
                formatDate(preview.exportedAt),
            ),
            label = preview.label,
            onReplace = { onConfirmImport(ImportMode.REPLACE) },
            onMerge = { onConfirmImport(ImportMode.MERGE) },
            onCancel = onCancelImport,
        )
    }
}

/**
 * When this installation last wrote a backup — absent entirely until one has.
 *
 * "Never backed up" is not written anywhere, and that is deliberate: the screen a parent opens to
 * make their first backup should not open by telling them off. The absence of the line is the same
 * information without the tone.
 */
@Composable
private fun LastBackupNote(
    backup: LastBackup?,
    formatDate: (Instant) -> String,
    modifier: Modifier = Modifier,
) {
    if (backup == null) return

    Column(
        modifier = modifier.padding(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = stringResource(Res.string.transfer_last_backup),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = stringResource(
                Res.string.transfer_preview,
                backup.cards,
                backup.media,
                formatDate(backup.at),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
