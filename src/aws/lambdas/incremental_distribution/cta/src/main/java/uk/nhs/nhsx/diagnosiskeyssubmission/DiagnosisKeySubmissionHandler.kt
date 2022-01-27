package uk.nhs.nhsx.diagnosiskeyssubmission

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.kms.AWSKMSClientBuilder
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.EnvironmentKeys.SUBMISSIONS_TOKENS_TABLE
import uk.nhs.nhsx.core.EnvironmentKeys.SUBMISSION_STORE
import uk.nhs.nhsx.core.HttpResponses.ok
import uk.nhs.nhsx.core.Json
import uk.nhs.nhsx.core.StandardSigningFactory
import uk.nhs.nhsx.core.SystemClock
import uk.nhs.nhsx.core.UniqueId.Companion.ID
import uk.nhs.nhsx.core.auth.ApiName.Health
import uk.nhs.nhsx.core.auth.ApiName.Mobile
import uk.nhs.nhsx.core.auth.Authenticator
import uk.nhs.nhsx.core.auth.ResponseSigner
import uk.nhs.nhsx.core.auth.StandardAuthentication.awsAuthentication
import uk.nhs.nhsx.core.aws.dynamodb.AwsDynamoClient
import uk.nhs.nhsx.core.aws.dynamodb.DynamoDBUtils
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.s3.ObjectKeyNameProvider
import uk.nhs.nhsx.core.aws.s3.UniqueObjectKeyNameProvider
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.events.DiagnosisKeySubmission
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.events.UnprocessableJson
import uk.nhs.nhsx.core.handler.ApiGatewayHandler
import uk.nhs.nhsx.core.handler.RoutingHandler
import uk.nhs.nhsx.core.readJsonOrNull
import uk.nhs.nhsx.core.routing.Routing.Method.POST
import uk.nhs.nhsx.core.routing.Routing.path
import uk.nhs.nhsx.core.routing.Routing.routes
import uk.nhs.nhsx.core.routing.authorisedBy
import uk.nhs.nhsx.core.routing.withSignedResponses
import uk.nhs.nhsx.diagnosiskeyssubmission.model.ClientTemporaryExposureKeysPayload

class DiagnosisKeySubmissionHandler @JvmOverloads constructor(
    environment: Environment = Environment.fromSystem(),
    clock: Clock = SystemClock.CLOCK,
    events: Events = PrintingJsonEvents(clock),
    mobileAuthenticator: Authenticator = awsAuthentication(Mobile, events),
    healthAuthenticator: Authenticator = awsAuthentication(Health, events),
    signer: ResponseSigner = StandardSigningFactory(
        clock,
        AwsSsmParameters(),
        AWSKMSClientBuilder.defaultClient()
    ).signResponseWithKeyGivenInSsm(environment, events),
    awsS3: AwsS3 = AwsS3Client(events),
    awsDynamoClient: AwsDynamoClient = DynamoDBUtils(AmazonDynamoDBClientBuilder.defaultClient()),
    objectKeyNameProvider: ObjectKeyNameProvider = UniqueObjectKeyNameProvider(clock, ID)
) : RoutingHandler() {

    private fun diagnosisKeysSubmissionService(
        environment: Environment,
        events: Events,
        clock: Clock,
        awsS3: AwsS3,
        awsDynamoClient: AwsDynamoClient,
        objectKeyNameProvider: ObjectKeyNameProvider
    ) = DiagnosisKeysSubmissionService(
        awsS3,
        awsDynamoClient,
        objectKeyNameProvider,
        environment.access.required(SUBMISSIONS_TOKENS_TABLE),
        environment.access.required(SUBMISSION_STORE),
        clock,
        events
    )

    override fun handler(): ApiGatewayHandler = handler

    private val handler = createHandler(
        events,
        environment,
        signer,
        mobileAuthenticator,
        healthAuthenticator,
        diagnosisKeysSubmissionService(
            environment,
            events,
            clock,
            awsS3,
            awsDynamoClient,
            objectKeyNameProvider
        )
    )

    private fun createHandler(
        events: Events,
        environment: Environment,
        signer: ResponseSigner,
        mobileAuthenticator: Authenticator,
        healthAuthenticator: Authenticator,
        diagnosisKeysSubmissionService: DiagnosisKeysSubmissionService
    ) = withSignedResponses(events,
        environment,
        signer,
        routes(
            authorisedBy(
                mobileAuthenticator,
                path(POST, "/submission/diagnosis-keys", ApiGatewayHandler { r, _ ->
                    events(DiagnosisKeySubmission())
                    Json.readJsonOrNull<ClientTemporaryExposureKeysPayload>(r.body) {
                        events(UnprocessableJson(it))
                    }?.also(diagnosisKeysSubmissionService::acceptTemporaryExposureKeys)
                    ok()
                })
            ),
            authorisedBy(
                healthAuthenticator,
                path(POST, "/submission/diagnosis-keys/health", ApiGatewayHandler { _, _ -> ok() })
            )
        )
    )
}
