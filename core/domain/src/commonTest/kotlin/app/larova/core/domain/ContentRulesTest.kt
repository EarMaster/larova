package app.larova.core.domain

import app.larova.core.domain.model.MAX_TABLE_COLUMNS
import app.larova.core.domain.model.isOpenableUrl
import app.larova.core.domain.model.sanitizePhoneNumber
import app.larova.core.domain.model.movedTableColumn
import app.larova.core.domain.model.tableOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Both rules decide what Larova hands to another app. A tile's contents are typed by a parent,
 * but an import can come from anywhere, so this is the boundary where content stops being trusted.
 */
class ContentRulesTest {

    @Test
    fun websiteTilesOpenWebsites() {
        assertTrue(isOpenableUrl("https://example.org"))
        assertTrue(isOpenableUrl("http://example.org/page?a=1"))
        assertTrue(isOpenableUrl("HTTPS://EXAMPLE.ORG"))
        assertTrue(isOpenableUrl("  https://example.org  "))
    }

    @Test
    fun anythingThatIsNotAWebsiteIsRefused() {
        // Intent would happily act on all of these. A website tile must only ever open a website.
        assertFalse(isOpenableUrl("javascript:alert(1)"))
        assertFalse(isOpenableUrl("file:///data/data/app.larova/databases/larova.db"))
        assertFalse(isOpenableUrl("content://media/external/images/media/1"))
        assertFalse(isOpenableUrl("intent://scan/#Intent;scheme=zxing;end"))
        assertFalse(isOpenableUrl("tel:+4917012345"))
        assertFalse(isOpenableUrl("market://details?id=app.larova"))
        assertFalse(isOpenableUrl("example.org"))
        assertFalse(isOpenableUrl("https://"))
        assertFalse(isOpenableUrl(""))
        // A space is how a second target gets smuggled onto the end of the first.
        assertFalse(isOpenableUrl("https://example.org evil"))
    }

    @Test
    fun numbersKeepWhatADiallerNeeds() {
        assertEquals("+4917012345678", sanitizePhoneNumber("+49 170 1234 5678"))
        assertEquals("0301234567", sanitizePhoneNumber("(030) 123-4567"))
        assertEquals("+4930123", sanitizePhoneNumber(" +49 30 123 "))
        assertEquals("*100#", sanitizePhoneNumber("*100#"))
    }

    @Test
    fun aPlusOnlyCountsAtTheFront() {
        // Otherwise a typo in the middle would silently change which number gets dialled.
        assertEquals("4917012345", sanitizePhoneNumber("49+170 12345"))
        assertEquals("", sanitizePhoneNumber("+"))
        assertEquals("", sanitizePhoneNumber("call grandma"))
    }

    /**
     * A table is read by position, so a row that is short by one cell would put the last value
     * under the wrong heading. On a tile that says "Time | What" that is not a cosmetic problem,
     * and the row can arrive short from an import as easily as from an editor.
     */
    @Test
    fun aTableIsSquare() {
        val table = tableOf(
            columns = listOf("Time", "What"),
            rows = listOf(
                listOf("18:00"),
                listOf("19:00", "Bath", "left over from a third column"),
            ),
        )

        assertEquals(listOf("Time", "What"), table.columns)
        assertEquals(listOf(listOf("18:00", ""), listOf("19:00", "Bath")), table.rows)
    }

    @Test
    fun aTableDropsTheRowsNobodyFilledIn() {
        val table = tableOf(
            columns = listOf(" Time ", "What"),
            rows = listOf(listOf("", ""), listOf(" 18:00 ", ""), listOf("  ", "   ")),
        )

        // Headings and cells are trimmed; a row with one value in it stays, blank cell and all.
        assertEquals(listOf("Time", "What"), table.columns)
        assertEquals(listOf(listOf("18:00", "")), table.rows)
    }

    @Test
    fun aTableStopsAtFourColumns() {
        val table = tableOf(
            columns = listOf("a", "b", "c", "d", "e"),
            rows = listOf(listOf("1", "2", "3", "4", "5")),
        )

        assertEquals(MAX_TABLE_COLUMNS, table.columns.size)
        assertEquals(listOf(listOf("1", "2", "3", "4")), table.rows)
    }

    /** No columns means nothing to put a value under, so there is no table to keep. */
    @Test
    fun aTableWithNoHeadingsIsEmpty() {
        val table = tableOf(columns = emptyList(), rows = listOf(listOf("orphaned")))

        assertEquals(emptyList(), table.columns)
        assertEquals(emptyList(), table.rows)
    }

    @Test
    fun movingAColumnTakesEveryValueUnderItAlong() {
        // The whole reason this is not a swap in one list. Moving the heading alone would leave a
        // table that still looks right and says something different about every row in it.
        val moved = movedTableColumn(
            columns = listOf("Day", "Time", "Dose"),
            rows = listOf(
                listOf("Mon", "08:00", "5 ml"),
                listOf("Tue", "09:00", "10 ml"),
            ),
            from = 1,
            to = 2,
        )

        assertEquals(listOf("Day", "Dose", "Time"), moved.columns)
        assertEquals(listOf("Mon", "5 ml", "08:00"), moved.rows[0])
        assertEquals(listOf("Tue", "10 ml", "09:00"), moved.rows[1])
    }

    @Test
    fun aColumnMoveThatGoesNowhereChangesNothing() {
        val columns = listOf("Day", "Time")
        val rows = listOf(listOf("Mon", "08:00"))

        for (to in listOf(-1, 0, 2)) {
            val moved = movedTableColumn(columns, rows, from = 0, to = to)
            assertEquals(columns, moved.columns)
            assertEquals(rows, moved.rows)
        }
    }

    @Test
    fun aRowTooShortForTheMoveIsLeftAsItIs() {
        // Squaring a table is `tableOf`'s job on the way to being stored. A short row here came
        // from a file rather than from the editor, and padding it would hide that.
        val moved = movedTableColumn(
            columns = listOf("Day", "Time", "Dose"),
            rows = listOf(listOf("Mon", "08:00", "5 ml"), listOf("Tue")),
            from = 0,
            to = 2,
        )

        assertEquals(listOf("5 ml", "08:00", "Mon"), moved.rows[0])
        assertEquals(listOf("Tue"), moved.rows[1])
    }

}
