package uk.nhs.nhsx.testkitorder;

import com.fasterxml.jackson.annotation.JsonCreator;

public class TestLookupRequest {

    public final TestResultPollingToken testResultPollingToken;

    @JsonCreator
    public TestLookupRequest(TestResultPollingToken testResultPollingToken) {
        this.testResultPollingToken = testResultPollingToken;
    }
}
