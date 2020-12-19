package uk.nhs.nhsx.testhelper.mocks;

import com.google.common.io.ByteSource;
import org.apache.http.entity.ContentType;
import uk.nhs.nhsx.core.aws.s3.BucketName;
import uk.nhs.nhsx.core.aws.s3.MetaHeader;
import uk.nhs.nhsx.core.aws.s3.S3Storage;

import java.util.ArrayList;
import java.util.List;

public class FakeS3StorageMultipleObjects implements S3Storage {
    public int count = 0;
    public BucketName bucket;
    public List<FakeS3Object> fakeS3Objects = new ArrayList<>();

    @Override
    public void upload(Locator locator, ContentType contentType, ByteSource bytes, MetaHeader[] meta) {
        this.count++;
        this.bucket = locator.bucket;
        this.fakeS3Objects.add(new FakeS3Object(locator.key, contentType, bytes, meta));
    }
}
