package uk.nhs.nhsx.core.aws.s3;

import java.util.Objects;
import java.util.StringJoiner;

public class Locator {
    public final BucketName bucket;
    public final ObjectKey key;

    public Locator(BucketName bucket, ObjectKey key) {
        this.bucket = bucket;
        this.key = key;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Locator locator = (Locator) o;
        return Objects.equals(bucket, locator.bucket) && Objects.equals(key, locator.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucket, key);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Locator.class.getSimpleName() + "{", "}")
            .add("bucket=" + bucket)
            .add("key=" + key)
            .toString();
    }

    public static Locator of(BucketName name, ObjectKey key) {
        return new Locator(name, key);
    }
}
