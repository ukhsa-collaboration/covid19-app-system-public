package uk.nhs.nhsx.diagnosiskeyssubmission.model;

import com.fasterxml.jackson.annotation.JsonCreator;

import java.util.List;
import java.util.UUID;

import static java.util.Objects.requireNonNull;

public class ClientTemporaryExposureKeysPayload {

    public final UUID diagnosisKeySubmissionToken;
    public final List<ClientTemporaryExposureKey> temporaryExposureKeys;

    @JsonCreator
    public ClientTemporaryExposureKeysPayload(UUID diagnosisKeySubmissionToken,
                                              List<ClientTemporaryExposureKey> temporaryExposureKeys) {
        this.diagnosisKeySubmissionToken = requireNonNull(diagnosisKeySubmissionToken);
        this.temporaryExposureKeys = requireNonNull(temporaryExposureKeys);
    }
}
