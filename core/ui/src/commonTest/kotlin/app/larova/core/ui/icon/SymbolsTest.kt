package app.larova.core.ui.icon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The picker's list, as the grid sees it.
 *
 * A lazy grid keys its cells by the symbol key, so a duplicate is not a cosmetic double entry: it
 * throws the moment both copies are near enough to be composed together — which is why the crash
 * arrived on the first search and not on the way in.
 */
class SymbolsTest {

    @Test
    fun everyChoiceHasItsOwnKey() {
        val keys = Symbols.all.map { it.key }
        val repeated = keys.groupingBy { it }.eachCount().filterValues { it > 1 }.keys

        assertEquals(emptySet(), repeated, "two symbol choices share a key")
    }

    /**
     * The three that got through: `book`, `key` and `map` are suggestions drawn by `book-open`,
     * `key-round` and `map-pin`, so the plain files were not recognised as taken.
     */
    @Test
    fun aFileNamedAfterASuggestionIsNotOfferedAgain() {
        val keys = Symbols.all.map { it.key }

        for (key in listOf("book", "key", "map")) {
            assertEquals(1, keys.count { it == key }, "'$key' is offered twice")
        }
    }

    /** Searching is the path the crash came in on; it never widens the list it filters. */
    @Test
    fun searchingReturnsASubsetWithItsKeysIntact() {
        val hits = Symbols.matching("key")

        assertTrue(hits.isNotEmpty(), "searching for 'key' finds nothing")
        assertTrue(hits.all { it in Symbols.all }, "search invented a choice")
        assertEquals(hits.size, hits.map { it.key }.toSet().size, "two results share a key")
    }

    /** The suggestions stay at the front: a parent browsing sees the moon, not a catalogue. */
    @Test
    fun theSuggestionsComeFirst() {
        assertEquals(Symbols.suggestions, Symbols.all.take(Symbols.suggestions.size))
    }
}
