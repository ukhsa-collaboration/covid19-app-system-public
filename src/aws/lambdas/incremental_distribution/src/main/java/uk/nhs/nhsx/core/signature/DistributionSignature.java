package uk.nhs.nhsx.core.signature;

import com.google.common.io.ByteSource;
import com.google.common.primitives.Bytes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class DistributionSignature implements Function<DatedSignature.SignatureDate, byte[]> {

    private final ByteSource bytes;

    public DistributionSignature(ByteSource bytes) {
        this.bytes = bytes;
    }

    @Override
    public byte[] apply(DatedSignature.SignatureDate sd) {
        try {
            return Bytes.concat(
                String.format("%s:", sd.string).getBytes(StandardCharsets.UTF_8),
                bytes.read()
            );
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read " + bytes, e);
        }
    }
}
