package uk.nhs.nhsx.isolationpayment.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import uk.nhs.nhsx.core.DateFormatValidator;

import java.util.regex.Pattern;

public class TokenUpdateRequest {
    public static final Pattern VALID_TOKEN_PATTERN = Pattern.compile("[0-9A-Za-z]{64}");

    public final String ipcToken;
    public final String riskyEncounterDate;
    public final String isolationPeriodEndDate;

    public TokenUpdateRequest(String ipcToken, String riskyEncounterDate, String isolationPeriodEndDate) {
        this.ipcToken = ipcToken;
        this.riskyEncounterDate = riskyEncounterDate;
        this.isolationPeriodEndDate = isolationPeriodEndDate;
    }

    @JsonCreator
    public static TokenUpdateRequest validatedFrom(String ipcToken, String riskyEncounterDate, String isolationPeriodEndDate) {
        var request = new TokenUpdateRequest(ipcToken, riskyEncounterDate, isolationPeriodEndDate);
        DateFormatValidator.formatter.parse(request.riskyEncounterDate);
        DateFormatValidator.formatter.parse(request.isolationPeriodEndDate);
        if (!VALID_TOKEN_PATTERN.matcher(request.ipcToken).matches()) {
            throw new RuntimeException("Invalid token format");
        }
        return request;
    }
}
