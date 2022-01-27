@file:Suppress("FunctionName")

package uk.nhs.nhsx.localstats.data.actions

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import uk.nhs.nhsx.localstats.data.ReducingCoronavirusApiAction
import uk.nhs.nhsx.localstats.domain.AreaType
import uk.nhs.nhsx.localstats.domain.AreaTypeCode
import uk.nhs.nhsx.localstats.domain.MetricResponse
import uk.nhs.nhsx.localstats.domain.ReleaseDate
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset.UTC

class DownloadMetrics(
    val areaType: AreaType,
    private val release: ReleaseDate = ReleaseDate.of(Instant.now(Clock.systemUTC()))
) : ReducingCoronavirusApiAction<MetricResponse> {
    private val maxAllowedMetricsPerDownload = 5
    private val csvParser = CoronavirusDataCSVParser(areaType.metrics)
    private val metrics = areaType.metrics.chunked(maxAllowedMetricsPerDownload).asSequence()

    override fun toRequest(): Sequence<Request> {
        val request = Request(GET, "/v2/data")
            .header("Accept", "text/csv")
            .query("format", "csv")
            .query("areaType", AreaTypeCode.from(areaType).value)
            .query("release", LocalDate.ofInstant(release.value, UTC).toString())

        return metrics.map { metricsToDownload ->
            metricsToDownload.fold(request) { req, metric -> req.query("metric", metric.name) }
        }
    }

    override fun fromResponse(response: Response) = when {
        response.status.successful -> MetricResponse(areaType, csvParser.parse(response.bodyString()))
        else -> error(response.toMessage())
    }

    override fun reduce(acc: MetricResponse, other: MetricResponse) = acc.plus(other)
}


