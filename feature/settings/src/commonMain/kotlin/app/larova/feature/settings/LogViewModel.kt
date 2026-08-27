package app.larova.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.larova.core.domain.usecase.ClearLog
import app.larova.core.domain.usecase.LogLine
import app.larova.core.domain.usecase.ObserveLog
import app.larova.core.domain.usecase.RecordEvent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The log screen reads the stored log, so a line a caregiver writes appears because it was written
 * and not because the screen assumed it would be. If a write fails the row does not appear, which
 * is the honest outcome.
 */
class LogViewModel(
    observeLog: ObserveLog,
    private val recordEvent: RecordEvent,
    private val clearLog: ClearLog,
) : ViewModel() {

    val lines: StateFlow<List<LogLine>> = observeLog()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList(),
        )

    private val _note = MutableStateFlow("")
    val note: StateFlow<String> = _note.asStateFlow()

    fun onNoteChange(text: String) {
        _note.value = text
    }

    /** Cleared only once the line is in. A note that failed to save must not vanish from the field. */
    fun onAddNote() {
        val text = _note.value
        viewModelScope.launch {
            if (recordEvent.note(text)) _note.value = ""
        }
    }

    fun onClear() {
        viewModelScope.launch { clearLog() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
