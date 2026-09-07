package app.larova.feature.card.edit

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import app.larova.core.ui.icon.Translate
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.cd_language
import app.larova.core.ui.resources.cd_translate
import app.larova.core.ui.resources.edit_add_language
import app.larova.core.ui.resources.edit_written_in
import app.larova.core.ui.resources.edit_written_in_unset
import app.larova.core.ui.resources.translate_original
import org.jetbrains.compose.resources.stringResource

/**
 * The language menu in the editor's bar: which language is being written, and what to hand over.
 *
 * The same globe in the same corner as the tile screen's, because it answers the same shape of
 * question one step earlier — that one chooses which language to *read* a tile in, this one which
 * language to *write* it in. A parent who has found one has found the other.
 *
 * Absent on a tile that has not been saved yet. A translation starts as a copy of the original, and
 * there is nothing to copy until there is something saved to copy from.
 *
 * The hand-off is here as well as on the tile screen, and it hands over **what is on screen**: the
 * original when the original is being edited, which is how somebody gets a first draft to paste
 * back, and the translation when that is, which is how they check what they already wrote.
 */
@Composable
fun EditLanguageMenu(state: EditUiState, callbacks: EditCardCallbacks) {
    if (state.isNew) return
    var expanded by remember { mutableStateOf(false) }

    IconButton(onClick = { expanded = true }) {
        Icon(imageVector = Translate, contentDescription = stringResource(Res.string.cd_language))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        // The tile's own text first. It is named when somebody has said what language it is in, and
        // "as written" when nobody has — never guessed, for the reason `Card.locale` gives.
        EditLanguageItem(
            label = state.localeName ?: stringResource(Res.string.translate_original),
            selected = state.editingLanguage == null,
            onClick = {
                expanded = false
                callbacks.onEditLanguage(null)
            },
        )
        for (language in state.languages) {
            EditLanguageItem(
                label = language.name,
                selected = state.editingLanguage == language.tag,
                onClick = {
                    expanded = false
                    callbacks.onEditLanguage(language.tag)
                },
            )
        }

        HorizontalDivider()

        // Below the line: these two do something to the tile rather than choosing which part of it
        // is on screen.
        DropdownMenuItem(
            text = {
                Text(
                    state.localeName
                        ?.let { stringResource(Res.string.edit_written_in, it) }
                        ?: stringResource(Res.string.edit_written_in_unset),
                )
            },
            onClick = {
                expanded = false
                callbacks.onPickLanguage(true)
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(Res.string.edit_add_language)) },
            onClick = {
                expanded = false
                callbacks.onPickLanguage(false)
            },
        )

        // `canTranslate` as well as the callback: the callback is wired in every real build, and
        // what decides whether the row is worth offering is whether anything on *this* phone will
        // take the words. Without it the row would be there and do nothing, silently.
        val translate = callbacks.onTranslate
        if (translate != null && state.canTranslate && state.handOffText.isNotBlank()) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.cd_translate)) },
                onClick = {
                    expanded = false
                    translate(state.handOffText)
                },
            )
        }
    }
}

/** One language to write the tile in. A radio, for the reason the tile screen's rows carry one. */
@Composable
private fun EditLanguageItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = { RadioButton(selected = selected, onClick = null) },
        onClick = onClick,
        modifier = Modifier.semantics { this.selected = selected },
    )
}
