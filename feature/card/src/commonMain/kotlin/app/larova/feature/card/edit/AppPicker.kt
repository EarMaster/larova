package app.larova.feature.card.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.Image
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.unit.dp
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.app_choose
import app.larova.core.ui.resources.app_none_found
import app.larova.core.ui.resources.edit_cancel
import app.larova.core.ui.resources.home_search
import app.larova.core.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/**
 * Choosing which app a tile opens.
 *
 * A dialog rather than a destination of its own. The graph is two levels deep on purpose, and an
 * app list is a step inside making one tile, not a place in the app somebody navigates to.
 *
 * The search field is there because a phone has a hundred apps on it. Icons are what a parent
 * actually recognises — the label of the app they think of as "the music one" is rarely the word
 * they would type.
 */
@Composable
fun AppPickerDialog(
    apps: List<AppChoice>,
    query: String,
    onQueryChange: (String) -> Unit,
    onPick: (AppChoice) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.app_choose)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    label = { Text(stringResource(Res.string.home_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                if (apps.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.app_none_found),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        // Bounded so the dialog cannot grow past the screen on a phone with two
                        // hundred apps, and scrollable inside that.
                        modifier = Modifier.heightIn(max = 360.dp),
                    ) {
                        items(apps, key = { it.packageName }) { app ->
                            AppRow(app = app, onPick = { onPick(app) })
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(Res.string.edit_cancel))
            }
        },
    )
}

@Composable
private fun AppRow(app: AppChoice, onPick: () -> Unit) {
    TextButton(
        onClick = onPick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val icon = app.icon
            if (icon != null) {
                Image(
                    bitmap = icon,
                    // The label beside it says which app this is; a screen reader announcing the
                    // icon as well would read every row twice.
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                )
            }
            Text(
                text = app.label,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp),
            )
        }
    }
}
