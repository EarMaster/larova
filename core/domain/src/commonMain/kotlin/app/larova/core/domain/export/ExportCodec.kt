package app.larova.core.domain.export

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * The one configured `Json` for the export container, so no caller can write a package with a
 * different set of rules.
 *
 * `encodeDefaults` is the reason this object exists rather than a plain `Json.encodeToString` at
 * the call site. `schemaVersion` and `encryption` both have defaults, and a manifest written
 * without them is a file no future version can migrate — it would have to guess which format it
 * was looking at. The field being present is the entire point of having it.
 */
object ExportCodec {

    val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
        prettyPrintIndent = "  "
    }

    fun encode(manifest: ExportManifest): String = json.encodeToString(manifest)

    /**
     * Null when the manifest cannot be read at all — a file that is not a Larova package, or one
     * truncated in transit. A manifest that reads but is too new is a different case, and is
     * answered by [ExportManifest.isReadable] with an explanation the user can act on.
     */
    fun decodeManifestOrNull(raw: String): ExportManifest? = try {
        json.decodeFromString<ExportManifest>(raw)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
