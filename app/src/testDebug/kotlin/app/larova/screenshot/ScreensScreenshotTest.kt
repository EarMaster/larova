package app.larova.screenshot

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import app.larova.core.domain.export.ExportCounts
import app.larova.core.domain.export.ExportManifest
import app.larova.core.domain.model.AppearanceSetting
import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.Entitlement
import app.larova.core.domain.model.LastBackup
import app.larova.core.domain.usecase.PAID_TILE_TYPES
import app.larova.feature.card.edit.EditCardScreen
import app.larova.feature.card.edit.EditUiState
import app.larova.feature.card.edit.StepDraft
import app.larova.feature.card.edit.SymbolPickerScreen
import app.larova.feature.help.HelpScreen
import app.larova.feature.home.ArrangeTilesScreen
import app.larova.feature.home.HomeScreen
import app.larova.feature.home.HomeUiState
import app.larova.feature.settings.LogScreen
import app.larova.feature.settings.PinSetupScreen
import app.larova.feature.settings.SettingsScreen
import app.larova.feature.settings.UnlockCheck
import app.larova.feature.settings.UnlockScreen
import app.larova.feature.transfer.TransferScreen
import app.larova.feature.transfer.TransferUiState
import org.junit.Test
import org.robolectric.annotation.Config

/**
 * Everything that is not the start screen or a tile: the states around them, and the screens a
 * parent uses once and then forgets about.
 *
 * Captured in light only, and deliberately. What the three-mode matrix exists to catch is the
 * colour tokens, and these screens have none: they are theme colours all the way down, so a night
 * capture of the settings list would be a picture of `MaterialTheme` rather than of Larova. The
 * screens that *do* carry tokens — the grid and every tile type — get all four appearances.
 *
 * Light and a tablet, then. Width is the other thing that changes these screens, and unlike the
 * mode it changes them *structurally*: every form and list here is capped at reading width and
 * centred on a wide window, and a screen that fights that cap with a `fillMaxWidth` of its own is
 * invisible in the phone picture. Sixteen screens is exactly the case where nobody would have
 * opened all of them on a tablet by hand.
 */
abstract class ScreensScreenshotTest : ScreenshotTest() {

    /**
     * A first run, which is the one screen that must not look like a failure. Nothing here is
     * loading: an empty grid and a grid on its way are different pictures on purpose.
     */
    @Test
    fun home_empty() {
        capture("screens/home_empty") { home(HomeUiState(isLoading = false)) }
    }

    @Test
    fun home_search() {
        capture("screens/home_search") {
            home(HomeUiState(tiles = Fixtures.searchResults, query = "a", isLoading = false))
        }
    }

    /** Parent view adds to the menu and puts a button on the grid; the tiles do not change. */
    @Test
    fun home_parent_view() {
        capture("screens/home_parent_view") {
            home(
                state = HomeUiState(tiles = Fixtures.homeTiles, isLoading = false),
                isParentView = true,
            )
        }
    }

    @Test
    fun arrange_tiles() {
        capture("screens/arrange") {
            ArrangeTilesScreen(
                tiles = Fixtures.homeTiles,
                onMoveUp = {},
                onMoveDown = {},
                onBack = {},
            )
        }
    }

    /** The sheet behind the red bar. Read under stress, which is why it is short by design. */
    @Test
    fun help_sheet() {
        capture("screens/help_sheet") {
            HelpScreen(
                contacts = Fixtures.helpContacts,
                onPrepareCall = {},
                onBack = {},
                onHelp = {},
            )
        }
    }

    /** No number has been marked yet. Not an error, and the wording has to say so. */
    @Test
    fun help_sheet_empty() {
        capture("screens/help_sheet_empty") {
            HelpScreen(contacts = emptyList(), onPrepareCall = {}, onBack = {}, onHelp = {})
        }
    }

    /**
     * A literal, never BuildConfig.VERSION_NAME. A golden that carries the real version would go
     * stale on every single release, and the failure would look like a UI regression.
     */
    private val FIXTURE_VERSION = "0.0.0"

