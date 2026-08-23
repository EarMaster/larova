package app.larova.feature.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.larova.core.domain.export.ExportManifest
import app.larova.core.domain.export.ImportMode
import app.larova.core.domain.usecase.ExportPackage
import app.larova.core.domain.usecase.ImportPackage
import app.larova.core.domain.usecase.ReadPackagePreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
)

class TransferViewModel(
    private val exportPackage: ExportPackage,
    private val readPreview: ReadPackagePreview,
    private val importPackage: ImportPackage,
) : ViewModel() {

    private val _state = MutableStateFlow(TransferUiState())
    val state: StateFlow<TransferUiState> = _state.asStateFlow()

    /** [destination] is whatever the system dialog handed back — a URI here, a path elsewhere. */
    fun onDestinationChosen(destination: String, label: String?) {
        _state.update { it.copy(isBusy = true, outcome = null) }
        viewModelScope.launch {
            val outcome = when (val result = exportPackage(destination, label)) {
                is ExportPackage.Result.Written ->
                    TransferOutcome.BackedUp(cards = result.counts, media = result.mediaCount)

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
