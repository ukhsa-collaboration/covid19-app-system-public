package uk.nhs.nhsx.core.signature

import com.amazonaws.services.kms.model.SigningAlgorithmSpec
import org.apache.commons.codec.binary.Base64
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SignatureTest {

    @Test
    fun returnsBase64EncodedSignature() {
        val bytes = SIGNATURE.toByteArray()
        val signature = Signature(KeyId.of("id"), SigningAlgorithmSpec.ECDSA_SHA_256, bytes)
        assertThat(Base64.decodeBase64(signature.asBase64Encoded())).isEqualTo(bytes)
    }

    companion object {
        private const val SIGNATURE = "some-signature"
    }
}