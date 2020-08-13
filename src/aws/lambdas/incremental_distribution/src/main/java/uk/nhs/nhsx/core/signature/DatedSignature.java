package uk.nhs.nhsx.core.signature;

import java.time.Instant;

public class DatedSignature {

    public static class SignatureDate {
        public final String string;
        public final Instant instant;

        public SignatureDate(String string, Instant instant) {
            this.string = string;
            this.instant = instant;
        }
    }

    public final SignatureDate signatureDate;
    public final Signature signature;

    public DatedSignature(SignatureDate signatureDate, Signature signature) {
        this.signatureDate = signatureDate;
        this.signature = signature;
    }
}
