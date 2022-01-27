@file:Suppress("FunctionName")

package uk.nhs.nhsx.localstats.data

import io.github.resilience4j.core.IntervalFunction.ofExponentialBackoff
import io.github.resilience4j.retry.Retry
import io.github.resilience4j.retry.RetryConfig
import org.http4k.core.HttpHandler
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters.SetBaseUriFrom
import org.http4k.filter.ResilienceFilters.RetryFailures
import java.time.Duration

interface CoronavirusWebsiteAction<R> {
    fun toRequest(): Request
    fun fromResponse(response: Response): R
}

interface CoronavirusWebsite {
    operator fun <R : Any> invoke(action: CoronavirusWebsiteAction<R>): R

    companion object
}

fun CoronavirusWebsite.Companion.Http(
    client: HttpHandler,
    baseUri: Uri = Uri.of("https://coronavirus.data.gov.uk"),
    backoff: Duration = Duration.ofSeconds(2)
) = object : CoronavirusWebsite {

    private val retry = RetryFailures(
        Retry.of(
            "retrying-website-coronavirus", RetryConfig.custom<RetryConfig>()
                .maxAttempts(2)
                .intervalFunction(ofExponentialBackoff(backoff))
                .build()
        ),
        isError = { !it.status.successful }
    )

    private val http = SetBaseUriFrom(baseUri).then(retry).then(client)

    override fun <R : Any> invoke(action: CoronavirusWebsiteAction<R>) =
        action.fromResponse(http(action.toRequest()))
}
