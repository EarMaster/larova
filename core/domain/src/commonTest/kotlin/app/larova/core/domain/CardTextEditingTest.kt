package app.larova.core.domain

import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.CheckItem
import app.larova.core.domain.model.PhoneEntry
import app.larova.core.domain.model.Step
import app.larova.core.domain.model.textFieldsOf
import app.larova.core.domain.model.withTextFields
import app.larova.core.domain.usecase.DeleteCardText
import app.larova.core.domain.usecase.SaveCardText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest

/**
 * Writing one tile's text in another language.
 *
 * The refusals are the point. A variant with no title is an unreadable square on the grid, and one
 * whose payload is a different kind of tile is the half-translated tile the whole design exists to
 * prevent — it would pass every decode and fail on the screen of the person who opened it.
 */
@OptIn(ExperimentalUuidApi::class)
class CardTextEditingTest {

    private val cardId = Uuid.parse("11111111-1111-1111-1111-111111111111")

    @Test
    fun aTranslationIsStoredUnderItsCanonicalTag() = runTest {
        val cards = FakeCardRepository(listOf(guide()))
        val texts = FakeCardTextRepository()

        val result = SaveCardText(cards, texts)(
            cardId = cardId,
            lang = "TR",
            title = "  Diş fırçalama  ",
            subtitle = null,
            payload = guidePayload,
        )

        assertIs<SaveCardText.Result.Saved>(result)
        val stored = texts.all().single()
        assertEquals("tr", stored.lang)
        assertEquals("Diş fırçalama", stored.title)
    }

    @Test
    fun aTranslationWithNoTitleIsRefused() = runTest {
        val result = SaveCardText(FakeCardRepository(listOf(guide())), FakeCardTextRepository())(
            cardId = cardId,
            lang = "tr",
            title = "   ",
            subtitle = null,
            payload = guidePayload,
        )

        assertIs<SaveCardText.Result.TitleMissing>(result)
    }

    @Test
    fun somethingThatIsNotALanguageIsRefused() = runTest {
        val result = SaveCardText(FakeCardRepository(listOf(guide())), FakeCardTextRepository())(
            cardId = cardId,
            lang = "de_DE",
            title = "Zähneputzen",
            subtitle = null,
            payload = guidePayload,
        )

        assertIs<SaveCardText.Result.LanguageMissing>(result)
    }

    /** What "never a half-translated tile" means at the byte level. */
    @Test
    fun aTranslationThatIsADifferentKindOfTileIsRefused() = runTest {
        val texts = FakeCardTextRepository()

        val result = SaveCardText(FakeCardRepository(listOf(guide())), texts)(
            cardId = cardId,
            lang = "tr",
            title = "Diş fırçalama",
            subtitle = null,
            payload = CardPayloadCodec.encode(CardPayload.Note("bir not")),
        )

        assertIs<SaveCardText.Result.WrongPayloadType>(result)
        assertTrue(texts.all().isEmpty())
    }

    @Test
    fun removingALanguageLeavesTheOthersAlone() = runTest {
        val cards = FakeCardRepository(listOf(guide()))
        val texts = FakeCardTextRepository()
        val save = SaveCardText(cards, texts)
        save(cardId, "tr", "Diş fırçalama", null, guidePayload)
        save(cardId, "uk", "Чищення зубів", null, guidePayload)

        DeleteCardText(texts)(cardId, "TR")

        assertEquals(listOf("uk"), texts.all().map { it.lang })
    }

    /**
     * The words go in and out in the same order, and everything else survives untouched.
     *
     * This pair is what makes the "same kind of tile" invariant structural rather than checked: a
     * variant is built by putting words back into the original's own structure, so the pictures a
     * guide points at and the numbers on a call tile cannot be lost by typing.
     */
    @Test
    fun onlyTheWordsChange() {
        val picture = Uuid.parse("22222222-2222-2222-2222-222222222222")
        val payload = CardPayload.Guide(
            listOf(Step("Teeth first.", mediaId = picture), Step("Then the story.")),
        )

        assertEquals(listOf("Teeth first.", "Then the story."), textFieldsOf(payload))

        val translated = withTextFields(payload, listOf("Önce dişler.", "Sonra masal."))

        assertIs<CardPayload.Guide>(translated)
        assertEquals(listOf("Önce dişler.", "Sonra masal."), translated.steps.map { it.text })
        // The picture is still on the step it was on.
        assertEquals(picture, translated.steps.first().mediaId)
    }

    @Test
    fun aCallTileKeepsItsNumbersWhateverIsTypedIntoTheWords() {
        val payload = CardPayload.Phone(
            contacts = listOf(PhoneEntry("Anna", "+49 30 123456", "Mum")),
        )

        val translated = withTextFields(payload, listOf("Anna", "Anne"))

        assertIs<CardPayload.Phone>(translated)
        assertEquals("+49 30 123456", translated.people.single().number)
        assertEquals("Anne", translated.people.single().relation)
    }

    /** A table translated into fewer cells would be a table of a different shape. */
    @Test
    fun aWrongNumberOfWordsChangesNothing() {
        val payload = CardPayload.Table(
            columns = listOf("When", "What"),
            rows = listOf(listOf("Morning", "One spoon")),
        )

        assertEquals(4, textFieldsOf(payload).size)
        assertEquals(payload, withTextFields(payload, listOf("Ne zaman")))
    }

    @Test
    fun aChecklistKeepsThisMorningsTicks() {
        val payload = CardPayload.Checklist(
            items = listOf(CheckItem("Wash", done = true), CheckItem("Dress")),
        )

        val translated = withTextFields(payload, listOf("Yıkan", "Giyin"))

        assertIs<CardPayload.Checklist>(translated)
        assertTrue(translated.items.first().done)
    }

    private val guidePayload = CardPayloadCodec.encode(
        CardPayload.Guide(listOf(Step("Teeth first."))),
    )

    private fun guide() = Card(
        id = cardId,
        boardId = Uuid.parse("33333333-3333-3333-3333-333333333333"),
        title = "Bedtime",
        icon = "moon",
        colorToken = "sage",
        sortIndex = 0,
        type = CardType.GUIDE,
        payload = guidePayload,
        updatedAt = Instant.parse("2026-03-07T19:00:00Z"),
    )
}
