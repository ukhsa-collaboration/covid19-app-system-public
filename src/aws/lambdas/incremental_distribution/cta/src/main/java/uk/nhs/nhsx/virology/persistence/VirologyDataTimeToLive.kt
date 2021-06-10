package uk.nhs.nhsx.virology.persistence

import java.time.Instant

data class VirologyDataTimeToLive(val testDataExpireAt: Instant, val submissionDataExpireAt: Instant)
