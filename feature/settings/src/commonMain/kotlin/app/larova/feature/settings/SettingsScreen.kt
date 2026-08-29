package app.larova.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.larova.core.domain.model.AppearanceSetting
import app.larova.core.ui.component.ActionCard
import app.larova.core.ui.component.LarovaScaffold
import app.larova.core.ui.icon.Transfer
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.settings_appearance
import app.larova.core.ui.resources.settings_appearance_dark
import app.larova.core.ui.resources.settings_appearance_dark_hint
import app.larova.core.ui.resources.settings_appearance_light
import app.larova.core.ui.resources.settings_appearance_light_hint
import app.larova.core.ui.resources.settings_appearance_night
import app.larova.core.ui.resources.settings_appearance_night_hint
import app.larova.core.ui.resources.settings_appearance_system
import app.larova.core.ui.resources.settings_appearance_system_hint
import app.larova.core.ui.resources.settings_title
import app.larova.core.ui.resources.settings_transfer_hint
import app.larova.core.ui.resources.transfer_title
import app.larova.core.ui.resources.view_leave
import app.larova.core.ui.resources.view_locked_note
import app.larova.core.ui.resources.view_parent_active
import app.larova.core.ui.resources.view_pin_change
import app.larova.core.ui.resources.view_unlock_title
import app.larova.core.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Settings.
 *
 * Appearance, the way in and out of parent view, and — once you are in it — the parent-view work
 * that is not editing a tile. Appearance comes first among the preferences because night mode is
 * not a preference in the usual sense: it exists for the leading use case, reading a guide aloud
 * in a darkened bedroom, and it is the one setting a caregiver might reasonably reach for
 * themselves.
 *
 * No help bar here. This is a screen somebody opened on purpose to change something, not one they
 * are on while a child is upset, and the red bar means "now" everywhere else in the product.
 */
@Composable
fun SettingsScreen(
    appearance: AppearanceSetting,
    onAppearanceChange: (AppearanceSetting) -> Unit,
    isParentView: Boolean,
    onUnlock: () -> Unit,
    onLock: () -> Unit,
    onChangePin: () -> Unit,
    onOpenTransfer: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LarovaScaffold(
        title = stringResource(Res.string.settings_title),
        onHelp = null,
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
            // Above appearance, because it is the reason most people open this screen: they
            // came here to change something and found they could not.
            ViewModeSection(
                isParentView = isParentView,
                onUnlock = onUnlock,
                onLock = onLock,
                onChangePin = onChangePin,
            )

            // Backup used to live behind the start screen's overflow menu, which is the last place
            // somebody looks for it — a menu is where things go when nobody has decided where they
            // belong. It belongs here: it is parent-view work, and this is the screen parent view
            // is turned on from.
            if (isParentView) {
                ActionCard(
                    icon = Transfer,
                    title = stringResource(Res.string.transfer_title),
                    description = stringResource(Res.string.settings_transfer_hint),
                    onClick = onOpenTransfer,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            }

            Text(
                text = stringResource(Res.string.settings_appearance),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(vertical = 12.dp),
            )

            AppearanceOptions(
                selected = appearance,
                onSelect = onAppearanceChange,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * The four appearances, as one thing.
 *
 * Loose rows on the page read as four separate settings that happen to be near each other, and a
 * caregiver who reads them that way looks for the "off". One surface with a line between each row
 * says what a radio group means — pick exactly one of these — before anybody has read a word of
 * it. `selectableGroup` says the same to a screen reader, which otherwise announces four unrelated
 * radio buttons rather than "1 of 4".
 */
@Composable
private fun AppearanceOptions(
    selected: AppearanceSetting,
    onSelect: (AppearanceSetting) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(Dimens.TileRadius),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.selectableGroup()) {
            AppearanceSetting.entries.forEachIndexed { index, option ->
                if (index > 0) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
                AppearanceOption(
                    label = stringResource(option.label),
                    hint = stringResource(option.hint),
                    selected = option == selected,
                    onSelect = { onSelect(option) },
                )
            }
        }
    }
}

/**
 * A row, not a switch. Four states cannot be a toggle, and a 56dp selectable row is easier to hit
 * than a radio button on its own — the whole row is the target, which is what
 * `Modifier.selectable` on the row rather than on the control gives.
 *
 * The padding is inside `selectable` rather than outside it, so the target is the full width of
 * the card and not a strip down the middle of it.
 */
@Composable
private fun AppearanceOption(
    label: String,
    hint: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // null: the row carries the semantics, so a screen reader announces one control rather
        // than two — and the hint is then read as part of that control's own label, which is what
        // makes it an explanation of this option rather than a stray sentence.
        RadioButton(selected = selected, onClick = null)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private val AppearanceSetting.label: StringResource
    get() = when (this) {
        AppearanceSetting.SYSTEM -> Res.string.settings_appearance_system
        AppearanceSetting.LIGHT -> Res.string.settings_appearance_light
        AppearanceSetting.DARK -> Res.string.settings_appearance_dark
        AppearanceSetting.NIGHT -> Res.string.settings_appearance_night
    }

/**
 * Every option has one, and they are deliberately parallel: three say what the screen will look
 * like, and night says what it is *for*, because that is the one whose name does not carry it.
 * Describing only night left the reasonable question of what was wrong with the other three.
 */
private val AppearanceSetting.hint: StringResource
    get() = when (this) {
        AppearanceSetting.SYSTEM -> Res.string.settings_appearance_system_hint
        AppearanceSetting.LIGHT -> Res.string.settings_appearance_light_hint
        AppearanceSetting.DARK -> Res.string.settings_appearance_dark_hint
        AppearanceSetting.NIGHT -> Res.string.settings_appearance_night_hint
    }

/**
 * Which view the app is in, and the way out of it.
 *
 * Stated in words rather than shown as a switch. A toggle labelled "parent view" invites a
 * caregiver to try it, and the honest answer to that tap — a PIN prompt — is better reached from a
 * button that says what it will do.
 */
@Composable
private fun ViewModeSection(
    isParentView: Boolean,
    onUnlock: () -> Unit,
    onLock: () -> Unit,
    onChangePin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = stringResource(
                if (isParentView) Res.string.view_parent_active else Res.string.view_locked_note,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (isParentView) {
            OutlinedButton(
                onClick = onLock,
                modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(Res.string.view_leave))
            }
            OutlinedButton(
                onClick = onChangePin,
                modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(Res.string.view_pin_change))
            }
        } else {
            Button(
                onClick = onUnlock,
                modifier = Modifier.fillMaxWidth().heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(Res.string.view_unlock_title))
            }
        }
    }
}
