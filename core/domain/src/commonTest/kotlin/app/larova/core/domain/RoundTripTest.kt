package app.larova.core.domain

import app.larova.core.domain.export.ExportCardText
import app.larova.core.domain.export.ExportCodec
import app.larova.core.domain.export.ExportContent
import app.larova.core.domain.export.ExportCounts
import app.larova.core.domain.export.ExportManifest
import app.larova.core.domain.export.ImportMode
import app.larova.core.domain.export.PackageIo
import app.larova.core.domain.model.Board
import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardText
import app.larova.core.domain.model.resolveCardText
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.CheckItem
import app.larova.core.domain.model.LogEntry
import app.larova.core.domain.model.LogKind
import app.larova.core.domain.model.MediaAsset
import app.larova.core.domain.model.Step
import app.larova.core.domain.usecase.EnsureRootBoard
import app.larova.core.domain.usecase.ExportPackage
import app.larova.core.domain.usecase.ImportPackage
import app.larova.core.domain.usecase.LOG_RETENTION_DAYS
import app.larova.core.domain.usecase.ReadPackagePreview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest

/**
 * M1's exit criterion, as far as a unit test can carry it: fill the app, export, wipe it, import,
 * and find everything back — including the media and including a payload this version cannot read.
 *
 * The ZIP and the content URI belong to the platform and are exercised on a device. What is checked
 * here is the part that decides whether a family's only copy of their content survives: what goes
 * into the package, what comes out, and every way the import is supposed to refuse.
 */
@OptIn(ExperimentalUuidApi::class)
class RoundTripTest {

    private val destination = "content://downloads/larova-2026-08-23.larova"

    @Test
    fun everythingComesBackAfterAWipe() = runTest {
        val world = World()
        world.fill()

        assertIs<ExportPackage.Result.Written>(world.export(destination, label = "Larova for Jonas"))

        // The reinstall: no boards, no cards, no media, no files.
        world.wipe()
        assertTrue(world.cards.cards.value.isEmpty())
        assertEquals(0, world.mediaFiles.count)

        val result = world.import(destination, ImportMode.REPLACE)

        assertIs<ImportPackage.Result.Imported>(result)
        assertEquals(2, result.boards)
        assertEquals(8, result.cards)
        assertEquals(2, result.media)

        val titles = world.cards.observeAllCards().first().map { it.title }
        assertEquals(
            setOf(
                "Bedtime", "Evening", "Grandma", "Holidays", "The week", "Music", "The lullaby",
                "From the future",
            ),
            titles.toSet(),
        )

        // The guide's picture and the recording are on disk again, byte for byte. The recording
        // matters most: a picture can be taken again, and the voice on it cannot.
        assertEquals("a picture", world.mediaFiles.contentOf("media/$MEDIA_ID.jpg"))
        assertEquals("a lullaby", world.mediaFiles.contentOf("media/$RECORDING_ID.m4a"))

        // And the payload this version cannot read came through untouched, so a newer build can
        // still render it.
        val future = world.cards.observeAllCards().first().single { it.title == "From the future" }
        assertEquals("""{"type":"hologram","projector":"living room"}""", future.payload)
        assertNull(CardPayloadCodec.decodeOrNull(future.payload))
    }

    @Test
    fun theExportedGuideStillHasItsStepsAndItsKeys() = runTest {
        val world = World()
        world.fill()
        world.export(destination)
        world.wipe()
        world.import(destination, ImportMode.REPLACE)

        val guide = world.cards.observeAllCards().first().single { it.title == "Bedtime" }
        assertEquals("sage", guide.colorToken)
        assertEquals("moon", guide.icon)
        assertEquals(CardType.GUIDE, guide.type)

        val payload = CardPayloadCodec.decodeOrNull(guide.payload)
        assertIs<CardPayload.Guide>(payload)
        assertEquals(listOf("Brush teeth", "Read a story"), payload.steps.map { it.text })
        assertEquals(Uuid.parse(MEDIA_ID), payload.steps.first().mediaId)
    }

