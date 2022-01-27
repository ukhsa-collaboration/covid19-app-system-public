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
import uk.nhs.nhsx.core.events.EventCategory
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.handler.SchedulingHandler
import uk.nhs.nhsx.diagnosiskeydist.s3.SubmissionFromS3Repository
import uk.nhs.nhsx.keyfederation.BatchTagDynamoDBService
import uk.nhs.nhsx.keyfederation.BatchTagService
import uk.nhs.nhsx.keyfederation.InteropClient

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

    private fun loadKeysAndUploadToFederatedServer(context: Context) = if (config.uploadFeatureFlag.isEnabled()) {
        try {
            val (filter, factory) = FederatedExposureUploadConfig(
                config.region,
                config.federatedKeyUploadPrefixes
            )

            val submissionRepository = SubmissionFromS3Repository(
                awsS3Client,
                filter,
                submissionBucket,
                config.loadSubmissionsTimeout,
                config.loadSubmissionsThreadPoolSize,
                events,
                clock
            )

            DiagnosisKeysUploadService(
                clock,
                interopClient,
                submissionRepository,
                batchTagService,
                factory,
                config.uploadRiskLevelDefaultEnabled,
                config.uploadRiskLevelDefault,
                config.initialUploadHistoryDays,
                config.maxUploadBatchSize,
                config.maxSubsequentBatchUploadCount,
                context,
                events
            ).loadKeysAndUploadToFederatedServer()
        } catch (e: Exception) {
            throw RuntimeException("Upload keys failed with error", e)
        }
    } else {
        events(InfoEvent("Upload to interop has been disabled, skipping this step"))
        0
    }
}

data class InteropConnectorUploadStats(val processedSubmissions: Int) : Event(EventCategory.Info)

private fun buildInteropClient(
    config: KeyFederationUploadConfig,
    secretManager: SecretManager,
    events: Events
): InteropClient {
    val authTokenSecretValue = secretManager.getSecret(config.interopAuthTokenSecretName)
        .orElseThrow { RuntimeException("Unable to retrieve authorization token from secrets storage") }

    val signer = StandardSigningFactory(
        CLOCK,
        AwsSsmParameters(),
        AWSKMSClientBuilder.defaultClient()
    ).signContentWithKeyFromParameter(config.signingKeyParameterName)

    return InteropClient(
        config.interopBaseUrl,
        authTokenSecretValue.value,
        JWS(signer),
        events
    )
}
