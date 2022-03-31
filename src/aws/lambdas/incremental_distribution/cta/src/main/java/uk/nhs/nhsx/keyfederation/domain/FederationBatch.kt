package uk.nhs.nhsx.keyfederation.domain

import uk.nhs.nhsx.domain.BatchTag
import java.time.LocalDate

data class FederationBatch(
    val batchTag: BatchTag,
    val batchDate: LocalDate
)
