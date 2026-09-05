package app.larova.core.domain.usecase

import app.larova.core.domain.export.ExportCodec
import app.larova.core.domain.export.ExportContent
import app.larova.core.domain.export.ExportManifest
import app.larova.core.domain.export.PackageIo
import app.larova.core.domain.export.toExport
import app.larova.core.domain.model.LastBackup
import app.larova.core.domain.repository.BoardRepository
import app.larova.core.domain.repository.CardRepository
import app.larova.core.domain.repository.CardTextRepository
import app.larova.core.domain.repository.LogRepository
import app.larova.core.domain.repository.MediaRepository
import app.larova.core.domain.repository.PreferencesRepository
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.flow.first

/**
 * Writes everything to one file.
 *
 * The order matters: `content.json` is serialized first so its hash can go into `manifest.json`,
 * and the manifest is written as the first entry so that reading it back does not mean unpacking
 * the media. That is what makes the import preview possible before anyone commits to anything.
 *
 * Media that has a row but no file on disk is left out of the package and out of the manifest's
 * count. It has already been lost; writing its name into a backup would only move the problem to
 * whoever restores it.
 */
/**
 * Suppressed rather than bundled. Five of the seven are the repositories a package is written from,
 * and this is the one place each of them is read in full — a holder object wrapping them to satisfy
 * a counter would hide exactly that, which is the thing the counter exists to make visible. The
 * same argument `EditCardViewModel` makes, and the same conclusion: never raise the threshold.
 */
@Suppress("LongParameterList")
class ExportPackage(
    private val boards: BoardRepository,
    private val cards: CardRepository,
    private val cardText: CardTextRepository,
    private val media: MediaRepository,
    private val log: LogRepository,
    private val io: PackageIo,
    private val appVersion: String,
) {

    sealed interface Result {
        /** [at] is the manifest's own timestamp, so what the screen shows is what the file says. */
        data class Written(val at: Instant, val counts: Int, val mediaCount: Int) : Result

        /** The destination could not be written: permission gone, document deleted, disk full. */
        data object Failed : Result
    }

    suspend operator fun invoke(destination: String, label: String? = null): Result {
        val allBoards = boards.all()
        val allCards = cards.observeAllCards().first()
        val presentMedia = media.observeAll().first().filter { io.mediaFiles.exists(it.relativePath) }
        // The log is part of what a family typed, so it goes into the backup. Bounded by the same
        // retention that bounds the screen: a backup is not the place a pruned event comes back.
        val entries = log.observeRecent(LOG_EXPORT_LIMIT).first()
        // Only variants whose tile is in the package. A row pointing at a tile that is not in the
        // file is not a translation of anything, and writing one out would hand whoever restores
        // it a problem this side already knew about.
        val cardIds = allCards.map { it.id }.toSet()
        val variants = cardText.all().filter { it.cardId in cardIds }

        // Mapped at the boundary rather than handed the domain models: the file has its own row
        // types so that adding a field to Card cannot silently change what a backup contains.
        val content = ExportContent(
            boards = allBoards.map { it.toExport() },
            cards = allCards.map { it.toExport() },
            media = presentMedia.map { it.toExport() },
            log = entries.map { it.toExport() },
            cardText = variants.map { it.toExport() },
        )
        val contentJson = ExportCodec.json.encodeToString(content)

        val manifest = ExportManifest(
            appVersion = appVersion,
            exportedAt = Clock.System.now(),
            label = label?.trim()?.takeIf { it.isNotEmpty() },
            counts = content.counts,
            contentSha256 = io.digest.sha256(contentJson),
        )

        val written = io.store.write(destination) { sink ->
            sink.putText(ExportManifest.MANIFEST_ENTRY, ExportCodec.encode(manifest))
            sink.putText(ExportManifest.CONTENT_ENTRY, contentJson)
            for (asset in presentMedia) {
                sink.putFile(asset.relativePath, io.mediaFiles.absolutePath(asset.relativePath))
            }
        }

        return if (written) {
            Result.Written(
                at = manifest.exportedAt,
                counts = allCards.size,
                mediaCount = presentMedia.size,
            )
        } else {
            Result.Failed
        }
    }
}

/**
 * As many entries as thirty days of a busy family can produce, and no more. A number rather than
 * everything, because `observeRecent` needs one and an unbounded read of a table that only ever
 * grows is the kind of thing that works until it does not.
 */
private const val LOG_EXPORT_LIMIT = 5_000

/** The date and counts the backup screen shows, or null on an installation that has never run one. */
class ObserveLastBackup(private val preferences: PreferencesRepository) {
    operator fun invoke() = preferences.observeLastBackup()
}

/**
 * Notes that a backup succeeded.
 *
 * Separate from [ExportPackage] on purpose, and not because of a parameter count: writing the file
 * is the family's data leaving the app, while this is a preference about this installation. The
 * caller runs it only after a `Written`, so a backup that failed leaves the old date standing —
 * "last backed up today" over a failure is the sentence that stops somebody trying again.
 */
class RecordLastBackup(private val preferences: PreferencesRepository) {
    suspend operator fun invoke(backup: LastBackup) = preferences.setLastBackup(backup)
}

/**
 * What a package says about itself, read without applying any of it.
 *
 * Shown to the person before they choose between merge and replace, because replace is the one
 * irreversible thing in the app and "12 tiles, 4 files, made on the 3rd" is the only way to tell a
 * backup from the wrong backup.
 */
class ReadPackagePreview(private val io: PackageIo) {

    sealed interface Result {
        data class Readable(val manifest: ExportManifest) : Result

        /** Written by a newer Larova. Declined politely rather than half-applied. */
        data class TooNew(val manifest: ExportManifest) : Result

        /** Not a Larova package, or damaged beyond the manifest. */
        data object Unreadable : Result

        /** The file could not be opened at all. See `ImportPackage.Result.CouldNotOpen`. */
        data object CouldNotOpen : Result
    }

    suspend operator fun invoke(source: String): Result {
        var manifest: ExportManifest? = null
        val opened = io.store.read(source) { archive ->
            manifest = archive.readText(ExportManifest.MANIFEST_ENTRY)
                ?.let { ExportCodec.decodeManifestOrNull(it) }
        }

        val found = manifest
        return when {
            !opened -> Result.CouldNotOpen
            found == null -> Result.Unreadable
            !found.isReadable -> Result.TooNew(found)
            else -> Result.Readable(found)
        }
    }
}
