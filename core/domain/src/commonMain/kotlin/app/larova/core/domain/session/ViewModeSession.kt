package app.larova.core.domain.session

import app.larova.core.domain.model.ViewMode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Which view the app is in, and the five minutes that end it.
 *
 * Held in memory on purpose. An unlock that survived a restart would mean a phone found on a table
 * is a phone in parent view, and the entire point of the split is that the content is safe from
 * being changed by whoever happens to be holding it.
 *
 * The timeout is a cancellable job rather than a polled clock: nothing runs while the app sits in
 * caregiver view, which is almost all of the time.
 */
class ViewModeSession(
    private val scope: CoroutineScope,
    private val timeout: Duration = DEFAULT_TIMEOUT,
) {

    private val _mode = MutableStateFlow(ViewMode.CAREGIVER)
    val mode: StateFlow<ViewMode> = _mode.asStateFlow()

    private var expiry: Job? = null

    /** Called once the PIN or a fingerprint has been accepted — never from the interface alone. */
    fun unlock() {
        _mode.value = ViewMode.PARENT
        restartExpiry()
    }

    /** Leaving parent view deliberately, or the app deciding the five minutes are up. */
    fun lock() {
        expiry?.cancel()
        expiry = null
        _mode.value = ViewMode.CAREGIVER
    }

    /**
     * Any interaction at all. Extends parent view rather than shortening it, and does nothing in
     * caregiver view — touching the screen must never be a way in.
     */
    fun touch() {
        if (_mode.value == ViewMode.PARENT) restartExpiry()
    }

    private fun restartExpiry() {
        expiry?.cancel()
        expiry = scope.launch {
            delay(timeout)
            _mode.value = ViewMode.CAREGIVER
        }
    }

    companion object {
        /**
         * Five minutes, from docs/concept.md §4.2. Long enough to write a guide with a child
         * asleep in the room, short enough that a phone put down mid-edit locks itself.
         */
        val DEFAULT_TIMEOUT = 5.minutes
    }
}
