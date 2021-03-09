package uk.nhs.nhsx.highriskpostcodesupload

import com.amazonaws.services.cloudfront.AmazonCloudFrontClientBuilder
import com.amazonaws.services.kms.AWSKMSClientBuilder
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.EnvironmentKeys.BUCKET_NAME
import uk.nhs.nhsx.core.EnvironmentKeys.DISTRIBUTION_ID
import uk.nhs.nhsx.core.EnvironmentKeys.DISTRIBUTION_INVALIDATION_PATTERN
import uk.nhs.nhsx.core.HttpResponses
import uk.nhs.nhsx.core.StandardSigningFactory
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.auth.ApiName.Health
import uk.nhs.nhsx.core.auth.ApiName.HighRiskPostCodeUpload
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFront
import uk.nhs.nhsx.core.aws.cloudfront.AwsCloudFrontClient
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.s3.ObjectKey.Companion.of
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.events.RiskyPostDistrictUpload
import uk.nhs.nhsx.core.routing.ApiGatewayHandler
import uk.nhs.nhsx.core.routing.Routing.Method.POST
import uk.nhs.nhsx.core.routing.Routing.path
import uk.nhs.nhsx.core.routing.Routing.routes
import uk.nhs.nhsx.core.routing.RoutingHandler
import uk.nhs.nhsx.core.routing.StandardHandlers.authorisedBy
import uk.nhs.nhsx.core.routing.StandardHandlers.withoutSignedResponses
import uk.nhs.nhsx.core.signature.DatedSigner
import java.time.Instant
import java.util.function.Supplier

class HighRiskPostcodesUploadHandler @JvmOverloads constructor(
    environment: Environment = Environment.fromSystem(),
    clock: Supplier<Instant> = CLOCK,
    events: Events = PrintingJsonEvents(clock),
    authenticator: Authenticator = awsAuthentication(HighRiskPostCodeUpload, events),
    signer: DatedSigner = StandardSigningFactory(
        clock,
        AwsSsmParameters(),
        AWSKMSClientBuilder.defaultClient()
    ).datedSigner(Environment.fromSystem()),
    s3Storage: AwsS3 = AwsS3Client(events),
    awsCloudFront: AwsCloudFront = AwsCloudFrontClient(events, AmazonCloudFrontClientBuilder.defaultClient()),
    healthAuthenticator: Authenticator = awsAuthentication(Health, events)
) : RoutingHandler() {
    override fun handler() = handler

    companion object {
        private const val DISTRIBUTION_OBJ_KEY_NAME = "distribution/risky-post-districts"
        private const val DISTRIBUTION_OBJ_V2_KEY_NAME = "distribution/risky-post-districts-v2"
        private const val BACKUP_JSON_KEY_NAME = "backup/api-payload"
        private const val RAW_CSV_KEY_NAME = "raw/risky-post-districts"
        private const val METADATA_OBJ_KEY_NAME = "tier-metadata"
    }

    private val handler: ApiGatewayHandler

    init {
        val service = riskyPostCodesUploadService(
            riskyPostCodesPersistence(environment, signer, s3Storage),
            awsCloudFront,
            environment,
            events
        )
        handler = withoutSignedResponses(
            events,
            environment,
            routes(
                authorisedBy(
                    authenticator, path(POST, "/upload/high-risk-postal-districts",
                        ApiGatewayHandler { r, _ ->
                            events(javaClass, RiskyPostDistrictUpload())
                            service.upload(r.body)
                        })
                ),
                authorisedBy(
                    healthAuthenticator, path(POST, "/upload/high-risk-postal-districts/health",
                        ApiGatewayHandler { _, _ -> HttpResponses.ok() })
                )
            )
        )
    }

    private fun riskyPostCodesUploadService(
        persistence: RiskyPostCodesPersistence,
        awsCloudFront: AwsCloudFront,
        environment: Environment,
        events: Events
    ) = RiskyPostCodesUploadService(
        persistence,
        awsCloudFront,
        environment.access.required(DISTRIBUTION_ID),
        environment.access.required(DISTRIBUTION_INVALIDATION_PATTERN),
        events
    )

    private fun riskyPostCodesPersistence(
        environment: Environment,
        signer: DatedSigner,
        s3Storage: AwsS3
    ) = RiskyPostCodesPersistence(
        environment.access.required(BUCKET_NAME),
        of(DISTRIBUTION_OBJ_KEY_NAME),
        of(DISTRIBUTION_OBJ_V2_KEY_NAME),
        of(BACKUP_JSON_KEY_NAME),
        of(RAW_CSV_KEY_NAME),
        of(METADATA_OBJ_KEY_NAME),
        signer,
        s3Storage
    )
}