    @Test
    fun settings() {
        capture("screens/settings") {
            SettingsScreen(
                appearance = AppearanceSetting.SYSTEM,
                onAppearanceChange = {},
                isParentView = false,
                onUnlock = {},
                onLock = {},
                onChangePin = {},
                onOpenTransfer = {},
                onBack = {},
                // Hidden in the caregiver view, so the value cannot show up in the picture.
                entitlement = Entitlement.NONE,
                onCheckPurchases = {},
                unlockCheck = UnlockCheck.Idle,
                onDismissUnlockCheck = {},
                onBuyUnlock = {},
                onOpenSupportPage = {},
                onOpenLanguageSettings = {},
                supportCount = 0,
                onSupport = null,
                supportMessage = null,
                appVersion = FIXTURE_VERSION,
            )
        }
    }

    @Test
    fun settings_in_parent_view() {
        capture("screens/settings_parent_view") {
            SettingsScreen(
                appearance = AppearanceSetting.NIGHT,
                onAppearanceChange = {},
                isParentView = true,
                onUnlock = {},
                onLock = {},
                onChangePin = {},
                onOpenTransfer = {},
                onBack = {},
                // Not bought: the state most people are in, and the only one that draws the
                // status, the explanation and the button together.
                entitlement = Entitlement.NONE,
                onCheckPurchases = {},
                unlockCheck = UnlockCheck.Idle,
                onDismissUnlockCheck = {},
                onBuyUnlock = {},
                onOpenSupportPage = {},
                onOpenLanguageSettings = {},
                // Twice already: the count is the interesting state, not the empty one.
                supportCount = 2,
                onSupport = {},
                supportMessage = null,
                appVersion = FIXTURE_VERSION,
            )
        }
    }

    /**
     * With a backup already behind it, because the "last backed up" line only exists in that state
     * and a golden of the empty screen would never show it breaking.
     */
    @Test
    fun backup_and_restore() {
        capture("screens/transfer") {
            transfer(
                TransferUiState(
                    lastBackup = LastBackup(at = Fixtures.someEvening, cards = 14, media = 9),
                ),
            )
        }
    }

    /**
     * The import preview, which is the one dialog in the app that offers something irreversible.
     *
     * Captured with [captureScreen] rather than [capture]: an `AlertDialog` composes into a window
     * of its own, so photographing the activity's root would show the screen behind it with the
     * dialog missing — the exact picture that would make a regression here invisible.
     */
    @Test
    fun import_preview() {
        show {
            transfer(
                TransferUiState(
                    preview = ExportManifest(
                        appVersion = "0.1.0",
                        exportedAt = Fixtures.someEvening,
                        label = "Larova for Jonas",
                        counts = ExportCounts(boards = 2, cards = 14, media = 9),
                        contentSha256 = "0".repeat(SHA256_HEX_LENGTH),
                    ),
                    pendingSource = "content://downloads/larova-2026-03-14.larova",
                ),
            )
        }
        captureScreen("screens/transfer_import_dialog_$variant")
    }

    @Test
    fun activity_log() {
        capture("screens/log") {
            LogScreen(
                lines = Fixtures.logLines,
                note = "",
                isParentView = false,
                formatTime = Fixtures::formatTime,
                onNoteChange = {},
                onAddNote = {},
                onClear = {},
                onBack = {},
            )
        }
    }

    /**
     * The way into parent view. The biometric button is offered because most phones have one; the
     * PIN field stays for the second attempt and for the phones that do not.
     */
    @Test
    fun unlock() {
        capture("screens/unlock") {
            UnlockScreen(
                pin = "",
                onPinChange = {},
                onUnlock = {},
                onUseBiometrics = null,
                wrongPin = false,
                onBack = {},
            )
        }
    }

    @Test
    fun unlock_after_a_wrong_pin() {
        capture("screens/unlock_wrong_pin") {
            UnlockScreen(
                pin = "1234",
                onPinChange = {},
                onUnlock = {},
                onUseBiometrics = null,
                wrongPin = true,
                onBack = {},
            )
        }
    }

    /**
     * Choosing a PIN, with the sentence that matters most on the screen: it stops the tiles being
     * changed, it does not hide them. Somebody who believed otherwise might write something into a
     * tile they would not want a caregiver to read.
     */
    @Test
    fun choosing_a_pin() {
        capture("screens/pin_setup") {
            PinSetupScreen(
                pin = "",
                repeated = "",
                onPinChange = {},
                onRepeatChange = {},
                onSave = {},
                error = null,
                onBack = {},
            )
        }
    }

