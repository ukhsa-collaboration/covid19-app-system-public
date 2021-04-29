package uk.nhs.nhsx.highriskvenuesupload

import com.amazonaws.services.cloudfront.AmazonCloudFrontClientBuilder
import com.amazonaws.services.kms.AWSKMSClientBuilder
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.EnvironmentKeys.BUCKET_NAME
import uk.nhs.nhsx.core.EnvironmentKeys.DISTRIBUTION_ID
import uk.nhs.nhsx.core.EnvironmentKeys.DISTRIBUTION_INVALIDATION_PATTERN
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.HttpResponses.accepted
import uk.nhs.nhsx.core.StandardSigningFactory
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.auth.ApiName
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFrontClient
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.s3.ObjectKey
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.events.RiskyVenuesUpload
import uk.nhs.nhsx.core.handler.ApiGatewayHandler
import uk.nhs.nhsx.core.routing.Routing.Method.POST
import uk.nhs.nhsx.core.routing.Routing.path
import uk.nhs.nhsx.core.routing.Routing.routes
import uk.nhs.nhsx.core.handler.RoutingHandler
import uk.nhs.nhsx.core.routing.authorisedBy
import uk.nhs.nhsx.core.routing.withoutSignedResponses
import uk.nhs.nhsx.highriskvenuesupload.VenuesUploadResult.Success
import uk.nhs.nhsx.highriskvenuesupload.VenuesUploadResult.ValidationError

class HighRiskVenuesUploadHandler @JvmOverloads constructor(
    environment: Environment = Environment.fromSystem(),
    clock: Clock = SystemClock.CLOCK,
    private val events: Events = PrintingJsonEvents(clock),
    authenticator: Authenticator = awsAuthentication(ApiName.HighRiskVenuesUpload, events),
    service: HighRiskVenuesUploadService = createUploadService(clock, environment, events),
    healthAuthenticator: Authenticator = awsAuthentication(ApiName.Health, events)
) : RoutingHandler() {
    override fun handler() = handler

    private fun mapResultToResponse(result: VenuesUploadResult) =
        when (result) {
            is Success -> accepted(result.message)
            is ValidationError -> {
                events(HighRiskVenueUploadFileInvalid)
                HttpResponses.unprocessableEntity(result.message)
            }
        }

    companion object {
        private fun createUploadService(
            clock: Clock,
            environment: Environment,
            events: Events
        ) = HighRiskVenuesUploadService(
            HighRiskVenuesUploadConfig.of(
                environment.access.required(BUCKET_NAME),
                ObjectKey.of("distribution/risky-venues"),
                environment.access.required(DISTRIBUTION_ID),
                environment.access.required(DISTRIBUTION_INVALIDATION_PATTERN)
            ),
            StandardSigningFactory(
                clock,
                AwsSsmParameters(),
                AWSKMSClientBuilder.defaultClient()
            ).datedSigner(environment),
            AwsS3Client(events),
            AwsCloudFrontClient(events, AmazonCloudFrontClientBuilder.defaultClient()),
            HighRiskVenueCsvParser()
        )
    }

    private val handler = withoutSignedResponses(
        events,
        environment,
        routes(
            authorisedBy(
                authenticator,
                path(POST, "/upload/identified-risk-venues", ApiGatewayHandler { r, _ ->
                    events(RiskyVenuesUpload())
                    if (r.isTextCsv()) {
                        mapResultToResponse(service.upload(r.body))
                    } else {
                        HttpResponses.unprocessableEntity("validation error: Content type is not text/csv")
                    }
                })
            ),
            authorisedBy(
                healthAuthenticator,
                path(POST, "/upload/identified-risk-venues/health", ApiGatewayHandler { _, _ -> HttpResponses.ok() })
            )
        )
    )

    private fun APIGatewayProxyRequestEvent.isTextCsv(): Boolean = headers["Content-Type"]?.startsWith("text/csv", ignoreCase = true) ?: false
}
