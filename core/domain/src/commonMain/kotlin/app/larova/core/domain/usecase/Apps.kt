package app.larova.core.domain.usecase

import app.larova.core.domain.app.InstalledApp
import app.larova.core.domain.app.InstalledApps

/**
 * The apps a parent can choose from, in the order they will look for them.
 *
 * Sorted by label, case-insensitively, because a phone has a hundred apps on it and the only order
 * anybody can search by eye is alphabetical. The filter matches the label a person reads, not the
 * package name they never see — someone typing "spot" is not thinking about `com.spotify.music`.
 *
 * Larova itself is left out. A tile that opens the app it is on does nothing visible except lose
 * whoever tapped it, and offering it is how that tile gets made.
 */
class PickableApps(private val apps: InstalledApps) {

    suspend operator fun invoke(query: String = ""): List<InstalledApp> {
        val trimmed = query.trim()

        return apps.launchable()
            .filter { it.packageName != apps.ownPackageName }
            .filter { trimmed.isEmpty() || it.label.contains(trimmed, ignoreCase = true) }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }
}

/**
 * Whether the app a tile points at is still there.
 *
 * A separate step from opening it, so the screen can say "that app is not on this phone any more"
 * instead of showing a button that swallows the tap. An uninstalled app is an ordinary thing to
 * happen to a tile that was made months ago.
 */
class IsAppInstalled(private val apps: InstalledApps) {

    suspend operator fun invoke(packageName: String): Boolean =
        packageName.isNotBlank() && apps.isInstalled(packageName)
}

/**
 * Both halves of an app-shortcut tile in one dependency: what can be chosen, and whether what was
 * chosen is still there. Grouped the way [Pictures] and [Folders] are — the screen that offers the
 * list is the screen that has to draw the tile afterwards.
 */
class Apps(
    private val pickableApps: PickableApps,
    private val isAppInstalled: IsAppInstalled,
) {

    suspend fun pickable(query: String = "") = pickableApps(query)

    suspend fun isInstalled(packageName: String) = isAppInstalled(packageName)
}
