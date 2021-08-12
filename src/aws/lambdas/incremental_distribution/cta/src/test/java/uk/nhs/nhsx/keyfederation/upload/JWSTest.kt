package uk.nhs.nhsx.keyfederation.upload

import assertions.JwtAssertions.algorithmEqualTo
import assertions.JwtAssertions.hasValidSignature
import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import org.jose4j.jws.JsonWebSignature
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.api.expectThrows
import strikt.assertions.isEqualTo
import uk.nhs.nhsx.core.exceptions.Defect
import uk.nhs.nhsx.keyfederation.TestKeyPairs

class JWSTest {

    private val keypair = TestKeyPairs.ecPrime256r1
    private val private = keypair.private
    private val public = keypair.public

    @Test
    fun `signing some content`() {
        val payload = """{ "key": 1234 }"""
        val generated = JWS(KmsCompatibleSigner(private)).sign(payload)

        val verified = JsonWebSignature().also {
            it.key = public
            it.compactSerialization = generated
        }

        expectThat(verified) {
            algorithmEqualTo("ES256")
            hasValidSignature()
            get(JsonWebSignature::getPayload).isEqualTo(payload)
        }
    }

    @Test
    fun `complains when KMS returns incompatible signature`() {
        expectThrows<Defect> {
            JWS(KmsCompatibleSigner(private, SigningAlgorithmSpec.ECDSA_SHA_384)).sign("something")
        }
    }
}
