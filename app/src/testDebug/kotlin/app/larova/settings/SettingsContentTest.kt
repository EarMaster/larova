package app.larova.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.larova.core.domain.model.AppearanceSetting
import app.larova.core.domain.model.Entitlement
import app.larova.core.ui.theme.LarovaTheme
import app.larova.feature.settings.SettingsScreen
import app.larova.feature.settings.SupportMessage
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * What the settings screen shows, asserted rather than photographed.
 *
 * The screenshot goldens capture a phone-sized viewport, so anything below the fold — the version
 * line at the very bottom, most of all — never appears in them. A golden that cannot show a row is
 * no evidence that the row exists, which is why these are text assertions.
 *
 * The screen takes state as parameters and hands events back out, so none of this needs a
 * ViewModel, a repository or a fake. That property is what makes it testable at all.
 */
@RunWith(AndroidJUnit4::class)
class SettingsContentTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun theVersionIsShownToEverybody() {
        show(isParentView = false, entitlement = Entitlement.NONE)

        // Not behind parent view: it is the first thing a support message needs, and a caregiver
        // is the person most likely to be asked for it over the phone.
        compose.onNodeWithText("Version 9.9.9").assertIsDisplayed()
    }

    @Test
    fun theCaregiverViewSaysNothingAboutBuying() {
        show(isParentView = false, entitlement = Entitlement.NONE)

        assertTrue(compose.countOf("The full version") == 0)
        assertTrue(compose.countOf("Check again") == 0)
    }

    @Test
    fun parentViewOffersToCheckAgainWhenNothingIsBought() {
        show(isParentView = true, entitlement = Entitlement.NONE)

        compose.onNodeWithText("The full version").assertIsDisplayed()
        compose.onNodeWithText("Not unlocked").assertIsDisplayed()
        compose.onNodeWithText("Check again").assertIsDisplayed()
    }

    @Test
    fun parentViewSaysSoOnceItIsBought() {
        show(isParentView = true, entitlement = Entitlement.PLAY)

        compose.onNodeWithText("Unlocked").assertIsDisplayed()
        // Still offered: a refund or a second phone are both reasons to ask again.
        compose.onNodeWithText("Check again").assertIsDisplayed()
    }

    /**
     * A build with no store behind it says why everything is available, and offers no button.
     * Somebody who compiled Larova themselves would otherwise wonder what they had unlocked.
     */
    @Test
    fun aBuildWithNoPaidTierExplainsItselfAndOffersNoCheck() {
        show(isParentView = true, entitlement = Entitlement.BUILD, onCheckPurchases = null)

        compose.onNodeWithText("Unlocked — this build has no paid version").assertIsDisplayed()
        assertTrue(compose.countOf("Check again") == 0)
    }

    @Test
    fun theContributionIsParentViewWorkToo() {
        show(isParentView = false, entitlement = Entitlement.PLAY)

        assertTrue(compose.countOf("Support the development") == 0)
    }

    @Test
    fun theContributionExplainsItselfBeforeAnybodyHasGiven() {
        show(isParentView = true, entitlement = Entitlement.PLAY, supportCount = 0)

        compose.onNodeWithText("Support the development").assertIsDisplayed()
        assertTrue(compose.countOf("Supported once") == 0)
    }

    /** The count is the feedback. A card that looked identical after paying would read as broken. */
    @Test
    fun theCountIsShownOnceSomebodyHasGiven() {
        show(isParentView = true, entitlement = Entitlement.PLAY, supportCount = 1)
        compose.onNodeWithText("Supported once").assertIsDisplayed()
    }

    @Test
    fun theCountIsPluralisedBeyondOne() {
        show(isParentView = true, entitlement = Entitlement.PLAY, supportCount = 3)
        compose.onNodeWithText("Supported 3 times").assertIsDisplayed()
    }

    @Test
    fun anOutcomeTakesPrecedenceOverTheCount() {
        show(
            isParentView = true, entitlement = Entitlement.PLAY,
            supportCount = 2, supportMessage = SupportMessage.THANKS,
        )

        compose.onNodeWithText("Thank you.").assertIsDisplayed()
        assertTrue(compose.countOf("Supported 2 times") == 0)
    }

    /** No store, no card. Drawn and inert would be worse than absent. */
    @Test
    fun aBuildWithNoStoreDrawsNoContributionCard() {
        show(isParentView = true, entitlement = Entitlement.BUILD, onSupport = null)

        assertTrue(compose.countOf("Support the development") == 0)
    }

    /** No `assertDoesNotExist` here: counting reads better when the point is "none of these". */
    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.countOf(text: String) =
        onAllNodesWithText(text).fetchSemanticsNodes().size

    private fun show(
        isParentView: Boolean,
        entitlement: Entitlement,
        onCheckPurchases: (() -> Unit)? = {},
        supportCount: Int = 0,
        onSupport: (() -> Unit)? = {},
        supportMessage: SupportMessage? = null,
    ) {
        compose.setContent {
            LarovaTheme {
                SettingsScreen(
                    appearance = AppearanceSetting.LIGHT,
                    onAppearanceChange = {},
                    isParentView = isParentView,
                    onUnlock = {},
                    onLock = {},
                    onChangePin = {},
                    onOpenTransfer = {},
                    onBack = {},
                    entitlement = entitlement,
                    onCheckPurchases = onCheckPurchases,
                    supportCount = supportCount,
                    onSupport = onSupport,
                    supportMessage = supportMessage,
                    appVersion = "9.9.9",
                )
            }
        }
    }
}
