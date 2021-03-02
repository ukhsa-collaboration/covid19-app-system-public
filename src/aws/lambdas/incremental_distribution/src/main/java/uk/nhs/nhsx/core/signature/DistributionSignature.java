package uk.nhs.nhsx.core.signature;

import uk.nhs.nhsx.core.aws.s3.ByteArraySource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Function;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static uk.nhs.nhsx.core.signature.DatedSignature.SignatureDate;

public class DistributionSignature implements Function<SignatureDate, byte[]> {

    private final ByteArraySource bytes;

    public DistributionSignature(ByteArraySource bytes) {
        this.bytes = bytes;
    }

    @Override
    public byte[] apply(SignatureDate signatureDate) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            out.write(format("%s:", signatureDate.string).getBytes(UTF_8));
            out.write(bytes.toArray());
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to read bytes", e);
        }
    }
}
