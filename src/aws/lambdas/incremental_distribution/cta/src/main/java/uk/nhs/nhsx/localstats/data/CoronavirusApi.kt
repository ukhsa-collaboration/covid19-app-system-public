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

sealed interface CoronavirusApiAction<R>

interface SingleCoronavirusApiAction<R> : CoronavirusApiAction<R> {
    fun toRequest(): Request
    fun fromResponse(response: Response): R
}

interface ReducingCoronavirusApiAction<R> : CoronavirusApiAction<R> {
    fun toRequest(): Sequence<Request>
    fun fromResponse(response: Response): R
    fun reduce(acc: R, other: R): R
}

interface CoronavirusApi {
    operator fun <R : Any> invoke(action: CoronavirusApiAction<R>): R

    companion object
}

fun CoronavirusApi.Companion.Http(
    client: HttpHandler,
    baseUri: Uri = Uri.of("https://api.coronavirus.data.gov.uk"),
    backoff: Duration = Duration.ofSeconds(2)
) = object : CoronavirusApi {

    private val retry = RetryFailures(
        Retry.of(
            "retrying-api-coronavirus", RetryConfig.custom<RetryConfig>()
                .maxAttempts(2)
                .intervalFunction(ofExponentialBackoff(backoff))
                .build()
        ),
        isError = { it.status.serverError }
    )

    private val http = SetBaseUriFrom(baseUri).then(retry).then(client)

    override fun <R : Any> invoke(action: CoronavirusApiAction<R>) = when (action) {
        is SingleCoronavirusApiAction -> action.fromResponse(http(action.toRequest()))
        is ReducingCoronavirusApiAction -> action.toRequest()
            .map(http::invoke)
            .map(action::fromResponse)
            .reduce(action::reduce)
    }
}
