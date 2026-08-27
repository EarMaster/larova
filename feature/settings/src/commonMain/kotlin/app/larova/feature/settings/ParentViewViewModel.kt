package app.larova.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.larova.core.domain.usecase.HasPin
import app.larova.core.domain.usecase.MAX_PIN_LENGTH
import app.larova.core.domain.usecase.SetPin
import app.larova.core.domain.usecase.UnlockWithBiometrics
import app.larova.core.domain.usecase.UnlockWithPin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class UnlockUiState(
    val pin: String = "",
    val wrongPin: Boolean = false,
    val unlocked: Boolean = false,
    /** No PIN has ever been chosen, so the first way in is to choose one. */
    val needsPinSetup: Boolean = false,
)

class UnlockViewModel(
    hasPin: HasPin,
    private val unlockWithPin: UnlockWithPin,
    private val unlockWithBiometrics: UnlockWithBiometrics,
) : ViewModel() {

    private val _state = MutableStateFlow(UnlockUiState())
    val state: StateFlow<UnlockUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            if (!hasPin()) _state.update { it.copy(needsPinSetup = true) }
        }
    }

    fun onPinChange(value: String) = _state.update {
        it.copy(pin = value.take(MAX_PIN_LENGTH), wrongPin = false)
    }

    fun onUnlock() {
        val pin = _state.value.pin
        viewModelScope.launch {
            if (unlockWithPin(pin)) {
                // The typed PIN is dropped as soon as it has been used. Nothing keeps it, including
                // this screen's state, which a screenshot or a saved instance state could outlive.
                _state.value = UnlockUiState(unlocked = true)
            } else {
                _state.update { it.copy(pin = "", wrongPin = true) }
            }
        }
    }

    /** Called once the platform prompt has succeeded — never to ask for it. */
    fun onBiometricsAccepted() {
        viewModelScope.launch {
            if (unlockWithBiometrics()) _state.value = UnlockUiState(unlocked = true)
        }
    }
}

data class PinSetupUiState(
    val pin: String = "",
    val repeated: String = "",
    val error: PinError? = null,
    val saved: Boolean = false,
)

class PinSetupViewModel(private val setPin: SetPin) : ViewModel() {

    private val _state = MutableStateFlow(PinSetupUiState())
    val state: StateFlow<PinSetupUiState> = _state.asStateFlow()

    fun onPinChange(value: String) = _state.update {
        it.copy(pin = value.take(MAX_PIN_LENGTH), error = null)
    }

    fun onRepeatChange(value: String) = _state.update {
        it.copy(repeated = value.take(MAX_PIN_LENGTH), error = null)
    }

    fun onSave() {
        val current = _state.value
        if (current.pin != current.repeated) {
            _state.update { it.copy(error = PinError.MISMATCH) }
            return
        }

        viewModelScope.launch {
            when (setPin(current.pin)) {
                SetPin.Result.Set -> _state.value = PinSetupUiState(saved = true)
                SetPin.Result.TooShort -> _state.update { it.copy(error = PinError.TOO_SHORT) }
                SetPin.Result.NotDigits -> _state.update { it.copy(error = PinError.NOT_DIGITS) }
            }
        }
    }
}
