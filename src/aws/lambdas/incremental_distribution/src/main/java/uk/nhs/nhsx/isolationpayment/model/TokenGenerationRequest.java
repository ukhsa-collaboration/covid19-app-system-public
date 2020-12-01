package uk.nhs.nhsx.isolationpayment.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenGenerationRequest {
    public final String country;

    @JsonCreator
    public TokenGenerationRequest(@JsonProperty("country") String country) {
        this.country = country;
    }
}
