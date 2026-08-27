package app.larova.core.platform

import android.content.Context
import android.net.Uri
import app.larova.core.domain.export.ArchiveSink
import app.larova.core.domain.export.ArchiveSource
import app.larova.core.domain.export.Digest
import app.larova.core.domain.export.MediaFiles
import app.larova.core.domain.export.PackageStore
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The export container as a plain ZIP, written through the URI the system dialog handed back.
 *
 * `java.util.zip` and nothing else. Every installed cloud provider appears in that dialog, Drive
 * included, so this is the whole of Larova's "cloud support": no SDK, no OAuth, no account, and
 * nothing to review when a provider changes its API.
 */
class AndroidPackageStore(private val context: Context) : PackageStore {

    override suspend fun write(
        destination: String,
        build: suspend (ArchiveSink) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        val uri = destination.toUriOrNull() ?: return@withContext false
        try {
            // "wt" truncates. Overwriting an earlier backup of the same name must not leave the
            // tail of the old one attached to the new.
            context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                ZipOutputStream(out.buffered()).use { zip ->
                    build(ZipSink(zip))
                    zip.finish()
                }
            } != null
        } catch (_: java.io.IOException) {
            false
        } catch (_: SecurityException) {
            // The permission the picker granted can be gone by the time it is used — the document
            // was deleted, or the provider revoked it.
            false
        }
    }

    override suspend fun read(
        source: String,
        read: suspend (ArchiveSource) -> Unit,
    ): Boolean = withContext(Dispatchers.IO) {
        val uri = source.toUriOrNull() ?: return@withContext false
        try {
            // Read into memory once for the text entries, and re-open the stream for each file
            // copy: a ZipInputStream cannot be rewound, and the alternative is holding a whole
            // package of videos in RAM.
            val entries = readTextEntries(uri) ?: return@withContext false
            read(UriArchiveSource(uri, entries))
            true
        } catch (_: java.io.IOException) {
            false
        } catch (_: SecurityException) {
            false
        }
    }

    private fun readTextEntries(uri: Uri): Map<String, String>? {
        val input = context.contentResolver.openInputStream(uri) ?: return null
        return input.use { stream ->
            ZipInputStream(stream.buffered()).use { zip -> zip.collectJsonEntries() }
        }
    }

    /**
     * The JSON entries only. Media is copied straight to disk later rather than held in memory, and
     * a package claiming an absurd number of entries or an absurdly large one is refused rather
     * than followed — a file that arrived by messenger is not a file to trust.
     */
    private fun ZipInputStream.collectJsonEntries(): Map<String, String>? {
        val texts = mutableMapOf<String, String>()
        var entry: ZipEntry? = nextEntry
        var seen = 0
        while (entry != null) {
            if (++seen > MAX_ENTRIES) return null
            val name = entry.name
            if (!entry.isDirectory && name.endsWith(".json") && !name.isTraversal()) {
                texts[name] = readBoundedText() ?: return null
            }
            closeEntry()
            entry = nextEntry
        }
        return texts
    }

    private inner class UriArchiveSource(
        private val uri: Uri,
        private val texts: Map<String, String>,
    ) : ArchiveSource {

        override suspend fun readText(name: String): String? = texts[name]

        override suspend fun namesUnder(prefix: String): List<String> =
            withContext(Dispatchers.IO) {
                val names = mutableListOf<String>()
                context.contentResolver.openInputStream(uri)?.use { input ->
                    ZipInputStream(input.buffered()).use { zip ->
                        var entry: ZipEntry? = zip.nextEntry
                        while (entry != null) {
                            val name = entry.name
                            if (!entry.isDirectory && name.startsWith(prefix) && !name.isTraversal()) {
                                names += name
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                }
                names
            }

        override suspend fun copyTo(name: String, absolutePath: String): Boolean =
            withContext(Dispatchers.IO) {
                if (name.isTraversal()) return@withContext false
                context.contentResolver.openInputStream(uri)?.use { input ->
                    ZipInputStream(input.buffered()).use { zip ->
                        var entry: ZipEntry? = zip.nextEntry
                        while (entry != null) {
                            if (entry.name == name && !entry.isDirectory) {
                                val target = File(absolutePath)
                                target.parentFile?.mkdirs()
                                target.outputStream().buffered().use { zip.copyTo(it) }
                                return@withContext true
                            }
                            zip.closeEntry()
                            entry = zip.nextEntry
                        }
                    }
                }
                false
            }
    }

    private class ZipSink(private val zip: ZipOutputStream) : ArchiveSink {

        override suspend fun putText(name: String, text: String) = withContext(Dispatchers.IO) {
            zip.putNextEntry(ZipEntry(name))
            zip.write(text.encodeToByteArray())
            zip.closeEntry()
        }

        override suspend fun putFile(name: String, absolutePath: String): Boolean =
            withContext(Dispatchers.IO) {
                val file = File(absolutePath)
                if (!file.isFile) return@withContext false
                zip.putNextEntry(ZipEntry(name))
                file.inputStream().buffered().use { it.copyTo(zip) }
                zip.closeEntry()
                true
            }
    }

    private fun String.toUriOrNull(): Uri? = try {
        Uri.parse(this)
    } catch (_: IllegalArgumentException) {
        null
    }

    /**
     * An entry name is not a path to trust. `../../databases/larova.db` inside a package would let
     * a file arriving by messenger write wherever the app can — the oldest trick there is against
     * an unpacker, and the reason every entry name is checked before it is used.
     */
    private fun String.isTraversal(): Boolean =
        startsWith("/") || contains("..") || contains("\\") || contains(":")

    /** Reads a text entry, refusing one large enough to have been meant as a way to run us out of memory. */
    private fun ZipInputStream.readBoundedText(): String? {
        val out = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(BUFFER_BYTES)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read <= 0) break
            total += read
            if (total > MAX_TEXT_BYTES) return null
            out.write(buffer, 0, read)
        }
        return out.toByteArray().decodeToString()
    }

    private companion object {
        const val BUFFER_BYTES = 8 * 1024

        /** content.json for a very full installation is well under this. */
        const val MAX_TEXT_BYTES = 32L * 1024 * 1024

        /** A package with more entries than this is not one of ours. */
        const val MAX_ENTRIES = 10_000
    }
}

