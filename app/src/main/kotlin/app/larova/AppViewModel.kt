package app.larova

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.larova.core.domain.model.AppearanceSetting
import app.larova.core.domain.model.ViewMode
import app.larova.core.domain.repository.PreferencesRepository
import app.larova.core.domain.session.ViewModeSession
import app.larova.core.domain.usecase.CleanUpMedia
import app.larova.core.domain.usecase.LockParentView
import app.larova.core.domain.model.Entitlement
import app.larova.feature.settings.SupportMessage
import app.larova.feature.settings.UnlockCheck
import app.larova.feature.settings.UnlockMessage
import app.larova.core.domain.usecase.ObserveEntitlement
import app.larova.core.domain.usecase.ObserveSupportCount
import app.larova.core.domain.usecase.PruneLog
import app.larova.core.domain.usecase.RecordSupport
import app.larova.core.domain.usecase.RefreshEntitlement
import app.larova.core.domain.usecase.UnlockPrice
import app.larova.core.domain.usecase.PublishShortcuts
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
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
 *
 * The parameter count is suppressed rather than bundled. Four of the seven are launch-time
 * housekeeping that has no screen to belong to — sweeping orphaned pictures, pruning the log,
 * republishing shortcuts, asking the store what was already bought — and a holder object wrapping
 * them would hide that this is the one place any of them is called from.
 */
@Suppress("LongParameterList")
class AppViewModel(
    private val preferences: PreferencesRepository,
    private val lockParentView: LockParentView,
    session: ViewModeSession,
    cleanUpMedia: CleanUpMedia,
    pruneLog: PruneLog,
    publishShortcuts: PublishShortcuts,
    private val observeEntitlement: ObserveEntitlement,
    observeSupportCount: ObserveSupportCount,
    private val refreshEntitlement: RefreshEntitlement,
    private val unlockPrice: UnlockPrice,
    private val recordSupport: RecordSupport,
) : ViewModel() {

    /**
     * Read here rather than in each screen so that the whole graph agrees on which view it is in.
     * When the five minutes run out, every screen loses its editing controls in the same frame.
     */
    val viewMode: StateFlow<ViewMode> = session.mode

    /**
     * What the settings screen says about the paid unlock, and why.
     *
     * Held here rather than in a settings ViewModel because the answer is the same everywhere and
     * the editor is already collecting it: two collectors of one flow beats two sources of truth.
     */
    val entitlement: StateFlow<Entitlement> = observeEntitlement()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = Entitlement.NONE,
        )

    /**
     * How many times this installation has contributed. Kept beside the entitlement because both
     * are answers the settings screen needs and neither belongs to a tile.
     *
     * Arguably one screen's state rather than the app's, and the first candidate to move if
     * settings grows a ViewModel of its own. One integer and two actions did not justify one yet.
     */
    val supportCount: StateFlow<Int> = observeSupportCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = 0,
        )

    private val _unlockCheck = MutableStateFlow<UnlockCheck>(UnlockCheck.Idle)

    /**
     * What the settings screen's full-version card is doing, and what the last ask found.
     *
     * Beside the entitlement rather than derived from it, because the two say different things:
     * the entitlement is what this installation owns, this is what happened when somebody asked.
     * "Still nothing" is not visible in the first and is the whole point of the second.
     */
    val unlockCheck: StateFlow<UnlockCheck> = _unlockCheck.asStateFlow()

    private val _supportMessage = MutableStateFlow<SupportMessage?>(null)

    /** What the contribution card says happened last. Null until anything has. */
    val supportMessage: StateFlow<SupportMessage?> = _supportMessage.asStateFlow()

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

    /**
     * Asks the store again, on request.
     *
     * The same call already runs at every launch, so this is not a second mechanism — it is a
     * retry for the case the automatic one cannot cover: a phone that was offline when the app
     * started and is online now, with the app still open. Nothing is reported back, because the
     * only honest outcome to show is the entitlement itself, and that arrives through [entitlement].
     */
    /** Counted only after Play confirmed and signed it — see `SupportPurchases.contribute`. */
    fun onSupported() {
        viewModelScope.launch {
            recordSupport()
            _supportMessage.value = SupportMessage.THANKS
        }
    }

    fun onSupportUnavailable() {
        _supportMessage.value = SupportMessage.UNAVAILABLE
    }

    fun checkPurchasesAgain() {
        if (_unlockCheck.value is UnlockCheck.Checking) return
        viewModelScope.launch {
            _unlockCheck.value = UnlockCheck.Checking
            refreshEntitlement()
            // Read back from the repository rather than from [entitlement]: that flow is shared
            // and only conflates through a collector, so a receipt written a millisecond ago may
            // not have reached its value yet. Asking again costs one verify and cannot be stale.
            _unlockCheck.value = if (observeEntitlement().first().unlocked) {
                // Nothing to say: the card behind this now reads "Unlocked", which is the answer.
                UnlockCheck.Idle
            } else {
                UnlockCheck.NotFound(price = unlockPrice())
            }
        }
    }

    /** Closes the offer. Not the same as buying: the card goes back to saying what it said. */
    fun dismissUnlockCheck() {
        _unlockCheck.value = UnlockCheck.Idle
    }

    /**
     * The purchase went through, so the offer has nothing left to offer.
     *
     * Nothing is refreshed here — the repository wrote the verified receipt before returning, and
     * the entitlement flow carries it to the card on its own.
     */
    fun onUnlockPurchased() {
        _unlockCheck.value = UnlockCheck.Idle
    }

    fun onUnlockPending() = messageOnOffer(UnlockMessage.PENDING)

    fun onUnlockUnavailable() = messageOnOffer(UnlockMessage.UNAVAILABLE)

    /**
     * Only onto an offer that is still open. A message with no dialog to sit in would be a state
     * nothing can show and nothing would ever clear.
     */
    private fun messageOnOffer(message: UnlockMessage) {
        _unlockCheck.update { current ->
            if (current is UnlockCheck.NotFound) current.copy(message = message) else current
        }
    }

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
        // Asks the store once per launch what this account owns. This is the whole of "restore
        // purchases": a new phone, a reinstall or a purchase finished after the app was closed all
        // arrive here, so there is no button for it and nothing for anybody to find.
        //
        // Failure is the normal case and is silent. There is no internet permission in this app, so
        // the question goes through the Play Store app, and on a phone that has been offline for a
        // fortnight there is nothing to ask. It never removes an unlock — see
        // PlayEntitlementRepository.
        viewModelScope.launch { refreshEntitlement() }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
