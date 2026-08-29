package app.larova.core.ui.icon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Symbol keys are stored in `Card.icon` and written to every export file, so they are frozen the
 * same way the colour tokens are.
 */
class TileSymbolTest {

    /**
     * The set grows; nothing ever leaves it.
     *
     * These ten shipped in 0.1.0 and are sitting in `Card.icon` on real phones and in export files
     * nobody can reach to correct. Renaming one silently repoints every tile that used it — on the
     * phone and in the backups both — so this asserts they are all still here rather than
     * asserting the whole list, which would only mean "nobody added an icon today".
     */
    @Test
    fun theOriginalSymbolKeysAreStillHere() {
        val shippedFirst =
            listOf("moon", "sun", "heart", "list", "note", "phone", "clock", "home", "meal", "star")
        val keys = TileSymbol.entries.map { it.key }

        for (key in shippedFirst) {
            assertTrue(key in keys, "the symbol key '$key' was renamed or removed, which it cannot be")
        }
    }

    /** Two entries with one key would make `fromKey` return whichever came first, silently. */
    @Test
    fun everyKeyIsUsedOnce() {
        val keys = TileSymbol.entries.map { it.key }
        assertEquals(keys.size, keys.toSet().size, "two symbols share a key")
    }

    /** The name is what the picker searches and what a screen reader reads out. */
    @Test
    fun everySymbolIsNamed() {
        for (symbol in TileSymbol.entries) {
            assertTrue(symbol.label.isNotBlank(), "${symbol.key} has no name")
        }
    }

    @Test
    fun keysRoundTripAndUnknownOnesFallBack() {
        for (symbol in TileSymbol.entries) {
            assertEquals(symbol, TileSymbol.fromKey(symbol.key))
        }
        assertEquals(TileSymbol.DEFAULT, TileSymbol.fromKey(null))
        assertEquals(TileSymbol.DEFAULT, TileSymbol.fromKey(""))
        // A symbol a later version added. The tile still draws, and its stored key is untouched.
        assertEquals(TileSymbol.DEFAULT, TileSymbol.fromKey("hologram"))
    }

    @Test
    fun everySymbolHasSomethingToDraw() {
        // A key with no vector behind it would be an invisible tile, and the enum is the only place
        // that mapping can be forgotten.
        for (symbol in TileSymbol.entries) {
            val image = symbol.image
            assertTrue(image.defaultWidth.value > 0f, "${symbol.key} has no size")
            assertTrue(image.root.iterator().hasNext(), "${symbol.key} draws nothing")
        }
    }
}
