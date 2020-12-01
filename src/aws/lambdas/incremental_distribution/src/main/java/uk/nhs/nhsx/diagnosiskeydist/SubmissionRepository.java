package uk.nhs.nhsx.diagnosiskeydist;

import uk.nhs.nhsx.core.Jackson;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public interface SubmissionRepository {

    default List<Submission> loadAllSubmissions() throws Exception {
        return loadAllSubmissions(0);
    }

    default List<Submission> loadAllSubmissions(long minimalSubmissionTimeEpocMillisExclusive) throws Exception {
        return loadAllSubmissions(minimalSubmissionTimeEpocMillisExclusive, Integer.MAX_VALUE, Integer.MAX_VALUE);
    }

    List<Submission> loadAllSubmissions(long minimalSubmissionTimeEpocMillisExclusive, int limit, int maxResults) throws Exception;

    static StoredTemporaryExposureKeyPayload getTemporaryExposureKeys(InputStream jsonInputStream) throws IOException {
        return Jackson.readJson(jsonInputStream, StoredTemporaryExposureKeyPayload.class);
    }
}
