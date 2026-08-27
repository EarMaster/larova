package app.larova.core.domain.model

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * The single place a payload is turned into JSON and back.
 *
 * Two settings here are compatibility decisions, not preferences:
 *
 * `ignoreUnknownKeys` means a payload that gained a field in a later version still loads in this
 * one, minus the field it does not know about. `decodeOrNull` returning null for an unrecognised
 * type is the other half of the same promise: an import from a newer Larova skips the tiles it
 * cannot render instead of refusing the whole file. That file may be the only copy of a family's
 * content, so "partly readable" has to beat "rejected".
 *
 * `encodeDefaults` is on so that a payload written today still says what it means when a later
 * version changes a default.
 */
object CardPayloadCodec {

    val json: Json = Json {
        classDiscriminator = TYPE_DISCRIMINATOR
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    fun encode(payload: CardPayload): String = json.encodeToString(payload)

    /**
     * Null when the payload cannot be read here: an unknown type, or malformed JSON. The caller
     * skips that tile and keeps the rest — never substitutes a different type.
     */
    fun decodeOrNull(raw: String): CardPayload? = try {
        json.decodeFromString<CardPayload>(raw)
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    const val TYPE_DISCRIMINATOR = "type"
}
