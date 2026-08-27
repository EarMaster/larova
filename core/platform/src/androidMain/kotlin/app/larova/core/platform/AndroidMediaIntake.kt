package app.larova.core.platform

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import app.larova.core.domain.export.MediaFiles
import app.larova.core.domain.media.MediaIntake
import app.larova.core.domain.model.MediaAsset
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * A picked video or recording, copied into app-private storage as it is.
 *
 * Streamed rather than read into memory. A family's holiday video does not fit in a `ByteArray` on
 * a phone that is already low on space, and the whole point of this class is that it never has to.
 *
 * The extension comes from the MIME type the provider reports rather than from the name it happens
 * to have, because a file arriving from a messenger often has neither. The MIME type is what the
 * player will be handed, and getting that wrong is what makes a recording refuse to play.
 */
@OptIn(ExperimentalUuidApi::class)
class AndroidMediaIntake(
    private val context: Context,
    private val mediaFiles: MediaFiles,
) : MediaIntake {

    override suspend fun copyIn(source: String): MediaAsset? = withContext(Dispatchers.IO) {
        val uri = source.toUriOrNull() ?: return@withContext null
        val mimeType = context.contentResolver.getType(uri) ?: FALLBACK_MIME
        val relativePath = PlatformNames.MEDIA_DIRECTORY + "/" + Uuid.random() + "." +
            extensionFor(mimeType)
        val file = File(mediaFiles.absolutePath(relativePath))

        val copied = try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().buffered().use { output -> input.copyTo(output) }
                true
            } ?: false
        } catch (_: IOException) {
            false
        } catch (_: SecurityException) {
            // The picker's grant is temporary and can be gone by the time it is used.
            false
        }

        if (!copied) {
            // Half a video is worse than none: it would sit on a tile looking playable and travel
            // into the next backup counted as content.
            file.delete()
            return@withContext null
        }

        MediaAsset(
            id = Uuid.parse(file.nameWithoutExtension),
            relativePath = relativePath,
            mimeType = mimeType,
            sizeBytes = file.length(),
            sha256 = file.sha256(),
        )
    }

    /** `mp4` for a video whose type the system cannot name; the player sniffs the container anyway. */
    private fun extensionFor(mimeType: String): String =
        MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
            ?: if (mimeType.startsWith("audio/")) FALLBACK_AUDIO_EXTENSION else FALLBACK_EXTENSION

    private fun String.toUriOrNull(): Uri? = try {
        Uri.parse(this)
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun File.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        inputStream().buffered().use { input ->
            val buffer = ByteArray(BUFFER_BYTES)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { byte ->
            byte.toInt().and(BYTE_MASK).toString(RADIX_HEX).padStart(HEX_DIGITS, '0')
        }
    }

    private companion object {
        const val FALLBACK_MIME = "application/octet-stream"
        const val FALLBACK_EXTENSION = "mp4"
        const val FALLBACK_AUDIO_EXTENSION = "m4a"
        const val BUFFER_BYTES = 8 * 1024
        const val RADIX_HEX = 16
        const val BYTE_MASK = 0xFF
        const val HEX_DIGITS = 2
    }
}
