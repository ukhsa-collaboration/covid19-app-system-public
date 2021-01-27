package uk.nhs.nhsx.core.aws.s3;

import com.google.common.io.ByteSource;
import org.apache.http.entity.ContentType;

public interface S3Storage {

    void upload(Locator locator, ContentType contentType, ByteSource bytes, MetaHeader... meta);
}
