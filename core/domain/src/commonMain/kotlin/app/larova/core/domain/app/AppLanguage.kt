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
}
