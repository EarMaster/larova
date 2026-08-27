package app.larova.core.data.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor

/**
 * The database.
 *
 * `exportSchema` is on and the generated JSON under `core/data/schemas/` is committed. The export
 * container and the database are one compatibility story: a migration that cannot be reconstructed
 * later is a family's content lost, and the schema file is the only record of what version 1
 * actually looked like.
 *
 * Settings are not in here. Appearance, retention and the parent-view PIN are preferences, and
 * none of them belong in an export of somebody's tiles.
 */
@Database(
    entities = [
        BoardEntity::class,
        CardEntity::class,
        MediaAssetEntity::class,
        LogEntryEntity::class,
    ],
    version = LarovaDatabase.VERSION,
    exportSchema = true,
)
@ConstructedBy(LarovaDatabaseConstructor::class)
abstract class LarovaDatabase : RoomDatabase() {

    abstract val boardDao: BoardDao
    abstract val cardDao: CardDao
    abstract val mediaDao: MediaDao
    abstract val logDao: LogDao

    companion object {
        /**
         * Raise this only with a migration beside it, and only after checking that an export from
         * the previous version still imports. Never with `fallbackToDestructiveMigration`: the
         * content is not ours to throw away.
         */
        const val VERSION = 1
    }
}

/**
 * Room generates the actual for each target. The suppression is what the Room documentation
 * prescribes for multiplatform databases — the expect has no hand-written actual by design.
 */
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object LarovaDatabaseConstructor : RoomDatabaseConstructor<LarovaDatabase> {
    override fun initialize(): LarovaDatabase
}
