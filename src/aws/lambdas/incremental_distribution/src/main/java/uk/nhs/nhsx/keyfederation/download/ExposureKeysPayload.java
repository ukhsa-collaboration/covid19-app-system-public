package uk.nhs.nhsx.keyfederation.download;

import com.fasterxml.jackson.annotation.JsonCreator;
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey;

import java.util.List;

public class ExposureKeysPayload {
    
    public final String origin;
    public final String batchTag;
    public final List<StoredTemporaryExposureKey> temporaryExposureKeys;

    @JsonCreator
    public ExposureKeysPayload(String origin, String batchTag, List<StoredTemporaryExposureKey> temporaryExposureKeys) {
        this.origin = origin;
        this.batchTag = batchTag;
        this.temporaryExposureKeys = temporaryExposureKeys;
    }

}