package uk.nhs.nhsx.core.aws.s3;

import uk.nhs.nhsx.core.ValueType;

public class BucketName extends ValueType<BucketName> {

    private BucketName(String value) {
        super(value);
    }

    public static BucketName of(String name) {
        return new BucketName(name);
    }
}
