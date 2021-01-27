package uk.nhs.nhsx.virology.exchange;

import com.fasterxml.jackson.annotation.JsonCreator;
import uk.nhs.nhsx.virology.Country;
import uk.nhs.nhsx.virology.CtaToken;

import java.util.Objects;

public class CtaExchangeRequestV2 {

    public final CtaToken ctaToken;
    public final Country country;

    @JsonCreator
    public CtaExchangeRequestV2(CtaToken ctaToken, Country country) {
        this.ctaToken = ctaToken;
        this.country = country;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CtaExchangeRequestV2 that = (CtaExchangeRequestV2) o;
        return Objects.equals(ctaToken, that.ctaToken) && Objects.equals(country, that.country);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ctaToken, country);
    }
}
