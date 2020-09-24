package uk.nhs.nhsx.virology.result;

import com.fasterxml.jackson.annotation.JsonCreator;

public class VirologyTokenGenResponse {

    public final String ctaToken;

    @JsonCreator
    private VirologyTokenGenResponse(String ctaToken) {
        this.ctaToken = ctaToken;
    }

    public static VirologyTokenGenResponse of(String ctaToken) {
        return new VirologyTokenGenResponse(ctaToken);
    }
}