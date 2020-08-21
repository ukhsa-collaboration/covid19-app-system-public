package uk.nhs.nhsx.testkitorder;

import java.util.Objects;

public class TestKitOrderConfig {

    public final String testOrdersTable;
    public final String testResultsTable;
    public final String virologySubmissionTokensTable;
    public final String orderWebsite;
    public final String registerWebsite;
    public final int maxTokenPersistenceRetryCount;

    public TestKitOrderConfig(String testOrdersTable,
                              String testResultsTable,
                              String virologySubmissionTokensTable,
                              String orderWebsite,
                              String registerWebsite,
                              int maxTokenPersistenceRetryCount) {
        if (maxTokenPersistenceRetryCount < 1) {
            throw new IllegalArgumentException("Retry count for persistence must be >= 1");
        }
        this.testOrdersTable = testOrdersTable;
        this.testResultsTable = testResultsTable;
        this.virologySubmissionTokensTable = virologySubmissionTokensTable;
        this.orderWebsite = orderWebsite;
        this.registerWebsite = registerWebsite;
        this.maxTokenPersistenceRetryCount = maxTokenPersistenceRetryCount;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TestKitOrderConfig that = (TestKitOrderConfig) o;
        return maxTokenPersistenceRetryCount == that.maxTokenPersistenceRetryCount &&
            Objects.equals(testOrdersTable, that.testOrdersTable) &&
            Objects.equals(testResultsTable, that.testResultsTable) &&
            Objects.equals(virologySubmissionTokensTable, that.virologySubmissionTokensTable) &&
            Objects.equals(orderWebsite, that.orderWebsite) &&
            Objects.equals(registerWebsite, that.registerWebsite);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            testOrdersTable,
            testResultsTable,
            virologySubmissionTokensTable,
            orderWebsite,
            registerWebsite,
            maxTokenPersistenceRetryCount
        );
    }
}
