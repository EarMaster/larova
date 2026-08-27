package app.larova.core.domain.usecase

import app.larova.core.domain.export.MediaFiles
import app.larova.core.domain.media.ImageStore
import app.larova.core.domain.media.MediaIntake
import app.larova.core.domain.model.MediaAsset
import app.larova.core.domain.repository.MediaRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first

/**
 * Everything a screen does with media, in one dependency.
 *
 * Grouped for the same reason `PackageIo` is: the screen that can add a picture is the screen that
 * shows it and the screen that can take it away again, and a ViewModel constructor listing five use
 * cases one by one is one nobody reads.
 *
 * Pictures and files sit side by side here because they are the same job with two different rules:
 * a picture is re-encoded to a size a phone screen can show, a video or a recording is copied
 * exactly as it is.
 */
class Media(
    val addImage: AddImage,
    val addFile: AddMediaFile,
    val loadImage: LoadImage,
    val findFile: FindMediaFile,
    val cleanUp: CleanUpMedia,
)

/**
 * The two things a folder needs from the domain, in one dependency.
 *
 * Making the board and reading what is on it, together for the same reason [Pictures] groups its
 * three: the screen that can make a folder is the screen that has to say what deleting it would
 * take with it, and a ViewModel constructor listing every use case one by one is one nobody reads.
 */
class Folders(
    val create: CreateFolderBoard,
    val observeTiles: ObserveBoardTiles,
)

/**
 * Brings a picked picture in and records it.
 *
 * The file is written first and the row second, in that order on purpose: a row pointing at a file
 * that was never written is a blank picture on a guide step, while a file whose row never arrived
 * is a few hundred kilobytes nobody ever sees. Only one of those is somebody's missing picture.
 *
 * The identifier goes back to the editor, which puts it on the step. Nothing is written to the tile
 * here — the parent may still cancel out of the editor, and a picture they never saved should not
 * turn up on a guide.
 */
@OptIn(ExperimentalUuidApi::class)
class AddImage(
    private val images: ImageStore,
    private val media: MediaRepository,
) {

    suspend operator fun invoke(source: String): Uuid? {
        val asset = images.store(source) ?: return null
        media.register(asset)
        return asset.id
    }
}

/**
 * A stored picture, at the size the screen asked for.
 *
 * Goes through the repository rather than straight to the file, so a picture whose row is gone is
 * gone here too. That is what keeps a deleted picture deleted even while its file waits for the
 * next cleanup.
 */
@OptIn(ExperimentalUuidApi::class)
class LoadImage(
    private val images: ImageStore,
    private val media: MediaRepository,
) {

    suspend operator fun invoke(id: Uuid, maxEdge: Int): ByteArray? {
        val asset = media.find(id) ?: return null
        return images.read(asset.relativePath, maxEdge)
    }
}

/**
 * Brings a picked video or recording in and records it.
 *
 * The same order as a picture — file first, row second — and the same reason: a row pointing at a
 * file that was never written is a tile that plays nothing, while a file whose row never arrived is
 * a few megabytes nobody ever sees.
 *
 * The asset comes back rather than only its identifier, because the editor has something to say
 * about the size before the tile is saved.
 */
@OptIn(ExperimentalUuidApi::class)
class AddMediaFile(
    private val intake: MediaIntake,
    private val media: MediaRepository,
) {

    suspend operator fun invoke(source: String): MediaAsset? {
        val asset = intake.copyIn(source) ?: return null
        media.register(asset)
        return asset
    }
}

/**
 * Where a stored video or recording actually is, for a player to open.
 *
 * Through the repository first, so a file whose row is gone is gone here too, and then through the
 * file system, so a row whose file is gone reports nothing rather than handing a player a path to
 * nowhere. Both happen: an import that arrived without its media leaves the first, and a phone that
 * ran out of space during one leaves the second.
 */
@OptIn(ExperimentalUuidApi::class)
class FindMediaFile(
    private val media: MediaRepository,
    private val files: MediaFiles,
) {

    suspend operator fun invoke(id: Uuid): StoredFile? {
        val asset = media.find(id) ?: return null
        if (!files.exists(asset.relativePath)) return null

        return StoredFile(
            absolutePath = files.absolutePath(asset.relativePath),
            mimeType = asset.mimeType,
            sizeBytes = asset.sizeBytes,
        )
    }
}

/**
 * A file on this device, as a screen needs it.
 *
 * The path is opaque to everything above the platform: it goes to the player and nowhere else. What
 * a path looks like is the platform's business, which is what keeps this shared with iOS.
 */
data class StoredFile(
    val absolutePath: String,
    val mimeType: String,
    val sizeBytes: Long,
)

/**
 * Removes the media no tile refers to any more, files included.
 *
 * Media outlives the tile that introduced it: removing a picture from a guide, deleting the guide,
 * or changing which recording a tile plays leaves a file that nothing points at. Left alone it is
 * invisible, unreachable, and counted against the app's storage for as long as Larova is
 * installed.
 *
 * The rows are what the repository can decide about — it is the only layer that can decode every
 * payload and see what is still pointed at. The files are decided here, from the difference between
 * what was registered before and what survived, because a row that has been deleted no longer knows
 * where its file was.
 */
@OptIn(ExperimentalUuidApi::class)
class CleanUpMedia(
    private val media: MediaRepository,
    private val files: MediaFiles,
) {

    suspend operator fun invoke(): Int {
        val before = media.observeAll().first()
        if (before.isEmpty()) return 0

        val removed = media.deleteOrphans()
        if (removed == 0) return 0

        val surviving = media.observeAll().first().mapTo(mutableSetOf()) { it.id }
        for (asset in before) {
            if (asset.id !in surviving) files.delete(asset.relativePath)
        }
        return removed
    }
}
