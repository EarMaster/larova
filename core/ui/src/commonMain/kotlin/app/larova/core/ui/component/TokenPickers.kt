package app.larova.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.larova.core.ui.icon.SymbolGroup
import app.larova.core.ui.icon.TileSymbol
import app.larova.core.ui.icon.image
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.cd_colour_selected
import app.larova.core.ui.resources.colour_clay
import app.larova.core.ui.resources.colour_lilac
import app.larova.core.ui.resources.colour_moss
import app.larova.core.ui.resources.colour_rose
import app.larova.core.ui.resources.colour_sage
import app.larova.core.ui.resources.colour_sand
import app.larova.core.ui.resources.colour_sky
import app.larova.core.ui.resources.colour_stone
import app.larova.core.ui.resources.edit_symbol_search
import app.larova.core.ui.resources.home_search_empty
import app.larova.core.ui.theme.Dimens
import app.larova.core.ui.theme.LocalAppMode
import app.larova.core.ui.theme.TileColor
import app.larova.core.ui.theme.TileColors
import app.larova.core.ui.theme.resolve
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * The eight colours, shown as they will look.
 *
 * Each swatch is painted with the token resolved for the *current* appearance mode, so what the
 * parent picks in dark mode is what they get in dark mode. The label is localized and comes from
 * `colour_<key>`; the key itself never reaches a user.
 *
 * Selection is marked by a ring and by the symbol inside, never by colour alone — under
 * red-green colour blindness `moss` and `clay` converge, and "which one is selected" has to
 * survive that.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorTokenPicker(
    selectedToken: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val mode = LocalAppMode.current
    val selected = TileColor.fromKey(selectedToken)

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for (token in TileColor.entries) {
            val colors = token.resolve(mode)
            val isSelected = token == selected
            val label = stringResource(token.labelResource)

            Surface(
                modifier = Modifier
                    .size(Dimens.MinTouchTarget)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(token.key) },
                    ),
                shape = RoundedCornerShape(Dimens.ChipRadius),
                color = colors.surface,
                border = if (isSelected) {
                    androidx.compose.foundation.BorderStroke(3.dp, colors.accent)
                } else {
                    null
                },
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isSelected) {
                        Icon(
                            imageVector = TileSymbol.STAR.image,
                            contentDescription = stringResource(Res.string.cd_colour_selected, label),
                            tint = colors.accent,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
    }
}

/**
 * The symbols, grouped and searchable.
 *
 * Sixty-eight in one flat grid is a wall somebody scrolls past rather than reads, so they sit on
 * eight shelves named after what a family's tiles are actually about. The search box is what makes
 * the long tail reachable: somebody who wants a bus types "bus" rather than hunting eight groups
 * for it.
 *
 * No nested scrolling. The whole picker is a stack of `FlowRow`s inside the editor's own scroll —
 * a scrollable box inside a scrollable form is the thing that eats a drag and leaves a parent
 * stuck halfway down a screen.
 *
 * Names are English and are not translated; see [TileSymbol.label] for why. They are also what a
 * screen reader announces, which is new: ten unnamed symbols were a shrug, sixty-eight would be a
 * picker nobody could use by ear.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymbolPicker(
    selectedKey: String,
    colorToken: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TileColor.fromKey(colorToken).resolve(LocalAppMode.current)
    val selected = TileSymbol.fromKey(selectedKey)
    var query by rememberSaveable { mutableStateOf("") }
    val trimmed = query.trim()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(Res.string.edit_symbol_search)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        if (trimmed.isEmpty()) {
            for (group in SymbolGroup.entries) {
                Text(
                    text = group.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SymbolGrid(
                    symbols = TileSymbol.entries.filter { it.group == group },
                    selected = selected,
                    colors = colors,
                    onSelect = onSelect,
                )
            }
        } else {
            // Searching flattens the shelves. Somebody who typed "bus" is not browsing, and
            // headings above a row of one are furniture in the way of the answer.
            val matches = TileSymbol.entries.filter { it.label.contains(trimmed, ignoreCase = true) }
            if (matches.isEmpty()) {
                Text(
                    text = stringResource(Res.string.home_search_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                SymbolGrid(
                    symbols = matches,
                    selected = selected,
                    colors = colors,
                    onSelect = onSelect,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SymbolGrid(
    symbols: List<TileSymbol>,
    selected: TileSymbol,
    colors: TileColors,
    onSelect: (String) -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for (symbol in symbols) {
            val isSelected = symbol == selected
            Surface(
                modifier = Modifier
                    .size(Dimens.MinTouchTarget)
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(symbol.key) },
                    ),
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
                        imageVector = symbol.image,
                        // The name, so the picker can be used by ear. The role carries the
                        // selection state, so this says what the drawing is and nothing else.
                        contentDescription = symbol.label,
                        tint = if (isSelected) colors.accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

/** Picker labels are localized; the token key is an internal identifier and stays out of sight. */
private val TileColor.labelResource: StringResource
    get() = when (this) {
        TileColor.SAND -> Res.string.colour_sand
        TileColor.CLAY -> Res.string.colour_clay
        TileColor.ROSE -> Res.string.colour_rose
        TileColor.LILAC -> Res.string.colour_lilac
        TileColor.SKY -> Res.string.colour_sky
        TileColor.SAGE -> Res.string.colour_sage
        TileColor.MOSS -> Res.string.colour_moss
        TileColor.STONE -> Res.string.colour_stone
    }
