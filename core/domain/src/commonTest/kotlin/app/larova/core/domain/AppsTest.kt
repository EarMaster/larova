package app.larova.core.domain

import app.larova.core.domain.app.InstalledApp
import app.larova.core.domain.app.InstalledApps
import app.larova.core.domain.usecase.IsAppInstalled
import app.larova.core.domain.usecase.PickableApps
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * What an app-shortcut tile is allowed to offer and allowed to trust.
 *
 * The `PackageManager` query and the launch intent belong to the platform and are exercised on a
 * device. What is checked here is the part that decides what a parent sees in the picker, and
 * whether a tile made months ago still has anything behind it.
 */
class AppsTest {

    @Test
    fun appsAreOfferedInTheOrderSomebodyWouldLookForThem() = runTest {
        val apps = FakeInstalledApps(
            listOf(
                app("com.example.zebra", "Zebra"),
                app("com.example.apple", "apple"),
                app("com.example.mail", "Mail"),
            ),
        )

        // Case-insensitively: a list where "apple" sorts after "Zebra" is a list nobody can scan.
        assertEquals(
            listOf("apple", "Mail", "Zebra"),
            PickableApps(apps)().map { it.label },
        )
    }

    /** A tile that opens the app it is on does nothing except lose whoever tapped it. */
    @Test
    fun larovaIsNotOnOffer() = runTest {
        val apps = FakeInstalledApps(
            listOf(app("app.larova", "Larova"), app("com.example.mail", "Mail")),
            own = "app.larova",
        )

        assertEquals(listOf("Mail"), PickableApps(apps)().map { it.label })
    }

    @Test
    fun searchingMatchesTheNameAPersonReads() = runTest {
        val apps = FakeInstalledApps(
            listOf(
                app("com.spotify.music", "Spotify"),
                app("com.example.mail", "Mail"),
            ),
        )
        val pickable = PickableApps(apps)

        assertEquals(listOf("Spotify"), pickable("spot").map { it.label })
        assertEquals(listOf("Spotify"), pickable("  SPOT ").map { it.label })
        // The package name is not what somebody typing "music" is thinking of, and matching it would
        // put Spotify under three different searches for no reason anybody could predict.
        assertTrue(pickable("com.spotify").isEmpty())
        assertEquals(2, pickable("").size)
    }

    @Test
    fun aTileWithNoAppBehindItIsNotTreatedAsInstalled() = runTest {
        val apps = FakeInstalledApps(listOf(app("com.example.mail", "Mail")))
        val installed = IsAppInstalled(apps)

        assertTrue(installed("com.example.mail"))
        // Uninstalled since the tile was made, and a tile saved with nothing chosen at all.
        assertFalse(installed("com.example.gone"))
        assertFalse(installed(""))
        assertFalse(installed("   "))
    }

    private fun app(packageName: String, label: String) =
        InstalledApp(packageName = packageName, label = label, icon = null)
}

private class FakeInstalledApps(
    private val installed: List<InstalledApp>,
    private val own: String = "app.larova.test",
) : InstalledApps {

    override val ownPackageName: String get() = own

    override suspend fun launchable(): List<InstalledApp> = installed

    override suspend fun isInstalled(packageName: String): Boolean =
        installed.any { it.packageName == packageName }
}
