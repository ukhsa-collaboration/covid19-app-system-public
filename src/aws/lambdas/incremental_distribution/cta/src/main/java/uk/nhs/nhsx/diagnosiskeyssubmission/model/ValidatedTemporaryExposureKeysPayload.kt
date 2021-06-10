package uk.nhs.nhsx.diagnosiskeyssubmission.model

import java.util.UUID

data class ValidatedTemporaryExposureKeysPayload(
    val diagnosisKeySubmissionToken: UUID,
    val temporaryExposureKeys: List<StoredTemporaryExposureKey>
)
