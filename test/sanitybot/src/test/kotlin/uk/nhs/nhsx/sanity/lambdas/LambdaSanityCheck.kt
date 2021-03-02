package uk.nhs.nhsx.sanity.lambdas

import org.http4k.client.JavaHttpClient
import org.http4k.core.Filter
import org.http4k.core.Request
import org.http4k.core.then
import org.http4k.filter.ClientFilters.BearerAuth
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse
import uk.nhs.nhsx.sanity.BaseSanityCheck
import uk.nhs.nhsx.sanity.lambdas.config.DeployedLambda
import uk.nhs.nhsx.sanity.lambdas.config.HealthCheck
import uk.nhs.nhsx.sanity.lambdas.config.Secured

abstract class LambdaSanityCheck : BaseSanityCheck() {

    private val setBaseHeaders = Filter { next ->
        {
            next(it
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
            )
        }
    }

    fun Secured.withSecureClient(request: Request) = BearerAuth(authHeader).then(insecureClient)(request)
    fun HealthCheck.withHealthClient(request: Request) = BearerAuth(healthAuthHeader).then(insecureClient)(request)

    protected val insecureClient = setBaseHeaders
        .then(PrintRequestAndResponse(System.err))
        .then(JavaHttpClient())

    companion object {
        fun endpoints() = DeployedLambda.values()
            .flatMap { it.endpointJsonNames.map { endpoint -> env.configFor(it, endpoint) } }

        fun healthEndPoints() = DeployedLambda.values()
            .flatMap { it.healthEndpointJsonNames.map { healthEndpoint -> env.configFor(it, healthEndpoint) } }

    }
}
