package app.larova.feature.card

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import app.larova.core.domain.model.CardPayload
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.cd_table_cell
import app.larova.core.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/**
 * A table the parents laid out themselves: what happens when, what is allowed, who to ask.
 *
 * Columns share the width evenly and cells wrap rather than being cut off. That is the whole layout
 * decision, and it is the reason four columns is the limit: this is read on a phone, sometimes at
 * 200 % font scale, and a fifth column turns every cell into two words on four lines. No horizontal
 * scrolling — content that has to be dragged sideways is content somebody misses.
 *
 * Larova does not read the table. It has no idea which column is a time and which is a dose, and
 * that is deliberate (docs/concept.md §2.2): a table with user-defined columns is a notebook page,
 * while a table the app understands is a calculator, and one of those is a medical device.
 */
@Composable
fun TableView(table: CardPayload.Table, modifier: Modifier = Modifier) {
    if (table.columns.isEmpty() || table.rows.isEmpty()) {
        EmptyPayloadNote(modifier = modifier)
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = Dimens.ScreenMargin),
    ) {
        HeaderRow(columns = table.columns)
        HorizontalDivider()

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(table.rows) { row ->
                DataRow(columns = table.columns, row = row)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
        }
    }
}

/**
 * The headings, in a weight of their own and on a tinted band.
 *
 * Two carriers rather than one, because colour is never the only thing that says "this row is
 * different" — someone reading this in night mode, or not distinguishing the tint at all, still
 * has the heavier type and the line underneath.
 */
@Composable
private fun HeaderRow(columns: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (column in columns) {
            Text(
                text = column,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * One row, cell by cell.
 *
 * Each cell carries its own heading for a screen reader — "Time, six o'clock" — rather than the row
 * being read as a run of values. A heading announced once at the top of the table and then twenty
 * bare values under it is a table nobody can follow by ear, and pairing them here needs no text
 * assembled in code: the pair is one string resource with two placeholders, so the order of the two
 * halves stays the translator's to decide.
 */
@Composable
private fun DataRow(columns: List<String>, row: List<String>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        columns.forEachIndexed { index, column ->
            val value = row.getOrNull(index).orEmpty()
            // A column nobody named has nothing to pair the value with, so the value stands alone.
            val spoken = if (column.isEmpty()) {
                value
            } else {
                stringResource(Res.string.cd_table_cell, column, value)
            }
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .weight(1f)
                    .clearAndSetSemantics { contentDescription = spoken },
            )
        }
    }
}
