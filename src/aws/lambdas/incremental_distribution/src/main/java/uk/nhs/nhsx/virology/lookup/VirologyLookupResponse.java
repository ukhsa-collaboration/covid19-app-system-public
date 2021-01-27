package uk.nhs.nhsx.virology.lookup;

import uk.nhs.nhsx.virology.TestKit;

public class VirologyLookupResponse {

    public final String testEndDate;
    public final String testResult;
    public final TestKit testKit;

    public VirologyLookupResponse(String testEndDate, String testResult, TestKit testKit) {
        this.testEndDate = testEndDate;
        this.testResult = testResult;
        this.testKit = testKit;
    }

}
