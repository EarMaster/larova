package app.larova

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.larova.core.domain.model.AppearanceSetting
import app.larova.core.domain.model.ViewMode
import app.larova.core.domain.repository.PreferencesRepository
import app.larova.core.domain.session.ViewModeSession
import app.larova.core.domain.usecase.CleanUpMedia
import app.larova.core.domain.usecase.LockParentView
import app.larova.core.domain.usecase.PruneLog
import app.larova.core.domain.usecase.PublishShortcuts
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State that belongs to the whole app rather than to one screen: the appearance setting the theme
 * above the navigation graph needs, which view the app is in, and the one piece of housekeeping
 * that has no screen of its own.
 *
 * The initial value is the default rather than the stored one, because the stored value arrives
 * from a file read. That means the very first frame can be light while the user has chosen dark.
 * The alternative is holding the first frame back until a disk read finishes, which is a worse
 * trade on an app that is opened in a hurry.
 */
class AppViewModel(
    private val preferences: PreferencesRepository,
    private val lockParentView: LockParentView,
    session: ViewModeSession,
    cleanUpMedia: CleanUpMedia,
    pruneLog: PruneLog,
    publishShortcuts: PublishShortcuts,
) : ViewModel() {

    /**
     * Read here rather than in each screen so that the whole graph agrees on which view it is in.
     * When the five minutes run out, every screen loses its editing controls in the same frame.
     */
    val viewMode: StateFlow<ViewMode> = session.mode

    val appearance: StateFlow<AppearanceSetting> = preferences.observeAppearance()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AppearanceSetting.DEFAULT,
        )

    fun setAppearance(setting: AppearanceSetting) {
        viewModelScope.launch { preferences.setAppearance(setting) }
    }

    fun leaveParentView() = lockParentView()

    init {
        // Pictures whose step never made it into a saved tile: picked, and then the editor was left
        // by the back gesture. The editor sweeps after a save and after a delete, and this is the
        // one way out of it that neither of those covers.
        viewModelScope.launch { cleanUpMedia() }
        // An offline app with no background work has one reliable moment to drop what is older than
        // the retention window, and to tell the launcher what is worth a shortcut: when somebody
        // opens it. The shortcuts come after the prune, so the count behind them is the log the app
        // actually keeps.
        viewModelScope.launch {
            pruneLog()
            publishShortcuts()
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
