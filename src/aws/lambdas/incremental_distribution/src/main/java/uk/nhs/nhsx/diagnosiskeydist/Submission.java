package uk.nhs.nhsx.diagnosiskeydist;

import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload;

import java.time.Instant;

public class Submission {

    public final Instant submissionDate;
    public final StoredTemporaryExposureKeyPayload payload;

    public Submission(Instant submissionDate, StoredTemporaryExposureKeyPayload payload) {
        this.submissionDate = submissionDate;
        this.payload = payload;
    }

}
