package uk.nhs.nhsx.crashreports

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.auth.ApiName.Mobile
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.MobileCrashReportsSubmission
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.events.RequestRejected
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.core.handler.ApiGatewayHandler
import uk.nhs.nhsx.core.handler.RoutingHandler
import uk.nhs.nhsx.core.readJsonOrNull
import uk.nhs.nhsx.core.routing.Routing.Method.POST
import uk.nhs.nhsx.core.routing.Routing.path
import uk.nhs.nhsx.core.routing.Routing.routes
import uk.nhs.nhsx.core.routing.authorisedBy
import uk.nhs.nhsx.core.routing.withoutSignedResponses

@Suppress("unused")
class CrashReportsHandler @JvmOverloads constructor(
    environment: Environment = Environment.fromSystem(),
    private val events: Events = PrintingJsonEvents(SystemClock.CLOCK),
    authenticator: Authenticator = awsAuthentication(Mobile, events)
) : RoutingHandler() {

    override fun handler() = handler

    private companion object {
        const val MAX_PAYLOAD_SIZE_BYTES = 10240 // 10KB
    }

    private val handler: ApiGatewayHandler = withoutSignedResponses(
        events, environment, routes(
            authorisedBy(
                authenticator,
                path(POST, "/submission/crash-reports", ApiGatewayHandler { r, _ ->
                    events(MobileCrashReportsSubmission())
                    handleCrashReportEvent(r)
                })
            )
        )
    )

    private fun handleCrashReportEvent(requestEvent: APIGatewayProxyRequestEvent): APIGatewayProxyResponseEvent {
        val bytes = requestEvent.body.toByteArray()
        if (bytes.size > MAX_PAYLOAD_SIZE_BYTES) {
            events(RequestRejected("payload size is too long:${bytes.size}, max:$MAX_PAYLOAD_SIZE_BYTES"))
            return HttpResponses.badRequest()
        }

        val request = Json.readJsonOrNull<CrashReportRequest>(requestEvent.body) { events(UnprocessableJson(it)) }
            ?: return HttpResponses.badRequest()

        storeEventFrom(request)
        return HttpResponses.ok()
    }

    private fun storeEventFrom(request: CrashReportRequest) {
        val sanitised = request.sanitise()
        when (sanitised.exception) {
            "android.app.RemoteServiceException" -> events(CrashReportStored(sanitised))
            else -> events(CrashReportNotRecognised(sanitised.exception))
        }
    }

    private fun CrashReportRequest.sanitise(): CrashReportRequest =
        CrashReportRequest(
            exception = exception.sanitiseUrls(),
            threadName = threadName.sanitiseUrls(),
            stackTrace = stackTrace.sanitiseUrls()
        )

    private fun String.sanitiseUrls(): String = replace("http://", "").replace("https://", "")
}
