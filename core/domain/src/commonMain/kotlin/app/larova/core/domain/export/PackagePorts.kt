package app.larova.core.domain.export

/**
 * Writing and reading the export container, without the domain having to know what a stream is.
 *
 * The destination and the source are opaque strings — a `content://` URI on Android, a file URL on
 * iOS. They come from the system file dialog and go straight back to the platform that issued them,
 * which is what lets an export land in Drive or Nextcloud with no cloud integration on our side.
 *
 * Entries are named, not ordered. `manifest.json` has to be readable without unpacking the rest,
 * because the import preview is shown before anyone commits to anything.
 */
interface PackageStore {

    /**
     * Opens [destination] for writing and hands the caller a sink. The archive is finished when
     * [build] returns; if it throws, nothing half-written is left behind that looks complete.
     */
    suspend fun write(destination: String, build: suspend (ArchiveSink) -> Unit): Boolean

    /** Opens [source] for reading. False when it cannot be opened or is not an archive at all. */
    suspend fun read(source: String, read: suspend (ArchiveSource) -> Unit): Boolean
}

interface ArchiveSink {

    /** For the small text entries: the manifest and the content. */
    suspend fun putText(name: String, text: String)

    /**
     * For media. Copied straight from disk rather than through memory — a family's holiday video
     * does not fit in a ByteArray on a phone that is already low on space.
     */
    suspend fun putFile(name: String, absolutePath: String): Boolean
}

interface ArchiveSource {

    suspend fun readText(name: String): String?

    /** Every entry under [prefix]. Used to find the media without trusting the content listing. */
    suspend fun namesUnder(prefix: String): List<String>

    /** Copies one entry to disk, creating parent directories. False if the entry is not there. */
    suspend fun copyTo(name: String, absolutePath: String): Boolean
}

/**
 * SHA-256, as hexadecimal.
 *
 * In the manifest so an import can tell a truncated transfer from a complete one. A messenger that
 * cut a file in half and an export that is genuinely empty look identical without it.
 */
interface Digest {
    fun sha256(text: String): String
}

/** Where media lives on this platform, and what is actually there. */
interface MediaFiles {

    /** Absolute path for a stored relative path such as `media/3f2a….jpg`. */
    fun absolutePath(relativePath: String): String

    fun exists(relativePath: String): Boolean

    fun sizeBytes(relativePath: String): Long

    fun delete(relativePath: String): Boolean

    /**
     * Empties the media directory.
     *
     * Called by a replacing import and by nothing else. Anything left behind would be a file no row
     * points at any more: invisible, unreachable, and counted against the app's storage for as long
     * as it is installed. The person asked for everything to be replaced, and files are part of
     * everything.
     */
    fun deleteAll(): Boolean
}

/**
 * The three pieces of plumbing an export needs from its platform, in one place.
 *
 * Grouped because they are never useful apart: writing a package means an archive, a digest for the
 * manifest, and the media directory the files come from. Passing them as one parameter also keeps
 * the use cases readable — a constructor with seven dependencies is one nobody reads.
 */
data class PackageIo(
    val store: PackageStore,
    val digest: Digest,
    val mediaFiles: MediaFiles,
)
