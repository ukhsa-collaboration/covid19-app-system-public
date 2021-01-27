package uk.nhs.nhsx.virology.exchange;

import uk.nhs.nhsx.virology.TestKit;

public class CtaExchangeResponseV2 {

    public final String diagnosisKeySubmissionToken;
    public final String testResult;
    public final String testEndDate;
    public final TestKit testKit;
    public final Boolean diagnosisKeySubmissionSupported;

    public CtaExchangeResponseV2(String diagnosisKeySubmissionToken,
                                 String testResult,
                                 String testEndDate,
                                 TestKit testKit,
                                 Boolean diagnosisKeySubmissionSupported) {
        this.diagnosisKeySubmissionToken = diagnosisKeySubmissionToken;
        this.testResult = testResult;
        this.testEndDate = testEndDate;
        this.testKit = testKit;
        this.diagnosisKeySubmissionSupported = diagnosisKeySubmissionSupported;
    }

}
