package app.larova.core.billing

import app.larova.core.domain.model.Receipt
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The verifier against a real RSA keypair rather than a recorded Play receipt.
 *
 * A captured receipt would be tied to one product, one account and one signing key, and could not
 * be committed without publishing a real purchase. Generating a keypair here tests the same thing:
 * that a payload signed with the matching private key passes, and that everything else does not.
 *
 * SHA1withRSA is what Play signs with, so it is what is signed with here. It is not a choice.
 */
class PurchaseVerifierTest {

    @Test
    fun aPayloadSignedWithTheMatchingKeyIsAccepted() {
        val keys = keyPair()
        val payload = """{"productId":"app.larova.unlock","purchaseState":0}"""
        val receipt = Receipt(payload, sign(payload, keys))

        assertTrue(verifier(keys).verify(receipt))
    }

    @Test
    fun aPayloadEditedAfterSigningIsRejected() {
        val keys = keyPair()
        val signed = """{"productId":"app.larova.unlock","purchaseState":0}"""
        val tampered = """{"productId":"app.larova.unlock","purchaseState":1}"""
        val receipt = Receipt(tampered, sign(signed, keys))

        assertFalse(verifier(keys).verify(receipt))
    }

    @Test
    fun aSignatureFromADifferentKeyIsRejected() {
        val payload = """{"productId":"app.larova.unlock"}"""
        val receipt = Receipt(payload, sign(payload, keyPair()))

        assertFalse(verifier(keyPair()).verify(receipt))
    }

    /**
     * A build assembled without a licensing key must lock, not unlock. This is the case a release
     * built from a fresh clone would hit, and defaulting it the other way would ship the paid tier
     * to everyone who forgot an environment variable.
     */
    @Test
    fun aBuildWithNoLicensingKeyUnlocksNothing() {
        val keys = keyPair()
        val payload = """{"productId":"app.larova.unlock"}"""
        val receipt = Receipt(payload, sign(payload, keys))

        assertFalse(PurchaseVerifier("").verify(receipt))
        assertFalse(PurchaseVerifier("   ").verify(receipt))
    }

    @Test
    fun anEmptyOrMalformedReceiptIsRejectedWithoutThrowing() {
        val subject = verifier(keyPair())

        assertFalse(subject.verify(Receipt("", "")))
        assertFalse(subject.verify(Receipt("{}", "")))
        assertFalse(subject.verify(Receipt("{}", "not base64 at all !!")))
        assertFalse(subject.verify(Receipt("{}", Base64.getEncoder().encodeToString(byteArrayOf(1)))))
    }

    /** A key that is valid base64 but not a key at all. Same answer, still no exception. */
    @Test
    fun aPublicKeyThatIsNotAKeyIsRejected() {
        val nonsense = Base64.getEncoder().encodeToString("definitely not a key".toByteArray())

        assertFalse(PurchaseVerifier(nonsense).verify(Receipt("{}", "AAAA")))
    }

    private fun verifier(keys: KeyPair) =
        PurchaseVerifier(Base64.getEncoder().encodeToString(keys.public.encoded))

    private fun keyPair(): KeyPair =
        KeyPairGenerator.getInstance("RSA").apply { initialize(RSA_KEY_SIZE) }.generateKeyPair()

    private fun sign(payload: String, keys: KeyPair): String {
        val signature = Signature.getInstance("SHA1withRSA").apply {
            initSign(keys.private)
            update(payload.toByteArray(Charsets.UTF_8))
        }
        return Base64.getEncoder().encodeToString(signature.sign())
    }

    private companion object {
        /** Play's licensing keys are 2048-bit. Matching that keeps the test honest about cost. */
        const val RSA_KEY_SIZE = 2048
    }
}
