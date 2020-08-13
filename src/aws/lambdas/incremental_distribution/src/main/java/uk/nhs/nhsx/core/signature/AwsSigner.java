package uk.nhs.nhsx.core.signature;

import uk.nhs.nhsx.core.aws.kms.KeySigner;
import uk.nhs.nhsx.core.aws.ssm.KeyLookup;

public class AwsSigner implements Signer {

    private final KeyLookup keyLookup;
    private final KeySigner keySigner;

    public AwsSigner(KeyLookup keyLookup, KeySigner keySigner) {
        this.keyLookup = keyLookup;
        this.keySigner = keySigner;
    }

    public Signature sign(byte[] bytes) {
        return keySigner.sign(keyLookup.getKmsKeyId(), bytes);
    }
}
