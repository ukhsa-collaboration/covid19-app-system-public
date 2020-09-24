package uk.nhs.nhsx.virology.exchange;

public class CtaExchangeResponse {

    public final String diagnosisKeySubmissionToken;
    public final String testResult;
    public final String testEndDate;

    public CtaExchangeResponse(String diagnosisKeySubmissionToken, String testResult, String testEndDate) {
        this.diagnosisKeySubmissionToken = diagnosisKeySubmissionToken;
        this.testResult = testResult;
        this.testEndDate = testEndDate;
    }

}
