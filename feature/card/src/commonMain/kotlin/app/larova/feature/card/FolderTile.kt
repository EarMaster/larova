package app.larova.feature.card

/**
 * One tile inside a folder, as the grid needs it.
 *
 * Keys, not values: the colour and the symbol are resolved against the active appearance mode
 * inside `TileCard`, which is what makes a stored tile look right in light, dark and night with
 * nothing converted on the way in.
 *
 * A near-twin of the start screen's own tile type, and deliberately not shared with it — the two
 * feature modules do not depend on each other, and one screen's model leaking into another is how
 * that starts.
 */
data class FolderTile(
    val id: String,
    val title: String,
    val colorToken: String,
    val symbolKey: String,
    val subtitle: String?,
)
