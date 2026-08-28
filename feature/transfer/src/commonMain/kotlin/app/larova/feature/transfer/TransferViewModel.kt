package app.larova.feature.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.larova.core.domain.export.ExportManifest
import app.larova.core.domain.export.ImportMode
import app.larova.core.domain.model.LastBackup
import app.larova.core.domain.usecase.ExportPackage
import app.larova.core.domain.usecase.ImportPackage
import app.larova.core.domain.usecase.ObserveLastBackup
import app.larova.core.domain.usecase.ReadPackagePreview
import app.larova.core.domain.usecase.RecordLastBackup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * What the screen has to say about the last thing that happened.
 *
 * Spelled out as cases rather than a message string, because the words belong in the resource file
 * and because two of these are refusals that have to be phrased carefully: a person restoring a
 * backup is often already having a bad day.
 */
sealed interface TransferOutcome {
    data class BackedUp(val cards: Int, val media: Int) : TransferOutcome
    data class Restored(val cards: Int, val media: Int) : TransferOutcome
    data object BackupFailed : TransferOutcome
    data class FileTooNew(val schemaVersion: Int) : TransferOutcome
    data object FileDamaged : TransferOutcome
    data object FileUnreadable : TransferOutcome
}

data class TransferUiState(
    val isBusy: Boolean = false,
    /** Set once a file has been chosen and read, before anything has been applied. */
    val preview: ExportManifest? = null,
    val pendingSource: String? = null,
    val outcome: TransferOutcome? = null,
    /**
     * Read from preferences rather than tracked here, so it survives the screen being closed —
     * which is the only case in which the question it answers is ever asked.
     */
    val lastBackup: LastBackup? = null,
)

class TransferViewModel(
    private val exportPackage: ExportPackage,
    private val readPreview: ReadPackagePreview,
    private val importPackage: ImportPackage,
    private val recordLastBackup: RecordLastBackup,
    observeLastBackup: ObserveLastBackup,
) : ViewModel() {

    private val _state = MutableStateFlow(TransferUiState())

    // Folded in rather than copied into `_state` on every write: the stored date is the export use
    // case's to set, and a second copy of it here is a second thing that can be stale. Every
    // `_state.value = TransferUiState(...)` below would otherwise have to remember to carry it.
    val state: StateFlow<TransferUiState> =
        combine(_state, observeLastBackup()) { ui, last -> ui.copy(lastBackup = last) }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                initialValue = TransferUiState(),
            )

    /** [destination] is whatever the system dialog handed back — a URI here, a path elsewhere. */
    fun onDestinationChosen(destination: String, label: String?) {
        _state.update { it.copy(isBusy = true, outcome = null) }
        viewModelScope.launch {
            val outcome = when (val result = exportPackage(destination, label)) {
                is ExportPackage.Result.Written -> {
                    recordLastBackup(
                        LastBackup(at = result.at, cards = result.counts, media = result.mediaCount),
                    )
                    TransferOutcome.BackedUp(cards = result.counts, media = result.mediaCount)
                }

                ExportPackage.Result.Failed -> TransferOutcome.BackupFailed
            }
            _state.update { it.copy(isBusy = false, outcome = outcome) }
        }
    }

    /**
     * Reading is separate from applying. Nothing is written until the person has seen what is in the
     * file and said which of the two things they meant — replace is the only irreversible action in
     * the app.
     */
    fun onSourceChosen(source: String) {
        _state.update { it.copy(isBusy = true, outcome = null, preview = null) }
        viewModelScope.launch {
            val next = when (val result = readPreview(source)) {
                is ReadPackagePreview.Result.Readable ->
                    TransferUiState(preview = result.manifest, pendingSource = source)

                is ReadPackagePreview.Result.TooNew ->
                    TransferUiState(outcome = TransferOutcome.FileTooNew(result.manifest.schemaVersion))

                ReadPackagePreview.Result.Unreadable ->
                    TransferUiState(outcome = TransferOutcome.FileUnreadable)
            }
            _state.value = next
        }
    }

    fun onCancelImport() = _state.update {
        it.copy(preview = null, pendingSource = null)
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }

    fun onConfirmImport(mode: ImportMode) {
        val source = _state.value.pendingSource ?: return
        _state.update { it.copy(isBusy = true, preview = null) }
        viewModelScope.launch {
            val outcome = when (val result = importPackage(source, mode)) {
                is ImportPackage.Result.Imported ->
                    TransferOutcome.Restored(cards = result.cards, media = result.media)

                is ImportPackage.Result.TooNew -> TransferOutcome.FileTooNew(result.schemaVersion)
                ImportPackage.Result.Damaged -> TransferOutcome.FileDamaged
                ImportPackage.Result.Unreadable -> TransferOutcome.FileUnreadable
            }
            _state.value = TransferUiState(outcome = outcome)
        }
    }
}
