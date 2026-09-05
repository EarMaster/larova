package app.larova.core.data.db

import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

/**
 * The first migration this app has ever needed, and the shape every later one is measured against.
 *
 * One statement, and it only adds. Nothing in `cards` is read, rewritten or dropped, so there is no
 * path through this that can lose a tile — which matters more here than anywhere else in the
 * codebase, because the alternative to a correct migration is not an error message, it is somebody
 * else's child's care instructions.
 *
 * **The SQL is transcribed verbatim from the `createSql` in
 * `schemas/app.larova.core.data.db.LarovaDatabase/2.json`, never typed by hand.** Room validates the
 * migrated database against that file the next time it opens, and a difference of one word — a
 * column order, a missing `NOT NULL` — is an `IllegalStateException` at startup on a phone whose
 * data is otherwise perfectly fine. Copying the generated string is what makes that impossible.
 *
 * `migrate(SQLiteConnection)` and not `migrate(SupportSQLiteDatabase)`. This project supplies a
 * `BundledSQLiteDriver`, and Room's default implementation of the other overload throws — which
 * would be a crash at upgrade time, on exactly the installations that have content.
 */
internal val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `card_text` (" +
                "`cardId` TEXT NOT NULL, " +
                "`lang` TEXT NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`subtitle` TEXT, " +
                "`payload` TEXT NOT NULL, " +
                "`updatedAtEpochMillis` INTEGER NOT NULL, " +
                "PRIMARY KEY(`cardId`, `lang`), " +
                "FOREIGN KEY(`cardId`) REFERENCES `cards`(`id`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
    }
}

/**
 * Every migration, in order, in the one place a platform factory has to call.
 *
 * A function rather than a list each factory applies for itself, because iOS will bring a second
 * factory and one that forgot a migration would open a version-1 database against a version-2
 * schema. Room then throws — correctly, and by this project's explicit refusal to use
 * `fallbackToDestructiveMigration` — and the result is an app that will not start on precisely the
 * phones that have something to lose. Adding a target should be adding a driver, not remembering
 * a list.
 */
internal fun <T : RoomDatabase> RoomDatabase.Builder<T>.withLarovaMigrations():
    RoomDatabase.Builder<T> = addMigrations(MIGRATION_1_2)
