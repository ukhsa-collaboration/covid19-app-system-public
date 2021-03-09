package uk.nhs.nhsx.isolationpayment.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import uk.nhs.nhsx.virology.IpcTokenId;

import java.time.Instant;

public class TokenUpdateRequest {
    public final IpcTokenId ipcToken;
    public final Instant riskyEncounterDate;
    public final Instant isolationPeriodEndDate;

    @JsonCreator
    public TokenUpdateRequest(IpcTokenId ipcToken, Instant riskyEncounterDate, Instant isolationPeriodEndDate) {
        this.ipcToken = ipcToken;
        this.riskyEncounterDate = riskyEncounterDate;
        this.isolationPeriodEndDate = isolationPeriodEndDate;
    }
}
