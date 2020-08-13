package uk.nhs.nhsx.testkitorder;

public class TestOrderResponse {

    public final String websiteUrlWithQuery;
    public final String tokenParameterValue;
    public final String testResultPollingToken;
    public final String diagnosisKeySubmissionToken;

    public TestOrderResponse(String websiteUrlWithQuery,
                             String tokenParameterValue,
                             String testResultPollingToken,
                             String diagnosisKeySubmissionToken) {
        this.websiteUrlWithQuery = websiteUrlWithQuery;
        this.tokenParameterValue = tokenParameterValue;
        this.testResultPollingToken = testResultPollingToken;
        this.diagnosisKeySubmissionToken = diagnosisKeySubmissionToken;
    }
}
