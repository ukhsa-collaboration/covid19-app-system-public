package uk.nhs.nhsx.testhelper.mocks;

import com.amazonaws.services.s3.model.S3Object;
import com.google.common.io.ByteSource;
import org.apache.http.entity.ContentType;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.MetaHeader;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;
import uk.nhs.nhsx.core.aws.s3.S3Storage;

import java.util.Optional;

public class FakeS3Storage implements S3Storage {
    public int count = 0;
    public BucketName bucket;
    public ObjectKey name;
    public ContentType contentType;
    public ByteSource bytes;
    public MetaHeader[] meta;

    public Optional<S3Object> exists = Optional.empty();

    @Override
    public void upload(Locator locator, ContentType contentType, ByteSource bytes, MetaHeader[] meta) {
        overwriting(locator, contentType, bytes, meta);
    }

    private void overwriting(Locator locator, ContentType contentType, ByteSource bytes, MetaHeader[] meta) {
        this.count++;
        this.bucket = locator.bucket;
        this.name = locator.key;
        this.contentType = contentType;
        this.bytes = bytes;
        this.meta = meta;
    }
}
