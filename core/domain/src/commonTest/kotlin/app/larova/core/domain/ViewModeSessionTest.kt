package app.larova.core.domain

import app.larova.core.domain.model.ViewMode
import app.larova.core.domain.session.ViewModeSession
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

/**
 * The five minutes are the part of the two-view split that nobody will test by hand — waiting out
 * a timeout is exactly what a person does not do — so it is tested on virtual time instead.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ViewModeSessionTest {

    @Test
    fun aFreshSessionIsInCaregiverView() = runTest {
        // Nothing persists an unlock. A phone found on a table is a phone in caregiver view.
        val session = ViewModeSession(backgroundScope)
        assertEquals(ViewMode.CAREGIVER, session.mode.value)
        assertFalse(session.mode.value.isParent)
    }

    @Test
    fun parentViewFallsBackAfterFiveMinutes() = runTest {
        val session = ViewModeSession(backgroundScope)

        session.unlock()
        assertTrue(session.mode.value.isParent)

        advanceTimeBy(4.minutes + 59.seconds)
        runCurrent()
        assertTrue(session.mode.value.isParent, "locked early")

        advanceTimeBy(2.seconds)
        runCurrent()
        assertEquals(ViewMode.CAREGIVER, session.mode.value)
    }

    @Test
    fun everyInteractionBuysAnotherFiveMinutes() = runTest {
        val session = ViewModeSession(backgroundScope)
        session.unlock()

        // Someone writing a long guide, typing every few minutes for a quarter of an hour.
        repeat(5) {
            advanceTimeBy(3.minutes)
            runCurrent()
            session.touch()
        }

        assertTrue(session.mode.value.isParent, "locked while being used")

        advanceTimeBy(6.minutes)
        runCurrent()
        assertEquals(ViewMode.CAREGIVER, session.mode.value, "did not lock once left alone")
    }

    @Test
    fun touchingTheScreenIsNeverAWayIn() = runTest {
        // The whole point of the split: interaction extends parent view, it cannot grant it.
        val session = ViewModeSession(backgroundScope)

        repeat(20) { session.touch() }
        advanceTimeBy(1.minutes)
        runCurrent()

        assertEquals(ViewMode.CAREGIVER, session.mode.value)
    }

    @Test
    fun leavingParentViewIsImmediate() = runTest {
        val session = ViewModeSession(backgroundScope)
        session.unlock()

        session.lock()

        assertEquals(ViewMode.CAREGIVER, session.mode.value)
        // And the timer that was running does not come back to lock an unlock that happened after.
        session.unlock()
        advanceTimeBy(1.minutes)
        runCurrent()
        assertTrue(session.mode.value.isParent)
    }

    @Test
    fun unlockingAgainRestartsTheClockRatherThanAddingASecondOne() = runTest {
        val session = ViewModeSession(backgroundScope)

        session.unlock()
        advanceTimeBy(4.minutes)
        runCurrent()
        session.unlock()

        advanceTimeBy(2.minutes)
        runCurrent()
        assertTrue(session.mode.value.isParent, "an old timer locked a newer session")

        advanceTimeBy(4.minutes)
        runCurrent()
        assertEquals(ViewMode.CAREGIVER, session.mode.value)
    }

    @Test
    fun theTimeoutIsFiveMinutes() {
        // Written down in docs/concept.md 4.2, so it is asserted rather than left to a constant
        // somebody might tidy.
        assertEquals(5.minutes, ViewModeSession.DEFAULT_TIMEOUT)
    }
}
