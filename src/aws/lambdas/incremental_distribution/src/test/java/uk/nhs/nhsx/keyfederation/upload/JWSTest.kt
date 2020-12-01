package uk.nhs.nhsx.keyfederation.upload

import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.jose4j.jws.JsonWebSignature
import org.junit.jupiter.api.Test
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

        assertThat(verified.payload, equalTo(payload));
        assertThat("signature is valid", verified.verifySignature(), equalTo(true));
        assertThat(verified.headers.getStringHeaderValue("alg"), equalTo("ES256"))
    }

    @Test
    fun `complains when KMS returns incompatible signature`() {
        assertThat(
            { JWS(KmsCompatibleSigner(private, SigningAlgorithmSpec.ECDSA_SHA_384)).sign("something") },
            throws<Defect>())
    }
}