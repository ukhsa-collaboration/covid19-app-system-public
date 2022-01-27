package uk.nhs.nhsx.localstats.storage

import uk.nhs.nhsx.localstats.domain.DailyLocalStatsDocument
import java.time.Instant

interface DailyLocalStatsDocumentStorage {
    fun exists(): Boolean
    fun lastModified(): Instant
    fun put(document: DailyLocalStatsDocument)
}

