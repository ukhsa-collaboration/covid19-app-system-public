package uk.nhs.nhsx.isolationpayment.model;

import com.fasterxml.jackson.annotation.JsonCreator;

public class IsolationRequest {
    public final String contractVersion = "1";
    public String ipcToken;

    @JsonCreator
    public IsolationRequest(String ipcToken) {
        this.ipcToken = ipcToken;
    }

    public IsolationRequest() { }
}
