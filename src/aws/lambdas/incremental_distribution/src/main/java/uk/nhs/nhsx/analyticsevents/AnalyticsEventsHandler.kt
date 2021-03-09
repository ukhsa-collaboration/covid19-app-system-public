package uk.nhs.nhsx.analyticsevents

import com.amazonaws.services.kms.AWSKMSClientBuilder
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.EnvironmentKeys
import uk.nhs.nhsx.core.HttpResponses.badRequest
import uk.nhs.nhsx.core.HttpResponses.ok
import uk.nhs.nhsx.core.HttpResponses.serviceUnavailable
import uk.nhs.nhsx.core.StandardSigningFactory
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.UniqueId
import uk.nhs.nhsx.core.auth.ApiName.Health
import uk.nhs.nhsx.core.auth.ApiName.Mobile
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.ResponseSigner
import uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.aws.s3.PartitionedObjectKeyNameProvider
import uk.nhs.nhsx.core.aws.s3.S3Storage
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.routing.ApiGatewayHandler
import uk.nhs.nhsx.core.routing.Routing.Method.POST
import uk.nhs.nhsx.core.routing.Routing.path
import uk.nhs.nhsx.core.routing.Routing.routes
import uk.nhs.nhsx.core.routing.RoutingHandler
import uk.nhs.nhsx.core.routing.StandardHandlers.authorisedBy
import uk.nhs.nhsx.core.routing.StandardHandlers.withSignedResponses
import java.time.Instant
import java.util.function.Supplier

class AnalyticsEventsHandler @JvmOverloads constructor(
    environment: Environment = Environment.fromSystem(),
    clock: Supplier<Instant> = SystemClock.CLOCK,
    events: Events = PrintingJsonEvents(clock),
    authenticator: Authenticator = awsAuthentication(Mobile, events),
    signer: ResponseSigner = StandardSigningFactory(
        clock,
        AwsSsmParameters(),
        AWSKMSClientBuilder.defaultClient()
    ).signResponseWithKeyGivenInSsm(environment, events),
    s3Storage: S3Storage = AwsS3Client(events),
    objectKeyNameProvider: ObjectKeyNameProvider = PartitionedObjectKeyNameProvider(clock, UniqueId.ID),
    healthAuthenticator: Authenticator = awsAuthentication(Health, events)
) : RoutingHandler() {

    private val handler = withSignedResponses(
        events,
        environment,
        signer,
        routes(
            authorisedBy(
                authenticator,
                path(POST, "/submission/mobile-analytics-events",
                    ApiGatewayHandler { request, _ ->
                        when {
                            environment.access.required(Environment.EnvironmentKey.bool("ACCEPT_REQUESTS_ENABLED")) -> {
                                val payload = PayloadValidator().maybeValidPayload(request.body)
                                payload?.let {
                                    AnalyticsEventsSubmissionService(
                                        s3Storage,
                                        objectKeyNameProvider,
                                        environment.access.required(EnvironmentKeys.SUBMISSION_STORE),
                                        events
                                    ).accept(payload)
                                    ok()
                                } ?: badRequest()
                            }
                            else -> serviceUnavailable()
                        }
                    })
            ),
            authorisedBy(
                healthAuthenticator,
                path(POST, "/submission/mobile-analytics-events/health", ApiGatewayHandler { _, _ -> ok() })
            )
        )
    )

    override fun handler() = handler
}
