package uk.nhs.nhsx.analyticssubmission

import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClientBuilder
import uk.nhs.nhsx.analyticssubmission.model.ClientAnalyticsSubmissionPayload
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Environment.EnvironmentKey
import uk.nhs.nhsx.core.HttpResponses.badRequest
import uk.nhs.nhsx.core.HttpResponses.ok
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.auth.ApiName.Health
import uk.nhs.nhsx.core.auth.ApiName.Mobile
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.MobileAnalyticsSubmission
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.core.handler.ApiGatewayHandler
import uk.nhs.nhsx.core.handler.RoutingHandler
import uk.nhs.nhsx.core.readJsonOrNull
import uk.nhs.nhsx.core.routing.Routing.Method.POST
import uk.nhs.nhsx.core.routing.Routing.path
import uk.nhs.nhsx.core.routing.Routing.routes
import uk.nhs.nhsx.core.routing.authorisedBy
import uk.nhs.nhsx.core.routing.withoutSignedResponses

//TODO: merge AnalyticsSubmissionQueuedHandler and AnalyticsSubmissionHandler
class AnalyticsSubmissionHandler @JvmOverloads constructor(
    environment: Environment = Environment.fromSystem(),
    clock: Clock = CLOCK,
    events: Events = PrintingJsonEvents(clock),
    healthAuthenticator: Authenticator = awsAuthentication(Health, events),
    mobileAuthenticator: Authenticator = awsAuthentication(Mobile, events),
    kinesisFirehose: AmazonKinesisFirehose = AmazonKinesisFirehoseClientBuilder.defaultClient(),
    analyticsConfig: AnalyticsConfig = analyticsConfig(environment),
    service: AnalyticsSubmissionService = AnalyticsSubmissionService(analyticsConfig, kinesisFirehose, events, clock)
) : RoutingHandler() {

    override fun handler(): ApiGatewayHandler = handler

    private val handler = withoutSignedResponses(
        events,
        environment,
        routes(
            authorisedBy(
                mobileAuthenticator,
                path(POST, "/submission/mobile-analytics", ApiGatewayHandler { r, _ ->
                    events(MobileAnalyticsSubmission())
                    Json.readJsonOrNull<ClientAnalyticsSubmissionPayload>(r.body) { events(UnprocessableJson(it)) }
                        ?.let {
                            service.accept(it)
                            ok()
                        } ?: badRequest()
                })
            ),
            authorisedBy(
                healthAuthenticator,
                path(POST, "/submission/mobile-analytics/health", ApiGatewayHandler { _, _ -> ok() })
            )
        )
    )

    companion object {
        private val FIREHOSE_INGEST_ENABLED = EnvironmentKey.bool("firehose_ingest_enabled")
        private val FIREHOSE_STREAM = EnvironmentKey.string("firehose_stream_name")

        private fun analyticsConfig(environment: Environment) = AnalyticsConfig(
            environment.access.required(FIREHOSE_STREAM),
            environment.access.required(FIREHOSE_INGEST_ENABLED)
        )
    }
}
