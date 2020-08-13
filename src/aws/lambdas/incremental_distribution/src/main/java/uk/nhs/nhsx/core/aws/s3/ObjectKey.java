package uk.nhs.nhsx.core.aws.s3;

import uk.nhs.nhsx.core.ValueType;

public class ObjectKey extends ValueType<ObjectKey> {

    private ObjectKey(String value) {
        super(value);
    }

    public ObjectKey append(String suffix) {
        return ObjectKey.of(value + suffix);
    }

    public static ObjectKey of(String name) {
        return new ObjectKey(name);
    }
}
