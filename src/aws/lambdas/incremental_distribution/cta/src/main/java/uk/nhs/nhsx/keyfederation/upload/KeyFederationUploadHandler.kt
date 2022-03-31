package uk.nhs.nhsx.keyfederation.upload

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.kms.AWSKMSClientBuilder
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.EnvironmentKeys
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.StandardSigningFactory
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.s3.BucketName
import uk.nhs.nhsx.core.aws.secretsmanager.AwsSecretManager
import uk.nhs.nhsx.core.aws.secretsmanager.SecretManager
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.handler.SchedulingHandler
import uk.nhs.nhsx.diagnosiskeydist.s3.SubmissionFromS3Repository
import uk.nhs.nhsx.keyfederation.storage.BatchTagDynamoDBService
import uk.nhs.nhsx.keyfederation.storage.BatchTagService
import uk.nhs.nhsx.keyfederation.client.HttpInteropClient
import uk.nhs.nhsx.keyfederation.client.InteropClient
import uk.nhs.nhsx.keyfederation.InteropConnectorUploadStats

/**
 * Key Federation upload lambda
 * s
 * doc/architecture/api-contracts/diagnosis-key-federation-connector.md
 */
class KeyFederationUploadHandler @JvmOverloads constructor(
    private val environment: Environment = Environment.fromSystem(),
    private val clock: Clock = CLOCK,
    events: Events = PrintingJsonEvents(clock),
    private val submissionBucket: BucketName = environment.access.required(EnvironmentKeys.SUBMISSION_BUCKET_NAME),
    private val config: KeyFederationUploadConfig = KeyFederationUploadConfig.fromEnvironment(environment),
    secretManager: SecretManager = AwsSecretManager(AWSSecretsManagerClientBuilder.defaultClient()),
    private val batchTagService: BatchTagService = BatchTagDynamoDBService(
        config.stateTableName,
        AmazonDynamoDBClientBuilder.defaultClient(),
        events
    ),
    private val interopClient: InteropClient = buildInteropClient(config, secretManager, events),
    private val awsS3Client: AwsS3 = AwsS3Client(events)
) : SchedulingHandler(events) {

    override fun handler() = Handler<ScheduledEvent, Event> { _, context ->
        InteropConnectorUploadStats(loadKeysAndUploadToFederatedServer(context))
    }

    private fun loadKeysAndUploadToFederatedServer(context: Context) = when {
        config.uploadFeatureFlag.isEnabled() -> {
            val (filter, factory) = FederatedExposureUploadConfig(
                config.region,
                config.federatedKeyUploadPrefixes
            )

            val submissionRepository = SubmissionFromS3Repository(
                awsS3 = awsS3Client,
                objectKeyFilter = filter,
                submissionBucketName = submissionBucket,
                loadSubmissionsTimeout = config.loadSubmissionsTimeout,
                loadSubmissionsThreadPoolSize = config.loadSubmissionsThreadPoolSize,
                events = events,
                clock = clock
            )

            DiagnosisKeysUploadService(
                clock = clock,
                interopClient = interopClient,
                submissionRepository = submissionRepository,
                batchTagService = batchTagService,
                exposureUploadFactory = factory,
                uploadRiskLevelDefaultEnabled = config.uploadRiskLevelDefaultEnabled,
                uploadRiskLevelDefault = config.uploadRiskLevelDefault,
                initialUploadHistoryDays = config.initialUploadHistoryDays,
                maxUploadBatchSize = config.maxUploadBatchSize,
                maxSubsequentBatchUploadCount = config.maxSubsequentBatchUploadCount,
                context = context,
                events = events
            ).loadKeysAndUploadToFederatedServer()
        }
        else -> {
            events(InfoEvent("Upload to interop has been disabled, skipping this step"))
            0
        }
    }
}

private fun buildInteropClient(
    config: KeyFederationUploadConfig,
    secretManager: SecretManager,
    events: Events
): InteropClient {
    val authTokenSecretValue = secretManager.getSecret(config.interopAuthTokenSecretName)
        .orElseThrow { RuntimeException("Unable to retrieve authorization token from secrets storage") }

    val signer = StandardSigningFactory(
        clock = CLOCK,
        parameters = AwsSsmParameters(),
        client = AWSKMSClientBuilder.defaultClient()
    ).signContentWithKeyFromParameter(config.signingKeyParameterName)

    return HttpInteropClient(
        interopBaseUrl = config.interopBaseUrl,
        authToken = authTokenSecretValue.value,
        jws = JWS(signer),
        events = events
    )
}
