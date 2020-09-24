package uk.nhs.nhsx.virology;

import uk.nhs.nhsx.core.ValueType;
import uk.nhs.nhsx.core.random.crockford.CrockfordDammRandomStringGenerator.DammChecksum;

import static uk.nhs.nhsx.core.random.crockford.CrockfordDammRandomStringGenerator.checksum;

public class CtaToken extends ValueType<CtaToken> {

    private static final DammChecksum checksum = checksum();

    private CtaToken(String value) {
        super(value);
        if (!checksum.validate(value)) throw new IllegalArgumentException("validation error: Token Failed Validation");
    }

    public static CtaToken of(String value) {
        return new CtaToken(value);
    }
}
