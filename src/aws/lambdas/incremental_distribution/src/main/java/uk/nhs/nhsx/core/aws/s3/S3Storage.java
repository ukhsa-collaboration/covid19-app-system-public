package uk.nhs.nhsx.core.aws.s3;

import com.google.common.io.ByteSource;
import org.apache.http.entity.ContentType;

public interface S3Storage {
    
    class Locator {
        public final BucketName bucket;
        public final ObjectKey key;

        public Locator(BucketName bucket, ObjectKey key) {
            this.bucket = bucket;
            this.key = key;
        }

        @Override
        public String toString() {
            return "Locator{" +
                "bucket=" + bucket +
                ", key=" + key +
                '}';
        }

        public static Locator of(BucketName name, ObjectKey key)  {
            return new Locator(name, key);
        }
    }
    
    void upload(Locator locator, ContentType contentType, ByteSource bytes, MetaHeader... meta);
}
