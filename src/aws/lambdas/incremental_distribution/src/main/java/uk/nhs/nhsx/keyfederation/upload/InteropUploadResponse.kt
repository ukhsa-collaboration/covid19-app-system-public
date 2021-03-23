package uk.nhs.nhsx.keyfederation.upload

import uk.nhs.nhsx.keyfederation.BatchTag

data class InteropUploadResponse(val batchTag: BatchTag, val insertedExposures: Int)
