package uk.nhs.nhsx.keyfederation

import uk.nhs.nhsx.keyfederation.upload.lookup.UploadKeysResult
import java.time.LocalDate
import java.util.*

open class InMemoryBatchTagService(var batchTag: BatchTag? = null, var batchDate: LocalDate? = null) : BatchTagService {

    override fun getLastUploadState(): Optional<UploadKeysResult> {
        return Optional.empty()
    }

    override fun updateLastUploadState(lastUploadState: Long?) {

    }

    override fun getLatestFederationBatch(): Optional<BatchTagService.FederationBatch> {
        return if (batchTag != null && batchDate != null) {
            Optional.of(BatchTagService.FederationBatch(batchTag, batchDate))
        } else {
            Optional.empty()
        }
    }

    override fun updateLatestFederationBatch(batch: BatchTagService.FederationBatch) {
        this.batchTag = batch.batchTag
        this.batchDate = batch.batchDate
    }

}