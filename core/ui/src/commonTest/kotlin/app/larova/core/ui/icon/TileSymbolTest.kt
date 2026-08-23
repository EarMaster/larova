package app.larova.core.ui.icon

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Symbol keys are stored in `Card.icon` and written to every export file, so they are frozen the
 * same way the colour tokens are.
 */
class TileSymbolTest {

    @Test
    fun theSymbolKeysAreFrozen() {
        assertEquals(
            listOf("moon", "sun", "heart", "list", "note", "phone", "clock", "home", "meal", "star"),
            TileSymbol.entries.map { it.key },
        )
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
