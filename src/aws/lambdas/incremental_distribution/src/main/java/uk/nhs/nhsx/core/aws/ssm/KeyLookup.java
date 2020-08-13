package uk.nhs.nhsx.core.aws.ssm;

import uk.nhs.nhsx.core.signature.KeyId;

public interface KeyLookup {
    KeyId getKmsKeyId();
}
