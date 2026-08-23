package app.larova.feature.home

/**
 * One tile as the grid needs it: a title, a symbol and a colour **key**.
 *
 * The key rather than a colour is the whole point. It is resolved against the active appearance
 * mode inside `TileCard`, which is why a tile drawn from a stored token looks right in light, dark
 * and night without anything being converted on the way in.
 */
data class HomeTile(
    val id: String,
    val title: String,
    val symbol: String,
    val colorToken: String,
    val subtitle: String? = null,
)
