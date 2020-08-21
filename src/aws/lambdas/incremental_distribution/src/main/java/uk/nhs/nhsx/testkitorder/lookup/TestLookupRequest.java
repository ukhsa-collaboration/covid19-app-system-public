package uk.nhs.nhsx.testkitorder.lookup;

import com.fasterxml.jackson.annotation.JsonCreator;
import uk.nhs.nhsx.testkitorder.TestResultPollingToken;

public class TestLookupRequest {

    public final TestResultPollingToken testResultPollingToken;

    @JsonCreator
    public TestLookupRequest(TestResultPollingToken testResultPollingToken) {
        this.testResultPollingToken = testResultPollingToken;
    }
}
