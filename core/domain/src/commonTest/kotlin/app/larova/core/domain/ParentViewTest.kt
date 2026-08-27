package app.larova.core.domain

import app.larova.core.domain.repository.PinRepository
import app.larova.core.domain.session.ViewModeSession
import app.larova.core.domain.usecase.HasPin
import app.larova.core.domain.usecase.LockParentView
import app.larova.core.domain.usecase.SetPin
import app.larova.core.domain.usecase.UnlockWithBiometrics
import app.larova.core.domain.usecase.UnlockWithPin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

/**
 * A PIN that can be bypassed is worse than no PIN, because the parents would believe in it. Every
 * way in is tested, including the ones that must not work.
 */
class ParentViewTest {

    @Test
    fun theRightPinOpensParentView() = runTest {
        val pins = FakePinRepository()
        val session = ViewModeSession(backgroundScope)
        SetPin(pins)("2468")

        assertTrue(UnlockWithPin(pins, session)("2468"))
        assertTrue(session.mode.value.isParent)
    }

    @Test
    fun theWrongPinChangesNothing() = runTest {
        val pins = FakePinRepository()
        val session = ViewModeSession(backgroundScope)
        SetPin(pins)("2468")
        val unlock = UnlockWithPin(pins, session)

        assertFalse(unlock("1234"))
        assertFalse(unlock(""))
        assertFalse(unlock("24680"))
        assertFalse(unlock("246"))
        assertFalse(session.mode.value.isParent, "a rejected PIN unlocked the app")
    }

    @Test
    fun withNoPinSetNothingUnlocks() = runTest {
        // A fresh installation. The way in is to choose a PIN, not to guess the empty one.
        val pins = FakePinRepository()
        val session = ViewModeSession(backgroundScope)

        assertFalse(HasPin(pins)())
        assertFalse(UnlockWithPin(pins, session)(""))
        assertFalse(UnlockWithPin(pins, session)("0000"))
        assertFalse(UnlockWithBiometrics(pins, session)())
        assertFalse(session.mode.value.isParent)
    }

    @Test
    fun biometricsNeedAPinToFallBackOn() = runTest {
        // An installation unlockable by fingerprint alone is one broken sensor away from being
        // locked out of its own content.
        val pins = FakePinRepository()
        val session = ViewModeSession(backgroundScope)
        val unlock = UnlockWithBiometrics(pins, session)

        assertFalse(unlock())

        SetPin(pins)("2468")
        assertTrue(unlock())
        assertTrue(session.mode.value.isParent)
    }

    @Test
    fun aPinIsFourToTwelveDigits() = runTest {
        val pins = FakePinRepository()
        val set = SetPin(pins)

        assertEquals(SetPin.Result.TooShort, set("123"))
        assertEquals(SetPin.Result.TooShort, set(""))
        assertEquals(SetPin.Result.TooShort, set("1234567890123"))
        assertEquals(SetPin.Result.NotDigits, set("12ab"))
        assertEquals(SetPin.Result.NotDigits, set("1 2 3 4"))
        assertFalse(pins.hasPin(), "a refused PIN was stored anyway")

        assertEquals(SetPin.Result.Set, set("1234"))
        assertEquals(SetPin.Result.Set, set("123456789012"))
        assertTrue(pins.hasPin())
    }

    @Test
    fun replacingThePinRetiresTheOldOne() = runTest {
        val pins = FakePinRepository()
        val session = ViewModeSession(backgroundScope)
        SetPin(pins)("2468")
        SetPin(pins)("1357")
        val unlock = UnlockWithPin(pins, session)

        assertFalse(unlock("2468"))
        assertTrue(unlock("1357"))
    }

    @Test
    fun thePinItselfIsNeverStored() = runTest {
        // What is kept must not be the PIN. This is a fake hasher, but the assertion is the
        // contract the real one has to keep too.
        val pins = FakePinRepository()
        SetPin(pins)("2468")

        assertNull(pins.stored?.let { if (it == "2468") it else null })
        assertTrue(pins.stored?.startsWith("hashed:") == true)
    }

    @Test
    fun leavingParentViewLocksItAtOnce() = runTest {
        val pins = FakePinRepository()
        val session = ViewModeSession(backgroundScope)
        SetPin(pins)("2468")
        UnlockWithPin(pins, session)("2468")

        LockParentView(session)()

        assertFalse(session.mode.value.isParent)
    }
}

/**
 * Stands in for the hashing platform. Deliberately stores something that is not the PIN, so a test
 * that passed by comparing plaintext would fail here.
 */
private class FakePinRepository : PinRepository {

    var stored: String? = null
        private set

    override suspend fun hasPin(): Boolean = stored != null

    override suspend fun setPin(pin: String) {
        stored = "hashed:" + pin.reversed()
    }

    override suspend fun verify(pin: String): Boolean = stored == "hashed:" + pin.reversed()

    override suspend fun clear() {
        stored = null
    }
}
