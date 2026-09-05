package app.larova.screenshot

import androidx.compose.runtime.Composable
import app.larova.core.ui.theme.AppMode
import app.larova.feature.card.CardScreen
import app.larova.feature.card.CardUiState
import org.junit.Test
import org.robolectric.annotation.Config

/**
 * Every tile type, in every appearance, at both text sizes.
 *
 * This is the matrix `docs/implementation-plan.md` asks M3 for, and it is wide on purpose. Ten tile
 * types times three modes times 200 % scale is forty pictures, which sounds excessive until you
 * remember what they replace: the only other way to know that a table is still readable in night
 * mode is for somebody to put the phone in night mode and open a table.
 *
 * Adding a tile type means adding one method here. It then appears in all five captures without
 * anyone having to remember to ask for them.
 *
 * The fifth is a tablet, and it is not about appearance at all: on a wide window every renderer is
 * capped at reading width and centred, and each of them can get that wrong in its own way — a
 * table's columns, a video's box, a checklist's rows. One picture each is what makes that visible.
 */
abstract class CardScreenshotTest : ScreenshotTest() {

    @Test
    fun guide() = card("guide", Fixtures.guide)

    @Test
    fun note() = card("note", Fixtures.note)

    @Test
    fun checklist() = card("checklist", Fixtures.checklist)

    @Test
    fun table() = card("table", Fixtures.table)

    @Test
    fun call() = card("call", Fixtures.call)

    @Test
    fun website() = card("website", Fixtures.website)

    @Test
    fun folder() = card("folder", Fixtures.folder)

    @Test
    fun app_shortcut() = card("app_shortcut", Fixtures.appLink)

    /** The same tile on a phone that does not have that app. An ordinary thing to find. */
    @Test
    fun app_shortcut_uninstalled() = card("app_shortcut_gone", Fixtures.appLinkMissing)

    /** Both media tiles are captured with their file missing — see the note in `Fixtures`. */
    @Test
    fun video() = card("video", Fixtures.video)

    @Test
    fun audio() = card("audio", Fixtures.audio)

    /**
     * A tile that is not there: deleted, or written by a version that knows a type this one does
     * not. All three cases end in the same screen, and none of them may end in a crash — this is a
     * screen somebody opened while looking for help.
     */
    @Test
    fun missing() = card("missing", Fixtures.missing)

    /**
     * The same tile on a phone that has a translation app on it.
     *
     * One golden rather than one per type: the control is in the bar and does not change with the
     * payload, and what is worth a picture is that it sits left of nothing in caregiver view and
     * still leaves the title room. Every other card golden has `canTranslate` false, which is what
     * keeps them all unchanged.
     */
    @Test
    fun a_tile_on_a_phone_that_can_translate() = card("note_translate", Fixtures.noteTranslatable)

    private fun card(name: String, state: CardUiState) {
        capture("card/$name") { Card(state) }
    }

    @Composable
    private fun Card(state: CardUiState) {
        CardScreen(
            state = state,
            isParentView = false,
            onToggleItem = {},
            onPrepareCall = {},
            onOpenUrl = {},
            onOpenApp = {},
            onTranslate = {},
            onEdit = {},
            onBack = {},
            onHelp = {},
        )
    }
}

class LightCardScreenshotTest : CardScreenshotTest()

class DarkCardScreenshotTest : CardScreenshotTest() {
    override val mode = AppMode.DARK
}

class NightCardScreenshotTest : CardScreenshotTest() {
    override val mode = AppMode.NIGHT
}

class LargeTextCardScreenshotTest : CardScreenshotTest() {
    override val fontScale = LARGE_FONT_SCALE
    override val variant = LARGE_TEXT_VARIANT
}

@Config(qualifiers = TABLET_QUALIFIERS)
class TabletCardScreenshotTest : CardScreenshotTest() {
    override val variant = TABLET_VARIANT
}
