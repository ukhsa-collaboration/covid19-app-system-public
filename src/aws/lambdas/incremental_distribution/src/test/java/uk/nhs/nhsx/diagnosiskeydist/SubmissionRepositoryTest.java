package uk.nhs.nhsx.diagnosiskeydist;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.jupiter.api.Test;
import uk.nhs.nhsx.testhelper.data.TestData;
import uk.nhs.nhsx.diagnosiskeydist.s3.SubmissionFromS3Repository;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.nhs.nhsx.testhelper.data.TestData.STORED_KEYS_PAYLOAD_DESERIALIZED;
import static uk.nhs.nhsx.testhelper.data.TestData.STORED_KEYS_PAYLOAD_DESERIALIZED_DAYS_SINCE_ONSET;

public class SubmissionRepositoryTest {

    @Test
    public void deserializesStoredPayload() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(TestData.STORED_KEYS_PAYLOAD.getBytes());

        StoredTemporaryExposureKeyPayload payload = SubmissionRepository.getTemporaryExposureKeys(inputStream);
        assertThat(payload.temporaryExposureKeys).isNotEmpty();
        assertThat(payload.temporaryExposureKeys.stream().allMatch(it -> matchKey(it, STORED_KEYS_PAYLOAD_DESERIALIZED))).isTrue();
    }

    @Test
    public void deserializesStoredPayloadWithDaysSinceOnset() throws IOException {
        InputStream inputStream = new ByteArrayInputStream(TestData.STORED_KEYS_PAYLOAD_DAYS_SINCE_ONSET.getBytes());

        StoredTemporaryExposureKeyPayload payload = SubmissionRepository.getTemporaryExposureKeys(inputStream);
        assertThat(payload.temporaryExposureKeys).isNotEmpty();
        assertThat(payload.temporaryExposureKeys.stream().allMatch(it -> matchKey(it, STORED_KEYS_PAYLOAD_DESERIALIZED_DAYS_SINCE_ONSET))).isTrue();
    }

    private boolean matchKey(StoredTemporaryExposureKey storedKey, StoredTemporaryExposureKeyPayload deserializedPayload) {
        return deserializedPayload.temporaryExposureKeys.stream()
                .anyMatch(k ->
                        k.key.equals(storedKey.key) &&
                                k.rollingPeriod.equals(storedKey.rollingPeriod) &&
                                k.rollingStartNumber.equals(storedKey.rollingStartNumber) &&
                                k.transmissionRisk.equals(storedKey.transmissionRisk) &&
                                ((k.daysSinceOnsetOfSymptoms == null && storedKey.daysSinceOnsetOfSymptoms == null) ||
                                        (k.daysSinceOnsetOfSymptoms != null && k.daysSinceOnsetOfSymptoms.equals(storedKey.daysSinceOnsetOfSymptoms)))
                );
    }

    private long now = System.currentTimeMillis();

    @Test
    public void objectSummaryLimitTests() {
        long now = System.currentTimeMillis();
        List<S3ObjectSummary> summaries = Arrays.asList(
                summary(3, "a"),
                summary(3, "b"),
                summary(3, "c"),
                summary(2, "d"),
                summary(2, "e"),
                summary(2, "f"),
                summary(1, "g"),
                summary(1, "h"),
                summary(1, "j")
        );

        assertEquals(3, SubmissionFromS3Repository.limit(summaries, 1, 5).size());
        assertEquals(3, SubmissionFromS3Repository.limit(summaries, 2, 5).size());
        assertEquals(3, SubmissionFromS3Repository.limit(summaries, 3, 5).size());
        assertEquals(5, SubmissionFromS3Repository.limit(summaries, 4, 5).size());
        assertEquals(5, SubmissionFromS3Repository.limit(summaries, 5, 5).size());
        assertEquals(5, SubmissionFromS3Repository.limit(summaries, 6, 5).size());

        assertEquals(9, SubmissionFromS3Repository.limit(summaries, 7, 100).size());
    }

    private S3ObjectSummary summary(int ageSeconds, String key) {
        S3ObjectSummary summary = new S3ObjectSummary();
        summary.setLastModified(new Date(now - ageSeconds * 1000));
        summary.setKey(key);
        return summary;
    }
}