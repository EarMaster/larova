package app.larova.core.domain.media

import app.larova.core.domain.model.MediaAsset

/**
 * Pictures on their way in and on their way to the screen.
 *
 * [store] is where a photo stops belonging to the gallery it came from: the bytes are copied into
 * app-private storage at a size this app chose, and the temporary read permission the picker
 * granted is never needed again. A picture that stayed a `content://` reference would go blank the
 * day the person tidied their gallery, and would not be in a backup at all.
 *
 * [read] hands back a picture at a size the caller can actually use. A guide step holds a 2048px
 * photo; a thumbnail in the editor is 300 dots across, and decoding the first to draw the second is
 * how an app with ten steps in a guide runs a phone out of memory.
 *
 * Both take and return the same opaque strings the rest of the app uses: a picker source on the way
 * in, a stored relative path on the way out. Nothing here knows what a file is.
 */
interface ImageStore {

    /** Copies a picked picture in, downscaled. Null if it cannot be read or cannot be written. */
    suspend fun store(source: String): MediaAsset?

    /** The stored picture, fitted to [maxEdge] on its longer side. Null if the file is gone. */
    suspend fun read(relativePath: String, maxEdge: Int): ByteArray?
}

/**
 * The sizes pictures exist at, in one place.
 *
 * [STORED] is the invariant from `AGENTS.md`: images are downscaled at import to a 2048px long
 * edge, JPEG q85. It is deliberately not the camera's size — a family with twenty guides would
 * otherwise be carrying a backup they cannot send anywhere.
 */
object ImageSize {

    /** What is written to disk and into every export. */
    const val STORED = 2048

    /** A guide step, on the largest phone screen this will realistically be read on. */
    const val ON_SCREEN = 1440

    /** The editor's preview of a step's picture. */
    const val THUMBNAIL = 320
}
