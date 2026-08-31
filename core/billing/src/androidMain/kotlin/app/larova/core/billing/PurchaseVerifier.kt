package app.larova.core.billing

import app.larova.core.domain.model.Receipt
import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.Base64

/**
 * Checks a Play receipt against the app's licensing public key, here on the device.
 *
 * This is the whole of the enforcement, and it is worth being honest about what it is and is not.
 * Google recommends verifying a purchase token on a server, which this app cannot do: it has no
 * internet permission and never will, so there is no server to ask. What is left is the offline
 * check Play has always supported — the payload it signed, the signature, and a public key
 * embedded in the build.
 *
 * The public key is not a secret, and anyone who can rebuild the app can replace it or delete this
 * class. Under the AGPL they can do that legally and publish the result, so the point of this is not
 * to be unbreakable. It is to make a purchase mean something without a network, and to make a `true`
 * written into a preferences file by hand not count.
 *
 * SHA1withRSA is not a choice — it is the algorithm Play signs with.
 */
class PurchaseVerifier(private val base64PublicKey: String) {

    /**
     * False on anything unexpected, deliberately. A malformed key, a truncated signature and a
     * forged payload are all the same answer from here, and telling them apart would only help
     * somebody find out which part they got wrong.
     *
     * An empty key means the build was assembled without one — a `foss` or debug build that got
     * this far by mistake. That is "not verified", never "verified".
     */
    fun verify(receipt: Receipt): Boolean {
        if (base64PublicKey.isBlank() || receipt.payload.isBlank() || receipt.signature.isBlank()) {
            return false
        }
        return try {
            val decoder = Base64.getDecoder()
            val key = KeyFactory.getInstance("RSA")
                .generatePublic(X509EncodedKeySpec(decoder.decode(base64PublicKey)))
            Signature.getInstance("SHA1withRSA").run {
                initVerify(key)
                update(receipt.payload.toByteArray(Charsets.UTF_8))
                verify(decoder.decode(receipt.signature))
            }
        } catch (_: GeneralSecurityException) {
            false
        } catch (_: IllegalArgumentException) {
            // Base64 that is not base64. Same answer.
            false
        }
    }
}
