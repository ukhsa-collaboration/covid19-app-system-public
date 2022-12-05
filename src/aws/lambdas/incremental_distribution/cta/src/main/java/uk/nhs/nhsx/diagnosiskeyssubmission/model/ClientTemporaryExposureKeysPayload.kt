package uk.nhs.nhsx.diagnosiskeyssubmission.model

import uk.nhs.nhsx.domain.TestKit
import java.util.*

data class ClientTemporaryExposureKeysPayload(
    val diagnosisKeySubmissionToken: UUID? = UUID.fromString("00000000-0000-0000-0000-000000000000"),
    val temporaryExposureKeys: List<ClientTemporaryExposureKey?>,
    val isPrivateJourney: Boolean? = false,
    val testKit: TestKit? = TestKit.LAB_RESULT
)
