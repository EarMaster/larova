package app.larova.core.domain.export

import app.larova.core.domain.model.Board
import app.larova.core.domain.model.Card
import app.larova.core.domain.model.LogEntry
import app.larova.core.domain.model.MediaAsset
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

    /**
     * `content.json` as the app's own models, with a tally of what had to be left behind.
     *
     * Null means the same thing it means for [decodeManifestOrNull]: the JSON itself could not be
     * read. A row this build does not understand is *not* that — it is counted and dropped, because
     * one unfamiliar tile type used to fail the decode of the whole file and be reported to the
     * person restoring it as "this is not a Larova backup".
     */
    fun decodeContentOrNull(raw: String): DecodedContent? {
        val wire = try {
            json.decodeFromString<ExportContent>(raw)
        } catch (_: SerializationException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
        val cards = wire.cards.mapNotNull { it.toDomainOrNull() }
        val log = wire.log.mapNotNull { it.toDomainOrNull() }
        return DecodedContent(
            boards = wire.boards.map { it.toDomain() },
            cards = cards,
            media = wire.media.map { it.toDomain() },
            log = log,
            skippedCards = wire.cards.size - cards.size,
            skippedLogEntries = wire.log.size - log.size,
        )
    }
}

/**
 * What a package turned out to contain, once the rows this build understands are separated from the
 * ones it does not.
 *
 * The counts are carried rather than recomputed by the caller: the decode is the only place that
 * knows both numbers, and a tally reconstructed downstream would be a second source of truth.
 *
 * [skippedLogEntries] is counted because it is free and true, but nothing shows it. A log line is a
 * record of something that happened rather than something a family wrote, and the only sentence
 * worth putting on that screen is about tiles.
 */
data class DecodedContent(
    val boards: List<Board> = emptyList(),
    val cards: List<Card> = emptyList(),
    val media: List<MediaAsset> = emptyList(),
    val log: List<LogEntry> = emptyList(),
    val skippedCards: Int = 0,
    val skippedLogEntries: Int = 0,
)
