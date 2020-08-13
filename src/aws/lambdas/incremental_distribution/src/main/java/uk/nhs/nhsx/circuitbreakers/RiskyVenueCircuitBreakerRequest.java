package uk.nhs.nhsx.circuitbreakers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class RiskyVenueCircuitBreakerRequest {
    public final String venueId;

    @JsonCreator
    public RiskyVenueCircuitBreakerRequest(@JsonProperty String venueId) {
        this.venueId = venueId;
    }
}
