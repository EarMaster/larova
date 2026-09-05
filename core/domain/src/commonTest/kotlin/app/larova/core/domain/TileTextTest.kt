package app.larova.core.domain

import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CheckItem
import app.larova.core.domain.model.PhoneEntry
import app.larova.core.domain.model.Step
import app.larova.core.domain.model.plainTextOf
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * What leaves the phone when somebody taps Translate.
 *
 * The exclusions matter more than the inclusions here. A translator hands back Arabic-Indic digits
 * for a phone number and a percent-encoded address, so a number or a URL that went out and came
 * back would be a tile that quietly stops working — and neither is translatable in the first place.
 */
@OptIn(ExperimentalUuidApi::class)
class TileTextTest {

    @Test
    fun aGuideIsItsStepsWithABlankLineBetweenThem() {
        val text = plainTextOf(
            title = "Bedtime",
            subtitle = "Every evening",
            payload = CardPayload.Guide(
                listOf(Step("Teeth first."), Step("Then the story.")),
            ),
        )

        // Title, second line, then one block per step — so a step can be pasted back on its own.
        assertEquals("Bedtime\n\nEvery evening\n\nTeeth first.\n\nThen the story.", text)
    }

    @Test
    fun aChecklistIsOneItemPerLineAndSaysNothingAboutTheTicks() {
        val text = plainTextOf(
            title = "Morning",
            subtitle = null,
            payload = CardPayload.Checklist(
                items = listOf(CheckItem("Wash", done = true), CheckItem("Dress", done = false)),
                resetDaily = true,
            ),
        )

        // Whether the teeth are brushed is this morning's state, not something to translate.
        assertEquals("Morning\n\nWash\nDress", text)
    }

    @Test
    fun aTableKeepsItsShapeIncludingTheEmptyCells() {
        val text = plainTextOf(
            title = "Doses",
            subtitle = null,
            payload = CardPayload.Table(
                columns = listOf("When", "What"),
                rows = listOf(listOf("Morning", "One spoon"), listOf("Evening", "")),
            ),
        )

        // The bar left standing at the end of the last row is the empty cell. Keeping it is what
        // says the value belonged to the first column and not the second.
        assertEquals("Doses\n\nWhen | What\nMorning | One spoon\nEvening |", text)
    }

    @Test
    fun aNoteIsItsText() {
        assertEquals(
            "About Jonas\n\nHe needs the light left on.",
            plainTextOf("About Jonas", null, CardPayload.Note("He needs the light left on.")),
        )
    }

    @Test
    fun mediaIsItsCaptionAndNothingElse() {
        val id = Uuid.parse("11111111-1111-1111-1111-111111111111")

        assertEquals(
            "How to do it\n\nWatch this first.",
            plainTextOf("How to do it", null, CardPayload.Video(id, "Watch this first.")),
        )
        // No caption is not an empty line: a tile with only a title hands over only its title.
        assertEquals("Grandma", plainTextOf("Grandma", null, CardPayload.Audio(id, caption = null)))
    }

    @Test
    fun aCallTileHandsOverTheNamesAndNeverTheNumbers() {
        val text = plainTextOf(
            title = "Who to ring",
            subtitle = null,
            payload = CardPayload.Phone(
                contacts = listOf(
                    PhoneEntry(displayName = "Anna", number = "+49 30 123456", relation = "Mum"),
                    PhoneEntry(displayName = "Dr Weber", number = "030 998877", relation = "the practice"),
                ),
            ),
        )

        assertEquals("Who to ring\n\nAnna\nMum\n\nDr Weber\nthe practice", text)
        assertFalse(text.contains("123456"))
        assertFalse(text.contains("998877"))
    }

    @Test
    fun aWebsiteHandsOverWhatItIsForAndNeverTheAddress() {
        val text = plainTextOf(
            title = "The bus",
            subtitle = null,
            payload = CardPayload.Web(
                url = "https://bvg.de/line142",
                label = "Timetable",
                caption = "Line 142 from the corner",
            ),
        )

        assertEquals("The bus\n\nTimetable\n\nLine 142 from the corner", text)
        assertFalse(text.contains("bvg.de"))
        assertFalse(text.contains("https"))
    }

    @Test
    fun anAppTileHandsOverWhatItIsForAndNeverThePackageName() {
        val text = plainTextOf(
            title = "Music",
            subtitle = null,
            payload = CardPayload.AppLink(
                packageName = "com.spotify.music",
                label = "Songs",
                caption = "The quiet playlist is already chosen.",
            ),
        )

        assertEquals("Music\n\nSongs\n\nThe quiet playlist is already chosen.", text)
        assertFalse(text.contains("com.spotify"))
    }

    /** A folder's words are the titles of the tiles inside it, and each of those translates itself. */
    @Test
    fun aFolderIsJustItsOwnTitle() {
        val boardId = Uuid.parse("22222222-2222-2222-2222-222222222222")

        assertEquals("Mornings", plainTextOf("Mornings", null, CardPayload.Folder(boardId)))
        assertFalse(plainTextOf("Mornings", null, CardPayload.Folder(boardId)).contains("2222"))
    }

    @Test
    fun blankFieldsLeaveNoEmptyLinesBehind() {
        val text = plainTextOf(
            title = "  Bedtime  ",
            subtitle = "   ",
            payload = CardPayload.Guide(listOf(Step("Teeth first."), Step(""))),
        )

        assertEquals("Bedtime\n\nTeeth first.", text)
    }

    /**
     * An intent's extras cross a Binder transaction, and going over it throws rather than failing
     * politely. No tile anybody typed comes close — a tile that arrived in an import was not typed
     * by anybody.
     */
    @Test
    fun anAbsurdlyLongTileIsCutAtABlockBoundary() {
        val step = "x".repeat(1_000)
        val text = plainTextOf(
            title = "Long",
            subtitle = null,
            payload = CardPayload.Guide(List(200) { Step(step) }),
        )

        assertTrue(text.length <= 60_000, "was ${text.length}")
        // Cut between blocks, so the last step handed over is a whole step.
        assertTrue(text.endsWith(step))
    }
}
