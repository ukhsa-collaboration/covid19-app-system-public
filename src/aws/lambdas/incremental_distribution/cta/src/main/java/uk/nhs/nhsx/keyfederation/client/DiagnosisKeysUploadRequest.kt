package uk.nhs.nhsx.keyfederation.client

import uk.nhs.nhsx.domain.BatchTag

data class DiagnosisKeysUploadRequest(
    val batchTag: BatchTag,
    val payload: String
)
