package app.larova.settings

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.larova.core.domain.model.AppearanceSetting
import app.larova.core.domain.model.Entitlement
import app.larova.core.ui.theme.LarovaTheme
import app.larova.feature.settings.ContentLanguageChoice
import app.larova.feature.settings.ContentLanguageSetting
import app.larova.feature.settings.SUPPORT_URL
import app.larova.feature.settings.SettingsScreen
import app.larova.feature.settings.SupportMessage
import app.larova.feature.settings.UnlockCheck
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
        compose.scrolledTo("Version 9.9.9").assertIsDisplayed()
    }

    @Test
    fun theCaregiverViewSaysNothingAboutBuying() {
        show(isParentView = false, entitlement = Entitlement.NONE)

        assertTrue(compose.countOf("The full version") == 0)
        assertTrue(compose.countOf(ASK_AGAIN) == 0)
    }

    @Test
    fun parentViewOffersToCheckAgainWhenNothingIsBought() {
        show(isParentView = true, entitlement = Entitlement.NONE)

        compose.scrolledTo("The full version").assertIsDisplayed()
        compose.scrolledTo("Not unlocked").assertIsDisplayed()
        // The card is the target, so the invitation is a sentence rather than a button label.
        assertTrue(compose.countOf(ASK_AGAIN) == 1)
    }

    @Test
    fun parentViewSaysSoOnceItIsBought() {
        show(isParentView = true, entitlement = Entitlement.PLAY)

        compose.scrolledTo("Unlocked").assertIsDisplayed()
        // Still offered: a refund or a second phone are both reasons to ask again.
        assertTrue(compose.countOf(ASK_AGAIN) == 1)
    }

    /**
     * A build with no store behind it says why everything is available, and offers no retry.
     * Somebody who compiled Larova themselves would otherwise wonder what they had unlocked.
     *
     * The status still reads "Unlocked" — it is the same line the paid case shows, because it is
     * the same fact. What differs is the sentence under it.
     *
     * Asserted as one exact string rather than two substrings, so it also proves the `\n\n` in
     * the resource arrives as a blank line: a Compose build that passed the escape through
     * verbatim would fail here rather than shipping a visible backslash-n to whoever sideloads.
     */
    @Test
    fun aBuildWithNoPaidTierExplainsItselfAndOffersNoCheck() {
        show(isParentView = true, entitlement = Entitlement.BUILD, onCheckPurchases = null)

        compose.scrolledTo("Unlocked").assertIsDisplayed()
        compose.scrolledTo(
            "This build has no paid version — every kind of tile is unlocked.\n\n" +
                "If you would still like to support the developer: \uD83D\uDC9D $SUPPORT_URL",
        ).assertIsDisplayed()
        assertTrue(compose.countOf(ASK_AGAIN) == 0)
    }

    /**
     * And the address is reachable, not just printed.
     *
     * This is the bug: the card was disabled in a build with nothing to sell, so the one place
     * Larova names its support page was the one place a tap did nothing.
     */
    @Test
    fun theSupportPageIsReachableInABuildWithNoStore() {
        var opened = false
        show(
            isParentView = true,
            entitlement = Entitlement.BUILD,
            onCheckPurchases = null,
            onOpenSupportPage = { opened = true },
        )

        compose.scrolledTo("The full version").performClick()

        assertTrue(opened)
    }

    /**
     * The tap's own answer, which is the thing that used to be missing: the check ran in the
     * background and a card that already read "Not unlocked" went on reading "Not unlocked".
     */
    @Test
    fun findingNothingSaysSoAndOffersToBuy() {
        show(
            isParentView = true,
            entitlement = Entitlement.NONE,
            unlockCheck = UnlockCheck.NotFound(price = "\u20ac3.99"),
        )

        compose.onNodeWithText("Google Play has no full version", substring = true)
            .assertIsDisplayed()
        // Play's own price, never a number written in the app.
        compose.onNodeWithText("Unlock for \u20ac3.99").assertIsDisplayed()
        compose.onNodeWithText("Not now").assertIsDisplayed()
    }

    /** A phone that could not reach the store gets the offer anyway, without a number on it. */
    @Test
    fun theOfferStandsWithoutAPrice() {
        show(
            isParentView = true,
            entitlement = Entitlement.NONE,
            unlockCheck = UnlockCheck.NotFound(),
        )

        compose.onNodeWithText("Unlock").assertIsDisplayed()
        assertTrue(compose.countOf("Unlock for") == 0)
    }

    /** While the store is being asked, the card says so rather than sitting there unchanged. */
    @Test
    fun theWaitIsShownOnTheCard() {
        show(
            isParentView = true,
            entitlement = Entitlement.NONE,
            unlockCheck = UnlockCheck.Checking,
        )

        compose.scrolledTo("Asking Google Play\u2026").assertIsDisplayed()
        assertTrue(compose.countOf("Not unlocked") == 0)
    }

    @Test
    fun theContributionIsParentViewWorkToo() {
        show(isParentView = false, entitlement = Entitlement.PLAY)

        assertTrue(compose.countOf("Support the development") == 0)
    }

    @Test
    fun theContributionExplainsItselfBeforeAnybodyHasGiven() {
        show(isParentView = true, entitlement = Entitlement.PLAY, supportCount = 0)

        compose.scrolledTo("Support the development").assertIsDisplayed()
        assertTrue(compose.countOf("Supported once") == 0)
    }

    /** The count is the feedback. A card that looked identical after paying would read as broken. */
    @Test
    fun theCountIsShownOnceSomebodyHasGiven() {
        show(isParentView = true, entitlement = Entitlement.PLAY, supportCount = 1)
        compose.scrolledTo("Supported once").assertIsDisplayed()
    }

    @Test
    fun theCountIsPluralisedBeyondOne() {
        show(isParentView = true, entitlement = Entitlement.PLAY, supportCount = 3)
        compose.scrolledTo("Supported 3 times").assertIsDisplayed()
    }

    @Test
    fun anOutcomeTakesPrecedenceOverTheCount() {
        show(
            isParentView = true, entitlement = Entitlement.PLAY,
            supportCount = 2, supportMessage = SupportMessage.THANKS,
        )

        compose.scrolledTo("Thank you.").assertIsDisplayed()
        assertTrue(compose.countOf("Supported 2 times") == 0)
    }

    /** No store, no card. Drawn and inert would be worse than absent. */
    @Test
    fun aBuildWithNoStoreDrawsNoContributionCard() {
        show(isParentView = true, entitlement = Entitlement.BUILD, onSupport = null)

        assertTrue(compose.countOf("Support the development") == 0)
    }

    /**
     * The node with this text, scrolled into view first.
     *
     * The about group — the full version, the contribution and the version line — sits at
     * the foot of a scrolling screen, so "is it displayed" is only a fair question once the screen
     * has been scrolled the way a person would scroll it. A node already on screen is unmoved by
     * this, so it costs nothing to ask for it either way.
     *
     * Only for nodes inside the screen's own scrolling column: a dialog has no scrollable parent,
     * and `performScrollTo` on one throws rather than doing nothing.
     */
    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.scrolledTo(
        text: String,
    ) = onNodeWithText(text).performScrollTo()

    /** No `assertDoesNotExist` here: counting reads better when the point is "none of these". */
    private fun androidx.compose.ui.test.junit4.AndroidComposeTestRule<*, *>.countOf(text: String) =
        onAllNodesWithText(text, substring = true).fetchSemanticsNodes().size

    private companion object {
        /**
         * The opening words of `settings_unlock_hint`, which is what the retry looks like now
         * that the whole card is the target and there is no "Check again" button to find.
         *
         * Matched as a substring, so rewording the rest of the sentence does not break this.
         */
        const val ASK_AGAIN = "Larova asks Google Play at every start."
    }

    private fun show(
        isParentView: Boolean,
        entitlement: Entitlement,
        onCheckPurchases: (() -> Unit)? = {},
        onOpenSupportPage: () -> Unit = {},
        onOpenLanguageSettings: (() -> Unit)? = {},
        contentLanguage: ContentLanguageSetting? = null,
        unlockCheck: UnlockCheck = UnlockCheck.Idle,
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
                    unlockCheck = unlockCheck,
                    onDismissUnlockCheck = {},
                    onBuyUnlock = {},
                    onOpenSupportPage = onOpenSupportPage,
                    onOpenLanguageSettings = onOpenLanguageSettings,
                    contentLanguage = contentLanguage,
                    onContentLanguageChange = {},
                    supportCount = supportCount,
                    onSupport = onSupport,
                    supportMessage = supportMessage,
                    appVersion = "9.9.9",
                )
            }
        }
    }
}
