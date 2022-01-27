package uk.nhs.nhsx.localstats.domain

import dev.forkhandles.values.InstantValue
import dev.forkhandles.values.InstantValueFactory
import java.time.Duration
import java.time.Instant

class ReleaseDate private constructor(value: Instant) : InstantValue(value) {

    fun isSafeToDownload(now: Instant): Boolean {
        // we've been told to give it 10-20 minutes before
        // downloading the latest stats because of caching
        // at GOV.UK
        val safeWindow = value.plus(Duration.ofMinutes(10))
        return now.isAfter(safeWindow) || now.compareTo(value) == 0
    }

    fun isAfter(now: Instant) = value.isAfter(now)

    companion object : InstantValueFactory<ReleaseDate>(::ReleaseDate)
}
