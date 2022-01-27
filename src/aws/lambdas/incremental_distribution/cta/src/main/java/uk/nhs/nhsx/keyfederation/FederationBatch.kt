package uk.nhs.nhsx.keyfederation

import uk.nhs.nhsx.domain.BatchTag
import java.time.LocalDate

data class FederationBatch(
    val batchTag: BatchTag,
    val batchDate: LocalDate
)
