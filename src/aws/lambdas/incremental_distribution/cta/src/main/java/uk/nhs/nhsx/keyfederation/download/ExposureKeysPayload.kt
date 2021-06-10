package uk.nhs.nhsx.keyfederation.download

import uk.nhs.nhsx.diagnosiskeyssubmission.model.StoredTemporaryExposureKey
import uk.nhs.nhsx.domain.BatchTag

data class ExposureKeysPayload(
    val origin: String,
    val batchTag: BatchTag,
    val temporaryExposureKeys: List<StoredTemporaryExposureKey>
)
