package app.larova.feature.card

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import app.larova.core.domain.model.CardPayload
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.checklist_progress
import app.larova.core.ui.resources.checklist_resets_daily
import app.larova.core.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/**
 * A checklist.
 *
 * Ticking an item is the one write available without unlocking parent view: it is reading the tile,
 * not editing it. The item text itself, and whether the list resets, stay with the parents.
 */
@Composable
fun ChecklistView(
    checklist: CardPayload.Checklist,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (checklist.items.isEmpty()) {
        EmptyPayloadNote(modifier = modifier)
        return
    }

    val done = checklist.items.count { it.done }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenMargin),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(Res.string.checklist_progress, done, checklist.items.size),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 4.dp),
        )

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            itemsIndexed(checklist.items) { index, item ->
                ChecklistRow(
                    text = item.text,
                    done = item.done,
                    onToggle = { onToggle(index) },
                )
            }
        }

        if (checklist.resetDaily) {
            Text(
                // The time is part of the setting rather than a fixed hour, so it comes from the
                // string's placeholder even though only one value exists today.
                text = stringResource(Res.string.checklist_resets_daily, DEFAULT_RESET_TIME),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        }
    }
}

/**
 * The whole row is the target, not just the box — 56dp of it. `toggleable` on the row also means a
 * screen reader announces one checkbox rather than a checkbox and a separate line of text.
 */
@Composable
private fun ChecklistRow(
    text: String,
    done: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget)
            .toggleable(value = done, role = Role.Checkbox, onValueChange = { onToggle() }),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Checkbox(checked = done, onCheckedChange = null)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            // Struck through as well as ticked: colour and the box are never the only carriers of
            // "this one is finished".
            textDecoration = if (done) TextDecoration.LineThrough else null,
            color = if (done) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onBackground
            },
        )
    }
}

/** Until reset times are configurable, this is the hour the string talks about. */
private const val DEFAULT_RESET_TIME = "06:00"
