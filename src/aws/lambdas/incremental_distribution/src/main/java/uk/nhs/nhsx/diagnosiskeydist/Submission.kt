package uk.nhs.nhsx.diagnosiskeydist

import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKeyPayload
import java.time.Instant

data class Submission(
    val submissionDate: Instant,
    val objectKey: ObjectKey,
    val payload: StoredTemporaryExposureKeyPayload
)
