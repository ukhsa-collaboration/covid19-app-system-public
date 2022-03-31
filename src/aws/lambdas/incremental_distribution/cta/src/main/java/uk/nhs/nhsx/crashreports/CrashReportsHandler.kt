package uk.nhs.nhsx.crashreports

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.HttpResponses.badRequest
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.auth.ApiName.Mobile
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.MobileCrashReportsSubmission
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.events.RequestRejected
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.core.handler.RoutingHandler
import uk.nhs.nhsx.core.readJsonOrNull
import uk.nhs.nhsx.core.routing.Routing.Method.POST
import uk.nhs.nhsx.core.routing.Routing.path
import uk.nhs.nhsx.core.routing.Routing.routes
import uk.nhs.nhsx.core.routing.authorisedBy
import uk.nhs.nhsx.core.routing.withoutSignedResponses

class CrashReportsHandler @JvmOverloads constructor(
    environment: Environment = Environment.fromSystem(),
    private val events: Events = PrintingJsonEvents(CLOCK),
    authenticator: Authenticator = awsAuthentication(Mobile, events)
) : RoutingHandler() {

    private val handler = withoutSignedResponses(
        events = events,
        environment = environment,
        delegate = routes(
            authorisedBy(
                authenticator,
                path(POST, "/submission/crash-reports") { r, _ ->
                    events(MobileCrashReportsSubmission())
                    handleCrashReportEvent(r)
                }
            )
        )
    )

    private fun handleCrashReportEvent(request: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
        val bytes = request.body.toByteArray()

        if (bytes.size > MAX_PAYLOAD_SIZE_BYTES) {
            events(RequestRejected("payload size is too long:${bytes.size}, max:$MAX_PAYLOAD_SIZE_BYTES"))
            return badRequest()
        }

        val crashReportRequest = Json.readJsonOrNull<CrashReportRequest>(request.body) {
            events(UnprocessableJson(it))
        } ?: return badRequest()

        return crashReportRequest
            .sanitiseUrls()
            .storeEvent()
            .let { HttpResponses.ok() }
    }

    private fun CrashReportRequest.storeEvent() = apply {
        when (exception) {
            "android.app.RemoteServiceException" -> events(CrashReportStored(this))
            else -> events(CrashReportNotRecognised(exception))
        }
    }

    override fun handler() = handler

    private companion object {
        const val MAX_PAYLOAD_SIZE_BYTES = 10240 // 10KB
    }
}
