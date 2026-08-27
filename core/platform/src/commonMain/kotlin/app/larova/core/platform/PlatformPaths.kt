package app.larova.core.platform

/**
 * Where this platform keeps the app's private files.
 *
 * Everything Larova writes lives in app-private storage: no other app can read it, no storage
 * permission is needed, and uninstalling removes it. That is a privacy decision as much as a
 * technical one — anything a parent writes about their child is a special category of personal
 * data under GDPR Art. 9.
 *
 * Paths are strings rather than `java.io.File`, because this interface is also the iOS one.
 */
interface PlatformPaths {

    /** `filesDir/media`, holding `<uuid>.<ext>` for every picture, video and recording. */
    fun mediaDirectory(): String

    /** The database file. Named here so an export knows what it is copying next to. */
    fun databaseFile(name: String): String

    /** A preferences file. Settings are deliberately not in the database, and never exported. */
    fun preferencesFile(name: String): String
}

/**
 * Names used on both platforms. They end up in file paths, so they are as fixed as the schema is.
 */
object PlatformNames {
    const val DATABASE = "larova.db"
    const val PREFERENCES = "larova.preferences_pb"
    const val MEDIA_DIRECTORY = "media"
}
