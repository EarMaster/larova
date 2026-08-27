package app.larova.core.domain.usecase

import app.larova.core.domain.export.ExportCodec
import app.larova.core.domain.export.ExportContent
import app.larova.core.domain.export.ExportManifest
import app.larova.core.domain.repository.BoardRepository
import app.larova.core.domain.repository.CardRepository
import app.larova.core.domain.repository.LogRepository
import app.larova.core.domain.repository.MediaRepository
import app.larova.core.domain.export.PackageIo
import kotlin.time.Clock
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
class ExportPackage(
    private val boards: BoardRepository,
    private val cards: CardRepository,
    private val media: MediaRepository,
    private val log: LogRepository,
    private val io: PackageIo,
    private val appVersion: String,
) {

    sealed interface Result {
        data class Written(val counts: Int, val mediaCount: Int) : Result

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

        val content = ExportContent(
            boards = allBoards,
            cards = allCards,
            media = presentMedia,
            log = entries,
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
            Result.Written(counts = allCards.size, mediaCount = presentMedia.size)
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
    }

    suspend operator fun invoke(source: String): Result {
        var manifest: ExportManifest? = null
        val opened = io.store.read(source) { archive ->
            manifest = archive.readText(ExportManifest.MANIFEST_ENTRY)
                ?.let { ExportCodec.decodeManifestOrNull(it) }
        }

        val found = manifest
        return when {
            !opened || found == null -> Result.Unreadable
            !found.isReadable -> Result.TooNew(found)
            else -> Result.Readable(found)
        }
    }
}
