package uk.nhs.nhsx.core.signature

import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import org.apache.commons.codec.binary.Base64
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class SignatureTest {

    @Test
    fun `returns base 64 encoded signature`() {
        val bytes = SIGNATURE.toByteArray()
        val signature = Signature(
            KeyId.of("id"),
            SigningAlgorithmSpec.ECDSA_SHA_256,
            bytes
        )
        expectThat(Base64.decodeBase64(signature.asBase64Encoded())).isEqualTo(bytes)
    }

    companion object {
        private const val SIGNATURE = "some-signature"
    }
}
