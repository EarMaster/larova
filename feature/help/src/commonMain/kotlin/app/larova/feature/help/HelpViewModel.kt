package app.larova.feature.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.larova.core.domain.usecase.HelpContact
import app.larova.core.domain.model.parseUuidOrNull
import app.larova.core.domain.usecase.ObserveHelpContacts
import app.larova.core.domain.usecase.RecordEvent
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * The contacts, kept up to date while the screen is open.
 *
 * No loading flag. An empty list and a list that has not arrived look the same here, and unlike the
 * start screen there is nothing useful to say about the difference — the message underneath is the
 * same either way: these are the numbers, and Larova will not dial them for you.
 */
class HelpViewModel(
    observeHelpContacts: ObserveHelpContacts,
    private val recordEvent: RecordEvent,
) : ViewModel() {

    val contacts: StateFlow<List<HelpContact>> = observeHelpContacts()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = emptyList(),
        )

    /**
     * A call from the help bar is the one the parents will most want to see in the log, and it is
     * the one nobody will be in a state to write down afterwards.
     */
    fun onCallPrepared(cardId: String) {
        val id = parseUuidOrNull(cardId) ?: return
        viewModelScope.launch { recordEvent.callPrepared(id) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
