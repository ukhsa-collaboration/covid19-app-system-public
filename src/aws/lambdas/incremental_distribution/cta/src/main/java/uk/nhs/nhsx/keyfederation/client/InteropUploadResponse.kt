package uk.nhs.nhsx.keyfederation.client

import uk.nhs.nhsx.domain.BatchTag

data class InteropUploadResponse(
    val batchTag: BatchTag,
    val insertedExposures: Int
)
