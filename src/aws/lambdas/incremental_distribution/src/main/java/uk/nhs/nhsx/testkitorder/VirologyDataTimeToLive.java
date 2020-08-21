package uk.nhs.nhsx.testkitorder;

import java.util.Objects;

public class VirologyDataTimeToLive {

    public final TestResultPollingToken pollingToken;
    public final long testDataExpireAt;
    public final long submissionDataExpireAt;

    VirologyDataTimeToLive(TestResultPollingToken pollingToken, long testDataExpireAt, long submissionDataExpireAt) {
        this.pollingToken = pollingToken;
        this.testDataExpireAt = testDataExpireAt;
        this.submissionDataExpireAt = submissionDataExpireAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirologyDataTimeToLive that = (VirologyDataTimeToLive) o;
        return testDataExpireAt == that.testDataExpireAt &&
            submissionDataExpireAt == that.submissionDataExpireAt &&
            Objects.equals(pollingToken, that.pollingToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pollingToken, testDataExpireAt, submissionDataExpireAt);
    }
}