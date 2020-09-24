package uk.nhs.nhsx.virology;

import java.util.Objects;

public class VirologyConfig {

    public static final int MAX_TOKEN_PERSISTENCE_RETRY_COUNT = 3;

    public final String testOrdersTable;
    public final String testResultsTable;
    public final String submissionTokensTable;
    public final int maxTokenPersistenceRetryCount;
    public final String testOrdersIndex;

    public VirologyConfig(String testOrdersTable,
                          String testResultsTable,
                          String submissionTokensTable,
                          String testOrdersIndex,
                          int maxTokenPersistenceRetryCount) {
        if (maxTokenPersistenceRetryCount < 1) {
            throw new IllegalArgumentException("Retry count for persistence must be >= 1");
        }
        this.testOrdersTable = testOrdersTable;
        this.testResultsTable = testResultsTable;
        this.submissionTokensTable = submissionTokensTable;
        this.maxTokenPersistenceRetryCount = maxTokenPersistenceRetryCount;
        this.testOrdersIndex = testOrdersIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirologyConfig that = (VirologyConfig) o;
        return maxTokenPersistenceRetryCount == that.maxTokenPersistenceRetryCount &&
            Objects.equals(testOrdersTable, that.testOrdersTable) &&
            Objects.equals(testResultsTable, that.testResultsTable) &&
            Objects.equals(submissionTokensTable, that.submissionTokensTable) &&
            Objects.equals(testOrdersIndex, that.testOrdersIndex);

    }

    @Override
    public int hashCode() {
        return Objects.hash(
            testOrdersTable,
            testResultsTable,
            submissionTokensTable,
            maxTokenPersistenceRetryCount,
            testOrdersIndex
        );
    }
}
