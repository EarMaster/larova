package app.larova.core.domain.export

import app.larova.core.domain.serialization.InstantSerializer
import kotlin.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The manifest of an export package (docs/technical-notes.md §6).
 *
 * It stays in the clear even when the rest of the package is encrypted, because the import preview
 * — "12 tiles, 4 videos, made on the 3rd" — has to be readable before anyone types a password.
 *
 * [schemaVersion] is the migration anchor and is present from the very first export. Import checks
 * it before anything else and declines a newer version politely rather than guessing.
 */
@Serializable
data class ExportManifest(
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION,
    val appVersion: String,
    @Serializable(with = InstantSerializer::class) val exportedAt: Instant,
    /** Free text the user chose, e.g. "Larova for Jonas". Never derived from content. */
    val label: String? = null,
    val counts: ExportCounts,
    val encryption: Encryption = Encryption.NONE,
    /** SHA-256 of content.json, so an import can tell truncation from tampering-free transfer. */
    val contentSha256: String,
) {
    /**
     * Whether this build can read the package. A file from the future is refused with an
     * explanation; a file from the past is migrated forward.
     */
    val isReadable: Boolean get() = schemaVersion <= CURRENT_SCHEMA_VERSION

    companion object {
        /**
         * Raise this only together with a migration path, and never without checking that an
         * export written by the previous version still imports. A broken export format is the one
         * regression that cannot be walked back: that file may be a family's only copy.
         *
         * **1** — `0.1.0` to `0.4.2`. Tile types and log kinds were written as Kotlin constant
         * names (`"type": "GUIDE"`), because the container serialized the domain enums directly.
         *
         * **2** — they are written as the frozen keys (`"type": "guide"`), matching the database
         * column and the payload discriminator. The migration path is the reader: `ExportRows.kt`
         * accepts both spellings and always will, so every v1 file still imports. `LegacyPackageFixture`
         * is a real v1 `content.json` and the tests around it are what keep that true.
         *
         * The bump costs an older build no capability — it could not read a key-spelled file
         * either way. What it changes is what that build *says*: refused on the manifest with
         * "made with a newer version, update the app", instead of failing the decode after the
         * hash check and calling a healthy file incomplete.
         *
         * **3** — `content.json` gained a `cardText` block: whole-tile text variants, one row per
         * tile and language, each carrying title, second line and payload together.
         *
         * Unlike `log`, this field was not declared from the first release, and that is what makes
         * this bump mandatory rather than tidy. `ExportCodec.json` sets `ignoreUnknownKeys`, so
         * **without it a 0.5.x build would import a v3 package, pass the hash check, restore every
         * tile, report success — and drop every translation in the file without a word.** The
         * person handed that backup would never be told. Raising the number turns that into
         * `isReadable == false` and a sentence they can act on.
         *
         * Reading forward is untouched: a v1 or v2 file has no `cardText` key, the field defaults
         * to empty, and `contentWrittenBeforeTranslationsExistedStillReads` pins it.
         */
        const val CURRENT_SCHEMA_VERSION = 3

        /** The container's extension. Also what a MIME registration would later claim. */
        const val FILE_EXTENSION = "larova"

        const val MANIFEST_ENTRY = "manifest.json"
        const val CONTENT_ENTRY = "content.json"
        const val MEDIA_DIRECTORY = "media/"
    }
}

@Serializable
data class ExportCounts(val boards: Int, val cards: Int, val media: Int)

/**
 * The names are lowercase on the wire, as written in docs/technical-notes.md §6. They are part of
 * the file format, so they do not follow Kotlin's enum casing.
 */
@Serializable
enum class Encryption {
    /** Backups meant to stay on the device or in the user's own cloud. */
    @SerialName("none")
    NONE,

    /** AES-256-GCM with an Argon2id-derived key. Recommended whenever the file is sent onward. */
    @SerialName("aes-256-gcm")
    AES_256_GCM,
}
