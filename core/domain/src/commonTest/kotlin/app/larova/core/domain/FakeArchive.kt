package app.larova.core.domain

import app.larova.core.domain.export.ArchiveSink
import app.larova.core.domain.export.ArchiveSource
import app.larova.core.domain.export.Digest
import app.larova.core.domain.export.MediaFiles
import app.larova.core.domain.export.PackageStore

/**
 * An archive in a map, and a media directory in another one.
 *
 * Enough to run the whole export-and-import path without a file system, which is what makes the
 * round trip a unit test rather than something only an instrumented run can check. The ZIP itself
 * is the platform's job and is exercised on a device.
 */
class FakePackageStore(
    private val mediaFiles: FakeMediaFiles,
    /**
     * The packages that have been written, by destination. Passed in rather than owned, because a
     * package is a file on shared storage: it outlives the installation that wrote it, and the one
     * that reads it back has its own media directory.
     */
    val packages: MutableMap<String, MutableMap<String, String>> = mutableMapOf(),
    var failWrites: Boolean = false,
    var failReads: Boolean = false,
) : PackageStore {

    override suspend fun write(destination: String, build: suspend (ArchiveSink) -> Unit): Boolean {
        if (failWrites) return false
        val entries = mutableMapOf<String, String>()
        build(
            object : ArchiveSink {
                override suspend fun putText(name: String, text: String) {
                    entries[name] = text
                }

                override suspend fun putFile(name: String, absolutePath: String): Boolean {
                    val bytes = mediaFiles.contentAt(absolutePath) ?: return false
                    entries[name] = bytes
                    return true
                }
            },
        )
        packages[destination] = entries
        return true
    }

    override suspend fun read(source: String, read: suspend (ArchiveSource) -> Unit): Boolean {
        if (failReads) return false
        val entries = packages[source] ?: return false
        read(
            object : ArchiveSource {
                override suspend fun readText(name: String): String? = entries[name]

                override suspend fun namesUnder(prefix: String): List<String> =
                    entries.keys.filter { it.startsWith(prefix) }.sorted()

                override suspend fun copyTo(name: String, absolutePath: String): Boolean {
                    val content = entries[name] ?: return false
                    mediaFiles.put(absolutePath, content)
                    return true
                }
            },
        )
        return true
    }

    /** Lets a test pretend a package was cut short in transit, or tampered with. */
    fun corruptContent(destination: String, replacement: String) {
        packages[destination]?.put("content.json", replacement)
    }
}

class FakeMediaFiles : MediaFiles {

    private val files = mutableMapOf<String, String>()

    override fun absolutePath(relativePath: String): String = "/fake/$relativePath"

    override fun exists(relativePath: String): Boolean = absolutePath(relativePath) in files

    override fun sizeBytes(relativePath: String): Long =
        files[absolutePath(relativePath)]?.length?.toLong() ?: 0L

    override fun delete(relativePath: String): Boolean =
        files.remove(absolutePath(relativePath)) != null

    override fun deleteAll(): Boolean {
        files.clear()
        return true
    }

    fun put(absolutePath: String, content: String) {
        files[absolutePath] = content
    }

    fun putRelative(relativePath: String, content: String) {
        files[absolutePath(relativePath)] = content
    }

    fun contentAt(absolutePath: String): String? = files[absolutePath]

    fun contentOf(relativePath: String): String? = files[absolutePath(relativePath)]

    val count: Int get() = files.size
}

/**
 * Not SHA-256, but a function of the whole input with the same property that matters here: change
 * one character and the digest changes. The real one is the platform's.
 */
class FakeDigest : Digest {
    override fun sha256(text: String): String = "len${text.length}:${text.hashCode()}"
}
