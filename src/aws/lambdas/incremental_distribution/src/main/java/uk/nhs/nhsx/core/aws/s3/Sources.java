package uk.nhs.nhsx.core.aws.s3;

import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;

import java.nio.charset.StandardCharsets;

public class Sources {

    public static ByteSource byteSourceFor(String json) {
        return CharSource.wrap(json).asByteSource(StandardCharsets.UTF_8);
    }
}
