package app.larova.core.domain

import app.larova.core.domain.export.Encryption
import app.larova.core.domain.export.ExportCodec
import app.larova.core.domain.export.ExportContent
import app.larova.core.domain.export.ExportCounts
import app.larova.core.domain.export.ExportManifest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class ExportManifestTest {

    /**
     * `content.json` written before the log existed has no `log` key at all. It has to read as an
     * empty log rather than as a damaged file: the field was declared with a default from the first
     * release for exactly this reason, which is why filling it in M2 is not a format change and
     * needs no `schemaVersion` bump.
     */
    @Test
    fun contentWrittenBeforeTheLogExistedStillReads() {
        val content = ExportCodec.json.decodeFromString<ExportContent>(
            """{"boards":[],"cards":[],"media":[]}""",
        )

        assertEquals(emptyList(), content.log)
    }

    private fun manifest(schemaVersion: Int = ExportManifest.CURRENT_SCHEMA_VERSION) = ExportManifest(
        schemaVersion = schemaVersion,
        appVersion = "0.1.0",
        exportedAt = Instant.parse("2026-08-23T18:12:00Z"),
        label = "Larova for Jonas",
        counts = ExportCounts(boards = 2, cards = 14, media = 9),
        contentSha256 = "0".repeat(64),
    )

    @Test
    fun theFirstSchemaVersionIsOne() {
        // The anchor every later migration is measured from. It is in the file from the first
        // export precisely so that this number never has to be guessed.
        assertEquals(1, ExportManifest.CURRENT_SCHEMA_VERSION)
        assertEquals(1, manifest().schemaVersion)
    }

    @Test
    fun aNewerFileIsDeclinedRatherThanGuessedAt() {
        assertFalse(manifest(schemaVersion = 2).isReadable)
        assertTrue(manifest().isReadable)
        assertTrue(manifest(schemaVersion = 0).isReadable)
    }

    @Test
    fun theManifestKeepsItsWireNames() {
        // The preview an import shows before a password is typed reads these fields, so their
        // names are part of the format rather than an implementation detail.
        val encoded = ExportCodec.encode(manifest())
        for (field in listOf(
            "schemaVersion",
            "appVersion",
            "exportedAt",
            "label",
            "counts",
            "encryption",
            "contentSha256",
        )) {
            assertTrue(encoded.contains("\"$field\""), "manifest lost the $field field: $encoded")
        }
        // Both of these carry a default, and a default that is not written is a field a later
        // version cannot find. That is what ExportCodec exists to prevent.
        assertTrue(encoded.contains("\"schemaVersion\": 1"), encoded)
        assertTrue(encoded.contains("\"encryption\": \"none\""), encoded)
        assertTrue(encoded.contains("\"exportedAt\": \"2026-08-23T18:12:00Z\""), encoded)
    }

    @Test
    fun encryptionNamesAreLowercaseOnTheWire() {
        assertEquals("\"none\"", ExportCodec.json.encodeToString(Encryption.NONE))
        assertEquals("\"aes-256-gcm\"", ExportCodec.json.encodeToString(Encryption.AES_256_GCM))
    }

    @Test
    fun anOlderManifestWithoutOptionalFieldsStillReads() {
        val decoded = ExportCodec.decodeManifestOrNull(
            """
            {"schemaVersion":1,"appVersion":"0.1.0","exportedAt":"2026-08-23T18:12:00Z",
             "counts":{"boards":1,"cards":3,"media":0},"contentSha256":"abc"}
            """.trimIndent(),
        )
        assertNotNull(decoded)
        assertEquals(null, decoded.label)
        assertEquals(Encryption.NONE, decoded.encryption)
        assertTrue(decoded.isReadable)
    }

    @Test
    fun theContainerNamesAreFixed() {
        assertEquals("larova", ExportManifest.FILE_EXTENSION)
        assertEquals("manifest.json", ExportManifest.MANIFEST_ENTRY)
        assertEquals("content.json", ExportManifest.CONTENT_ENTRY)
        assertEquals("media/", ExportManifest.MEDIA_DIRECTORY)
    }

    @Test
    fun somethingThatIsNotAPackageIsRefusedRatherThanCrashing() {
        assertNull(ExportCodec.decodeManifestOrNull("a photo, probably"))
        assertNull(ExportCodec.decodeManifestOrNull(""))
    }
}
