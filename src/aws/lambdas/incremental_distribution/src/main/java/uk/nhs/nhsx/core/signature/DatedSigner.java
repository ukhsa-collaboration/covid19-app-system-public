package uk.nhs.nhsx.core.signature;

import java.util.function.Function;

public interface DatedSigner {
    DatedSignature sign(Function<DatedSignature.SignatureDate, byte[]> content);
}
