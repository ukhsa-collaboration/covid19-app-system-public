package uk.nhs.nhsx.keyfederation.upload

import uk.nhs.nhsx.domain.BatchTag

data class InteropUploadResponse(val batchTag: BatchTag, val insertedExposures: Int)
