package app.larova.core.data.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import app.larova.core.platform.PlatformNames
import kotlinx.coroutines.Dispatchers

/**
 * The Android way to open the database.
 *
 * The bundled SQLite driver is used rather than the one on the device: the version of SQLite that
 * ships with Android varies by manufacturer and by release, and a query that behaves differently on
 * one phone is not something an offline app can debug remotely.
 *
 * No `fallbackToDestructiveMigration` anywhere. If a migration is missing, failing loudly is the
 * correct outcome — the content is not ours to discard. Which migrations there are is
 * `Migrations.kt`, applied through `withLarovaMigrations` rather than listed here, so that the
 * second factory this project gains cannot be written without them.
 */
fun createLarovaDatabase(context: Context): LarovaDatabase =
    Room.databaseBuilder<LarovaDatabase>(
        context = context.applicationContext,
        name = context.getDatabasePath(PlatformNames.DATABASE).absolutePath,
    )
        .withLarovaMigrations()
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
