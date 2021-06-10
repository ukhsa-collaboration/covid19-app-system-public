package uk.nhs.nhsx.keyfederation.upload

import uk.nhs.nhsx.domain.BatchTag

data class DiagnosisKeysUploadRequest(val batchTag: BatchTag, val payload: String)
