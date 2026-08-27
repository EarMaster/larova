package app.larova.feature.card.edit

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import app.larova.core.domain.model.CardType
import app.larova.core.ui.component.ColorTokenPicker
import app.larova.core.ui.component.LarovaScaffold
import app.larova.core.ui.component.SymbolPicker
import app.larova.core.ui.icon.BackArrow
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.cd_remove_line
import app.larova.core.ui.resources.cd_step_picture
import app.larova.core.ui.resources.edit_add_item
import app.larova.core.ui.resources.edit_add_picture
import app.larova.core.ui.resources.edit_add_step
import app.larova.core.ui.resources.edit_change_picture
import app.larova.core.ui.resources.edit_call_in_help
import app.larova.core.ui.resources.edit_call_name
import app.larova.core.ui.resources.edit_call_number
import app.larova.core.ui.resources.edit_call_relation
import app.larova.core.ui.resources.edit_cancel
import app.larova.core.ui.resources.edit_choose_type
import app.larova.core.ui.resources.edit_colour
import app.larova.core.ui.resources.edit_delete
import app.larova.core.ui.resources.edit_delete_question
import app.larova.core.ui.resources.edit_edit_tile
import app.larova.core.ui.resources.edit_item_number
import app.larova.core.ui.resources.edit_items
import app.larova.core.ui.resources.edit_new_tile
import app.larova.core.ui.resources.edit_note_text
import app.larova.core.ui.resources.edit_picture_failed
import app.larova.core.ui.resources.edit_remove
import app.larova.core.ui.resources.edit_remove_picture
import app.larova.core.ui.resources.edit_reset_daily
import app.larova.core.ui.resources.edit_save
import app.larova.core.ui.resources.edit_step_number
import app.larova.core.ui.resources.edit_steps
import app.larova.core.ui.resources.edit_subtitle
import app.larova.core.ui.resources.edit_symbol
import app.larova.core.ui.resources.edit_title
import app.larova.core.ui.resources.edit_title_required
import app.larova.core.ui.resources.edit_web_address
import app.larova.core.ui.resources.edit_web_address_invalid
import app.larova.core.ui.resources.edit_web_label
import app.larova.core.ui.resources.tile_call
import app.larova.core.ui.resources.tile_checklist
import app.larova.core.ui.resources.tile_guide
import app.larova.core.ui.resources.tile_link
import app.larova.core.ui.resources.tile_note
import app.larova.core.ui.theme.Dimens
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/**
 * Making a tile.
 *
 * One scrolling form rather than a wizard. The parents use this rarely but intensively, usually
 * with the child asleep and the phone in one hand, and a five-step wizard for something that fits
 * on one screen turns two minutes of work into ten.
 *
 * The type can only be chosen while creating. Changing it afterwards would mean deciding what to
 * do with content that no longer has a home, and quietly discarding what somebody typed is not a
 * decision this app gets to make.
 */
