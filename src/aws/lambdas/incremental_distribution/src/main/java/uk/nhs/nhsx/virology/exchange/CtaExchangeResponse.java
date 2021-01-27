package uk.nhs.nhsx.virology.exchange;

import uk.nhs.nhsx.virology.TestKit;

public class CtaExchangeResponse {

    public final String diagnosisKeySubmissionToken;
    public final String testResult;
    public final String testEndDate;
    public final TestKit testKit;

    public CtaExchangeResponse(String diagnosisKeySubmissionToken, String testResult, String testEndDate, TestKit testKit) {
        this.diagnosisKeySubmissionToken = diagnosisKeySubmissionToken;
        this.testResult = testResult;
        this.testEndDate = testEndDate;
        this.testKit = testKit;
    }

}
