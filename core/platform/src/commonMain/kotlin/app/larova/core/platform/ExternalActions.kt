package app.larova.core.platform

/**
 * The things Larova hands to another app.
 *
 * All of them are handovers, never actions the app completes itself. That is a deliberate limit rather
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

    /**
     * Hands [text] to whatever translation app is on this phone.
     *
     * Larova translates nothing itself: there is no internet permission here, and every on-device
     * translation library within reach downloads its models over the network. The text goes to an
     * app the person already chose, which does the work under its own permissions and its own
     * policy. Nothing comes back — the person copies what they want out of it themselves.
     *
     * *What* text a tile amounts to is decided in `:core:domain` by `plainTextOf`, so the numbers
     * and addresses on a tile are excluded there rather than here.
     */
    fun translate(text: String)

    /**
     * Whether this phone has a per-app language screen to send somebody to.
     *
     * Android grew one in 13; below that the app's language is the phone's and there is nothing to
     * open. Asked rather than assumed, because the answer decides whether the row exists at all —
     * a row that opens nothing is worse than no row.
     */
    val canOpenAppLanguageSettings: Boolean

    /**
     * Opens Android's own language screen for Larova.
     *
     * A handover like the rest, and for the same reason: the list of languages, the search, the
     * "system default" entry and the restart are all work Android already does properly, and a
     * picker written here would be a second, worse copy of it that has to be kept in step with
     * `locales_config.xml`.
     */
    fun openAppLanguageSettings()
}
