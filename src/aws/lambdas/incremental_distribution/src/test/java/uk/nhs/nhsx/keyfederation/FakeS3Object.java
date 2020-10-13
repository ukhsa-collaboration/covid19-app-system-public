package uk.nhs.nhsx.keyfederation;

import com.google.common.io.ByteSource;
import org.apache.http.entity.ContentType;
import uk.nhs.nhsx.core.aws.s3.MetaHeader;
import uk.nhs.nhsx.core.aws.s3.ObjectKey;

public class FakeS3Object {
    public final ObjectKey name;
    public final ContentType contentType;
    public final ByteSource bytes;
    public final MetaHeader[] meta;

    public FakeS3Object(ObjectKey name, ContentType contentType, ByteSource bytes, MetaHeader[] meta) {
        this.name = name;
        this.contentType = contentType;
        this.bytes = bytes;
        this.meta = meta;
    }
}
