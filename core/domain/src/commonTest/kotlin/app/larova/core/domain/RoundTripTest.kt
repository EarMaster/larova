package app.larova.core.domain

import app.larova.core.domain.export.ExportCodec
import app.larova.core.domain.export.ExportManifest
import app.larova.core.domain.export.ImportMode
import app.larova.core.domain.export.PackageIo
import app.larova.core.domain.model.Board
import app.larova.core.domain.model.Card
import app.larova.core.domain.model.CardPayload
import app.larova.core.domain.model.CardPayloadCodec
import app.larova.core.domain.model.CardType
import app.larova.core.domain.model.CheckItem
import app.larova.core.domain.model.MediaAsset
import app.larova.core.domain.model.Step
import app.larova.core.domain.usecase.EnsureRootBoard
import app.larova.core.domain.usecase.ExportPackage
import app.larova.core.domain.usecase.ImportPackage
import app.larova.core.domain.usecase.ReadPackagePreview
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
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
        assertEquals(6, result.cards)
        assertEquals(1, result.media)

        val titles = world.cards.observeAllCards().first().map { it.title }
        assertEquals(
            setOf("Bedtime", "Evening", "Grandma", "Holidays", "The week", "From the future"),
            titles.toSet(),
        )

        // The guide's picture is on disk again, byte for byte.
        assertEquals("a picture", world.mediaFiles.contentOf("media/$MEDIA_ID.jpg"))

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
        assertEquals(6, manifest.counts.cards)
        assertEquals(2, manifest.counts.boards)
        assertEquals(1, manifest.counts.media)
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

    @Test
    fun somethingThatIsNotAPackageIsRefused() = runTest {
        val world = World()
        assertEquals(
            ImportPackage.Result.Unreadable,
            world.import("content://downloads/holiday.jpg", ImportMode.REPLACE),
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
        assertEquals(1, result.mediaCount)
        assertNull(world.store.packages.getValue(destination)["media/gone.jpg"])
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
        val media = FakeMediaRepository()
        val mediaFiles = FakeMediaFiles()
        val store = FakePackageStore(mediaFiles, shelf)
        val io = PackageIo(store = store, digest = FakeDigest(), mediaFiles = mediaFiles)
        private val ensureRoot = EnsureRootBoard(boards)

        suspend fun export(destination: String, label: String? = null) =
            ExportPackage(boards, cards, media, io, APP_VERSION)(destination, label)

        suspend fun import(source: String, mode: ImportMode) =
            ImportPackage(boards, cards, media, io, ensureRoot)(source, mode)

        suspend fun rootId(): Uuid = ensureRoot().id

        /**
         * A small but realistic installation: a guide with a picture, a checklist, a number, and one
         * tile written by a version that knows a type this one does not.
         */
        suspend fun fill() {
            val root = ensureRoot()
            val folder = addFolder(root.id)
            addPicture()
            addTiles(rootId = root.id, folderId = folder.id)
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
        fun wipe() {
            boards.boards.value = emptyList()
            cards.cards.value = emptyList()
            media.assets.value = emptyList()
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
        val AT: Instant = Instant.parse("2026-08-23T18:12:00Z")
    }
}
