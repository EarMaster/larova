package app.larova.core.platform

import android.content.Context
import app.larova.core.domain.app.AppLanguage
import app.larova.core.domain.app.LanguageOption
import java.text.Collator
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

    /**
     * Every language the platform can name, once each.
     *
     * `getAvailableLocales` returns hundreds of *locales* — every region of every language — so it
     * is reduced to the primary subtag and deduplicated. That is the level tiles are written at:
     * `resolveCardText` matches primary subtags, and asking a parent to choose between four
     * Spanishes to write one tile is a question with no right answer in it.
     *
     * Sorted with a [Collator] for the app's own locale rather than by code points, so a German
     * list puts Ö where a German reader looks for it. Computed on each call and not cached: the
     * picker is opened rarely, and a cache here would be a copy of the platform's own data that
     * cannot be invalidated when the phone's language changes underneath it.
     */
    override fun available(): List<LanguageOption> {
        val app = context.resources.configuration.locales[0]
        val collator = Collator.getInstance(app)
        return Locale.getAvailableLocales()
            .asSequence()
            .map { it.language }
            .filter { it.isNotBlank() }
            .distinct()
            .map { tag ->
                LanguageOption(
                    tag = tag,
                    endonym = nameOf(tag),
                    localName = Locale.forLanguageTag(tag).getDisplayLanguage(app),
                )
            }
            // A language the platform has a code for but no name for comes back as its own tag,
            // which is not a word anybody is looking for. Dropped rather than shown as "sga".
            .filter { it.endonym != it.tag }
            .sortedWith { a, b -> collator.compare(a.endonym, b.endonym) }
            .toList()
    }
}
