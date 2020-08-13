package uk.nhs.nhsx.diagnosiskeydist;

import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface SubmissionRepository {

    List<Submission> loadAllSubmissions() throws Exception;

    default StoredTemporaryExposureKeyPayload getTemporaryExposureKeys(InputStream jsonInputStream) throws IOException {
        return Jackson.readJson(jsonInputStream, StoredTemporaryExposureKeyPayload.class);
    }
}
