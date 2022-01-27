@file:Suppress("TestFunctionName", "SpellCheckingInspection")

package uk.nhs.nhsx.localstats.data

import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.Path
import org.http4k.lens.Query
import org.http4k.lens.value
import org.http4k.routing.bind
import org.http4k.routing.routes
import uk.nhs.nhsx.localstats.data.CoronavirusApiResponses.LTLA_HARINGEY_DAILY_FIRST
import uk.nhs.nhsx.localstats.data.CoronavirusApiResponses.LTLA_HARINGEY_DAILY_SECOND
import uk.nhs.nhsx.localstats.data.CoronavirusApiResponses.LTLA_METRIC_AVAILABILITY
import uk.nhs.nhsx.localstats.data.CoronavirusApiResponses.NATION_DAILY
import uk.nhs.nhsx.localstats.data.CoronavirusApiResponses.NATION_METRIC_AVAILABILITY
import uk.nhs.nhsx.localstats.domain.AreaTypeCode
import uk.nhs.nhsx.localstats.domain.AreaTypeCode.Companion.LTLA
import uk.nhs.nhsx.localstats.domain.AreaTypeCode.Companion.NATION

fun DownloadHandler(
    vararg responses: CoronavirusApiResponses = arrayOf(
        LTLA_HARINGEY_DAILY_FIRST,
        LTLA_HARINGEY_DAILY_SECOND
    )
) = DownloadHandler(ArrayDeque(responses.toList()))

fun DownloadHandler(responses: ArrayDeque<CoronavirusApiResponses>): HttpHandler = {
    when (Query.value(AreaTypeCode).required("areaType")(it)) {
        LTLA -> responses.removeFirstOrNull()
            ?.let { resp -> Response(OK).body(resp) }
            ?: Response(INTERNAL_SERVER_ERROR)
        NATION -> Response(OK).body(NATION_DAILY)
        else -> Response(INTERNAL_SERVER_ERROR)
    }
}

fun MetricAvailabilityHandler(): HttpHandler = { req ->
    when (Path.value(AreaTypeCode).of("areaTypeCode")(req)) {
        LTLA -> Response(OK).body(LTLA_METRIC_AVAILABILITY)
        NATION -> Response(OK).body(NATION_METRIC_AVAILABILITY)
        else -> Response(INTERNAL_SERVER_ERROR)
    }
}

class FakeCoronavirusApiBackend(handler: HttpHandler = DownloadHandler()) : ChaoticHttpHandler() {
    override val app = routes(
        "/v2/data" bind GET to handler,
        "/generic/metric_availability/{areaTypeCode}" bind GET to MetricAvailabilityHandler()
    )
}

enum class CoronavirusApiResponses(private val filename: String) {
    LTLA_HARINGEY_DAILY_FULL("ltla_E09000014_2021-11-16.csv"),
    LTLA_HARINGEY_DAILY_FIRST("ltla_E09000014_2021-11-16_0.csv"),
    LTLA_HARINGEY_DAILY_SECOND("ltla_E09000014_2021-11-16_1.csv"),
    LTLA_REDCAR_DAILY_FIRST("ltla_E06000003_2021-11-16_0.csv"),
    LTLA_REDCAR_DAILY_SECOND("ltla_E06000003_2021-11-16_1.csv"),
    LTLA_METRIC_AVAILABILITY("ltla_metric_availability_2021-11-16.json"),
    NATION_METRIC_AVAILABILITY("nation_metric_availability_2021-11-16.json"),
    NATION_DAILY("nation_2021-11-16.csv");

    fun read() = javaClass
        .getResource("/coronavirus-api/${filename}")
        ?.readText()
        ?: error("could not read: $this")
}

fun Response.body(response: CoronavirusApiResponses) = body(response.read())
