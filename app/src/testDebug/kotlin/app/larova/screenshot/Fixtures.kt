package app.larova.screenshot

import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CheckItem
import app.larova.core.domain.model.LogKind
import app.larova.core.domain.model.PhoneEntry
import app.larova.core.domain.model.Step
import app.larova.core.domain.model.phoneOf
import app.larova.core.domain.usecase.HelpContact
import app.larova.core.domain.usecase.LogLine
import app.larova.feature.card.CardUiState
import app.larova.feature.card.FolderTile
import app.larova.feature.card.TileLanguage
import app.larova.feature.home.HomeTile
import app.larova.feature.home.TileSubtitle
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * One family's tiles, and the only content any picture in this module is made from.
 *
 * Written as a real installation rather than as `Tile 1`, `Tile 2`, for two reasons. A golden is
 * read by a person deciding whether a change is right, and a screen of placeholder text tells them
 * nothing about whether the app is legible. And the same fixtures make the Play Store images
 * (`StoreAssetTest`), where placeholder content would be worse than no screenshot at all.
 *
 * The stress cases are deliberate and each is here to catch something:
 *
 * - **A title long enough to wrap**, because 200 % font scale is a promise this app makes and a
 *   tile that clips at that size is a bug only a picture finds.
 * - **Umlauts**, because German is in the launch set and the tile grid is the first place a font
 *   substitution would show.
 * - **All eight colour tokens across the grid**, so a token that resolves wrong in dark or night is
 *   visible in one image rather than in none.
 */
@OptIn(ExperimentalUuidApi::class)
internal object Fixtures {

    /** Fixed rather than generated: a golden cannot afford an identifier that changes per run. */
    val folderBoardId: Uuid = Uuid.parse("0f1e2d3c-4b5a-6978-8796-a5b4c3d2e1f0")

    /**
     * A moment in the past, spelled out. `Clock.System.now()` in a fixture is a golden that fails
     * tomorrow, and the log screen prints what it is given.
     */
    val someEvening: Instant = Instant.parse("2026-03-14T18:12:00Z")

    /**
     * The start screen: eight tiles across all eight colour tokens, in the order the parents put
     * them in rather than sorted by anything.
     */
    val homeTiles = listOf(
        HomeTile(
            id = "bedtime",
            title = "Bedtime",
            colorToken = "sage",
            symbolKey = "moon",
            subtitle = TileSubtitle.Steps(count = 5),
        ),
        HomeTile(
            id = "morning",
            title = "Getting up",
            colorToken = "sand",
            symbolKey = "sun",
            subtitle = TileSubtitle.Steps(count = 4),
        ),
        HomeTile(
            id = "lunch",
            title = "Food and drink",
            colorToken = "moss",
            symbolKey = "meal",
            subtitle = TileSubtitle.Custom("No nuts, no cow milk"),
        ),
        HomeTile(
            id = "packing",
            title = "Bag for nursery",
            colorToken = "sky",
            symbolKey = "list",
            subtitle = TileSubtitle.Items(count = 6),
        ),
        HomeTile(
            id = "grandma",
            title = "Grandma Käthe",
            colorToken = "rose",
            symbolKey = "phone",
            subtitle = TileSubtitle.Custom("Mum's mother"),
        ),
        HomeTile(
            id = "calm",
            title = "What helps when he is upset",
            colorToken = "lilac",
            symbolKey = "heart",
            subtitle = TileSubtitle.Steps(count = 3),
        ),
        HomeTile(
            id = "week",
            title = "A day with us",
            colorToken = "clay",
            symbolKey = "clock",
            subtitle = TileSubtitle.None,
        ),
        HomeTile(
            id = "holidays",
            title = "Holidays",
            colorToken = "stone",
            symbolKey = "home",
            subtitle = TileSubtitle.Folder,
        ),
    )

    /** What a search that found something looks like, which is not what an empty grid looks like. */
    val searchResults = homeTiles.filter { it.title.contains("a", ignoreCase = true) }.take(3)

    val guide = CardUiState(
        title = "Bedtime",
        colorToken = "sage",
        isLoading = false,
        payload = CardPayload.Guide(
            steps = listOf(
                Step(text = "Bath, and let him choose the duck."),
                Step(text = "Pyjamas — the blue ones are in the second drawer."),
                Step(text = "Teeth. He will say he has done them. He has not."),
                Step(text = "One story, sitting on the bed, not lying down."),
                Step(text = "Light off, door open a hand's width. He checks."),
            ),
        ),
    )

