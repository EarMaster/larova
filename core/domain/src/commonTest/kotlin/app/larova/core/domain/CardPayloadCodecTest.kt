package app.larova.core.domain

import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.CheckItem
import app.larova.core.domain.model.PhoneEntry
import app.larova.core.domain.model.Step
import app.larova.core.domain.model.phoneOf
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
        phoneOf(
            listOf(
                PhoneEntry("Grandma", "+49 170 1234567", "Mother of the mother", inHelpSheet = true),
                PhoneEntry("Dr Keller", "+49 30 7654321", "Paediatrician"),
            ),
        ),
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

    // ---- A call tile gained room for more than one person in 0.3.0. Both directions of that
    // change are a promise to a file somebody may be holding as their only copy.

    /**
     * A tile written by 0.2.1 or earlier: four flat fields and no list at all. It has to open here
     * as the one person it holds, not as an empty tile.
     */
    @Test
    fun aSingleContactTileFromAnOlderVersionStillReads() {
        val old = """{"type":"phone","displayName":"Grandma","number":"+49 170 1",""" +
            """"relation":"Mother of the mother","inHelpSheet":true}"""

        val decoded = CardPayloadCodec.decodeOrNull(old)

        assertNotNull(decoded)
        val person = (decoded as CardPayload.Phone).people.single()
        assertEquals("Grandma", person.displayName)
        assertEquals("+49 170 1", person.number)
        assertEquals("Mother of the mother", person.relation)
        assertTrue(person.inHelpSheet)
    }

    /**
     * The other direction, which is the one that cannot be fixed later: a tile written here has to
     * open in a version that has never heard of `contacts`. Such a version reads the flat fields
     * and ignores what it does not know, so the first person must be in both places. It sees one
     * number instead of three — losing two is a bad afternoon, where a tile that will not open is
     * a caregiver who cannot reach anybody.
     */
    @Test
    fun aMultiContactTileStillOpensInAVersionThatOnlyKnowsOne() {
        val payload = phoneOf(
            listOf(
                PhoneEntry("Grandma", "+49 170 1", "Mother of the mother", inHelpSheet = true),
                PhoneEntry("Dr Keller", "+49 30 2", "Paediatrician"),
                PhoneEntry("Frau Adler", "+49 170 3", "Next door"),
            ),
        )

        val json = CardPayloadCodec.encode(payload)

        assertTrue(json.contains(""""displayName":"Grandma""""), "the flat name is not written")
        assertTrue(json.contains(""""number":"+49 170 1""""), "the flat number is not written")
        assertTrue(json.contains(""""inHelpSheet":true"""), "the flat help flag is not written")
        // And everyone is still there for a version that does know about the list.
        val decoded = CardPayloadCodec.decodeOrNull(json) as? CardPayload.Phone
        assertNotNull(decoded)
        assertEquals(3, decoded.people.size)
        assertEquals(listOf("Grandma", "Dr Keller", "Frau Adler"), decoded.people.map { it.displayName })
    }

    /** Somebody added a row and typed a name into it, then saved. A row with nothing to dial. */
    @Test
    fun contactsWithNoNumberAreDropped() {
        val payload = phoneOf(
            listOf(
                PhoneEntry("Grandma", "+49 170 1"),
                PhoneEntry("Half-typed", "   "),
                PhoneEntry("", ""),
            ),
        )

        assertEquals(listOf("Grandma"), payload.people.map { it.displayName })
    }
}
