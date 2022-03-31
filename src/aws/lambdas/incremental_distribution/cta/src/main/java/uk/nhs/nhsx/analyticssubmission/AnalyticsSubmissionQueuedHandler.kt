package uk.nhs.nhsx.analyticssubmission

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClientBuilder
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.auth.ApiName
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.StandardAuthentication
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.MobileAnalyticsSubmission
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.core.handler.ApiGatewayHandler
import uk.nhs.nhsx.core.handler.QueuedHandler
import uk.nhs.nhsx.core.handler.RoutingHandler
import uk.nhs.nhsx.core.readJsonOrNull
import uk.nhs.nhsx.core.routing.Routing.Method.POST
import uk.nhs.nhsx.core.routing.Routing.path
import uk.nhs.nhsx.core.routing.Routing.routes
import uk.nhs.nhsx.core.routing.authorisedBy
import uk.nhs.nhsx.core.routing.withoutSignedResponses

class AnalyticsSubmissionQueuedHandler constructor(
    environment: Environment = Environment.fromSystem(),
    clock: Clock = SystemClock.CLOCK,
    events: Events = PrintingJsonEvents(clock),
    submissionDelegate: AnalyticsSubmissionDelegate = AnalyticsSubmissionDelegate(environment, clock, events)
) : QueuedHandler(events) {

    private val handler = Handler<SQSEvent, Event> { input, context ->
        submissionDelegate.handleRequest(extractRequestEventFrom(input), context)
        QueueEntryProcessed
    }

    override fun handler() = handler

    private fun extractRequestEventFrom(input: SQSEvent): APIGatewayProxyRequestEvent {
        if (input.records.size != 1) throw IllegalStateException(".tf configuration error: batch_size != 1")
        return Json.readJsonOrThrow(input.records[0].body)
    }

    class AnalyticsSubmissionDelegate @JvmOverloads constructor(
        environment: Environment,
        clock: Clock,
        events: Events,
        healthAuthenticator: Authenticator = StandardAuthentication.awsAuthentication(ApiName.Health, events),
        mobileAuthenticator: Authenticator = StandardAuthentication.awsAuthentication(ApiName.Mobile, events),
        kinesisFirehose: AmazonKinesisFirehose = AmazonKinesisFirehoseClientBuilder.defaultClient(),
        analyticsConfig: AnalyticsConfig = AnalyticsConfig.from(environment),
        service: AnalyticsSubmissionService = AnalyticsSubmissionService(
            config = analyticsConfig,
            kinesisFirehose = kinesisFirehose,
            events = events,
            clock = clock
        )
    ) : RoutingHandler() {

        override fun handler(): ApiGatewayHandler = handler

        private val handler = withoutSignedResponses(
            events = events,
            environment = environment,
            delegate = routes(
                authorisedBy(
                    mobileAuthenticator,
                    path(POST, "/submission/mobile-analytics") { r, _ ->
                        events(MobileAnalyticsSubmission())
                        Json.readJsonOrNull<ClientAnalyticsSubmissionPayload>(r.body) { events(UnprocessableJson(it)) }
                            ?.let {
                                service.accept(it)
                                HttpResponses.ok()
                            } ?: HttpResponses.badRequest()
                    }
                ),
                authorisedBy(
                    healthAuthenticator,
                    path(POST, "/submission/mobile-analytics/health") { _, _ -> HttpResponses.ok() }
                )
            )
        )
    }
}

