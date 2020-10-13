package uk.nhs.nhsx.keyfederation.download;

import com.fasterxml.jackson.annotation.JsonCreator;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey;

import java.util.List;

public class ExposureKeysPayload {
    public final String region;
    public final String batchTag;
    public final List<StoredTemporaryExposureKey> temporaryExposureKeys;

    @JsonCreator
    public ExposureKeysPayload(String region, String batchTag, List<StoredTemporaryExposureKey> temporaryExposureKeys) {
        this.region = region;
        this.batchTag = batchTag;
        this.temporaryExposureKeys = temporaryExposureKeys;
    }

}