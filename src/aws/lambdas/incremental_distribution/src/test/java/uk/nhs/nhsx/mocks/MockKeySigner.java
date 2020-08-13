package uk.nhs.nhsx.mocks;

import com.amazonaws.services.kms.model.SigningAlgorithmSpec;
import uk.nhs.nhsx.core.aws.kms.KeySigner;
import uk.nhs.nhsx.core.signature.KeyId;
import uk.nhs.nhsx.core.signature.Signature;

public class MockKeySigner implements KeySigner {

    private final String signatureValue;

    public MockKeySigner(String signatureValue) {
        this.signatureValue = signatureValue;
    }

    @Override
    public Signature sign(KeyId keyId, byte[] content) {
        return new Signature(keyId, SigningAlgorithmSpec.ECDSA_SHA_256, signatureValue.getBytes());
    }
}