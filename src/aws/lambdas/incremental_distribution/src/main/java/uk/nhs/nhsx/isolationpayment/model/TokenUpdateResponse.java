package uk.nhs.nhsx.isolationpayment.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenUpdateResponse {
    
    public final String websiteUrlWithQuery;

    @JsonCreator
    public TokenUpdateResponse(@JsonProperty("websiteUrlWithQuery") String websiteUrlWithQuery) {
        this.websiteUrlWithQuery = websiteUrlWithQuery;
    }
}
