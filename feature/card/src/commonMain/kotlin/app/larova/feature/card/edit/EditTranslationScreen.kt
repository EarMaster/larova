package app.larova.feature.card.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.larova.core.ui.component.LarovaScaffold
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.cd_translate
import app.larova.core.ui.resources.edit_cancel
import app.larova.core.ui.resources.edit_language_remove
import app.larova.core.ui.resources.edit_save
import app.larova.core.ui.resources.edit_subtitle
import app.larova.core.ui.resources.edit_title
import app.larova.core.ui.resources.edit_title_required
import app.larova.core.ui.resources.edit_translation_field
import app.larova.core.ui.resources.edit_translation_hint
import app.larova.core.ui.resources.edit_translation_title
import app.larova.core.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/**
 * One tile in one other language.
 *
 * Every field here is words and nothing else. There is no colour, no symbol, no picture, no phone
 * number and no address — none of them are translated, and a form that offered them would let a
 * translation quietly become a different tile. What is left is the title, the second line, and one
 * box per phrase, in the order they appear on the tile.
 *
 * The Translate button hands these same words to a translation app. What comes back is pasted in by
 * the person, box by box: Larova never reads the clipboard and never splits an answer up for you,
 * which is the honest cost of not interpreting what somebody wrote.
 */
@Composable
fun EditTranslationScreen(
    state: EditTranslationUiState,
    onTitleChange: (String) -> Unit,
    onSubtitleChange: (String) -> Unit,
    onFieldChange: (Int, String) -> Unit,
    onTranslate: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LarovaScaffold(
        title = stringResource(Res.string.edit_translation_title, state.languageName),
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.edit_translation_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (state.canTranslate && state.handOffText.isNotBlank()) {
                OutlinedButton(
                    onClick = { onTranslate(state.handOffText) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.MinTouchTarget),
                ) {
                    Text(stringResource(Res.string.cd_translate))
                }
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(Res.string.edit_title)) },
                isError = state.titleMissing,
                supportingText = if (state.titleMissing) {
                    { Text(stringResource(Res.string.edit_title_required)) }
                } else {
                    null
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.subtitle,
                onValueChange = onSubtitleChange,
                label = { Text(stringResource(Res.string.edit_subtitle)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // One box per phrase, numbered only for the label a screen reader announces — the
            // number is never part of the text, because the text is what gets stored.
            state.fields.forEachIndexed { index, value ->
                OutlinedTextField(
                    value = value,
                    onValueChange = { onFieldChange(index, it) },
                    label = { Text(stringResource(Res.string.edit_translation_field, index + 1)) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(Res.string.edit_save))
            }

            TextButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(Res.string.edit_cancel))
            }

            if (state.exists) {
                TextButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.MinTouchTarget)
                        .padding(bottom = 24.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.edit_language_remove),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
