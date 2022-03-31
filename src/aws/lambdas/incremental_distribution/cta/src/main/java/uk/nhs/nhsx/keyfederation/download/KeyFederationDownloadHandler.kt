package uk.nhs.nhsx.keyfederation.download

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import com.amazonaws.services.kms.AWSKMSClientBuilder
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.events.ScheduledEvent
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder
import uk.nhs.nhsx.core.Clock
import uk.nhs.nhsx.core.Environment
import uk.nhs.nhsx.core.Handler
import uk.nhs.nhsx.core.StandardSigningFactory
import uk.nhs.nhsx.core.SystemClock.CLOCK
import uk.nhs.nhsx.core.aws.s3.AwsS3
import uk.nhs.nhsx.core.aws.s3.AwsS3Client
import uk.nhs.nhsx.core.aws.secretsmanager.AwsSecretManager
import uk.nhs.nhsx.core.aws.secretsmanager.SecretManager
import uk.nhs.nhsx.core.aws.ssm.AwsSsmParameters
import uk.nhs.nhsx.core.events.Event
import uk.nhs.nhsx.core.events.Events
import uk.nhs.nhsx.core.events.InfoEvent
import uk.nhs.nhsx.core.events.PrintingJsonEvents
import uk.nhs.nhsx.core.handler.SchedulingHandler
import uk.nhs.nhsx.keyfederation.storage.BatchTagDynamoDBService
import uk.nhs.nhsx.keyfederation.storage.BatchTagService
import uk.nhs.nhsx.keyfederation.FederatedKeyUploader
import uk.nhs.nhsx.keyfederation.client.HttpInteropClient
import uk.nhs.nhsx.keyfederation.client.InteropClient
import uk.nhs.nhsx.keyfederation.InteropConnectorDownloadStats
import uk.nhs.nhsx.keyfederation.upload.JWS

/**
 * Key Federation download lambda
 *
 *
 * doc/architecture/api-contracts/key-federation/diagnosis-key-federation-connector.md
 */
class KeyFederationDownloadHandler @JvmOverloads constructor(
    private val clock: Clock = CLOCK,
    events: Events = PrintingJsonEvents(clock),
    private val config: KeyFederationDownloadConfig = KeyFederationDownloadConfig.fromEnvironment(Environment.fromSystem()),
    private val batchTagService: BatchTagService = BatchTagDynamoDBService(
        config.stateTableName,
        AmazonDynamoDBClientBuilder.defaultClient(),
        events
    ),
    private val secretManager: SecretManager = AwsSecretManager(AWSSecretsManagerClientBuilder.defaultClient()),
    private val interopClient: InteropClient = buildInteropClient(config, secretManager, events, CLOCK),
    private val awsS3: AwsS3 = AwsS3Client(events)
) : SchedulingHandler(events) {

    private fun downloadFromFederatedServerAndStoreKeys(context: Context) = when {
        config.downloadFeatureFlag.isEnabled() -> {

            DiagnosisKeysDownloadService(
                clock = clock,
                interopClient = interopClient,
                keyUploader = FederatedKeyUploader(
                    awsS3 = awsS3,
                    bucketName = config.submissionBucketName,
                    federatedKeySourcePrefix = config.federatedKeyDownloadPrefix,
                    clock = clock,
                    validOrigins = config.validOrigins,
                    events = events
                ),
                batchTagService = batchTagService,
                downloadRiskLevelDefaultEnabled = config.downloadRiskLevelDefaultEnabled,
                downloadRiskLevelDefault = config.downloadRiskLevelDefault,
                initialDownloadHistoryDays = config.initialDownloadHistoryDays,
                maxSubsequentBatchDownloadCount = config.maxSubsequentBatchDownloadCount,
                context = context,
                events = events
            ).downloadFromFederatedServerAndStoreKeys()
        }
        else -> {
            events(InfoEvent("Download to interop has been disabled, skipping this step"))
            0
        }
    }

    override fun handler() = Handler<ScheduledEvent, Event> { _, context ->
        InteropConnectorDownloadStats(downloadFromFederatedServerAndStoreKeys(context))
    }
}

private fun buildInteropClient(
    config: KeyFederationDownloadConfig,
    secretManager: SecretManager,
    events: Events,
    clock: Clock
): InteropClient {

    val authTokenSecretValue = secretManager.getSecret(config.interopAuthTokenSecretName)
        .orElseThrow { RuntimeException("Unable to retrieve authorization token from secrets storage") }

    val signer = StandardSigningFactory(
        clock = clock,
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
