package app.larova.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.math.max
import kotlin.math.min
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The colour token indirection is the one thing in this app that cannot be repaired after release,
 * because the values were chosen by users and live in their export files. So it is tested rather
 * than reviewed.
 */
class AppColorsTest {

    @Test
    fun everyTokenResolvesInEveryMode() {
        for (token in TileColor.entries) {
            for (mode in AppMode.entries) {
                // resolve() reads from a map with getValue: a token missing from one of the three
                // tables throws here rather than painting a tile invisible on someone's phone.
                val colors = token.resolve(mode)
                assertEquals(1f, colors.surface.alpha, "${token.key} surface in $mode is translucent")
                assertEquals(1f, colors.accent.alpha, "${token.key} accent in $mode is translucent")
            }
        }
    }

    @Test
    fun theEightKeysAreFrozen() {
        // A key is a value in Card.colorToken and in every export file. Renaming one after release
        // means migrating data that may only exist in a backup nobody can reach.
        assertEquals(
            listOf("sand", "clay", "rose", "lilac", "sky", "sage", "moss", "stone"),
            TileColor.entries.map { it.key },
        )
    }

    @Test
    fun unknownKeysFallBackToTheDefaultRatherThanFailing() {
        // What lets an export written by a newer version still open here.
        assertEquals(TileColor.SAND, TileColor.fromKey(null))
        assertEquals(TileColor.SAND, TileColor.fromKey(""))
        assertEquals(TileColor.SAND, TileColor.fromKey("aubergine"))
        // The keys were German before the rename, which happened while the app had no users.
        assertEquals(TileColor.SAND, TileColor.fromKey("moosgruen"))
    }

    @Test
    fun keysRoundTripThroughFromKey() {
        for (token in TileColor.entries) {
            assertEquals(token, TileColor.fromKey(token.key))
        }
    }

    @Test
    fun amberAndAlarmAreNotAvailableAsTileColours() {
        // Amber marks what is happening right now and alarm red is the help bar. Once every third
        // tile is amber, that signal is gone permanently, because users assign the colours.
        val reserved = setOf(
            Signal.amberOnDark,
            Signal.amberOnLight,
            Signal.alarmLight,
            Signal.alarmDark,
        )
        for (token in TileColor.entries) {
            for (mode in AppMode.entries) {
                val colors = token.resolve(mode)
                assertTrue(
                    colors.surface !in reserved,
                    "${token.key} in $mode uses a reserved colour as its surface",
                )
            }
        }
    }

    @Test
    fun titleAndSubtitleStayLegibleOnEveryTileSurface() {
        // docs/design/design-system.md claims title contrast above 9:1 and subtitle above 4.5:1.
        // 4.5:1 is also the floor the whole product holds itself to, so assert against the floor:
        // a failure here means a tile someone cannot read, in a mode they may never see reported.
        for (mode in AppMode.entries) {
            val surfaces = when (mode) {
                AppMode.LIGHT -> LightSurfaces
                AppMode.DARK -> DarkSurfaces
                AppMode.NIGHT -> NightSurfaces
            }
            for (token in TileColor.entries) {
                val tile = token.resolve(mode)
                assertTrue(
                    contrast(tile.accent, tile.surface) >= MIN_CONTRAST,
                    "${token.key} accent on its own surface in $mode is below 4.5:1",
                )
                assertTrue(
                    contrast(surfaces.ink, surfaces.background) >= MIN_CONTRAST,
                    "ink on background in $mode is below 4.5:1",
                )
                assertTrue(
                    contrast(surfaces.inkMuted, surfaces.background) >= MIN_CONTRAST,
                    "muted ink on background in $mode is below 4.5:1",
                )
            }
        }
    }

    @Test
    fun nightIsDimmerThanDarkForEveryToken() {
        // Night is the dark palette pulled toward #12101F, not a brighter variation on it. If a
        // token ever came out lighter, the mode would stop serving the case it exists for:
        // reading a guide aloud in a darkened bedroom.
        for (token in TileColor.entries) {
            val dark = token.resolve(AppMode.DARK).surface.luminance()
            val night = token.resolve(AppMode.NIGHT).surface.luminance()
            assertTrue(night < dark, "${token.key} is brighter in night than in dark")
        }
        assertTrue(NightSurfaces.background.luminance() < DarkSurfaces.background.luminance())
    }

    private companion object {
        const val MIN_CONTRAST = 4.5

        /** WCAG 2.1 relative contrast ratio. */
        fun contrast(a: Color, b: Color): Double {
            val la = a.luminance().toDouble()
            val lb = b.luminance().toDouble()
            return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
        }
    }
}
