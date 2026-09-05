package app.larova.core.domain.app

/**
 * Whether this phone has anything that will take text to translate.
 *
 * A port for the same reason [InstalledApps] is one: the answer comes from the platform's own
 * package manager, and the screen that needs it has no business knowing that. Asked rather than
 * assumed, because it decides whether the control is drawn at all — a tablet with no translation
 * app is an ordinary device, and a button that does nothing on it is worse than no button.
 *
 * Larova translates nothing itself and never will. There is no internet permission here, and every
 * on-device translation library within reach downloads its models over the network. What this
 * interface answers is "is there somebody else to hand this to", and nothing more.
 */
interface Translators {

    /** False on a phone with no translation app, and on one that has hidden it. */
    suspend fun canTranslate(): Boolean
}