    /** The editor, mid-guide, which is where a parent spends the evening they set the app up. */
    @Test
    fun editing_a_tile() {
        capture("screens/edit_guide") {
            EditCardScreen(
                state = EditUiState(
                    isNew = false,
                    type = CardType.GUIDE,
                    title = "Bedtime",
                    subtitle = "",
                    colorToken = "sage",
                    symbolKey = "moon",
                    steps = listOf(
                        StepDraft(text = "Bath, and let him choose the duck."),
                        StepDraft(text = "Pyjamas — the blue ones are in the second drawer."),
                        StepDraft(text = "Teeth. He will say he has done them. He has not."),
                    ),
                ),
                callbacks = noOpEditCallbacks(),
                onBack = {},
            )
        }
    }

    /** A tile being made from scratch, which is the only time the type picker is on screen. */
    @Test
    fun making_a_tile() {
        capture("screens/edit_new") {
            EditCardScreen(
                state = EditUiState(isNew = true),
                callbacks = noOpEditCallbacks(),
                onBack = {},
            )
        }
    }

    /**
     * A paid tile type chosen in a build that has not bought it: the resting state.
     *
     * This is the picture that matters, and it is the reason the offer folds at all. The fields are
     * built as usual and stay legible behind a light wash, with one lock button over them, so
     * somebody can see what an Audio tile actually asks for before deciding whether it is worth
     * paying for. This golden used to show six lines of sales copy in front of the very fields they
     * were selling. The type picker stays outside the cover either way, which is what makes
     * choosing something else the way out.
     */
    @Test
    fun choosing_a_tile_type_that_costs_money() {
        capture("screens/edit_locked") { lockedAudioTile() }
    }

    /**
     * The same screen after the lock button is tapped.
     *
     * Driven through the button rather than set up by a flag, because the fold is state inside the
     * screen with no way in from outside — deliberately: nothing but a tap should open the offer.
     *
     * Worth its own picture because it is the only place the two body paragraphs, the price and the
     * full scrim are rendered together, and the most likely thing here to overflow at a large font
     * scale.
     */
    @Test
    fun the_full_version_offer_once_it_is_opened() {
        show { lockedAudioTile() }
        compose.onNodeWithText(FOLD_BUTTON).performClick()
        captureRoot("screens/edit_locked_offer_$variant")
    }

    /** A new Audio tile in a build that has not paid for one. Both stages start here. */
    @Composable
    private fun lockedAudioTile() {
        EditCardScreen(
            state = EditUiState(
                isNew = true,
                type = CardType.AUDIO,
                lockedTypes = PAID_TILE_TYPES,
                offerPrice = "€4.99",
            ),
            // A non-null onBuyUnlock, because the default fixture leaves it null and the button
            // would then be captured disabled — which is not what anybody sees.
            callbacks = noOpEditCallbacks().copy(onBuyUnlock = {}),
            onBack = {},
        )
    }

    /** The picker, browsing: sixty-eight suggestions first, the other two hundred behind search. */
    @Test
    fun symbol_picker() {
        capture("screens/symbol_picker") {
            SymbolPickerScreen(
                selectedKey = "moon",
                colorToken = "sage",
                onPick = {},
                onBack = {},
            )
        }
    }

    @Composable
    private fun home(state: HomeUiState, isParentView: Boolean = false) {
        HomeScreen(
            state = state,
            isParentView = isParentView,
            onQueryChange = {},
            onClearQuery = {},
            onOpenTile = {},
            onAddTile = {},
            onArrange = {},
            onOpenSettings = {},
            onOpenLog = {},
            onUseTemplate = {},
            onHelp = {},
        )
    }

    @Composable
    private fun transfer(state: TransferUiState) {
        TransferScreen(
            state = state,
            formatDate = { "14 Mar 2026" },
            onBackup = {},
            onRestore = {},
            onConfirmImport = {},
            onCancelImport = {},
            onBack = {},
        )
    }

    private companion object {
        /**
         * `purchase_show`, spelled out.
         *
         * These tests render in English, and matching on the text is how the rest of the suite
         * finds its nodes. It does mean editing that string means editing this line — which is
         * the cheap half of the trade: the alternative is a tag in production code that exists
         * only for a screenshot.
         */
        const val FOLD_BUTTON = "Locked — tap to unlock"

        /** A hash the preview never checks, only long enough to be one. */
        const val SHA256_HEX_LENGTH = 64
    }
}

class LightScreensScreenshotTest : ScreensScreenshotTest()

@Config(qualifiers = TABLET_QUALIFIERS)
class TabletScreensScreenshotTest : ScreensScreenshotTest() {
    override val variant = TABLET_VARIANT
}
