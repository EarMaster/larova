package app.larova.core.domain.app

/**
 * One app that can be opened from a tile.
 *
 * [icon] is the live system icon, encoded as bytes for the picker to draw. It is never stored: a
 * tile keeps a symbol **key** and a colour **key** and nothing else, which is invariant 1 in
 * `AGENTS.md`. An icon copied into a tile would be a bitmap frozen at the moment it was picked —
 * wrong after the app updates, wrong in night mode, and unrepairable because the tile was made by
 * a parent who is not there to fix it.
 *
 * Not a data class: a `ByteArray` in one gives an `equals` that compares references, which is a
 * trap for anything that later tries to deduplicate a list of these.
 */
class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: ByteArray?,
)

/**
 * What is on this phone that a tile could open.
 *
 * Only apps with a launcher entry, which is what the `<queries>` element in the manifest asks the
 * system for. `QUERY_ALL_PACKAGES` is deliberately not used — an app that can enumerate everything
 * installed is an app with a reason to explain itself to Play review and to the person reading the
 * data safety form, and "the parents pick one app to put on a tile" does not need it.
 */
interface InstalledApps {

    /** The package Larova itself runs as, so it can be kept out of what it offers. */
    val ownPackageName: String

    suspend fun launchable(): List<InstalledApp>

    /**
     * Whether this package is still here **and** still openable. Asked when a tile is drawn rather
     * than assumed from the tile: apps get uninstalled, and a button that does nothing is worse
     * than a line saying the app is gone.
     */
    suspend fun isInstalled(packageName: String): Boolean
}
