package app.larova.core.ui.icon

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * One choice in the symbol picker: what a tile would store, what it is called, and which shelf it
 * sits on.
 *
 * [label] is English and is not translated — see [TileSymbol] for the reasoning, which applies here
 * with more force: this is nearly three hundred nouns.
 */
data class SymbolChoice(val key: String, val label: String, val group: SymbolGroup)

/**
 * Every symbol a tile can carry.
 *
 * Two layers, and the difference matters. [suggestions] are the sixty-eight chosen for what a
 * family's tiles are usually about, with names written here; [all] is those plus everything else in
 * `core/ui/icons/`, named after the file it came from. A parent browsing sees the suggestions
 * first, and search reaches the rest.
 *
 * A drawing appears once. A symbol offered as a suggestion is not offered a second time under the
 * name of the file that draws it — `meal` is in the list and `utensils`, which draws it, is not.
 */
object Symbols {

    val suggestions: List<SymbolChoice> by lazy {
        TileSymbol.entries.map { SymbolChoice(it.key, it.label, it.group) }
    }

    val all: List<SymbolChoice> by lazy {
        val drawnBySuggestion = TileSymbol.entries.map { it.drawing }.toSet()
        val rest = SYMBOL_SHELVES
            .filterKeys { it !in drawnBySuggestion }
            .map { (key, shelf) -> SymbolChoice(key, prettify(key), shelfOf(shelf)) }
            .sortedBy { it.label }
        suggestions + rest
    }

    /** What to call a key that was stored on a tile, whether or not it is a suggestion. */
    fun label(key: String): String =
        TileSymbol.entries.firstOrNull { it.key == key }?.label ?: prettify(key)

    /**
     * Matches a name loosely enough to be useful and strictly enough to be predictable: "bus"
     * finds "Bus" and "Bus front", and the hyphen in a file name is a word break, so "front" finds
     * it too.
     */
    fun matching(query: String): List<SymbolChoice> {
        val needle = query.trim()
        if (needle.isEmpty()) return all
        return all.filter { choice ->
            choice.label.contains(needle, ignoreCase = true) ||
                choice.key.replace('-', ' ').contains(needle, ignoreCase = true)
        }
    }

    /**
     * "cup-soda" becomes "Cup soda".
     *
     * Upstream's naming, tidied rather than rewritten. It reads a little like a catalogue in the
     * long tail, which is the honest trade for reaching three hundred drawings without writing —
     * and translating — three hundred names.
     */
    private fun prettify(key: String): String = key
        .replace('-', ' ')
        .replaceFirstChar { it.uppercase() }

    private fun shelfOf(shelf: String): SymbolGroup =
        SymbolGroup.entries.firstOrNull { it.name.equals(shelf, ignoreCase = true) }
            ?: SymbolGroup.NOTES
}

/**
 * The drawing for a key a tile stored, whatever it is.
 *
 * Resolves a suggestion through its `drawing` first, so `meal` finds `utensils.svg`; then tries the
 * key as a file name, which is how everything outside the suggestions is stored. An unknown key —
 * a tile written by a newer Larova with a symbol this build has never heard of — falls back to the
 * default rather than failing. The tile still opens and still says what the parents titled it;
 * only the picture is missing, which is the right way round.
 */
fun symbolImage(key: String?): ImageVector {
    val viaSuggestion = TileSymbol.entries.firstOrNull { it.key == key }?.drawing
    return tileSymbolVector(viaSuggestion ?: key.orEmpty())
        ?: tileSymbolVector(TileSymbol.DEFAULT.drawing)
        ?: BlankSymbol
}