@Composable
fun EditCardScreen(
    state: EditUiState,
    callbacks: EditCardCallbacks,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmingDelete by remember { mutableStateOf(false) }

    LarovaScaffold(
        title = stringResource(if (state.isNew) Res.string.edit_new_tile else Res.string.edit_edit_tile),
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
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (state.isNew) {
                Section(title = stringResource(Res.string.edit_choose_type)) {
                    TypePicker(selected = state.type, onSelect = callbacks.onTypeChange)
                }
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = callbacks.onTitleChange,
                label = { Text(stringResource(Res.string.edit_title)) },
                isError = state.titleMissing,
                supportingText = if (state.titleMissing) {
                    { Text(stringResource(Res.string.edit_title_required)) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.subtitle,
                onValueChange = callbacks.onSubtitleChange,
                label = { Text(stringResource(Res.string.edit_subtitle)) },
                modifier = Modifier.fillMaxWidth(),
            )

            TypeFields(state = state, callbacks = callbacks)

            Section(title = stringResource(Res.string.edit_colour)) {
                ColorTokenPicker(
                    selectedToken = state.colorToken,
                    onSelect = callbacks.onColorChange,
                )
            }

            Section(title = stringResource(Res.string.edit_symbol)) {
                SymbolPicker(
                    selectedKey = state.symbolKey,
                    colorToken = state.colorToken,
                    onSelect = callbacks.onSymbolChange,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget),
                ) {
                    Text(stringResource(Res.string.edit_cancel))
                }
                Button(
                    onClick = callbacks.onSave,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = Dimens.MinTouchTarget),
                ) {
                    Text(stringResource(Res.string.edit_save))
                }
            }

            if (!state.isNew) {
                TextButton(
                    onClick = { confirmingDelete = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.MinTouchTarget)
                        .padding(bottom = 16.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.edit_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    if (confirmingDelete) {
        // Asked rather than offered as an undo: there is no server and no bin, so a deleted tile
        // is gone, and the only honest moment to say so is before it happens.
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text(stringResource(Res.string.edit_delete)) },
            text = { Text(stringResource(Res.string.edit_delete_question)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmingDelete = false
                        callbacks.onDelete()
                    },
                ) {
                    Text(
                        text = stringResource(Res.string.edit_delete),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) {
                    Text(stringResource(Res.string.edit_cancel))
                }
            },
        )
    }
}

@Composable
private fun TypeFields(state: EditUiState, callbacks: EditCardCallbacks) {
    when (state.type) {
        CardType.GUIDE -> StepList(state = state, callbacks = callbacks)

        CardType.CHECKLIST -> {
            LineList(
                title = stringResource(Res.string.edit_items),
                addLabel = stringResource(Res.string.edit_add_item),
                lines = state.items.map { it.text },
                label = { index -> stringResource(Res.string.edit_item_number, index + 1) },
                onChange = callbacks.onItemChange,
                onAdd = callbacks.onAddItem,
                onRemove = callbacks.onRemoveItem,
            )
            SwitchRow(
                label = stringResource(Res.string.edit_reset_daily),
                checked = state.resetDaily,
                onCheckedChange = callbacks.onResetDailyChange,
            )
        }

        CardType.NOTE -> OutlinedTextField(
            value = state.noteText,
            onValueChange = callbacks.onNoteChange,
            label = { Text(stringResource(Res.string.edit_note_text)) },
            minLines = MIN_NOTE_LINES,
            modifier = Modifier.fillMaxWidth(),
        )

        CardType.PHONE -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.callName,
                onValueChange = callbacks.onCallNameChange,
                label = { Text(stringResource(Res.string.edit_call_name)) },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.callNumber,
                onValueChange = callbacks.onCallNumberChange,
                label = { Text(stringResource(Res.string.edit_call_number)) },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Phone,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.callRelation,
                onValueChange = callbacks.onCallRelationChange,
                label = { Text(stringResource(Res.string.edit_call_relation)) },
                modifier = Modifier.fillMaxWidth(),
            )
            SwitchRow(
                label = stringResource(Res.string.edit_call_in_help),
                checked = state.callInHelpSheet,
                onCheckedChange = callbacks.onCallInHelpSheetChange,
            )
        }

        CardType.WEB -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = state.webUrl,
                onValueChange = callbacks.onWebUrlChange,
                label = { Text(stringResource(Res.string.edit_web_address)) },
                isError = state.urlInvalid,
                supportingText = if (state.urlInvalid) {
                    { Text(stringResource(Res.string.edit_web_address_invalid)) }
                } else {
                    null
                },
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.webLabel,
                onValueChange = callbacks.onWebLabelChange,
                label = { Text(stringResource(Res.string.edit_web_label)) },
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // Not offered while creating and not reachable while editing, since nothing can create one.
        CardType.TABLE, CardType.VIDEO, CardType.AUDIO, CardType.APP_LINK, CardType.FOLDER -> Unit
    }
}

