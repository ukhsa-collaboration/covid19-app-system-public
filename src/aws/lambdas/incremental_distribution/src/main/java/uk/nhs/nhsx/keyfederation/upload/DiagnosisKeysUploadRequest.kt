package uk.nhs.nhsx.keyfederation.upload

import uk.nhs.nhsx.keyfederation.BatchTag

data class DiagnosisKeysUploadRequest(val batchTag: BatchTag, val payload: String)
