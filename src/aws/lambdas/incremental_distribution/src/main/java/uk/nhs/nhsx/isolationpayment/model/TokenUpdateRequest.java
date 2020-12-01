package uk.nhs.nhsx.isolationpayment.model;

import uk.nhs.nhsx.core.DateFormatValidator;

public class TokenUpdateRequest {
    public final String ipcToken;
    public final String riskyEncounterDate;
    public final String isolationPeriodEndDate;

    public TokenUpdateRequest(String ipcToken, String riskyEncounterDate, String isolationPeriodEndDate) {
        this.ipcToken = ipcToken;
        this.riskyEncounterDate = riskyEncounterDate;
        this.isolationPeriodEndDate = isolationPeriodEndDate;
    }

    public static TokenUpdateRequest dateFormatValidator(TokenUpdateRequest request) {
        DateFormatValidator.formatter.parse(request.riskyEncounterDate);
        DateFormatValidator.formatter.parse(request.isolationPeriodEndDate);
        return request;
    }
}