/**
 * A guide step: the line of text, and the picture that goes with it.
 *
 * Steps have the picture and checklist items do not, which is why this is not [LineList] with a
 * flag. A picture belongs to a step of a guide — showing which cupboard, which button, which of
 * the two blue boxes — and a list of things to tick off has nowhere to put one.
 */
@Composable
private fun StepList(state: EditUiState, callbacks: EditCardCallbacks) {
    Section(title = stringResource(Res.string.edit_steps)) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            state.steps.forEachIndexed { index, step ->
                val label = stringResource(Res.string.edit_step_number, index + 1)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        OutlinedTextField(
                            value = step.text,
                            onValueChange = { callbacks.onStepChange(index, it) },
                            label = { Text(label) },
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { callbacks.onRemoveStep(index) },
                            modifier = Modifier.heightIn(min = Dimens.MinTouchTarget),
                        ) {
                            Icon(
                                imageVector = BackArrow,
                                contentDescription = stringResource(Res.string.cd_remove_line, label),
                            )
                        }
                    }
                    StepPicture(
                        stepNumber = index + 1,
                        picture = step.mediaId?.let { state.pictures[it] },
                        hasPicture = step.mediaId != null,
                        onPick = { callbacks.onPickPicture(index) },
                        onRemove = { callbacks.onRemovePicture(index) },
                    )
                }
            }

            if (state.pictureFailed) {
                // Said rather than swallowed: the person watched the gallery close and has no other
                // way to tell that nothing arrived.
                Text(
                    text = stringResource(Res.string.edit_picture_failed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            OutlinedButton(
                onClick = callbacks.onAddStep,
                modifier = Modifier.heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(Res.string.edit_add_step))
            }
        }
    }
}

/**
 * The picture on one step, with the two things that can be done to it.
 *
 * Wrapping rather than one fixed row: at 200 % font scale a thumbnail and two buttons do not fit
 * across a phone, and the alternative to wrapping is a label cut in half.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepPicture(
    stepNumber: Int,
    picture: ImageBitmap?,
    hasPicture: Boolean,
    onPick: () -> Unit,
    onRemove: () -> Unit,
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (picture != null) {
            Image(
                bitmap = picture,
                contentDescription = stringResource(Res.string.cd_step_picture, stepNumber),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .size(72.dp)
                    .clip(MaterialTheme.shapes.extraSmall),
            )
        }
        TextButton(
            onClick = onPick,
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .heightIn(min = Dimens.MinTouchTarget),
        ) {
            Text(
                stringResource(
                    if (hasPicture) Res.string.edit_change_picture else Res.string.edit_add_picture,
                ),
            )
        }
        if (hasPicture) {
            TextButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(Res.string.edit_remove_picture))
            }
        }
    }
}

/**
 * Checklist items: a numbered line, a way to remove it, and a way to add another.
 */
