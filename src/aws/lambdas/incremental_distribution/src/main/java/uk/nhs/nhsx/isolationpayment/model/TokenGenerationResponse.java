package uk.nhs.nhsx.isolationpayment.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import uk.nhs.nhsx.virology.IpcTokenId;

public class TokenGenerationResponse {

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public IpcTokenId ipcToken;
    public boolean isEnabled;

    @JsonCreator
    public TokenGenerationResponse(@JsonProperty("isEnabled") boolean isEnabled) {
        this.isEnabled = isEnabled;
    }

    public TokenGenerationResponse(boolean isEnabled, IpcTokenId ipcToken) {
        this.ipcToken = ipcToken;
        this.isEnabled = isEnabled;
    }
}
