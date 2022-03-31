package uk.nhs.nhsx.keyfederation.storage

import uk.nhs.nhsx.keyfederation.domain.FederationBatch
import java.time.Instant

interface BatchTagService {
    fun lastUploadState(): UploadKeysResult?
    fun updateLastUploadState(lastUploadTimestamp: Instant)
    fun latestFederationBatch(): FederationBatch?
    fun updateLatestFederationBatch(batch: FederationBatch)
}
