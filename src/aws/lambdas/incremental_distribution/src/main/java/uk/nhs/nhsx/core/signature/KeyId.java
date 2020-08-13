package uk.nhs.nhsx.core.signature;

import uk.nhs.nhsx.core.ValueType;

public class KeyId extends ValueType<KeyId> {

    private KeyId(String value) {
        super(value);
    }
    
    public static KeyId of(String idOrArn) {
        return new KeyId(kmsKeyId(idOrArn));
    }

    private static String kmsKeyId(String kmsKeyArnOrId) {
        if (kmsKeyArnOrId.startsWith("arn:aws:kms:")) {
            return kmsKeyArnOrId.substring(kmsKeyArnOrId.indexOf(":key/") + ":key/".length());
        }
        return kmsKeyArnOrId;
    }
}
