package uk.nhs.nhsx.localstats.data.actions

import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import uk.nhs.nhsx.localstats.LocalStatsJson.auto
import uk.nhs.nhsx.localstats.data.SingleCoronavirusApiAction
import uk.nhs.nhsx.localstats.domain.AreaType
import uk.nhs.nhsx.localstats.domain.AreaTypeCode
import uk.nhs.nhsx.localstats.domain.MetricName
import java.time.LocalDate

data class MetricAvailability(val areaType: AreaType) : SingleCoronavirusApiAction<Map<MetricName, LocalDate>> {

    override fun toRequest() = Request(GET, "/generic/metric_availability/${AreaTypeCode.from(areaType)}")
        .header("Accept", "application/json")

    private val body = Body.auto<List<HttpMetricsMetadata>>().toLens()

    override fun fromResponse(response: Response) = when {
        response.status.successful -> body(response)
            .associateBy(HttpMetricsMetadata::metric)
            .mapValues { it.value.last_update }
        else -> error(response.toMessage())
    }
}

data class HttpMetricsMetadata(
    val metric: MetricName,
    val last_update: LocalDate
)
