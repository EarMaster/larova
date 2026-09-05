package app.larova.core.platform

import android.content.Context
import app.larova.core.domain.app.AppLanguage
import java.util.Locale

/**
 * The app's own resolved locale, straight from the configuration.
 *
 * `configuration.locales[0]` rather than `Locale.getDefault()`: on Android 13 and later a per-app
 * language set from the phone's own settings screen shows up in the configuration, and that is the
 * setting this feature exists alongside — a caregiver who put Larova into Turkish expects the tiles
 * to follow.
 */
class AndroidAppLanguage(private val context: Context) : AppLanguage {

    override val current: String
        get() = context.resources.configuration.locales[0].toLanguageTag()

    /**
     * The endonym, capitalised in its own language rather than in the app's.
     *
     * `getDisplayLanguage` is asked for the name *in* that language, which is what makes "Türkçe"
     * come back rather than "Turkish". Capitalising with the same locale matters for the reason
     * `docs/localization.md` §3 already flags: a Turkish `i` uppercases to `İ`, and using the app's
     * locale to do it would produce a word Turkish readers see as misspelt.
     */
    override fun nameOf(tag: String): String {
        val locale = Locale.forLanguageTag(tag)
        val name = locale.getDisplayLanguage(locale)
        return name.replaceFirstChar { it.titlecase(locale) }.ifBlank { tag }
    }
}
