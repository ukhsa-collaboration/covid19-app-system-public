package uk.nhs.nhsx.core.aws.kms;

import uk.nhs.nhsx.core.signature.Signature;
import uk.nhs.nhsx.core.signature.KeyId;

public interface KeySigner {
    Signature sign(KeyId keyId, byte[] content);
}