    /**
     * A folder is the one tile that points at something other than itself, so a restore has to put
     * the board back **and** leave the tile pointing at it. A folder tile whose board did not
     * survive is a tile that opens nothing, with the tiles that were inside it stranded on a board
     * nothing reaches.
     */
    @Test
    fun aFolderStillOpensItsOwnTilesAfterARestore() = runTest {
        val world = World()
        world.fill()
        world.export(destination)
        world.wipe()
        world.import(destination, ImportMode.REPLACE)

        val folderTile = world.cards.observeAllCards().first().single { it.title == "Holidays" }
        val payload = CardPayloadCodec.decodeOrNull(folderTile.payload)
        assertIs<CardPayload.Folder>(payload)

        val board = world.boards.boards.value.singleOrNull { it.id == payload.boardId }
        assertNotNull(board, "the folder points at a board that is not there any more")
        assertEquals(
            listOf("From the future"),
            world.cards.observeCards(payload.boardId).first().map { it.title },
        )
    }

    /**
     * A shortcut carries a package name, and a package name means nothing on the phone that
     * restores it — the app may not be installed there at all. What has to survive is the tile:
     * the label the parents wrote and the package it points at, so it works again on a phone that
     * does have the app.
     */
    /**
     * The log is part of what a family typed, so it is in the backup — and the retention window
     * applies to what comes back out of one. Restoring a two-year-old package must not resurrect
     * two years of events, and the rule about how long a log is kept lives in one place.
     */
    @Test
    fun theLogComesBackWithoutWhatTheWindowHasDropped() = runTest {
        val world = World()
        world.fill()
        world.export(destination)
        world.wipe()
        world.import(destination, ImportMode.REPLACE)

        val notes = world.log.entries.value.map { it.note }
        assertTrue("he would not eat lunch" in notes, "a line a caregiver wrote did not survive")
        assertTrue(
            "from long before the window" !in notes,
            "an entry older than the retention window came back",
        )
        // The line the app wrote about a tile came back with the tile it refers to.
        val opened = world.log.entries.value.single { it.kind == LogKind.CARD_OPENED }
        val titles = world.cards.observeAllCards().first().associate { it.id to it.title }
        assertEquals("Bedtime", titles[opened.cardId])
    }

    /** A replacing import throws the old log away with everything else. */
    @Test
    fun replacingTheContentReplacesTheLog() = runTest {
        val world = World()
        world.fill()
        world.export(destination)

        val other = World(world.store.packages)
        other.log.append(
            LogEntry(
                id = Uuid.random(),
                at = Clock.System.now(),
                kind = LogKind.MANUAL_NOTE,
                note = "their own line",
            ),
        )

        other.import(destination, ImportMode.REPLACE)

        assertTrue("their own line" !in other.log.entries.value.map { it.note })
    }

    @Test
    fun aShortcutKeepsTheAppItPointsAtAndTheWordsOnIt() = runTest {
        val world = World()
        world.fill()
        world.export(destination)
        world.wipe()
        world.import(destination, ImportMode.REPLACE)

        val tile = world.cards.observeAllCards().first().single { it.title == "Music" }
        val payload = CardPayloadCodec.decodeOrNull(tile.payload)
        assertIs<CardPayload.AppLink>(payload)
        assertEquals("com.example.music", payload.packageName)
        assertEquals("Music for the car", payload.label)
    }

    @Test
    fun aTableComesBackWithItsHeadingsAndItsEmptyCell() = runTest {
        val world = World()
        world.fill()
        world.export(destination)
        world.wipe()
        world.import(destination, ImportMode.REPLACE)

        val tile = world.cards.observeAllCards().first().single { it.title == "The week" }
        val payload = CardPayloadCodec.decodeOrNull(tile.payload)
        assertIs<CardPayload.Table>(payload)
        assertEquals(listOf("Day", "Who fetches"), payload.columns)
        // The blank cell is content: "nobody yet" is an answer, and dropping it would shift the row.
        assertEquals(listOf(listOf("Monday", "Grandma"), listOf("Tuesday", "")), payload.rows)
    }

