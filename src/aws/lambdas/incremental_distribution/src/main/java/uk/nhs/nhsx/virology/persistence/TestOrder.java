package uk.nhs.nhsx.virology.persistence;

import uk.nhs.nhsx.virology.CtaToken;
import uk.nhs.nhsx.virology.DiagnosisKeySubmissionToken;
import uk.nhs.nhsx.virology.TestResultPollingToken;

public class TestOrder {

    public final CtaToken ctaToken;
    public final TestResultPollingToken testResultPollingToken;
    public final DiagnosisKeySubmissionToken diagnosisKeySubmissionToken;
    public final int downloadCounter;

    public TestOrder(String ctaToken, String testResultPollingToken, String diagnosisKeySubmissionToken) {
        this(ctaToken,testResultPollingToken,diagnosisKeySubmissionToken,0);
    }
    public TestOrder(String ctaToken, String testResultPollingToken, String diagnosisKeySubmissionToken, int downloadCounter) {
        this.ctaToken = CtaToken.of(ctaToken);
        this.testResultPollingToken = TestResultPollingToken.of(testResultPollingToken);
        this.diagnosisKeySubmissionToken = DiagnosisKeySubmissionToken.of(diagnosisKeySubmissionToken);
        this.downloadCounter = downloadCounter;
    }


}