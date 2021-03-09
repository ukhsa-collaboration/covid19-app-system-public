package uk.nhs.nhsx.isolationpayment.model;

import uk.nhs.nhsx.virology.IpcTokenId;

public class IsolationToken {
    public IpcTokenId tokenId;
    public String tokenStatus;
    public Long riskyEncounterDate;
    public Long isolationPeriodEndDate;
    public Long createdTimestamp;
    public Long updatedTimestamp;
    public Long validatedTimestamp;
    public Long consumedTimestamp;
    public Long expireAt;

    public IsolationToken() {}

    public IsolationToken(IpcTokenId tokenId,
                          String tokenStatus,
                          Long riskyEncounterDate,
                          Long isolationPeriodEndDate,
                          Long createdTimestamp,
                          Long updatedTimestamp,
                          Long validatedTimestamp,
                          Long consumedTimestamp,
                          Long expireAt)
    {
        this.tokenId = tokenId;
        this.tokenStatus = tokenStatus;
        this.riskyEncounterDate = riskyEncounterDate;
        this.isolationPeriodEndDate = isolationPeriodEndDate;
        this.createdTimestamp = createdTimestamp;
        this.updatedTimestamp = updatedTimestamp;
        this.validatedTimestamp = validatedTimestamp;
        this.consumedTimestamp = consumedTimestamp;
        this.expireAt = expireAt;
    }

    public static IsolationToken clonedToken(IsolationToken token) {
        return new IsolationToken(
            token.tokenId,
            token.tokenStatus,
            token.riskyEncounterDate,
            token.isolationPeriodEndDate,
            token.createdTimestamp,
            token.updatedTimestamp,
            token.validatedTimestamp,
            token.consumedTimestamp,
            token.expireAt
        );
    }

    @Override
    public String toString() {
        return "{" +
            "tokenId='" + tokenId + '\'' +
            ", tokenStatus='" + tokenStatus + '\'' +
            ", riskyEncounterDate=" + riskyEncounterDate +
            ", isolationPeriodEndDate=" + isolationPeriodEndDate +
            ", createdTimestamp=" + createdTimestamp +
            ", updatedTimestamp=" + updatedTimestamp +
            ", validatedTimestamp=" + validatedTimestamp +
            ", consumedTimestamp=" + consumedTimestamp +
            ", expireAt=" + expireAt +
            '}';
    }
}