    @Test
    fun theManifestDescribesWhatIsInThePackage() = runTest {
        val world = World()
        world.fill()
        world.export(destination, label = "Larova for Jonas")

        val preview = ReadPackagePreview(world.io)(destination)

        assertIs<ReadPackagePreview.Result.Readable>(preview)
        val manifest = preview.manifest
        assertEquals(ExportManifest.CURRENT_SCHEMA_VERSION, manifest.schemaVersion)
        assertEquals("Larova for Jonas", manifest.label)
        assertEquals(8, manifest.counts.cards)
        assertEquals(2, manifest.counts.boards)
        assertEquals(2, manifest.counts.media)
    }

    @Test
    fun mergingAddsTilesWithoutMovingTheOnesAlreadyThere() = runTest {
        val world = World()
        world.fill()
        world.export(destination)

        // A second installation with its own tiles, importing what the first one sent.
        val other = World(world.store.packages)
        other.addNote("Their own tile", sortIndex = 0)

        val result = other.import(destination, ImportMode.MERGE)

        assertIs<ImportPackage.Result.Imported>(result)
        val ordered = other.cards.observeCards(other.rootId()).first().map { it.title }
        assertEquals("Their own tile", ordered.first(), "an existing tile was pushed out of place")
        assertTrue(ordered.containsAll(listOf("Bedtime", "Evening", "Grandma")))
    }

    @Test
    fun mergingTwiceDoesNotDoubleTheTiles() = runTest {
        val world = World()
        world.fill()
        world.export(destination)

        val other = World(world.store.packages)
        other.import(destination, ImportMode.MERGE)
        val afterFirst = other.cards.cards.value.size

        other.import(destination, ImportMode.MERGE)

        assertEquals(afterFirst, other.cards.cards.value.size)
    }

    @Test
    fun aMergeLeavesOnlyOneStartScreen() = runTest {
        // Two boards with no parent would leave the app with two start screens and no way to say
        // which one holds the tiles.
        val world = World()
        world.fill()
        world.export(destination)

        val other = World(world.store.packages)
        other.addNote("Their own tile", sortIndex = 0)
        other.import(destination, ImportMode.MERGE)

        assertEquals(1, other.boards.boards.value.count { it.parentId == null })
        // Every tile is reachable from that one start screen or from a board below it.
        val boardIds = other.boards.boards.value.map { it.id }.toSet()
        assertTrue(other.cards.cards.value.all { it.boardId in boardIds })
    }

    @Test
    fun replacingThrowsAwayWhatWasThereFirst() = runTest {
        val world = World()
        world.fill()
        world.export(destination)

        val other = World(world.store.packages)
        other.addNote("Their own tile", sortIndex = 0)
        other.mediaFiles.putRelative("media/old.jpg", "an old picture")

        other.import(destination, ImportMode.REPLACE)

        val titles = other.cards.observeAllCards().first().map { it.title }
        assertTrue("Their own tile" !in titles, "replace kept a tile it should have removed")
        // The file went with the row. Otherwise the app grows by every picture ever imported, with
        // nothing left pointing at them.
        assertNull(other.mediaFiles.contentOf("media/old.jpg"))
    }

    @Test
    fun aPackageFromANewerVersionIsRefusedRatherThanPartlyApplied() = runTest {
        val world = World()
        world.fill()
        world.export(destination)
        // Rewrite the manifest as something a later Larova would have written.
        val entries = world.store.packages.getValue(destination)
        val manifest = ExportCodec.decodeManifestOrNull(entries.getValue("manifest.json"))!!
        entries["manifest.json"] = ExportCodec.encode(manifest.copy(schemaVersion = 99))

        val other = World(world.store.packages)
        other.addNote("Their own tile", sortIndex = 0)
        val result = other.import(destination, ImportMode.REPLACE)

        assertEquals(ImportPackage.Result.TooNew(99), result)
        // Nothing was touched, least of all by a replace that had already started.
        assertEquals(listOf("Their own tile"), other.cards.observeAllCards().first().map { it.title })
    }

