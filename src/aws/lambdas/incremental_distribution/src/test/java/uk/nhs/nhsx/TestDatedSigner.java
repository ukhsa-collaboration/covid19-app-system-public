package uk.nhs.nhsx;

import com.amazonaws.services.kms.model.SigningAlgorithmSpec;
import uk.nhs.nhsx.core.signature.DatedSignature;
import uk.nhs.nhsx.core.signature.DatedSigner;
import uk.nhs.nhsx.core.signature.KeyId;
import uk.nhs.nhsx.core.signature.Signature;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class TestDatedSigner implements DatedSigner {
    private final String date;
    private final byte[] signatureBytes = {0, 1, 2, 3, 4};
    public List<byte[]> content = new ArrayList<>();
    public int count = 0;

    public KeyId keyId = KeyId.of("some-key");


    public TestDatedSigner(String date) {
        this.date = date;
    }

    @Override
    public DatedSignature sign(Function<DatedSignature.SignatureDate, byte[]> content) {
        count++;
        DatedSignature.SignatureDate date = new DatedSignature.SignatureDate(this.date, Instant.EPOCH);
        this.content.add(content.apply(date));
        return new DatedSignature(date, new Signature(keyId, SigningAlgorithmSpec.ECDSA_SHA_256, signatureBytes));
    }
}
