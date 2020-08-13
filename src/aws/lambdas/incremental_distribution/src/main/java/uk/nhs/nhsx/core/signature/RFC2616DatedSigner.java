package uk.nhs.nhsx.core.signature;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.Function;
import java.util.function.Supplier;

public class RFC2616DatedSigner implements DatedSigner {

    private static final DateTimeFormatter format = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH);
    

    private final Supplier<Instant> clock;
    private final Signer signer;

    public RFC2616DatedSigner(Supplier<Instant> clock, Signer signer) {
        this.clock = clock;
        this.signer = signer;
    }

    @Override
    public DatedSignature sign(Function<DatedSignature.SignatureDate, byte[]> content) {

        Instant instant = clock.get();
        String date = format.format(instant.atZone(ZoneId.of("UTC")));

        DatedSignature.SignatureDate signatureDate = new DatedSignature.SignatureDate(date, instant);

        return new DatedSignature(signatureDate,signer.sign(content.apply(signatureDate)));
    }
}
