package app.larova.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Larova colour tokens.
 *
 * Rule: the database stores the key only (for example "sage"). The hex value is
 * resolved here at runtime. Storing the colour itself instead makes every
 * user-created tile unreadable in dark mode — retroactively, with no way to
 * repair it, because the values were chosen by users.
 *
 * Token keys are internal identifiers and never reach a user; the labels shown
 * in the picker come from strings.xml. See docs/design/design-system.md.
 *
 * The keys were German until the rename in this file's history — deliberately
 * done while the app had no users, because a key is a value in an export file
 * and renaming one after content exists means migrating other people's data.
 * The `label` below is a development convenience only; the picker reads
 * `colour_<key>` from strings.xml, which is localized.
 */

enum class AppMode { LIGHT, DARK, NIGHT }

/** The eight colours a user can pick for a tile. */
enum class TileColor(val key: String, val label: String) {
    SAND("sand", "Sand"),
    CLAY("clay", "Clay"),
    ROSE("rose", "Rose"),
    LILAC("lilac", "Lilac"),
    SKY("sky", "Sky"),
    SAGE("sage", "Sage"),
    MOSS("moss", "Moss"),
    STONE("stone", "Stone");

    companion object {
        val DEFAULT = SAND

        /** Unknown keys from a newer export fall back to the default rather than failing. */
        fun fromKey(key: String?): TileColor =
            entries.firstOrNull { it.key == key } ?: DEFAULT
    }
}

@Immutable
data class TileColors(val surface: Color, val accent: Color)

private val LIGHT = mapOf(
    TileColor.SAND  to TileColors(Color(0xFFF7E4C2), Color(0xFF8A5B12)),
    TileColor.CLAY  to TileColors(Color(0xFFF4DBCB), Color(0xFF8E4A2E)),
    TileColor.ROSE  to TileColors(Color(0xFFF6DBE1), Color(0xFF8C3D57)),
    TileColor.LILAC to TileColors(Color(0xFFE4DCF6), Color(0xFF4B3E86)),
    TileColor.SKY   to TileColors(Color(0xFFD8E5F5), Color(0xFF2F4E7C)),
    TileColor.SAGE  to TileColors(Color(0xFFD5E9E1), Color(0xFF1F5F4E)),
    TileColor.MOSS  to TileColors(Color(0xFFDFEBD4), Color(0xFF41652C)),
    TileColor.STONE to TileColors(Color(0xFFE7E2DA), Color(0xFF5A5348)),
)

private val DARK = mapOf(
    TileColor.SAND  to TileColors(Color(0xFF4C3B1E), Color(0xFFF0C88A)),
    TileColor.CLAY  to TileColors(Color(0xFF4E332A), Color(0xFFEFB496)),
    TileColor.ROSE  to TileColors(Color(0xFF4A2C38), Color(0xFFF0AEC0)),
    TileColor.LILAC to TileColors(Color(0xFF37305A), Color(0xFFC6B8F0)),
    TileColor.SKY   to TileColors(Color(0xFF243B55), Color(0xFFA8C8EC)),
    TileColor.SAGE  to TileColors(Color(0xFF22423A), Color(0xFF8FD0BC)),
    TileColor.MOSS  to TileColors(Color(0xFF2F4229), Color(0xFFB4D69A)),
    TileColor.STONE to TileColors(Color(0xFF3A3733), Color(0xFFCFC7BA)),
)

private val NIGHT = mapOf(
    TileColor.SAND  to TileColors(Color(0xFF32281E), Color(0xFFEBD3AC)),
    TileColor.CLAY  to TileColors(Color(0xFF332325), Color(0xFFEACCB0)),
    TileColor.ROSE  to TileColors(Color(0xFF311F2D), Color(0xFFEBCABF)),
    TileColor.LILAC to TileColors(Color(0xFF26223F), Color(0xFFDCCDD0)),
    TileColor.SKY   to TileColors(Color(0xFF1C283D), Color(0xFFD2D3CE)),
    TileColor.SAGE  to TileColors(Color(0xFF1B2C2E), Color(0xFFC9D6BD)),
    TileColor.MOSS  to TileColors(Color(0xFF222C24), Color(0xFFD6D8B1)),
    TileColor.STONE to TileColors(Color(0xFF28252A), Color(0xFFDFD3BD)),
)

fun TileColor.resolve(mode: AppMode): TileColors = when (mode) {
    AppMode.LIGHT -> LIGHT.getValue(this)
    AppMode.DARK  -> DARK.getValue(this)
    AppMode.NIGHT -> NIGHT.getValue(this)
}

/**
 * Surfaces and text. Title contrast against a tile surface exceeds 9:1 in all
 * three modes; subtitle contrast exceeds 5:1.
 */
@Immutable
data class Surfaces(
    val background: Color,
    val raised: Color,
    val ink: Color,
    val inkMuted: Color,
    /** Overlay above the tile surface, used for the symbol chip. */
    val chipOverlay: Color,
)

val LightSurfaces = Surfaces(
    background   = Color(0xFFFBF7F1),
    raised       = Color(0xFFFFFFFF),
    ink          = Color(0xFF241F35),
    inkMuted     = Color(0xFF5A5470),
    chipOverlay  = Color(0xB8FFFFFF), // white, 72 %
)

val DarkSurfaces = Surfaces(
    background   = Color(0xFF1B1830),
    raised       = Color(0xFF241F3E),
    ink          = Color(0xFFF1ECF8),
    inkMuted     = Color(0xFFB9B2CC),
    chipOverlay  = Color(0x17FFFFFF), // white, 9 %
)

val NightSurfaces = Surfaces(
    background   = Color(0xFF12101F),
    raised       = Color(0xFF1A1729),
    ink          = Color(0xFFE8D9BE), // warm rather than cool, so nothing glows blue in a dark room
    inkMuted     = Color(0xFFA99781),
    chipOverlay  = Color(0x12FFFFFF), // white, 7 %
)

/**
 * Reserved system colours. These are NOT offered as tile colours — they are the
 * app's voice, not the user's. Amber marks what is happening right now; once
 * every third tile is amber that signal stops working, permanently.
 */
object Signal {
    /** Brand and progress. The bright tone carries no text on light surfaces (2.4:1). */
    val amberOnDark  = Color(0xFFFFC46B)
    val amberOnLight = Color(0xFF9E5E0C)

    /** The help bar and nothing else, anywhere in the product. */
    val alarmLight     = Color(0xFFC0392B) // with white text, 5.4:1
    val alarmDark      = Color(0xFFFF9B8F)
    val alarmDarkInk   = Color(0xFF2A1512) // text on alarmDark, 8.5:1
}
