package uk.nhs.nhsx.mocks;

import uk.nhs.nhsx.core.signature.KeyId;
import uk.nhs.nhsx.core.aws.ssm.KeyLookup;

public class MockKeyLookup implements KeyLookup {

    private final String keyId;

    public MockKeyLookup(String keyId) {
        this.keyId = keyId;
    }

    @Override
    public KeyId getKmsKeyId() {
        return KeyId.of(keyId);
    }
}