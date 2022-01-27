@file:Suppress("TestFunctionName")

package uk.nhs.nhsx.localstats.data

import org.http4k.chaos.ChaoticHttpHandler
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.routing.bind
import org.http4k.routing.routes
import java.time.Clock
import java.time.Instant

class FakeCoronavirusWebsiteBackend(
    private val timestamp: Instant = Instant.now(Clock.systemUTC())
) : ChaoticHttpHandler() {

    override val app = routes(
        "/public/assets/dispatch/website_timestamp" bind Method.GET to Timestamp()
    )

    private fun Timestamp(): HttpHandler = {
        Response(Status.OK).header("Content-Type", "text/plain").body("""$timestamp""")
    }
}