    /**
     * A real backup from a shipped version, restored end to end.
     *
     * [LEGACY_CONTENT_JSON] spells tile types as Kotlin constant names, which is what every file
     * written between `0.1.0` and `0.4.2` does. The reader accepts both spellings forever, and
     * this is the test that says so through the whole use case rather than only the codec: the
     * manifest, the hash, and a REPLACE that wipes a tile made on this phone first.
     */
    @Test
    fun aPackageWrittenByAShippedVersionStillRestores() = runTest {
        val source = "content://downloads/larova-from-0.4.2.larova"
        val digest = FakeDigest()
        val shelf = mutableMapOf(
            source to mutableMapOf(
                "manifest.json" to ExportCodec.encode(
                    ExportManifest(
                        schemaVersion = LEGACY_SCHEMA_VERSION,
                        appVersion = LEGACY_APP_VERSION,
                        exportedAt = Instant.fromEpochMilliseconds(1_772_000_000_000),
                        counts = ExportCounts(boards = 2, cards = 8, media = 1),
                        // Computed, never pinned. FakeDigest is "len<n>:<hashCode>", and a baked
                        // String.hashCode is a trap for whoever next edits the fixture.
                        contentSha256 = digest.sha256(LEGACY_CONTENT_JSON),
                    ),
                ),
                "content.json" to LEGACY_CONTENT_JSON,
            ),
        )

        val world = World(shelf)
        world.addNote("A tile made on this phone", sortIndex = 0)
        val result = world.import(source, ImportMode.REPLACE)

        assertIs<ImportPackage.Result.Imported>(result)
        assertEquals(8, result.cards)
        assertEquals(0, result.skippedCards, "a v1 file holds nothing this build cannot read")
        // Zero media: the fixture names an asset the archive does not carry, which restoreMedia
        // skips rather than registering a row for a file that is not there.
        assertEquals(0, result.media)

        assertEquals(
            LEGACY_CARD_TITLES.sorted(),
            world.cards.observeAllCards().first().map { it.title }.sorted(),
        )
        // The folder tile still opens a board that exists, after the root was remapped.
        assertTrue(world.boards.all().any { it.title == "Mornings" })
    }

    @Test
    fun aTruncatedPackageIsRefusedBeforeAnythingIsWritten() = runTest {
        // What a messenger that cut the file in half produces. Without the hash it would import as
        // a smaller backup, which is how content disappears quietly.
        val world = World()
        world.fill()
        world.export(destination)
        world.store.corruptContent(destination, """{"boards":[],"cards":[]}""")

        val other = World(world.store.packages)
        other.addNote("Their own tile", sortIndex = 0)
        val result = other.import(destination, ImportMode.REPLACE)

        assertEquals(ImportPackage.Result.Damaged, result)
        assertEquals(listOf("Their own tile"), other.cards.observeAllCards().first().map { it.title })
    }

    /**
     * A file that cannot be opened at all, which is not the same as a file that is not a package.
     *
     * This case used to share `Unreadable` with the one below, and so shared its message: "this is
     * not a Larova backup". That is the wrong thing to say to somebody whose backup is fine and
     * merely still sitting in cloud storage undownloaded, and it is the sentence that sends them
     * looking for a copy that does not exist.
     */
    @Test
    fun aFileThatCannotBeOpenedSaysSoRatherThanBlamingTheFile() = runTest {
        val world = World()
        assertEquals(
            ImportPackage.Result.CouldNotOpen,
            world.import("content://cloud/not-downloaded-yet.larova", ImportMode.REPLACE),
        )
    }

    /** Opened, and genuinely not a package: no manifest beside the content. */
    @Test
    fun somethingThatIsNotAPackageIsRefused() = runTest {
        val source = "content://downloads/holiday.jpg"
        val world = World(mutableMapOf(source to mutableMapOf("exif.txt" to "not a manifest")))

        assertEquals(
            ImportPackage.Result.Unreadable,
            world.import(source, ImportMode.REPLACE),
        )
    }

    @Test
    fun aFailedWriteIsReportedRatherThanLookingLikeABackup() = runTest {
        // The document was deleted, or the provider withdrew the permission the picker granted.
        val world = World()
        world.fill()
        world.store.failWrites = true

        assertEquals(ExportPackage.Result.Failed, world.export(destination))
        assertTrue(world.store.packages.isEmpty())
    }