@Composable
private fun LineList(
    title: String,
    addLabel: String,
    lines: List<String>,
    label: @Composable (Int) -> String,
    onChange: (Int, String) -> Unit,
    onAdd: () -> Unit,
    onRemove: (Int) -> Unit,
) {
    Section(title = title) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            lines.forEachIndexed { index, line ->
                val lineLabel = label(index)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    OutlinedTextField(
                        value = line,
                        onValueChange = { onChange(index, it) },
                        label = { Text(lineLabel) },
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(
                        onClick = { onRemove(index) },
                        modifier = Modifier.heightIn(min = Dimens.MinTouchTarget),
                    ) {
                        Icon(
                            // The arrow points back the way a line leaves the list; it mirrors in
                            // right-to-left layouts along with everything else directional.
                            imageVector = BackArrow,
                            contentDescription = stringResource(Res.string.cd_remove_line, lineLabel),
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = onAdd,
                modifier = Modifier.heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(addLabel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TypePicker(selected: CardType, onSelect: (CardType) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (type in EDITABLE_TYPES) {
            FilterChip(
                selected = type == selected,
                onClick = { onSelect(type) },
                label = { Text(stringResource(type.label)) },
                modifier = Modifier.heightIn(min = 44.dp),
            )
        }
    }
}

/** One switch and its label, with the whole row as the target. */
@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = Dimens.MinTouchTarget)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        // null: the row above carries the semantics, so a screen reader announces one switch.
        Switch(checked = checked, onCheckedChange = null)
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

/** The names the tile types already have in the interface — no second vocabulary for the editor. */
private val CardType.label: StringResource
    get() = when (this) {
        CardType.GUIDE -> Res.string.tile_guide
        CardType.CHECKLIST -> Res.string.tile_checklist
        CardType.NOTE -> Res.string.tile_note
        CardType.PHONE -> Res.string.tile_call
        else -> Res.string.tile_link
    }

private const val MIN_NOTE_LINES = 6

/**
 * The editor's callbacks in one place.
 *
 * A screen with two dozen lambdas in its signature is unreadable at the call site and impossible
 * to add to without touching every caller. Bundling them keeps the screen stateless, which is what
 * lets it be previewed and screenshot-tested without a ViewModel.
 */
data class EditCardCallbacks(
    val onTypeChange: (CardType) -> Unit,
    val onTitleChange: (String) -> Unit,
    val onSubtitleChange: (String) -> Unit,
    val onColorChange: (String) -> Unit,
    val onSymbolChange: (String) -> Unit,
    val onStepChange: (Int, String) -> Unit,
    val onAddStep: () -> Unit,
    val onRemoveStep: (Int) -> Unit,
    /** Opens the photo picker for one step. Both halves of it live outside the ViewModel. */
    val onPickPicture: (Int) -> Unit,
    val onRemovePicture: (Int) -> Unit,
    val onItemChange: (Int, String) -> Unit,
    val onAddItem: () -> Unit,
    val onRemoveItem: (Int) -> Unit,
    val onResetDailyChange: (Boolean) -> Unit,
    val onNoteChange: (String) -> Unit,
    val onCallNameChange: (String) -> Unit,
    val onCallNumberChange: (String) -> Unit,
    val onCallRelationChange: (String) -> Unit,
    val onCallInHelpSheetChange: (Boolean) -> Unit,
    val onWebUrlChange: (String) -> Unit,
    val onWebLabelChange: (String) -> Unit,
    val onSave: () -> Unit,
    val onDelete: () -> Unit,
)

/**
 * Built from a ViewModel in one place, so a screen cannot be wired to the wrong handler.
 *
 * [openPicturePicker] comes from outside: the photo picker is the platform's, and this module is
 * the same code on a phone that has no such thing. The ViewModel is told which step first, so that
 * whatever comes back has somewhere to land.
 */
fun EditCardViewModel.callbacks(openPicturePicker: () -> Unit = {}) = EditCardCallbacks(
    onTypeChange = ::onTypeChange,
    onTitleChange = ::onTitleChange,
    onSubtitleChange = ::onSubtitleChange,
    onColorChange = ::onColorChange,
    onSymbolChange = ::onSymbolChange,
    onStepChange = ::onStepChange,
    onAddStep = ::onAddStep,
    onRemoveStep = ::onRemoveStep,
    onPickPicture = { index ->
        onPickPictureFor(index)
        openPicturePicker()
    },
    onRemovePicture = ::onRemovePicture,
    onItemChange = ::onItemChange,
    onAddItem = ::onAddItem,
    onRemoveItem = ::onRemoveItem,
    onResetDailyChange = ::onResetDailyChange,
    onNoteChange = ::onNoteChange,
    onCallNameChange = ::onCallNameChange,
    onCallNumberChange = ::onCallNumberChange,
    onCallRelationChange = ::onCallRelationChange,
    onCallInHelpSheetChange = ::onCallInHelpSheetChange,
    onWebUrlChange = ::onWebUrlChange,
    onWebLabelChange = ::onWebLabelChange,
    onSave = ::onSave,
    onDelete = ::onDelete,
)
