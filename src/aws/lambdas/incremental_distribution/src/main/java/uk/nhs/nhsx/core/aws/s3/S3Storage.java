package uk.nhs.nhsx.core.aws.s3;

import org.apache.http.entity.ContentType;

import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public interface S3Storage {

    void upload(Locator locator, ContentType contentType, ByteArraySource bytes, List<MetaHeader> metaHeaders);

    default void upload(Locator locator, ContentType contentType, ByteArraySource bytes) {
        upload(locator, contentType, bytes, List.of());
    }

    Optional<URL> getSignedURL(Locator locator, Date expiration);

}
