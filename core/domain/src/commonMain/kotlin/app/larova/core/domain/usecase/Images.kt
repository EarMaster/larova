package app.larova.core.domain.usecase

import app.larova.core.domain.export.MediaFiles
import app.larova.core.domain.media.ImageStore
import app.larova.core.domain.repository.MediaRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first

/**
 * Everything the editor does with pictures, in one dependency.
 *
 * Grouped for the same reason `PackageIo` is: the screen that can add a picture is the screen that
 * shows it and the screen that can take it away again, and a ViewModel constructor listing them one
 * by one is one nobody reads. Screens that only ever show a picture take [LoadImage] alone.
 */
class Pictures(
    val add: AddImage,
    val load: LoadImage,
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
 * Removes the pictures no tile refers to any more, files included.
 *
 * Media outlives the step that introduced it: removing a picture from a guide, or deleting the
 * guide, leaves a file that nothing points at. Left alone it is invisible, unreachable, and counted
 * against the app's storage for as long as Larova is installed.
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
