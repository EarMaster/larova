package app.larova.screenshot

import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CheckItem
import app.larova.core.domain.model.Step
import app.larova.core.domain.usecase.HelpContact
import app.larova.feature.card.CardUiState
import app.larova.feature.home.HomeTile
import app.larova.feature.home.TileSubtitle

/**
 * The family content behind one locale's store screenshots.
 *
 * The app's own chrome — "Get help", "Step 1 of 5" — comes from `strings.xml` and follows the
 * locale qualifier on the test class. What does *not* follow it is everything a parent typed, and
 * on these five screens that is most of the words on them. A German listing showing a tile called
 * "Bag for nursery" would advertise an app nobody wants, so each locale gets its own object here.
 *
 * That also sets the bar for adding one: a locale needs a person who speaks it to write the tiles,
 * not a qualifier. See `AGENTS.md`, "Store assets".
 */
// Public rather than `internal` only because `StoreAssetTest.content` is protected, and Kotlin
// refuses to let a protected member expose an internal type. Nothing outside this source set can
// see any of it either way.
class StoreContent(
    val homeTiles: List<HomeTile>,
    val guide: CardUiState,
    val checklist: CardUiState,
    val helpContacts: List<HelpContact>,
)

/** English is the source, and reuses the goldens' fixtures so the two cannot drift apart. */
val EnglishStoreContent = StoreContent(
    homeTiles = Fixtures.homeTiles,
    guide = Fixtures.guide,
    checklist = Fixtures.checklist,
    helpContacts = Fixtures.helpContacts,
)

/**
 * German — the launch market, and the only locale besides English the app itself is translated
 * into, which is what makes a German screenshot worth taking at all.
 *
 * The same family as the English fixture, not a different one: the two listings should show the
 * same app rather than invite a comparison of which is nicer. The phone numbers and the name were
 * already German.
 */
val GermanStoreContent = StoreContent(
    homeTiles = listOf(
        HomeTile(
            id = "bedtime",
            title = "Zubettgehen",
            colorToken = "sage",
            symbolKey = "moon",
            subtitle = TileSubtitle.Steps(count = 5),
        ),
        HomeTile(
            id = "morning",
            title = "Aufstehen",
            colorToken = "sand",
            symbolKey = "sun",
            subtitle = TileSubtitle.Steps(count = 4),
        ),
        HomeTile(
            id = "lunch",
            title = "Essen und Trinken",
            colorToken = "moss",
            symbolKey = "meal",
            subtitle = TileSubtitle.Custom("Keine Nüsse, keine Kuhmilch"),
        ),
        HomeTile(
            id = "packing",
            title = "Kita-Tasche",
            colorToken = "sky",
            symbolKey = "list",
            subtitle = TileSubtitle.Items(count = 6),
        ),
        HomeTile(
            id = "grandma",
            title = "Oma Käthe",
            colorToken = "rose",
            symbolKey = "phone",
            subtitle = TileSubtitle.Custom("Mamas Mutter"),
        ),
        HomeTile(
            id = "calm",
            title = "Was hilft, wenn er sich aufregt",
            colorToken = "lilac",
            symbolKey = "heart",
            subtitle = TileSubtitle.Steps(count = 3),
        ),
        HomeTile(
            id = "week",
            title = "Ein Tag bei uns",
            colorToken = "clay",
            symbolKey = "clock",
            subtitle = TileSubtitle.None,
        ),
        HomeTile(
            id = "holidays",
            title = "Urlaub",
            colorToken = "stone",
            symbolKey = "home",
            subtitle = TileSubtitle.Folder,
        ),
    ),
    guide = CardUiState(
        title = "Zubettgehen",
        colorToken = "sage",
        isLoading = false,
        payload = CardPayload.Guide(
            steps = listOf(
                Step(text = "Baden — die Ente darf er sich aussuchen."),
                Step(text = "Schlafanzug. Der blaue liegt in der zweiten Schublade."),
                Step(text = "Zähne putzen. Er sagt, er hätte schon. Hat er nicht."),
                Step(text = "Eine Geschichte, im Sitzen auf dem Bett, nicht im Liegen."),
                Step(text = "Licht aus, Tür eine Handbreit offen. Das kontrolliert er."),
            ),
        ),
    ),
    checklist = CardUiState(
        title = "Kita-Tasche",
        colorToken = "sky",
        isLoading = false,
        payload = CardPayload.Checklist(
            items = listOf(
                CheckItem(text = "Windeln, mindestens vier", done = true),
                CheckItem(text = "Wechselkleidung", done = true),
                CheckItem(text = "Der Hase — ohne ihn schläft er nicht", done = false),
                CheckItem(text = "Sonnenhut", done = false),
                CheckItem(text = "Trinkflasche", done = false),
                CheckItem(text = "Gummistiefel, falls es geregnet hat", done = false),
            ),
            resetDaily = true,
        ),
    ),
    helpContacts = listOf(
        HelpContact(
            cardId = "grandma",
            displayName = "Käthe Bergmann",
            number = "+49 30 1234567",
            relation = "Mamas Mutter",
        ),
        HelpContact(
            cardId = "mum",
            displayName = "Mama",
            number = "+49 170 9876543",
            relation = null,
        ),
        HelpContact(
            cardId = "doctor",
            displayName = "Praxis Dr. Ilg",
            number = "+49 30 7654321",
            relation = "Werktags bis 17 Uhr",
        ),
    ),
)
