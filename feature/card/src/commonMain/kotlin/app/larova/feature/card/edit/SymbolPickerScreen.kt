package app.larova.feature.card.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.larova.core.ui.component.LarovaScaffold
import app.larova.core.ui.icon.SymbolChoice
import app.larova.core.ui.icon.Symbols
import app.larova.core.ui.icon.symbolImage
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.edit_symbol
import app.larova.core.ui.resources.edit_symbol_search
import app.larova.core.ui.resources.edit_symbol_suggestions
import app.larova.core.ui.resources.home_search_empty
import app.larova.core.ui.theme.Dimens
import app.larova.core.ui.theme.LocalAppMode
import app.larova.core.ui.theme.TileColor
import app.larova.core.ui.theme.resolve
import org.jetbrains.compose.resources.stringResource

/**
 * Choosing a symbol, on a screen of its own.
 *
 * Nearly three hundred drawings do not belong inline in a form. As a block inside the editor they
 * pushed the title, the colour and the Save button off a phone screen, and a parent scrolled past
 * the thing they came to do. Here the grid has the whole screen and the editor keeps its shape.
 *
 * Sixty-eight suggestions first, because a parent making a bedtime tile wants the moon and not a
 * catalogue; search reaches the rest. Every symbol shows its name, which is how somebody finds the
 * same one again a month later — and it is what a screen reader reads out, on a grid where the
 * picture is the whole content.
 *
 * Picking one returns immediately. There is no confirm step: the choice is visible on the tile the
 * moment the editor comes back, and an extra tap on a grid of three hundred is an extra tap three
 * hundred times.
 */
@Composable
fun SymbolPickerScreen(
    selectedKey: String,
    colorToken: String,
    onPick: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val trimmed = query.trim()
    val results = remember(trimmed) { Symbols.matching(trimmed) }

    LarovaScaffold(
        title = stringResource(Res.string.edit_symbol),
        // Parent-view work, opened on purpose. See LarovaScaffold.
        onHelp = null,
        onBack = onBack,
        modifier = modifier,
    ) { insets ->
        Column(modifier = Modifier.fillMaxSize().padding(insets)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                label = { Text(stringResource(Res.string.edit_symbol_search)) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenMargin, vertical = 4.dp),
            )

            if (results.isEmpty()) {
                Text(
                    text = stringResource(Res.string.home_search_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(Dimens.ScreenMargin),
                )
                return@Column
            }

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = SYMBOL_CELL),
                contentPadding = PaddingValues(Dimens.ScreenMargin),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // The heading only appears while browsing. Once somebody has typed, the results are
                // one list and a "Suggestions" label above them would be a claim about ranking.
                if (trimmed.isEmpty()) {
                    item(span = { androidx.compose.foundation.lazy.grid.GridItemSpan(maxLineSpan) }) {
                        Text(
                            text = stringResource(Res.string.edit_symbol_suggestions),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }

                items(results, key = { it.key }) { choice ->
                    SymbolCell(
                        choice = choice,
                        isSelected = choice.key == selectedKey,
                        colorToken = colorToken,
                        onPick = { onPick(choice.key) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SymbolCell(
    choice: SymbolChoice,
    isSelected: Boolean,
    colorToken: String,
    onPick: () -> Unit,
) {
    val colors = TileColor.fromKey(colorToken).resolve(LocalAppMode.current)

    Column(
        modifier = Modifier
            .selectable(selected = isSelected, role = Role.RadioButton, onClick = onPick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Surface(
            modifier = Modifier.size(Dimens.MinTouchTarget),
            shape = RoundedCornerShape(Dimens.ChipRadius),
            color = if (isSelected) colors.surface else MaterialTheme.colorScheme.surfaceVariant,
            border = if (isSelected) {
                androidx.compose.foundation.BorderStroke(3.dp, colors.accent)
            } else {
                null
            },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = symbolImage(choice.key),
                    // The name is written underneath, and the row already announces itself as a
                    // choice — describing the drawing here would read it twice.
                    contentDescription = null,
                    tint = if (isSelected) colors.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp),
                )
            }
        }
        Text(
            text = choice.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Wide enough for two short words under a 56dp chip without the names colliding. */
private val SYMBOL_CELL = 84.dp
