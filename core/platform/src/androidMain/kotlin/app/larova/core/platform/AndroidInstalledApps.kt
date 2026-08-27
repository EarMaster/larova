package app.larova.core.platform

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import app.larova.core.domain.app.InstalledApp
import app.larova.core.domain.app.InstalledApps
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The launcher, asked what it has.
 *
 * `queryIntentActivities` with MAIN/LAUNCHER, which is exactly what the `<queries>` element in the
 * manifest declares. Without that element this returns nothing on Android 11 and later rather than
 * failing, which is the kind of quiet emptiness that looks like a bug in the picker — so the two
 * belong together and the manifest says so.
 *
 * Icons are rendered here and handed over as PNG bytes. A `Drawable` cannot cross into shared code,
 * and an adaptive icon has no bitmap to fetch — it has to be drawn. They are for the picker only:
 * nothing an app draws is ever stored on a tile.
 */
class AndroidInstalledApps(private val context: Context) : InstalledApps {

    override val ownPackageName: String get() = context.packageName

    override suspend fun launchable(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val manager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        manager.queryIntentActivities(intent, 0)
            // An app can have several launcher entries; the parents are choosing an app, not an
            // activity, so the first one per package is the one to offer.
            .distinctBy { it.activityInfo.packageName }
            .map { resolved ->
                InstalledApp(
                    packageName = resolved.activityInfo.packageName,
                    label = resolved.loadLabel(manager).toString(),
                    icon = resolved.loadIcon(manager)?.toPngBytes(),
                )
            }
    }

    /**
     * A launch intent rather than a package lookup.
     *
     * "Installed" is not the question a tile asks — plenty of packages are present and cannot be
     * opened, from disabled apps to ones with no launcher entry at all. What matters is whether
     * tapping the tile will do something.
     */
    override suspend fun isInstalled(packageName: String): Boolean = withContext(Dispatchers.IO) {
        packageName.isNotBlank() &&
            context.packageManager.getLaunchIntentForPackage(packageName) != null
    }

    private fun Drawable.toPngBytes(): ByteArray? {
        val bitmap = Bitmap.createBitmap(ICON_PIXELS, ICON_PIXELS, Bitmap.Config.ARGB_8888)
        try {
            val canvas = Canvas(bitmap)
            setBounds(0, 0, canvas.width, canvas.height)
            draw(canvas)
            return ByteArrayOutputStream().use { out ->
                // Lossless and small at this size, and an app icon with a transparent corner has to
                // stay transparent — a JPEG would fill it in with black.
                if (bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, out)) {
                    out.toByteArray()
                } else {
                    null
                }
            }
        } finally {
            bitmap.recycle()
        }
    }

    private companion object {
        /** Drawn once per app for a list row. Larger would only cost memory on a phone with a hundred. */
        const val ICON_PIXELS = 96

        /** Ignored for PNG, but the parameter is not optional. */
        const val PNG_QUALITY = 100
    }
}
