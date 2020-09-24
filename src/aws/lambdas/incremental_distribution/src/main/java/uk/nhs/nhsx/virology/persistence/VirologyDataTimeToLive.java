package uk.nhs.nhsx.virology.persistence;

import java.util.Objects;

public class VirologyDataTimeToLive {

    public final long testDataExpireAt;
    public final long submissionDataExpireAt;

    public VirologyDataTimeToLive(long testDataExpireAt, long submissionDataExpireAt) {
        this.testDataExpireAt = testDataExpireAt;
        this.submissionDataExpireAt = submissionDataExpireAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirologyDataTimeToLive that = (VirologyDataTimeToLive) o;
        return testDataExpireAt == that.testDataExpireAt &&
            submissionDataExpireAt == that.submissionDataExpireAt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(testDataExpireAt, submissionDataExpireAt);
    }
}