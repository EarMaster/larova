package app.larova.core.domain

import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.CheckItem
import app.larova.core.domain.model.Step
import app.larova.core.domain.model.cardType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Payload JSON is an export-file format, not an internal detail. Everything asserted here is a
 * promise to a file that may be the only copy of a family's content.
 */
@OptIn(ExperimentalUuidApi::class)
class CardPayloadCodecTest {

    private val media = Uuid.parse("3f2a1b4c-5d6e-4f70-8192-a3b4c5d6e7f8")

    private val everyType: List<CardPayload> = listOf(
        CardPayload.Guide(listOf(Step("Brush teeth", mediaId = media, audioId = null))),
        CardPayload.Note("Anna sleeps with the small lamp on."),
        CardPayload.Checklist(listOf(CheckItem("Pyjamas", done = true)), resetDaily = true),
        CardPayload.Table(listOf("Day", "Pick-up"), listOf(listOf("Monday", "16:00"))),
        CardPayload.Video(media, caption = "Bedtime song"),
        CardPayload.Audio(media),
        CardPayload.Phone("Grandma", "+49 170 1234567", relation = "Mother of the mother"),
        CardPayload.Web("https://example.org", label = "The nursery"),
        CardPayload.AppLink("com.example.messenger", "Messenger"),
        CardPayload.Folder(media),
    )

    @Test
    fun everyPayloadTypeSurvivesARoundTrip() {
        for (payload in everyType) {
            val decoded = CardPayloadCodec.decodeOrNull(CardPayloadCodec.encode(payload))
            assertEquals(payload, decoded, "${payload.cardType.key} did not round trip")
        }
    }

    @Test
    fun everyCardTypeHasAPayload() {
        // A type with no payload variant would be a tile that can be listed but never opened.
        assertEquals(CardType.entries.toSet(), everyType.map { it.cardType }.toSet())
    }

    @Test
    fun theDiscriminatorMatchesTheCardTypeKey() {
        // The database column and the JSON discriminator have to agree, or an import can write a
        // row whose type says one thing and whose payload says another.
        for (payload in everyType) {
            val json = CardPayloadCodec.encode(payload)
            assertTrue(
                json.contains("\"${CardPayloadCodec.TYPE_DISCRIMINATOR}\":\"${payload.cardType.key}\""),
                "encoded ${payload.cardType} does not carry its own key: $json",
            )
        }
    }

    @Test
    fun anUnknownTypeIsSkippedRatherThanFatal() {
        // What an export from a newer Larova looks like from here. The tile is dropped; the rest of
        // the file still imports.
        assertNull(CardPayloadCodec.decodeOrNull("""{"type":"hologram","message":"hello"}"""))
    }

    @Test
    fun malformedJsonIsSkippedRatherThanFatal() {
        assertNull(CardPayloadCodec.decodeOrNull("not json at all"))
        assertNull(CardPayloadCodec.decodeOrNull(""))
        assertNull(CardPayloadCodec.decodeOrNull("""{"no":"type"}"""))
    }

    @Test
    fun anUnknownFieldOnAKnownTypeIsIgnored() {
        // A payload that gained a field in a later version still loads here, minus the field.
        val decoded = CardPayloadCodec.decodeOrNull(
            """{"type":"note","text":"Keep this","readAloudVoice":"grandma"}""",
        )
        assertEquals(CardPayload.Note("Keep this"), decoded)
    }

    @Test
    fun defaultsAreWrittenOutRatherThanImplied() {
        // So that changing a default later cannot silently change what an old file means.
        val json = CardPayloadCodec.encode(CardPayload.Checklist(listOf(CheckItem("Teeth"))))
        assertTrue(json.contains("\"resetDaily\":false"), json)
        assertTrue(json.contains("\"done\":false"), json)
    }

    @Test
    fun optionalReferencesRoundTripAsNull() {
        val decoded = CardPayloadCodec.decodeOrNull(
            CardPayloadCodec.encode(CardPayload.Guide(listOf(Step("Just text")))),
        )
        assertNotNull(decoded)
        val step = (decoded as CardPayload.Guide).steps.single()
        assertNull(step.mediaId)
        assertNull(step.audioId)
    }
}
