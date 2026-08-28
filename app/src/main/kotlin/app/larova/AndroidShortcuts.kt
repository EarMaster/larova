package app.larova

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import app.larova.core.domain.app.Shortcuts
import app.larova.core.domain.usecase.ShortcutTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The three most-opened tiles, as launcher shortcuts.
 *
 * In `:app` rather than in `:core:platform`, because this is the one piece of platform glue that
 * needs both halves of the app it is glueing: the activity the shortcut opens and the icon it is
 * drawn with. A port in the platform module would have to be handed both.
 *
 * `setDynamicShortcuts` replaces the whole list. That is what keeps a shortcut to a deleted tile
 * from surviving on somebody's home screen as a promise that opens nothing.
 *
 * The launcher icon rather than the tile's own symbol. A shortcut is drawn at launcher size against
 * a wallpaper nobody chose, and the tile symbols are designed for a coloured tile — using them here
 * would mean shipping a second set that works on any background, for a gain nobody asked for.
 */
class AndroidShortcuts(private val context: Context) : Shortcuts {

    override suspend fun publish(targets: List<ShortcutTarget>) = withContext(Dispatchers.IO) {
        val shortcuts = targets.map { target ->
            ShortcutInfoCompat.Builder(context, target.cardId.toString())
                // The whole title in both, and no truncation of our own. A launcher picks
                // whichever label fits the space it has and elides what does not with an ellipsis,
                // at the width it actually knows; cutting the string here produced "Wichtige Num"
                // with no ellipsis and no way to tell a shortened title from a badly named tile.
                // Android recommends a short label of about ten characters, which is a hint about
                // what will fit rather than a limit that is enforced.
                .setShortLabel(target.label)
                .setLongLabel(target.label)
                .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
                .setIntent(
                    Intent(context, MainActivity::class.java).apply {
                        // A shortcut intent has to name an action, or the launcher refuses it.
                        action = Intent.ACTION_VIEW
                        putExtra(EXTRA_CARD_ID, target.cardId.toString())
                    },
                )
                .build()
        }

        try {
            ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
        } catch (_: IllegalArgumentException) {
            // A launcher that allows fewer shortcuts than we offered, or one that refuses the list
            // outright. Nothing a person could do about it and nothing worth a crash: the tiles are
            // all still two taps away inside the app.
            false
        }
        Unit
    }
}

/** The tile a shortcut opens. Read in `MainActivity`, which is the only place an intent arrives. */
const val EXTRA_CARD_ID = "app.larova.extra.CARD_ID"
