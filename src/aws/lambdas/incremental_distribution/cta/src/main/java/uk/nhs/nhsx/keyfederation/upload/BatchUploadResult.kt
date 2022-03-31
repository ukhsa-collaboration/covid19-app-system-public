package uk.nhs.nhsx.keyfederation.upload

import java.time.Instant

data class BatchUploadResult(
    val lastUploadedSubmissionTime: Instant,
    val submissionCount: Int
)
