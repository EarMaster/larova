package app.larova.core.domain.usecase

import app.larova.core.domain.repository.PinRepository
import app.larova.core.domain.session.ViewModeSession

/** The shortest PIN worth having. Four digits is what a phone lock screen asks for. */
const val MIN_PIN_LENGTH = 4

/** Long enough for anyone who wants more, short enough to be typed one-handed in the dark. */
const val MAX_PIN_LENGTH = 12

/**
 * Sets or replaces the PIN.
 *
 * Digits only, and length-checked here rather than in the screen, because the rule belongs with
 * the thing that stores it — a second entry point that skipped the check would be a PIN nobody
 * could type.
 */
class SetPin(private val pins: PinRepository) {

    sealed interface Result {
        data object Set : Result
        data object TooShort : Result
        data object NotDigits : Result
    }

    suspend operator fun invoke(pin: String): Result = when {
        pin.length < MIN_PIN_LENGTH -> Result.TooShort
        pin.length > MAX_PIN_LENGTH -> Result.TooShort
        !pin.all { it.isDigit() } -> Result.NotDigits
        else -> {
            pins.setPin(pin)
            Result.Set
        }
    }
}

/**
 * Opens parent view, if the PIN is right.
 *
 * The session is only ever unlocked from here or from the biometric path, never from a screen
 * deciding it looks unlocked. A wrong PIN leaves the mode exactly as it was.
 */
class UnlockWithPin(
    private val pins: PinRepository,
    private val session: ViewModeSession,
) {

    suspend operator fun invoke(pin: String): Boolean {
        val accepted = pins.verify(pin)
        if (accepted) session.unlock()
        return accepted
    }
}

/**
 * Opens parent view after the system has vouched for the person — a fingerprint or a face.
 *
 * Separate from the PIN path and deliberately not given the PIN: the platform has already done the
 * checking, and this app never sees the credential. It still refuses when no PIN exists, because
 * the PIN is the fallback that keeps a device without biometrics usable, and an installation that
 * can be unlocked by a fingerprint but has no other way in is one lost sensor from being locked
 * out of its own content.
 */
class UnlockWithBiometrics(
    private val pins: PinRepository,
    private val session: ViewModeSession,
) {

    suspend operator fun invoke(): Boolean {
        if (!pins.hasPin()) return false
        session.unlock()
        return true
    }
}

/**
 * Whether parent view has a lock at all.
 *
 * A fresh installation has none, and the first attempt to unlock is therefore a set-up rather than
 * a challenge. Asked before showing the PIN field, so nobody is presented with a prompt for a
 * secret that was never chosen.
 */
class HasPin(private val pins: PinRepository) {
    suspend operator fun invoke(): Boolean = pins.hasPin()
}

/** Leaving parent view on purpose. The five-minute fallback does the same thing unprompted. */
class LockParentView(private val session: ViewModeSession) {
    operator fun invoke() = session.lock()
}