/** SHA-256 through the platform's own implementation; nothing to get wrong. */
class AndroidDigest : Digest {
    override fun sha256(text: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(text.encodeToByteArray())
            .joinToString("") { byte ->
                byte.toInt().and(BYTE_MASK).toString(RADIX_HEX).padStart(HEX_DIGITS, '0')
            }

    private companion object {
        const val RADIX_HEX = 16
        const val BYTE_MASK = 0xFF
        const val HEX_DIGITS = 2
    }
}

/** Media on disk, under `filesDir/media`. */
class AndroidMediaFiles(private val paths: PlatformPaths) : MediaFiles {

    override fun absolutePath(relativePath: String): String =
        File(root(), relativePath.removePrefix(PREFIX)).absolutePath

    override fun exists(relativePath: String): Boolean = File(absolutePath(relativePath)).isFile

    override fun sizeBytes(relativePath: String): Long = File(absolutePath(relativePath)).length()

    override fun delete(relativePath: String): Boolean = File(absolutePath(relativePath)).delete()

    override fun deleteAll(): Boolean {
        // Only the files directly in the media directory, and only files. Not a recursive delete:
        // this runs on a path the app owns, but a recursive delete is the wrong tool to have lying
        // around next to a user's content.
        val children = root().listFiles() ?: return false
        return children.filter { it.isFile }.all { it.delete() }
    }

    private fun root(): File = File(paths.mediaDirectory())

    private companion object {
        const val PREFIX = "media/"
    }
}
