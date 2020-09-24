package uk.nhs.nhsx.core.aws.s3;

import com.google.common.io.ByteSource;
import com.google.common.io.CharSource;

import java.nio.charset.StandardCharsets;

public class Sources {

    public static ByteSource byteSourceFor(String data) {
        return CharSource.wrap(data).asByteSource(StandardCharsets.UTF_8);
    }
}
