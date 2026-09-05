package app.larova.core.domain

import app.larova.core.domain.export.ExportCodec
import app.larova.core.domain.export.ExportContent
import app.larova.core.domain.export.toExport
import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.LogKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * A backup written by a shipped version still reads after the format changed.
 *
 * This is the test the whole change exists for. [LEGACY_CONTENT_JSON] is a verbatim `content.json`
 * from the tree at `v0.4.2`, and because the export path did not change between `v0.1.0` and that
 * tag it stands in for every `.larova` file anyone has ever written. If this goes red, somebody's
 * only copy of their family's tiles has stopped opening.
 *
 * The whole-package version of this — manifest, hash, `ImportPackage`, `REPLACE` — lives in
 * `RoundTripTest`, where the `World` harness already is.
 */
class LegacyPackageTest {

    @Test
    fun aVersionOneFileDecodesWithEveryTileIntact() {
        val decoded = ExportCodec.decodeContentOrNull(LEGACY_CONTENT_JSON)

        assertNotNull(decoded, "a v1 file must still decode")
        assertEquals(LEGACY_CARD_TITLES, decoded.cards.map { it.title })
        assertEquals(0, decoded.skippedCards, "nothing in a v1 file is unknown to this build")
        assertEquals(0, decoded.skippedLogEntries)
        // A v1 file has no cardText key at all. Absent has to read as "no translations" rather
        // than as a damaged file — the field was declared with a default for exactly this.
        assertEquals(emptyList(), decoded.cardText)
        assertEquals(0, decoded.skippedCardText)
        assertEquals(2, decoded.boards.size)
        assertEquals(1, decoded.media.size)
    }

    /**
     * `APP_LINK` is the one that matters. Its key (`appLink`) differs from its constant name by
     * more than case, so a reader that cheated with `lowercase()` would pass on the other nine
     * types and fail only here — silently, on real files.
     */
    @Test
    fun theConstantNameSpellingResolvesForEveryType() {
        val decoded = assertNotNull(ExportCodec.decodeContentOrNull(LEGACY_CONTENT_JSON))
        val byTitle = decoded.cards.associateBy { it.title }

        assertEquals(CardType.APP_LINK, byTitle.getValue("Music").type)
        assertEquals(CardType.GUIDE, byTitle.getValue("Bedtime").type)
        assertEquals(CardType.NOTE, byTitle.getValue("Allergies").type)
        assertEquals(CardType.CHECKLIST, byTitle.getValue("Packing").type)
        assertEquals(CardType.TABLE, byTitle.getValue("Bus").type)
        assertEquals(CardType.PHONE, byTitle.getValue("Grandma").type)
        assertEquals(CardType.WEB, byTitle.getValue("Weather").type)
        assertEquals(CardType.FOLDER, byTitle.getValue("Mornings").type)
        assertEquals(LogKind.CARD_OPENED, decoded.log.first().kind)
        assertEquals(LogKind.CHECK_TOGGLED, decoded.log.last().kind)
    }

    /** Legacy in, modern out: the first backup made after updating is a clean v2 file. */
    @Test
    fun aFileImportedFromVersionOneIsRewrittenWithKeys() {
        val decoded = assertNotNull(ExportCodec.decodeContentOrNull(LEGACY_CONTENT_JSON))

        val rewritten = ExportCodec.json.encodeToString(
            ExportContent(
                boards = decoded.boards.map { it.toExport() },
                cards = decoded.cards.map { it.toExport() },
                media = decoded.media.map { it.toExport() },
                log = decoded.log.map { it.toExport() },
            ),
        )

        assertTrue(rewritten.contains("\"type\": \"appLink\""), rewritten)
        assertTrue(rewritten.contains("\"kind\": \"cardOpened\""), rewritten)
        assertTrue(!rewritten.contains("APP_LINK"), rewritten)
        assertTrue(!rewritten.contains("CARD_OPENED"), rewritten)

        // And reading our own output loses nothing.
        val again = assertNotNull(ExportCodec.decodeContentOrNull(rewritten))
        assertEquals(decoded.cards.map { it.title }, again.cards.map { it.title })
        assertEquals(decoded.cards.map { it.type }, again.cards.map { it.type })
        assertEquals(0, again.skippedCards)
        assertEquals(0, again.skippedLogEntries)
    }
}
