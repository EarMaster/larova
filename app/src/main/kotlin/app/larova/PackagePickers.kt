package app.larova

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.larova.core.domain.export.ExportManifest
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Instant

/**
 * The system file dialog, in both directions.
 *
 * This is the whole of Larova's "cloud support". `CreateDocument` and `OpenDocument` show every
 * provider installed on the phone — Drive, Nextcloud, the SD card, a USB stick — so a backup can go
 * anywhere the person already keeps things, with no SDK, no OAuth and no account on our side. There
 * is nothing here that Google has to approve and nothing that breaks when a provider changes its
 * API.
 */
@Composable
fun rememberBackupPicker(onDestination: (String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        // A specific MIME type would have the dialog offer to open the file with something that
        // cannot read it. Octet-stream says "a file", which is what this is.
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri -> uri?.let { onDestination(it.toString()) } }

    return remember(launcher) { { launcher.launch(suggestedFileName()) } }
}

@Composable
fun rememberRestorePicker(onSource: (String) -> Unit): () -> Unit {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { onSource(it.toString()) } }

    // Any file: a package that arrived by messenger often has whatever type the messenger gave it,
    // and a filter that hid it would leave the person unable to select the file they can plainly
    // see. Whether it is really a package is answered by reading the manifest, not by its name.
    return remember(launcher) { { launcher.launch(arrayOf("*/*")) } }
}

/**
 * `larova-2026-08-23.larova`.
 *
 * Dated rather than numbered, because the question a person asks of a folder full of backups is
 * "which one is recent", and sortable, because that is how the folder will show them.
 */
private fun suggestedFileName(): String =
    "larova-${LocalDate.now()}.${ExportManifest.FILE_EXTENSION}"

/**
 * The date in the import preview, in the phone's own format.
 *
 * Localized here rather than in the shared screen: the platform knows whether this person writes
 * 23.8.2026 or 8/23/2026, and getting that wrong on the one screen that asks "is this the right
 * backup" would make the answer harder to see.
 */
fun formatExportDate(manifest: ExportManifest): String = formatExportDate(manifest.exportedAt)

private fun formatExportDate(instant: Instant): String {
    val local = java.time.Instant.ofEpochMilli(instant.toEpochMilliseconds())
        .atZone(ZoneId.systemDefault())
        .toLocalDate()
    return local.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}
