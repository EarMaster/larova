package app.larova.core.domain.usecase

import app.larova.core.domain.repository.CardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Finds tiles by what they are called.
 *
 * Titles and second lines only. Larova stores and displays; it does not read what is inside a tile
 * — that is the regulatory line (docs/concept.md §2.2), and it is also why search cannot surprise
 * anyone with a hit from a note they had forgotten writing.
 *
 * A blank query returns nothing rather than everything. The caller shows the ordered start screen
 * in that case, and "everything, in title order" is not what an empty search box means.
 */
class SearchTiles(private val cards: CardRepository) {

    operator fun invoke(query: String): Flow<List<Tile>> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return flowOf(emptyList())
        return cards.search(trimmed).map { list -> list.mapNotNull { it.toTileOrNull() } }
    }
}
