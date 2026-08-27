package app.larova.core.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import app.larova.core.domain.export.MediaFiles
import app.larova.core.domain.media.ImageSize
import app.larova.core.domain.media.ImageStore
import app.larova.core.domain.model.MediaAsset
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Pictures, through `BitmapFactory` and nothing else.
 *
 * A photo arrives as a `content://` URI the picker granted temporary read access to, and leaves
 * this class as a file in app-private storage at 2048px and JPEG q85 — the invariant in
 * `AGENTS.md`, and the reason a family's backup stays a file they can actually send somewhere.
 *
 * Three details are not obvious and all three are load-bearing:
 *
 * The picture is decoded in two passes. The first reads only its dimensions, which is what lets the
 * second ask for a sampled decode — a fifty-megapixel photo decoded at full size is hundreds of
 * megabytes of bitmap and an out-of-memory kill on the phone of whoever picked it.
 *
 * Orientation is applied here rather than carried. Phones store a portrait photo as a landscape
 * bitmap plus an EXIF rotation flag, and re-encoding without applying that flag is how a bedtime
 * guide ends up sideways. The stored file is upright, so nothing downstream — the export, the
 * import, a future iOS renderer — has to know EXIF exists.
 *
 * The hash is over the file that was written, not the file that was picked. It is what an import
 * checks a restored picture against, and the two are not the same bytes.
 */
@OptIn(ExperimentalUuidApi::class)
class AndroidImageStore(
    private val context: Context,
    private val mediaFiles: MediaFiles,
) : ImageStore {

    override suspend fun store(source: String): MediaAsset? = withContext(Dispatchers.IO) {
        val uri = source.toUriOrNull() ?: return@withContext null
        val bitmap = decodeFitted(uri) ?: return@withContext null

        val id = Uuid.random()
        val relativePath = PlatformNames.MEDIA_DIRECTORY + "/" + id + "." + EXTENSION
        val file = File(mediaFiles.absolutePath(relativePath))
        val written = try {
            file.outputStream().buffered()
                .use { bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, it) }
        } catch (_: IOException) {
            false
        } finally {
            bitmap.recycle()
        }

        if (!written) {
            // A half-written file is worse than none: it would show as a torn picture on a guide
            // step and travel into the next backup looking complete.
            file.delete()
            return@withContext null
        }

        MediaAsset(
            id = id,
            relativePath = relativePath,
            mimeType = MIME_TYPE,
            sizeBytes = file.length(),
            sha256 = file.sha256(),
        )
    }

    override suspend fun read(relativePath: String, maxEdge: Int): ByteArray? =
        withContext(Dispatchers.IO) {
            val file = File(mediaFiles.absolutePath(relativePath))
            if (!file.isFile) return@withContext null

            val bounds = boundsOf(file) ?: return@withContext null
            // Already small enough: hand back the bytes on disk rather than re-encoding them, which
            // would cost quality for nothing.
            if (bounds.longEdge <= maxEdge) return@withContext file.readBytesOrNull()

            val decoded = decodeSampled(file, bounds, maxEdge) ?: return@withContext null
            val fitted = decoded.fitted(maxEdge, rotationDegrees = 0)
            try {
                ByteArrayOutputStream().use { out ->
                    if (fitted.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)) {
                        out.toByteArray()
                    } else {
                        null
                    }
                }
            } finally {
                fitted.recycle()
            }
        }

    /** The picked photo, sampled down, turned upright, and cut to the stored size. */
    private fun decodeFitted(uri: Uri): Bitmap? {
        val bounds = boundsOf(uri) ?: return null
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.longEdge, ImageSize.STORED)
        }
        val decoded = openStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return null
        return decoded.fitted(ImageSize.STORED, rotationDegrees = rotationOf(uri))
    }

    private fun boundsOf(uri: Uri): Bounds? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        openStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) } ?: return null
        return Bounds.of(options)
    }

    private fun boundsOf(file: File): Bounds? {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        return Bounds.of(options)
    }

    private fun decodeSampled(file: File, bounds: Bounds, maxEdge: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSizeFor(bounds.longEdge, maxEdge)
        }
        return BitmapFactory.decodeFile(file.absolutePath, options)
    }

    /** How far the phone recorded this photo as being turned. Unreadable EXIF means upright. */
    private fun rotationOf(uri: Uri): Int = try {
        openStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
    } catch (_: IOException) {
        0
    }

    private fun openStream(uri: Uri) = try {
        context.contentResolver.openInputStream(uri)
    } catch (_: IOException) {
        null
    } catch (_: SecurityException) {
        // The picker's grant is temporary and can be gone by the time it is used.
        null
    }

    private fun String.toUriOrNull(): Uri? = try {
        Uri.parse(this)
    } catch (_: IllegalArgumentException) {
        null
    }

    private fun File.readBytesOrNull(): ByteArray? = try {
        readBytes()
    } catch (_: IOException) {
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

    /** What a decode found before any of it was read into memory. */
    private data class Bounds(val width: Int, val height: Int) {

        val longEdge: Int get() = maxOf(width, height)

        companion object {
            /** Null for a file that is not an image, or one this device has no decoder for. */
            fun of(options: BitmapFactory.Options): Bounds? =
                if (options.outWidth > 0 && options.outHeight > 0) {
                    Bounds(options.outWidth, options.outHeight)
                } else {
                    null
                }
        }
    }

    private companion object {
        const val EXTENSION = "jpg"
        const val MIME_TYPE = "image/jpeg"

        /** q85: where a photograph stops getting visibly better and only gets larger. */
        const val QUALITY = 85

        const val BUFFER_BYTES = 8 * 1024
        const val RADIX_HEX = 16
        const val BYTE_MASK = 0xFF
        const val HEX_DIGITS = 2
    }
}

/**
 * The power of two to decode at.
 *
 * Halving is all `BitmapFactory` offers, so this lands on the smallest bitmap that is still at
 * least as large as what is wanted, and the exact size is cut from it afterwards. Sampling below
 * the target and scaling back up would be visibly soft.
 */
private fun sampleSizeFor(longEdge: Int, maxEdge: Int): Int {
    var sample = 1
    var edge = longEdge
    while (edge / 2 >= maxEdge) {
        edge /= 2
        sample *= 2
    }
    return sample
}

/**
 * Scaled to fit [maxEdge] and turned upright, in one pass.
 *
 * One matrix rather than two operations: rotating and then scaling allocates a second full-size
 * bitmap, and that is the allocation that fails on the phone this is running on.
 */
private fun Bitmap.fitted(maxEdge: Int, rotationDegrees: Int): Bitmap {
    val longEdge = maxOf(width, height)
    val scale = if (longEdge > maxEdge) maxEdge.toFloat() / longEdge else 1f
    if (scale == 1f && rotationDegrees == 0) return this

    val matrix = Matrix().apply {
        if (scale != 1f) postScale(scale, scale)
        if (rotationDegrees != 0) postRotate(rotationDegrees.toFloat())
    }
    val result = Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    if (result !== this) recycle()
    return result
}