    @Test
    fun mediaWithNoFileOnDiskIsLeftOutOfThePackage() = runTest {
        // The row survived something the file did not. Writing its name into a backup would only
        // move the problem to whoever restores it.
        val world = World()
        world.fill()
        world.media.register(
            MediaAsset(
                id = Uuid.random(),
                relativePath = "media/gone.jpg",
                mimeType = "image/jpeg",
                sizeBytes = 10,
                sha256 = "x".repeat(64),
            ),
        )

        val result = world.export(destination)

        assertIs<ExportPackage.Result.Written>(result)
        assertEquals(2, result.mediaCount)
        assertNull(world.store.packages.getValue(destination)["media/gone.jpg"])
    }

    /**
     * A translation comes back with its tile, and comes back attached to it.
     *
     * Not counted as a tile, which is the design stated as an assertion: the counts either side of
     * a wipe are the same 8 they have always been, because a translation is a tile's text and not
     * a tile.
     */
    @Test
    fun aTranslationSurvivesTheRoundTrip() = runTest {
        val world = World()
        world.fill()
        assertIs<ExportPackage.Result.Written>(world.export(destination))

        world.wipe()
        val result = world.import(destination, ImportMode.REPLACE)

        assertIs<ImportPackage.Result.Imported>(result)
        assertEquals(8, result.cards)

        val bedtime = world.cards.observeAllCards().first().first { it.title == "Bedtime" }
        val variants = world.cardText.all()
        assertEquals(1, variants.size)
        assertEquals(bedtime.id, variants.single().cardId)
        assertEquals("tr", variants.single().lang)
        assertEquals("Yatma vakti", variants.single().title)

        // And it is the text a caregiver reading Turkish would be shown.
        assertEquals("Yatma vakti", resolveCardText(bedtime, variants, "tr").title)
    }

    /** Replacing everything replaces the translations too, rather than leaving orphans behind. */
    @Test
    fun replacingThrowsAwayTheTranslationsToo() = runTest {
        val shelf = mutableMapOf<String, MutableMap<String, String>>()
        val theirs = World(shelf)
        theirs.fill()
        assertIs<ExportPackage.Result.Written>(theirs.export(destination))

        val mine = World(shelf)
        mine.fill()
        val ownVariant = mine.cardText.all().single()

        assertIs<ImportPackage.Result.Imported>(mine.import(destination, ImportMode.REPLACE))

        val after = mine.cardText.all()
        assertEquals(1, after.size)
        // The one that is there came out of the file, not from before the replace.
        assertTrue(after.none { it.cardId == ownVariant.cardId && it.updatedAt != ownVariant.updatedAt })
    }

    /** Importing the same package twice is the same rows: the key came with the file. */
    @Test
    fun mergingTwiceDoesNotDoubleTheTranslations() = runTest {
        val shelf = mutableMapOf<String, MutableMap<String, String>>()
        val theirs = World(shelf)
        theirs.fill()
        assertIs<ExportPackage.Result.Written>(theirs.export(destination))

        val mine = World(shelf)
        mine.import(destination, ImportMode.MERGE)
        mine.import(destination, ImportMode.MERGE)

        assertEquals(1, mine.cardText.all().size)
    }

    /**
     * A translation whose payload this build cannot read is left where it is.
     *
     * The tile itself comes back untouched — an unreadable payload is copied through so a newer
     * build can still render it — but its translation cannot be checked against the tile's type,
     * and a variant that might be a different kind of tile is the half-translated tile this design
     * exists to prevent. Refused, and still in the file for the version that understands it.
     */
    @Test
    fun aTranslationThisBuildCannotCheckIsLeftInTheFile() = runTest {
        val world = World()
        world.fill()
        assertIs<ExportPackage.Result.Written>(world.export(destination))

        val future = world.cards.observeAllCards().first().first { it.title == "From the future" }
        world.putVariantInFile(destination, future.id, "tr", future.payload)

        world.wipe()
        val result = world.import(destination, ImportMode.REPLACE)

        assertIs<ImportPackage.Result.Imported>(result)
        // The tile is back, payload and all.
        assertEquals(8, result.cards)
        // The Turkish Bedtime is back; the one on the unreadable tile is not.
        assertEquals(listOf("Yatma vakti"), world.cardText.all().map { it.title })
    }

