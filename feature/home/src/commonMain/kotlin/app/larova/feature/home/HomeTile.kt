package app.larova.feature.home

/**
 * One tile as the grid needs it: a title, a symbol **key** and a colour **key**.
 *
 * The keys rather than the values are the whole point. Both are resolved against the active
 * appearance mode inside `TileCard`, which is why a tile drawn from stored keys looks right in
 * light, dark and night with nothing converted on the way in.
 */
data class HomeTile(
    val id: String,
    val title: String,
    val colorToken: String,
    val symbolKey: String,
    val subtitle: TileSubtitle = TileSubtitle.None,
)

/**
 * What the second line of a tile says.
 *
 * A type rather than a formatted string, because the formatting needs plurals and plurals need the
 * language — which the ViewModel has no business knowing. Polish, Russian and Arabic have more
 * than two forms, so "1 step / 2 steps" is not something to assemble in Kotlin.
 */
sealed interface TileSubtitle {

    /** Nothing to say. Most tiles, and deliberately so — the grid is read at a glance. */
    data object None : TileSubtitle

    /** What the parents typed. Always wins over anything derived. */
    data class Custom(val text: String) : TileSubtitle

    data class Steps(val count: Int) : TileSubtitle

    data class Items(val count: Int) : TileSubtitle

    /**
     * A folder says so.
     *
     * The one type that names itself rather than counting: the symbol is the parents' choice, so
     * nothing else on the tile distinguishes "opens further tiles" from "opens a note". A count of
     * what is inside would mean a query per tile on the screen that has to appear fastest.
     */
    data object Folder : TileSubtitle
}
