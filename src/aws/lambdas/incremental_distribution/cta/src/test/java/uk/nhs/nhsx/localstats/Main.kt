package uk.nhs.nhsx.localstats

import org.http4k.client.JavaHttpClient
import org.http4k.filter.debug
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.localstats.data.CoronavirusApi
import uk.nhs.nhsx.localstats.data.CoronavirusWebsite
import uk.nhs.nhsx.localstats.data.Http
import uk.nhs.nhsx.localstats.data.actions.LatestReleaseTimestamp
import java.io.File
import java.io.PrintStream
import java.time.Clock

fun main() {
    val handler = JavaHttpClient().debug(
        out = PrintStream(File("http-out.log"))
    )

    val coronavirusWebsite = CoronavirusWebsite.Http(handler)
    val coronavirusApi = CoronavirusApi.Http(handler)

    val releaseDate = coronavirusWebsite(LatestReleaseTimestamp)

    val metricsDocument = DailyLocalStats(
        coronavirusApi = coronavirusApi,
        clock = SystemClock.CLOCK
    ).generateDocument(releaseDate)

    File("all.json").writeText(LocalStatsJson.asFormatString(metricsDocument))
}