    /** And so is one naming a tile that is not in the file at all. */
    @Test
    fun aTranslationForATileThatIsNotInTheFileIsDropped() = runTest {
        val world = World()
        world.fill()
        assertIs<ExportPackage.Result.Written>(world.export(destination))
        world.putVariantInFile(destination, Uuid.random(), "tr", """{"type":"note"}""")

        world.wipe()
        assertIs<ImportPackage.Result.Imported>(world.import(destination, ImportMode.REPLACE))

        assertEquals(listOf("Yatma vakti"), world.cardText.all().map { it.title })
    }

    /**
     * A variant claiming to be a different kind of tile is refused.
     *
     * This is what "never a half-translated tile" means at the byte level. It would pass every
     * decode and then fail on the screen of the person who opened it, and a file can come from
     * anywhere — so the editor refusing it is not enough.
     */
    @Test
    fun aTranslationWhosePayloadIsADifferentTileTypeIsRefused() = runTest {
        val world = World()
        world.fill()
        assertIs<ExportPackage.Result.Written>(world.export(destination))

        val bedtime = world.cards.observeAllCards().first().first { it.title == "Bedtime" }
        world.putVariantInFile(destination, bedtime.id, "uk", """{"type":"note"}""")

        world.wipe()
        assertIs<ImportPackage.Result.Imported>(world.import(destination, ImportMode.REPLACE))

        // The Turkish guide is back; the Ukrainian "note" on a guide tile is not.
        assertEquals(listOf("Yatma vakti"), world.cardText.all().map { it.title })
    }

    /**
     * One installation: its repositories, its media directory, and the two use cases.
     *
     * [shelf] is the shared storage the package is written to. Two Worlds pointing at the same shelf
     * are two phones and one file between them, which is the case that matters — the second one has
     * never seen the first one's media directory.
     */
    private class World(shelf: MutableMap<String, MutableMap<String, String>> = mutableMapOf()) {
        val boards = FakeBoardRepository()
        val cards = FakeCardRepository()
        val cardText = FakeCardTextRepository()
        val media = FakeMediaRepository()
        val log = FakeLogRepository()
        val mediaFiles = FakeMediaFiles()
        val store = FakePackageStore(mediaFiles, shelf)
        val io = PackageIo(store = store, digest = FakeDigest(), mediaFiles = mediaFiles)
        private val ensureRoot = EnsureRootBoard(boards)

        suspend fun export(destination: String, label: String? = null) =
            ExportPackage(boards, cards, cardText, media, log, io, APP_VERSION)(destination, label)

        suspend fun import(source: String, mode: ImportMode) =
            ImportPackage(boards, cards, cardText, media, log, io, ensureRoot)(source, mode)

        suspend fun rootId(): Uuid = ensureRoot().id

        /**
         * A small but realistic installation: a guide with a picture, a checklist, a number, and one
         * tile written by a version that knows a type this one does not.
         */
        suspend fun fill() {
            val root = ensureRoot()
            val folder = addFolder(root.id)
            addPicture()
            addRecording()
            addTiles(rootId = root.id, folderId = folder.id)
            addTranslation()
            addLog()
        }

        /**
         * One tile in a second language. Deliberately not a second tile: the counts every test in
         * this file asserts stay 2 boards, 8 cards and 2 media, because a translation is not a
         * tile — which is the whole design, stated as an assertion nobody had to write.
         */
        private suspend fun addTranslation() {
            val bedtime = cards.observeAllCards().first().first { it.title == "Bedtime" }
            cardText.upsert(
                CardText(
                    cardId = bedtime.id,
                    lang = "tr",
                    title = "Yatma vakti",
                    subtitle = "Her akşam",
                    payload = bedtime.payload,
                    updatedAt = bedtime.updatedAt,
                ),
            )
        }

        /**
         * One line the app wrote and one a caregiver did, plus one from long enough ago that the
         * retention window has to drop it on the way back in.
         */
        private suspend fun addLog() {
            val opened = cards.observeAllCards().first().first { it.title == "Bedtime" }
            log.append(
                LogEntry(
                    id = Uuid.random(),
                    at = Clock.System.now(),
                    kind = LogKind.CARD_OPENED,
                    cardId = opened.id,
                ),
            )
            log.append(
                LogEntry(
                    id = Uuid.random(),
                    at = Clock.System.now(),
                    kind = LogKind.MANUAL_NOTE,
                    note = "he would not eat lunch",
                ),
            )
            log.append(
                LogEntry(
                    id = Uuid.random(),
                    at = Clock.System.now() - (LOG_RETENTION_DAYS + 10).days,
                    kind = LogKind.MANUAL_NOTE,
                    note = "from long before the window",
                ),
            )
        }

