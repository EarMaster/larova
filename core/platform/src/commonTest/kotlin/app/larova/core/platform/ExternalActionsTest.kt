package app.larova.core.platform

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Both helpers decide what Larova hands to another app. A tile's contents are typed by a parent,
 * but an import can come from anywhere, so this is the boundary where content stops being trusted.
 */
class ExternalActionsTest {

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
}
