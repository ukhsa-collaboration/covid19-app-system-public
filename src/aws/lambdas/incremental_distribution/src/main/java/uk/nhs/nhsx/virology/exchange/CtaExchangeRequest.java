package uk.nhs.nhsx.virology.exchange;

import com.fasterxml.jackson.annotation.JsonCreator;
import uk.nhs.nhsx.virology.CtaToken;

import java.util.Objects;

public class CtaExchangeRequest {
    
    public final CtaToken ctaToken;

    @JsonCreator
    public CtaExchangeRequest(CtaToken ctaToken) {
        this.ctaToken = ctaToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CtaExchangeRequest that = (CtaExchangeRequest) o;
        return Objects.equals(ctaToken, that.ctaToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ctaToken);
    }
}
