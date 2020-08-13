package uk.nhs.nhsx.diagnosiskeydist;

import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload;

import java.util.Date;

public class Submission {

    public final Date submissionDate;
    public final StoredTemporaryExposureKeyPayload payload;

    public Submission(Date submissionDate, StoredTemporaryExposureKeyPayload payload) {
        this.submissionDate = submissionDate;
        this.payload = payload;
    }

}
