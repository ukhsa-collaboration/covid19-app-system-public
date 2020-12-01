package uk.nhs.nhsx.core.signature;

import com.amazonaws.services.kms.model.SigningAlgorithmSpec;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SignatureTest {

    private static final String SIGNATURE = "some-signature";

    @Test
    public void returnsBase64EncodedSignature() {
        byte[] bytes = SIGNATURE.getBytes();
        Signature signature = new Signature(KeyId.of("id"), SigningAlgorithmSpec.ECDSA_SHA_256, bytes);
        assertThat(Base64.decodeBase64(signature.asBase64Encoded())).isEqualTo(bytes);
    }
}