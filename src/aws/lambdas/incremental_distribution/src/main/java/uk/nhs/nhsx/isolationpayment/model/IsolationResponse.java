package uk.nhs.nhsx.isolationpayment.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class IsolationResponse {
    public final int contractVersion = 1;
    public final String ipcToken;
    public final String state;
    public final String riskyEncounterDate;
    public final String isolationPeriodEndDate;
    public final String createdTimestamp;
    public final String updatedTimestamp;

    public IsolationResponse(String ipcToken, String state, String riskyEncounterDate, String isolationPeriodEndDate,
                             String createdTimestamp, String updatedTimestamp) {
        this.ipcToken = ipcToken;
        this.state = state;
        this.riskyEncounterDate = riskyEncounterDate;
        this.isolationPeriodEndDate = isolationPeriodEndDate;
        this.createdTimestamp = createdTimestamp;
        this.updatedTimestamp = updatedTimestamp;
    }
    public IsolationResponse(String ipcToken, String state) {
        this.ipcToken = ipcToken;
        this.state = state;
        this.riskyEncounterDate = null;
        this.isolationPeriodEndDate = null;
        this.createdTimestamp = null;
        this.updatedTimestamp = null;
    }
}
