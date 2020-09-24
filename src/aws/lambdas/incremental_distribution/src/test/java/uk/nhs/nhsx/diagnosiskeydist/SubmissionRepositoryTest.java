package uk.nhs.nhsx.diagnosiskeydist;

import org.junit.Test;
import uk.nhs.nhsx.TestData;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.nhs.nhsx.TestData.STORED_KEYS_PAYLOAD_DESERIALIZED;
import static uk.nhs.nhsx.TestData.STORED_KEYS_PAYLOAD_DESERIALIZED_DAYS_SINCE_ONSET;

public class SubmissionRepositoryTest {

    @Test
    public void deserializesStoredPayload() throws IOException {
        SubmissionRepository repository = Collections::emptyList;

        InputStream inputStream = new ByteArrayInputStream(TestData.STORED_KEYS_PAYLOAD.getBytes());

        StoredTemporaryExposureKeyPayload payload = repository.getTemporaryExposureKeys(inputStream);
        assertThat(payload.temporaryExposureKeys).isNotEmpty();
        assertThat(payload.temporaryExposureKeys.stream().allMatch(it -> matchKey(it, STORED_KEYS_PAYLOAD_DESERIALIZED))).isTrue();
    }

    @Test
    public void deserializesStoredPayloadWithDaysSinceOnset() throws IOException {
        SubmissionRepository repository = Collections::emptyList;

        InputStream inputStream = new ByteArrayInputStream(TestData.STORED_KEYS_PAYLOAD_DAYS_SINCE_ONSET.getBytes());

        StoredTemporaryExposureKeyPayload payload = repository.getTemporaryExposureKeys(inputStream);
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

}