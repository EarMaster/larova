package app.larova.core.domain.usecase

import app.larova.core.domain.export.ExportCodec
import app.larova.core.domain.export.DecodedContent
import app.larova.core.domain.export.ExportManifest
import app.larova.core.domain.export.ImportMode
import app.larova.core.domain.model.LogEntry
import app.larova.core.domain.model.MediaAsset
import app.larova.core.domain.repository.BoardRepository
import app.larova.core.domain.repository.CardRepository
import app.larova.core.domain.repository.LogRepository
import app.larova.core.domain.repository.MediaRepository
import app.larova.core.domain.export.PackageIo
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first

/**
 * Puts a package back.
 *
 * This is the one operation in the app that can destroy content, so it is deliberately literal
 * about what it will and will not do:
 *
 * A package from a newer version is declined rather than partly applied — its tiles may hold types
 * this build cannot render, and a half-restored backup is worse than a refused one.
 *
 * `content.json` is checked against the hash in the manifest before anything is written. A file cut
 * short by a messenger and a file that is genuinely small look identical otherwise.
 *
 * Replace deletes the media files as well as the rows. Leaving them would grow the app by the size
 * of every picture the family ever had, invisibly, with nothing left pointing at them.
 *
 * Merge keeps one start screen. Two boards with no parent would leave the app with two start
 * screens and no way to say which holds the tiles, so the incoming root is remapped onto the
 * existing one and its tiles are appended after what is already there.
 */
