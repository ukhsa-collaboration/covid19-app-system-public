package uk.nhs.nhsx.diagnosiskeyssubmission.model

import uk.nhs.nhsx.domain.TestKit
import java.util.UUID

data class ValidatedTemporaryExposureKeysPayload(
    val diagnosisKeySubmissionToken: UUID? = null,
    val temporaryExposureKeys: List<StoredTemporaryExposureKey>,
    val isPrivateJourney: Boolean? = false,
    val testKit: TestKit? = null
)
