package app.larova.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.larova.core.domain.model.LogKind
import app.larova.core.domain.usecase.LOG_RETENTION_DAYS
import app.larova.core.domain.usecase.LogLine
import app.larova.core.ui.component.LarovaScaffold
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.edit_cancel
import app.larova.core.ui.resources.log_add
import app.larova.core.ui.resources.log_call_prepared
import app.larova.core.ui.resources.log_card_opened
import app.larova.core.ui.resources.log_check_toggled
import app.larova.core.ui.resources.log_clear_question
import app.larova.core.ui.resources.log_empty
import app.larova.core.ui.resources.log_note_hint
import app.larova.core.ui.resources.log_retention
import app.larova.core.ui.resources.log_tile_gone
import app.larova.core.ui.resources.settings_log
import app.larova.core.ui.resources.settings_log_clear
import app.larova.core.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/**
 * The log: what happened, when, newest first.
 *
 * A plain list and nothing more. No totals, no streaks, no "three days in a row" — the log is the
 * documentation feature for the parents precisely because it does not interpret anything
 * (docs/concept.md §2.2 and §4.4).
 *
 * Adding a line is available in caregiver view. Whoever is with the child is the person who knows
 * that lunch did not happen, and asking them for a PIN first would mean it never gets written down.
 * Clearing is parent view only, because it is the one thing here that destroys something.
 *
 * [formatTime] comes from the platform: whether this person reads 18:12 or 6:12 pm is the phone's
 * business, not this screen's.
 */
@Composable
fun LogScreen(
    lines: List<LogLine>,
    note: String,
    isParentView: Boolean,
    formatTime: (kotlin.time.Instant) -> String,
    onNoteChange: (String) -> Unit,
    onAddNote: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingClear by remember { mutableStateOf(false) }

    LarovaScaffold(
        title = stringResource(Res.string.settings_log),
        onHelp = onHelp,
        onBack = onBack,
        modifier = modifier,
    ) { insets ->
        Column(modifier = Modifier.fillMaxSize().padding(insets)) {
            NoteField(
                note = note,
                onNoteChange = onNoteChange,
                onAddNote = onAddNote,
            )

            if (lines.isEmpty()) {
                Message(text = stringResource(Res.string.log_empty))
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(lines, key = { it.id.toString() }) { line ->
                        LogRow(line = line, formatTime = formatTime)
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }

            Text(
                text = stringResource(Res.string.log_retention, LOG_RETENTION_DAYS),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = Dimens.ScreenMargin, vertical = 8.dp),
            )

            if (isParentView && lines.isNotEmpty()) {
                TextButton(
                    onClick = { confirmingClear = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.MinTouchTarget)
                        .padding(bottom = 8.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.settings_log_clear),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (confirmingClear) {
        AlertDialog(
            onDismissRequest = { confirmingClear = false },
            title = { Text(stringResource(Res.string.settings_log_clear)) },
            text = { Text(stringResource(Res.string.log_clear_question)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingClear = false
                        onClear()
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.settings_log_clear),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingClear = false }) {
                    Text(stringResource(Res.string.edit_cancel))
                }
            },
        )
    }
}

/** At the top, because writing a line is what somebody came here to do. */
@Composable
private fun NoteField(note: String, onNoteChange: (String) -> Unit, onAddNote: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenMargin, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = note,
            onValueChange = onNoteChange,
            label = { Text(stringResource(Res.string.log_note_hint)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onAddNote,
            enabled = note.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Dimens.MinTouchTarget),
        ) {
            Text(stringResource(Res.string.log_add))
        }
    }
}

@Composable
private fun LogRow(line: LogLine, formatTime: (kotlin.time.Instant) -> String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.ScreenMargin, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = formatTime(line.at),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = line.describe(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * What a line says, in words.
 *
 * Assembled from a resource with a placeholder rather than from pieces, because word order is the
 * translator's to decide. A tile deleted since leaves its entries standing and says so — what
 * happened still happened, and inventing a title for it would be worse than admitting the tile is
 * gone.
 */
@Composable
private fun LogLine.describe(): String {
    val subject = cardTitle ?: stringResource(Res.string.log_tile_gone)
    return when (kind) {
        LogKind.CARD_OPENED -> stringResource(Res.string.log_card_opened, subject)
        LogKind.CHECK_TOGGLED -> stringResource(Res.string.log_check_toggled, subject)
        LogKind.CALL_PREPARED -> stringResource(Res.string.log_call_prepared, subject)
        // The note is what a person wrote. It is shown as written and never wrapped in anything.
        LogKind.MANUAL_NOTE -> note.orEmpty()
    }
}

@Composable
private fun Message(text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenMargin),
        verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
