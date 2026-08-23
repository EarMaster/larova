package app.larova.feature.settings

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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import app.larova.core.ui.component.LarovaScaffold
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.edit_cancel
import app.larova.core.ui.resources.edit_save
import app.larova.core.ui.resources.view_parent_active
import app.larova.core.ui.resources.view_pin_create_hint
import app.larova.core.ui.resources.view_pin_create_title
import app.larova.core.ui.resources.view_pin_digits_only
import app.larova.core.ui.resources.view_pin_mismatch
import app.larova.core.ui.resources.view_pin_repeat
import app.larova.core.ui.resources.view_pin_too_short
import app.larova.core.ui.resources.view_pin_wrong
import app.larova.core.ui.resources.view_unlock
import app.larova.core.ui.resources.view_unlock_biometric
import app.larova.core.ui.resources.view_unlock_pin
import app.larova.core.ui.resources.view_unlock_title
import app.larova.core.ui.theme.Dimens
import org.jetbrains.compose.resources.stringResource

/**
 * The way into parent view.
 *
 * A plain PIN field rather than a custom keypad. The system keyboard is the one thing on the phone
 * this person already knows how to use, it comes with their own text size and their own haptics,
 * and a hand-drawn keypad would be one more thing to get wrong for someone with poor eyesight.
 *
 * The biometric route is offered first when the device has it, because it is faster and because it
 * means the PIN is typed rarely enough to stay unguessable by whoever is watching.
 */
@Composable
fun UnlockScreen(
    pin: String,
    onPinChange: (String) -> Unit,
    onUnlock: () -> Unit,
    onUseBiometrics: (() -> Unit)?,
    wrongPin: Boolean,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LarovaScaffold(
        title = stringResource(Res.string.view_unlock_title),
        onHelp = onHelp,
        onBack = onBack,
        modifier = modifier,
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenMargin, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PinField(
                value = pin,
                onValueChange = onPinChange,
                label = stringResource(Res.string.view_unlock_pin),
                errorText = if (wrongPin) stringResource(Res.string.view_pin_wrong) else null,
                imeAction = ImeAction.Go,
                onSubmit = onUnlock,
            )

            Button(
                onClick = onUnlock,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(Res.string.view_unlock))
            }

            if (onUseBiometrics != null) {
                OutlinedButton(
                    onClick = onUseBiometrics,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Dimens.MinTouchTarget),
                ) {
                    Text(stringResource(Res.string.view_unlock_biometric))
                }
            }

            Text(
                // Said before the unlock rather than after: five minutes is short enough to
                // surprise someone who was not told.
                text = stringResource(Res.string.view_parent_active),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Choosing or replacing the PIN.
 *
 * Typed twice, because it cannot be recovered — there is no server to ask and no email to send. A
 * mistyped PIN that was only entered once would lock the parents out of their own content, and the
 * only remedy would be reinstalling and losing everything.
 *
 * The hint says what the PIN does and, more importantly, what it does not: it stops the tiles being
 * changed, it does not hide them. Someone who believed otherwise might write something in a tile
 * they would not want a caregiver to read.
 */
@Composable
fun PinSetupScreen(
    pin: String,
    repeated: String,
    onPinChange: (String) -> Unit,
    onRepeatChange: (String) -> Unit,
    onSave: () -> Unit,
    error: PinError?,
    onBack: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LarovaScaffold(
        title = stringResource(Res.string.view_pin_create_title),
        onHelp = onHelp,
        onBack = onBack,
        modifier = modifier,
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenMargin, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = stringResource(Res.string.view_pin_create_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            PinField(
                value = pin,
                onValueChange = onPinChange,
                label = stringResource(Res.string.view_unlock_pin),
                errorText = when (error) {
                    PinError.TOO_SHORT -> stringResource(Res.string.view_pin_too_short)
                    PinError.NOT_DIGITS -> stringResource(Res.string.view_pin_digits_only)
                    else -> null
                },
                imeAction = ImeAction.Next,
                onSubmit = {},
            )

            PinField(
                value = repeated,
                onValueChange = onRepeatChange,
                label = stringResource(Res.string.view_pin_repeat),
                errorText = if (error == PinError.MISMATCH) {
                    stringResource(Res.string.view_pin_mismatch)
                } else {
                    null
                },
                imeAction = ImeAction.Done,
                onSubmit = onSave,
            )

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(Res.string.edit_save))
            }
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Dimens.MinTouchTarget),
            ) {
                Text(stringResource(Res.string.edit_cancel))
            }
        }
    }
}

/** What can be wrong with a PIN as it is being set. */
enum class PinError { TOO_SHORT, NOT_DIGITS, MISMATCH }

@Composable
private fun PinField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    errorText: String?,
    imeAction: ImeAction,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        // Filtered here as well as checked on save: a keyboard can be swapped for one that offers
        // letters, and silently dropping them beats an error nobody expected.
        onValueChange = { text -> onValueChange(text.filter { it.isDigit() }) },
        label = { Text(label) },
        singleLine = true,
        isError = errorText != null,
        supportingText = errorText?.let { { Text(it) } },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = KeyboardType.NumberPassword,
            imeAction = imeAction,
        ),
        keyboardActions = androidx.compose.foundation.text.KeyboardActions(
            onGo = { onSubmit() },
            onDone = { onSubmit() },
        ),
        modifier = modifier.fillMaxWidth(),
    )
}
