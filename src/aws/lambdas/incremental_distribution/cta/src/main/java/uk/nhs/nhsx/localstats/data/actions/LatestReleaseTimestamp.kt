package uk.nhs.nhsx.localstats.data.actions

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import uk.nhs.nhsx.localstats.data.CoronavirusWebsiteAction
import uk.nhs.nhsx.localstats.domain.ReleaseDate

object LatestReleaseTimestamp : CoronavirusWebsiteAction<ReleaseDate> {
    override fun toRequest() = Request(GET, "/public/assets/dispatch/website_timestamp")
        .header("Accept", "text/plain")

    override fun fromResponse(response: Response) = when {
        response.status.successful -> ReleaseDate.parse(response.bodyString())
        else -> error(response.toMessage())
    }
}