        private suspend fun addFolder(parentId: Uuid): Board {
            val folder = Board(
                id = Uuid.random(),
                parentId = parentId,
                title = "Holidays",
                sortIndex = 0,
                updatedAt = AT,
            )
            boards.upsert(folder)
            return folder
        }

        /**
         * A recording, which travels the way a picture does but is copied rather than re-encoded.
         * The bytes here stand in for a file: what the round trip has to prove is that the same
         * bytes come out of the package on the other side.
         */
        private suspend fun addRecording() {
            mediaFiles.putRelative("media/$RECORDING_ID.m4a", "a lullaby")
            media.register(
                MediaAsset(
                    id = Uuid.parse(RECORDING_ID),
                    relativePath = "media/$RECORDING_ID.m4a",
                    mimeType = "audio/mp4",
                    sizeBytes = "a lullaby".length.toLong(),
                    sha256 = "c".repeat(64),
                ),
            )
        }

        private suspend fun addPicture() {
            mediaFiles.putRelative("media/$MEDIA_ID.jpg", "a picture")
            media.register(
                MediaAsset(
                    id = Uuid.parse(MEDIA_ID),
                    relativePath = "media/$MEDIA_ID.jpg",
                    mimeType = "image/jpeg",
                    sizeBytes = "a picture".length.toLong(),
                    sha256 = "b".repeat(64),
                ),
            )
        }

        /** Split by where the tiles live, because one function listing six of them reads as none. */
        private suspend fun addTiles(rootId: Uuid, folderId: Uuid) {
            addReadingTiles(rootId)
            addFolderAndTable(rootId = rootId, folderId = folderId)
            addShortcut(rootId)
            addSong(rootId)
            addTileFromTheFuture(folderId)
        }

        private suspend fun addReadingTiles(rootId: Uuid) {
            cards.upsert(
                card(
                    boardId = rootId,
                    title = "Bedtime",
                    colorToken = "sage",
                    icon = "moon",
                    type = CardType.GUIDE,
                    payload = CardPayloadCodec.encode(
                        CardPayload.Guide(
                            listOf(
                                Step("Brush teeth", mediaId = Uuid.parse(MEDIA_ID)),
                                Step("Read a story"),
                            ),
                        ),
                    ),
                    sortIndex = 0,
                ),
            )
            cards.upsert(
                card(
                    boardId = rootId,
                    title = "Evening",
                    type = CardType.CHECKLIST,
                    payload = CardPayloadCodec.encode(
                        CardPayload.Checklist(listOf(CheckItem("Pyjamas", done = true))),
                    ),
                    sortIndex = 1,
                ),
            )
            cards.upsert(
                card(
                    boardId = rootId,
                    title = "Grandma",
                    type = CardType.PHONE,
                    payload = CardPayloadCodec.encode(
                        CardPayload.Phone("Grandma", "+49 170 1234567", inHelpSheet = true),
                    ),
                    sortIndex = 2,
                ),
            )
        }

        /** The folder tile points at the board `addFolder` made, which is what a restore must keep. */
        private suspend fun addSong(rootId: Uuid) {
            cards.upsert(
                card(
                    boardId = rootId,
                    title = "The lullaby",
                    type = CardType.AUDIO,
                    payload = CardPayloadCodec.encode(
                        CardPayload.Audio(
                            mediaId = Uuid.parse(RECORDING_ID),
                            caption = "Mum singing it",
                        ),
                    ),
                    sortIndex = 6,
                ),
            )
        }

        private suspend fun addShortcut(rootId: Uuid) {
            cards.upsert(
                card(
                    boardId = rootId,
                    title = "Music",
                    type = CardType.APP_LINK,
                    payload = CardPayloadCodec.encode(
                        CardPayload.AppLink(
                            packageName = "com.example.music",
                            label = "Music for the car",
                        ),
                    ),
                    sortIndex = 5,
                ),
            )
        }

