package app.larova.screenshot

import app.larova.feature.card.edit.EditCardCallbacks

/**
 * The editor's handlers, all of them empty.
 *
 * `EditCardCallbacks` has one field per thing a parent can do on that screen, which is a lot of
 * fields — and every one of them is required, so a screenshot of the editor cannot be taken
 * without them. They are empty on purpose: a golden may never depend on something having happened,
 * only on the state the screen was handed.
 *
 * It lives in its own file so the next person to add a handler edits one boring function rather
 * than hunting for it in the middle of a test.
 */
internal fun noOpEditCallbacks() = EditCardCallbacks(
    onTypeChange = {},
    onTitleChange = {},
    onSubtitleChange = {},
    onColorChange = {},
    onSymbolChange = {},
    onChooseSymbol = {},
    onStepChange = { _, _ -> },
    onAddStep = {},
    onRemoveStep = {},
    onPickPicture = {},
    onRemovePicture = {},
    onItemChange = { _, _ -> },
    onAddItem = {},
    onRemoveItem = {},
    onColumnChange = { _, _ -> },
    onAddColumn = {},
    onRemoveColumn = {},
    onCellChange = { _, _, _ -> },
    onAddRow = {},
    onRemoveRow = {},
    onResetDailyChange = {},
    onNoteChange = {},
    onContactChange = { _, _ -> },
    onAddContact = {},
    onRemoveContact = {},
    onWebUrlChange = {},
    onWebLabelChange = {},
    onLinkCaptionChange = {},
    onChooseApp = {},
    onAppQueryChange = {},
    onAppPicked = {},
    onDismissAppPicker = {},
    onAppLabelChange = {},
    onChooseMedia = {},
    onMediaCaptionChange = {},
    onRecord = {},
    onStopRecording = {},
    onSave = {},
    onDelete = {},
)
