package uk.nhs.nhsx.analyticssubmission

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClientBuilder
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import com.amazonaws.services.lambda.runtime.events.SQSEvent
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.core.*
import uk.nhs.nhsx.core.auth.ApiName
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.StandardAuthentication
import uk.nhs.nhsx.core.events.*
import uk.nhs.nhsx.core.handler.ApiGatewayHandler
import uk.nhs.nhsx.core.handler.QueuedHandler
import uk.nhs.nhsx.core.handler.RoutingHandler
import uk.nhs.nhsx.core.routing.Routing
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
        if (input.records.size != 1) {
            throw IllegalStateException(".tf configuration error: batch_size != 1")
        }

        return Json.readJsonOrThrow(input.records[0].body)
    }

    class AnalyticsSubmissionDelegate @JvmOverloads constructor(
        environment: Environment,
        clock: Clock,
        events: Events,
        healthAuthenticator: Authenticator = StandardAuthentication.awsAuthentication(ApiName.Health, events),
        mobileAuthenticator: Authenticator = StandardAuthentication.awsAuthentication(ApiName.Mobile, events),
        kinesisFirehose: AmazonKinesisFirehose = AmazonKinesisFirehoseClientBuilder.defaultClient(),
        analyticsConfig: AnalyticsConfig = analyticsConfig(environment),
        service: AnalyticsSubmissionService = AnalyticsSubmissionService(
            analyticsConfig,
            kinesisFirehose,
            events,
            clock
        )
    ) : RoutingHandler() {

        override fun handler(): ApiGatewayHandler = handler

        private val handler = withoutSignedResponses(
            events,
            environment,
            Routing.routes(
                authorisedBy(
                    mobileAuthenticator,
                    Routing.path(Routing.Method.POST, "/submission/mobile-analytics", ApiGatewayHandler { r, _ ->
                        events(MobileAnalyticsSubmission())
                        Json.readJsonOrNull<ClientAnalyticsSubmissionPayload>(r.body) { events(UnprocessableJson(it)) }
                            ?.let {
                                service.accept(it)
                                HttpResponses.ok()
                            } ?: HttpResponses.badRequest()
                    })
                ),
                authorisedBy(
                    healthAuthenticator,
                    Routing.path(
                        Routing.Method.POST,
                        "/submission/mobile-analytics/health",
                        ApiGatewayHandler { _, _ -> HttpResponses.ok() })
                )
            )
        )

        companion object {
            private val FIREHOSE_INGEST_ENABLED = Environment.EnvironmentKey.bool("firehose_ingest_enabled")
            private val FIREHOSE_STREAM = Environment.EnvironmentKey.string("firehose_stream_name")

            private fun analyticsConfig(environment: Environment) = AnalyticsConfig(
                environment.access.required(FIREHOSE_STREAM),
                environment.access.required(FIREHOSE_INGEST_ENABLED)
            )
        }
    }
}

