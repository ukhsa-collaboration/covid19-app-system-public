package uk.nhs.nhsx.diagnosiskeyssubmission.model

import java.util.*

data class ClientTemporaryExposureKeysPayload(
    val diagnosisKeySubmissionToken: UUID,
    val temporaryExposureKeys: List<ClientTemporaryExposureKey?>
)
