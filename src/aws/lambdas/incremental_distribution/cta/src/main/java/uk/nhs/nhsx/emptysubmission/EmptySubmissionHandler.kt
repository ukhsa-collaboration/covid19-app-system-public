package uk.nhs.nhsx.emptysubmission

import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.HttpResponses.ok
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.auth.ApiName.Mobile
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.handler.RoutingHandler
import uk.nhs.nhsx.core.routing.Routing.Method.POST
import uk.nhs.nhsx.core.routing.Routing.path
import uk.nhs.nhsx.core.routing.Routing.routes
import uk.nhs.nhsx.core.routing.authorisedBy
import uk.nhs.nhsx.core.routing.withoutSignedResponses

class EmptySubmissionHandler @JvmOverloads constructor(
    environment: Environment = Environment.fromSystem(),
    events: Events = PrintingJsonEvents(SystemClock.CLOCK),
    authenticator: Authenticator = awsAuthentication(Mobile, events)
) : RoutingHandler() {

    private val handler = withoutSignedResponses(
        events = events,
        environment = environment,
        delegate = routes(
            authorisedBy(
                authenticator,
                path(POST, "/submission/empty-submission") { _, _ -> ok() }
            )
        )
    )

    override fun handler() = handler
}
