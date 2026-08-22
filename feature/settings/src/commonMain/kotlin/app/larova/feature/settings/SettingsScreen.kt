package app.larova.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.larova.core.domain.model.AppearanceSetting
import app.larova.core.ui.component.LarovaScaffold
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.settings_appearance
import app.larova.core.ui.resources.settings_appearance_dark
import app.larova.core.ui.resources.settings_appearance_light
import app.larova.core.ui.resources.settings_appearance_night
import app.larova.core.ui.resources.settings_appearance_night_hint
import app.larova.core.ui.resources.settings_appearance_system
import app.larova.core.ui.resources.settings_title
import app.larova.core.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Settings.
 *
 * Only appearance for now; language, the PIN and the log arrive with the parent view. Appearance
 * comes first because night mode is not a preference in the usual sense — it exists for the
 * leading use case, reading a guide aloud in a darkened bedroom, and it is the one setting a
 * caregiver might reasonably reach for themselves.
 */
@Composable
fun SettingsScreen(
    appearance: AppearanceSetting,
    onAppearanceChange: (AppearanceSetting) -> Unit,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LarovaScaffold(
        title = stringResource(Res.string.settings_title),
        onHelp = onHelp,
        onBack = onBack,
        modifier = modifier,
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenMargin),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = stringResource(Res.string.settings_appearance),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            for (option in AppearanceSetting.entries) {
                AppearanceOption(
                    label = stringResource(option.label),
                    selected = option == appearance,
                    onSelect = { onAppearanceChange(option) },
                )
            }

            Text(
                text = stringResource(Res.string.settings_appearance_night_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
        }
    }
}

/**
 * A row, not a switch. Four states cannot be a toggle, and a 56dp selectable row is easier to hit
 * than a radio button on its own — the whole row is the target, which is what
 * `Modifier.selectable` on the row rather than on the control gives.
 */
@Composable
private fun AppearanceOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // null: the row above carries the semantics, so a screen reader announces one control
        // rather than two.
        RadioButton(selected = selected, onClick = null)
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
    }
}

private val AppearanceSetting.label: StringResource
    get() = when (this) {
        AppearanceSetting.SYSTEM -> Res.string.settings_appearance_system
        AppearanceSetting.LIGHT -> Res.string.settings_appearance_light
        AppearanceSetting.DARK -> Res.string.settings_appearance_dark
        AppearanceSetting.NIGHT -> Res.string.settings_appearance_night
    }
