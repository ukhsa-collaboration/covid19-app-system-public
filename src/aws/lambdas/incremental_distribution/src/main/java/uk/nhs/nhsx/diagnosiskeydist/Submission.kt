package uk.nhs.nhsx.diagnosiskeydist

import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import java.time.Instant

data class Submission(
    val submissionDate: Instant,
    val payload: StoredTemporaryExposureKeyPayload
)