@OptIn(ExperimentalUuidApi::class)
class ImportPackage(
    private val boards: BoardRepository,
    private val cards: CardRepository,
    private val media: MediaRepository,
    private val log: LogRepository,
    private val io: PackageIo,
    private val ensureRootBoard: EnsureRootBoard,
) {

    sealed interface Result {
        /**
         * [skippedCards] is how many tiles this build could not read — a type a newer Larova
         * wrote. They are still in the file, which is what the screen tells the person.
         */
        data class Imported(
            val boards: Int,
            val cards: Int,
            val media: Int,
            val skippedCards: Int = 0,
        ) : Result

        /** Written by a newer version. The manifest is returned so the screen can say which. */
        data class TooNew(val schemaVersion: Int) : Result

        /** The content does not match the hash in the manifest: truncated or altered in transit. */
        data object Damaged : Result

        /** Opened, but not a Larova package: no manifest, or no content beside it. */
        data object Unreadable : Result

        /**
         * The file itself could not be opened — a permission that lapsed between the picker and
         * the read, or a cloud file the provider has not downloaded yet.
         *
         * Split out from [Unreadable] because the remedies have nothing in common. "This is not a
         * Larova backup" is the wrong thing to tell somebody whose backup is fine and merely
         * still in the cloud, and it is the sentence that makes them go looking for another copy.
         */
        data object CouldNotOpen : Result
    }

    suspend operator fun invoke(source: String, mode: ImportMode): Result {
        // Everything the package says, read and checked before a single row is written.
        return when (val read = read(source)) {
            is Read.Refused -> read.result
            is Read.Accepted -> apply(source, mode, read.content, read.mediaNames)
        }
    }

    private sealed interface Read {
        data class Accepted(val content: DecodedContent, val mediaNames: List<String>) : Read
        data class Refused(val result: Result) : Read
    }

    private suspend fun read(source: String): Read {
        var manifest: ExportManifest? = null
        var contentJson: String? = null
        var mediaNames: List<String> = emptyList()

        val opened = io.store.read(source) { archive ->
            manifest = archive.readText(ExportManifest.MANIFEST_ENTRY)
                ?.let { ExportCodec.decodeManifestOrNull(it) }
            contentJson = archive.readText(ExportManifest.CONTENT_ENTRY)
            mediaNames = archive.namesUnder(ExportManifest.MEDIA_DIRECTORY)
        }

        val foundContent = contentJson
        refusalFor(opened, manifest, foundContent)?.let { return Read.Refused(it) }

        // Null here means the JSON itself is unreadable. A row this build does not understand is a
        // different thing entirely: the decode counts it and carries on, so a backup written by a
        // newer Larova is no longer reported as damaged.
        val content = ExportCodec.decodeContentOrNull(requireNotNull(foundContent))
            ?: return Read.Refused(Result.Damaged)
        return Read.Accepted(content = content, mediaNames = mediaNames)
    }

    /**
     * Every reason to stop, in the order they have to be checked: is it a package at all, is it from
     * a version this build understands, and is it complete. Null means go ahead.
     */
    private fun refusalFor(opened: Boolean, manifest: ExportManifest?, content: String?): Result? =
        when {
            !opened -> Result.CouldNotOpen
            manifest == null || content == null -> Result.Unreadable
            !manifest.isReadable -> Result.TooNew(manifest.schemaVersion)
            io.digest.sha256(content) != manifest.contentSha256 -> Result.Damaged
            else -> null
        }

    private suspend fun apply(
        source: String,
        mode: ImportMode,
        content: DecodedContent,
        mediaNames: List<String>,
    ): Result {
        if (mode == ImportMode.REPLACE) clearEverything()

        val root = ensureRootBoard()
        val incomingRoot = content.boards.firstOrNull { it.parentId == null }
        val remapped = remapRoot(incoming = incomingRoot?.id, existing = root.id)

        for (board in content.boards) {
            if (board.id == incomingRoot?.id) continue // the existing start screen stands in for it
            boards.upsert(board.copy(parentId = board.parentId?.let(remapped)))
        }

        val offset = if (mode == ImportMode.MERGE) nextSortIndex(root.id) else 0
        for ((index, card) in content.cards.withIndex()) {
            cards.upsert(
                card.copy(
                    boardId = remapped(card.boardId),
                    // Appended rather than interleaved: a merge must not reshuffle the tiles a
                    // caregiver has already learned the position of.
                    sortIndex = if (mode == ImportMode.MERGE) offset + index else card.sortIndex,
                ),
            )
        }

        val restored = restoreMedia(source, content.media, mediaNames)
        restoreLog(content.log)

        return Result.Imported(
            boards = content.boards.size,
            cards = content.cards.size,
            media = restored,
            skippedCards = content.skippedCards,
        )
    }

    /**
     * The log, then pruned.
     *
     * Entries are written whatever their age and the retention window is applied afterwards, rather
     * than filtering on the way in: the rule about how long a log is kept lives in one place, and
     * restoring a two-year-old backup must not resurrect two years of events either way.
     *
     * Not cleared first, even by a replacing import — [clearEverything] has already done that, and
     * a merge deliberately keeps both sides. An entry that arrives twice is one row, because the
     * identifier came with it.
     */
    private suspend fun restoreLog(entries: List<LogEntry>) {
        for (entry in entries) {
            log.append(entry)
        }
        log.pruneOlderThanDays(LOG_RETENTION_DAYS)
    }

    /** Only the media the content actually refers to, and only what the archive really contains. */
    private suspend fun restoreMedia(
        source: String,
        assets: List<MediaAsset>,
        namesInArchive: List<String>,
    ): Int {
        if (assets.isEmpty()) return 0
        var restored = 0
        val present = namesInArchive.toSet()

        io.store.read(source) { archive ->
            for (asset in assets) {
                if (asset.relativePath !in present) continue
                val copied = archive.copyTo(
                    name = asset.relativePath,
                    absolutePath = io.mediaFiles.absolutePath(asset.relativePath),
                )
                if (copied) {
                    media.register(asset)
                    restored++
                }
            }
        }
        return restored
    }

    private suspend fun clearEverything() {
        // Rows first, then the files. The database does cascade cards when their board goes, but
        // deleting them explicitly is what makes this function readable as what it is: the one
        // place in the app that throws content away.
        for (card in cards.observeAllCards().first()) {
            cards.delete(card.id)
        }
        for (asset in media.observeAll().first()) {
            media.delete(asset.id)
        }
        log.clear()
        for (board in boards.all()) {
            boards.delete(board.id)
        }
        io.mediaFiles.deleteAll()
    }

    private suspend fun nextSortIndex(boardId: Uuid): Int =
        cards.observeCards(boardId).first().maxOfOrNull { it.sortIndex }?.plus(1) ?: 0

    private fun remapRoot(incoming: Uuid?, existing: Uuid): (Uuid) -> Uuid = { id ->
        if (incoming != null && id == incoming) existing else id
    }

}
