package app.larova.core.domain

import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.CardType
import app.larova.core.domain.usecase.ObserveHelpContacts
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * The help sheet is read under stress by someone who may never have seen the app. What appears on it
 * — and what does not — is therefore worth pinning down.
 */
@OptIn(ExperimentalUuidApi::class)
class HelpContactsTest {

    private val at = Instant.parse("2026-08-23T18:12:00Z")
    private val boardId = Uuid.parse("66666666-7777-4888-8999-aaaaaaaaaaaa")

    @Test
    fun onlyMarkedNumbersAppear() = runTest {
        val cards = FakeCardRepository(
            listOf(
                phoneCard("Mum", "+49 170 1", sortIndex = 0, inHelpSheet = true),
                phoneCard("Dentist", "+49 30 2", sortIndex = 1, inHelpSheet = false),
                phoneCard("Dad", "+49 170 3", sortIndex = 2, inHelpSheet = true),
            ),
        )

        val contacts = ObserveHelpContacts(cards)().first()

        assertEquals(listOf("Mum", "Dad"), contacts.map { it.displayName })
    }

    @Test
    fun theSheetKeepsTheOrderOfTheTiles() = runTest {
        // The parents arranged the tiles; the sheet does not second-guess that order.
        val cards = FakeCardRepository(
            listOf(
                phoneCard("Second", "2", sortIndex = 5, inHelpSheet = true),
                phoneCard("First", "1", sortIndex = 1, inHelpSheet = true),
            ),
        )

        assertEquals(
            listOf("First", "Second"),
            ObserveHelpContacts(cards)().first().map { it.displayName },
        )
    }

    @Test
    fun theSheetIsCappedSoItStaysReadable() = runTest {
        // A sheet with nine numbers on it is not a sheet anyone reads in an emergency. The cap
        // holds even if somebody marks every tile.
        val many = (1..9).map { phoneCard("Contact $it", "$it", sortIndex = it, inHelpSheet = true) }

        val contacts = ObserveHelpContacts(FakeCardRepository(many))().first()

        assertEquals(ObserveHelpContacts.MAX_CONTACTS, contacts.size)
        assertEquals("Contact 1", contacts.first().displayName)
    }

    @Test
    fun tilesThatAreNotNumbersAreNotContacts() = runTest {
        val cards = FakeCardRepository(
            listOf(
                noteCard("Bedtime", sortIndex = 0),
                phoneCard("Mum", "+49 170 1", sortIndex = 1, inHelpSheet = true),
            ),
        )

        assertEquals(listOf("Mum"), ObserveHelpContacts(cards)().first().map { it.displayName })
    }

    @Test
    fun aMarkedTileWithNoNumberIsLeftOut() = runTest {
        // Half-finished while a parent was interrupted. A row that dials nothing is worse than no
        // row at all on this screen.
        val cards = FakeCardRepository(listOf(phoneCard("Mum", "  ", sortIndex = 0, inHelpSheet = true)))

        assertTrue(ObserveHelpContacts(cards)().first().isEmpty())
    }

    @Test
    fun theTileTitleStandsInForAMissingName() = runTest {
        val cards = FakeCardRepository(
            listOf(
                phoneCard(
                    displayName = "",
                    number = "+49 170 1",
                    sortIndex = 0,
                    inHelpSheet = true,
                    title = "Grandma",
                ),
            ),
        )

        val contact = ObserveHelpContacts(cards)().first().single()
        assertEquals("Grandma", contact.displayName)
        assertNull(contact.relation)
    }

    @Test
    fun aPayloadWrittenBeforeTheFlagExistedStillReads() = runTest {
        // What every already-stored call tile looks like: no inHelpSheet at all. It has to decode,
        // and it has to stay off the sheet rather than appearing on it by surprise.
        val stored = """{"type":"phone","displayName":"Mum","number":"+49 170 1"}"""
        val decoded = CardPayloadCodec.decodeOrNull(stored)

        assertEquals(CardPayload.Phone("Mum", "+49 170 1", relation = null, inHelpSheet = false), decoded)

        val cards = FakeCardRepository(listOf(card("Mum", CardType.PHONE, stored, sortIndex = 0)))
        assertTrue(ObserveHelpContacts(cards)().first().isEmpty())
    }

    private fun phoneCard(
        displayName: String,
        number: String,
        sortIndex: Int,
        inHelpSheet: Boolean,
        title: String = displayName,
    ) = card(
        title = title,
        type = CardType.PHONE,
        payload = CardPayloadCodec.encode(
            CardPayload.Phone(displayName = displayName, number = number, inHelpSheet = inHelpSheet),
        ),
        sortIndex = sortIndex,
    )

    private fun noteCard(title: String, sortIndex: Int) = card(
        title = title,
        type = CardType.NOTE,
        payload = CardPayloadCodec.encode(CardPayload.Note("Something")),
        sortIndex = sortIndex,
    )

    private fun card(title: String, type: CardType, payload: String, sortIndex: Int) = Card(
        id = Uuid.random(),
        boardId = boardId,
        title = title,
        icon = "phone",
        colorToken = "clay",
        sortIndex = sortIndex,
        type = type,
        payload = payload,
        updatedAt = at,
    )
}
