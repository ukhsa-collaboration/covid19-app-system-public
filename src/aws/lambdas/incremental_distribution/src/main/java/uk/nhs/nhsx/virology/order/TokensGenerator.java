package uk.nhs.nhsx.virology.order;

import uk.nhs.nhsx.core.random.crockford.CrockfordDammRandomStringGenerator;
import uk.nhs.nhsx.virology.persistence.TestOrder;

import java.util.UUID;

public class TokensGenerator {

    private final CrockfordDammRandomStringGenerator ctaTokenGenerator = new CrockfordDammRandomStringGenerator();

    public TestOrder generateVirologyTokens() {
        return new TestOrder(
            ctaTokenGenerator.generate(),
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString()
        );
    }

}
