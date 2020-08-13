package uk.nhs.nhsx.diagnosiskeyssubmission.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;

public class StoredTemporaryExposureKeyPayload {

    public final List<StoredTemporaryExposureKey> temporaryExposureKeys;

    @JsonCreator
    public StoredTemporaryExposureKeyPayload(List<StoredTemporaryExposureKey> temporaryExposureKeys) {
        this.temporaryExposureKeys = temporaryExposureKeys;
    }
}
