package app.larova

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import app.larova.core.ui.resources.Res
import app.larova.core.ui.resources.app_name
import app.larova.core.ui.resources.edit_cancel
import app.larova.core.ui.resources.view_unlock_title
import org.jetbrains.compose.resources.stringResource

/**
 * The fingerprint or face route into parent view.
 *
 * This lives in `:app` rather than in `:core:platform` because it needs the Activity, and an
 * Activity is not something a singleton can hold without leaking it. The iOS entry point will
 * provide its own equivalent the same way, which is why the screens take a nullable lambda instead
 * of asking a shared service whether biometrics exist.
 *
 * Larova never sees the credential. The platform vouches for the person and this code learns only
 * that it did — which is also why the PIN has to exist first: a device whose sensor stops working
 * must still have a way in to its own content.
 */
@Composable
fun rememberBiometricUnlock(onAccepted: () -> Unit): (() -> Unit)? {
    val context = LocalContext.current
    val activity = context.findFragmentActivity() ?: return null

    val available = BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
        BiometricManager.BIOMETRIC_SUCCESS
    if (!available) return null

    val title = stringResource(Res.string.view_unlock_title)
    val subtitle = stringResource(Res.string.app_name)
    val cancel = stringResource(Res.string.edit_cancel)

    return {
        val prompt = BiometricPrompt(
            activity,
            object : BiometricPrompt.AuthenticationCallback() {
                /**
                 * Deliberately not backed by a `CryptoObject`, which CodeQL flags as insecure local
                 * authentication (`java/android/insecure-local-authentication`). The rule is right
                 * about the mechanism: without a Keystore key behind it, this callback is a boolean
                 * that code running in the process could reach on its own.
                 *
                 * Accepted here because of what the gate is for. Parent view keeps a caregiver or a
                 * child from editing tiles on a phone they are holding. Anyone able to invoke this
                 * callback directly already has code execution in the app and can read the database
                 * next to it, so a crypto-backed prompt would move no attacker out of reach.
                 *
                 * The cost of the alternative is not theoretical: a `CryptoObject` requires
                 * BIOMETRIC_STRONG, which would remove biometric unlock from every device whose only
                 * sensor is weak face unlock — common on exactly the older phones this app is meant
                 * to be usable on. The PIN is what carries the security, and it is always available.
                 *
                 * Revisit in M3, when the PIN hash moves into the Keystore
                 * (docs/technical-notes.md §8) and there is a device to test the key lifecycle on —
                 * a key invalidated by biometric re-enrolment must fall back to the PIN, not lock a
                 * parent out of their own content.
                 */
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onAccepted()
                }
                // A failed or cancelled attempt is not an error to report: the PIN field is still
                // on screen behind the prompt, which is the answer to "what now".
            },
        )
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(cancel)
                // BIOMETRIC_WEAK rather than STRONG: this gate protects a phone's own tiles from
                // being edited by whoever is holding it, not a key from being extracted. Demanding
                // STRONG would shut out perfectly ordinary face unlock on a lot of devices, and
                // the PIN fallback is what carries the actual security.
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK)
                .build(),
        )
    }
}

// Internal rather than private: UnlockPurchase.kt needs the same walk up the context chain,
// and two copies of it would drift.
internal tailrec fun android.content.Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is android.content.ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}