    val note = CardUiState(
        title = "Food and drink",
        colorToken = "moss",
        isLoading = false,
        payload = CardPayload.Note(
            text = "No nuts and no cow milk — the oat milk in the fridge door is his.\n\n" +
                "He eats slowly. That is normal and not something to hurry.\n\n" +
                "Water with meals. Juice only if he has eaten something first.",
        ),
    )

    /**
     * The same note, on a phone that has a translation app on it.
     *
     * The flattened text is what the ViewModel would have built from the title and the payload.
     * It never reaches the screen — only the hand-off — so it is here to make the control real
     * rather than to be read.
     */
    val noteTranslatable = note.copy(
        canTranslate = true,
        translationText = "Food and drink\n\nNo nuts and no cow milk.",
    )

    /**
     * The same note as a family would see it in Turkish, with the original one chip away.
     *
     * The chips carry endonyms — the original's chip says "Deutsch" because that is what German
     * calls itself, not because a label in `strings.xml` says so. This is the fixture that shows
     * the row at all: every other card golden leaves `languages` empty, which is what keeps them
     * unchanged by this feature.
     */
    val noteTranslated = note.copy(
        title = "Yemek ve içmek",
        payload = CardPayload.Note(
            text = "Fındık yok, inek sütü yok — buzdolabı kapağındaki yulaf sütü onun.\n\n" +
                "Yavaş yer. Bu normaldir, acele ettirmeyin.\n\n" +
                "Yemekle birlikte su. Meyve suyu, ancak bir şeyler yediyse.",
        ),
        languages = listOf(
            TileLanguage(tag = null, name = "Deutsch"),
            TileLanguage(tag = "tr", name = "Türkçe"),
        ),
        shownLanguage = "tr",
    )

    /** And the same again, after somebody edited the original it was translated from. */
    val noteTranslatedStale = noteTranslated.copy(isStaleTranslation = true)

    val checklist = CardUiState(
        title = "Bag for nursery",
        colorToken = "sky",
        isLoading = false,
        payload = CardPayload.Checklist(
            items = listOf(
                CheckItem(text = "Nappies, at least four", done = true),
                CheckItem(text = "Change of clothes", done = true),
                CheckItem(text = "The rabbit — he will not sleep without it", done = false),
                CheckItem(text = "Sun hat", done = false),
                CheckItem(text = "Water bottle", done = false),
                CheckItem(text = "Wellies if it has rained", done = false),
            ),
            resetDaily = true,
        ),
    )

    val table = CardUiState(
        title = "A day with us",
        colorToken = "clay",
        isLoading = false,
        payload = CardPayload.Table(
            columns = listOf("Time", "What happens", "Where"),
            rows = listOf(
                listOf("07:30", "Breakfast", "Kitchen"),
                listOf("09:00", "Nursery", "Ten minutes on foot"),
                listOf("12:30", "Lunch, then a nap", "His room"),
                listOf("15:00", "Snack and the garden", "Outside"),
                listOf("18:00", "Dinner", "Kitchen"),
            ),
        ),
    )

    /**
     * Three people on one tile, which is what a call tile holds now — and what the prototype's
     * grid always said it held, with "Call · 4 numbers". Built through `phoneOf` rather than by
     * hand, so the golden shows a tile stored the way the editor stores one.
     */
    val call = CardUiState(
        title = "Important numbers",
        colorToken = "rose",
        isLoading = false,
        payload = phoneOf(
            listOf(
                PhoneEntry("Käthe Bergmann", "+49 30 1234567", "Mum's mother", inHelpSheet = true),
                PhoneEntry("Dr Keller", "+49 30 7654321", "Paediatrician, Mon-Fri 8-17"),
                PhoneEntry("Frau Adler", "+49 170 2223344", "Next door"),
            ),
        ),
    )

    val website = CardUiState(
        title = "The bus timetable",
        colorToken = "sky",
        isLoading = false,
        payload = CardPayload.Web(url = "https://bvg.de/", label = "Line 142 from the corner"),
    )

    val appLink = CardUiState(
        title = "His music",
        colorToken = "lilac",
        isLoading = false,
        appInstalled = true,
        payload = CardPayload.AppLink(
            packageName = "com.example.music",
            label = "The songs he falls asleep to",
        ),
    )

