package app.larova.core.platform

import android.content.Context
import app.larova.core.domain.app.Translators
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Asks the package manager whether anything on this phone will take text to translate.
 *
 * The probe is sent with an empty string rather than the tile's own text: what is being asked is
 * which apps are installed, and the answer cannot depend on what a parent typed. Nothing is
 * launched, so nothing reads it.
 *
 * On `Dispatchers.IO` for the same reason `AndroidInstalledApps` is — `queryIntentActivities` is a
 * binder call to the package manager, and the tile screen asks while it is drawing.
 */
class AndroidTranslators(private val context: Context) : Translators {

    override suspend fun canTranslate(): Boolean = withContext(Dispatchers.IO) {
        TranslateIntents.firstResolvable(
            context.packageManager,
            TranslateIntents.candidates(""),
        ) != null
    }
}
