package app.larova.core.data

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import app.larova.core.data.db.MIGRATION_1_2
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * The upgrade path, against real SQLite.
 *
 * This is the one test in the repository whose absence would not show up as a failing build. A
 * database version raised without a migration compiles cleanly, passes every other test, and then
 * throws the first time an *upgraded* installation opens — which is to say on exactly the phones
 * that have a family's tiles on them, and on none of the fresh installs anybody would test with.
 *
 * Both databases are built from the **committed schema JSON** rather than from SQL written here.
 * `1.json` is the only record of what version 1 actually shipped as, and `2.json` is what Room will
 * validate the migrated database against on the next open. Comparing the migration's own output
 * against `2.json` is therefore the same comparison Room makes, made early enough to be a red test
 * instead of a crash on somebody's phone.
 *
 * The fakes elsewhere in this module cannot stand in for any of it: a foreign key that cascades in
 * SQLite is a for-loop in a fake, and a `CREATE TABLE` differing from Room's by one word is
 * invisible to both.
 */
class MigrationTest {

    private val schemaDirectory = File(
        requireNotNull(System.getProperty("larova.schemaDir")) {
            "larova.schemaDir is set in core/data/build.gradle.kts; without it this test cannot " +
                "find the committed schema JSON, and the whole point of it is that it does."
        },
    )

    private val databaseFile =
        File.createTempFile("larova-migration", ".db").also { it.delete() }

    private val driver = BundledSQLiteDriver()

    private var connection: SQLiteConnection? = null

    @AfterTest
    fun cleanUp() {
        connection?.close()
        databaseFile.delete()
    }

    @Test
    fun aTileMadeInVersionOneSurvivesTheUpgradeUntouched() {
        val db = openAtVersionOne()
        db.execSQL("INSERT INTO boards VALUES ('$BOARD_ID', NULL, 'Start', 0, $WHEN)")
        db.execSQL(
            "INSERT INTO cards VALUES ('$CARD_ID', '$BOARD_ID', 'Zähneputzen', 'Jeden Abend', " +
                "'toothbrush', 'sage', 0, 1, 'guide', '$PAYLOAD', NULL, $WHEN)",
        )

        MIGRATION_1_2.migrate(db)

        // Every column by name, so this says what it means and keeps saying it if a column is
        // ever added. A migration that quietly rewrote a tile would pass a row count and fail here,
        // which is the failure worth catching.
        assertEquals(
            mapOf(
                "id" to CARD_ID,
                "boardId" to BOARD_ID,
                "title" to "Zähneputzen",
                "subtitle" to "Jeden Abend",
                "icon" to "toothbrush",
                "colorToken" to "sage",
                "sortIndex" to "0",
                "visibleToCaregiver" to "1",
                "type" to "guide",
                "payload" to PAYLOAD,
                // The migration adds a table; it has no business writing to this one.
                "locale" to null,
                "updatedAtEpochMillis" to WHEN.toString(),
            ),
            rowOf(db, "SELECT * FROM cards WHERE id = '$CARD_ID'"),
        )
    }

    /**
     * The check Room makes on the next open, made here instead.
     *
     * A migration whose `CREATE TABLE` has drifted from the entity — one column order, one missing
     * `NOT NULL` — is an `IllegalStateException` at startup on a phone whose data is otherwise
     * perfectly fine, and nothing before this point would have said so.
     */
    @Test
    fun theTableTheMigrationBuildsIsTheTableRoomExpects() {
        val db = openAtVersionOne()

        MIGRATION_1_2.migrate(db)

        assertEquals(
            expectedSql = createSqlFor(version = 2, table = "card_text"),
            actual = storedSqlFor(db, table = "card_text"),
        )
    }

    /** And the migration adds only that. Every version-1 table is still exactly as it was. */
    @Test
    fun nothingElseAboutTheDatabaseChanges() {
        val db = openAtVersionOne()
        val before = tableNames(db)

        MIGRATION_1_2.migrate(db)

        assertEquals((before + "card_text").sorted(), tableNames(db))
        for (table in before) {
            assertEquals(
                expectedSql = createSqlFor(version = 2, table = table),
                actual = storedSqlFor(db, table = table),
            )
        }
    }

    /**
     * The foreign key, against the real engine. A translation of a tile that is not there is not a
     * translation of anything, and the database is the last place that can still refuse it — an
     * import reading a hand-edited file is what this actually catches.
     */
    @Test
    fun aTranslationOfATileThatIsNotThereIsRefused() {
        val db = openAtVersionOne()
        MIGRATION_1_2.migrate(db)
        db.execSQL("PRAGMA foreign_keys = ON")

        assertFailsWith<Exception> {
            db.execSQL("INSERT INTO card_text VALUES ('$CARD_ID', 'tr', 'Diş', NULL, '$PAYLOAD', $WHEN)")
        }
    }

