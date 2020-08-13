package uk.nhs.nhsx.diagnosiskeyssubmission.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ClientTemporaryExposureKeysPayload {

    public final UUID diagnosisKeySubmissionToken;
    public final List<ClientTemporaryExposureKey> temporaryExposureKeys;

    @JsonCreator
    public ClientTemporaryExposureKeysPayload(UUID diagnosisKeySubmissionToken,
                                              List<ClientTemporaryExposureKey> temporaryExposureKeys) {
        Objects.requireNonNull(diagnosisKeySubmissionToken);
        Objects.requireNonNull(temporaryExposureKeys);
        this.diagnosisKeySubmissionToken = diagnosisKeySubmissionToken;
        this.temporaryExposureKeys = temporaryExposureKeys;
    }

}