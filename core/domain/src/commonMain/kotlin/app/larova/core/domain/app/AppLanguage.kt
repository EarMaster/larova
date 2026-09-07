package app.larova.core.domain.app

/**
 * The language Android resolved for this process, and what a language calls itself.
 *
 * A port for the same reason [InstalledApps] is one: both answers come from the platform, and the
 * grid resolves a whole board inside a ViewModel where the composition's locale is out of reach.
 *
 * Nothing here is stored. The app's language is Android's business — set from the phone's own
 * per-app language screen, which the settings row opens — and this only reads it.
 */
interface AppLanguage {

    /** A BCP-47 tag, e.g. `de-AT`. Whatever the phone resolved; never derived from anything here. */
    val current: String

    /**
     * A language's own name for itself: "Türkçe", never "Turkish".
     *
     * The endonym is the only correct answer, and it also keeps invariant 2 honest. A Turkish
     * caregiver must recognise their language whatever language the app's chrome is in, and the
     * platform's own data supplies it — so no language name ever enters `strings.xml`, where it
     * would need translating into fourteen languages and would still be wrong for this purpose.
     */
    fun nameOf(tag: String): String

    /**
     * Every language this phone can name, for the picker a parent writes a tile in.
     *
     * Deliberately far wider than the fourteen the app's own chrome speaks. Nothing in `Card.locale`
     * or in `resolveCardText` cares which languages Larova was translated into — a family with a
     * Romanian carer needs a Romanian tile whether or not the buttons around it are Romanian, and
     * the buttons are the half that already falls back.
     *
     * Language level only, no regions: `resolveCardText` matches on the primary subtag anyway, and
     * a list carrying four Spanishes is a list nobody can find Spanish in.
     */
    fun available(): List<LanguageOption>
}

/**
 * One language, named twice.
 *
 * [endonym] is what it calls itself and is what the picker shows, for the reason [AppLanguage.nameOf]
 * gives. [localName] is the same language in the *app's* language, and exists only so that searching
 * works from either end: a German parent looking for Romanian may well type "Rumänisch", and a list
 * that only matched "Română" would answer that with nothing.
 */
data class LanguageOption(val tag: String, val endonym: String, val localName: String)