    /** The same tile on a phone where that app is not installed — an ordinary thing to find. */
    val appLinkMissing = appLink.copy(appInstalled = false)

    val folder = CardUiState(
        title = "Holidays",
        colorToken = "stone",
        isLoading = false,
        payload = CardPayload.Folder(boardId = folderBoardId),
        folderBoardId = folderBoardId.toString(),
        folderTiles = listOf(
            FolderTile(
                id = "beach",
                title = "At the sea",
                colorToken = "sky",
                symbolKey = "sun",
                subtitle = "Three steps",
            ),
            FolderTile(
                id = "packing-list",
                title = "Suitcase",
                colorToken = "sand",
                symbolKey = "list",
                subtitle = "Nine things",
            ),
            FolderTile(
                id = "grandparents",
                title = "At Grandma's",
                colorToken = "moss",
                symbolKey = "home",
                subtitle = null,
            ),
        ),
    )

    /**
     * [CardUiState.mediaPath] is deliberately null on both media fixtures.
     *
     * A golden of a playing video is a golden of one frame of it, and which frame depends on a
     * decoder that Robolectric does not have. What these two pictures are for is the case the
     * screen has to put into words — the row is there and the file is not — which is also the one a
     * device test would never reach on purpose.
     */
    val video = CardUiState(
        title = "How the sling goes on",
        colorToken = "sand",
        isLoading = false,
        payload = CardPayload.Video(
            mediaId = Uuid.parse("1a2b3c4d-5e6f-7081-9203-a4b5c6d7e8f9"),
            caption = "Forty seconds. The knot is the part people get wrong.",
        ),
        mediaPath = null,
    )

    val audio = CardUiState(
        title = "The lullaby",
        colorToken = "lilac",
        isLoading = false,
        payload = CardPayload.Audio(
            mediaId = Uuid.parse("2b3c4d5e-6f70-8192-a3b4-c5d6e7f80912"),
            caption = "Mum singing it, so it is the right words.",
        ),
        mediaPath = null,
    )

    /** A tile that is not there any more: deleted, or a type this build cannot render. */
    val missing = CardUiState(isLoading = false, missing = true)

    /** The numbers behind the help bar, capped as the sheet caps them. */
    val helpContacts = listOf(
        HelpContact(
            cardId = "grandma",
            displayName = "Käthe Bergmann",
            number = "+49 30 1234567",
            relation = "Mum's mother",
        ),
        HelpContact(
            cardId = "mum",
            displayName = "Mum",
            number = "+49 170 9876543",
            relation = null,
        ),
        HelpContact(
            cardId = "doctor",
            displayName = "Dr Ilg, the practice",
            number = "+49 30 7654321",
            relation = "Weekdays until 17:00",
        ),
    )

    /** Newest first, as the screen shows it, mixing what the app wrote with what a person did. */
    val logLines = listOf(
        LogLine(
            id = Uuid.parse("3c4d5e6f-7081-9203-b4c5-d6e7f8091a2b"),
            at = someEvening,
            kind = LogKind.MANUAL_NOTE,
            cardTitle = null,
            note = "He would not eat lunch. Had a banana at four and was fine.",
        ),
        LogLine(
            id = Uuid.parse("4d5e6f70-8192-a3b4-c5d6-e7f8091a2b3c"),
            at = someEvening,
            kind = LogKind.CALL_PREPARED,
            cardTitle = "Grandma Käthe",
            note = null,
        ),
        LogLine(
            id = Uuid.parse("5e6f7081-92a3-b4c5-d6e7-f8091a2b3c4d"),
            at = someEvening,
            kind = LogKind.CHECK_TOGGLED,
            cardTitle = "Bag for nursery",
            note = null,
        ),
        LogLine(
            id = Uuid.parse("6f708192-a3b4-c5d6-e7f8-091a2b3c4d5e"),
            at = someEvening,
            kind = LogKind.CARD_OPENED,
            cardTitle = "Bedtime",
            note = null,
        ),
    )

    /**
     * Stands in for the phone's own date format.
     *
     * The screens take the formatter as a parameter precisely so the platform decides whether a
     * person reads 18:12 or 6:12 pm. A golden cannot have that, so it gets one fixed string —
     * which is also why every log line in [logLines] carries the same timestamp.
     */
    @Suppress("UNUSED_PARAMETER")
    fun formatTime(instant: Instant): String = "14 Mar 2026, 18:12"
}