    /** Deleting a tile takes its translations with it, without anybody remembering to ask. */
    @Test
    fun deletingATileTakesItsTranslationsWithIt() {
        val db = openAtVersionOne()
        MIGRATION_1_2.migrate(db)
        db.execSQL("PRAGMA foreign_keys = ON")
        db.execSQL("INSERT INTO boards VALUES ('$BOARD_ID', NULL, 'Start', 0, $WHEN)")
        db.execSQL(
            "INSERT INTO cards VALUES ('$CARD_ID', '$BOARD_ID', 'Zähneputzen', NULL, " +
                "'toothbrush', 'sage', 0, 1, 'guide', '$PAYLOAD', NULL, $WHEN)",
        )
        db.execSQL("INSERT INTO card_text VALUES ('$CARD_ID', 'tr', 'Diş', NULL, '$PAYLOAD', $WHEN)")

        db.execSQL("DELETE FROM cards WHERE id = '$CARD_ID'")

        val row = db.prepare("SELECT COUNT(*) FROM card_text")
        assertTrue(row.step())
        assertEquals(0, row.getInt(0))
        row.close()
    }

    /**
     * One row, keyed by column name, everything read as text.
     *
     * SQLite hands back an integer's text form happily, and what is being asserted here is that
     * nothing changed rather than what type it changed to — so one map is clearer than twelve
     * positional reads, and it does not go stale when a column is added.
     */
    private fun rowOf(db: SQLiteConnection, sql: String): Map<String, String?> {
        val row = db.prepare(sql)
        assertTrue(row.step(), "no row for: $sql")
        val values = (0 until row.getColumnCount()).associate { column ->
            row.getColumnName(column) to if (row.isNull(column)) null else row.getText(column)
        }
        row.close()
        return values
    }

    /** A database exactly as version 1 shipped, built from the file that recorded it. */
    private fun openAtVersionOne(): SQLiteConnection {
        val db = driver.open(databaseFile.absolutePath)
        connection = db
        for (statement in createStatements(version = 1)) db.execSQL(statement)
        return db
    }

    private fun schema(version: Int) =
        Json.parseToJsonElement(File(schemaDirectory, "$SCHEMA_PACKAGE/$version.json").readText())
            .jsonObject["database"]!!
            .jsonObject

    private fun entities(version: Int) = schema(version)["entities"]!!.jsonArray.map { it.jsonObject }

    private fun createStatements(version: Int): List<String> =
        entities(version).flatMap { entity ->
            val table = entity["tableName"]!!.jsonPrimitive.content
            val create = entity["createSql"]!!.jsonPrimitive.content.forTable(table)
            val indices = entity["indices"]?.jsonArray.orEmpty().map {
                it.jsonObject["createSql"]!!.jsonPrimitive.content.forTable(table)
            }
            listOf(create) + indices
        }

    private fun createSqlFor(version: Int, table: String): String =
        entities(version)
            .first { it["tableName"]!!.jsonPrimitive.content == table }["createSql"]!!
            .jsonPrimitive.content
            .forTable(table)
            .normalized()

    private fun storedSqlFor(db: SQLiteConnection, table: String): String {
        val row = db.prepare("SELECT sql FROM sqlite_master WHERE type = 'table' AND name = ?")
        row.bindText(1, table)
        assertTrue(row.step(), "there is no table called $table")
        val sql = row.getText(0)
        row.close()
        return sql.normalized()
    }

    private fun tableNames(db: SQLiteConnection): List<String> {
        val row = db.prepare(
            "SELECT name FROM sqlite_master WHERE type = 'table' " +
                "AND name NOT LIKE 'sqlite_%' AND name NOT LIKE 'android_%' ORDER BY name",
        )
        val names = buildList { while (row.step()) add(row.getText(0)) }
        row.close()
        return names
    }

    private fun String.forTable(table: String) = replace("\${TABLE_NAME}", table)

    /**
     * SQLite stores the statement it was given, minus `IF NOT EXISTS`, so the two sides are
     * compared with that and with runs of whitespace taken out. Everything that matters — the
     * columns, their order, their types, the key and the foreign key — survives the normalisation.
     */
    private fun String.normalized() = replace("IF NOT EXISTS ", "")
        .replace(Regex("\\s+"), " ")
        .trim()

    private fun assertEquals(expectedSql: String, actual: String) =
        assertEquals<String>(expectedSql, actual, "the migration and the committed schema disagree")

    private companion object {
        const val SCHEMA_PACKAGE = "app.larova.core.data.db.LarovaDatabase"
        const val BOARD_ID = "33333333-3333-3333-3333-333333333333"
        const val CARD_ID = "11111111-1111-1111-1111-111111111111"
        const val WHEN = 1_700_000_000_000
        const val PAYLOAD = """{"type":"guide","steps":[]}"""
    }
}
