package app.larova.core.domain.app

import app.larova.core.domain.usecase.ShortcutTarget

/**
 * The launcher's own list of shortcuts into this app.
 *
 * A platform thing through and through: dynamic shortcuts on Android, quick actions on iOS, and
 * nothing shared between them but "here are three tiles and what they are called".
 *
 * Publishing replaces the whole list rather than adding to it. A shortcut to a tile that has since
 * been deleted is worse than no shortcut — it is a promise on the home screen that opens nothing —
 * and the only way to be sure none is left behind is to write the list whole.
 */
interface Shortcuts {

    suspend fun publish(targets: List<ShortcutTarget>)
}
