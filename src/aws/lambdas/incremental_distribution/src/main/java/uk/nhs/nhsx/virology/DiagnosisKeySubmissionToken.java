package uk.nhs.nhsx.virology;

import uk.nhs.nhsx.core.ValueType;

import static com.google.common.base.Preconditions.checkArgument;

public class DiagnosisKeySubmissionToken extends ValueType<DiagnosisKeySubmissionToken> {

    private DiagnosisKeySubmissionToken(String value) {
        super(value);
        checkArgument(!value.isEmpty(), "Diagnosis key submission token not present");
    }

    public static DiagnosisKeySubmissionToken of(String value) {
        return new DiagnosisKeySubmissionToken(value);
    }
}