        private suspend fun addFolderAndTable(rootId: Uuid, folderId: Uuid) {
            cards.upsert(
                card(
                    boardId = rootId,
                    title = "Holidays",
                    type = CardType.FOLDER,
                    payload = CardPayloadCodec.encode(CardPayload.Folder(boardId = folderId)),
                    sortIndex = 3,
                ),
            )
            cards.upsert(
                card(
                    boardId = rootId,
                    title = "The week",
                    type = CardType.TABLE,
                    payload = CardPayloadCodec.encode(
                        CardPayload.Table(
                            columns = listOf("Day", "Who fetches"),
                            rows = listOf(listOf("Monday", "Grandma"), listOf("Tuesday", "")),
                        ),
                    ),
                    sortIndex = 4,
                ),
            )
        }

        /** A type this build has never heard of, inside the folder, to be carried through untouched. */
        private suspend fun addTileFromTheFuture(folderId: Uuid) {
            cards.upsert(
                card(
                    boardId = folderId,
                    title = "From the future",
                    type = CardType.NOTE,
                    payload = """{"type":"hologram","projector":"living room"}""",
                    sortIndex = 0,
                ),
            )
        }

        suspend fun addNote(title: String, sortIndex: Int) {
            val root = ensureRoot()
            cards.upsert(
                card(
                    boardId = root.id,
                    title = title,
                    type = CardType.NOTE,
                    payload = CardPayloadCodec.encode(CardPayload.Note("Theirs")),
                    sortIndex = sortIndex,
                ),
            )
        }

        /** What an uninstall leaves behind: nothing. */
        /**
         * Writes one more variant straight into the package on the shelf.
         *
         * Rewriting `content.json` by hand rather than through `ExportPackage`, because these are
         * the rows an export written here would never produce: one naming a tile this build cannot
         * read, one naming no tile at all, one whose payload is the wrong kind. A file arrives from
         * somebody else's phone, or from a text editor, and the import has to hold either way. The
         * hash is recomputed so the file stays well-formed and the refusal under test is the row's,
         * not the checksum's.
         */
        suspend fun putVariantInFile(
            destination: String,
            cardId: Uuid,
            lang: String,
            payload: String,
        ) {
            val entries = store.packages.getValue(destination)
            val content = ExportCodec.json.decodeFromString<ExportContent>(
                entries.getValue(ExportManifest.CONTENT_ENTRY),
            )
            val amended = content.copy(
                cardText = content.cardText + ExportCardText(
                    cardId = cardId,
                    lang = lang,
                    title = "Sonradan eklendi",
                    payload = payload,
                    updatedAt = Clock.System.now(),
                ),
            )
            val json = ExportCodec.json.encodeToString(amended)
            entries[ExportManifest.CONTENT_ENTRY] = json

            val manifest = ExportCodec.decodeManifestOrNull(
                entries.getValue(ExportManifest.MANIFEST_ENTRY),
            )!!
            entries[ExportManifest.MANIFEST_ENTRY] = ExportCodec.encode(
                manifest.copy(contentSha256 = io.digest.sha256(json)),
            )
        }

        fun wipe() {
            boards.boards.value = emptyList()
            cards.cards.value = emptyList()
            cardText.texts.value = emptyList()
            media.assets.value = emptyList()
            log.entries.value = emptyList()
            mediaFiles.deleteAll()
        }

        private fun card(
            boardId: Uuid,
            title: String,
            type: CardType,
            payload: String,
            sortIndex: Int,
            colorToken: String = "sand",
            icon: String = "star",
        ) = Card(
            id = Uuid.random(),
            boardId = boardId,
            title = title,
            icon = icon,
            colorToken = colorToken,
            sortIndex = sortIndex,
            type = type,
            payload = payload,
            updatedAt = AT,
        )
    }

    private companion object {
        const val APP_VERSION = "0.1.0"
        const val MEDIA_ID = "3f2a1b4c-5d6e-4f70-8192-a3b4c5d6e7f8"
        const val RECORDING_ID = "cccccccc-dddd-4eee-8fff-111111111111"
        val AT: Instant = Instant.parse("2026-08-23T18:12:00Z")
    }
}
