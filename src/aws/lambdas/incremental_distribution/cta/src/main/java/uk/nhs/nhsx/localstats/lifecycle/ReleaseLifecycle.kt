package uk.nhs.nhsx.localstats.lifecycle

import uk.nhs.nhsx.localstats.domain.ReleaseDate

fun interface ReleaseLifecycle {
    fun isNewReleaseAvailable(): ReleaseDate?
}
