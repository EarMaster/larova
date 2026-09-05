package app.larova.core.domain

import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardText
import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.canonicalLanguageTag
import app.larova.core.domain.model.resolveCardText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Which text a caregiver is shown, and — the part that matters more — that they are always shown
 * something.
 *
 * There is no rule in here that hides a tile. A tile with no translation for the language somebody
 * asked for falls through to what the parent wrote, because a tile that disappeared because it
 * lacked a translation would be indistinguishable from a tile that never existed.
 */
@OptIn(ExperimentalUuidApi::class)
class CardTextResolutionTest {

    @Test
    fun theExactLanguageWinsOverOneThatIsMerelyClose() {
        val card = card()
        val variants = listOf(variant(card, "de"), variant(card, "de-AT"))

        assertEquals("de-AT", resolveCardText(card, variants, "de-AT").lang)
        assertEquals("de", resolveCardText(card, variants, "de").lang)
    }

    @Test
    fun aRegionIsDroppedRatherThanRefused() {
        val card = card()

        // Somebody reading European Portuguese is served by plain Portuguese, and the other way
        // round. The alternative is the original in a language they cannot read.
        assertEquals("pt", resolveCardText(card, listOf(variant(card, "pt")), "pt-PT").lang)
        assertEquals("pt-BR", resolveCardText(card, listOf(variant(card, "pt-BR")), "pt").lang)
    }

    /**
     * The same list in a different order must give the same answer, and so must the same list a
     * week later. A tie broken on "most recently edited" would change which language a caregiver
     * sees because somebody touched an unrelated translation.
     */
    @Test
    fun aTieIsBrokenTheSameWayEveryTime() {
        val card = card()
        val ordered = listOf(variant(card, "pt"), variant(card, "pt-BR"))

        // No region wins.
        assertEquals("pt", resolveCardText(card, ordered, "pt-PT").lang)
        assertEquals("pt", resolveCardText(card, ordered.reversed(), "pt-PT").lang)

        // Then alphabetically, and again regardless of order.
        val regionsOnly = listOf(variant(card, "pt-BR"), variant(card, "pt-AO"))
        assertEquals("pt-AO", resolveCardText(card, regionsOnly, "pt").lang)
        assertEquals("pt-AO", resolveCardText(card, regionsOnly.reversed(), "pt").lang)
    }

    /** Asking for the language the tile was written in means asking for the tile. */
    @Test
    fun theOriginalIsNeverAStaleTranslationOfItself() {
        val card = card(locale = "de")
        // Even with a German variant sitting there — which is a tile somebody translated into the
        // language it was already in, and the original still wins.
        val resolved = resolveCardText(card, listOf(variant(card, "de")), "de")

        assertNull(resolved.lang)
        assertEquals("Zähneputzen", resolved.title)
    }

    @Test
    fun aTileWithNothingInThatLanguageShowsWhatTheParentWrote() {
        val card = card()
        val resolved = resolveCardText(card, listOf(variant(card, "tr")), "uk")

        // Not the Turkish one, and not nothing: the original.
        assertNull(resolved.lang)
        assertEquals("Zähneputzen", resolved.title)
        assertFalse(resolved.possiblyOutOfDate)
    }

    @Test
    fun askingForNothingOrForNonsenseIsAskingForTheOriginal() {
        val card = card()
        val variants = listOf(variant(card, "tr"))

        for (asked in listOf(null, "", "   ", "de_DE", "english!", "1234", "-", "de-")) {
            assertNull(resolveCardText(card, variants, asked).lang, "asked for $asked")
        }
    }

    @Test
    fun aVariantBelongingToAnotherTileIsNeverShown() {
        val card = card()
        val other = card(id = "22222222-2222-2222-2222-222222222222")

        assertNull(resolveCardText(card, listOf(variant(other, "tr")), "tr").lang)
    }

    @Test
    fun theTileHavingBeenEditedSinceIsSaidRatherThanHidden() {
        val card = card()
        val stale = variant(card, "tr", updatedAt = Instant.parse("2026-03-01T08:00:00Z"))
        val fresh = variant(card, "tr", updatedAt = Instant.parse("2026-03-14T20:00:00Z"))

        // Shown either way — text nobody can read helps nobody — but said.
        assertTrue(resolveCardText(card, listOf(stale), "tr").possiblyOutOfDate)
        assertEquals("Diş fırçalama", resolveCardText(card, listOf(stale), "tr").title)

        assertFalse(resolveCardText(card, listOf(fresh), "tr").possiblyOutOfDate)
    }

    /** A restore writes both with the same timestamp. That is not somebody having edited one. */
    @Test
    fun anEqualTimestampIsNotOutOfDate() {
        val card = card()
        val same = variant(card, "tr", updatedAt = card.updatedAt)

        assertFalse(resolveCardText(card, listOf(same), "tr").possiblyOutOfDate)
    }

    @Test
    fun aTagIsStoredInOneShapeWhicheverWayItIsWritten() {
        assertEquals("de", canonicalLanguageTag("DE"))
        assertEquals("de", canonicalLanguageTag("  de  "))
        assertEquals("pt-PT", canonicalLanguageTag("PT-pt"))
        assertEquals("pt-PT", canonicalLanguageTag("pt-pt"))
        assertEquals("zh-Hans", canonicalLanguageTag("ZH-HANS"))
        assertEquals("zh-Hans-CN", canonicalLanguageTag("zh-hans-cn"))
    }

    @Test
    fun anythingThatIsNotATagIsRefusedRatherThanRepaired() {
        for (raw in listOf(null, "", "   ", "d", "de_DE", "de-", "-de", "123", "de-über")) {
            assertNull(canonicalLanguageTag(raw), "for $raw")
        }
    }

    /** Written the same on a Turkish phone as anywhere else — see the KDoc on the folding. */
    @Test
    fun theTurkishDotlessIDoesNotChangeATag() {
        assertEquals("id", canonicalLanguageTag("ID"))
        assertEquals("fil", canonicalLanguageTag("FIL"))
    }

    private fun card(
        id: String = "11111111-1111-1111-1111-111111111111",
        locale: String? = null,
    ) = Card(
        id = Uuid.parse(id),
        boardId = Uuid.parse("33333333-3333-3333-3333-333333333333"),
        title = "Zähneputzen",
        subtitle = "Jeden Abend",
        icon = "toothbrush",
        colorToken = "sage",
        sortIndex = 0,
        type = CardType.GUIDE,
        payload = """{"type":"guide","steps":[]}""",
        locale = locale,
        updatedAt = Instant.parse("2026-03-07T19:00:00Z"),
    )

    private fun variant(
        card: Card,
        lang: String,
        updatedAt: Instant = Instant.parse("2026-03-14T20:00:00Z"),
    ) = CardText(
        cardId = card.id,
        lang = lang,
        title = "Diş fırçalama",
        subtitle = "Her akşam",
        payload = """{"type":"guide","steps":[]}""",
        updatedAt = updatedAt,
    )
}
