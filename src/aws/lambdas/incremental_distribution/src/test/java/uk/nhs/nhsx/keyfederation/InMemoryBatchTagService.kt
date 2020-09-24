package uk.nhs.nhsx.keyfederation

import uk.nhs.nhsx.keyfederation.upload.lookup.UploadKeysResult
import java.util.*

open class InMemoryBatchTagService(var batchTag: BatchTag? = null) : BatchTagService {

    override fun getLatestBatchTag(): BatchTag? = batchTag

    override fun updateLatestBatchTag(batchTag: BatchTag?) {
        this.batchTag = batchTag
    }

    override fun getLastUploadState(): Optional<UploadKeysResult> {
        return Optional.empty()
    }

    override fun updateLastUploadState(lastUploadState: String?) {

    }

}