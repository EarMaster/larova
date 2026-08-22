package app.larova

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.larova.core.domain.model.AppearanceSetting
import app.larova.core.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State that belongs to the whole app rather than to one screen — currently just the appearance
 * setting, which the theme above the navigation graph needs.
 *
 * The initial value is the default rather than the stored one, because the stored value arrives
 * from a file read. That means the very first frame can be light while the user has chosen dark.
 * The alternative is holding the first frame back until a disk read finishes, which is a worse
 * trade on an app that is opened in a hurry.
 */
class AppViewModel(private val preferences: PreferencesRepository) : ViewModel() {

    val appearance: StateFlow<AppearanceSetting> = preferences.observeAppearance()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AppearanceSetting.DEFAULT,
        )

    fun setAppearance(setting: AppearanceSetting) {
        viewModelScope.launch { preferences.setAppearance(setting) }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
