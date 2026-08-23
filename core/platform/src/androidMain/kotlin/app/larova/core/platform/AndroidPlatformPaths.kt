package app.larova.core.platform

import android.content.Context
import java.io.File

/**
 * The Android half of [PlatformPaths]. `filesDir` is app-private: not readable by other apps, not
 * on external storage, and deleted with the app.
 *
 * This is the only file in the module that knows what a `Context` is, which is the reason the
 * module exists at all — the iOS milestone should be a second file beside this one rather than a
 * search through the feature modules.
 */
class AndroidPlatformPaths(private val context: Context) : PlatformPaths {

    override fun mediaDirectory(): String =
        File(context.filesDir, PlatformNames.MEDIA_DIRECTORY).also { it.mkdirs() }.absolutePath

    override fun databaseFile(name: String): String =
        context.getDatabasePath(name).absolutePath

    override fun preferencesFile(name: String): String =
        File(context.filesDir, name).absolutePath
}
