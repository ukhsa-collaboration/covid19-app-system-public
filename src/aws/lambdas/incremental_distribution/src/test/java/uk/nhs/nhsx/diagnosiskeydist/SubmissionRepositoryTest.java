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

public class SubmissionRepositoryTest {

    @Test
    public void deserializesStoredPayload() throws IOException {
        SubmissionRepository repository = Collections::emptyList;

        InputStream inputStream = new ByteArrayInputStream(TestData.STORED_KEYS_PAYLOAD.getBytes());

        StoredTemporaryExposureKeyPayload payload = repository.getTemporaryExposureKeys(inputStream);
        assertThat(payload.temporaryExposureKeys).isNotEmpty();
        assertThat(payload.temporaryExposureKeys.stream().allMatch(this::matchKey)).isTrue();
    }

    private boolean matchKey(StoredTemporaryExposureKey storedKey) {
        return TestData.STORED_KEYS_PAYLOAD_DESERIALIZED.temporaryExposureKeys.stream()
            .anyMatch(k ->
                k.key.equals(storedKey.key) &&
                    k.rollingPeriod.equals(storedKey.rollingPeriod) &&
                    k.rollingStartNumber.equals(storedKey.rollingStartNumber) &&
                    k.transmissionRisk.equals(storedKey.transmissionRisk)
            );
    }

}