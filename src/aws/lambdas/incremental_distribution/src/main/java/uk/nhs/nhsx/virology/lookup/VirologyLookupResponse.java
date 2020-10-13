package uk.nhs.nhsx.virology.lookup;

public class VirologyLookupResponse {

    public final String testEndDate;
    public final String testResult;

    public VirologyLookupResponse(String testEndDate, String testResult) {
        this.testEndDate = testEndDate;
        this.testResult = testResult;
    }

}
