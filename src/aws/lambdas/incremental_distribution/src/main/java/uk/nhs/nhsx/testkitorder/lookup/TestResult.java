package uk.nhs.nhsx.testkitorder.lookup;

public class TestResult {

    public final String testResultPollingToken;
    public final String testEndDate;
    public final String testResult;
    public final String status;

    public TestResult(String testResultPollingToken, String testEndDate, String testResult, String status) {
        this.testResultPollingToken = testResultPollingToken;
        this.testEndDate = testEndDate;
        this.testResult = testResult;
        this.status = status;
    }

}
