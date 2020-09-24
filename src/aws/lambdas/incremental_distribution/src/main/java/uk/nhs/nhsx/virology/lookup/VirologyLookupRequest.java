package uk.nhs.nhsx.virology.lookup;

import com.fasterxml.jackson.annotation.JsonCreator;
import uk.nhs.nhsx.virology.TestResultPollingToken;

public class VirologyLookupRequest {

    public final TestResultPollingToken testResultPollingToken;

    @JsonCreator
    public VirologyLookupRequest(TestResultPollingToken testResultPollingToken) {
        this.testResultPollingToken = testResultPollingToken;
    }
}
