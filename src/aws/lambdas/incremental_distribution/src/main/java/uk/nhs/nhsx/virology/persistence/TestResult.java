package uk.nhs.nhsx.virology.persistence;

import uk.nhs.nhsx.virology.TestKit;

public class TestResult {

    public final String testResultPollingToken;
    public final String testEndDate;
    public final String testResult;
    public final String status;
    public final TestKit testKit;

    public TestResult(String testResultPollingToken, String testEndDate, String testResult, String status, TestKit testKit) {
        this.testResultPollingToken = testResultPollingToken;
        this.testEndDate = testEndDate;
        this.testResult = testResult;
        this.status = status;
        this.testKit = testKit;
    }

    public boolean isPositive() {
        return "POSITIVE".equals(testResult);
    }

    public boolean isAvailable() {
        return "available".equals(status);
    }
}
