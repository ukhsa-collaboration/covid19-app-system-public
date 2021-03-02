package uk.nhs.nhsx.virology.lookup;

import uk.nhs.nhsx.virology.TestKit;

public class VirologyLookupResponseV2 {

    public final String testEndDate;
    public final String testResult;
    public final TestKit testKit;
    public final Boolean diagnosisKeySubmissionSupported;
    public final Boolean requiresConfirmatoryTest;

    public VirologyLookupResponseV2(String testEndDate,
                                    String testResult,
                                    TestKit testKit,
                                    Boolean diagnosisKeySubmissionSupported,
                                    Boolean requiresConfirmatoryTest) {
        this.testEndDate = testEndDate;
        this.testResult = testResult;
        this.testKit = testKit;
        this.diagnosisKeySubmissionSupported = diagnosisKeySubmissionSupported;
        this.requiresConfirmatoryTest = requiresConfirmatoryTest;
    }

}
