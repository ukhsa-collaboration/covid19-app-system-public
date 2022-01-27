package uk.nhs.nhsx.keyfederation

import uk.nhs.nhsx.keyfederation.upload.lookup.UploadKeysResult
import java.time.Instant
import java.util.*

interface BatchTagService {
    fun lastUploadState(): UploadKeysResult?
    fun updateLastUploadState(lastUploadTimestamp: Instant)
    fun latestFederationBatch(): FederationBatch?
    fun updateLatestFederationBatch(batch: FederationBatch)
}
