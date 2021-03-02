package uk.nhs.nhsx.core.aws.s3;

import org.apache.http.entity.ContentType;

import java.util.List;

public interface S3Storage {

    void upload(Locator locator, ContentType contentType, ByteArraySource bytes, List<MetaHeader> metaHeaders);

    default void upload(Locator locator, ContentType contentType, ByteArraySource bytes) {
        upload(locator, contentType, bytes, List.of());
    }
}
