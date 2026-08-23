package app.larova.core.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
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
import app.larova.core.ui.theme.Dimens
import app.larova.core.ui.theme.LocalAppMode
import app.larova.core.ui.theme.TileColor
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

/** The symbols, drawn at the size they appear on a tile. */
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

    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        for (symbol in TileSymbol.entries) {
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
                        // The picker is a row of choices; a screen reader reads the selection
                        // state from the role, and ten symbol names would be ten guesses at what
                        // to call a drawing.
                        contentDescription = null,
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
