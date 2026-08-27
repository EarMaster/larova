package app.larova.core.platform

/**
 * The three things Larova hands to another app.
 *
 * Both are handovers, never actions the app completes itself. That is a deliberate limit rather
 * than a shortcut: an app that dials by itself needs the `CALL_PHONE` permission, turns a mistaken
 * tap into a real call, and starts to look like something that alerts emergency services. Handing
 * the number to the phone app with the dialler open is harmless if triggered by accident and needs
 * no permission at all.
 *
 * What counts as a dialable number or an openable address is decided in `:core:domain`, so the
 * editor validating a tile and the opener acting on it cannot disagree.
 */
interface ExternalActions {

    /** Opens the phone app with [number] filled in. The person taps the call button, not Larova. */
    fun prepareCall(number: String)

    /** Opens [url] in the browser, if it is an http or https address. */
    fun openUrl(url: String)

    /**
     * Opens another app at its own start screen.
     *
     * The launcher's own intent, not a component named by the tile: an activity that was exported
     * when the tile was made may not be tomorrow, and starting one directly is how a tile begins
     * throwing on a phone whose apps have simply been updated.
     */
    fun openApp(packageName: String)
}
